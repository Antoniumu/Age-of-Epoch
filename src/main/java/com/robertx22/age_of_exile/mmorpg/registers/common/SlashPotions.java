package com.robertx22.age_of_exile.mmorpg.registers.common;

import com.google.common.base.Preconditions;
import com.robertx22.age_of_exile.database.data.exile_effects.MobEffectCategory;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.age_of_exile.mmorpg.registers.deferred_wrapper.Def;
import com.robertx22.age_of_exile.mmorpg.registers.deferred_wrapper.RegObj;
import com.robertx22.age_of_exile.player_skills.ingredient.ConsumablePotionEffect;
import com.robertx22.age_of_exile.saveclasses.player_skills.PlayerSkillEnum;
import com.robertx22.age_of_exile.vanilla_mc.potion_effects.ModStatusEffect;
import com.robertx22.age_of_exile.vanilla_mc.potion_effects.types.ExileStatusEffect;
import com.robertx22.age_of_exile.vanilla_mc.potion_effects.types.compat_food_effects.HealthRegenFoodEffect;
import com.robertx22.age_of_exile.vanilla_mc.potion_effects.types.compat_food_effects.ManaRegenFoodEffect;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class SlashPotions {

    public static ResourceLocation FOOD_HP = ResourceLocation.fromNamespaceAndPath(SlashRef.MODID, "food_health_regen");
    public static ResourceLocation FOOD_MANA = ResourceLocation.fromNamespaceAndPath(SlashRef.MODID, "food_mana_regen");

    public static HashMap<PlayerSkillEnum, RegObj<MobEffect>> CRAFTED_CONSUMABLES_MAP = new HashMap<>();

    public static RegObj<MobEffect> KNOCKBACK_RESISTANCE = Def.potion("knockback_resist", () -> new ModStatusEffect(net.minecraft.potion.MobEffectCategory.BENEFICIAL, 1)
        .addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE, "648D7564-6A60-4F59-8ABE-C2C27A6DD7A9", 0.75F, AttributeModifier.Operation.ADDITION));

    static HashMap<String, RegObj<ExileStatusEffect>> exileEffectsMap = new HashMap<>();

    public static ExileStatusEffect getExileEffect(String num) {
        Preconditions.checkArgument(exileEffectsMap.containsKey(num), num + " effect not there?");
        return exileEffectsMap.get(num)
            .get();
    }

    public static void init() {

        List<PlayerSkillEnum> professions = Arrays.asList(PlayerSkillEnum.ALCHEMY, PlayerSkillEnum.COOKING, PlayerSkillEnum.INSCRIBING);

        professions.forEach(x -> {
            RegObj<MobEffect> ef = Def.potion(x.id + "_consumable", () -> new ConsumablePotionEffect(x));
            CRAFTED_CONSUMABLES_MAP.put(x, ef);
        });

        for (int i = 0; i < 20; i++) {
            String key = ExileStatusEffect.getIdPath(MobEffectCategory.negative, i);
            ExileStatusEffect eff = new ExileStatusEffect(MobEffectCategory.negative, i);

            exileEffectsMap.put(key, Def.potion(key, () -> eff));
        }
        for (int i = 0; i < 40; i++) {
            String key = ExileStatusEffect.getIdPath(MobEffectCategory.beneficial, i);
            ExileStatusEffect eff = new ExileStatusEffect(MobEffectCategory.beneficial, i);

            exileEffectsMap.put(key, Def.potion(key, () -> eff));
        }

        Def.potion(FOOD_HP.getPath(), () -> HealthRegenFoodEffect.INSTANCE);
        Def.potion(FOOD_MANA.getPath(), () -> ManaRegenFoodEffect.INSTANCE);

    }

}
