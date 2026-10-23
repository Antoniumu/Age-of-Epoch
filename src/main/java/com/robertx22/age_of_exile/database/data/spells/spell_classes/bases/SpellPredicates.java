package com.robertx22.age_of_exile.database.data.spells.spell_classes.bases;

import com.robertx22.age_of_exile.aoe_data.database.gear_slots.GearSlots;
import com.robertx22.age_of_exile.database.data.gear_slots.GearSlot;
import com.robertx22.age_of_exile.database.data.gear_types.bases.BaseGearType;
import com.robertx22.age_of_exile.saveclasses.item_classes.GearItemData;
import com.robertx22.age_of_exile.uncommon.datasaving.Gear;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

public class SpellPredicates {
    private static Predicate<LivingEntity> SHOOTABLE_PRED = x -> {
        try {
            ItemStack stack = x.getMainHandItem();
            if (stack.isEmpty()) {
                return false;
            }
            GearItemData data = Gear.Load(stack);
            if (data != null) {
                return data.GetBaseGearType()
                    .getTags()
                    .contains(BaseGearType.SlotTag.ranger_casting_weapon);
            }
            GearSlot slot = GearSlot.getSlotOf(stack.getItem());
            return slot != null && (GearSlots.BOW.equals(slot.GUID()) || GearSlots.CROSBOW.equals(slot.GUID()));
        } catch (Exception e) {
            return false;
        }
    };

    private static Predicate<LivingEntity> MELEE_PRED = x -> {
        try {
            ItemStack stack = x.getMainHandItem();
            if (stack.isEmpty()) {
                return false;
            }
            GearItemData data = Gear.Load(stack);
            if (data != null) {
                return data.GetBaseGearType()
                    .getTags()
                    .contains(BaseGearType.SlotTag.melee_weapon);
            }
            GearSlot slot = GearSlot.getSlotOf(stack.getItem());
            return slot != null && (GearSlots.SWORD.equals(slot.GUID()) || GearSlots.AXE.equals(slot.GUID()));
        } catch (Exception e) {
            return false;
        }
    };

    private static Predicate<LivingEntity> MAGE_PRED = x -> {
        try {
            ItemStack stack = x.getMainHandItem();
            if (stack.isEmpty()) {
                return false;
            }
            GearItemData data = Gear.Load(stack);
            if (data != null) {
                return data.GetBaseGearType()
                    .getTags()
                    .contains(BaseGearType.SlotTag.mage_weapon);
            }
            GearSlot slot = GearSlot.getSlotOf(stack.getItem());
            return slot != null && (GearSlots.STAFF.equals(slot.GUID()) || GearSlots.SCEPTER.equals(slot.GUID()));
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
