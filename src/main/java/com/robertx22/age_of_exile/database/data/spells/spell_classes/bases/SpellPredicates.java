package com.robertx22.age_of_exile.database.data.spells.spell_classes.bases;

import com.robertx22.age_of_exile.database.data.gear_types.bases.BaseGearType;
import com.robertx22.age_of_exile.saveclasses.item_classes.GearItemData;
import com.robertx22.age_of_exile.uncommon.datasaving.Gear;
import com.robertx22.age_of_exile.uncommon.wrappers.SText;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.ChatFormatting;

import java.util.function.Predicate;

public class SpellPredicates {
    private static Predicate<LivingEntity> SHOOTABLE_PRED = x -> {
        try {
            GearItemData data = Gear.Load(x.getMainHandItem());
            return data != null && data.GetBaseGearType()
                .getTags()
                .contains(BaseGearType.SlotTag.ranger_casting_weapon);
        } catch (Exception e) {
            return false;
        }

    };

    private static Predicate<LivingEntity> MELEE_PRED = x -> {
        try {
            GearItemData data = Gear.Load(x.getMainHandItem());
            return data != null && data.GetBaseGearType()
                .getTags()
                .contains(BaseGearType.SlotTag.melee_weapon);
        } catch (Exception e) {
            return false;
        }
    };

    private static Predicate<LivingEntity> MAGE_PRED = x -> {
        try {
            GearItemData data = Gear.Load(x.getMainHandItem());
            return data != null && data.GetBaseGearType()
                .getTags()
                .contains(BaseGearType.SlotTag.mage_weapon);
        } catch (Exception e) {
            return false;
        }
    };
    private static Predicate<LivingEntity> NON_MAGE_PRED = x -> {
        return !MAGE_PRED.test(x);
    };
    private static Predicate<LivingEntity> ANY = x -> {
        return true;
    };

    public static SpellPredicate REQUIRE_SHOOTABLE = new SpellPredicate(SHOOTABLE_PRED, net.minecraft.network.chat.Component.literal(ChatFormatting.GREEN + "Требуется оружие дальнего боя."));
    public static SpellPredicate REQUIRE_MAGE_WEAPON = new SpellPredicate(MAGE_PRED, net.minecraft.network.chat.Component.literal(ChatFormatting.GREEN + "Требуется магическое оружие."));
    public static SpellPredicate REQUIRE_MELEE = new SpellPredicate(MELEE_PRED, net.minecraft.network.chat.Component.literal(ChatFormatting.GOLD + "Требуется оружие ближнего боя."));
    public static SpellPredicate ANY_ITEM = new SpellPredicate(ANY, net.minecraft.network.chat.Component.literal(ChatFormatting.GOLD + "Можно применять с любым оружием."));
    public static SpellPredicate NON_MAGE_WEAPON = new SpellPredicate(NON_MAGE_PRED, net.minecraft.network.chat.Component.literal(ChatFormatting.GOLD + "Можно применять с немагическим оружием."));

}

