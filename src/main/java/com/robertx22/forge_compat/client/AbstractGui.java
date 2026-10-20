package com.robertx22.forge_compat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

public class AbstractGui {

    public static GuiGraphicsExtractor CURRENT_GRAPHICS;
    public static Identifier BOUND_TEXTURE = Identifier.withDefaultNamespace("textures/gui/widgets.png");

    public AbstractGui() {
    }

    public AbstractGui(net.minecraft.client.Minecraft mc) {
    }

    public static void renderFakeItem(net.minecraft.world.item.ItemStack stack, int x, int y) {
        if (CURRENT_GRAPHICS != null && stack != null) {
            CURRENT_GRAPHICS.item(stack, x, y);
        }
    }

    public static void renderItemDecorations(Font font, net.minecraft.world.item.ItemStack stack, int x, int y) {
        if (CURRENT_GRAPHICS != null && stack != null) {
            CURRENT_GRAPHICS.itemDecorations(font, stack, x, y);
        }
    }

    public static void bindTexture(Identifier id) {
        if (id != null) {
            BOUND_TEXTURE = id;
        }
    }

    public static void blit(PoseStack matrix, int x, int y, int u, int v, int w, int h) {
        blit(matrix, x, y, (float) u, (float) v, w, h, 256, 256);
    }

    public static void blit(PoseStack matrix, int x, int y, float u, float v, int w, int h, int texW, int texH) {
        if (CURRENT_GRAPHICS != null && BOUND_TEXTURE != null) {
            CURRENT_GRAPHICS.blit(RenderPipelines.GUI_TEXTURED, BOUND_TEXTURE, x, y, u, v, w, h, texW, texH);
        }
    }

    public static void blit(PoseStack matrix, int x, int y, int u, int v, int w, int h, int texW, int texH) {
        blit(matrix, x, y, (float) u, (float) v, w, h, texW, texH);
    }

    public static void blit(PoseStack matrix, int x, int y, int z, float u, float v, int w, int h, int texW, int texH) {
        blit(matrix, x, y, u, v, w, h, texW, texH);
    }

    public static void blit(PoseStack matrix, int x, int y, int w, int h, float u, float v, int regionW, int regionH, int texW, int texH) {
        blit(matrix, x, y, u, v, w, h, texW, texH);
    }

    public static void blit(PoseStack matrix, int x, int y, int w, int h, int u, int v, int regionW, int regionH, int texW, int texH) {
        blit(matrix, x, y, w, h, (float) u, (float) v, regionW, regionH, texW, texH);
    }

    public static void fill(PoseStack matrix, int x1, int y1, int x2, int y2, int color) {
        if (CURRENT_GRAPHICS != null) {
            CURRENT_GRAPHICS.fill(x1, y1, x2, y2, color);
        }
    }

    public void fillGradient(PoseStack matrix, int x1, int y1, int x2, int y2, int colorFrom, int colorTo) {
        if (CURRENT_GRAPHICS != null) {
            CURRENT_GRAPHICS.fillGradient(x1, y1, x2, y2, colorFrom, colorTo);
        }
    }

    public static int drawString(Font font, PoseStack matrix, String text, float x, float y, int color) {
        if (CURRENT_GRAPHICS != null && font != null && text != null) {
            CURRENT_GRAPHICS.text(font, text, (int) x, (int) y, color, false);
            return (int) x + font.width(text);
        }
        return (int) x;
    }

    public static int drawString(Font font, PoseStack matrix, Component text, float x, float y, int color) {
        if (CURRENT_GRAPHICS != null && font != null && text != null) {
            CURRENT_GRAPHICS.text(font, text, (int) x, (int) y, color, false);
            return (int) x + font.width(text);
        }
        return (int) x;
    }

    public static int drawString(Font font, PoseStack matrix, FormattedCharSequence text, float x, float y, int color) {
        if (CURRENT_GRAPHICS != null && font != null && text != null) {
            CURRENT_GRAPHICS.text(font, text, (int) x, (int) y, color, false);
            return (int) x + font.width(text);
        }
        return (int) x;
    }

    public static int drawShadow(Font font, PoseStack matrix, String text, float x, float y, int color) {
        if (CURRENT_GRAPHICS != null && font != null && text != null) {
            CURRENT_GRAPHICS.text(font, text, (int) x, (int) y, color, true);
            return (int) x + font.width(text);
        }
        return (int) x;
    }

    public static int drawShadow(Font font, PoseStack matrix, Component text, float x, float y, int color) {
        if (CURRENT_GRAPHICS != null && font != null && text != null) {
            CURRENT_GRAPHICS.text(font, text, (int) x, (int) y, color, true);
            return (int) x + font.width(text);
        }
        return (int) x;
    }

    public static int drawShadow(Font font, PoseStack matrix, FormattedCharSequence text, float x, float y, int color) {
        if (CURRENT_GRAPHICS != null && font != null && text != null) {
            CURRENT_GRAPHICS.text(font, text, (int) x, (int) y, color, true);
            return (int) x + font.width(text);
        }
        return (int) x;
    }

    public static void drawWordWrap(Font font, net.minecraft.network.chat.FormattedText text, int x, int y, int maxWidth, int color) {
        if (CURRENT_GRAPHICS != null && font != null && text != null) {
            CURRENT_GRAPHICS.textWithWordWrap(font, text, x, y, maxWidth, color, false);
        }
    }
}
