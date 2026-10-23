package com.robertx22.age_of_exile.database.data.exile_effects;

import net.minecraft.world.effect.MobEffectCategory;

public enum EffectType {
    beneficial(MobEffectCategory.BENEFICIAL),
    negative(MobEffectCategory.HARMFUL),
    buff(MobEffectCategory.BENEFICIAL),
    neutral(MobEffectCategory.NEUTRAL);

    public MobEffectCategory type;

    EffectType(MobEffectCategory type) {
        this.type = type;
    }
}
