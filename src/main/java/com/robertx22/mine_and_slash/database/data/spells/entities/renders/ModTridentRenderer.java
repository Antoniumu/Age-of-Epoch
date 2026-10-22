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
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public class ModTridentRenderer<T extends Projectile> extends EntityRenderer<T, ModTridentRenderer.TridentState> {
    public static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/entity/trident.png");
    private static final AABB SHAFT_BOX = new AABB(-0.05D, -0.05D, -0.55D, 0.05D, 0.05D, 0.45D);
    private static final AABB HEAD_BOX = new AABB(-0.18D, -0.05D, 0.35D, 0.18D, 0.05D, 0.58D);

    public ModTridentRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class TridentState extends EntityRenderState {
        public float yRot;
        public float xRot;
    }

    @Override
    public TridentState createRenderState() {
        return new TridentState();
    }

    @Override
    public void extractRenderState(T entity, TridentState state, float tickProgress) {
        super.extractRenderState(entity, state, tickProgress);
        state.yRot = entity.getYRot(tickProgress);
        state.xRot = entity.getXRot(tickProgress);
    }

    @Override
    public void submit(TridentState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraState) {
        super.submit(state, poseStack, submitNodeCollector, cameraState);
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.15F, 0.0F);
        poseStack.mulPose(new Matrix4f().rotationY((float) Math.toRadians(-state.yRot)));
        poseStack.mulPose(new Matrix4f().rotationX((float) Math.toRadians(state.xRot)));
        int shaftColor = ARGB.colorFromFloat(1.0F, 0.35F, 0.75F, 0.85F);
        int headColor = ARGB.colorFromFloat(1.0F, 0.75F, 0.95F, 1.0F);
        submitNodeCollector.order(0).submitCustomGeometry(poseStack, RenderTypes.debugFilledBox(), (pose, buffer) -> {
            MySpriteRenderer.drawFilledBox(pose, buffer, SHAFT_BOX, shaftColor);
            MySpriteRenderer.drawFilledBox(pose, buffer, HEAD_BOX, headColor);
        });
        poseStack.popPose();
    }
}
