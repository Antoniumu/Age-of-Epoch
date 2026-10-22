package com.robertx22.mine_and_slash.database.data.spells.entities.renders;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.robertx22.mine_and_slash.database.data.spells.entities.SimpleProjectileEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

public class MySpriteRenderer<T extends Entity> extends EntityRenderer<T, MySpriteRenderer.SpriteState> {

    public static class SpriteState extends EntityRenderState {
        public float r = 1.0F;
        public float g = 0.85F;
        public float b = 0.35F;
    }

    public MySpriteRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public MySpriteRenderer(EntityRendererProvider.Context context, Object unused) {
        super(context);
    }

    @Override
    public SpriteState createRenderState() {
        return new SpriteState();
    }

    @Override
    public void extractRenderState(T entity, SpriteState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        if (entity instanceof SimpleProjectileEntity) {
            state.r = 1.0F;
            state.g = 0.65F;
            state.b = 0.25F;
        }
    }

    @Override
    public void submit(SpriteState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.15F, 0.0F);
        poseStack.scale(0.30F, 0.30F, 0.30F);
        final float r = state.r, g = state.g, b = state.b;
        collector.order(0).submitCustomGeometry(poseStack, RenderTypes.debugFilledBox(), (pose, vc) -> drawColoredBox(pose, vc, r, g, b, 0.90F));
        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    public static void drawFilledBox(PoseStack.Pose pose, VertexConsumer vc, AABB box, int argb) {
        float a = ((argb >>> 24) & 0xFF) / 255.0F;
        if (a <= 0.001F) a = 1.0F;
        float r = ((argb >>> 16) & 0xFF) / 255.0F;
        float g = ((argb >>> 8) & 0xFF) / 255.0F;
        float b = (argb & 0xFF) / 255.0F;
        float x0 = (float) box.minX, y0 = (float) box.minY, z0 = (float) box.minZ;
        float x1 = (float) box.maxX, y1 = (float) box.maxY, z1 = (float) box.maxZ;
        float[][] quads = {
            {x0, y0, z1,  x1, y0, z1,  x1, y1, z1,  x0, y1, z1},
            {x1, y0, z0,  x0, y0, z0,  x0, y1, z0,  x1, y1, z0},
            {x0, y1, z1,  x1, y1, z1,  x1, y1, z0,  x0, y1, z0},
            {x0, y0, z0,  x1, y0, z0,  x1, y0, z1,  x0, y0, z1},
            {x1, y0, z1,  x1, y0, z0,  x1, y1, z0,  x1, y1, z1},
            {x0, y0, z0,  x0, y0, z1,  x0, y1, z1,  x0, y1, z0}
        };
        for (float[] q : quads) {
            for (int i = 0; i < 12; i += 3) {
                vc.addVertex(pose, q[i], q[i + 1], q[i + 2]).setColor(r, g, b, a);
            }
        }
    }

    public static void drawColoredBox(PoseStack.Pose pose, VertexConsumer vc, float r, float g, float b, float a) {
        drawFilledBox(pose, vc, new AABB(-0.5D, -0.5D, -0.5D, 0.5D, 0.5D, 0.5D),
            ((int) (a * 255) << 24) | ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | ((int) (b * 255)));
    }
}
