package com.robertx22.forge_compat.event.entity;

import com.robertx22.forge_compat.eventbus.api.Event;
import com.robertx22.forge_compat.fml.event.lifecycle.IModBusEvent;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;

public class EntityAttributeCreationEvent extends Event implements IModBusEvent {
    public void put(EntityType<? extends LivingEntity> entity, AttributeSupplier map) {
        FabricDefaultAttributeRegistry.register(entity, map);
    }
}
