package com.robertx22.forge_compat.event.entity.living;

import com.robertx22.forge_compat.eventbus.api.Event;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public class LivingHurtEvent extends Event {
    private final LivingEntity entityLiving;
    private final LivingEntity targetEntity;
    private final DamageSource source;
    private float amount;

    public LivingHurtEvent(LivingEntity entityLiving, LivingEntity targetEntity, DamageSource source, float amount) {
        this.entityLiving = entityLiving;
        this.targetEntity = targetEntity;
        this.source = source;
        this.amount = amount;
    }

    public LivingEntity getEntityLiving() { return entityLiving; }

    public LivingEntity getEntity() { return entityLiving; }

    public LivingEntity getTargetEntity() { return targetEntity; }

    public DamageSource getSource() { return source; }

    public float getAmount() { return amount; }

    public void setAmount(float amount) { this.amount = amount; }

    @Override
    public boolean isCancelable() { return true; }
}
