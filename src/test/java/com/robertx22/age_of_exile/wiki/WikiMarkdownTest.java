package com.robertx22.age_of_exile.wiki;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тесты парсера вики. Запускаются обычным {@code ./gradlew test} (и, значит,
 * {@code ./gradlew build}) — до этого парсер на 600 строк не проверялся ни разу
 * ни в одной сборке: standalone-тест {@code tools/wiki_gen/WikiMarkdownTest.java}
 * нужно было запускать руками, и в CI он не попадал.
 *
 * <p>Проверяется и сам парсер (юнит-кейсы на разметку и на битый ввод), и
 * реальный каталог страниц: все 371 страница парсятся, ссылки и якоря
 * разрешаются, якоря не дублируются, каждая страница достаточно длинная.
 */
public class WikiMarkdownTest {

    private static final Path WIKI = Paths.get("src/main/resources/assets/mmorpg/wiki_pages");
    private static final int MIN_VISIBLE = 1000;

    private static final Pattern LINK = Pattern.compile("\\[\\[([^\\]|#]+)(?:#([^\\]|]+))?(?:\\|[^\\]]*)?\\]\\]");

    // ---------------------------------------------------------------- каталог

    private static Map<String, String> readPages() throws Exception {
        Map<String, String> pages = new TreeMap<String, String>();
        if (!Files.isDirectory(WIKI)) {
            return pages;
        }
        List<Path> files = new ArrayList<Path>();
        collect(WIKI.toFile(), files);
        for (Path file : files) {
            String rel = WIKI.relativize(file).toString().replace(File.separatorChar, '/');
            String id = rel.substring(0, rel.length() - ".md".length());
            pages.put(id, new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
        }
        return pages;
    }

    private static void collect(File dir, List<Path> out) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                collect(child, out);
            } else if (child.getName().endsWith(".md")) {
                out.add(child.toPath());
            }
        }
    }

    private static int visibleChars(WikiMarkdown.Page page) {
        int visible = 0;
        for (WikiMarkdown.Block block : page.blocks) {
            String plain = block.plain.trim();
            if (plain.length() > 0) {
                visible += plain.length() + 1;
            }
        }
        return visible;
    }

    @Test
    @DisplayName("каталог вики на месте и не пустой")
    public void catalogExists() throws Exception {
        Map<String, String> pages = readPages();
        assertTrue(pages.size() > 100, "страниц вики слишком мало: " + pages.size());
        assertTrue(pages.containsKey("index"), "нет главной страницы index.md");
    }

    @Test
    @DisplayName("каждая страница парсится: заголовок, блоки, >=1000 видимых символов")
    public void everyPageParses() throws Exception {
        List<String> problems = new ArrayList<String>();
        for (Map.Entry<String, String> entry : readPages().entrySet()) {
            WikiMarkdown.Page page = WikiMarkdown.parse(entry.getKey(), entry.getValue());
            if (page.title == null || page.title.trim().length() == 0) {
                problems.add(entry.getKey() + ": пустой title");
            }
            if (page.blocks.isEmpty()) {
                problems.add(entry.getKey() + ": нет блоков");
            }
            int visible = visibleChars(page);
            if (visible < MIN_VISIBLE) {
                problems.add(entry.getKey() + ": видимого текста " + visible + " < " + MIN_VISIBLE);
            }
        }
        assertTrue(problems.isEmpty(), "проблемные страницы:\n  " + String.join("\n  ", problems));
    }

    @Test
    @DisplayName("все [[ссылки]] ведут на существующие страницы, якоря существуют")
    public void linksResolve() throws Exception {
        Map<String, String> raw = readPages();
        Map<String, WikiMarkdown.Page> pages = new TreeMap<String, WikiMarkdown.Page>();
        for (Map.Entry<String, String> entry : raw.entrySet()) {
            pages.put(entry.getKey(), WikiMarkdown.parse(entry.getKey(), entry.getValue()));
        }

        List<String> problems = new ArrayList<String>();
        for (Map.Entry<String, String> entry : raw.entrySet()) {
            String id = entry.getKey();
            String body = bodyOf(entry.getValue());
            Matcher matcher = LINK.matcher(body);
            while (matcher.find()) {
                String target = matcher.group(1).trim();
                String anchor = matcher.group(2) == null ? null : matcher.group(2).trim();
                if (!pages.containsKey(target)) {
                    problems.add(id + ": битая ссылка [[" + target + "]]");
                    continue;
                }
                if (anchor != null && anchor.length() > 0
                    && !pages.get(target).hasAnchor(WikiMarkdown.slugify(anchor))) {
                    problems.add(id + ": нет якоря #" + anchor + " на [[" + target + "]]");
                }
            }
        }
        assertTrue(problems.isEmpty(), "битые ссылки:\n  " + String.join("\n  ", problems));
    }

    @Test
    @DisplayName("якоря заголовков не дублируются внутри страницы")
    public void anchorsAreUnique() throws Exception {
        List<String> problems = new ArrayList<String>();
        for (Map.Entry<String, String> entry : readPages().entrySet()) {
            Set<String> seen = new HashSet<String>();
            for (WikiMarkdown.Block block : WikiMarkdown.parse(entry.getKey(), entry.getValue()).blocks) {
                if (block.kind != WikiMarkdown.Kind.HEADING || block.anchor == null) {
                    continue;
                }
                if (!seen.add(block.anchor)) {
                    problems.add(entry.getKey() + ": якорь #" + block.anchor + " повторяется");
                }
            }
        }
        assertTrue(problems.isEmpty(), "дубли якорей:\n  " + String.join("\n  ", problems));
    }

    @Test
    @DisplayName("каждая страница связана с главной через категорию или прямую ссылку")
    public void everyPageIsReachable() throws Exception {
        Map<String, WikiMarkdown.Page> pages = new TreeMap<String, WikiMarkdown.Page>();
        for (Map.Entry<String, String> entry : readPages().entrySet()) {
            pages.put(entry.getKey(), WikiMarkdown.parse(entry.getKey(), entry.getValue()));
        }
        Set<String> linkedFromHome = new HashSet<String>();
        Matcher home = LINK.matcher(bodyOf(readPages().get("index")));
        while (home.find()) {
            linkedFromHome.add(home.group(1).trim());
        }
        List<String> orphans = new ArrayList<String>();
        for (Map.Entry<String, WikiMarkdown.Page> entry : pages.entrySet()) {
            String id = entry.getKey();
            if ("index".equals(id) || linkedFromHome.contains(id)) {
                continue;
            }
            String category = entry.getValue().category;
            if (category == null || category.length() == 0) {
                orphans.add(id + ": нет category и нет ссылки с главной");
            } else if (!pages.containsKey("categories/" + category) && !"categories".equals(category)
                && !"guides".equals(category) && !"home".equals(category)) {
                orphans.add(id + ": категория '" + category + "' не имеет своей страницы");
            }
        }
        assertTrue(orphans.isEmpty(), "страницы вне навигации:\n  " + String.join("\n  ", orphans));
    }

    // ---------------------------------------------------------------- парсер

    @Test
    @DisplayName("frontmatter читается, тело начинается после него")
    public void frontmatterParsed() {
        WikiMarkdown.Page page = WikiMarkdown.parse("items/test",
            "---\ntitle: Тестовый предмет\nitem: mmorpg:test\ncategory: misc\ntags: a, b\n---\n# Заголовок\n\nТекст страницы.\n");
        assertEquals("Тестовый предмет", page.title);
        assertEquals("mmorpg:test", page.itemId);
        assertEquals("misc", page.category);
        boolean hasBody = false;
        for (WikiMarkdown.Block block : page.blocks) {
            if (block.plain.contains("Текст страницы")) {
                hasBody = true;
            }
        }
        assertTrue(hasBody, "тело страницы не распарсилось");
    }

    @Test
    @DisplayName("инлайн-разметка: жирный, курсив, код, ссылка")
    public void inlineMarkup() {
        WikiMarkdown.Page page = WikiMarkdown.parse("p",
            "---\ntitle: P\ncategory: misc\n---\n**жирный** *курсив* `код` [[items/x|подпись]]\n");
        boolean bold = false, italic = false, code = false, link = false;
        for (WikiMarkdown.Block block : page.blocks) {
            for (WikiMarkdown.Span span : block.spans) {
                bold |= span.bold;
                italic |= span.italic;
                code |= span.code;
                if ("items/x".equals(span.link)) {
                    link = true;
                }
            }
        }
        assertTrue(bold, "**жирный** не распознан");
        assertTrue(italic, "*курсив* не распознан");
        assertTrue(code, "`код` не распознан");
        assertTrue(link, "[[ссылка]] не распознана");
    }

    @Test
    @DisplayName("блочная разметка: списки, цитаты, таблицы, код-блоки")
    public void blockMarkup() {
        WikiMarkdown.Page page = WikiMarkdown.parse("p",
            "---\ntitle: P\ncategory: misc\n---\n- пункт\n1. шаг\n> цитата\n| a | b |\n```\nкод\n```\n---\n");
        Set<WikiMarkdown.Kind> kinds = new HashSet<WikiMarkdown.Kind>();
        for (WikiMarkdown.Block block : page.blocks) {
            kinds.add(block.kind);
        }
        assertTrue(kinds.contains(WikiMarkdown.Kind.LIST_ITEM), "список не распознан");
        assertTrue(kinds.contains(WikiMarkdown.Kind.NUMBERED_ITEM), "нумерованный список не распознан");
        assertTrue(kinds.contains(WikiMarkdown.Kind.QUOTE), "цитата не распознана");
        assertTrue(kinds.contains(WikiMarkdown.Kind.TABLE_ROW), "таблица не распознана");
        assertTrue(kinds.contains(WikiMarkdown.Kind.CODE), "код-блок не распознан");
        assertTrue(kinds.contains(WikiMarkdown.Kind.RULE), "разделитель не распознан");
    }

    @Test
    @DisplayName("битый ввод не бросает исключений и остаётся текстом")
    public void brokenInputNeverThrows() {
        String[] broken = {
            "", "---", "---\ntitle:", "# ", "[[", "[[|]]", "[[#]]", "**", "`", "```",
            "---\ntitle: x\n---\n# Заголовок с  двойным  пробелом\n",
            "---\r\ntitle: crlf\r\ncategory: misc\r\n---\r\n# T\r\n\r\nТекст\r\n",
            "---\ntitle: no closing block\n# без закрывающего ---\n"
        };
        for (String input : broken) {
            WikiMarkdown.Page page = WikiMarkdown.parse("id", input);
            assertNotNull(page, "null на входе: " + input);
            assertNotNull(page.blocks, "null blocks на входе: " + input);
            assertNotNull(page.title, "null title на входе: " + input);
        }
    }

    @Test
    @DisplayName("slugify совпадает с проверкой tools/wiki_gen/verify_wiki.py")
    public void slugifyMatchesPythonVerifier() {
        // verify_wiki.py: re.sub(r"\s+", "-", text.lower()).strip("-")
        String[][] cases = {
            {"Что это такое", "что-это-такое"},
            {"  Пробелы  по  краям  ", "пробелы--по-краям"},
            {"Ёлка и йод", "ёлка-и-йод"},
            {"Смешанный\tТаб", "смешанный-таб"},
            {"", ""},
            {"---", "---"},
        };
        for (String[] c : cases) {
            assertEquals(c[1], WikiMarkdown.slugify(c[0]), "slugify(" + c[0] + ")");
        }
    }

    @Test
    @DisplayName("поисковый текст: нижний регистр, включает теги, id и текст страницы")
    public void searchTextIsLowercaseAndComplete() {
        WikiMarkdown.Page page = WikiMarkdown.parse("items/x",
            "---\ntitle: Кристалл очищения\nitem: mmorpg:currency/crystal_of_purification\ncategory: currency\n"
                + "tags: Кристалл, очищение\n---\n# Кристалл\n\nОчищает предмет.\n");
        assertEquals("Кристалл очищения", page.title);
        assertEquals("mmorpg:currency/crystal_of_purification", page.itemId);
        assertEquals(page.searchText.toLowerCase(Locale.ROOT), page.searchText);
        assertTrue(page.searchText.contains("кристалл"), "в поиске нет названия");
        assertTrue(page.searchText.contains("mmorpg:currency/crystal_of_purification"), "в поиске нет id предмета");
        assertTrue(page.searchText.contains("очищает"), "в поиске нет текста страницы");
    }

    @Test
    @DisplayName("длинные идентификаторы в тексте не ломают парсер и попадают в plain")
    public void longTokensSurvive() {
        String token = "mmorpg:currency/reroll_primary_stats_numbers";
        WikiMarkdown.Page page = WikiMarkdown.parse("items/y",
            "---\ntitle: Y\ncategory: currency\n---\n# Y\n\nID: `" + token + "` в тексте.\n");
        boolean found = false;
        for (WikiMarkdown.Block block : page.blocks) {
            if (block.plain.contains(token)) {
                found = true;
                break;
            }
        }
        assertTrue(found, "длинный идентификатор потерялся при разборе");
    }
}
