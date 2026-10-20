package com.robertx22.forge_compat.event.entity.living;

import com.robertx22.forge_compat.eventbus.api.Event;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public class LivingDamageEvent extends Event {
    private final LivingEntity entityLiving;
    private final DamageSource source;
    private float amount;

    public LivingDamageEvent(LivingEntity entityLiving, DamageSource source, float amount) {
        this.entityLiving = entityLiving;
        this.source = source;
        this.amount = amount;
    }

    public LivingEntity getEntityLiving() { return entityLiving; }

    public DamageSource getSource() { return source; }

    public float getAmount() { return amount; }

    public void setAmount(float amount) { this.amount = amount; }

    @Override
    public boolean isCancelable() { return true; }
}
