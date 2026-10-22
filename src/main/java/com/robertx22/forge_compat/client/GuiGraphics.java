package com.robertx22.forge_compat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public class GuiGraphics {
    private final GuiGraphicsExtractor extractor;
    private final PoseStack compatPose = new PoseStack();
    private float r = 1f, g = 1f, b = 1f, a = 1f;

    public GuiGraphics(GuiGraphicsExtractor extractor) {
        this.extractor = extractor;
    }

    public GuiGraphics(Minecraft mc, Object bufferSource) {
        // MC 26.3: GuiGraphicsExtractor has no default constructor anymore
        this.extractor = new GuiGraphicsExtractor(mc, new net.minecraft.client.renderer.state.gui.GuiRenderState(), 0, 0);
    }

    public static GuiGraphics wrap(GuiGraphicsExtractor ext) {
        return new GuiGraphics(ext);
    }

    public GuiGraphicsExtractor unwrap() {
        return extractor;
    }

    public PoseStack pose() {
        return new PoseStack() {
            @Override public void pushPose() { super.pushPose(); if (extractor != null) extractor.pose().pushMatrix(); }
            @Override public void popPose() { super.popPose(); if (extractor != null) extractor.pose().popMatrix(); }
            @Override public void translate(float x, float y, float z) { super.translate(x, y, z); if (extractor != null) extractor.pose().translate(x, y); }
            @Override public void translate(double x, double y, double z) { super.translate(x, y, z); if (extractor != null) extractor.pose().translate((float) x, (float) y); }
            @Override public void scale(float x, float y, float z) { super.scale(x, y, z); if (extractor != null) extractor.pose().scale(x, y); }
        };
    }

    public int guiWidth() { return extractor != null ? extractor.guiWidth() : Minecraft.getInstance().getWindow().getGuiScaledWidth(); }
    public int guiHeight() { return extractor != null ? extractor.guiHeight() : Minecraft.getInstance().getWindow().getGuiScaledHeight(); }

    public void setColor(float r, float g, float b, float a) {
        this.r = r; this.g = g; this.b = b; this.a = a;
        RenderSystem.setShaderColor(r, g, b, a);
    }

    private int tintColor() {
        int ia = Math.clamp((int) (a * 255f), 0, 255);
        int ir = Math.clamp((int) (r * 255f), 0, 255);
        int ig = Math.clamp((int) (g * 255f), 0, 255);
        int ib = Math.clamp((int) (b * 255f), 0, 255);
        return (ia << 24) | (ir << 16) | (ig << 8) | ib;
    }

    public void blit(Identifier tex, int x, int y, int u, int v, int width, int height) {
        blit(tex, x, y, (float) u, (float) v, width, height, 256, 256);
    }

    public void blit(Identifier tex, int x, int y, float u, float v, int width, int height, int texWidth, int texHeight) {
        if (extractor != null) {
            extractor.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, u, v, width, height, texWidth, texHeight, tintColor());
        }
    }

    public void blit(Identifier tex, int x, int y, int blitOffset, float u, float v, int width, int height, int texWidth, int texHeight) {
        blit(tex, x, y, u, v, width, height, texWidth, texHeight);
    }

    public void blit(Identifier tex, int x, int y, int width, int height, float u, float v, int regionWidth, int regionHeight, int texWidth, int texHeight) {
        if (extractor != null) {
            extractor.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, u, v, width, height, regionWidth, regionHeight, texWidth, texHeight, tintColor());
        }
    }

    public void blitNineSliced(Identifier tex, int x, int y, int width, int height, int slice, int uWidth, int vHeight, int uOffset, int vOffset) {
        blit(tex, x, y, (float) uOffset, (float) vOffset, width, height, 256, 256);
    }

    public void blitNineSliced(Identifier tex, int x, int y, int width, int height, int sliceW, int sliceH, int uWidth, int vHeight, int uOffset, int vOffset) {
        blit(tex, x, y, (float) uOffset, (float) vOffset, width, height, 256, 256);
    }

    public void fill(int x1, int y1, int x2, int y2, int color) {
        if (extractor != null) extractor.fill(x1, y1, x2, y2, color);
    }

    public void fillGradient(int x1, int y1, int x2, int y2, int c1, int c2) {
        if (extractor != null) extractor.fillGradient(x1, y1, x2, y2, c1, c2);
    }

    public void renderOutline(int x, int y, int width, int height, int color) {
        if (extractor != null) extractor.outline(x, y, width, height, color);
    }

    public void enableScissor(int x1, int y1, int x2, int y2) {
        if (extractor != null) extractor.enableScissor(x1, y1, x2, y2);
    }

    public void disableScissor() {
        if (extractor != null) extractor.disableScissor();
    }

    public int drawString(Font font, String text, int x, int y, int color, boolean shadow) {
        if (extractor != null && text != null) extractor.text(font, text, x, y, (color & 0xFF000000) == 0 ? (color | 0xFF000000) : color, shadow);
        return x + (font != null && text != null ? font.width(text) : 0);
    }

    public int drawString(Font font, String text, float x, float y, int color, boolean shadow) {
        return drawString(font, text, (int) x, (int) y, color, shadow);
    }

    public int drawString(Font font, String text, int x, int y, int color) {
        return drawString(font, text, x, y, color, true);
    }

    public int drawString(Font font, Component text, int x, int y, int color, boolean shadow) {
        if (extractor != null && text != null) extractor.text(font, text, x, y, (color & 0xFF000000) == 0 ? (color | 0xFF000000) : color, shadow);
        return x + (font != null && text != null ? font.width(text) : 0);
    }

    public int drawString(Font font, Component text, int x, int y, int color) {
        return drawString(font, text, x, y, color, true);
    }

    public int drawString(Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow) {
        if (extractor != null && text != null) extractor.text(font, text, x, y, (color & 0xFF000000) == 0 ? (color | 0xFF000000) : color, shadow);
        return x + (font != null && text != null ? font.width(text) : 0);
    }

    public int drawString(Font font, FormattedCharSequence text, int x, int y, int color) {
        return drawString(font, text, x, y, color, true);
    }

    public void drawCenteredString(Font font, Component text, int x, int y, int color) {
        if (extractor != null && text != null) extractor.centeredText(font, text, x, y, (color & 0xFF000000) == 0 ? (color | 0xFF000000) : color);
    }

    public void drawCenteredString(Font font, String text, int x, int y, int color) {
        if (extractor != null && text != null) extractor.centeredText(font, text, x, y, (color & 0xFF000000) == 0 ? (color | 0xFF000000) : color);
    }

    public void drawWordWrap(Font font, FormattedText text, int x, int y, int lineWidth, int color) {
        if (extractor != null && text != null) extractor.textWithWordWrap(font, text, x, y, lineWidth, (color & 0xFF000000) == 0 ? (color | 0xFF000000) : color, false);
    }

    public void renderItem(ItemStack stack, int x, int y) {
        if (extractor != null && stack != null && !stack.isEmpty()) extractor.item(stack, x, y);
    }

    public void renderFakeItem(ItemStack stack, int x, int y) {
        if (extractor != null && stack != null && !stack.isEmpty()) extractor.fakeItem(stack, x, y);
    }

    public void renderItem(LivingEntity entity, ItemStack stack, int x, int y, int seed) {
        if (extractor != null && stack != null && !stack.isEmpty()) extractor.item(entity, stack, x, y, seed);
    }

    public void renderItemDecorations(Font font, ItemStack stack, int x, int y) {
        if (extractor != null && stack != null && !stack.isEmpty()) extractor.itemDecorations(font, stack, x, y);
    }

    public void renderItemDecorations(Font font, ItemStack stack, int x, int y, String countText) {
        if (extractor != null && stack != null && !stack.isEmpty()) extractor.itemDecorations(font, stack, x, y, countText);
    }

    public void renderTooltip(Font font, ItemStack stack, int x, int y) {
        if (extractor != null && stack != null && !stack.isEmpty()) extractor.setTooltipForNextFrame(font, stack, x, y);
    }

    public void renderTooltip(Font font, Component text, int x, int y) {
        if (extractor != null && text != null) extractor.setTooltipForNextFrame(font, text, x, y);
    }

    public void renderTooltip(Font font, List<FormattedCharSequence> lines, int x, int y) {
        if (extractor != null && lines != null && !lines.isEmpty()) extractor.setTooltipForNextFrame(font, lines, x, y);
    }

    public void renderTooltip(Font font, List<Component> lines, Optional<TooltipComponent> image, int x, int y) {
        if (extractor != null && lines != null && !lines.isEmpty()) extractor.setTooltipForNextFrame(font, lines, image, x, y);
    }

    public void renderComponentTooltip(Font font, List<Component> lines, int x, int y) {
        if (extractor != null && lines != null && !lines.isEmpty()) extractor.setComponentTooltipForNextFrame(font, lines, x, y);
    }

    public void flush() {}
}
