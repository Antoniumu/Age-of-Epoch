package com.robertx22.infinite_dungeons.block;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.EntityBlock;
import com.robertx22.forge_compat.material.Material;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.BlockGetter;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ControlBlock extends Block implements EntityBlock {

    public ControlBlock() {
        super(BlockBehaviour.Properties.of(Material.STONE)
            .noOcclusion()
            .lightLevel(x -> 15));
    }

    @Override
    @Deprecated
    public List<ItemStack> getDrops(BlockState blockstate, LootContext.Builder context) {
        ArrayList<ItemStack> items = new ArrayList<ItemStack>();
        return items;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockGetter p_196283_1_) {
        return new ControlBlockEntity();
    }
}
