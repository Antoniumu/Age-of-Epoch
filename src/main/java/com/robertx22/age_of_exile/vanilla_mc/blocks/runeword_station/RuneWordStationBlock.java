package com.robertx22.age_of_exile.vanilla_mc.blocks.runeword_station;

import com.robertx22.age_of_exile.vanilla_mc.blocks.BaseModificationBlock;
import com.robertx22.forge_compat.material.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class RuneWordStationBlock extends BaseModificationBlock {

    public RuneWordStationBlock() {
        super(Properties.of()
            .strength(5F, 2));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RuneWordStationTile(pos, state);
    }

    public BlockEntity newBlockEntity(BlockGetter world) {
        return new RuneWordStationTile();
    }

}
