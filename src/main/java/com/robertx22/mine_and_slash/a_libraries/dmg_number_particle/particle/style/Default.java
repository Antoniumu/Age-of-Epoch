package com.robertx22.mine_and_slash.a_libraries.dmg_number_particle.particle.style;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.robertx22.mine_and_slash.a_libraries.dmg_number_particle.particle.ExileInteractionResultParticle;
import net.minecraft.client.Camera;

public class Default implements IParticleRenderStrategy {
    @Override
    public void setupStyle(ExileInteractionResultParticle particle) {
        particle.setLifetime(25);
        particle.setParticleSpeed(0.0D, 0.045D, 0.0D);
    }

    @Override
    public void tick(ExileInteractionResultParticle particle) {}

    @Override
    public void setupParticle(ExileInteractionResultParticle particle, VertexConsumer vertexConsumer, Camera camera, float partialTick, PoseStack posestack) {}

    @Override
    public void renderDamage(ExileInteractionResultParticle particle, VertexConsumer vertexConsumer, Camera camera, float partialTick, PoseStack posestack, IParticleRenderMaterial material) {}

    @Override
    public void renderNullifiedDamage(ExileInteractionResultParticle particle, VertexConsumer vertexConsumer, Camera camera, float partialTick, PoseStack posestack, IParticleRenderMaterial material) {}

    @Override
    public void renderHeal(ExileInteractionResultParticle particle, VertexConsumer vertexConsumer, Camera camera, float partialTick, PoseStack posestack, IParticleRenderMaterial material) {}

    @Override
    public float changeScale(ExileInteractionResultParticle particle, int age, int lifeTime, float partialTick) {
        return 1.0F;
    }
}
