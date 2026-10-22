package com.robertx22.forge_compat.event.entity;

import com.robertx22.forge_compat.eventbus.api.Event;
import net.minecraft.world.entity.Entity;

public class EntityMobGriefingEvent extends Event {
    private final Entity entity;
    public EntityMobGriefingEvent(Entity entity) { this.entity = entity; }
    public Entity getEntity() { return entity; }
}
