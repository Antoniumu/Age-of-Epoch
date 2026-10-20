package com.robertx22.forge_compat.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.function.Supplier;

public class RenderSystem {

    public static void pushMatrix() {
        GuiGraphicsExtractor g = AbstractGui.CURRENT_GRAPHICS;
        if (g != null) {
            g.pose().pushMatrix();
        }
    }

    public static void popMatrix() {
        GuiGraphicsExtractor g = AbstractGui.CURRENT_GRAPHICS;
        if (g != null) {
            try {
                g.pose().popMatrix();
            } catch (Throwable ignored) {
            }
        }
    }

    public static void translated(double x, double y, double z) {
        GuiGraphicsExtractor g = AbstractGui.CURRENT_GRAPHICS;
        if (g != null) {
            g.pose().translate((float) x, (float) y);
        }
    }

    public static void translatef(float x, float y, float z) {
        GuiGraphicsExtractor g = AbstractGui.CURRENT_GRAPHICS;
        if (g != null) {
            g.pose().translate(x, y);
        }
    }

    public static void scaled(double x, double y, double z) {
        GuiGraphicsExtractor g = AbstractGui.CURRENT_GRAPHICS;
        if (g != null) {
            g.pose().scale((float) x, (float) y);
        }
    }

    public static void scalef(float x, float y, float z) {
        GuiGraphicsExtractor g = AbstractGui.CURRENT_GRAPHICS;
        if (g != null) {
            g.pose().scale(x, y);
        }
    }

    public static void rotatef(float angle, float x, float y, float z) {}

    public static void color4f(float r, float g, float b, float a) {}

    public static void color3f(float r, float g, float b) {}

    public static void clearColor(float r, float g, float b, float a) {}

    public static void enableBlend() {}

    public static void disableBlend() {}

    public static void defaultBlendFunc() {}

    public static void blendFunc(int sfactor, int dfactor) {}

    public static void blendFuncSeparate(int srcRGB, int dstRGB, int srcAlpha, int dstAlpha) {}

    public static void enableDepthTest() {}

    public static void disableDepthTest() {}

    public static void depthMask(boolean flag) {}

    public static void enableAlphaTest() {}

    public static void disableAlphaTest() {}

    public static void alphaFunc(int func, float ref) {}

    public static void enableLighting() {}

    public static void disableLighting() {}

    public static void enableTexture() {}

    public static void disableTexture() {}

    public static void enableRescaleNormal() {}

    public static void disableRescaleNormal() {}

    public static void shadeModel(int mode) {}

    public static void assertThread(Supplier<Boolean> check) {}

    public static boolean isOnRenderThread() {
        return true;
    }
}
