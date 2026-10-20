package com.robertx22.forge_compat.common;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.crafting.Ingredient;

public interface ArmorMaterial {
    ArmorMaterial LEATHER = ArmorMaterials.LEATHER;
    ArmorMaterial CHAIN = ArmorMaterials.CHAIN;
    ArmorMaterial IRON = ArmorMaterials.IRON;
    ArmorMaterial GOLD = ArmorMaterials.GOLD;
    ArmorMaterial DIAMOND = ArmorMaterials.DIAMOND;
    ArmorMaterial TURTLE = ArmorMaterials.TURTLE;
    ArmorMaterial NETHERITE = ArmorMaterials.NETHERITE;

    int getDurabilityForSlot(EquipmentSlot slotIn);

    int getDefenseForSlot(EquipmentSlot slotIn);

    int getEnchantmentValue();

    SoundEvent getEquipSound();

    Ingredient getRepairIngredient();

    String getName();

    float getToughness();

    float getKnockbackResistance();
}
