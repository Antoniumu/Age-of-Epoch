package com.robertx22.ancient_obelisks.block_entity;

import com.robertx22.ancient_obelisks.main.ObeliskEntries;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ObeliskRewardBE extends BlockEntity {

    public int ticks = 0;

    public ObeliskRewardBE(BlockPos pPos, BlockState pBlockState) {
        super(ObeliskEntries.OBELISK_REWARD_BE.get(), pPos, pBlockState);
    }


}
