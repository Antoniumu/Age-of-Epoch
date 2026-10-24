package com.robertx22.age_of_exile.wiki;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Парсер markdown-диалекта внутриигровой вики Age of Epoch.
 *
 * <p>Класс специально не зависит от Minecraft: только чистая Java, поэтому его
 * можно тестировать вне игры (tools/wiki_gen/WikiMarkdownTest.java). Синтаксис
 * тоже специально консервативный — файлы вики проверяются статическими
 * парсерами в CI.
 *
 * <p>Поддерживается:
 * frontmatter между строками --- (ключи title, item, category,
 * category_label, tags), заголовки #..###, абзацы, списки «-» и «1.»,
 * цитаты «&gt;», разделители ---, таблицы | a | b | (рендерятся строками),
 * код-блоки ``` и инлайн: **жирный**, *курсив*, `код`,
 * [[страница|подпись]], [[страница#якорь|подпись]], [[#якорь|подпись]],
 * [подпись](страница).
 *
 * <p>Парсер никогда не бросает исключений на битом вводе: незакрытая
 * разметка остаётся обычным текстом, а вместо страницы с ошибкой игрок
 * видит её содержимое как есть.
 */
public final class WikiMarkdown {

    private WikiMarkdown() {
    }

    public enum Kind {
        HEADING, PARAGRAPH, LIST_ITEM, NUMBERED_ITEM, QUOTE, RULE, TABLE_ROW, CODE
    }

    /** Один инлайн-кусок текста внутри блока. */
    public static final class Span {
        public final String text;
        public final boolean bold;
        public final boolean italic;
        public final boolean code;
        /** Цель ссылки: «page», «page#anchor» или «#anchor»; null — не ссылка. */
        public final String link;

        public Span(String text, boolean bold, boolean italic, boolean code, String link) {
            this.text = text == null ? "" : text;
            this.bold = bold;
            this.italic = italic;
            this.code = code;
            this.link = link;
        }

        static Span plain(String text) {
            return new Span(text, false, false, false, null);
        }
    }

    /** Один блок документа. */
    public static final class Block {
        public final Kind kind;
        /** Для заголовков — глубина (1..3); для списков — уровень вложенности. */
        public final int level;
        public final List<Span> spans;
        /** Якорь заголовка (slug), для остальных блоков — null. */
        public final String anchor;
        /**_plain-текст блока без разметки (для поиска и отладки). */
        public final String plain;

        public Block(Kind kind, int level, List<Span> spans, String anchor) {
            this.kind = kind;
            this.level = level;
            this.spans = spans == null ? Collections.<Span>emptyList() : spans;
            this.anchor = anchor;
            StringBuilder sb = new StringBuilder();
            for (Span s : this.spans) {
                sb.append(s.text);
            }
            this.plain = sb.toString();
        }
    }

    /** Распарсенная страница вики. */
    public static final class Page {
        public final String id;
        public final String title;
        /** Registry id предмета для 3D-превью, может быть null. */
        public final String itemId;
        public final String category;
        public final String categoryLabel;
        public final String tags;
        public final List<Block> blocks;
        /** Все уникальные цели ссылок на странице (без якорей, в порядке появления). */
        public final List<String> links;
        /** Нижний регистр: название+теги+весь текст, для поиска. */
        public final String searchText;

        public Page(String id, String title, String itemId, String category, String categoryLabel,
                    String tags, List<Block> blocks) {
            this.id = id;
            this.title = title;
            this.itemId = itemId;
            this.category = category == null ? "" : category;
            this.categoryLabel = categoryLabel == null ? "" : categoryLabel;
            this.tags = tags == null ? "" : tags;
            this.blocks = blocks == null ? Collections.<Block>emptyList() : blocks;

            List<String> found = new ArrayList<String>();
            StringBuilder search = new StringBuilder();
            search.append(title).append(' ').append(this.tags).append(' ').append(id).append(' ');
            if (itemId != null) {
                search.append(itemId).append(' ');
            }
            for (Block b : this.blocks) {
                search.append(b.plain).append(' ');
                for (Span s : b.spans) {
                    if (s.link != null) {
                        String target = s.link;
                        int hash = target.indexOf('#');
                        if (hash >= 0) {
                            target = target.substring(0, hash);
                        }
                        if (target.length() > 0 && !found.contains(target)) {
                            found.add(target);
                        }
                    }
                }
            }
            this.links = found;
            this.searchText = search.toString().toLowerCase(Locale.ROOT);
        }

        public boolean hasAnchor(String anchor) {
            if (anchor == null) {
                return false;
            }
            for (Block b : blocks) {
                if (anchor.equals(b.anchor)) {
                    return true;
                }
            }
            return false;
        }
    }

    /** Slug заголовка: нижний регистр, пробелы -> '-', обрезка '-' по краям. Должен совпадать с verify_wiki.py. */
    public static String slugify(String text) {
        if (text == null) {
            return "";
        }
        String lowered = text.toLowerCase(Locale.ROOT).trim();
        StringBuilder sb = new StringBuilder();
        boolean prevSpace = false;
        for (int i = 0; i < lowered.length(); i++) {
            char c = lowered.charAt(i);
            if (Character.isWhitespace(c)) {
                if (!prevSpace && sb.length() > 0) {
                    sb.append('-');
                }
                prevSpace = true;
            } else {
                sb.append(c);
                prevSpace = false;
            }
        }
        String s = sb.toString();
        while (s.startsWith("-")) {
            s = s.substring(1);
        }
        while (s.endsWith("-")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    public static Page parse(String pageId, String markdown) {
        String id = pageId == null ? "index" : pageId;
        String source = markdown == null ? "" : markdown;
        try {
            return doParse(id, source);
        } catch (Throwable ignored) {
            // Последний рубеж: показать сырой текст вместо падения экрана.
            List<Block> fallback = new ArrayList<Block>();
            fallback.add(new Block(Kind.HEADING, 1, oneSpan(id), slugify(id)));
            fallback.add(new Block(Kind.PARAGRAPH, 0, oneSpan(source), null));
            return new Page(id, id, null, "", "", "", fallback);
        }
    }

    private static List<Span> oneSpan(String text) {
        List<Span> list = new ArrayList<Span>();
        list.add(Span.plain(text));
        return list;
    }

    private static Page doParse(String pageId, String markdown) {
        String[] rawLines = markdown.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);

        String title = pageId;
        String itemId = null;
        String category = "";
        String categoryLabel = "";
        String tags = "";
        int start = 0;

        // frontmatter
        if (rawLines.length > 0 && rawLines[0].trim().equals("---")) {
            for (int i = 1; i < rawLines.length; i++) {
                String line = rawLines[i];
                if (line.trim().equals("---")) {
                    start = i + 1;
                    break;
                }
                int colon = line.indexOf(':');
                if (colon > 0) {
                    String key = line.substring(0, colon).trim();
                    String value = line.substring(colon + 1).trim();
                    if (key.equals("title") && value.length() > 0) {
                        title = value;
                    } else if (key.equals("item") && value.length() > 0) {
                        itemId = value;
                    } else if (key.equals("category")) {
                        category = value;
                    } else if (key.equals("category_label")) {
                        categoryLabel = value;
                    } else if (key.equals("tags")) {
                        tags = value;
                    }
                }
                if (i == rawLines.length - 1) {
                    start = rawLines.length;
                }
            }
        }

        List<Block> blocks = new ArrayList<Block>();
        StringBuilder paragraph = new StringBuilder();
        boolean inCode = false;
        StringBuilder codeBuf = new StringBuilder();

        for (int i = start; i < rawLines.length; i++) {
            String line = rawLines[i];
            String trimmed = line.trim();

            if (inCode) {
                if (trimmed.startsWith("```")) {
                    blocks.add(new Block(Kind.CODE, 0, oneSpan(codeBuf.toString()), null));
                    codeBuf.setLength(0);
                    inCode = false;
                } else {
                    if (codeBuf.length() > 0) {
                        codeBuf.append('\n');
                    }
                    codeBuf.append(line);
                }
                continue;
            }

            if (trimmed.startsWith("```")) {
                flushParagraph(blocks, paragraph);
                inCode = true;
                continue;
            }

            if (trimmed.length() == 0) {
                flushParagraph(blocks, paragraph);
                continue;
            }

            // заголовок
            if (trimmed.startsWith("#")) {
                int level = 0;
                while (level < trimmed.length() && trimmed.charAt(level) == '#') {
                    level++;
                }
                if (level <= 4 && level < trimmed.length() && trimmed.charAt(level) == ' ') {
                    flushParagraph(blocks, paragraph);
                    String text = trimmed.substring(level + 1).trim();
                    List<Span> spans = parseInline(text);
                    String plainText = plainOf(spans);
                    blocks.add(new Block(Kind.HEADING, Math.min(3, level), spans, slugify(plainText)));
                    continue;
                }
            }

            // разделитель
            if (trimmed.equals("---") || trimmed.equals("***") || trimmed.equals("___")) {
                flushParagraph(blocks, paragraph);
                blocks.add(new Block(Kind.RULE, 0, Collections.<Span>emptyList(), null));
                continue;
            }

            // таблица
            if (trimmed.startsWith("|")) {
                flushParagraph(blocks, paragraph);
                Block row = parseTableRow(trimmed);
                if (row != null) {
                    blocks.add(row);
                }
                continue;
            }

            // цитата
            if (trimmed.startsWith(">")) {
                flushParagraph(blocks, paragraph);
                String text = trimmed.substring(1).trim();
                blocks.add(new Block(Kind.QUOTE, 0, parseInline(text), null));
                continue;
            }

            // маркированный список (с возможным отступом)
            int indent = countIndent(line);
            if (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.equals("-")) {
                flushParagraph(blocks, paragraph);
                String text = trimmed.length() > 1 ? trimmed.substring(2).trim() : "";
                blocks.add(new Block(Kind.LIST_ITEM, indent >= 2 ? 1 : 0, parseInline(text), null));
                continue;
            }

            // нумерованный список
            int dotPos = numberedPrefixLength(trimmed);
            if (dotPos > 0) {
                flushParagraph(blocks, paragraph);
                String text = trimmed.substring(dotPos).trim();
                blocks.add(new Block(Kind.NUMBERED_ITEM, indent >= 2 ? 1 : 0, parseInline(text), null));
                continue;
            }

            // обычный абзац: склеиваем строки через пробел
            if (paragraph.length() > 0) {
                paragraph.append(' ');
            }
            paragraph.append(trimmed);
        }

        if (inCode && codeBuf.length() > 0) {
            blocks.add(new Block(Kind.CODE, 0, oneSpan(codeBuf.toString()), null));
        }
        flushParagraph(blocks, paragraph);

        return new Page(pageId, title, itemId, category, categoryLabel, tags, blocks);
    }

    private static void flushParagraph(List<Block> blocks, StringBuilder paragraph) {
        if (paragraph.length() > 0) {
            blocks.add(new Block(Kind.PARAGRAPH, 0, parseInline(paragraph.toString()), null));
            paragraph.setLength(0);
        }
    }

    private static String plainOf(List<Span> spans) {
        StringBuilder sb = new StringBuilder();
        for (Span s : spans) {
            sb.append(s.text);
        }
        return sb.toString();
    }

    private static int countIndent(String line) {
        int n = 0;
        while (n < line.length() && (line.charAt(n) == ' ' || line.charAt(n) == '\t')) {
            n++;
        }
        return n;
    }

    /** Длина префикса «12. » или -1, если строка не нумерованная. */
    private static int numberedPrefixLength(String trimmed) {
        int i = 0;
        while (i < trimmed.length() && Character.isDigit(trimmed.charAt(i))) {
            i++;
        }
        if (i > 0 && i < trimmed.length() && trimmed.charAt(i) == '.'
            && (i + 1 >= trimmed.length() || trimmed.charAt(i + 1) == ' ')) {
            return Math.min(trimmed.length(), i + 2);
        }
        return -1;
    }

    private static Block parseTableRow(String trimmed) {
        String body = trimmed;
        if (body.startsWith("|")) {
            body = body.substring(1);
        }
        if (body.endsWith("|")) {
            body = body.substring(0, body.length() - 1);
        }
        String[] cells = body.split("\\|", -1);
        // строка-разделитель вида |---|---|
        boolean separator = true;
        for (String cell : cells) {
            String c = cell.trim().replace(":", "");
            if (c.length() == 0 || !c.replace("-", "").equals("")) {
                separator = false;
                break;
            }
        }
        if (separator) {
            return null;
        }
        List<Span> spans = new ArrayList<Span>();
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                spans.add(new Span(" │ ", false, false, false, null));
            }
            spans.addAll(parseInline(cells[i].trim()));
        }
        return new Block(Kind.TABLE_ROW, 0, spans, null);
    }

    /**
     * Инлайн-разметка: [[a|b]], [[a#b|c]], [[#b|c]], [b](a), **b**, *i*, `c`.
     * Незакрытые конструкции остаются текстом.
     */
    public static List<Span> parseInline(String text) {
        List<Span> out = new ArrayList<Span>();
        if (text == null || text.length() == 0) {
            return out;
        }
        StringBuilder buf = new StringBuilder();
        int i = 0;
        int n = text.length();
        while (i < n) {
            char c = text.charAt(i);

            // [[...]]
            if (c == '[' && i + 1 < n && text.charAt(i + 1) == '[') {
                int end = text.indexOf("]]", i + 2);
                if (end > i + 2) {
                    String inside = text.substring(i + 2, end);
                    String target = inside;
                    String label = inside;
                    int pipe = inside.indexOf('|');
                    if (pipe >= 0) {
                        target = inside.substring(0, pipe);
                        label = inside.substring(pipe + 1);
                    }
                    target = target.trim();
                    label = label.trim();
                    if (label.length() == 0) {
                        label = target;
                    }
                    flushBuf(out, buf);
                    // подпись может содержать **жир** — разбираем безопасно без вложенных ссылок
                    for (Span s : parseSimple(label)) {
                        out.add(new Span(s.text, s.bold, s.italic, s.code, target));
                    }
                    i = end + 2;
                    continue;
                }
            }

            // [label](target)
            if (c == '[') {
                int close = text.indexOf(']', i + 1);
                if (close > i + 1 && close + 1 < n && text.charAt(close + 1) == '(') {
                    int paren = text.indexOf(')', close + 2);
                    if (paren > close + 1) {
                        String label = text.substring(i + 1, close);
                        String target = text.substring(close + 2, paren).trim();
                        flushBuf(out, buf);
                        for (Span s : parseSimple(label)) {
                            out.add(new Span(s.text, s.bold, s.italic, s.code, target));
                        }
                        i = paren + 1;
                        continue;
                    }
                }
            }

            // **bold**
            if (c == '*' && i + 1 < n && text.charAt(i + 1) == '*') {
                int end = text.indexOf("**", i + 2);
                if (end > i + 2) {
                    flushBuf(out, buf);
                    for (Span s : parseInlineNoBold(text.substring(i + 2, end))) {
                        out.add(new Span(s.text, true, s.italic, s.code, s.link));
                    }
                    i = end + 2;
                    continue;
                }
            }

            // *italic* (не путать с умножением: требуются не-пробелы по краям)
            if (c == '*' && i + 1 < n && !Character.isWhitespace(text.charAt(i + 1))) {
                int end = text.indexOf('*', i + 1);
                if (end > i + 1 && !Character.isWhitespace(text.charAt(end - 1))) {
                    flushBuf(out, buf);
                    out.add(new Span(text.substring(i + 1, end), false, true, false, null));
                    i = end + 1;
                    continue;
                }
            }

            // `code`
            if (c == '`') {
                int end = text.indexOf('`', i + 1);
                if (end > i) {
                    flushBuf(out, buf);
                    out.add(new Span(text.substring(i + 1, end), false, false, true, null));
                    i = end + 1;
                    continue;
                }
            }

            buf.append(c);
            i++;
        }
        flushBuf(out, buf);
        return out;
    }

    /** Внутри **...** разрешаем ссылки и код, но не вложенный жир. */
    private static List<Span> parseInlineNoBold(String text) {
        List<Span> out = new ArrayList<Span>();
        StringBuilder buf = new StringBuilder();
        int i = 0;
        int n = text.length();
        while (i < n) {
            char c = text.charAt(i);
            if (c == '[' && i + 1 < n && text.charAt(i + 1) == '[') {
                int end = text.indexOf("]]", i + 2);
                if (end > i + 2) {
                    String inside = text.substring(i + 2, end);
                    String target = inside;
                    String label = inside;
                    int pipe = inside.indexOf('|');
                    if (pipe >= 0) {
                        target = inside.substring(0, pipe).trim();
                        label = inside.substring(pipe + 1).trim();
                    }
                    flushBuf(out, buf);
                    out.add(new Span(label.length() == 0 ? target : label, false, false, false, target));
                    i = end + 2;
                    continue;
                }
            }
            if (c == '`') {
                int end = text.indexOf('`', i + 1);
                if (end > i) {
                    flushBuf(out, buf);
                    out.add(new Span(text.substring(i + 1, end), false, false, true, null));
                    i = end + 1;
                    continue;
                }
            }
            buf.append(c);
            i++;
        }
        flushBuf(out, buf);
        return out;
    }

    /** Подписи ссылок: только **жир** и `код`, никаких вложенных ссылок. */
    private static List<Span> parseSimple(String text) {
        List<Span> out = new ArrayList<Span>();
        StringBuilder buf = new StringBuilder();
        int i = 0;
        int n = text.length();
        while (i < n) {
            char c = text.charAt(i);
            if (c == '*' && i + 1 < n && text.charAt(i + 1) == '*') {
                int end = text.indexOf("**", i + 2);
                if (end > i + 2) {
                    flushBuf(out, buf);
                    out.add(new Span(text.substring(i + 2, end), true, false, false, null));
                    i = end + 2;
                    continue;
                }
            }
            if (c == '`') {
                int end = text.indexOf('`', i + 1);
                if (end > i) {
                    flushBuf(out, buf);
                    out.add(new Span(text.substring(i + 1, end), false, false, true, null));
                    i = end + 1;
                    continue;
                }
            }
            buf.append(c);
            i++;
        }
        flushBuf(out, buf);
        if (out.isEmpty()) {
            out.add(Span.plain(text));
        }
        return out;
    }

    private static void flushBuf(List<Span> out, StringBuilder buf) {
        if (buf.length() > 0) {
            out.add(Span.plain(buf.toString()));
            buf.setLength(0);
        }
    }
}
