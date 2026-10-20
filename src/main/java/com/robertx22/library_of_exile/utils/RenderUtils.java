package com.robertx22.library_of_exile.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.forge_compat.client.AbstractGui;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.lang.reflect.Method;

public class RenderUtils {

    private static Method GUI_ITEM_METHOD;
    private static Method GUI_ITEM_DECO_METHOD;
    private static boolean ITEM_METHODS_SEARCHED = false;

    private static Method INV_ENTITY_METHOD;
    private static boolean INV_ENTITY_SEARCHED = false;

    public static void render16Icon(PoseStack matrix, Identifier tex, int x, int y) {
        AbstractGui.bindTexture(tex);
        AbstractGui.blit(matrix, x, y, 0, 0, 16, 16, 16, 16);
    }

    public static void renderStack(ItemStack stack, int x, int y) {
        if (stack == null || stack.isEmpty()) return;
        GuiGraphicsExtractor g = AbstractGui.currentGraphics();
        if (g == null) return;

        if (!ITEM_METHODS_SEARCHED) {
            ITEM_METHODS_SEARCHED = true;
            for (Method m : GuiGraphicsExtractor.class.getMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 3 && p[0] == ItemStack.class && p[1] == int.class && p[2] == int.class) {
                    String name = m.getName();
                    if (name.equals("item") || name.equals("renderItem") || name.equals(" renderFakeItem") || GUI_ITEM_METHOD == null) {
                        GUI_ITEM_METHOD = m;
                        if (name.equals("item") || name.equals("renderItem")) break;
                    }
                }
            }
            for (Method m : GuiGraphicsExtractor.class.getMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 4 && p[0] == Font.class && p[1] == ItemStack.class && p[2] == int.class && p[3] == int.class) {
                    GUI_ITEM_DECO_METHOD = m;
                    break;
                }
            }
        }

        try {
            if (GUI_ITEM_METHOD != null) {
                GUI_ITEM_METHOD.invoke(g, stack, x, y);
            }
            if (GUI_ITEM_DECO_METHOD != null) {
                GUI_ITEM_DECO_METHOD.invoke(g, Minecraft.getInstance().font, stack, x, y);
            }
        } catch (Throwable ignored) {
        }
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

    public static void renderintoWorld(ItemStack stack, Vec3 pos, float size) {
    }

    public static void rotateToPlayer(PoseStack matrix) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Quaternionf cam = camera.rotation();
        matrix.mulPose(cam);
    }

    public static Vec3 getInterpolatedPos(Entity entity, float ticks) {
        double x = Mth.lerp(ticks, entity.xo, entity.getX());
        double y = Mth.lerp(ticks, entity.yo, entity.getY());
        double z = Mth.lerp(ticks, entity.zo, entity.getZ());
        return new Vec3(x, y, z);
    }
}
