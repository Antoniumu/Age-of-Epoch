package com.robertx22.age_of_exile.a_libraries.dmg_number_particle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.forge_compat.client.AbstractGui;
import com.robertx22.forge_compat.common.CompatChatFormatting;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DamageParticleRenderer {

    public static final List<DamageParticle> PARTICLES = new CopyOnWriteArrayList<>();

    public static void tickParticles() {
        for (DamageParticle p : PARTICLES) {
            p.tick();
        }
        PARTICLES.removeIf(x -> x.age > 45);
    }

    public static void submitWorldParticles(LevelRenderContext context) {
        try {
            if (PARTICLES.isEmpty()) {
                return;
            }
            PoseStack poseStack = context.poseStack();
            CameraRenderState cameraState = context.levelState().cameraRenderState;
            if (poseStack == null || cameraState == null || cameraState.pos == null) {
                return;
            }
            Vec3 camPos = cameraState.pos;
            for (DamageParticle p : PARTICLES) {
                if (!p.hasWorldPos) {
                    continue;
                }
                double dx = p.x - camPos.x;
                double dy = p.y - camPos.y;
                double dz = p.z - camPos.z;
                if (dx * dx + dy * dy + dz * dz > 64 * 64) {
                    continue;
                }
                poseStack.pushPose();
                poseStack.translate(dx, dy, dz);
                ChatFormatting fmt = (p.packet != null && p.packet.format != null) ? p.packet.format : ChatFormatting.RED;
                MutableComponent comp = Component.literal(p.renderString).withStyle(fmt);
                if (p.packet != null && p.packet.iscrit) {
                    comp = comp.withStyle(ChatFormatting.BOLD);
                }
                context.submitNodeCollector()
                    .order(0)
                    .submitNameTag(poseStack, Vec3.ZERO, 0, comp, true, 0xF000F0, cameraState);
                poseStack.popPose();
            }
        } catch (Exception ignored) {
        }
    }

    public static void renderHudParticles(PoseStack matrix) {
        try {
            if (PARTICLES.isEmpty()) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.font == null) {
                return;
            }
            int cx = mc.getWindow().getGuiScaledWidth() / 2 + 18;
            int cy = mc.getWindow().getGuiScaledHeight() / 2 - 16;

            int shown = 0;
            for (int i = PARTICLES.size() - 1; i >= 0 && shown < 6; i--) {
                DamageParticle p = PARTICLES.get(i);
                if (p == null || p.age > 40) {
                    continue;
                }
                ChatFormatting fmt = (p.packet != null && p.packet.format != null) ? p.packet.format : ChatFormatting.RED;
                int color = CompatChatFormatting.getColor(fmt);
                int yOff = cy + shown * 11 - (p.age / 3);
                AbstractGui.drawShadow(mc.font, matrix, p.renderString, cx, yOff, color);
                shown++;
            }
        } catch (Exception ignored) {
        }
    }

    public static void renderParticles(PoseStack matrix, Camera camera) {
        // Legacy entry point kept for compatibility
    }

    public static void drawDamageNumber(PoseStack matrix, String s, double x, double y, float width) {
        Minecraft minecraft = Minecraft.getInstance();
        int sw = minecraft.font.width(s);
        AbstractGui.drawShadow(minecraft.font, matrix, s, (int) (x + (width / 2) - sw), (int) y + 5, CompatChatFormatting.getColor(ChatFormatting.RED));
    }
}
