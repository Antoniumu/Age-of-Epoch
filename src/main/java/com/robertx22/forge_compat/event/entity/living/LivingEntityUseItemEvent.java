package com.robertx22.forge_compat.event.entity.living;

import com.robertx22.forge_compat.eventbus.api.Event;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class LivingEntityUseItemEvent extends Event {
    private final LivingEntity entityLiving;
    private final ItemStack item;
    private int duration;

    public LivingEntityUseItemEvent(LivingEntity entityLiving, ItemStack item, int duration) {
        this.entityLiving = entityLiving;
        this.item = item;
        this.duration = duration;
    }

    public LivingEntity getEntityLiving() { return entityLiving; }

    public ItemStack getItem() { return item; }

    public int getDuration() { return duration; }

    public void setDuration(int duration) { this.duration = duration; }

    @Override
    public boolean isCancelable() { return true; }

    public static class Start extends LivingEntityUseItemEvent {
        public Start(LivingEntity entityLiving, ItemStack item, int duration) {
            super(entityLiving, item, duration);
        }
    }

    public static class Stop extends LivingEntityUseItemEvent {
        public Stop(LivingEntity entityLiving, ItemStack item, int duration) {
            super(entityLiving, item, duration);
        }
    }
}
