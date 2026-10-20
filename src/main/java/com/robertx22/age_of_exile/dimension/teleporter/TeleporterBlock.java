package com.robertx22.age_of_exile.dimension.teleporter;

import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.vanilla_mc.blocks.bases.OpaqueBlock;
import com.robertx22.age_of_exile.vanilla_mc.packets.SendActionToClient;
import com.robertx22.library_of_exile.main.Packets;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.EntityBlock;
import com.robertx22.forge_compat.material.Material;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;

public class TeleporterBlock extends OpaqueBlock implements EntityBlock {

    public TeleporterBlock() {
        super(Properties.of(Material.STONE)
            .strength(5F, 2));
    }

    /*
    @Override
    @Deprecated
    public List<ItemStack> getDroppedStacks(BlockState blockstate, LootContext.Builder context) {
        ArrayList<ItemStack> items = new ArrayList<ItemStack>();
        items.add(new ItemStack(this));
        return items;
    }

     */

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TeleportedBlockEntity(pos, state);
    }

    public BlockEntity newBlockEntity(BlockGetter world) {
        return new TeleportedBlockEntity();
    }

    @Override
    @Deprecated
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player,
                                InteractionHand hand, BlockHitResult ray) {

        ItemStack stack = player.getItemInHand(hand);

        BlockEntity tile = world.getBlockEntity(pos);

        if (tile instanceof TeleportedBlockEntity) {

            TeleportedBlockEntity be = (TeleportedBlockEntity) tile;

            if (!world.isClientSide()) {

                if (!be.data.activated) {
                    if (be.data.type.isDungeon()) {
                        Load.playerRPGData(player)
                            .createRandomDungeon(ExileDB.Difficulties()
                                .random());
                        be.data.activated = true;
                    }
                }

                Load.playerRPGData(player)
                    .syncToClient(player);

                Packets.sendToClient(player, new SendActionToClient(pos, SendActionToClient.Action.OPEN_MAPS_GUI));

                return InteractionResult.SUCCESS;
            }

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.CONSUME;
    }
}
