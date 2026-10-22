package com.robertx22.library_of_exile.utils;

import com.robertx22.forge_compat.client.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public class RenderUtils {

    public static void render16Icon(GuiGraphicsExtractor matrix, Identifier tex, int x, int y) {
        Minecraft.getInstance()
                .getTextureManager()
                ;


        com.robertx22.forge_compat.client.GuiBlitHelper.blit(matrix, tex, x, y, 0, 0, 16, 16, 16, 16);
    }

    public static void renderStack(GuiGraphicsExtractor gui, ItemStack stack, int x, int y) {
        Minecraft mc = Minecraft.getInstance();
        RenderSystem.enableDepthTest();

        
        gui.pose().pushMatrix();

        gui.pose().translate((float)(x), (float)(y));

        //BakedModel bakedmodel = mc.getItemRenderer().getModel(stack, LibClientOnly.getPlayer().level(), (LivingEntity) null, 1);

        gui.item(stack, x, y);

        // mc.getItemRenderer().render(stack, ItemDisplayContext.GROUND, false, gui.pose(), mc.renderBuffers().bufferSource(), 100, OverlayTexture.NO_OVERLAY, bakedmodel);

/*
        mc.getItemRenderer()
                .renderAndDecorateFakeItem(stack, x, y);
        mc.getItemRenderer()
                .renderGuiItemDecorations(mc.font, stack, x, y);

 */

        RenderSystem.disableDepthTest();

        gui.pose().popMatrix();
    }
}