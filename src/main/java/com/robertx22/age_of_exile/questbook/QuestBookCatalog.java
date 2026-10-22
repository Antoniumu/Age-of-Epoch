package com.robertx22.age_of_exile.questbook;

import java.util.List;

public final class QuestBookCatalog {

    public enum Chapter {
        AWAKENING("Пробуждение", "Первые шаги, характеристики, таланты и школы заклинаний"),
        CRAFTING("Мастерство и Ремесло", "Профессии, руны, рунворды и редкое снаряжение"),
        CONQUEST("Покорение Измерений", "Подземелья Dungeon Realm, Башни, Делвы, Обелиски и Жатва"),
        PLATINUM("Платиновый Кодекс", "100% коллекция рун, рунвордов, уников, боссов и профессий");

        public final String title;
        public final String subtitle;

        Chapter(String title, String subtitle) {
            this.title = title;
            this.subtitle = subtitle;
        }
    }

    public enum Metric {
        LEVEL,
        STAT_POINTS,
        TALENT_POINTS,
        SPELLS_LEARNED,
        PROFESSIONS_USED,
        MAX_PROFESSIONS,
        RUNES,
        RUNEWORDS,
        UNIQUE_GEARS,
        DUNGEON_LAYOUTS,
        TOWERS,
        DELVES,
        UBER_BOSSES
    }

    public record QuestEntry(String id, Chapter chapter, String title, String description, Metric metric, int target) {}

    public static final List<QuestEntry> ALL = List.of(
        new QuestEntry("lvl_10", Chapter.AWAKENING, "Первая закалка", "Достигните 10-го уровня персонажа.", Metric.LEVEL, 10),
        new QuestEntry("stats_15", Chapter.AWAKENING, "Фундамент силы", "Распределите 15 очков характеристик.", Metric.STAT_POINTS, 15),
        new QuestEntry("talents_10", Chapter.AWAKENING, "Древо судьбы", "Вложите 10 очков талантов или возвышения.", Metric.TALENT_POINTS, 10),
        new QuestEntry("spells_4", Chapter.AWAKENING, "Школа арканы", "Изучите 4 навыка или пассивки в школе заклинаний.", Metric.SPELLS_LEARNED, 4),
        new QuestEntry("lvl_50", Chapter.AWAKENING, "Ветеран Эпохи", "Достигните 50-го уровня персонажа.", Metric.LEVEL, 50),

        new QuestEntry("prof_use_3", Chapter.CRAFTING, "Подмастерье", "Освойте хотя бы 3 ремесленные профессии.", Metric.PROFESSIONS_USED, 3),
        new QuestEntry("runes_5", Chapter.CRAFTING, "Сборщик рун", "Соберите 5 различных рун в коллекцию.", Metric.RUNES, 5),
        new QuestEntry("rw_1", Chapter.CRAFTING, "Первое рунное слово", "Создайте предмет с активным рунным словом.", Metric.RUNEWORDS, 1),
        new QuestEntry("uniq_5", Chapter.CRAFTING, "Охотник за реликвиями", "Найдите 5 уникальных предметов.", Metric.UNIQUE_GEARS, 5),

        new QuestEntry("dungeon_3", Chapter.CONQUEST, "Исследователь разломов", "Зачистите 3 типа карт Dungeon Realm.", Metric.DUNGEON_LAYOUTS, 3),
        new QuestEntry("towers_3", Chapter.CONQUEST, "Штурм башен", "Победите стражей 3 различных башен в мире.", Metric.TOWERS, 3),
        new QuestEntry("delves_5", Chapter.CONQUEST, "Глубинный проходчик", "Завершите 5 подземных разломов Делва.", Metric.DELVES, 5),
        new QuestEntry("lvl_100", Chapter.CONQUEST, "Вершина развития", "Достигните 100-го уровня персонажа.", Metric.LEVEL, 100),

        new QuestEntry("platinum_runes", Chapter.PLATINUM, "Полный каталог рун", "Соберите все руны Mine and Slash.", Metric.RUNES, 999),
        new QuestEntry("platinum_rw", Chapter.PLATINUM, "Магистр рунных слов", "Соберите все рунные слова.", Metric.RUNEWORDS, 999),
        new QuestEntry("platinum_uniques", Chapter.PLATINUM, "Хранитель артефактов", "Соберите всю коллекцию уникальной экипировки.", Metric.UNIQUE_GEARS, 999),
        new QuestEntry("platinum_professions", Chapter.PLATINUM, "Великий ремесленник", "Достигните максимума во всех профессиях.", Metric.MAX_PROFESSIONS, 999),
        new QuestEntry("platinum_dungeons", Chapter.PLATINUM, "Владыка подземелий", "Пройдите все карты Dungeon Realm.", Metric.DUNGEON_LAYOUTS, 999),
        new QuestEntry("platinum_bosses", Chapter.PLATINUM, "Покоритель Убер-Боссов", "Победите всех Убер-боссов и Древние Обелиски.", Metric.UBER_BOSSES, 999)
    );
}
