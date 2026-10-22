package com.robertx22.ancient_obelisks.block;

import com.robertx22.ancient_obelisks.block_entity.ObeliskBE;
import com.robertx22.ancient_obelisks.item.ObeliskItemMapData;
import com.robertx22.ancient_obelisks.item.ObeliskItemNbt;
import com.robertx22.ancient_obelisks.item.ObeliskMapItem;
import com.robertx22.ancient_obelisks.main.ObeliskEntries;
import com.robertx22.ancient_obelisks.main.ObeliskWords;
import com.robertx22.ancient_obelisks.main.ObelisksMain;
import com.robertx22.ancient_obelisks.structure.ObeliskMapCapability;
import com.robertx22.ancient_obelisks.structure.ObeliskMapData;
import com.robertx22.library_of_exile.components.PlayerDataCapability;
import com.robertx22.library_of_exile.dimension.MapDimensions;
import com.robertx22.library_of_exile.utils.PlayerUtil;
import com.robertx22.library_of_exile.utils.SoundUtils;
import com.robertx22.library_of_exile.utils.TeleportUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.List;

public class ObeliskBlock extends BaseEntityBlock {
    public ObeliskBlock() {
        super(BlockBehaviour.Properties.of().strength(10).noOcclusion().lightLevel(x -> 10));
    }

    @Override
    public List<ItemStack> getDrops(BlockState pState, LootParams.Builder pParams) {

        List<ItemStack> all = new ArrayList<>();
        BlockEntity blockentity = pParams.getOptionalParameter(LootContextParams.BLOCK_ENTITY);

        if (blockentity instanceof ObeliskBE be) {
            all.add(asItem().getDefaultInstance());
        }

        return all;
    }

    public static void startNewMap(Player p, ItemStack stack, ObeliskBE be) {

        ObeliskItemMapData map = ObeliskItemNbt.OBELISK_MAP.loadFrom(stack);

        var count = map.getOrSetStartPos(p.level(), stack);
        var start = ObelisksMain.OBELISK_MAP_STRUCTURE.getStartFromCounter(count.x, count.z);
        var pos = TeleportUtils.getSpawnTeleportPos(ObelisksMain.OBELISK_MAP_STRUCTURE, start.getMiddleBlockPosition(5));

        var pdata = PlayerDataCapability.get(p);

        var data = new ObeliskMapData();
        data.item = map;
        data.x = start.x;
        data.z = start.z;

        be.x = count.x;
        be.z = count.z;

        be.currentWorldUUID = ObeliskMapCapability.get(p.level()).data.data.uuid;


        be.setChanged();

        stack.shrink(1);

        ObeliskMapCapability.get(p.level()).data.data.setData(p, data, ObelisksMain.OBELISK_MAP_STRUCTURE, start.getMiddleBlockPosition(5));

        pdata.mapTeleports.entranceTeleportLogic(p, ObelisksMain.DIMENSION_KEY, pos);

    }

    public static void joinCurrentMap(Player p, ObeliskBE be) {

        var start = ObelisksMain.OBELISK_MAP_STRUCTURE.getStartFromCounter(be.x, be.z);
        var pos = TeleportUtils.getSpawnTeleportPos(ObelisksMain.OBELISK_MAP_STRUCTURE, start.getMiddleBlockPosition(5));
        var pdata = PlayerDataCapability.get(p);
        pdata.mapTeleports.entranceTeleportLogic(p, ObelisksMain.DIMENSION_KEY, pos);
    }


    
    public InteractionResult use(BlockState pState, Level world, BlockPos pPos, Player p, InteractionHand pHand, BlockHitResult pHit) {

        if (!world.isClientSide()) {
            var be = world.getBlockEntity(pPos);

            if (be instanceof ObeliskBE obe) {
                ItemStack stack = p.getMainHandItem();

                if (ObeliskItemNbt.OBELISK_MAP.has(stack)) {
                    ObeliskItemMapData map = ObeliskItemNbt.OBELISK_MAP.loadFrom(stack);

                    if (map.relic && !MapDimensions.isMap(world)) {
                        p.sendSystemMessage(ObeliskWords.RELIC_MAPS_ONLY.get().withStyle(ChatFormatting.RED));
                        return InteractionResult.SUCCESS;
                    }

                    ObelisksMain.debugMsg(p, "Trying to start new map");
                    startNewMap(p, stack, obe);
                    ObelisksMain.debugMsg(p, "Map started");
                } else {
                    if (obe.isActivated()) {
                        ObelisksMain.debugMsg(p, "Trying to join existing map");
                        joinCurrentMap(p, obe);
                    } else {
                        // only obelisks found in maps give maps
                        if (MapDimensions.isMap(world)) {
                            if (!obe.gaveMap) {
                                obe.setGaveMap();
                                var map = ObeliskMapItem.blankMap(ObeliskEntries.OBELISK_MAP_ITEM.get().getDefaultInstance(), true);
                                PlayerUtil.giveItem(map, p);
                                SoundUtils.playSound(p, com.robertx22.forge_compat.common.CompatHelper.unwrapSound(SoundEvents.ITEM_PICKUP));
                                p.sendSystemMessage(ObeliskWords.NEW_MAP_GIVEN.get().withStyle(ChatFormatting.LIGHT_PURPLE));
                            } else {
                                ObelisksMain.debugMsg(p, "Obelisk is not activated and already gave a map");
                            }
                        }
                    }

                }
            } else {
                ObelisksMain.debugMsg(p, "Missing Block entity");
            }
        }

        return InteractionResult.SUCCESS;
    }


    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new ObeliskBE(pPos, pState);
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        return new BlockEntityTicker<T>() {
            @Override
            public void tick(Level pLevel, BlockPos pPos, BlockState pState, T pBlockEntity) {
                // todo
            }
        };
    }


    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return (com.mojang.serialization.MapCodec) simpleCodec(p -> this);
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
