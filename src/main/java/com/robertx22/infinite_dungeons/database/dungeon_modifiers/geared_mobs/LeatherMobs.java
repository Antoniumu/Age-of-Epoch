package com.robertx22.infinite_dungeons.database.dungeon_modifiers.geared_mobs;

import com.robertx22.infinite_dungeons.database.db_types.dungeon_modifiers.ModifierType;
import com.robertx22.infinite_dungeons.database.ids.DungeonModifierIds;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class LeatherMobs extends BaseGearMobsModifier {

    public LeatherMobs() {
        super(DungeonModifierIds.LEATHER_MOBS);
    }

    @Override
    public ModifierType getModifierType() {
        return ModifierType.gearedMobs(0);
    }

    @Override
    public void onMobSpawn(LivingEntity en) {

        if (en instanceof Mob) {
            disableMobGearDrops(en);
        }

        equipMobIfEmpty(en, EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
        equipMobIfEmpty(en, EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));
        equipMobIfEmpty(en, EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        equipMobIfEmpty(en, EquipmentSlot.LEGS, new ItemStack(Items.LEATHER_LEGGINGS));

        equipMobIfEmpty(en, EquipmentSlot.MAINHAND, new ItemStack(Items.WOODEN_SWORD));

    }
}
