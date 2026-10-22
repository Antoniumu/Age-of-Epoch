package com.robertx22.forge_compat.event.entity;

import com.robertx22.forge_compat.eventbus.api.Event;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public class EntityJoinLevelEvent extends Event {
    private final Entity entity;
    private final Level level;
    public EntityJoinLevelEvent(Entity entity, Level level) { this.entity = entity; this.level = level; }
    public Entity getEntity() { return entity; }
    public Level getLevel() { return level; }
}
