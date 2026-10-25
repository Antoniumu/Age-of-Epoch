package com.robertx22.age_of_exile.a_libraries.mob_plates;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.age_of_exile.config.forge.ClientConfigs;
import com.robertx22.age_of_exile.config.forge.MobPlateNameMode;
import com.robertx22.age_of_exile.config.forge.MobPlateStyle;
import com.robertx22.forge_compat.client.AbstractGui;
import net.minecraft.client.Minecraft;

/**
 * Tiny in-game confirmation shown when the plate settings are cycled with the
 * keybind (J for the style, Shift+J for the way named entities are handled).
 */
public final class MobPlateStyleHud {

    private static long showUntilMs = 0L;
    private static String text = "";

    private MobPlateStyleHud() {
    }

    public static void cycle() {
        MobPlateStyle next = ClientConfigs.getConfig()
            .MOB_PLATE_STYLE.get()
            .next();
        ClientConfigs.getConfig()
            .MOB_PLATE_STYLE.set(next);
        MobPlateSettings.save();
        announce(next);
    }

    /**
     * Cycles how entities with a vanilla name tag are drawn (compact banner above
     * the nickname / full plate above it / ignore the nickname).
     */
    public static void cycleNamedMode() {
        MobPlateNameMode next = ClientConfigs.getConfig()
            .MOB_PLATE_NAMED_MODE.get()
            .next();
        ClientConfigs.getConfig()
            .MOB_PLATE_NAMED_MODE.set(next);
        MobPlateSettings.save();
        announce(ClientConfigs.getConfig()
            .MOB_PLATE_STYLE.get());
    }

    public static void announce(MobPlateStyle style) {
        text = "Плашки мобов: " + style.displayName + " · имена: "
            + ClientConfigs.getConfig()
            .MOB_PLATE_NAMED_MODE.get().displayName;
        showUntilMs = System.currentTimeMillis() + 2600L;
    }

    public static void render(PoseStack matrix) {
        if (System.currentTimeMillis() > showUntilMs || text.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.font == null) {
            return;
        }
        int width = mc.getWindow()
            .getGuiScaledWidth();
        int height = mc.getWindow()
            .getGuiScaledHeight();
        int textWidth = mc.font.width(text);
        int x = width / 2 - textWidth / 2;
        int y = height - 70;

        AbstractGui.fill(matrix, x - 7, y - 3, x + textWidth + 7, y + 11, 0x99101418);
        AbstractGui.drawShadow(mc.font, matrix, text, x, y, 0xFFFFD98A);
    }
}
