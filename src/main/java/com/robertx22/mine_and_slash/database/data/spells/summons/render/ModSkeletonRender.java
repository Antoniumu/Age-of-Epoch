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
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public class ModSkeletonRender<T extends Mob> extends EntityRenderer<T, ModSkeletonRender.SummonRenderState> {
    private final Identifier texture;
    private static final AABB HEAD = new AABB(-0.24, 1.38, -0.24, 0.24, 1.86, 0.24);
    private static final AABB BODY = new AABB(-0.24, 0.68, -0.13, 0.24, 1.38, 0.13);
    private static final AABB ARM_L = new AABB(0.24, 0.70, -0.10, 0.42, 1.36, 0.10);
    private static final AABB ARM_R = new AABB(-0.42, 0.70, -0.10, -0.24, 1.36, 0.10);
    private static final AABB LEG_L = new AABB(0.02, 0.0, -0.10, 0.22, 0.68, 0.10);
    private static final AABB LEG_R = new AABB(-0.22, 0.0, -0.10, -0.02, 0.68, 0.10);

    public ModSkeletonRender(Identifier id, EntityRendererProvider.Context ctx) {
        super(ctx);
        this.texture = id;
    }

    public static class SummonRenderState extends EntityRenderState {
        public float bodyYaw;
        public int bodyColor = ARGB.colorFromFloat(0.95F, 0.88F, 0.88F, 0.84F);
        public int trimColor = ARGB.colorFromFloat(0.85F, 0.62F, 0.65F, 0.72F);
    }

    @Override
    public SummonRenderState createRenderState() {
        return new SummonRenderState();
    }

    @Override
    public void extractRenderState(T entity, SummonRenderState state, float tickProgress) {
        super.extractRenderState(entity, state, tickProgress);
        state.bodyYaw = entity.getYRot(tickProgress);
    }

    @Override
    public void submit(SummonRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);
        poseStack.pushPose();
        poseStack.mulPose(new Matrix4f().rotationY((float) Math.toRadians(-state.bodyYaw)));
        int cMain = state.bodyColor;
        int cTrim = state.trimColor;
        collector.order(0).submitCustomGeometry(poseStack, RenderTypes.debugFilledBox(), (pose, vc) -> {
            MySpriteRenderer.drawFilledBox(pose, vc, HEAD, cMain);
            MySpriteRenderer.drawFilledBox(pose, vc, BODY, cTrim);
            MySpriteRenderer.drawFilledBox(pose, vc, ARM_L, cMain);
            MySpriteRenderer.drawFilledBox(pose, vc, ARM_R, cMain);
            MySpriteRenderer.drawFilledBox(pose, vc, LEG_L, cTrim);
            MySpriteRenderer.drawFilledBox(pose, vc, LEG_R, cTrim);
        });
        poseStack.popPose();
    }
}
