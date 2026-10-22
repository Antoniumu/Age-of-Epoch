package com.robertx22.the_harvest.block_entity;

import com.robertx22.the_harvest.main.HarvestEntries;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class HarvestSpawnerBE extends BlockEntity {

    public int tick = 0;

    public HarvestSpawnerBE(BlockPos pPos, BlockState pBlockState) {
        super(HarvestEntries.SPAWNER_BE.get(), pPos, pBlockState);
    }



}
