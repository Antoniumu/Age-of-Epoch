package com.robertx22.forge_compat.client;

import java.util.function.Supplier;

public class RenderSystem {

    public static void color4f(float r, float g, float b, float a) {}

    public static void color3f(float r, float g, float b) {}

    public static void enableBlend() {}

    public static void disableBlend() {}

    public static void defaultBlendFunc() {}

    public static void blendFunc(int sfactor, int dfactor) {}

    public static void blendFuncSeparate(int srcRGB, int dstRGB, int srcAlpha, int dstAlpha) {}

    public static void enableDepthTest() {}

    public static void disableDepthTest() {}

    public static void enableAlphaTest() {}

    public static void disableAlphaTest() {}

    public static void enableLighting() {}

    public static void disableLighting() {}

    public static void shadeModel(int mode) {}

    public static void scaled(double x, double y, double z) {}

    public static void scalef(float x, float y, float z) {}

    public static void translated(double x, double y, double z) {}

    public static void translatef(float x, float y, float z) {}

    public static void pushMatrix() {}

    public static void popMatrix() {}

    public static void assertThread(Supplier<Boolean> check) {}

    public static boolean isOnRenderThread() { return true; }
}
