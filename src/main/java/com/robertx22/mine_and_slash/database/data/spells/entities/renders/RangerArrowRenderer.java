package com.robertx22.mine_and_slash.database.data.spells.entities.renders;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public class RangerArrowRenderer<T extends AbstractArrow> extends EntityRenderer<T, RangerArrowRenderer.ArrowState> {
    public static final Identifier RES_ARROW = Identifier.withDefaultNamespace("textures/entity/projectiles/arrow.png");
    private static final AABB SHAFT_BOX = new AABB(-0.04D, -0.04D, -0.35D, 0.04D, 0.04D, 0.35D);
    private static final AABB TIP_BOX = new AABB(-0.08D, -0.08D, 0.25D, 0.08D, 0.08D, 0.42D);

    public RangerArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ArrowState extends EntityRenderState {
        public float yRot;
        public float xRot;
    }

    @Override
    public ArrowState createRenderState() {
        return new ArrowState();
    }

    @Override
    public void extractRenderState(T entity, ArrowState state, float tickProgress) {
        super.extractRenderState(entity, state, tickProgress);
        state.yRot = entity.getYRot(tickProgress);
        state.xRot = entity.getXRot(tickProgress);
    }

    @Override
    public void submit(ArrowState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraState) {
        super.submit(state, poseStack, submitNodeCollector, cameraState);
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.1F, 0.0F);
        poseStack.mulPose(new Matrix4f().rotationY((float) Math.toRadians(-state.yRot)));
        poseStack.mulPose(new Matrix4f().rotationX((float) Math.toRadians(state.xRot)));
        int shaftColor = ARGB.colorFromFloat(1.0F, 0.65F, 0.48F, 0.25F);
        int tipColor = ARGB.colorFromFloat(1.0F, 0.85F, 0.9F, 0.95F);
        submitNodeCollector.order(0).submitCustomGeometry(poseStack, RenderTypes.debugFilledBox(), (pose, buffer) -> {
            MySpriteRenderer.drawFilledBox(pose, buffer, SHAFT_BOX, shaftColor);
            MySpriteRenderer.drawFilledBox(pose, buffer, TIP_BOX, tipColor);
        });
        poseStack.popPose();
    }
}
