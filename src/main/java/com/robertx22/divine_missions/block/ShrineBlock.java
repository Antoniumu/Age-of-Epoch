package com.robertx22.divine_missions.block;

import com.robertx22.divine_missions.components.PlayerMissionCap;
import com.robertx22.divine_missions.util.ClientOnly;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.List;

public class ShrineBlock extends Block {

    public ShrineBlock() {
        super(BlockBehaviour.Properties.of()
            .noOcclusion()
            .lightLevel(x -> 15));
    }

    @Override
    @Deprecated
    public List<ItemStack> getDrops(BlockState blockstate, LootParams.Builder context) {
        ArrayList<ItemStack> items = new ArrayList<ItemStack>();
        items.add(new ItemStack(this));
        return items;
    }

    private InteractionResult openShrine(Level world, Player player) {
        if (world.isClientSide()) {
            ClientOnly.openMissionsScreen();
        } else {
            PlayerMissionCap.get(player)
                .syncToClient(player);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult ray) {
        return openShrine(world, player);
    }

    @Override
    @Deprecated
    public InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player,
                                       InteractionHand hand, BlockHitResult ray) {
        return openShrine(world, player);
    }
}
