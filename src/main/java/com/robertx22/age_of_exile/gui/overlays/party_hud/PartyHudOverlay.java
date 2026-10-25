package com.robertx22.age_of_exile.gui.overlays.party_hud;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.age_of_exile.combat.CombatPartyTracker;
import com.robertx22.age_of_exile.combat.PartyHudState;
import com.robertx22.forge_compat.client.AbstractGui;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/**
 * Правая панель «AOE / Обзор»: команда, дневная цель, цель недели и живой DPS
 * в бою.
 *
 * <p>Всё настраивается: панель можно выключить целиком (клавиша F7 или
 * {@code visible=false}), двигать по четырём углам/центрам (клавиша F8 или
 * {@code position}), менять ширину и отключать отдельные блоки. Настройки
 * лежат в {@code config/age_of_epoch/party_hud.properties} и перечитываются на
 * лету: файл можно править хоть в игре, панель перестроится сама.
 *
 * <p>Длинный текст (например «Победите монстров · 0/25») больше не обрезается
 * многоточием: он переносится по словам на несколько строк, а высота панели
 * считается по фактической вёрстке. Раньше хвост цели просто пропадал.
 */
public final class PartyHudOverlay extends AbstractGui {

    private static final int BG = 0xD9111822;
    private static final int INNER = 0xC91D2938;
    private static final int MUTED = 0xFF9EAFC2;
    private static final int WHITE = 0xFFF0F5FA;
    private static final int ACCENT = 0xFF63D7C2;
    private static final int GOLD = 0xFFF3C978;
    private static final int LINE = 0x554D6073;

    /** Пределы ширины панели: уже — не влезает «ЦЕЛЬ НЕДЕЛИ», шире — лезет на хотбар. */
    private static final int MIN_WIDTH = 132;
    private static final int MAX_WIDTH = 220;

    private static boolean visible = true;
    private static int position = 0;
    private static int width = 154;
    private static boolean showTeam = true;
    private static boolean showDaily = true;
    private static boolean showWeekly = true;
    private static boolean showCombat = true;
    private static float combatBlend = 0;
    private static long lastSave = 0;
    private static long configLoadedAt = 0;

    public PartyHudOverlay() {
        load();
        // Файл настроек создаём сразу: иначе игроку неоткуда узнать про
        // visible/position/width, пока он случайно не нажмёт O или P.
        if (!Files.exists(configPath())) {
            save();
        }
    }

    // ------------------------------------------------------------------ состояние

    public static boolean isVisible() {
        return visible;
    }

    public static void toggle() {
        visible = !visible;
        save();
    }

    public static void cyclePosition() {
        position = (position + 1) % 4;
        save();
    }

    public static int getPosition() {
        return position;
    }

    public static int getWidth() {
        return width;
    }

    // ------------------------------------------------------------------ отрисовка

    public void onHudRender(PoseStack pose, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        reloadIfChanged();

        // Во время аудита умений панель закрывает правую часть кадра и мешает
        // смотреть видео: она про группу и цели, а не про проверяемое умение.
        if (com.robertx22.age_of_exile.uncommon.testing.SpellAuditState.active) {
            return;
        }

        if (!visible || mc.player == null || mc.getDebugOverlay().showDebugScreen()) {
            return;
        }
        if (mc.player.isSpectator()) {
            return;
        }

        boolean combat = showCombat && PartyHudState.combat
            && System.currentTimeMillis() - PartyHudState.lastUpdate < 3_000L;
        combatBlend += ((combat ? 1.0f : 0.0f) - combatBlend) * 0.22f;
        int rows = combat ? Math.min(4, PartyHudState.members.size()) : 0;

        int textWidth = width - 22;

        // Сначала считаем текст, потом высоту: панель должна расти под переносы,
        // а не обрезать цель многоточием.
        List<String> teamLines = showTeam ? wrap(mc, PartyHudState.team, textWidth, 2) : List.of();
        List<String> dailyLines = showDaily ? wrap(mc, PartyHudState.daily, textWidth, 2) : List.of();
        List<String> weeklyLines = showWeekly ? wrap(mc, PartyHudState.weekly, textWidth, 2) : List.of();

        int height = 12;                                   // заголовок
        if (!teamLines.isEmpty()) {
            height += teamLines.size() * 10 + 3;
        }
        if (!dailyLines.isEmpty()) {
            height += 14 + dailyLines.size() * 10;         // подпись + строки
        }
        if (!weeklyLines.isEmpty()) {
            height += 6 + 14 + weeklyLines.size() * 10;
        }
        if (combat && rows > 0) {
            height += 12;                                  // разделитель
            height += 13;                                  // название отряда
            height += rows * 17;
        }
        height += 15;                                      // футер

        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        int x = position == 3 ? 8 : screenW - width - 8;
        int y = switch (position) {
            case 1 -> 34;
            case 2 -> screenH - height - 34;
            default -> (screenH - height) / 2;
        };
        x = Math.max(4, Math.min(screenW - width - 4, x));
        y = Math.max(4, Math.min(screenH - height - 4, y));

        fill(pose, x, y, x + width, y + height, BG);
        fill(pose, x, y, x + 2, y + height, blendColor(ACCENT, 0xFF8FC8FF, combatBlend));
        fill(pose, x + 2, y, x + width, y + 1, 0x663D5268);

        String header = combat ? "✦  AOE  /  БОЙ" : "✦  AOE  /  ОБЗОР";
        drawShadow(mc.font, pose, header, x + 11, y + 8, ACCENT);

        int cy = y + 20;
        for (String line : teamLines) {
            drawString(pose, mc.font, line, x + 11, cy, WHITE);
            cy += 10;
        }
        if (!teamLines.isEmpty()) {
            cy += 3;
            fill(pose, x + 10, cy, x + width - 10, cy + 1, LINE);
            cy += 2;
        }

        if (!dailyLines.isEmpty()) {
            drawString(pose, mc.font, "◷  ДНЕВНАЯ ЦЕЛЬ", x + 11, cy + 3, GOLD);
            int ty = cy + 7;
            for (String line : dailyLines) {
                drawString(pose, mc.font, line, x + 11, ty + 6, MUTED);
                ty += 10;
            }
            cy = ty + 6;
        }

        if (!weeklyLines.isEmpty()) {
            drawString(pose, mc.font, "◇  ЦЕЛЬ НЕДЕЛИ", x + 11, cy + 3, 0xFF8FC8FF);
            int ty = cy + 7;
            for (String line : weeklyLines) {
                drawString(pose, mc.font, line, x + 11, ty + 6, MUTED);
                ty += 10;
            }
            cy = ty + 6;
        }

        if (combat && rows > 0) {
            fill(pose, x + 10, cy, x + width - 10, cy + 1, LINE);
            drawString(pose, mc.font, PartyHudState.squadIcon + "  " + PartyHudState.squadName,
                x + 11, cy + 6, WHITE);
            double maxDps = PartyHudState.members.stream()
                .mapToDouble(CombatPartyTracker.Member::dps).max().orElse(1);
            int rowY = cy + 19;
            for (int i = 0; i < rows; i++) {
                CombatPartyTracker.Member member = PartyHudState.members.get(i);
                int color = 0xFF000000 | (member.color() & 0xFFFFFF);
                fill(pose, x + 11, rowY + 2, x + 15, rowY + 7, color);
                String dps = formatDps(member.dps());
                int nameWidth = width - 32 - mc.font.width(dps) - 6;
                drawString(pose, mc.font, trim(mc, member.name(), nameWidth), x + 19, rowY,
                    i == 0 ? WHITE : MUTED);
                drawString(pose, mc.font, dps, x + width - 11 - mc.font.width(dps), rowY,
                    i == 0 ? GOLD : WHITE);
                int barX = x + 19;
                int barY = rowY + 10;
                int barW = width - 32;
                fill(pose, barX, barY, barX + barW, barY + 2, INNER);
                int fillW = Math.max(1, (int) (barW * Math.min(1, member.dps() / Math.max(1, maxDps))));
                fill(pose, barX, barY, barX + fillW, barY + 2, color);
                rowY += 17;
            }
            String live = "LIVE · DPS";
            drawString(pose, mc.font, live, x + width - 11 - mc.font.width(live), y + height - 11, ACCENT);
        } else {
            String hint = trim(mc, "F7 скрыть · F8 положение", textWidth);
            drawString(pose, mc.font, hint, x + 11, y + height - 12, 0xFF8293A5);
        }
    }

    private static int blendColor(int from, int to, float amount) {
        float t = Math.max(0, Math.min(1, amount));
        int r = (int) (((from >> 16) & 255) * (1 - t) + ((to >> 16) & 255) * t);
        int g = (int) (((from >> 8) & 255) * (1 - t) + ((to >> 8) & 255) * t);
        int b = (int) ((from & 255) * (1 - t) + (to & 255) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static String formatDps(double dps) {
        if (dps >= 1_000_000) {
            return String.format(Locale.ROOT, "%.1fM", dps / 1_000_000);
        }
        if (dps >= 10_000) {
            return String.format(Locale.ROOT, "%.0fk", dps / 1_000);
        }
        if (dps >= 1_000) {
            return String.format(Locale.ROOT, "%.1fk", dps / 1_000);
        }
        return String.format(Locale.ROOT, "%.0f", dps);
    }

    // ------------------------------------------------------------------ текст

    /** Перенос по словам: см. {@link HudTextWrap}, там же и тесты вёрстки. */
    static List<String> wrap(Minecraft mc, String text, int maxWidth, int maxLines) {
        return HudTextWrap.wrap(text, maxWidth, maxLines, value -> mc.font.width(value));
    }

    private static String trim(Minecraft mc, String text, int maxWidth) {
        return HudTextWrap.trim(text, maxWidth, value -> mc.font.width(value));
    }

    // ------------------------------------------------------------------ конфиг

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("age_of_epoch/party_hud.properties");
    }

    /** Перечитывает файл, если его изменили руками во время игры. */
    private static void reloadIfChanged() {
        try {
            Path path = configPath();
            if (!Files.exists(path)) {
                return;
            }
            long modified = Files.getLastModifiedTime(path).toMillis();
            if (modified != configLoadedAt) {
                configLoadedAt = modified;
                load();
            }
        } catch (Exception ignored) {
            // настройки не критичны для игры: молча оставляем прежние
        }
    }

    private static void load() {
        try {
            Path path = configPath();
            if (!Files.exists(path)) {
                return;
            }
            Properties properties = new Properties();
            try (var reader = Files.newBufferedReader(path)) {
                properties.load(reader);
            }
            visible = Boolean.parseBoolean(properties.getProperty("visible", "true"));
            position = Math.floorMod(Integer.parseInt(properties.getProperty("position", "0")), 4);
            width = clampWidth(Integer.parseInt(properties.getProperty("width", "154")));
            showTeam = Boolean.parseBoolean(properties.getProperty("show_team", "true"));
            showDaily = Boolean.parseBoolean(properties.getProperty("show_daily", "true"));
            showWeekly = Boolean.parseBoolean(properties.getProperty("show_weekly", "true"));
            showCombat = Boolean.parseBoolean(properties.getProperty("show_combat", "true"));
        } catch (Exception ignored) {
            // битый файл настроек не должен ломать HUD
        }
    }

    private static int clampWidth(int value) {
        return Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, value));
    }

    private static void save() {
        if (System.currentTimeMillis() - lastSave < 100) {
            return;
        }
        lastSave = System.currentTimeMillis();
        try {
            Path path = configPath();
            Files.createDirectories(path.getParent());
            Properties properties = new Properties();
            properties.setProperty("visible", Boolean.toString(visible));
            properties.setProperty("position", Integer.toString(position));
            properties.setProperty("width", Integer.toString(width));
            properties.setProperty("show_team", Boolean.toString(showTeam));
            properties.setProperty("show_daily", Boolean.toString(showDaily));
            properties.setProperty("show_weekly", Boolean.toString(showWeekly));
            properties.setProperty("show_combat", Boolean.toString(showCombat));
            try (var writer = Files.newBufferedWriter(path)) {
                properties.store(writer, "Age of Epoch party HUD: visible, position (0..3), width (132..220), show_team/show_daily/show_weekly/show_combat");
            }
            configLoadedAt = Files.getLastModifiedTime(path).toMillis();
        } catch (IOException ignored) {
            // нет прав на запись — живём с настройками в памяти
        }
    }
}
