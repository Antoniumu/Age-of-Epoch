package com.robertx22.age_of_exile.dimension.teleporter;

import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.vanilla_mc.blocks.bases.OpaqueBlock;
import com.robertx22.age_of_exile.vanilla_mc.packets.SendActionToClient;
import com.robertx22.library_of_exile.main.Packets;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;

/**
 * Блок-портал. По нажатию только открывает экран ({@link
 * com.robertx22.age_of_exile.gui.screens.dungeon.PortalScreen}).
 *
 * <p>Раньше здесь сразу вызывался {@code createRandomDungeon(random)} — то есть
 * подземелье создавалось со случайной сложностью ещё до того, как игрок что-то
 * выбрал, а сам экран показывал уже «выпавшую» сложность. Теперь сложность
 * выбирается в интерфейсе, а подземелье создаётся в
 * {@link com.robertx22.age_of_exile.dimension.packets.StartDungeonPacket}.</p>
 */
public class TeleporterBlock extends OpaqueBlock implements EntityBlock {

    public TeleporterBlock() {
        super(Properties.of()
            .strength(5F, 2));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TeleportedBlockEntity(pos, state);
    }

    public BlockEntity newBlockEntity(BlockGetter world) {
        return new TeleportedBlockEntity();
    }

    private InteractionResult openTeleporter(Level world, BlockPos pos, Player player) {
        BlockEntity tile = world.getBlockEntity(pos);

        if (tile instanceof TeleportedBlockEntity) {

            if (!world.isClientSide()) {
                // клиенту нужны актуальные данные капабилити (уровень, текущее
                // подземелье), иначе экран покажет устаревшую информацию
                Load.playerRPGData(player)
                    .syncToClient(player);

                Packets.sendToClient(player, new SendActionToClient(pos, SendActionToClient.Action.OPEN_MAPS_GUI));
            }

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult ray) {
        return openTeleporter(world, pos, player);
    }

    @Override
    @Deprecated
    public InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player,
                                       InteractionHand hand, BlockHitResult ray) {
        return openTeleporter(world, pos, player);
    }
}
