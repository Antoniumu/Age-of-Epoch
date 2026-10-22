package com.robertx22.mine_and_slash.gui;

import com.robertx22.forge_compat.common.CompatChatFormatting;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class TextUtils {

    public static void renderText(GuiGraphicsExtractor gui, float scale, String text, int x, int y, ChatFormatting color) {
        Minecraft mc = Minecraft.getInstance();

        int width = mc.font.width(text);
        int textX = (int) (x - width / 2F);
        int textY = y;

        float antiScale = 1.0F / scale;

        gui.pose().scale((float)(scale), (float)(scale));
        double textWidthMinus = (width * antiScale / 2) - width / 2F;// fixed the centering with this!!!
        double textHeightMinus = 9.0D * scale / 2.0D;
        float xp = (float) ((double) textX + textWidthMinus);
        float yp = (float) ((double) textY - textHeightMinus);
        float xf = (float) ((double) xp * antiScale);
        float yf = (float) ((double) yp * antiScale);


        gui.text(mc.font, text, (int) xf, (int) yf, (CompatChatFormatting.getColor(color) == null ? 0xFFFFFFFF : CompatChatFormatting.getColor(color)), true);


        gui.pose().scale((float)(antiScale), (float)(antiScale));
    }

}
