package com.robertx22.age_of_exile.player_skills.ingredient;

import com.robertx22.age_of_exile.saveclasses.player_skills.PlayerSkillEnum;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.interfaces.IAutoLocName;
import com.robertx22.age_of_exile.vanilla_mc.potion_effects.IOneOfATypePotion;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.core.Registry;

import net.minecraft.core.registries.BuiltInRegistries;
public class ConsumablePotionEffect extends MobEffect implements IOneOfATypePotion, IAutoLocName {

    public PlayerSkillEnum profession;

    public ConsumablePotionEffect(PlayerSkillEnum profession) {
        super(MobEffectCategory.BENEFICIAL, 0);
        this.profession = profession;
    }

    @Override
    public void addAttributeModifiers(LivingEntity entity, AttributeMap attributes, int amplifier) {
        Load.Unit(entity)
            .forceRecalculateStats();
        super.addAttributeModifiers(entity, attributes, amplifier);
    }

    @Override
    public void removeAttributeModifiers(LivingEntity target, AttributeMap attributes,
                                         int amplifier) {

        try {
            Load.Unit(target)
                .forceRecalculateStats();
            super.removeAttributeModifiers(target, attributes, amplifier);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getOneOfATypeType() {
        return profession.id;
    }

    @Override
    public AutoLocGroup locNameGroup() {
        return AutoLocGroup.Potions;
    }

    @Override
    public String locNameLangFileGUID() {
        return BuiltInRegistries.MOB_EFFECT.getKey(this)
            .toString();
    }

    @Override
    public String locNameForLangFile() {
        return profession.word.locNameForLangFile();
    }

    @Override
    public String GUID() {
        return "";
    }

}
