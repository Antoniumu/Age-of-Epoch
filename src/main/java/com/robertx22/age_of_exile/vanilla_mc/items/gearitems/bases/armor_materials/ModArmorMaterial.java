package com.robertx22.age_of_exile.vanilla_mc.items.gearitems.bases.armor_materials;

import com.robertx22.forge_compat.common.ArmorMaterial;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

public class ModArmorMaterial implements ArmorMaterial {

    ArmorTier tier;
    ArmorType type;
    boolean isUnique;

    public ModArmorMaterial(ArmorTier tier, ArmorType type, boolean isUnique) {
        this.tier = tier;
        this.type = type;
        this.isUnique = isUnique;
    }

    @Override
    public int getDurabilityForSlot(EquipmentSlot slotIn) {
        return 100 + (int) (this.tier.vanillaMat.getDurabilityForSlot(slotIn) * getExtraMulti());
    }

    @Override
    public int getDefenseForSlot(EquipmentSlot slotIn) {
        return tier.vanillaMat.getDefenseForSlot(slotIn);
    }

    @Override
    public int getEnchantmentValue() {
        return (int) (tier.vanillaMat.getEnchantmentValue() * getExtraMulti());
    }

    @Override
    public float getToughness() {
        return tier.vanillaMat.getToughness();
    }

    @Override
    public float getKnockbackResistance() {
        return tier.vanillaMat.getKnockbackResistance();
    }

    @Override
    public String getName() {
        return type.id + "/" + tier.tier;
    }

    private float getExtraMulti() {
        return isUnique ? 1.2F : 1F;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_IRON.value();
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(Items.STRUCTURE_BLOCK);
    }

}
