package com.robertx22.ancient_obelisks.block;

import com.robertx22.ancient_obelisks.block_entity.ObeliskRewardBE;
import com.robertx22.ancient_obelisks.main.ObeliskRewardLogic;
import com.robertx22.ancient_obelisks.main.ObelisksMain;
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

public class ObeliskRewardBlock extends BaseEntityBlock {
    public ObeliskRewardBlock() {
        super(BlockBehaviour.Properties.of().strength(2).noOcclusion());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new ObeliskRewardBE(pPos, pState);
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
                        if (be instanceof ObeliskRewardBE sbe) {
                            if (sbe.ticks % 20 == 0) {
                                ObelisksMain.ifMapData(world, pos).ifPresent(x -> {
                                    if (x.canSpawnRewards && !x.spawnedRewards) {
                                        x.spawnedRewards = true;
                                        ObeliskRewardLogic.spawnChests(x, world, pos);
                                    }
                                });
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return (com.mojang.serialization.MapCodec) simpleCodec(p -> this);
    }

}
