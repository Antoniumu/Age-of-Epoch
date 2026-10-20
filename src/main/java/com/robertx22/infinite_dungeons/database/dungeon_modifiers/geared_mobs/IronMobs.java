package com.robertx22.infinite_dungeons.database.dungeon_modifiers.geared_mobs;

import com.robertx22.infinite_dungeons.database.db_types.dungeon_modifiers.ModifierType;
import com.robertx22.infinite_dungeons.database.ids.DungeonModifierIds;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class IronMobs extends BaseGearMobsModifier {

    public IronMobs() {
        super(DungeonModifierIds.IRON_MOBS);
        this.weight = 500;
    }

    @Override
    public ModifierType getModifierType() {
        return ModifierType.gearedMobs(1);
    }

    @Override
    public void onMobSpawn(LivingEntity en) {

        if (en instanceof Mob) {
            disableMobGearDrops(en);
        }

        equipMobIfEmpty(en, EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        equipMobIfEmpty(en, EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
        equipMobIfEmpty(en, EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        equipMobIfEmpty(en, EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));

        equipMobIfEmpty(en, EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
    }
}
