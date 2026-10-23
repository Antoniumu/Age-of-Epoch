package com.robertx22.infinite_dungeons.database.dungeon_modifiers.geared_mobs;

import com.robertx22.infinite_dungeons.block.ControlBlockEntity;
import com.robertx22.infinite_dungeons.database.db_types.dungeon_modifiers.DungeonModifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public abstract class BaseGearMobsModifier extends DungeonModifier {

    public BaseGearMobsModifier(String id) {
        super(id);
    }

    @Override
    public void onControlBlockTick(ControlBlockEntity en) {

    }

    @Override
    public void onMobSpawn(LivingEntity en) {

    }

    public void equipMobIfEmpty(LivingEntity en, EquipmentSlot slot, ItemStack stack) {
        if (en.getItemBySlot(slot)
            .isEmpty()) {
            en.setItemSlot(slot, stack);
        }
    }

    public void disableMobGearDrops(LivingEntity en) {

        if (en instanceof Mob) {
            Mob mob = (Mob) en;

            mob.setDropChance(EquipmentSlot.CHEST, 0);
            mob.setDropChance(EquipmentSlot.FEET, 0);
            mob.setDropChance(EquipmentSlot.LEGS, 0);
            mob.setDropChance(EquipmentSlot.HEAD, 0);
            mob.setDropChance(EquipmentSlot.MAINHAND, 0);
            mob.setDropChance(EquipmentSlot.OFFHAND, 0);
        }

    }
}
