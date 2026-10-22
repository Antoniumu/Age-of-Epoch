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

public class ModSkeletonRender<T extends Mob> extends EntityRenderer<T, ModSkeletonRender.HumanoidSummonState> {
    private final Identifier texture;
    private static final AABB HEAD = new AABB(-0.25, 1.4, -0.25, 0.25, 1.9, 0.25);
    private static final AABB BODY = new AABB(-0.25, 0.7, -0.13, 0.25, 1.4, 0.13);
    private static final AABB ARM_L = new AABB(0.25, 0.7, -0.1, 0.45, 1.4, 0.1);
    private static final AABB ARM_R = new AABB(-0.45, 0.7, -0.1, -0.25, 1.4, 0.1);
    private static final AABB LEG_L = new AABB(0.02, 0.0, -0.1, 0.22, 0.7, 0.1);
    private static final AABB LEG_R = new AABB(-0.22, 0.0, -0.1, -0.02, 0.7, 0.1);

    public ModSkeletonRender(Identifier id, EntityRendererProvider.Context ctx) {
        super(ctx);
        this.texture = id;
    }

    public static class HumanoidSummonState extends EntityRenderState {
        public float bodyYaw;
    }

    @Override
    public HumanoidSummonState createRenderState() {
        return new HumanoidSummonState();
    }

    @Override
    public void extractRenderState(T entity, HumanoidSummonState state, float tickProgress) {
        super.extractRenderState(entity, state, tickProgress);
        state.bodyYaw = entity.getYRot(tickProgress);
    }

    @Override
    public void submit(HumanoidSummonState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);
        poseStack.pushPose();
        poseStack.mulPose(new Matrix4f().rotationY((float) Math.toRadians(-state.bodyYaw)));
        int light = state.lightCoords;
        collector.order(0).submitCustomGeometry(poseStack, RenderTypes.entityCutout(this.texture, true), (pose, vc) -> {
            MySpriteRenderer.drawTexturedBox(pose, vc, HEAD, light);
            MySpriteRenderer.drawTexturedBox(pose, vc, BODY, light);
            MySpriteRenderer.drawTexturedBox(pose, vc, ARM_L, light);
            MySpriteRenderer.drawTexturedBox(pose, vc, ARM_R, light);
            MySpriteRenderer.drawTexturedBox(pose, vc, LEG_L, light);
            MySpriteRenderer.drawTexturedBox(pose, vc, LEG_R, light);
        });
        poseStack.popPose();
    }
}
