package com.robertx22.forge_compat.common;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

public enum ArmorMaterials implements ArmorMaterial {
    LEATHER("leather", 5, new int[]{1, 2, 3, 1}, 15, 0.0F, 0.0F),
    CHAIN("chainmail", 15, new int[]{1, 4, 5, 2}, 12, 0.0F, 0.0F),
    IRON("iron", 15, new int[]{2, 5, 6, 2}, 9, 0.0F, 0.0F),
    GOLD("gold", 7, new int[]{1, 3, 5, 2}, 25, 0.0F, 0.0F),
    DIAMOND("diamond", 33, new int[]{3, 6, 8, 3}, 10, 2.0F, 0.0F),
    TURTLE("turtle", 25, new int[]{2, 5, 6, 2}, 9, 0.0F, 0.0F),
    NETHERITE("netherite", 37, new int[]{3, 6, 8, 3}, 15, 3.0F, 0.1F);

    private static final int[] HEALTH_PER_SLOT = new int[]{13, 15, 16, 11};
    private final String name;
    private final int durabilityMultiplier;
    private final int[] slotProtections;
    private final int enchantmentValue;
    private final float toughness;
    private final float knockbackResistance;

    ArmorMaterials(String name, int durabilityMultiplier, int[] slotProtections, int enchantmentValue, float toughness, float knockbackResistance) {
        this.name = name;
        this.durabilityMultiplier = durabilityMultiplier;
        this.slotProtections = slotProtections;
        this.enchantmentValue = enchantmentValue;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
    }

    private int slotIndex(EquipmentSlot slot) {
        return switch (slot) {
            case FEET -> 0;
            case LEGS -> 1;
            case CHEST -> 2;
            case HEAD -> 3;
            default -> 0;
        };
    }

    @Override
    public int getDurabilityForSlot(EquipmentSlot slot) {
        return HEALTH_PER_SLOT[slotIndex(slot)] * this.durabilityMultiplier;
    }

    @Override
    public int getDefenseForSlot(EquipmentSlot slot) {
        return this.slotProtections[slotIndex(slot)];
    }

    @Override
    public int getEnchantmentValue() {
        return this.enchantmentValue;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_IRON.value();
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(Items.IRON_INGOT);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public float getToughness() {
        return this.toughness;
    }

    @Override
    public float getKnockbackResistance() {
        return this.knockbackResistance;
    }
}
