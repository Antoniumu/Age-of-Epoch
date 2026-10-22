package com.robertx22.forge_compat.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public final class GuiBlitHelper {
    public static void blit(GuiGraphicsExtractor g, Identifier tex, int x, int y, int u, int v, int w, int h) {
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, (float) u, (float) v, w, h, 256, 256);
    }

    public static void blit(GuiGraphicsExtractor g, Identifier tex, int x, int y, float u, float v, int w, int h, int tw, int th) {
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, u, v, w, h, tw, th);
    }

    public static void blit(GuiGraphicsExtractor g, Identifier tex, int x, int y, int z, float u, float v, int w, int h, int tw, int th) {
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, u, v, w, h, tw, th);
    }

    public static void blit(GuiGraphicsExtractor g, Identifier tex, int x, int y, int w, int h, float u, float v, int rw, int rh, int tw, int th) {
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, u, v, w, h, rw, rh, tw, th);
    }

    public static void blit(GuiGraphicsExtractor g, Identifier tex, int x, int y, int z, int w, int h, float u, float v, int rw, int rh, int tw, int th) {
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, u, v, w, h, rw, rh, tw, th);
    }

    public static void drawString(GuiGraphicsExtractor g, Font font, String text, int x, int y, int color) {
        g.text(font, text, x, y, color, true);
    }

    public static void drawString(GuiGraphicsExtractor g, Font font, String text, int x, int y, int color, boolean shadow) {
        g.text(font, text, x, y, color, shadow);
    }

    public static void drawString(GuiGraphicsExtractor g, Font font, Component text, int x, int y, int color) {
        g.text(font, text, x, y, color, true);
    }

    public static void drawString(GuiGraphicsExtractor g, Font font, Component text, int x, int y, int color, boolean shadow) {
        g.text(font, text, x, y, color, shadow);
    }

    public static void drawString(GuiGraphicsExtractor g, Font font, FormattedCharSequence text, int x, int y, int color) {
        g.text(font, text, x, y, color, true);
    }

    public static void drawString(GuiGraphicsExtractor g, Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow) {
        g.text(font, text, x, y, color, shadow);
    }

    public static void drawCenteredString(GuiGraphicsExtractor g, Font font, String text, int x, int y, int color) {
        g.centeredText(font, Component.literal(text), x, y, color);
    }

    public static void drawCenteredString(GuiGraphicsExtractor g, Font font, Component text, int x, int y, int color) {
        g.centeredText(font, text, x, y, color);
    }

    public static void drawCenteredString(GuiGraphicsExtractor g, Font font, FormattedCharSequence text, int x, int y, int color) {
        g.centeredText(font, text, x, y, color);
    }

    public static void renderTooltip(GuiGraphicsExtractor g, Font font, ItemStack stack, int x, int y) {
        g.setTooltipForNextFrame(font, stack, x, y);
    }

    public static void renderTooltip(GuiGraphicsExtractor g, Font font, Component text, int x, int y) {
        g.setTooltipForNextFrame(font, text, x, y);
    }

    public static void renderTooltip(GuiGraphicsExtractor g, Font font, List<? extends FormattedCharSequence> lines, int x, int y) {
        g.setTooltipForNextFrame(font, List.copyOf(lines), x, y);
    }

    public static void renderTooltip(GuiGraphicsExtractor g, Font font, List<Component> lines, Optional<TooltipComponent> comp, int x, int y) {
        g.setComponentTooltipForNextFrame(font, lines, x, y);
    }

    public static void renderComponentTooltip(GuiGraphicsExtractor g, Font font, List<Component> lines, int x, int y) {
        g.setComponentTooltipForNextFrame(font, lines, x, y);
    }
}
