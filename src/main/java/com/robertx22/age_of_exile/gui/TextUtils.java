package com.robertx22.age_of_exile.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.forge_compat.client.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.ChatFormatting;

public class TextUtils {

    public static void renderText(PoseStack matrix, double scale, String text, int x, int y, ChatFormatting color) {
        com.robertx22.library_of_exile.utils.GuiUtils.renderScaledText(matrix, x, y, scale, text, color);
    }

}
