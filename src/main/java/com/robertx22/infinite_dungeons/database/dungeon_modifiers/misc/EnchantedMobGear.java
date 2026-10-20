package com.robertx22.infinite_dungeons.database.dungeon_modifiers.misc;

import com.robertx22.infinite_dungeons.block.ControlBlockEntity;
import com.robertx22.infinite_dungeons.database.db_types.dungeon_modifiers.DungeonModifier;
import com.robertx22.infinite_dungeons.database.db_types.dungeon_modifiers.ModifierType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class EnchantedMobGear extends DungeonModifier {

    public int enchantLevels;

    public EnchantedMobGear(String id, int enchantLevels) {
        super(id);
        this.enchantLevels = enchantLevels;
        this.order = 5;
        this.weight = weight * 10 / enchantLevels;
    }

    @Override
    public ModifierType getModifierType() {
        return ModifierType.enchantedMobGear(enchantLevels);
    }

    @Override
    public void onControlBlockTick(ControlBlockEntity en) {

    }

    @Override
    public void onMobSpawn(LivingEntity en) {
        for (ItemStack stack : getAllGearOn(en)) {
        }
    }
}
