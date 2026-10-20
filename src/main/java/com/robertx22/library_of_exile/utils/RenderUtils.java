package com.robertx22.library_of_exile.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.forge_compat.client.AbstractGui;
import com.robertx22.forge_compat.client.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public class RenderUtils {

    public static void render16Icon(PoseStack matrix, Identifier tex, int x, int y) {
        AbstractGui.bindTexture(tex);
        AbstractGui.blit(matrix, x, y, 0, 0, 16, 16, 16, 16);
    }

    public static void renderStack(ItemStack stack, int x, int y) {
        Minecraft mc = Minecraft.getInstance();
        RenderSystem.enableDepthTest();
        AbstractGui.renderFakeItem(stack, x, y);
        AbstractGui.renderItemDecorations(mc.font, stack, x, y);
        RenderSystem.disableDepthTest();
    }
}
