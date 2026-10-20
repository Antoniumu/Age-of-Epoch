package com.robertx22.library_of_exile.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;

public class RenderUtils {

    public static void render16Icon(PoseStack matrix, ResourceLocation tex, int x, int y) {
        Minecraft.getInstance()
            .getTextureManager()
            .bind(tex);

        Gui.blit(matrix, x, y, 0, 0, 16, 16, 16, 16);
    }

    public static void renderStack(ItemStack stack, int x, int y) {
        Minecraft mc = Minecraft.getInstance();
        RenderSystem.enableDepthTest();
        mc.getItemRenderer()
            .renderAndDecorateFakeItem(stack, x, y);
        mc.getItemRenderer()
            .renderGuiItemDecorations(mc.font, stack, x, y);

        RenderSystem.disableDepthTest();
    }
}