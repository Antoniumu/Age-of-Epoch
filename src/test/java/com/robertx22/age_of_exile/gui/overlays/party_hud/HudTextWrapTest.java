package com.robertx22.age_of_exile.gui.overlays.party_hud;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.ToIntFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Вёрстка панели HUD. Дефект, из-за которого панель обрезала цель
 * («Победите монстров · 0/…»), был именно в отсутствии переноса строк.
 */
public class HudTextWrapTest {

    /** Ширина символа в пикселях; у ванильного шрифта это ≈6 px. */
    private static final ToIntFunction<String> WIDTH = value -> value.length() * 6;

    @Test
    @DisplayName("короткая строка остаётся одной строкой")
    public void keepsShortLine() {
        List<String> lines = HudTextWrap.wrap("0/25", 150, 2, WIDTH);
        assertEquals(List.of("0/25"), lines);
    }

    @Test
    @DisplayName("цель «Победите монстров · 0/25» видна целиком, а не обрезана")
    public void wrapsDailyGoal() {
        List<String> lines = HudTextWrap.wrap("Победите монстров · 0/25", 110, 2, WIDTH);
        assertEquals(2, lines.size(), "ожидалось две строки, получено: " + lines);
        assertEquals("Победите монстров", lines.get(0));
        // Прогресс должен быть виден полностью — именно его раньше срезало.
        assertEquals("0/25", lines.get(1));
        assertFalse(lines.get(1).contains("…"), "прогресс обрезан многоточием");
    }

    @Test
    @DisplayName("без «·» перенос идёт по словам и не режет слова")
    public void wrapsByWords() {
        List<String> lines = HudTextWrap.wrap("Ежедневная цель ещё не назначена", 120, 3, WIDTH);
        assertTrue(lines.size() > 1, "строка должна переноситься");
        for (String line : lines) {
            assertTrue(WIDTH.applyAsInt(line) <= 120 || line.length() == 1, "строка шире панели: " + line);
        }
        assertEquals("Ежедневная цель ещё не назначена", String.join(" ", lines));
    }

    @Test
    @DisplayName("слишком много строк — хвост схлопывается многоточием")
    public void limitsLines() {
        List<String> lines = HudTextWrap.wrap("a a a a a a a a a a a a", 30, 2, WIDTH);
        assertEquals(2, lines.size());
        assertTrue(lines.get(1).endsWith("…"), "ожидалось многоточие в последней строке: " + lines);
    }

    @Test
    @DisplayName("слово шире панели режется по символам, а не вылезает за край")
    public void cutsLongWord() {
        List<String> lines = HudTextWrap.wrap("Суперкалифраджилистикэкспиалидоция", 60, 4, WIDTH);
        assertTrue(lines.size() > 1);
        for (String line : lines) {
            assertTrue(WIDTH.applyAsInt(line) <= 60, "строка шире панели: " + line);
        }
    }

    @Test
    @DisplayName("пустой текст даёт пустой список")
    public void emptyText() {
        assertTrue(HudTextWrap.wrap("", 150, 2, WIDTH).isEmpty());
        assertTrue(HudTextWrap.wrap(null, 150, 2, WIDTH).isEmpty());
    }

    @Test
    @DisplayName("trim добавляет многоточие только когда не влезает")
    public void trimAddsEllipsis() {
        assertEquals("ник", HudTextWrap.trim("ник", 60, WIDTH));
        String trimmed = HudTextWrap.trim("ОченьДлинныйНикИгрока", 60, WIDTH);
        assertTrue(trimmed.endsWith("…"));
        assertTrue(WIDTH.applyAsInt(trimmed) <= 60, "trim не уложился в ширину: " + trimmed);
    }
}
