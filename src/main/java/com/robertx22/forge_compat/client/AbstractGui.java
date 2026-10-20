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
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public class AbstractGui {

    public static GuiGraphicsExtractor CURRENT_GRAPHICS;
    public static Identifier BOUND_TEXTURE = Identifier.withDefaultNamespace("textures/gui/widgets.png");
    public static PoseStack CURRENT_POSE;

    public AbstractGui() {
    }

    public AbstractGui(Minecraft mc) {
    }

    public static void setCurrentGraphics(GuiGraphicsExtractor g) {
        CURRENT_GRAPHICS = g;
    }

    public static GuiGraphicsExtractor currentGraphics() {
        return CURRENT_GRAPHICS;
    }

    public static void setCurrentPose(PoseStack pose) {
        CURRENT_POSE = pose;
    }

    public static PoseStack currentPose() {
        return CURRENT_POSE != null ? CURRENT_POSE : new PoseStack();
    }

    public static void bindTexture(Identifier id) {
        if (id != null) {
            BOUND_TEXTURE = id;
        }
    }

    public static Identifier currentTexture() {
        return BOUND_TEXTURE;
    }

    public static int opaqueColor(int color) {
        return (color & 0xFF000000) == 0 ? (color | 0xFF000000) : color;
    }

    private static boolean pushPoseIfTransformed(GuiGraphicsExtractor g, PoseStack pose) {
        PoseStack active = pose != null ? pose : CURRENT_POSE;
        if (g == null || active == null) return false;
        Matrix4f m = active.last().pose();
        float sx = m.m00();
        float sy = m.m11();
        float tx = m.m30();
        float ty = m.m31();
        if (Math.abs(sx - 1.0f) > 1e-4f || Math.abs(sy - 1.0f) > 1e-4f || Math.abs(tx) > 1e-4f || Math.abs(ty) > 1e-4f) {
            g.pose().pushMatrix();
            if (Math.abs(tx) > 1e-4f || Math.abs(ty) > 1e-4f) {
                g.pose().translate(tx, ty);
            }
            if (Math.abs(sx - 1.0f) > 1e-4f || Math.abs(sy - 1.0f) > 1e-4f) {
                g.pose().scale(sx, sy);
            }
            return true;
        }
        return false;
    }

    private static void popPoseIfTransformed(GuiGraphicsExtractor g, boolean pushed) {
        if (pushed && g != null) {
            g.pose().popMatrix();
        }
    }

    public int getBlitOffset() {
        return 0;
    }

    public void setBlitOffset(int offset) {
    }

    public static void renderFakeItem(ItemStack stack, int x, int y) {
        if (CURRENT_GRAPHICS != null && stack != null && !stack.isEmpty()) {
            CURRENT_GRAPHICS.item(stack, x, y);
        }
    }

    public static void renderItemDecorations(Font font, ItemStack stack, int x, int y) {
        if (CURRENT_GRAPHICS != null && stack != null && !stack.isEmpty()) {
            CURRENT_GRAPHICS.itemDecorations(font, stack, x, y);
        }
    }

    public static void fill(PoseStack matrices, int x1, int y1, int x2, int y2, int color) {
        GuiGraphicsExtractor g = CURRENT_GRAPHICS;
        if (g != null) {
            boolean pushed = pushPoseIfTransformed(g, matrices);
            try {
                g.fill(x1, y1, x2, y2, color);
            } finally {
                popPoseIfTransformed(g, pushed);
            }
        }
    }

    public void fillGradient(PoseStack matrices, int x1, int y1, int x2, int y2, int colorFrom, int colorTo) {
        GuiGraphicsExtractor g = CURRENT_GRAPHICS;
        if (g != null) {
            boolean pushed = pushPoseIfTransformed(g, matrices);
            try {
                g.fillGradient(x1, y1, x2, y2, colorFrom, colorTo);
            } finally {
                popPoseIfTransformed(g, pushed);
            }
        }
    }

    public static void blit(PoseStack matrices, int x, int y, int u, int v, int width, int height) {
        blit(matrices, x, y, width, height, (float) u, (float) v, width, height, 256, 256);
    }

    public static void blit(PoseStack matrices, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        blit(matrices, x, y, width, height, u, v, width, height, textureWidth, textureHeight);
    }

    public static void blit(PoseStack matrices, int x, int y, int u, int v, int width, int height, int textureWidth, int textureHeight) {
        blit(matrices, x, y, width, height, (float) u, (float) v, width, height, textureWidth, textureHeight);
    }

    public static void blit(PoseStack matrices, int x, int y, int z, float u, float v, int width, int height, int textureHeight, int textureWidth) {
        blit(matrices, x, y, width, height, u, v, width, height, textureWidth, textureHeight);
    }

    public static void blit(PoseStack matrices, int x, int y, int width, int height, float u, float v, int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
        GuiGraphicsExtractor g = CURRENT_GRAPHICS;
        Identifier tex = BOUND_TEXTURE;
        if (g != null && tex != null) {
            boolean pushed = pushPoseIfTransformed(g, matrices);
            try {
                g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, u, v, width, height, regionWidth, regionHeight, textureWidth, textureHeight);
            } finally {
                popPoseIfTransformed(g, pushed);
            }
        }
    }

    public static void blit(PoseStack matrices, int x, int y, int width, int height, int u, int v, int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
        blit(matrices, x, y, width, height, (float) u, (float) v, regionWidth, regionHeight, textureWidth, textureHeight);
    }

    public static void drawCenteredString(PoseStack matrices, Font font, String text, int x, int y, int color) {
        GuiGraphicsExtractor g = CURRENT_GRAPHICS;
        if (g != null && font != null && text != null) {
            boolean pushed = pushPoseIfTransformed(g, matrices);
            try {
                g.centeredText(font, text, x, y, opaqueColor(color));
            } finally {
                popPoseIfTransformed(g, pushed);
            }
        }
    }

    public static void drawCenteredString(PoseStack matrices, Font font, Component text, int x, int y, int color) {
        GuiGraphicsExtractor g = CURRENT_GRAPHICS;
        if (g != null && font != null && text != null) {
            boolean pushed = pushPoseIfTransformed(g, matrices);
            try {
                g.centeredText(font, text, x, y, opaqueColor(color));
            } finally {
                popPoseIfTransformed(g, pushed);
            }
        }
    }

    public static void drawString(PoseStack matrices, Font font, String text, int x, int y, int color) {
        GuiGraphicsExtractor g = CURRENT_GRAPHICS;
        if (g != null && font != null && text != null) {
            boolean pushed = pushPoseIfTransformed(g, matrices);
            try {
                g.text(font, text, x, y, opaqueColor(color), true);
            } finally {
                popPoseIfTransformed(g, pushed);
            }
        }
    }

    public static void drawString(PoseStack matrices, Font font, Component text, int x, int y, int color) {
        GuiGraphicsExtractor g = CURRENT_GRAPHICS;
        if (g != null && font != null && text != null) {
            boolean pushed = pushPoseIfTransformed(g, matrices);
            try {
                g.text(font, text, x, y, opaqueColor(color), true);
            } finally {
                popPoseIfTransformed(g, pushed);
            }
        }
    }

    public static void drawString(PoseStack matrices, Font font, FormattedCharSequence text, int x, int y, int color) {
        GuiGraphicsExtractor g = CURRENT_GRAPHICS;
        if (g != null && font != null && text != null) {
            boolean pushed = pushPoseIfTransformed(g, matrices);
            try {
                g.text(font, text, x, y, opaqueColor(color), true);
            } finally {
                popPoseIfTransformed(g, pushed);
            }
        }
    }

    public static int drawString(Font font, PoseStack matrices, String text, float x, float y, int color) {
        GuiGraphicsExtractor g = CURRENT_GRAPHICS;
        if (g != null && font != null && text != null) {
            boolean pushed = pushPoseIfTransformed(g, matrices);
            try {
                g.text(font, text, (int) x, (int) y, opaqueColor(color), false);
            } finally {
                popPoseIfTransformed(g, pushed);
            }
            return (int) x + font.width(text);
        }
        return (int) x;
    }

    public static int drawString(Font font, PoseStack matrices, Component text, float x, float y, int color) {
        GuiGraphicsExtractor g = CURRENT_GRAPHICS;
        if (g != null && font != null && text != null) {
            boolean pushed = pushPoseIfTransformed(g, matrices);
            try {
                g.text(font, text, (int) x, (int) y, opaqueColor(color), false);
            } finally {
                popPoseIfTransformed(g, pushed);
            }
            return (int) x + font.width(text);
        }
        return (int) x;
    }

    public static int drawString(Font font, PoseStack matrices, FormattedCharSequence text, float x, float y, int color) {
        GuiGraphicsExtractor g = CURRENT_GRAPHICS;
        if (g != null && font != null && text != null) {
            boolean pushed = pushPoseIfTransformed(g, matrices);
            try {
                g.text(font, text, (int) x, (int) y, opaqueColor(color), false);
            } finally {
                popPoseIfTransformed(g, pushed);
            }
            return (int) x + font.width(text);
        }
        return (int) x;
    }

    public static int drawShadow(Font font, PoseStack matrices, String text, float x, float y, int color) {
        GuiGraphicsExtractor g = CURRENT_GRAPHICS;
        if (g != null && font != null && text != null) {
            boolean pushed = pushPoseIfTransformed(g, matrices);
            try {
                g.text(font, text, (int) x, (int) y, opaqueColor(color), true);
            } finally {
                popPoseIfTransformed(g, pushed);
            }
            return (int) x + font.width(text);
        }
        return (int) x;
    }

    public static int drawShadow(Font font, PoseStack matrices, Component text, float x, float y, int color) {
        GuiGraphicsExtractor g = CURRENT_GRAPHICS;
        if (g != null && font != null && text != null) {
            boolean pushed = pushPoseIfTransformed(g, matrices);
            try {
                g.text(font, text, (int) x, (int) y, opaqueColor(color), true);
            } finally {
                popPoseIfTransformed(g, pushed);
            }
            return (int) x + font.width(text);
        }
        return (int) x;
    }

    public static int drawShadow(Font font, PoseStack matrices, FormattedCharSequence text, float x, float y, int color) {
        GuiGraphicsExtractor g = CURRENT_GRAPHICS;
        if (g != null && font != null && text != null) {
            boolean pushed = pushPoseIfTransformed(g, matrices);
            try {
                g.text(font, text, (int) x, (int) y, opaqueColor(color), true);
            } finally {
                popPoseIfTransformed(g, pushed);
            }
            return (int) x + font.width(text);
        }
        return (int) x;
    }

    public static void drawWordWrap(Font font, FormattedText text, int x, int y, int maxWidth, int color) {
        if (CURRENT_GRAPHICS != null && font != null && text != null) {
            CURRENT_GRAPHICS.textWithWordWrap(font, text, x, y, maxWidth, opaqueColor(color), false);
        }
    }

    public static void renderTooltip(PoseStack matrices, List<? extends FormattedCharSequence> lines, int mouseX, int mouseY) {
        GuiGraphicsExtractor g = CURRENT_GRAPHICS;
        if (g == null || lines == null || lines.isEmpty()) return;
        Font font = Minecraft.getInstance().font;
        if (font == null) return;
        g.setTooltipForNextFrame(font, new ArrayList<>(lines), mouseX, mouseY);
    }

    public static void renderComponentTooltip(PoseStack matrices, List<Component> lines, int mouseX, int mouseY) {
        GuiGraphicsExtractor g = CURRENT_GRAPHICS;
        if (g == null || lines == null || lines.isEmpty()) return;
        Font font = Minecraft.getInstance().font;
        if (font == null) return;
        g.setComponentTooltipForNextFrame(font, new ArrayList<>(lines), mouseX, mouseY);
    }
}
