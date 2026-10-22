package com.robertx22.age_of_exile.vanilla_mc.blocks;

import com.robertx22.age_of_exile.vanilla_mc.blocks.bases.BaseModificationStation;
import com.robertx22.age_of_exile.vanilla_mc.blocks.bases.OpaqueBlock;
import com.robertx22.forge_compat.api.distmarker.Dist;
import com.robertx22.forge_compat.api.distmarker.OnlyIn;
import com.robertx22.forge_compat.fml.network.NetworkHooks;
import com.robertx22.library_of_exile.tile_bases.NonFullBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseModificationBlock extends OpaqueBlock implements EntityBlock {

    protected BaseModificationBlock(BlockBehaviour.Properties prop) {
        super(prop.lightLevel(x -> x.getValue(NonFullBlock.light) ? 10 : 0));

    }

    @Override
    public MenuProvider getMenuProvider(BlockState state, Level world, BlockPos pos) {
        return (MenuProvider) world.getBlockEntity(pos);
    }

    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(
        Level level,
        BlockState state,
        net.minecraft.world.level.block.entity.BlockEntityType<T> type
    ) {
        return (lvl, pos, st, be) -> {
            if (be instanceof com.robertx22.forge_compat.blocks.ITickableBlockEntity tickable) {
                tickable.tick();
            }
        };
    }

    @Override
    @Deprecated
    public List<ItemStack> getDrops(BlockState blockstate, LootParams.Builder context) {

        ArrayList<ItemStack> items = new ArrayList<ItemStack>();
        items.add(new ItemStack(this));

        BlockEntity tileentity = context.getOptionalParameter(LootContextParams.BLOCK_ENTITY);

        if (tileentity instanceof BaseModificationStation) {

            BaseModificationStation inv = (BaseModificationStation) tileentity;

            for (ItemStack stack : inv.itemStacks) {
                if (stack.isEmpty() == false) {
                    items.add(stack);
                }
            }

        }

        return items;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public float getShadeBrightness(BlockState state, BlockGetter worldIn, BlockPos pos) {
        return 1.0F;
    }

    public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
        return true;
    }

    private InteractionResult openStation(Level world, BlockPos pos, Player player) {
        BlockEntity tile = world.getBlockEntity(pos);
        if (!(tile instanceof BaseModificationStation station)) {
            return InteractionResult.PASS;
        }
        if (!world.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openGui(serverPlayer, station, pos);
            com.robertx22.library_of_exile.main.Packets.sendToClient(
                serverPlayer,
                new com.robertx22.library_of_exile.packets.TileUpdatePacket(station)
            );
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult ray) {
        return openStation(world, pos, player);
    }

    @Override
    @Deprecated
    public InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player,
                                       InteractionHand hand, BlockHitResult ray) {
        return openStation(world, pos, player);
    }

}
