package com.robertx22.mine_and_slash.database.data.spells.summons.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.mine_and_slash.database.data.spells.entities.renders.MySpriteRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public class ModSpiderRender<T extends Mob> extends EntityRenderer<T, ModSpiderRender.SpiderSummonState> {
    private final Identifier texture;
    private static final AABB HEAD = new AABB(-0.22, 0.18, 0.15, 0.22, 0.52, 0.50);
    private static final AABB ABDOMEN = new AABB(-0.30, 0.16, -0.50, 0.30, 0.58, 0.15);

    public ModSpiderRender(Identifier id, EntityRendererProvider.Context ctx) {
        super(ctx);
        this.texture = id;
    }

    public static class SpiderSummonState extends EntityRenderState {
        public float bodyYaw;
    }

    @Override
    public SpiderSummonState createRenderState() {
        return new SpiderSummonState();
    }

    @Override
    public void extractRenderState(T entity, SpiderSummonState state, float tickProgress) {
        super.extractRenderState(entity, state, tickProgress);
        state.bodyYaw = entity.getYRot(tickProgress);
    }

    @Override
    public void submit(SpiderSummonState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);
        poseStack.pushPose();
        poseStack.scale(0.55F, 0.55F, 0.55F);
        poseStack.mulPose(new Matrix4f().rotationY((float) Math.toRadians(-state.bodyYaw)));
        int light = state.lightCoords;
        collector.order(0).submitCustomGeometry(poseStack, RenderTypes.entityCutoutNoCull(this.texture), (pose, vc) -> {
            MySpriteRenderer.drawTexturedBox(pose, vc, HEAD, light);
            MySpriteRenderer.drawTexturedBox(pose, vc, ABDOMEN, light);
        });
        poseStack.popPose();
    }
}
