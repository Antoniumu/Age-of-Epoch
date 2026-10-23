package com.robertx22.infinite_dungeons.database.dungeon_modifiers.misc;

import com.robertx22.infinite_dungeons.block.ControlBlockEntity;
import com.robertx22.infinite_dungeons.database.db_types.dungeon_modifiers.DungeonModifier;
import com.robertx22.infinite_dungeons.database.db_types.dungeon_modifiers.ModifierType;
import com.robertx22.infinite_dungeons.database.ids.DungeonModifierIds;
import com.robertx22.infinite_dungeons.database.ids.DungeonTypeIds;
import net.minecraft.world.entity.LivingEntity;

public class DoubleFasterMobSpawning extends DungeonModifier {

    public DoubleFasterMobSpawning() {
        super(DungeonModifierIds.FASTER_MOB_SPAWNING);
    }

    @Override
    public ModifierType getModifierType() {
        return ModifierType.fasterMobSpawning();
    }

    @Override
    public void onControlBlockTick(ControlBlockEntity en) {

    }

    @Override
    public void onMobSpawn(LivingEntity en) {

    }

    @Override
    public boolean canDungeonGetThis(ControlBlockEntity be) {
        return be.data.getDungeonLayout().dungeon_type.equals(DungeonTypeIds.SLAUGHTER);
    }

}
