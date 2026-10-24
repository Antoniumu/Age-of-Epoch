package com.robertx22.age_of_exile.wiki;

import com.robertx22.age_of_exile.mmorpg.SlashRef;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Каталог страниц вики: читает все `assets/mmorpg/wiki_pages/*.md` из
 * ресурс-пака при первом открытии экрана и держит распарсенные страницы.
 *
 * <p>Защитный режим: любая ошибка чтения отдельной страницы проглатывает
 * только её, а если каталог в целом недоступен, подставляется одна
 * страница-объяснение — игра никогда не падает из-за вики.
 */
public final class WikiCatalog {

    public static final String HOME_ID = "index";
    private static final String PREFIX = "wiki_pages/";

    private static boolean loaded = false;
    private static Map<String, WikiMarkdown.Page> pages = new TreeMap<String, WikiMarkdown.Page>();
    private static List<WikiMarkdown.Page> sorted = new ArrayList<WikiMarkdown.Page>();

    private WikiCatalog() {
    }

    public static synchronized void ensureLoaded() {
        if (loaded && !pages.isEmpty()) {
            return;
        }
        reload();
    }

    /** Перечитать страницы из ресурсов (например, после пересборки каталога). */
    public static synchronized void reload() {
        Map<String, WikiMarkdown.Page> next = new TreeMap<String, WikiMarkdown.Page>();
        try {
            ResourceManager manager = Minecraft.getInstance().getResourceManager();
            Map<Identifier, Resource> resources = manager.listResources("wiki_pages",
                id -> id.getPath().endsWith(".md"));
            for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
                Identifier id = entry.getKey();
                if (!SlashRef.MODID.equals(id.getNamespace())) {
                    continue;
                }
                String path = id.getPath();
                if (!path.startsWith(PREFIX) || !path.endsWith(".md")) {
                    continue;
                }
                String pageId = path.substring(PREFIX.length(), path.length() - ".md".length());
                try {
                    String text = readAll(entry.getValue());
                    next.put(pageId, WikiMarkdown.parse(pageId, text));
                } catch (Throwable ignored) {
                    // одна битая страница не должна губить весь каталог
                }
            }
        } catch (Throwable ignored) {
            // каталог останется пустым — ниже подставим объяснение
        }

        if (next.isEmpty()) {
            WikiMarkdown.Page fallback = WikiMarkdown.parse(HOME_ID,
                "---\ntitle: Вики Эпохи\ncategory: home\n---\n# Вики Эпохи\n\n" +
                    "Страницы вики не нашлись в ресурсах мода (`assets/mmorpg/wiki_pages`). " +
                    "Каталог генерируется командой `python3 tools/wiki_gen/generate_wiki.py` из репозитория мода — " +
                    "см. WIKI.md. Игра при этом работает как обычно: это сообщение показывает только вики.");
            next.put(HOME_ID, fallback);
        }

        pages = next;
        sorted = new ArrayList<WikiMarkdown.Page>(next.values());
        Collections.sort(sorted, new Comparator<WikiMarkdown.Page>() {
            @Override
            public int compare(WikiMarkdown.Page a, WikiMarkdown.Page b) {
                int byRank = categoryRank(a) - categoryRank(b);
                if (byRank != 0) {
                    return byRank;
                }
                return a.title.compareToIgnoreCase(b.title);
            }
        });
        loaded = true;
    }

    private static int categoryRank(WikiMarkdown.Page page) {
        if (HOME_ID.equals(page.id)) {
            return 0;
        }
        if ("guides".equals(page.category)) {
            return 1;
        }
        if ("categories".equals(page.category)) {
            return 2;
        }
        return 3;
    }

    private static String readAll(Resource resource) throws Exception {
        InputStream in = resource.open();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[4096];
            int read;
            while ((read = reader.read(buf)) != -1) {
                sb.append(buf, 0, read);
            }
            return sb.toString();
        } finally {
            in.close();
        }
    }

    public static synchronized WikiMarkdown.Page page(String id) {
        ensureLoaded();
        if (id == null || id.length() == 0) {
            return pages.get(HOME_ID);
        }
        WikiMarkdown.Page page = pages.get(id);
        return page != null ? page : pages.get(HOME_ID);
    }

    public static synchronized boolean exists(String id) {
        ensureLoaded();
        return id != null && pages.containsKey(id);
    }

    public static synchronized List<WikiMarkdown.Page> all() {
        ensureLoaded();
        return sorted;
    }

    /** Поиск: совпадения в названии раньше, дальше — в тегах и тексте. Пустой запрос = весь каталог. */
    public static synchronized List<WikiMarkdown.Page> search(String query) {
        ensureLoaded();
        if (query == null || query.trim().length() == 0) {
            return sorted;
        }
        String q = query.trim().toLowerCase(Locale.ROOT);
        List<WikiMarkdown.Page> titleHits = new ArrayList<WikiMarkdown.Page>();
        List<WikiMarkdown.Page> textHits = new ArrayList<WikiMarkdown.Page>();
        for (WikiMarkdown.Page page : sorted) {
            if (page.title.toLowerCase(Locale.ROOT).contains(q) || page.id.contains(q)
                || (page.itemId != null && page.itemId.contains(q))) {
                titleHits.add(page);
            } else if (page.searchText.contains(q)) {
                textHits.add(page);
            }
        }
        titleHits.addAll(textHits);
        return titleHits;
    }
}
