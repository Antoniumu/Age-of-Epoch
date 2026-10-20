package com.robertx22.age_of_exile.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.forge_compat.client.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.ChatFormatting;

public class TextUtils {

    public static void renderText(PoseStack matrix, double scale, String text, int x, int y, ChatFormatting color) {
        Minecraft mc = Minecraft.getInstance();

        int width = mc.font.width(text);
        int textX = (int) (x - width / 2F);
        int textY = y;

        double antiScale = 1.0D / scale;

        RenderSystem.scaled(scale, scale, scale);
        double textWidthMinus = (width * antiScale / 2) - width / 2F;// fixed the centering with this!!!
        double textHeightMinus = 9.0D * scale / 2.0D;
        float xp = (float) ((double) textX + textWidthMinus);
        float yp = (float) ((double) textY - textHeightMinus);
        float xf = (float) ((double) xp * antiScale);
        float yf = (float) ((double) yp * antiScale);

        com.robertx22.forge_compat.client.AbstractGui.drawShadow(mc.font, matrix, text, xf, yf, com.robertx22.forge_compat.common.CompatChatFormatting.getColor(color));
        RenderSystem.scaled(antiScale, antiScale, antiScale);
    }

}
