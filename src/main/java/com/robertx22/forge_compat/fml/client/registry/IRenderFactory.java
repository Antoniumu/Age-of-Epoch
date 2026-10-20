package com.robertx22.forge_compat.fml.client.registry;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;

@FunctionalInterface
public interface IRenderFactory<T extends net.minecraft.world.entity.Entity> {
    EntityRenderer<? super T> createRenderFor(EntityRenderDispatcher manager);
}
