package com.robertx22.ancient_obelisks.block_entity;

import com.robertx22.ancient_obelisks.main.ObeliskEntries;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ObeliskMobSpawnerBE extends BlockEntity {

    public int tick = 0;

    public ObeliskMobSpawnerBE(BlockPos pPos, BlockState pBlockState) {
        super(ObeliskEntries.SPAWNER_BE.get(), pPos, pBlockState);
    }
    


}
