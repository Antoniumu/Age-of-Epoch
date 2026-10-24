package com.robertx22.age_of_exile.wikibook;

import java.util.ArrayList;
import java.util.List;

/**
 * Мини-парсер Markdown для внутриигровой вики.
 *
 * Поддерживает ровно то, что нужно каталогу, и ничего лишнего:
 * заголовки (#, ##, ###), абзацы, списки (- и цифры), цитаты (>),
 * таблицы (| a | b |), разделители (---) и инлайн-разметку:
 * **жирный**, *курсив*, `код`, [текст](страница), [[страница]], [[страница|текст]].
 *
 * Парсер намеренно «терпимый»: любая незнакомая конструкция остаётся обычным текстом,
 * поэтому битый .md-файл не ломает экран, а просто выглядит чуть хуже.
 */
public final class WikiMarkdown {

    private WikiMarkdown() {
    }

    // ── палитра ────────────────────────────────────────────────────────────────
    public static final int C_TEXT = 0xD8E3F2;
    public static final int C_DIM = 0x93A3BA;
    public static final int C_MUTED = 0x66748A;
    public static final int C_LINK = 0x74D2FF;
    public static final int C_LINK_HOVER = 0xFFE08A;
    public static final int C_ACCENT = 0x52C7FF;
    public static final int C_GOLD = 0xFFC857;
    public static final int C_CODE = 0xE7C67A;
    public static final int C_QUOTE = 0xA9C8DC;
    public static final int C_H1 = 0xFFE3A3;
    public static final int C_H2 = 0x8FE1FF;
    public static final int C_H3 = 0xC9D6E8;

    public enum Kind {
        HEADING,
        PARAGRAPH,
        BULLET,
        QUOTE,
        TABLE,
        RULE
    }

    /** Кусок текста с оформлением. link == null — обычный текст. */
    public record Span(String text, int color, boolean bold, boolean italic, String link) {

        public static Span plain(String text, int color) {
            return new Span(text, color, false, false, null);
        }

        public boolean isLink() {
            return link != null && !link.isEmpty();
        }
    }

    /** Блок страницы. Для TABLE: rows.get(0) — шапка. */
    public record Block(Kind kind, int level, List<Span> spans, List<List<String>> rows) {

        public static Block heading(int level, List<Span> spans) {
            return new Block(Kind.HEADING, level, spans, List.of());
        }

        public static Block text(Kind kind, List<Span> spans) {
            return new Block(kind, 0, spans, List.of());
        }

        public static Block table(List<List<String>> rows) {
            return new Block(Kind.TABLE, 0, List.of(), rows);
        }

        public static Block rule() {
            return new Block(Kind.RULE, 0, List.of(), List.of());
        }
    }

    // ── разбор блоков ──────────────────────────────────────────────────────────

    public static List<Block> parseBlocks(String body) {
        List<Block> blocks = new ArrayList<>();
        if (body == null) {
            return blocks;
        }
        String[] lines = body.replace("\r", "").split("\n", -1);

        StringBuilder paragraph = new StringBuilder();
        List<List<String>> table = new ArrayList<>();

        for (String rawLine : lines) {
            String line = rawLine.trim();

            if (line.startsWith("|")) {
                List<String> cells = splitRow(line);
                if (cells != null) {
                    table.add(cells);
                }
                continue;
            }
            if (!table.isEmpty()) {
                blocks.add(Block.table(table));
                table = new ArrayList<>();
            }

            if (line.isEmpty()) {
                flushParagraph(blocks, paragraph);
                continue;
            }

            if (line.equals("---") || line.equals("***") || line.equals("___")) {
                flushParagraph(blocks, paragraph);
                blocks.add(Block.rule());
                continue;
            }

            if (line.startsWith("### ")) {
                flushParagraph(blocks, paragraph);
                blocks.add(Block.heading(3, inline(line.substring(4), false, false)));
                continue;
            }
            if (line.startsWith("## ")) {
                flushParagraph(blocks, paragraph);
                blocks.add(Block.heading(2, inline(line.substring(3), false, false)));
                continue;
            }
            if (line.startsWith("# ")) {
                flushParagraph(blocks, paragraph);
                blocks.add(Block.heading(1, inline(line.substring(2), false, false)));
                continue;
            }
            if (line.startsWith("> ")) {
                flushParagraph(blocks, paragraph);
                blocks.add(Block.text(Kind.QUOTE, inline(line.substring(2), false, false)));
                continue;
            }
            if (line.startsWith("- ") || line.startsWith("* ") || line.startsWith("+ ")) {
                flushParagraph(blocks, paragraph);
                blocks.add(new Block(Kind.BULLET, 0, inline(line.substring(2), false, false), List.of()));
                continue;
            }
            if (isNumbered(line)) {
                flushParagraph(blocks, paragraph);
                blocks.add(new Block(Kind.BULLET, 1, inline(line, false, false), List.of()));
                continue;
            }

            if (paragraph.length() > 0) {
                paragraph.append(' ');
            }
            paragraph.append(line);
        }

        if (!table.isEmpty()) {
            blocks.add(Block.table(table));
        }
        flushParagraph(blocks, paragraph);
        return blocks;
    }

    private static void flushParagraph(List<Block> blocks, StringBuilder paragraph) {
        if (paragraph.length() == 0) {
            return;
        }
        String text = paragraph.toString().trim();
        paragraph.setLength(0);
        if (!text.isEmpty()) {
            blocks.add(Block.text(Kind.PARAGRAPH, inline(text, false, false)));
        }
    }

    private static boolean isNumbered(String line) {
        int dot = line.indexOf(". ");
        if (dot <= 0 || dot > 3) {
            return false;
        }
        for (int i = 0; i < dot; i++) {
            if (!Character.isDigit(line.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /** Возвращает null для строки-разделителя таблицы (|---|---|). */
    private static List<String> splitRow(String line) {
        String trimmed = line;
        if (trimmed.startsWith("|")) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.endsWith("|")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        List<String> cells = new ArrayList<>();
        boolean separator = true;
        for (String part : trimmed.split("\\|", -1)) {
            String cell = part.trim();
            if (!cell.isEmpty() && !cell.chars().allMatch(c -> c == '-' || c == ':' || c == ' ')) {
                separator = false;
            }
            cells.add(cell);
        }
        return separator ? null : cells;
    }

    // ── разбор инлайн-разметки ────────────────────────────────────────────────

    public static List<Span> inline(String source, boolean bold, boolean italic) {
        List<Span> out = new ArrayList<>();
        if (source == null) {
            return out;
        }
        StringBuilder buffer = new StringBuilder();
        int i = 0;
        while (i < source.length()) {
            char c = source.charAt(i);

            if (source.startsWith("[[", i)) {
                int end = source.indexOf("]]", i + 2);
                if (end > i) {
                    String inner = source.substring(i + 2, end);
                    String target = inner;
                    String label = inner;
                    int bar = inner.indexOf('|');
                    if (bar >= 0) {
                        target = inner.substring(0, bar);
                        label = inner.substring(bar + 1);
                    }
                    flush(out, buffer, bold, italic, null);
                    out.add(new Span(label.trim(), C_LINK, bold, italic, normalize(target)));
                    i = end + 2;
                    continue;
                }
            }

            if (c == '[') {
                int close = source.indexOf(']', i);
                if (close > i) {
                    boolean paren = close + 1 < source.length() && source.charAt(close + 1) == '(';
                    int end = paren ? source.indexOf(')', close) : -1;
                    if (paren && end > close) {
                        String label = source.substring(i + 1, close);
                        String target = source.substring(close + 2, end);
                        flush(out, buffer, bold, italic, null);
                        out.add(new Span(label.trim(), C_LINK, bold, italic, normalize(target)));
                        i = end + 1;
                        continue;
                    }
                }
            }

            if (source.startsWith("**", i)) {
                int end = source.indexOf("**", i + 2);
                if (end > i) {
                    flush(out, buffer, bold, italic, null);
                    out.addAll(inline(source.substring(i + 2, end), true, italic));
                    i = end + 2;
                    continue;
                }
            }

            if (c == '`') {
                int end = source.indexOf('`', i + 1);
                if (end > i) {
                    flush(out, buffer, bold, italic, null);
                    out.add(new Span(source.substring(i + 1, end), C_CODE, bold, italic, null));
                    i = end + 1;
                    continue;
                }
            }

            if (c == '*' && !source.startsWith("**", i)) {
                int end = source.indexOf('*', i + 1);
                if (end > i + 1) {
                    flush(out, buffer, bold, italic, null);
                    out.addAll(inline(source.substring(i + 1, end), bold, true));
                    i = end + 1;
                    continue;
                }
            }

            if (c == '§') {
                // Старые текстовые файлы могли содержать §-коды — выкидываем их вместе с кодом.
                i += 2;
                continue;
            }

            buffer.append(c);
            i++;
        }
        flush(out, buffer, bold, italic, null);
        return out;
    }

    private static void flush(List<Span> out, StringBuilder buffer, boolean bold, boolean italic, String link) {
        if (buffer.length() == 0) {
            return;
        }
        String text = buffer.toString();
        buffer.setLength(0);
        if (!text.isEmpty()) {
            out.add(new Span(text, C_TEXT, bold, italic, link));
        }
    }

    public static String normalize(String target) {
        if (target == null) {
            return "";
        }
        String t = target.trim().toLowerCase(java.util.Locale.ROOT);
        if (t.endsWith(".md")) {
            t = t.substring(0, t.length() - 3);
        }
        while (t.startsWith("./")) {
            t = t.substring(2);
        }
        if (t.startsWith("/")) {
            t = t.substring(1);
        }
        return t.replace(' ', '-');
    }

    public static String plainOf(List<Span> spans) {
        StringBuilder sb = new StringBuilder();
        for (Span s : spans) {
            sb.append(s.text());
        }
        return sb.toString();
    }
}
