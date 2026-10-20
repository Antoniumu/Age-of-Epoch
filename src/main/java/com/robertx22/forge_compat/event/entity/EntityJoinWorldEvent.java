package com.robertx22.forge_compat.event.entity;

import com.robertx22.forge_compat.eventbus.api.Event;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public class EntityJoinWorldEvent extends Event {
    private final Entity entity;
    private final Level world;

    public EntityJoinWorldEvent(Entity entity, Level world) {
        this.entity = entity;
        this.world = world;
    }

    public Entity getEntity() { return entity; }

    public Level getWorld() { return world; }
}
