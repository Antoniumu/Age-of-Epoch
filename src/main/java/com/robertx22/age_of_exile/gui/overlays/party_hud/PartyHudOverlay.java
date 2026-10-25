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
import java.util.Properties;

/** A small, configurable right-side MMO-style party/goal/combat panel. */
public final class PartyHudOverlay extends AbstractGui {
    private static final int WIDTH = 154;
    private static final int BG = 0xD9111822;
    private static final int INNER = 0xC91D2938;
    private static final int MUTED = 0xFF9EAFC2;
    private static final int WHITE = 0xFFF0F5FA;
    private static final int ACCENT = 0xFF63D7C2;
    private static final int GOLD = 0xFFF3C978;
    private static boolean visible = true;
    private static int position = 0;
    private static float combatBlend = 0;
    private static long lastSave = 0;

    public PartyHudOverlay() {
        load();
    }

    public static boolean isVisible() { return visible; }
    public static void toggle() { visible = !visible; save(); }
    public static void cyclePosition() { position = (position + 1) % 4; save(); }

    public void onHudRender(PoseStack pose, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (!visible || mc.player == null || mc.getDebugOverlay().showDebugScreen()) return;
        if (mc.player.isSpectator()) return;

        boolean combat = PartyHudState.combat && System.currentTimeMillis() - PartyHudState.lastUpdate < 3_000L;
        combatBlend += ((combat ? 1.0f : 0.0f) - combatBlend) * 0.22f;
        int rows = combat ? Math.min(4, PartyHudState.members.size()) : 0;
        int height = combat ? 119 + rows * 17 : 110;
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        int x = switch (position) {
            case 1 -> screenW - WIDTH - 8;
            case 2 -> screenW - WIDTH - 8;
            case 3 -> 8;
            default -> screenW - WIDTH - 8;
        };
        int y = switch (position) {
            case 1 -> 34;
            case 2 -> screenH - height - 34;
            default -> (screenH - height) / 2;
        };
        x = Math.max(4, Math.min(screenW - WIDTH - 4, x));
        y = Math.max(4, Math.min(screenH - height - 4, y));

        fill(pose, x, y, x + WIDTH, y + height, BG);
        fill(pose, x, y, x + 2, y + height, blendColor(ACCENT, 0xFF8FC8FF, combatBlend));
        fill(pose, x + 2, y, x + WIDTH, y + 1, 0x663D5268);

        String header = combat ? "✦  AOE  /  БОЙ" : "✦  AOE  /  ОБЗОР";
        drawShadow(mc.font, pose, header, x + 11, y + 8, ACCENT);
        String team = PartyHudState.team;
        drawString(pose, mc.font, truncate(mc, team, WIDTH - 22), x + 11, y + 22, WHITE);
        fill(pose, x + 10, y + 35, x + WIDTH - 10, y + 36, 0x554D6073);

        int contentY = y + 43;
        drawString(pose, mc.font, "◷  ДНЕВНАЯ ЦЕЛЬ", x + 11, contentY, GOLD);
        drawString(pose, mc.font, truncate(mc, PartyHudState.daily, WIDTH - 22), x + 11, contentY + 11, MUTED);
        contentY += 27;
        drawString(pose, mc.font, "◇  ЦЕЛЬ НЕДЕЛИ", x + 11, contentY, 0xFF8FC8FF);
        drawString(pose, mc.font, truncate(mc, PartyHudState.weekly, WIDTH - 22), x + 11, contentY + 11, MUTED);

        if (combat && rows > 0) {
            int dividerY = contentY + 26;
            fill(pose, x + 10, dividerY, x + WIDTH - 10, dividerY + 1, 0x554D6073);
            drawString(pose, mc.font, PartyHudState.squadIcon + "  " + PartyHudState.squadName,
                x + 11, dividerY + 6, WHITE);
            double maxDps = PartyHudState.members.stream().mapToDouble(CombatPartyTracker.Member::dps).max().orElse(1);
            int rowY = dividerY + 19;
            for (int i = 0; i < rows; i++) {
                CombatPartyTracker.Member member = PartyHudState.members.get(i);
                int color = 0xFF000000 | (member.color() & 0xFFFFFF);
                fill(pose, x + 11, rowY + 2, x + 15, rowY + 7, color);
                String name = truncate(mc, member.name(), 76);
                drawString(pose, mc.font, name, x + 19, rowY, i == 0 ? WHITE : MUTED);
                String dps = formatDps(member.dps());
                drawString(pose, mc.font, dps, x + WIDTH - 11 - mc.font.width(dps), rowY, i == 0 ? GOLD : WHITE);
                int barX = x + 19;
                int barY = rowY + 10;
                int barW = WIDTH - 32;
                fill(pose, barX, barY, barX + barW, barY + 2, INNER);
                int fillW = Math.max(1, (int) (barW * Math.min(1, member.dps() / Math.max(1, maxDps))));
                fill(pose, barX, barY, barX + fillW, barY + 2, color);
                rowY += 17;
            }
            String live = "LIVE · DPS";
            drawString(pose, mc.font, live, x + WIDTH - 11 - mc.font.width(live), y + height - 11, ACCENT);
        } else {
            String hint = "O скрыть  ·  P положение";
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
        if (dps >= 1_000_000) return String.format(java.util.Locale.ROOT, "%.1fM", dps / 1_000_000);
        if (dps >= 10_000) return String.format(java.util.Locale.ROOT, "%.0fk", dps / 1_000);
        if (dps >= 1_000) return String.format(java.util.Locale.ROOT, "%.1fk", dps / 1_000);
        return String.format(java.util.Locale.ROOT, "%.0f", dps);
    }

    private static String truncate(Minecraft mc, String text, int maxWidth) {
        if (text == null || mc.font.width(text) <= maxWidth) return text == null ? "" : text;
        String ellipsis = "…";
        int end = text.length();
        while (end > 0 && mc.font.width(text.substring(0, end) + ellipsis) > maxWidth) end--;
        return text.substring(0, end) + ellipsis;
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("age_of_epoch/party_hud.properties");
    }

    private static void load() {
        try {
            Path path = configPath();
            if (Files.exists(path)) {
                Properties properties = new Properties();
                try (var reader = Files.newBufferedReader(path)) { properties.load(reader); }
                visible = Boolean.parseBoolean(properties.getProperty("visible", "true"));
                position = Math.floorMod(Integer.parseInt(properties.getProperty("position", "0")), 4);
            }
        } catch (Exception ignored) { }
    }

    private static void save() {
        if (System.currentTimeMillis() - lastSave < 100) return;
        lastSave = System.currentTimeMillis();
        try {
            Path path = configPath();
            Files.createDirectories(path.getParent());
            Properties properties = new Properties();
            properties.setProperty("visible", Boolean.toString(visible));
            properties.setProperty("position", Integer.toString(position));
            try (var writer = Files.newBufferedWriter(path)) { properties.store(writer, "Age of Epoch party HUD"); }
        } catch (IOException ignored) { }
    }
}
