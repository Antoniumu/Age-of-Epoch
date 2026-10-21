package com.robertx22.age_of_exile.dimension.spawner;

import com.robertx22.age_of_exile.vanilla_mc.blocks.bases.OpaqueBlock;
import com.robertx22.forge_compat.material.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ModSpawnerBlock extends OpaqueBlock implements EntityBlock {

    public ModSpawnerBlock() {
        super(Properties.of()
            .strength(5F, 2));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ModSpawnerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(
        net.minecraft.world.level.Level level,
        BlockState state,
        net.minecraft.world.level.block.entity.BlockEntityType<T> type
    ) {
        return (lvl, pos, st, be) -> {
            if (be instanceof com.robertx22.forge_compat.blocks.ITickableBlockEntity tickable) {
                tickable.tick();
            }
        };
    }

    public BlockEntity newBlockEntity(BlockGetter world) {
        return new ModSpawnerBlockEntity();
    }

}
