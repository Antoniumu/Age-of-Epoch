package com.robertx22.age_of_exile.capability.player.data;

import java.util.Locale;

/**
 * Правила названий команд. Вынесено отдельно и без зависимостей от Minecraft:
 * так это можно покрыть обычным JUnit-тестом (см. {@code TeamNamesTest}) и
 * использовать и на сервере, и в интерфейсе.
 */
public final class TeamNames {

    /** Длиннее не влезает ни в HUD, ни в строку чата. */
    public static final int MAX_LENGTH = 24;

    /** Запасное имя, если игрок не задал своё. */
    public static final String FALLBACK_PREFIX = "Отряд";

    private TeamNames() {
    }

    /**
     * Чистит введённое название: убирает §-коды (иначе игрок перекрашивает
     * интерфейс и ломает вёрстку HUD), сводит пробелы, обрезает по длине.
     *
     * @return пустая строка, если после чистки ничего не осталось
     */
    public static String sanitize(String raw) {
        if (raw == null) {
            return "";
        }
        // Убираем § вместе со следующим символом-кодом: иначе «§c§l» превратится
        // в «c l» — мусор из букв формата вместо пустого имени.
        String cleaned = raw.replaceAll("§.", "")
            .replace("§", "")
            .replaceAll("\\s+", " ")
            .trim();
        if (cleaned.length() > MAX_LENGTH) {
            cleaned = cleaned.substring(0, MAX_LENGTH).trim();
        }
        return cleaned;
    }

    /** Название для показа: заданное, иначе короткий id команды. */
    public static String display(String name, String teamId) {
        String cleaned = sanitize(name);
        if (!cleaned.isEmpty()) {
            return cleaned;
        }
        if (teamId == null || teamId.isEmpty()) {
            return "";
        }
        return teamId.length() > 8 ? teamId.substring(0, 8) : teamId;
    }

    /** Предложение имени по умолчанию (например, при создании без названия). */
    public static String defaultName(String playerName) {
        String base = sanitize(playerName);
        if (base.isEmpty()) {
            return FALLBACK_PREFIX;
        }
        String candidate = FALLBACK_PREFIX + " " + base;
        return candidate.length() > MAX_LENGTH ? candidate.substring(0, MAX_LENGTH).trim() : candidate;
    }

    /** Сравнение названий без учёта регистра — для поиска в списке команд. */
    public static boolean same(String a, String b) {
        return sanitize(a).toLowerCase(Locale.ROOT).equals(sanitize(b).toLowerCase(Locale.ROOT));
    }
}
