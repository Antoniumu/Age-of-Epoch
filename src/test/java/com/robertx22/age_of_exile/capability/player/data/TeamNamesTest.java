package com.robertx22.age_of_exile.capability.player.data;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Названия команд: раньше команда создавалась с случайным UUID и в HUD
 * показывались его первые 8 символов — игрок ничего не мог настроить.
 */
public class TeamNamesTest {

    @Test
    @DisplayName("обычное название сохраняется как есть")
    public void keepsNormalName() {
        assertEquals("Тихий авангард", TeamNames.sanitize("Тихий авангард"));
        assertEquals("Sapphire Watch", TeamNames.sanitize("Sapphire Watch"));
    }

    @Test
    @DisplayName("§-коды вырезаются: иначе игрок перекрашивает интерфейс")
    public void stripsFormattingCodes() {
        String cleaned = TeamNames.sanitize("§c§lЧитеры§r навсегда");
        assertFalse(cleaned.contains("§"), "в названии остались §-коды: " + cleaned);
        assertTrue(cleaned.contains("Читеры"));
    }

    @Test
    @DisplayName("пробелы сводятся, края обрезаются")
    public void collapsesWhitespace() {
        assertEquals("Тихий авангард", TeamNames.sanitize("  Тихий \t  авангард  "));
    }

    @Test
    @DisplayName("длина ограничена MAX_LENGTH")
    public void limitsLength() {
        String longName = "А".repeat(TeamNames.MAX_LENGTH + 20);
        assertEquals(TeamNames.MAX_LENGTH, TeamNames.sanitize(longName).length());
    }

    @Test
    @DisplayName("пустое и мусорное название даёт пустую строку")
    public void emptyInput() {
        assertEquals("", TeamNames.sanitize(null));
        assertEquals("", TeamNames.sanitize(""));
        assertEquals("", TeamNames.sanitize("   "));
        assertEquals("", TeamNames.sanitize("§c§l"));
    }

    @Test
    @DisplayName("показ: заданное имя, иначе короткий id")
    public void displayFallsBackToId() {
        assertEquals("Тихий авангард", TeamNames.display("Тихий авангард", "12345678-90ab-cdef"));
        assertEquals("12345678", TeamNames.display("", "12345678-90ab-cdef"));
        assertEquals("short", TeamNames.display(null, "short"));
        assertEquals("", TeamNames.display(null, ""));
    }

    @Test
    @DisplayName("имя по умолчанию собирается из ника игрока и не длиннее лимита")
    public void defaultNameUsesPlayer() {
        assertEquals("Отряд Steve", TeamNames.defaultName("Steve"));
        assertEquals(TeamNames.FALLBACK_PREFIX, TeamNames.defaultName(""));
        assertTrue(TeamNames.defaultName("ОченьДлинныйНикИгрока").length() <= TeamNames.MAX_LENGTH);
    }

    @Test
    @DisplayName("сравнение без учёта регистра и лишних пробелов")
    public void comparesLoosely() {
        assertTrue(TeamNames.same("Тихий авангард", "  тихий   АВАНГАРД "));
        assertFalse(TeamNames.same("Отряд A", "Отряд B"));
    }
}
