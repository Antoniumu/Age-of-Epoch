package com.robertx22.age_of_exile.vanilla_mc.blocks.runeword_station;

import com.robertx22.age_of_exile.vanilla_mc.blocks.BaseModificationBlock;
import com.robertx22.forge_compat.material.Material;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.BlockGetter;

public class RuneWordStationBlock extends BaseModificationBlock {

    public RuneWordStationBlock() {
        super(Properties.of(Material.STONE)
            .strength(5F, 2));
    }

    @Override
    public BlockEntity newBlockEntity(BlockGetter world) {
        return new RuneWordStationTile();
    }

}