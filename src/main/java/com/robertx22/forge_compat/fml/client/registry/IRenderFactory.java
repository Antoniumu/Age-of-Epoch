package com.robertx22.forge_compat.fml.client.registry;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;

@FunctionalInterface
public interface IRenderFactory<T extends Entity> {
    EntityRenderer<? super T, ?> createRenderFor(EntityRendererProvider.Context context);
}
