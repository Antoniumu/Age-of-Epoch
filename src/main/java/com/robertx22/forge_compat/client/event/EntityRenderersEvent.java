package com.robertx22.forge_compat.client.event;

import com.robertx22.forge_compat.eventbus.api.Event;
import com.robertx22.forge_compat.fml.event.lifecycle.IModBusEvent;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public class EntityRenderersEvent extends Event implements IModBusEvent {
    public static class RegisterRenderers extends EntityRenderersEvent {
        public <T extends Entity> void registerEntityRenderer(EntityType<? extends T> type, EntityRendererProvider<T> provider) {
            EntityRenderers.register(type, provider);
        }
    }
    public static class RegisterLayerDefinitions extends EntityRenderersEvent {}
    public static class AddLayers extends EntityRenderersEvent {}
}
