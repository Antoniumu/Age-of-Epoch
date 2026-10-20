package com.robertx22.infinite_dungeons.database.dungeon_modifiers.geared_mobs;

import com.robertx22.infinite_dungeons.block.ControlBlockEntity;
import com.robertx22.infinite_dungeons.database.db_types.dungeon_modifiers.DungeonModifier;
import com.robertx22.infinite_dungeons.database.db_types.dungeon_modifiers.ModifierType;
import com.robertx22.library_of_exile.utils.RandomUtils;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class MeleeSkeletonChance extends DungeonModifier {

    public int chance;

    public MeleeSkeletonChance(String id, int chance) {
        super(id);
        this.chance = chance;
        this.weight = 0;
    }

    @Override
    public ModifierType getModifierType() {
        return ModifierType.meleeSkeletons(0);
    }

    @Override
    public void onControlBlockTick(ControlBlockEntity en) {

    }

    @Override
    public void onMobSpawn(LivingEntity en) {
        if (RandomUtils.roll(chance)) {
            if (en.getType() == net.minecraft.world.entity.EntityTypes.SKELETON) {
                en.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            }
        }
    }
}
