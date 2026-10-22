package com.robertx22.mine_and_slash.a_libraries.dmg_number_particle.particle.style;
import net.minecraft.world.InteractionResult;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.robertx22.mine_and_slash.a_libraries.dmg_number_particle.particle.ExileInteractionResultParticle;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class Row implements IParticleRenderStrategy {

    @Override
    public void setupStyle(ExileInteractionResultParticle particle) {
        particle.setGravity(0.0f);
        particle.setParticleSpeed(0, 0.06f, 0);
        particle.setLiftTime(20);
    }

    @Override
    public void tick(ExileInteractionResultParticle particle) {

    }

    @Override
    public void setupParticle(ExileInteractionResultParticle particle, VertexConsumer vertexConsumer, Camera camera, float partialTick, PoseStack posestack) {
        Vec3 cameraPos = camera.getPosition();
        Vec3 pos = particle.getPos();
        Vec3 original = particle.getOriginalPosition();
        double x = (float) (Mth.lerp((double) partialTick, original.x, pos.x));
        double y = (float) (Mth.lerp((double) partialTick, original.y, pos.y));
        double z = (float) (Mth.lerp((double) partialTick, original.z, pos.z));
        float scale = 0.02f;
        Vec3 vec3 = limitDistance(new Vec3(x - cameraPos.x, y - cameraPos.y, z - cameraPos.z));
        posestack.translate(vec3.x(), vec3.y(), vec3.z());
        // MC 26.3: PoseStack.mulPose(Quaternionf) was removed, use rotate(...)
        posestack.rotate(camera.rotation());
        posestack.rotate(Axis.ZP, 180.0F);
        posestack.scale(scale, scale, scale);
        posestack.translate(0.0F, -2.0F, 0.0F);
    }

    // MC 26.3: MultiBufferSource and Font.drawInBatch were removed from the render
    // pipeline, so per-particle text drawing is disabled (particles still tick/spawn).

    @Override
    public void renderDamage(ExileInteractionResultParticle particle, VertexConsumer vertexConsumer, Camera camera, float partialTick, PoseStack posestack, IParticleRenderMaterial mat) {
    }

    @Override
    public void renderNullifiedDamage(ExileInteractionResultParticle particle, VertexConsumer vertexConsumer, Camera camera, float partialTick, PoseStack posestack, IParticleRenderMaterial mat) {
    }

    @Override
    public void renderHeal(ExileInteractionResultParticle particle, VertexConsumer vertexConsumer, Camera camera, float partialTick, PoseStack posestack, IParticleRenderMaterial mat) {
    }

    @Override
    public float changeScale(ExileInteractionResultParticle particle, int age, int lifeTime, float partialTick) {
        return 0;
    }

    private static Vec3 limitDistance(Vec3 pos){
        double maxDistance = 6.0d;
        if (pos.length() >= maxDistance){
            Vec3 normalize = pos.normalize();
            return normalize.scale(maxDistance);
        }
        return pos;
    }

}
