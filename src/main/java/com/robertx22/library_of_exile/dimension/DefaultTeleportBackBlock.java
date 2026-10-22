package com.robertx22.library_of_exile.dimension;

import com.robertx22.library_of_exile.components.PlayerDataCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class DefaultTeleportBackBlock extends Block {

    public DefaultTeleportBackBlock(Properties pProperties) {
        super(pProperties);
    }

    
    public InteractionResult use(BlockState pState, Level world, BlockPos pPos, Player p, InteractionHand pHand, BlockHitResult pHit) {

        if (!world.isClientSide()) {
            if (p.isCrouching()) {
                PlayerDataCapability.get(p).mapTeleports.teleportHome(p);
            } else {
                PlayerDataCapability.get(p).mapTeleports.exitTeleportLogic(p);
            }
        }

        return InteractionResult.SUCCESS;
    }


    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        return this.use(state, world, pos, player, InteractionHand.MAIN_HAND, hit);
    }

    @Override
    public InteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return this.use(state, world, pos, player, hand, hit);
    }

}
