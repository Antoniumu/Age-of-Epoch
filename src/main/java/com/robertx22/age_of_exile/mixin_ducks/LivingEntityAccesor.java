package com.robertx22.age_of_exile.mixin_ducks;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public interface LivingEntityAccesor {

    void myknockback(LivingEntity target);

    SoundEvent myGetHurtSound(DamageSource source);

    float myGetHurtVolume();

    float myGetHurtPitch();

    default void setMyInvulnerableTime(int time) {}

    default int getMyInvulnerableTime() { return 0; }

    static void setInvulnerableTime(Entity entity, int time) {
        if (entity instanceof LivingEntityAccesor acc) {
            acc.setMyInvulnerableTime(time);
        }
    }

    static int getInvulnerableTime(Entity entity) {
        if (entity != null) {
            return entity.getInvulnerableTime();
        }
        return 0;
    }
}
