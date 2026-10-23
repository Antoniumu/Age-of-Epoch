package com.robertx22.age_of_exile.database.data.spells.entities.renders;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.robertx22.age_of_exile.database.data.spells.entities.EntitySavedSpellData;
import com.robertx22.age_of_exile.database.data.spells.entities.SimpleProjectileEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

import java.util.Locale;

public class MySpriteRenderer<T extends Entity & IMyRenderAsItem> extends EntityRenderer<T, MySpriteRenderer.SpriteRenderState> {
    private final float scale;
    private final boolean lit;

    private static final AABB CORE_BOX = new AABB(-0.15D, -0.15D, -0.15D, 0.15D, 0.15D, 0.15D);
    private static final AABB OUTER_BOX = new AABB(-0.23D, -0.23D, -0.23D, 0.23D, 0.23D, 0.23D);

    public MySpriteRenderer(EntityRendererProvider.Context context, Object itemRenderer, float scale, boolean lit) {
        super(context);
        this.scale = scale;
        this.lit = lit;
    }

    public MySpriteRenderer(EntityRendererProvider.Context context, Object itemRenderer) {
        this(context, itemRenderer, 1.0F, false);
    }

    public MySpriteRenderer(EntityRendererProvider.Context context) {
        this(context, null, 1.0F, false);
    }

    public static class SpriteRenderState extends EntityRenderState {
        public int coreColor = ARGB.colorFromFloat(1.0F, 0.25F, 1.0F, 0.25F);
        public int outerColor = ARGB.colorFromFloat(0.65F, 0.08F, 0.72F, 0.18F);
        public float spinAngle = 0.0F;
    }

    @Override
    public SpriteRenderState createRenderState() {
        return new SpriteRenderState();
    }

    @Override
    public void extractRenderState(T entity, SpriteRenderState state, float tickProgress) {
        super.extractRenderState(entity, state, tickProgress);
        state.spinAngle = (entity.tickCount + tickProgress) * 0.35F;

        String id = "";
        try {
            if (entity instanceof SimpleProjectileEntity sp) {
                EntitySavedSpellData data = sp.getSpellData();
                if (data != null) {
                    if (data.item_id != null) id += data.item_id + " ";
                    if (data.spell_id != null) id += data.spell_id + " ";
                }
                if (sp.getScoreboardName() != null) {
                    id += sp.getScoreboardName() + " ";
                }
            }
            ItemStack stack = entity.getItem();
            if (stack != null && !stack.isEmpty()) {
                id += BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            }
        } catch (Exception ignored) {
        }
        id = id.toLowerCase(Locale.ROOT);

        if (id.contains("fire") || id.contains("flame") || id.contains("magma") || id.contains("blaze")) {
            state.coreColor = ARGB.colorFromFloat(1.0F, 1.0F, 0.85F, 0.2F);
            state.outerColor = ARGB.colorFromFloat(0.75F, 1.0F, 0.28F, 0.05F);
        } else if (id.contains("snow") || id.contains("ice") || id.contains("frost") || id.contains("chill") || id.contains("water")) {
            state.coreColor = ARGB.colorFromFloat(1.0F, 0.85F, 0.98F, 1.0F);
            state.outerColor = ARGB.colorFromFloat(0.7F, 0.15F, 0.75F, 1.0F);
        } else if (id.contains("thunder") || id.contains("lightning") || id.contains("spark")) {
            state.coreColor = ARGB.colorFromFloat(1.0F, 1.0F, 1.0F, 0.55F);
            state.outerColor = ARGB.colorFromFloat(0.7F, 1.0F, 0.85F, 0.1F);
        } else if (id.contains("seed") || id.contains("thorn") || id.contains("entangle")) {
            state.coreColor = ARGB.colorFromFloat(1.0F, 0.45F, 0.85F, 0.22F);
            state.outerColor = ARGB.colorFromFloat(0.75F, 0.22F, 0.55F, 0.12F);
        } else {
            // Default & Poison Ball / Slimeball: vibrant toxic green orb with emerald outer shell
            state.coreColor = ARGB.colorFromFloat(1.0F, 0.25F, 1.0F, 0.25F);
            state.outerColor = ARGB.colorFromFloat(0.68F, 0.08F, 0.72F, 0.18F);
        }
    }

    @Override
    public void submit(SpriteRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraState) {
        super.submit(state, poseStack, submitNodeCollector, cameraState);

        poseStack.pushPose();
        poseStack.translate(0.0F, 0.2F, 0.0F);
        poseStack.scale(this.scale, this.scale, this.scale);
        poseStack.mulPose(new Matrix4f().rotationXYZ(state.spinAngle, state.spinAngle * 1.3F, state.spinAngle * 0.7F));

        int coreCol = state.coreColor;
        submitNodeCollector.order(0).submitCustomGeometry(poseStack, RenderTypes.debugFilledBox(), (pose, buffer) -> {
            drawFilledBox(pose, buffer, CORE_BOX, coreCol);
        });

        poseStack.mulPose(new Matrix4f().rotationXYZ(0.7853982F, 0.7853982F, 0.7853982F));
        int outerCol = state.outerColor;
        submitNodeCollector.order(0).submitCustomGeometry(poseStack, RenderTypes.debugFilledBox(), (pose, buffer) -> {
            drawFilledBox(pose, buffer, OUTER_BOX, outerCol);
        });

        poseStack.popPose();
    }

    public static void drawFilledBox(PoseStack.Pose pose, VertexConsumer vertexConsumer, AABB box, int color) {
        // Front
        vertexConsumer.addVertex(pose, (float) box.minX, (float) box.minY, (float) box.minZ).setColor(color);
        vertexConsumer.addVertex(pose, (float) box.maxX, (float) box.minY, (float) box.minZ).setColor(color);
        vertexConsumer.addVertex(pose, (float) box.maxX, (float) box.maxY, (float) box.minZ).setColor(color);
        vertexConsumer.addVertex(pose, (float) box.minX, (float) box.maxY, (float) box.minZ).setColor(color);
        // Back
        vertexConsumer.addVertex(pose, (float) box.maxX, (float) box.minY, (float) box.maxZ).setColor(color);
        vertexConsumer.addVertex(pose, (float) box.minX, (float) box.minY, (float) box.maxZ).setColor(color);
        vertexConsumer.addVertex(pose, (float) box.minX, (float) box.maxY, (float) box.maxZ).setColor(color);
        vertexConsumer.addVertex(pose, (float) box.maxX, (float) box.maxY, (float) box.maxZ).setColor(color);
        // Left
        vertexConsumer.addVertex(pose, (float) box.minX, (float) box.minY, (float) box.maxZ).setColor(color);
        vertexConsumer.addVertex(pose, (float) box.minX, (float) box.minY, (float) box.minZ).setColor(color);
        vertexConsumer.addVertex(pose, (float) box.minX, (float) box.maxY, (float) box.minZ).setColor(color);
        vertexConsumer.addVertex(pose, (float) box.minX, (float) box.maxY, (float) box.maxZ).setColor(color);
        // Right
        vertexConsumer.addVertex(pose, (float) box.maxX, (float) box.minY, (float) box.minZ).setColor(color);
        vertexConsumer.addVertex(pose, (float) box.maxX, (float) box.minY, (float) box.maxZ).setColor(color);
        vertexConsumer.addVertex(pose, (float) box.maxX, (float) box.maxY, (float) box.maxZ).setColor(color);
        vertexConsumer.addVertex(pose, (float) box.maxX, (float) box.maxY, (float) box.minZ).setColor(color);
        // Top
        vertexConsumer.addVertex(pose, (float) box.minX, (float) box.maxY, (float) box.minZ).setColor(color);
        vertexConsumer.addVertex(pose, (float) box.maxX, (float) box.maxY, (float) box.minZ).setColor(color);
        vertexConsumer.addVertex(pose, (float) box.maxX, (float) box.maxY, (float) box.maxZ).setColor(color);
        vertexConsumer.addVertex(pose, (float) box.minX, (float) box.maxY, (float) box.maxZ).setColor(color);
        // Bottom
        vertexConsumer.addVertex(pose, (float) box.minX, (float) box.minY, (float) box.maxZ).setColor(color);
        vertexConsumer.addVertex(pose, (float) box.maxX, (float) box.minY, (float) box.maxZ).setColor(color);
        vertexConsumer.addVertex(pose, (float) box.maxX, (float) box.minY, (float) box.minZ).setColor(color);
        vertexConsumer.addVertex(pose, (float) box.minX, (float) box.minY, (float) box.minZ).setColor(color);
    }

    @Override
    protected int getBlockLightLevel(T entity, BlockPos blockPos) {
        return this.lit ? 15 : super.getBlockLightLevel(entity, blockPos);
    }
}
