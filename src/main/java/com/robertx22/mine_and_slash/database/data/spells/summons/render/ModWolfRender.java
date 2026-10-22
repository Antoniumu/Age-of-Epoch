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

public class ModWolfRender<T extends Mob> extends EntityRenderer<T, ModWolfRender.WolfSummonState> {
    private final Identifier texture;
    private static final AABB HEAD = new AABB(-0.19, 0.38, 0.28, 0.19, 0.72, 0.62);
    private static final AABB BODY = new AABB(-0.22, 0.32, -0.35, 0.22, 0.68, 0.30);
    private static final AABB LEG_FL = new AABB(0.06, 0.0, 0.12, 0.20, 0.35, 0.26);
    private static final AABB LEG_FR = new AABB(-0.20, 0.0, 0.12, -0.06, 0.35, 0.26);
    private static final AABB LEG_BL = new AABB(0.06, 0.0, -0.30, 0.20, 0.35, -0.16);
    private static final AABB LEG_BR = new AABB(-0.20, 0.0, -0.30, -0.06, 0.35, -0.16);

    public ModWolfRender(Identifier id, EntityRendererProvider.Context ctx) {
        super(ctx);
        this.texture = id;
    }

    public static class WolfSummonState extends EntityRenderState {
        public float bodyYaw;
    }

    @Override
    public WolfSummonState createRenderState() {
        return new WolfSummonState();
    }

    @Override
    public void extractRenderState(T entity, WolfSummonState state, float tickProgress) {
        super.extractRenderState(entity, state, tickProgress);
        state.bodyYaw = entity.getYRot(tickProgress);
    }

    @Override
    public void submit(WolfSummonState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);
        poseStack.pushPose();
        poseStack.mulPose(new Matrix4f().rotationY((float) Math.toRadians(-state.bodyYaw)));
        int light = state.lightCoords;
        collector.order(0).submitCustomGeometry(poseStack, RenderTypes.entityCutoutNoCull(this.texture), (pose, vc) -> {
            MySpriteRenderer.drawTexturedBox(pose, vc, HEAD, light);
            MySpriteRenderer.drawTexturedBox(pose, vc, BODY, light);
            MySpriteRenderer.drawTexturedBox(pose, vc, LEG_FL, light);
            MySpriteRenderer.drawTexturedBox(pose, vc, LEG_FR, light);
            MySpriteRenderer.drawTexturedBox(pose, vc, LEG_BL, light);
            MySpriteRenderer.drawTexturedBox(pose, vc, LEG_BR, light);
        });
        poseStack.popPose();
    }
}
