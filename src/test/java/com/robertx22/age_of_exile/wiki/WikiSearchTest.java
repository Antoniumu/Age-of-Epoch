package com.robertx22.age_of_exile.wiki;

import com.robertx22.age_of_exile.wiki.WikiMarkdown.Page;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WikiSearchTest {

    private Page page(String id, String title, String itemId, String tags, String body) {
        StringBuilder raw = new StringBuilder("---\n")
            .append("title: ").append(title).append('\n')
            .append("category: test\n");
        if (itemId != null) {
            raw.append("item: ").append(itemId).append('\n');
        }
        if (tags != null) {
            raw.append("tags: ").append(tags).append('\n');
        }
        raw.append("---\n# ").append(title).append("\n\n").append(body).append('\n');
        return WikiMarkdown.parse(id, raw.toString());
    }

    @Test
    @DisplayName("пустой запрос возвращает каталог без перестановки")
    void emptyQueryPreservesCatalogOrder() {
        Page first = page("guides/a", "А", null, null, "Первая страница.");
        Page second = page("guides/b", "Б", null, null, "Вторая страница.");
        List<Page> result = WikiSearch.search(Arrays.asList(first, second), "  ");

        assertEquals(Arrays.asList(first, second), result);
    }

    @Test
    @DisplayName("точное совпадение с названием выше совпадения в тексте")
    void titleMatchesRankAboveBodyMatches() {
        Page bodyHit = page("guides/crafting", "Ремесло", null, null,
            "Кристалл очищения применяется к снаряжению.");
        Page titleHit = page("items/crystal", "Кристалл очищения",
            "mmorpg:currency/crystal_of_purification", "очищение", "Снимает порчу.");

        List<Page> result = WikiSearch.search(Arrays.asList(bodyHit, titleHit), "КРИСТАЛЛ ОЧИЩЕНИЯ");

        assertEquals(2, result.size());
        assertEquals("items/crystal", result.get(0).id);
        assertEquals("guides/crafting", result.get(1).id);
    }

    @Test
    @DisplayName("многословный запрос ищет слова независимо от порядка по тегам и тексту")
    void allTermsMatchRegardlessOfOrder() {
        Page resultPage = page("items/golden_orb", "Золотой шар", "mmorpg:mat/golden_orb",
            "Golden Orb, материалы", "Материал для крафта валюты.");
        Page partialMatch = page("items/orb", "Шар", null, "Golden", "Применяется в рецепте.");

        List<Page> result = WikiSearch.search(Arrays.asList(partialMatch, resultPage), "orb GOLDEN");

        assertEquals(2, result.size());
        assertEquals("items/golden_orb", result.get(0).id);
    }

    @Test
    @DisplayName("ID предмета сравнивается без учёта регистра")
    void registryIdSearchIsCaseInsensitive() {
        Page item = page("items/ruby", "Божественный рубин", "mmorpg:gems/ruby/6", "Самоцветы", "Огненный камень.");

        List<Page> result = WikiSearch.search(Arrays.asList(item), "MMORPG:GEMS/RUBY/6");

        assertEquals(1, result.size());
        assertEquals(item, result.get(0));
    }

    @Test
    @DisplayName("ID страницы тоже участвует в поиске без учёта регистра")
    void pageIdSearchIsCaseInsensitive() {
        Page guide = page("guides/rare-path", "Особый путь", null, null, "Краткое описание.");

        List<Page> result = WikiSearch.search(Arrays.asList(guide), "GUIDES/RARE-PATH");

        assertEquals(Arrays.asList(guide), result);
    }

    @Test
    @DisplayName("NFKC и Unicode-пробелы нормализуются перед поиском")
    void compatibilityCharactersAndUnicodeSpacesNormalize() {
        Page item = page("items/potion", "Potion", null, null, "A useful item.");

        List<Page> result = WikiSearch.search(Arrays.asList(item), "ＰＯＴＩＯＮ\u00a0USEFUL");

        assertEquals(Arrays.asList(item), result);
    }

    @Test
    @DisplayName("в русском поиске ё и е считаются эквивалентными")
    void russianYoAndEAreEquivalent() {
        Page item = page("items/yolka", "Ёлка", null, null, "Предмет для украшения.");

        List<Page> result = WikiSearch.search(Arrays.asList(item), "елка");

        assertEquals(1, result.size());
        assertEquals(item, result.get(0));
    }

    @Test
    @DisplayName("поиск требует наличия всех слов запроса")
    void allTermsAreRequired() {
        Page item = page("items/crystal", "Кристалл очищения", null, null, "Снимает порчу.");

        assertTrue(WikiSearch.search(Arrays.asList(item), "кристалл отсутствует").isEmpty());
    }
}
