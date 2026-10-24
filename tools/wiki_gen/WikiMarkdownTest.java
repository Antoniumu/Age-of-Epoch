import com.robertx22.age_of_exile.wiki.WikiMarkdown;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Автономный тест парсера WikiMarkdown без Minecraft.
 *
 * Запуск из корня репозитория (нужен любой JDK 17+):
 *   mkdir -p /tmp/wiki-test
 *   javac -encoding UTF-8 -d /tmp/wiki-test \
 *     src/main/java/com/robertx22/age_of_exile/wiki/WikiMarkdown.java \
 *     tools/wiki_gen/WikiMarkdownTest.java
 *   java -cp /tmp/wiki-test WikiMarkdownTest src/main/resources/assets/mmorpg/wiki_pages
 *
 * Проверяет: парсер не падает, frontmatter читается, все [[ссылки]]
 * и якоря разрешаются через сам парсер, видимый текст каждой страницы
 * не короче 1000 символов.
 */
public final class WikiMarkdownTest {

    public static void main(String[] args) throws Exception {
        Path root = Paths.get(args.length > 0 ? args[0] : "src/main/resources/assets/mmorpg/wiki_pages");
        List<Path> files = new ArrayList<Path>();
        collect(root.toFile(), files);
        if (files.isEmpty()) {
            throw new IllegalStateException("Нет .md файлов в " + root);
        }

        Map<String, WikiMarkdown.Page> pages = new HashMap<String, WikiMarkdown.Page>();
        for (Path file : files) {
            String rel = root.relativize(file).toString().replace(File.separatorChar, '/');
            String id = rel.substring(0, rel.length() - ".md".length());
            String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            WikiMarkdown.Page page = WikiMarkdown.parse(id, text);
            pages.put(id, page);
            check(page.title != null && page.title.length() > 0, id + ": пустой заголовок");
            check(!page.blocks.isEmpty(), id + ": нет блоков");
            int visible = 0;
            for (WikiMarkdown.Block block : page.blocks) {
                visible += block.plain.trim().length() > 0 ? block.plain.trim().length() + 1 : 0;
            }
            check(visible >= 1000, id + ": видимый текст короче 1000: " + visible);
        }

        int linkCount = 0;
        for (WikiMarkdown.Page page : pages.values()) {
            for (WikiMarkdown.Block block : page.blocks) {
                for (WikiMarkdown.Span span : block.spans) {
                    if (span.link == null) {
                        continue;
                    }
                    linkCount++;
                    String target = span.link;
                    String anchor = null;
                    int hash = target.indexOf('#');
                    if (hash >= 0) {
                        anchor = target.substring(hash + 1);
                        target = target.substring(0, hash);
                    }
                    WikiMarkdown.Page dest = target.length() == 0 ? page : pages.get(target);
                    check(dest != null, page.id + ": битая ссылка " + span.link);
                    if (anchor != null && anchor.length() > 0) {
                        check(dest.hasAnchor(WikiMarkdown.slugify(anchor)),
                            page.id + ": нет якоря " + anchor + " на странице " + dest.id);
                    }
                }
            }
        }

        // пара прямых проверок диалекта
        WikiMarkdown.Page probe = WikiMarkdown.parse("probe",
            "---\ntitle: Проверка\nitem: mmorpg:gems/ruby/6\n---\n# Проверка\n\n" +
                "Текст с [[guides/damage|ссылкой]], **жиром**, *курсивом* и `кодом`.\n\n- пункт [[#проверка|наверх]]\n");
        check("Проверка".equals(probe.title), "probe: title");
        check("mmorpg:gems/ruby/6".equals(probe.itemId), "probe: item");
        check(probe.hasAnchor(WikiMarkdown.slugify("Проверка")), "probe: anchor");
        check(probe.links.contains("guides/damage"), "probe: links");
        // битый вход не должен ронять парсер
        WikiMarkdown.parse("bad", "[[незакрыто **тоже `и так далее");
        WikiMarkdown.parse("bad2", null);

        System.out.println("OK: страниц " + pages.size() + ", ссылок проверено " + linkCount);
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

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException("FAIL: " + message);
        }
    }
}
