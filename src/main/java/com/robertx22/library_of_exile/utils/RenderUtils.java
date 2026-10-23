package com.robertx22.library_of_exile.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.forge_compat.client.AbstractGui;
import com.robertx22.forge_compat.client.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;

public class RenderUtils {

    private static Method INV_ENTITY_METHOD;
    private static boolean INV_ENTITY_SEARCHED = false;

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

    public static void renderEntityInInventory(int x1, int y1, int x2, int y2, int scale, float yOffset, float mouseX, float mouseY, LivingEntity entity) {
        if (entity == null) return;
        GuiGraphicsExtractor g = AbstractGui.currentGraphics();
        if (g == null) return;

        if (!INV_ENTITY_SEARCHED) {
            INV_ENTITY_SEARCHED = true;
            for (Method m : InventoryScreen.class.getMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 10
                    && p[0] == GuiGraphicsExtractor.class
                    && p[1] == int.class && p[2] == int.class && p[3] == int.class && p[4] == int.class
                    && p[5] == int.class && p[6] == float.class && p[7] == float.class && p[8] == float.class
                    && LivingEntity.class.isAssignableFrom(p[9])) {
                    INV_ENTITY_METHOD = m;
                    break;
                }
            }
        }

        if (INV_ENTITY_METHOD != null) {
            try {
                INV_ENTITY_METHOD.invoke(null, g, x1, y1, x2, y2, scale, yOffset, mouseX, mouseY, entity);
            } catch (Throwable ignored) {
            }
        }
    }
}
