package com.robertx22.age_of_exile.gui.overlays.progression;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.age_of_exile.uncommon.testing.ProgressionState;
import com.robertx22.forge_compat.client.AbstractGui;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

/**
 * Подпись к видео прогона прокачки: какой этап идёт, какой уровень и что
 * произошло (выбран путь, вложены очки).
 *
 * <p>Тот же приём, что у {@code SpellAuditOverlay}: текст рисует сам клиент,
 * поэтому подпись всегда в кадре и не зависит от шрифтов ffmpeg на раннере.
 * Панель видна только во время прогона ({@link ProgressionState#active}).</p>
 */
public final class ProgressionOverlay extends AbstractGui {

    private static final int PANEL_BG = 0xE0101620;
    private static final int ACCENT = 0xFF63C7D7;
    private static final int GOLD = 0xFFF3C978;
    private static final int WHITE = 0xFFF0F5FA;
    private static final int MUTED = 0xFF9EAFC2;
    private static final int FAIL = 0xFFFF8A80;
    private static final int PASS = 0xFF9BE89B;

    private static final int X = 10;
    private static final int Y = 10;
    private static final int WIDTH = 400;
    private static final int PADDING = 10;

    public void onHudRender(PoseStack pose, float partialTick) {
        if (!ProgressionState.active) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.font == null) {
            return;
        }
        try {
            if (mc.getDebugOverlay().showDebugScreen()) {
                return;
            }
        } catch (Throwable ignored) {
        }

        String head = "ПРОКАЧКА: ПОЛНЫЙ ЦИКЛ";
        String counter = ProgressionState.targetLevel > 0
            ? ProgressionState.level + " / " + ProgressionState.targetLevel
            : "";
        List<String> stageLines = wrap(mc, ProgressionState.stage, WIDTH - PADDING * 2);
        List<String> detailLines = wrap(mc, ProgressionState.detail, WIDTH - PADDING * 2);
        List<String> verdictLines = wrap(mc, ProgressionState.verdict, WIDTH - PADDING * 2);

        int height = PADDING;
        height += 11;                            // заголовок
        height += stageLines.size() * 10 + 4;    // этап
        height += detailLines.size() * 10 + 2;   // деталь
        if (!verdictLines.isEmpty()) {
            height += 3 + verdictLines.size() * 10;
        }
        height += PADDING;

        fill(pose, X, Y, X + WIDTH, Y + height, PANEL_BG);
        fill(pose, X, Y, X + 3, Y + height, ACCENT);
        fill(pose, X + 3, Y, X + WIDTH, Y + 1, 0x66406A78);

        int cy = Y + PADDING;
        drawString(pose, mc.font, head, X + PADDING, cy, ACCENT);
        if (!counter.isEmpty()) {
            drawString(pose, mc.font, counter, X + WIDTH - PADDING - mc.font.width(counter), cy, MUTED);
        }
        cy += 13;

        for (String line : stageLines) {
            drawString(pose, mc.font, line, X + PADDING, cy, GOLD);
            cy += 10;
        }
        cy += 4;

        for (String line : detailLines) {
            drawString(pose, mc.font, line, X + PADDING, cy, WHITE);
            cy += 10;
        }
        cy += 2;

        if (!verdictLines.isEmpty()) {
            cy += 3;
            boolean ok = ProgressionState.verdict.startsWith("PASS");
            for (String line : verdictLines) {
                drawString(pose, mc.font, line, X + PADDING, cy, ok ? PASS : FAIL);
                cy += 10;
            }
        }
    }

    private static List<String> wrap(Minecraft mc, String text, int width) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank() || width <= 0) {
            return lines;
        }
        StringBuilder line = new StringBuilder();
        for (String word : text.trim().split("\\s+")) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (mc.font.width(candidate) > width && line.length() > 0) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
            if (lines.size() >= 4) {
                return lines;
            }
        }
        if (line.length() > 0) {
            lines.add(line.toString());
        }
        return lines;
    }
}
