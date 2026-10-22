package com.robertx22.mine_and_slash.a_libraries.dmg_number_particle;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.MultiBufferSource;

import java.util.HashSet;
import java.util.Set;

public class DamageParticleRenderer {
    public static Set<DamageParticle> PARTICLES = new HashSet<>();

    public static void renderParticles(GuiGraphicsExtractor gui, Camera camera) {
        PARTICLES.forEach(DamageParticle::tick);
        PARTICLES.removeIf(x -> x.age > 50);
    }

    public static void renderNameTag(Camera camera, String text, DamageParticle p, PoseStack stack, float tick, MultiBufferSource buf) {
    }
}
