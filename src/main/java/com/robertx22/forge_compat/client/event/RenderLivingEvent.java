package com.robertx22.forge_compat.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.forge_compat.client.MultiBufferSource;
import com.robertx22.forge_compat.eventbus.api.Event;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.LivingEntity;

public class RenderLivingEvent<T extends LivingEntity, R extends EntityRenderer<T, ?>> extends Event {

    private final T entity;
    private final R renderer;
    private final PoseStack matrixStack;
    private final MultiBufferSource buffers;
    private final int light;

    public RenderLivingEvent(T entity, R renderer, PoseStack matrixStack, MultiBufferSource buffers, int light) {
        this.entity = entity;
        this.renderer = renderer;
        this.matrixStack = matrixStack;
        this.buffers = buffers;
        this.light = light;
    }

    public T getEntity() { return entity; }

    public LivingEntity getEntityLiving() { return entity; }

    public R getRenderer() { return renderer; }

    public PoseStack getMatrixStack() { return matrixStack; }

    public PoseStack getPoseStack() { return matrixStack; }

    public MultiBufferSource getBuffers() { return buffers; }

    public int getLight() { return light; }

    public static class Pre<T extends LivingEntity, R extends EntityRenderer<T, ?>> extends RenderLivingEvent<T, R> {
        public Pre(T entity, R renderer, PoseStack matrixStack, MultiBufferSource buffers, int light) {
            super(entity, renderer, matrixStack, buffers, light);
        }
    }

    public static class Post<T extends LivingEntity, R extends EntityRenderer<T, ?>> extends RenderLivingEvent<T, R> {
        public Post(T entity, R renderer, PoseStack matrixStack, MultiBufferSource buffers, int light) {
            super(entity, renderer, matrixStack, buffers, light);
        }
    }
}
