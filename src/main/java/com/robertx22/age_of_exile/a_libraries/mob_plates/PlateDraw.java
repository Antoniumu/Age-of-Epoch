package com.robertx22.age_of_exile.a_libraries.mob_plates;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;

/**
 * Small vertex-level drawing toolkit used by the mob plates.
 *
 * <p>Everything is emitted in "plate pixels": the caller has already set up a
 * billboard pose where 1 unit == 1 font pixel, +X points right and +Y points
 * down. The {@link VertexConsumer} is expected to come from
 * {@code submitCustomGeometry} with a colour-only render type.</p>
 */
public final class PlateDraw {

    /**
     * Depth of the layer currently being drawn, in plate pixels. Successive layers
     * are pulled a hair towards the camera so that translucent, coplanar quads
     * never share a depth value (which would make the upload-time depth sort
     * shuffle them). Set via {@link #layer(float)} before each group of shapes.
     */
    private static float layer = 0.0F;

    private PlateDraw() {
    }

    /**
     * Selects the depth offset for the shapes that follow. Smaller (more negative)
     * values are further away from the camera.
     */
    public static void layer(float z) {
        layer = z;
    }

    // ------------------------------------------------------------------
    // primitives
    // ------------------------------------------------------------------

    public static void rect(PoseStack.Pose pose, VertexConsumer buf, float x0, float y0, float x1, float y1, int color) {
        rect(pose, buf, x0, y0, x1, y1, color, color);
    }

    public static void rect(PoseStack.Pose pose, VertexConsumer buf, float x0, float y0, float x1, float y1, int top, int bottom) {
        if (x1 <= x0 || y1 <= y0) {
            return;
        }
        vertex(pose, buf, x0, y0, top);
        vertex(pose, buf, x0, y1, bottom);
        vertex(pose, buf, x1, y1, bottom);
        vertex(pose, buf, x1, y0, top);
    }

    /**
     * A rectangle with chamfered corners. The rounding is built from single-pixel
     * rows, which keeps the vertex count low and gives a soft, "anti-aliased-ish"
     * silhouette at the distances plates are normally seen from.
     */
    public static void rounded(PoseStack.Pose pose, VertexConsumer buf, float x0, float y0, float x1, float y1, int radius, int color) {
        rounded(pose, buf, x0, y0, x1, y1, radius, color, color);
    }

    public static void rounded(PoseStack.Pose pose, VertexConsumer buf, float x0, float y0, float x1, float y1, int radius,
                               int top, int bottom) {
        if (x1 <= x0 || y1 <= y0) {
            return;
        }
        float height = y1 - y0;
        int r = (int) Math.min(radius, Math.min(height / 2f, (x1 - x0) / 2f));
        if (r <= 1) {
            rect(pose, buf, x0, y0, x1, y1, top, bottom);
            return;
        }

        for (int i = 0; i < r; i++) {
            int inset = cornerInset(r, i);
            float ya = y0 + i;
            float yb = ya + 1f;
            // top row
            rect(pose, buf, x0 + inset, ya, x1 - inset, yb,
                lerp(top, bottom, (ya - y0) / height), lerp(top, bottom, (yb - y0) / height));
            // mirrored bottom row
            float yb2 = y1 - i;
            float ya2 = yb2 - 1f;
            rect(pose, buf, x0 + inset, ya2, x1 - inset, yb2,
                lerp(top, bottom, (ya2 - y0) / height), lerp(top, bottom, (yb2 - y0) / height));
        }

        if (y1 - r > y0 + r) {
            rect(pose, buf, x0, y0 + r, x1, y1 - r,
                lerp(top, bottom, r / height), lerp(top, bottom, (height - r) / height));
        }
    }

    /**
     * A tapered pointer used to visually attach a plate to the mob below it.
     */
    public static void tail(PoseStack.Pose pose, VertexConsumer buf, float centerX, float topY, float height, float width,
                            int top, int bottom) {
        int rows = 3;
        for (int i = 0; i < rows; i++) {
            float t0 = i / (float) rows;
            float t1 = (i + 1) / (float) rows;
            float half0 = width * (1f - t0) / 2f;
            float half1 = width * (1f - t1) / 2f;
            rect(pose, buf, centerX - half0, topY + height * t0, centerX + half0, topY + height * t1,
                lerp(top, bottom, t0), lerp(top, bottom, t1));
        }
    }

    /**
     * A small faceted diamond used for the ornamental style / bar end caps.
     */
    public static void diamond(PoseStack.Pose pose, VertexConsumer buf, float centerX, float centerY, float size,
                               int top, int bottom) {
        float h = size * 0.5f;
        rect(pose, buf, centerX - size * 0.16f, centerY - h, centerX + size * 0.16f, centerY - h * 0.34f, top);
        rect(pose, buf, centerX - size * 0.40f, centerY - h * 0.34f, centerX + size * 0.40f, centerY + h * 0.34f, top, bottom);
        rect(pose, buf, centerX - size * 0.16f, centerY + h * 0.34f, centerX + size * 0.16f, centerY + h, bottom);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buf, float x, float y, int color) {
        buf.addVertex(pose, x, y, layer)
            .setColor(color);
    }

    private static int cornerInset(int radius, int row) {
        float dy = radius - row - 0.5f;
        float dx = (float) Math.sqrt(Math.max(0f, radius * (float) radius - dy * dy));
        return Math.max(0, Math.round(radius - dx));
    }

    // ------------------------------------------------------------------
    // colours
    // ------------------------------------------------------------------

    public static int alpha(int color, int alpha) {
        return (Mth.clamp(alpha, 0, 255) << 24) | (color & 0xFFFFFF);
    }

    public static int lerp(int from, int to, float t) {
        t = Mth.clamp(t, 0f, 1f);
        int a = (int) Mth.lerp(t, (from >>> 24) & 0xFF, (to >>> 24) & 0xFF);
        int r = (int) Mth.lerp(t, (from >> 16) & 0xFF, (to >> 16) & 0xFF);
        int g = (int) Mth.lerp(t, (from >> 8) & 0xFF, (to >> 8) & 0xFF);
        int b = (int) Mth.lerp(t, from & 0xFF, to & 0xFF);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * Multiplies the rgb channels while keeping the original alpha.
     */
    public static int shade(int color, float mul) {
        int a = (color >>> 24) & 0xFF;
        int r = (int) Mth.clamp(((color >> 16) & 0xFF) * mul, 0, 255);
        int g = (int) Mth.clamp(((color >> 8) & 0xFF) * mul, 0, 255);
        int b = (int) Mth.clamp((color & 0xFF) * mul, 0, 255);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int brighten(int color, float amount) {
        int a = (color >>> 24) & 0xFF;
        int r = (int) Mth.lerp(Mth.clamp(amount, 0f, 1f), (color >> 16) & 0xFF, 255);
        int g = (int) Mth.lerp(Mth.clamp(amount, 0f, 1f), (color >> 8) & 0xFF, 255);
        int b = (int) Mth.lerp(Mth.clamp(amount, 0f, 1f), color & 0xFF, 255);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * Health colour ramp: red -> amber -> green.
     */
    public static int hpColor(float percent) {
        float p = Mth.clamp(percent, 0f, 1f);
        int full = 0xFF57D977;
        int mid = 0xFFF0A93B;
        int empty = 0xFFE5484D;
        if (p < 0.5f) {
            return lerp(empty, mid, p / 0.5f);
        }
        return lerp(mid, full, (p - 0.5f) / 0.5f);
    }
}
