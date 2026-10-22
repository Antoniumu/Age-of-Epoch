package com.robertx22.mine_and_slash.gui.screens.skill_tree;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;

public record BufferInfo(
    float pX1, float pX2, float pY1, float pY2, float pBlitOffset,
    int uWidth, int vHeight, float uOffset, float vOffset, int texWidth, int texHeight,
    Matrix3x2f matrix, RenderInfo renderInfo
) {
    public static BufferInfo of(float pX, float pY, float pBlitOffset, int pUOffset, int pVOffset, int pUWidth, int pVHeight, Matrix3x2f m) {
        return of(pX, pY, pBlitOffset, (float) pUOffset, (float) pVOffset, pUWidth, pVHeight, 256, 256, m);
    }

    public static BufferInfo of(float pX, float pY, int pUOffset, int pVOffset, int pUWidth, int pVHeight, Matrix3x2f m) {
        return of(pX, pY, 0, (float) pUOffset, (float) pVOffset, pUWidth, pVHeight, 256, 256, m);
    }

    public static BufferInfo of(float pX, float pY, float pBlitOffset, float pUOffset, float pVOffset, int pUWidth, int pVHeight, int pTextureWidth, int pTextureHeight, Matrix3x2f m) {
        return of(pX, pX + pUWidth, pY, pY + pVHeight, pBlitOffset, pUWidth, pVHeight, pUOffset, pVOffset, pTextureWidth, pTextureHeight, m);
    }

    public static BufferInfo of(float pX, float pY, int pWidth, int pHeight, float pUOffset, float pVOffset, int pUWidth, int pVHeight, int pTextureWidth, int pTextureHeight, Matrix3x2f m) {
        return of(pX, pX + pWidth, pY, pY + pHeight, 0, pUWidth, pVHeight, pUOffset, pVOffset, pTextureWidth, pTextureHeight, m);
    }

    public static BufferInfo of(float pX, float pY, int pWidth, int pHeight, float pBlitOffset, float pUOffset, float pVOffset, int pUWidth, int pVHeight, int pTextureWidth, int pTextureHeight, Matrix3x2f m) {
        return of(pX, pX + pWidth, pY, pY + pHeight, pBlitOffset, pUWidth, pVHeight, pUOffset, pVOffset, pTextureWidth, pTextureHeight, m);
    }

    public static BufferInfo of(float pX, float pY, float pUOffset, float pVOffset, int pWidth, int pHeight, int pTextureWidth, int pTextureHeight, Matrix3x2f m) {
        return of(pX, pY, pWidth, pHeight, pUOffset, pVOffset, pWidth, pHeight, pTextureWidth, pTextureHeight, m);
    }

    public static BufferInfo of(float pX1, float pX2, float pY1, float pY2, float pBlitOffset, int pUWidth, int pVHeight, float pUOffset, float pVOffset, int pTextureWidth, int pTextureHeight, Matrix3x2f m) {
        return new BufferInfo(pX1, pX2, pY1, pY2, pBlitOffset, pUWidth, pVHeight, pUOffset, pVOffset, pTextureWidth, pTextureHeight, new Matrix3x2f(m), new RenderInfo(1.0f));
    }

    public BufferInfo withRenderInfo(RenderInfo renderInfo) {
        return new BufferInfo(pX1, pX2, pY1, pY2, pBlitOffset, uWidth, vHeight, uOffset, vOffset, texWidth, texHeight, matrix, renderInfo);
    }

    public void draw(GuiGraphicsExtractor gui, Identifier texture) {
        if (texture == null) return;
        gui.pose().pushMatrix();
        gui.pose().set(this.matrix);
        gui.blit(
            RenderPipelines.GUI_TEXTURED,
            texture,
            Math.round(pX1),
            Math.round(pY1),
            uOffset,
            vOffset,
            Math.max(1, Math.round(pX2 - pX1)),
            Math.max(1, Math.round(pY2 - pY1)),
            uWidth,
            vHeight,
            Math.max(1, texWidth),
            Math.max(1, texHeight)
        );
        gui.pose().popMatrix();
    }

    public record RenderInfo(float opacity) {}
}
