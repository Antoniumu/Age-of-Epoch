package com.robertx22.forge_compat.fml.client.registry;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public class RenderingRegistry {

    public static <T extends Entity> void registerEntityRenderingHandler(EntityType<T> type, IRenderFactory<T> factory) {
        EntityRendererRegistry.register(type, (ctx) -> factory.createRenderFor(ctx));
    }
}
