package com.robertx22.infinite_dungeons.database.dungeon_modifiers.geared_mobs;

import com.robertx22.infinite_dungeons.database.db_types.dungeon_modifiers.ModifierType;
import com.robertx22.infinite_dungeons.database.ids.DungeonModifierIds;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class NetheriteMobs extends BaseGearMobsModifier {

    public NetheriteMobs() {
        super(DungeonModifierIds.NETHERITE_MOBS);
        this.weight = 100;
    }

    @Override
    public ModifierType getModifierType() {
        return ModifierType.gearedMobs(3);
    }

    @Override
    public void onMobSpawn(LivingEntity en) {

        if (en instanceof Mob) {
            disableMobGearDrops(en);
        }

        equipMobIfEmpty(en, EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
        equipMobIfEmpty(en, EquipmentSlot.FEET, new ItemStack(Items.NETHERITE_BOOTS));
        equipMobIfEmpty(en, EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
        equipMobIfEmpty(en, EquipmentSlot.LEGS, new ItemStack(Items.NETHERITE_LEGGINGS));

        equipMobIfEmpty(en, EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_SWORD));
    }
}
