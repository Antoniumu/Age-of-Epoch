package com.robertx22.forge_compat.client;

import net.minecraft.resources.Identifier;

public class RenderSystem {
    public static void enableBlend() {}
    public static void disableBlend() {}
    public static void defaultBlendFunc() {}
    public static void blendFunc(int s, int d) {}
    public static void enableDepthTest() {}
    public static void disableDepthTest() {}
    public static void depthMask(boolean flag) {}
    public static void depthFunc(int func) {}
    public static void setShaderColor(float r, float g, float b, float a) {}
    public static void color4f(float r, float g, float b, float a) {}
    public static void setShaderTexture(int unit, Identifier id) {}
    public static void bindTexture(Identifier id) {}
    public static void setShader(Object s) {}
}
