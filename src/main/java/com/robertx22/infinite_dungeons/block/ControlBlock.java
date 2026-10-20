package com.robertx22.infinite_dungeons.block;

import com.robertx22.forge_compat.material.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ControlBlock extends Block implements EntityBlock {

    public ControlBlock() {
        super(BlockBehaviour.Properties.of()
            .noOcclusion()
            .lightLevel(x -> 15));
    }

    @Override
    public List<ItemStack> getDrops(BlockState blockstate, LootParams.Builder context) {
        return new ArrayList<>();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ControlBlockEntity(pos, state);
    }

    @Nullable
    public BlockEntity newBlockEntity(BlockGetter p_196283_1_) {
        return new ControlBlockEntity();
    }
}
