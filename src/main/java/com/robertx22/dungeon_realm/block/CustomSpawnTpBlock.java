package com.robertx22.dungeon_realm.block;

import com.robertx22.dungeon_realm.main.DungeonMain;
import com.robertx22.library_of_exile.components.PlayerDataCapability;
import com.robertx22.library_of_exile.dimension.structure.MapStructure;
import com.robertx22.library_of_exile.utils.TeleportUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.function.Supplier;

public class CustomSpawnTpBlock extends Block {

    Supplier<MapStructure> structure;


    public CustomSpawnTpBlock(Supplier<MapStructure> structure) {
        super(BlockBehaviour.Properties.of().strength(2).noOcclusion());
        this.structure = structure;
    }

    
    public InteractionResult use(BlockState pState, Level level, BlockPos pPos, Player p, InteractionHand pHand, BlockHitResult pHit) {


        if (!level.isClientSide()) {

            if (level instanceof ServerLevel == false) {
                // can this happen?
                return InteractionResult.SUCCESS;
            }
            
            DungeonMain.ifMapData(level, pPos).ifPresent(x -> {
                MapStructure<?> mapStructure = structure.get();
                if (mapStructure.isInside((ServerLevel) level, pPos)) {
                    PlayerDataCapability.get(p).mapTeleports.exitTeleportLogic(p);
                } else {
                    BlockPos pos = null;

                    if (x.spawnPositions.containsKey(mapStructure.guid())) {
                        pos = BlockPos.of(x.spawnPositions.get(mapStructure.guid()));
                    } else {
                        pos = TeleportUtils.getSpawnTeleportPos(mapStructure, pPos);
                    }
                    var dim = level.dimension().identifier();
                    PlayerDataCapability.get(p).mapTeleports.teleportToMap(p, dim, dim, pos);
                }
            });

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
