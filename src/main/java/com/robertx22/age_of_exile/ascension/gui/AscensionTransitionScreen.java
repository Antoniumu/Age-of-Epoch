package com.robertx22.age_of_exile.ascension.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.forge_compat.client.AbstractGui;
import com.robertx22.forge_compat.client.Screen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Анимированный переход «разрыв воды» между экраном Навыков и экраном
 * Вознесённых классов. Из точки клика расходятся концентрические водяные
 * кольца, экран заливает глубинная синева снизу вверх, мелькают призматические
 * искры; по завершении (~0.65 с) открывается целевой экран.
 *
 * <p>Рисуется только голограммной графикой (без FBO): кольца — это дуги из
 * мелких квадратов, волна — полосы заливки с перепадом прозрачности.
 */
public class AscensionTransitionScreen extends Screen {

    private final Screen target;
    private final double originX;
    private final double originY;
    private final long startedAt;

    private static final long DURATION_MS = 650L;

    public AscensionTransitionScreen(Screen target, double originX, double originY) {
        super(Component.literal(""));
        this.target = target;
        this.originX = originX;
        this.originY = originY;
        this.startedAt = System.currentTimeMillis();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(PoseStack matrix, int mouseX, int mouseY, float delta) {
        long elapsed = System.currentTimeMillis() - startedAt;
        double t = Math.min(1.0D, elapsed / (double) DURATION_MS);

        // подложка: глубина, поднимающаяся снизу — «водяная стена»
        AbstractGui.fill(matrix, 0, 0, width, height, 0xFF02060C);
        int wallTop = (int) (height * (1.0D - t));
        for (int y = wallTop; y < height; y += 2) {
            float depth = (y - wallTop) / (float) Math.max(1, height - wallTop);
            int a = (int) (40 + 200 * Math.min(1.0D, t * 1.4D));
            int col = (a << 24) | mix(0x0A2340, 0x1B5B8A, depth);
            AbstractGui.fill(matrix, 0, y, width, Math.min(y + 2, height), col);
        }

        // кольца прилива из точки клика
        double maxR = Math.hypot(width, height);
        for (int k = 0; k < 4; k++) {
            double rr = t * maxR * 1.15D - k * 34.0D;
            if (rr <= 4) {
                continue;
            }
            float fade = (float) Math.max(0.0D, 1.0D - t) * (1.0F - k * 0.18F);
            int color = ringColor(k, fade);
            drawRing(matrix, originX, originY, rr, 38, color);
        }

        // призматические искры, несущиеся по спирали
        int sparks = 26;
        for (int i = 0; i < sparks; i++) {
            double seed = i * 137.51D;
            double ang = Math.toRadians(seed) + t * 5.0D;
            double rad = (t * maxR * (0.25D + (i % 7) / 9.0D));
            int sx = (int) (originX + Math.cos(ang) * rad);
            int sy = (int) (originY + Math.sin(ang) * rad * 0.6D);
            int a = (int) (230 * Math.max(0.0D, 1.0D - t));
            int col = (a << 24) | (i % 3 == 0 ? 0xB26CFF : (i % 2 == 0 ? 0x7DFFE9 : 0x37E2FF));
            AbstractGui.fill(matrix, sx, sy, Math.min(sx + 2, width), Math.min(sy + 2, height), col);
        }

        if (t >= 1.0D) {
            Minecraft mc = Minecraft.getInstance();
            mc.gui.setScreen(target);
        }
    }

    private static int ringColor(int k, float alpha) {
        int rgb = switch (k % 3) {
            case 0 -> 0x37E2FF;
            case 1 -> 0x7DFFE9;
            default -> 0xB26CFF;
        };
        return ((int) (alpha * 235) << 24) | rgb;
    }

    /**
     * Кольцо из мелких квадратов 2×2 по дуге окружности.
     */
    static void drawRing(PoseStack matrix, double cx, double cy, double radius, int segments, int color) {
        for (int s = 0; s < segments; s++) {
            double ang = Math.PI * 2 * s / segments;
            int x = (int) (cx + Math.cos(ang) * radius);
            int y = (int) (cy + Math.sin(ang) * radius * 0.55D); // лёгкая перспектива воды
            AbstractGui.fill(matrix, x, y, x + 2, y + 2, color);
        }
    }

    static int mix(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int r = (int) (ar + (br - ar) * t);
        int g = (int) (ag + (bg - ag) * t);
        int bl = (int) (ab + (bb - ab) * t);
        return (r << 16) | (g << 8) | bl;
    }
}
