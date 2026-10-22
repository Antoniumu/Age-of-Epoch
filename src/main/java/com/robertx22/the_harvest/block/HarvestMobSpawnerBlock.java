package com.robertx22.the_harvest.block;

import com.robertx22.the_harvest.block_entity.HarvestSpawnerBE;
import com.robertx22.the_harvest.main.HarvestMain;
import com.robertx22.the_harvest.structure.HarvestMapCap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class HarvestMobSpawnerBlock extends BaseEntityBlock {
    public HarvestMobSpawnerBlock() {
        super(BlockBehaviour.Properties.of().strength(2).noOcclusion());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new HarvestSpawnerBE(pPos, pState);
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        return new BlockEntityTicker<T>() {
            @Override
            public void tick(Level world, BlockPos pos, BlockState pState, T be) {

                try {
                    if (!world.isClientSide()) {
                        if (be instanceof HarvestSpawnerBE sbe) {
                            if (sbe.tick++ % 20 == 0) {
                                var map = HarvestMapCap.get(world).data.data.getData(HarvestMain.HARVEST_MAP_STRUCTURE, sbe.getBlockPos());
                                if (map != null) {
                                    map.waveLogicSecond(world, pos);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        };
    }

}
