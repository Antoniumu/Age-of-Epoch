package com.robertx22.mine_and_slash.mmorpg.registers.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.mine_and_slash.database.data.spells.entities.AutoAimingProj;
import com.robertx22.mine_and_slash.database.data.spells.entities.renders.MySpriteRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public class AutoSkullRender extends EntityRenderer<AutoAimingProj, AutoSkullRender.SkullProjState> {
    private static final Identifier WITHER_LOCATION = Identifier.withDefaultNamespace("textures/entity/wither/wither.png");
    private static final AABB SKULL = new AABB(-0.25, 0.0, -0.25, 0.25, 0.5, 0.25);

    public AutoSkullRender(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    public static class SkullProjState extends EntityRenderState {
        public float yRot;
        public float xRot;
    }

    @Override
    public SkullProjState createRenderState() {
        return new SkullProjState();
    }

    @Override
    public void extractRenderState(AutoAimingProj entity, SkullProjState state, float tickProgress) {
        super.extractRenderState(entity, state, tickProgress);
        state.yRot = entity.getYRot(tickProgress);
        state.xRot = entity.getXRot(tickProgress);
    }

    @Override
    protected int getBlockLightLevel(AutoAimingProj entity, BlockPos pos) {
        return 15;
    }

    @Override
    public void submit(SkullProjState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);
        poseStack.pushPose();
        poseStack.mulPose(new Matrix4f().rotationY((float) Math.toRadians(-state.yRot)));
        poseStack.mulPose(new Matrix4f().rotationX((float) Math.toRadians(state.xRot)));
        collector.order(0).submitCustomGeometry(poseStack, RenderTypes.entityCutout(WITHER_LOCATION, true), (pose, vc) -> MySpriteRenderer.drawTexturedBox(pose, vc, SKULL, 0xF000F0));
        poseStack.popPose();
    }
}
