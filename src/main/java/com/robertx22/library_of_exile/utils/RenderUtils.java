package com.robertx22.library_of_exile.utils;

import com.robertx22.forge_compat.client.GuiBlitHelper;
import com.robertx22.forge_compat.client.RenderSystem;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public class RenderUtils {
    public static void render16Icon(GuiGraphicsExtractor matrix, Identifier tex, int x, int y) {
        GuiBlitHelper.blit(matrix, tex, x, y, 0, 0, 16, 16, 16, 16);
    }

    public static void renderStack(GuiGraphicsExtractor gui, ItemStack stack, int x, int y) {
        RenderSystem.enableDepthTest();
        gui.pose().pushMatrix();
        gui.pose().translate((float) x, (float) y);
        gui.item(stack, x, y);
        RenderSystem.disableDepthTest();
        gui.pose().popMatrix();
    }
}
