package com.robertx22.age_of_exile.wikibook;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Одна страница вики: заголовок, метаданные и разобранные блоки.
 * Страница = .md-файл в assets/mmorpg/wiki/ru/&lt;id&gt;.md
 */
public final class WikiPage {

    public final String id;
    public final String title;
    public final String titleEn;
    public final String category;
    public final String summary;
    public final String plainText;
    public final List<String> tags;
    public final List<String> items;
    public final List<WikiMarkdown.Block> blocks;
    public final int chars;
    public final List<String> links;

    private WikiPage(String id, Map<String, String> meta, List<String> items,
                     List<WikiMarkdown.Block> blocks) {
        this.id = id;
        this.title = meta.getOrDefault("title", id);
        this.titleEn = meta.getOrDefault("en", "");
        this.category = meta.getOrDefault("category", "");
        this.tags = splitTags(meta.get("tags"));
        this.items = List.copyOf(items);
        this.blocks = List.copyOf(blocks);

        StringBuilder plain = new StringBuilder();
        List<String> outbound = new ArrayList<>();
        for (WikiMarkdown.Block block : blocks) {
            if (block.kind() == WikiMarkdown.Kind.TABLE) {
                for (List<String> row : block.rows()) {
                    plain.append(String.join(" ", row)).append(' ');
                }
                continue;
            }
            for (WikiMarkdown.Span span : block.spans()) {
                plain.append(span.text()).append(' ');
                if (span.isLink() && !outbound.contains(span.link())) {
                    outbound.add(span.link());
                }
            }
            plain.append(' ');
        }
        this.plainText = plain.toString().trim();
        this.chars = plainText.length();
        this.links = List.copyOf(outbound);
        this.summary = firstParagraph(blocks);
    }

    private static String firstParagraph(List<WikiMarkdown.Block> blocks) {
        for (WikiMarkdown.Block block : blocks) {
            if (block.kind() == WikiMarkdown.Kind.PARAGRAPH) {
                String text = WikiMarkdown.plainOf(block.spans()).trim();
                if (!text.isEmpty()) {
                    return text;
                }
            }
        }
        return "";
    }

    private static List<String> splitTags(String raw) {
        List<String> tags = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return tags;
        }
        for (String part : raw.split("[,;]")) {
            String tag = part.trim().toLowerCase(Locale.ROOT);
            if (!tag.isEmpty()) {
                tags.add(tag);
            }
        }
        return tags;
    }

    /** Разбирает .md-файл: строки "@ключ значение" до первой пустой строки, дальше — Markdown. */
    public static WikiPage parse(String id, String raw) {
        Map<String, String> meta = new LinkedHashMap<>();
        List<String> items = new ArrayList<>();
        String[] lines = (raw == null ? "" : raw.replace("\r", "")).split("\n", -1);

        int index = 0;
        boolean metaDone = false;
        while (index < lines.length && !metaDone) {
            String line = lines[index].trim();
            if (line.isEmpty()) {
                metaDone = true;
                index++;
                break;
            }
            if (!line.startsWith("@")) {
                metaDone = true;
                break;
            }
            String body = line.substring(1).trim();
            int space = body.indexOf(' ');
            String key = space < 0 ? body : body.substring(0, space).toLowerCase(Locale.ROOT);
            String value = space < 0 ? "" : body.substring(space + 1).trim();
            if (key.equals("item")) {
                if (!value.isEmpty()) {
                    items.add(value.toLowerCase(Locale.ROOT));
                }
            } else {
                meta.put(key, value);
            }
            index++;
        }

        StringBuilder body = new StringBuilder();
        for (int i = index; i < lines.length; i++) {
            body.append(lines[i]).append('\n');
        }
        return new WikiPage(id, meta, items, WikiMarkdown.parseBlocks(body.toString()));
    }

    public String shortTitle() {
        return title.length() > 46 ? title.substring(0, 44) + "…" : title;
    }
}
