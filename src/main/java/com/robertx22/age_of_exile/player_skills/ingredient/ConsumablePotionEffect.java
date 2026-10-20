package com.robertx22.age_of_exile.player_skills.ingredient;

import com.robertx22.age_of_exile.saveclasses.player_skills.PlayerSkillEnum;
import com.robertx22.age_of_exile.uncommon.interfaces.IAutoLocName;
import com.robertx22.age_of_exile.vanilla_mc.potion_effects.IOneOfATypePotion;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeMap;

public class ConsumablePotionEffect extends MobEffect implements IOneOfATypePotion, IAutoLocName {

    public PlayerSkillEnum profession;

    public ConsumablePotionEffect(PlayerSkillEnum profession) {
        super(MobEffectCategory.BENEFICIAL, 0);
        this.profession = profession;
    }

    @Override
    public void addAttributeModifiers(AttributeMap attributes, int amplifier) {
        super.addAttributeModifiers(attributes, amplifier);
    }

    @Override
    public void removeAttributeModifiers(AttributeMap attributes) {
        try {
            super.removeAttributeModifiers(attributes);
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
