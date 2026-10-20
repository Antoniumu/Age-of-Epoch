package com.robertx22.forge_compat.event.entity.living;

import com.robertx22.forge_compat.eventbus.api.Event;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public class LivingDeathEvent extends Event {
    private final LivingEntity entityLiving;
    private final DamageSource source;

    public LivingDeathEvent(LivingEntity entityLiving, DamageSource source) {
        this.entityLiving = entityLiving;
        this.source = source;
    }

    public LivingEntity getEntityLiving() { return entityLiving; }

    public LivingEntity getEntity() { return entityLiving; }

    public DamageSource getSource() { return source; }

    @Override
    public boolean isCancelable() { return true; }
}
