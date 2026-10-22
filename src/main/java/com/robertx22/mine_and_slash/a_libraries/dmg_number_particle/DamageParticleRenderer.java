package com.robertx22.mine_and_slash.a_libraries.dmg_number_particle;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DamageParticleRenderer {
    public static final List<DamageParticle> PARTICLES = new CopyOnWriteArrayList<>();

    public static void spawn(Component component, double x, double y, double z, double vx, double vy, double vz, float scale, boolean isCrit) {
        DamageParticle dp = new DamageParticle();
        dp.component = component.copy();
        dp.renderString = component.getString();
        dp.hasWorldPos = true;
        dp.x = x;
        dp.y = y;
        dp.z = z;
        dp.vx = vx;
        dp.vy = vy;
        dp.vz = vz;
        dp.scale = scale;
        dp.isCrit = isCrit;
        PARTICLES.add(dp);
    }

    public static void tickParticles() {
        for (DamageParticle p : PARTICLES) {
            p.tick();
        }
        PARTICLES.removeIf(x -> x.age > x.maxAge);
    }

    public static void renderParticles(GuiGraphicsExtractor gui, Camera camera) {
        tickParticles();
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
                MutableComponent comp = p.component != null ? p.component : Component.literal(p.renderString).withStyle(ChatFormatting.RED);
                context.submitNodeCollector()
                    .order(0)
                    .submitNameTag(poseStack, Vec3.ZERO, 0, comp, true, 0xF000F0, cameraState);
                poseStack.popPose();
            }
        } catch (Exception ignored) {
        }
    }
}
