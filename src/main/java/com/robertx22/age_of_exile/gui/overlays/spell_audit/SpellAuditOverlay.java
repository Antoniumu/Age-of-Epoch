package com.robertx22.age_of_exile.gui.overlays.spell_audit;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.age_of_exile.uncommon.testing.SpellAuditState;
import com.robertx22.forge_compat.client.AbstractGui;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

/**
 * Подпись к видео с проверкой заклинаний: что за умение, что оно делает и что
 * реально произошло в этот момент (урон, попадания, эффекты).
 *
 * <p>Зачем в игре, а не текстом поверх видео: шрифт ffmpeg на раннере может
 * отсутствовать, а тут подпись рисует сам клиент — она всегда в кадре, совпадает
 * с тем, что видит игрок в тултипе, и не требует монтажа текста.
 *
 * <p>Панель показывается только во время прогона ({@link SpellAuditState#active}),
 * то есть в обычной игре её не видно.
 */
public final class SpellAuditOverlay extends AbstractGui {

    private static final int PANEL_BG = 0xE0101620;
    private static final int ACCENT = 0xFF63D7C2;
    private static final int GOLD = 0xFFF3C978;
    private static final int WHITE = 0xFFF0F5FA;
    private static final int MUTED = 0xFF9EAFC2;
    private static final int FAIL = 0xFFFF8A80;
    private static final int PASS = 0xFF9BE89B;

    private static final int X = 10;
    private static final int Y = 10;
    private static final int WIDTH = 470;
    private static final int PADDING = 10;

    public SpellAuditOverlay() {

    }

    public void onHudRender(PoseStack pose, float partialTick) {
        if (!SpellAuditState.active) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.font == null) {
            return;
        }
        // В дебаг-экране подпись мешала бы: прогон идёт без него.
        try {
            if (mc.getDebugOverlay().showDebugScreen()) {
                return;
            }
        } catch (Throwable ignored) {
        }

        String head = "ПРОВЕРКА УМЕНИЙ · " + SpellAuditState.school;
        String counter = SpellAuditState.index + "/" + SpellAuditState.total;
        List<String> description = wrap(mc, SpellAuditState.description, WIDTH - PADDING * 2);
        List<String> metrics = wrap(mc, SpellAuditState.metrics, WIDTH - PADDING * 2);
        List<String> verdict = wrap(mc, SpellAuditState.verdict, WIDTH - PADDING * 2);

        int height = PADDING;
        height += 11;                       // заголовок
        height += 14;                       // название умения
        height += description.size() * 10 + 3;
        height += 10;                       // фаза
        if (!metrics.isEmpty()) {
            height += metrics.size() * 10;
        }
        if (!verdict.isEmpty()) {
            height += 3 + verdict.size() * 10;
        }
        height += PADDING;

        fill(pose, X, Y, X + WIDTH, Y + height, PANEL_BG);
        fill(pose, X, Y, X + 3, Y + height, ACCENT);
        fill(pose, X + 3, Y, X + WIDTH, Y + 1, 0x66406A78);

        int cy = Y + PADDING;
        drawString(pose, mc.font, trim(mc, head, WIDTH - PADDING * 2 - 40), X + PADDING, cy, ACCENT);
        drawString(pose, mc.font, counter, X + WIDTH - PADDING - mc.font.width(counter), cy, MUTED);
        cy += 13;

        drawString(pose, mc.font, SpellAuditState.name, X + PADDING, cy, GOLD);
        cy += 14;

        for (String line : description) {
            drawString(pose, mc.font, line, X + PADDING, cy, WHITE);
            cy += 10;
        }
        cy += 3;

        drawString(pose, mc.font, SpellAuditState.phase, X + PADDING, cy, MUTED);
        cy += 10;

        for (String line : metrics) {
            drawString(pose, mc.font, line, X + PADDING, cy, 0xFFCFE3F2);
            cy += 10;
        }

        if (!verdict.isEmpty()) {
            cy += 3;
            boolean ok = SpellAuditState.verdict.startsWith("PASS");
            for (String line : verdict) {
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
            if (lines.size() >= 5) {
                return lines;
            }
        }
        if (line.length() > 0) {
            lines.add(line.toString());
        }
        return lines;
    }

    private static String trim(Minecraft mc, String text, int width) {
        if (text == null) {
            return "";
        }
        if (mc.font.width(text) <= width) {
            return text;
        }
        StringBuilder cut = new StringBuilder();
        for (char ch : text.toCharArray()) {
            if (mc.font.width(cut.toString() + ch + "…") > width) {
                break;
            }
            cut.append(ch);
        }
        return cut + "…";
    }
}
