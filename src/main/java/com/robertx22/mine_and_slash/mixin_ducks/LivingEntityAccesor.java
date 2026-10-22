package com.robertx22.mine_and_slash.mixin_ducks;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public interface LivingEntityAccesor {

    void setMyLastHurt(float lastHurt);

    float getMyLastHurt();

    void myknockback(LivingEntity target);

    SoundEvent myGetHurtSound(DamageSource source);

    float myGetHurtVolume();

    float myGetHurtPitch();

    static void setInvulnerableTime(Entity entity, int time) {
        if (entity != null) {
            entity.setInvulnerableTime(time);
        }
        if (entity instanceof LivingEntity living) {
            living.damageCooldownTime = time;
            if (time <= 0 && entity instanceof LivingEntityAccesor acc) {
                acc.setMyLastHurt(0.0F);
            }
        }
    }

    static int getInvulnerableTime(Entity entity) {
        if (entity instanceof LivingEntity living) {
            return Math.max(entity.getInvulnerableTime(), living.damageCooldownTime);
        }
        if (entity != null) {
            return entity.getInvulnerableTime();
        }
        return 0;
    }
}
