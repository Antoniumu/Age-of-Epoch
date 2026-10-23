package com.robertx22.forge_compat.event.entity.living;

import com.robertx22.forge_compat.eventbus.api.Event;
import net.minecraft.world.entity.LivingEntity;

public class LivingEvent extends Event {
    private final LivingEntity entityLiving;

    public LivingEvent(LivingEntity entityLiving) {
        this.entityLiving = entityLiving;
    }

    public LivingEntity getEntityLiving() { return entityLiving; }

    public LivingEntity getEntity() { return entityLiving; }

    public static class LivingUpdateEvent extends LivingEvent {
        public LivingUpdateEvent(LivingEntity entityLiving) {
            super(entityLiving);
        }
    }

    public static class LivingJumpEvent extends LivingEvent {
        public LivingJumpEvent(LivingEntity entityLiving) {
            super(entityLiving);
        }
    }
}
