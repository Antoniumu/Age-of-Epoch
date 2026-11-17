package com.robertx22.age_of_exile.wiki;

import com.robertx22.age_of_exile.wiki.WikiMarkdown.Page;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Pure, deterministic search over parsed wiki pages. Kept Minecraft-free so it can be unit tested. */
public final class WikiSearch {

    private WikiSearch() {
    }

    /**
     * Search names, tags, page/registry ids and article text. Every whitespace-delimited query term must
     * occur somewhere on a page; the order of terms does not matter. Name and id matches rank above body text.
     */
    public static List<Page> search(List<Page> pages, String query) {
        if (pages == null || pages.isEmpty()) {
            return Collections.emptyList();
        }
        String normalizedQuery = normalize(query);
        if (normalizedQuery.isEmpty()) {
            return new ArrayList<Page>(pages);
        }

        String[] terms = normalizedQuery.split("\\s+");
        List<Match> matches = new ArrayList<Match>();
        for (Page page : pages) {
            if (page == null) {
                continue;
            }
            String title = normalize(page.title);
            String pageId = normalize(page.id);
            String itemId = normalize(page.itemId);
            String tags = normalize(page.tags);
            String allText = normalize(page.searchText);
            if (!containsAll(allText, terms)) {
                continue;
            }

            int rank;
            if (title.equals(normalizedQuery)) {
                rank = 0;
            } else if (title.startsWith(normalizedQuery)) {
                rank = 10;
            } else if (title.contains(normalizedQuery)) {
                rank = 20;
            } else if (containsAll(title, terms)) {
                rank = 30;
            } else if (pageId.equals(normalizedQuery) || itemId.equals(normalizedQuery)) {
                rank = 40;
            } else if (pageId.contains(normalizedQuery) || itemId.contains(normalizedQuery)) {
                rank = 50;
            } else if (containsAll(tags, terms)) {
                rank = 60;
            } else {
                rank = 100;
            }
            matches.add(new Match(page, rank));
        }

        Collections.sort(matches, new Comparator<Match>() {
            @Override
            public int compare(Match left, Match right) {
                int byRank = left.rank - right.rank;
                if (byRank != 0) {
                    return byRank;
                }
                int byTitle = left.page.title.compareToIgnoreCase(right.page.title);
                return byTitle != 0 ? byTitle : left.page.id.compareTo(right.page.id);
            }
        });

        List<Page> result = new ArrayList<Page>(matches.size());
        for (Match match : matches) {
            result.add(match.page);
        }
        return result;
    }

    /** Case-insensitive Unicode search; NFKC normalizes compatibility forms and Russian ё/е are equivalent. */
    static String normalize(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC)
            .toLowerCase(Locale.ROOT)
            .replace('\u0451', '\u0435');
        StringBuilder compact = new StringBuilder(normalized.length());
        boolean pendingSpace = false;
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (Character.isWhitespace(c) || Character.isSpaceChar(c)) {
                pendingSpace = compact.length() > 0;
            } else {
                if (pendingSpace) {
                    compact.append(' ');
                    pendingSpace = false;
                }
                compact.append(c);
            }
        }
        return compact.toString();
    }

    private static boolean containsAll(String text, String[] terms) {
        for (String term : terms) {
            if (!text.contains(term)) {
                return false;
            }
        }
        return true;
    }

    private static final class Match {
        final Page page;
        final int rank;

        Match(Page page, int rank) {
            this.page = page;
            this.rank = rank;
        }
    }
}
