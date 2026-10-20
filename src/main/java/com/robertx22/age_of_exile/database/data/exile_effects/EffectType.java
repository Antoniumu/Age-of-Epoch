import net.minecraft.world.effect.MobEffectCategory;
package com.robertx22.age_of_exile.database.data.exile_effects;

public enum MobEffectCategory {
    beneficial(net.minecraft.potion.MobEffectCategory.BENEFICIAL),
    negative(net.minecraft.potion.MobEffectCategory.HARMFUL),
    buff(net.minecraft.potion.MobEffectCategory.BENEFICIAL),
    neutral(net.minecraft.potion.MobEffectCategory.NEUTRAL);

    public net.minecraft.potion.MobEffectCategory type;

    MobEffectCategory(net.minecraft.potion.MobEffectCategory type) {
        this.type = type;
    }
}