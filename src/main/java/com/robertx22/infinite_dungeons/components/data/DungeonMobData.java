package com.robertx22.infinite_dungeons.components.data;

import info.loenwind.autosave.annotations.Storable;
import info.loenwind.autosave.annotations.Store;
import net.minecraft.core.BlockPos;

@Storable
public class DungeonMobData {

    @Store
    public String dungeon_uuid = "";
    @Store
    public BlockPos control_block_pos = BlockPos.ZERO;

}
