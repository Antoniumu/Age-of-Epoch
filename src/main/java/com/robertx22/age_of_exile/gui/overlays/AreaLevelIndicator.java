package com.robertx22.age_of_exile.gui.overlays;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.age_of_exile.mmorpg.SyncedToClientValues;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.library_of_exile.utils.GuiUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;

public class AreaLevelIndicator {

    public static void draw(PoseStack matrix, int x, int y) {

        Minecraft mc = Minecraft.getInstance();

        String text = "Уровень зоны: ";

        String lvltext = SyncedToClientValues.areaLevel + "";

        int arealvl = SyncedToClientValues.areaLevel;
        TextColors color = TextColors.GREEN;

        int playerlvl = Load.Unit(mc.player)
            .getLevel();

        if (arealvl - 10 > playerlvl) {
            color = TextColors.RED;
            lvltext += "!!";
        } else if (arealvl - 5 > playerlvl) {
            color = TextColors.YELLOW;
            lvltext += "!";
        }
        text += lvltext;

        int width = mc.font.width(text);

        drawPrettyLevelText(text, matrix, x - width / 2F, y + 12, color);
    }

    public enum TextColors {
        RED(13313553),
        YELLOW(15252255),
        GREEN(8453920);
        public int color;

        TextColors(int color) {
            this.color = color;
        }
    }

    public static void drawPrettyLevelText(String string, PoseStack matrix, float m, float n, TextColors color) {
        GuiUtils.renderScaledText(matrix, (int) m, (int) n, 1.0D, string, ChatFormatting.WHITE);
    }

}
