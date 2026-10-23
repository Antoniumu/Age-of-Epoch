package com.robertx22.age_of_exile.mixin_methods;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.age_of_exile.a_libraries.mob_plates.MobPlateRenderer;
import com.robertx22.forge_compat.client.MultiBufferSource;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.LivingEntity;

public class RenderMobInfo {

    /**
     * Entry point used by {@code LevelRenderEvents.COLLECT_SUBMITS}.
     */
    public static void submitWorldMobLabels(LevelRenderContext context) {
        MobPlateRenderer.submitWorldMobLabels(context);
    }

    /**
     * Kept for compatibility with the old Neat-style render hooks: labels are now
     * submitted through the 26.3 submit-node pipeline instead of the immediate
     * render pass.
     */
    public static void renderLivingEntityLabelIfPresent(Font textRenderer, EntityRenderDispatcher dispatcher,
                                                        LivingEntity entity, PoseStack matrixStack,
                                                        MultiBufferSource vertex, int i) {
    }
}
