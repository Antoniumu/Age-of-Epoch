package com.robertx22.age_of_exile.wikibook;

import com.robertx22.age_of_exile.mmorpg.SlashRef;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Каталог вики: читает assets/mmorpg/wiki/index.txt (манифест) и все .md-страницы
 * из assets/mmorpg/wiki/ru/. Загружается один раз на клиенте, дальше всё в памяти.
 *
 * Никаких падений: любая ошибка чтения превращается в одну страницу-заглушку
 * с текстом ошибки, чтобы игрок видел проблему, а не краш.
 */
public final class WikiLibrary {

    public static final String MANIFEST = "wiki/index.txt";
    public static final String PAGES_DIR = "wiki/ru/";

    public record Category(String id, String title, List<WikiPage> pages) {
    }

    private static WikiLibrary instance;

    public final List<Category> categories;
    public final List<WikiPage> pages;
    public final Map<String, WikiPage> byId;
    public final Map<String, List<WikiPage>> backlinks;
    public final String error;
    public final String homeId;

    public static WikiLibrary get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    public static void invalidate() {
        instance = null;
    }

    private WikiLibrary(List<Category> categories, List<WikiPage> pages, Map<String, WikiPage> byId,
                        Map<String, List<WikiPage>> backlinks, String error, String homeId) {
        this.categories = List.copyOf(categories);
        this.pages = List.copyOf(pages);
        this.byId = Map.copyOf(byId);
        this.backlinks = Map.copyOf(backlinks);
        this.error = error;
        this.homeId = homeId;
    }

    // ── загрузка ──────────────────────────────────────────────────────────────

    private static WikiLibrary load() {
        List<Category> categories = new ArrayList<>();
        List<WikiPage> pages = new ArrayList<>();
        Map<String, WikiPage> byId = new LinkedHashMap<>();
        String error = "";
        String homeId = "";

        try {
            ResourceManager rm = Minecraft.getInstance().getResourceManager();
            String manifest = read(rm, MANIFEST);
            if (manifest == null) {
                error = "Не найден манифест " + MANIFEST;
            } else {
                String currentCat = "general";
                String currentTitle = "Общее";
                List<WikiPage> currentPages = new ArrayList<>();

                for (String rawLine : manifest.replace("\r", "").split("\n")) {
                    String line = rawLine.trim();
                    if (line.isEmpty() || line.startsWith(";;")) {
                        continue;
                    }
                    if (line.startsWith("# category")) {
                        if (!currentPages.isEmpty()) {
                            categories.add(new Category(currentCat, currentTitle, List.copyOf(currentPages)));
                            currentPages = new ArrayList<>();
                        }
                        String rest = line.substring("# category".length()).trim();
                        int space = rest.indexOf(' ');
                        currentCat = space < 0 ? rest : rest.substring(0, space);
                        currentTitle = space < 0 ? rest : rest.substring(space + 1).trim();
                        continue;
                    }
                    if (line.startsWith("# home")) {
                        homeId = line.substring("# home".length()).trim();
                        continue;
                    }
                    if (line.startsWith("#")) {
                        continue;
                    }

                    String pageId = WikiMarkdown.normalize(line);
                    String raw = read(rm, PAGES_DIR + pageId + ".md");
                    if (raw == null) {
                        if (error.isEmpty()) {
                            error = "Нет файла " + PAGES_DIR + pageId + ".md";
                        }
                        continue;
                    }
                    WikiPage page = WikiPage.parse(pageId, raw);
                    pages.add(page);
                    byId.put(pageId, page);
                    currentPages.add(page);
                }
                if (!currentPages.isEmpty()) {
                    categories.add(new Category(currentCat, currentTitle, List.copyOf(currentPages)));
                }
                if (pages.isEmpty() && error.isEmpty()) {
                    error = "Манифест пуст";
                }
            }
        } catch (Throwable t) {
            error = "Ошибка чтения вики: " + t.getClass().getSimpleName() + ": " + t.getMessage();
        }

        if (pages.isEmpty()) {
            WikiPage fallback = WikiPage.parse("error",
                "@title Вики недоступна\n@category Ошибка\n\n"
                    + "Каталог вики не загрузился. Это не краш: экран продолжает работать, "
                    + "но текста нет.\n\n"
                    + "Причина: " + (error.isEmpty() ? "неизвестна" : error) + "\n\n"
                    + "Проверь, что в сборке есть assets/mmorpg/wiki/index.txt и папка wiki/ru. "
                    + "Если файлы на месте — напиши в баг-репорт строку выше.");
            categories.add(new Category("error", "Ошибка", List.of(fallback)));
            pages.add(fallback);
            byId.put("error", fallback);
        }

        Map<String, List<WikiPage>> backlinks = new LinkedHashMap<>();
        for (WikiPage page : pages) {
            for (String target : page.links) {
                if (!byId.containsKey(target)) {
                    continue;
                }
                backlinks.computeIfAbsent(target, k -> new ArrayList<>()).add(page);
            }
        }

        String home = homeId.isEmpty() ? pages.get(0).id : homeId;
        if (!byId.containsKey(home)) {
            home = pages.get(0).id;
        }
        return new WikiLibrary(categories, pages, byId, backlinks, error, home);
    }

    private static String read(ResourceManager rm, String path) {
        try {
            Identifier id = Identifier.fromNamespaceAndPath(SlashRef.MODID, path);
            Optional<Resource> resource = rm.getResource(id);
            if (resource == null || resource.isEmpty()) {
                return null;
            }
            try (Reader reader = resource.get().openAsReader()) {
                StringBuilder sb = new StringBuilder();
                char[] buffer = new char[4096];
                int read;
                while ((read = reader.read(buffer)) >= 0) {
                    sb.append(buffer, 0, read);
                }
                return sb.toString();
            }
        } catch (Throwable t) {
            return null;
        }
    }

    // ── запросы ───────────────────────────────────────────────────────────────

    public WikiPage page(String id) {
        return id == null ? null : byId.get(WikiMarkdown.normalize(id));
    }

    public WikiPage home() {
        WikiPage page = byId.get(homeId);
        return page != null ? page : pages.get(0);
    }

    public List<WikiPage> backlinksOf(String id) {
        List<WikiPage> list = backlinks.get(id);
        return list == null ? List.of() : list;
    }

    public Category categoryOf(WikiPage page) {
        for (Category category : categories) {
            for (WikiPage candidate : category.pages()) {
                if (candidate.id.equals(page.id)) {
                    return category;
                }
            }
        }
        return null;
    }

    public WikiPage neighbor(WikiPage page, int direction) {
        Category category = categoryOf(page);
        if (category == null) {
            return null;
        }
        int index = category.pages().indexOf(page);
        int target = index + direction;
        if (index < 0 || target < 0 || target >= category.pages().size()) {
            return null;
        }
        return category.pages().get(target);
    }

    /** Поиск по названию, тегам и тексту. Пустой запрос — пустой список. */
    public List<WikiPage> search(String query, int limit) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            return List.of();
        }
        Map<WikiPage, Integer> score = new LinkedHashMap<>();
        for (WikiPage page : pages) {
            int points = 0;
            String title = page.title.toLowerCase(Locale.ROOT);
            String titleEn = page.titleEn.toLowerCase(Locale.ROOT);
            if (title.startsWith(q)) {
                points += 80;
            } else if (title.contains(q)) {
                points += 55;
            }
            if (!titleEn.isEmpty() && titleEn.contains(q)) {
                points += 30;
            }
            if (page.id.contains(q)) {
                points += 25;
            }
            for (String itemId : page.items) {
                if (itemId.contains(q)) {
                    points += 18;
                    break;
                }
            }
            for (String tag : page.tags) {
                if (tag.contains(q)) {
                    points += 20;
                }
            }
            if (points > 0 && page.plainText.toLowerCase(Locale.ROOT).contains(q)) {
                points += 10;
            } else if (points == 0 && page.plainText.toLowerCase(Locale.ROOT).contains(q)) {
                points += 6;
            }
            if (points > 0) {
                score.put(page, points);
            }
        }
        List<WikiPage> result = new ArrayList<>(score.keySet());
        result.sort((a, b) -> {
            int diff = score.getOrDefault(b, 0) - score.getOrDefault(a, 0);
            return diff != 0 ? diff : a.title.compareToIgnoreCase(b.title);
        });
        return result.size() > limit ? new ArrayList<>(result.subList(0, limit)) : result;
    }

    /** Все предметы, у которых есть страница (для самопроверки и отладки). */
    public Set<String> coveredItems() {
        Set<String> set = new LinkedHashSet<>();
        for (WikiPage page : pages) {
            set.addAll(page.items);
        }
        return set;
    }

    public int totalChars() {
        int sum = 0;
        for (WikiPage page : pages) {
            sum += page.chars;
        }
        return sum;
    }
}
