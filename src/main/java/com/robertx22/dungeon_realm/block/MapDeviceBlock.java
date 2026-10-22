package com.robertx22.dungeon_realm.block;

import com.robertx22.dungeon_realm.api.CanEnterMapEvent;
import com.robertx22.dungeon_realm.api.CanStartMapEvent;
import com.robertx22.dungeon_realm.api.DungeonExileEvents;
import com.robertx22.dungeon_realm.api.OnStartMapEvent;
import com.robertx22.dungeon_realm.block_entity.MapDeviceBE;
import com.robertx22.dungeon_realm.block_entity.MapDeviceMenu;
import com.robertx22.dungeon_realm.item.DungeonItemMapData;
import com.robertx22.dungeon_realm.item.DungeonItemNbt;
import com.robertx22.dungeon_realm.main.DungeonEntries;
import com.robertx22.dungeon_realm.main.DungeonMain;
import com.robertx22.dungeon_realm.main.DungeonWords;
import com.robertx22.dungeon_realm.structure.DungeonMapCapability;
import com.robertx22.dungeon_realm.structure.DungeonMapData;
import com.robertx22.library_of_exile.components.LibMapCap;
import com.robertx22.library_of_exile.components.LibMapData;
import com.robertx22.library_of_exile.components.PlayerDataCapability;
import com.robertx22.library_of_exile.database.relic.stat.RelicStatsContainer;
import com.robertx22.library_of_exile.dimension.MapDimensions;
import com.robertx22.library_of_exile.utils.TeleportUtils;
import com.robertx22.library_of_exile.utils.geometry.Circle2d;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
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

public class MapDeviceBlock extends BaseEntityBlock {
    public MapDeviceBlock() {
        super(BlockBehaviour.Properties.of().strength(10).noOcclusion().lightLevel(x -> 10));
    }

    @Override
    public List<ItemStack> getDrops(BlockState pState, LootParams.Builder pParams) {

        List<ItemStack> all = new ArrayList<>();

        BlockEntity blockentity = pParams.getOptionalParameter(LootContextParams.BLOCK_ENTITY);

        if (blockentity instanceof MapDeviceBE be) {
            all.add(asItem().getDefaultInstance());

            for (int i = 0; i < be.inv.getContainerSize(); i++) {
                var s = be.inv.getItem(i);
                if (!s.isEmpty()) {
                    all.add(s.copy());
                }
            }
        }

        return all;
    }

    @Override
    public void animateTick(BlockState pState, Level pLevel, BlockPos pPos, RandomSource pRandom) {

        try {
            if (pLevel.isClientSide()) {
                var particle = ParticleTypes.WITCH;

                Circle2d c = new Circle2d(pPos, 1.5F);
                SimpleParticleType finalParticle = particle;
                c.doXTimes(5, x -> {
                    c.spawnParticle(pLevel, c.getRandomEdgePos(), finalParticle);
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void startNewMap(Player p, ItemStack stack, MapDeviceBE be) {

        try {
            DungeonItemMapData map = DungeonItemNbt.DUNGEON_MAP.loadFrom(stack);

            var count = map.getOrSetStartPos(p.level(), stack);
            var start = DungeonMain.MAIN_DUNGEON_STRUCTURE.getStartFromCounter(count.x(), count.z());
            var pos = TeleportUtils.getSpawnTeleportPos(DungeonMain.MAIN_DUNGEON_STRUCTURE, start.getMiddleBlockPosition(5));

            //var pdata = PlayerDataCapability.get(p);

            var data = new DungeonMapData();
            data.item = map;
            data.x = start.x();
            data.z = start.z();

            be.pos = pos;
            be.currentWorldUUID = DungeonMapCapability.getFromServer().data.data.uuid;

            be.setChanged();


            var libdata = new LibMapData();
            libdata.relicStats = RelicStatsContainer.calculate(be.getAllValidRelicStats());

            data.bonusContents.setupOnMapStart(stack, libdata, p);


            DungeonMapCapability.get(p.level()).data.data.setData(p, data, DungeonMain.MAIN_DUNGEON_STRUCTURE, start.getMiddleBlockPosition(5));
            LibMapCap.get(p.level()).data.setData(p, libdata, DungeonMain.MAIN_DUNGEON_STRUCTURE, start.getMiddleBlockPosition(5));

            // todo
            var event = new OnStartMapEvent(p, stack, start, DungeonMain.MAP);
            DungeonExileEvents.ON_START_NEW_MAP.callEvents(event);

            stack.shrink(1);

            if (joinCurrentMap(p, be)) {
                p.level().getServer().getLevel(ResourceKey.create(Registries.DIMENSION, DungeonMain.DIMENSION_KEY)).setBlock(pos.south(), DungeonEntries.MAP_DEVICE_BLOCK.get().defaultBlockState(), Block.UPDATE_ALL);
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean joinCurrentMap(Player p, MapDeviceBE be) {

        var event = new CanEnterMapEvent(p, be);
        DungeonExileEvents.CAN_ENTER_MAP.callEvents(event);
        if (!event.canEnter) {
            return false;
        }

        var pdata = PlayerDataCapability.get(p);
        pdata.mapTeleports.entranceTeleportLogic(p, DungeonMain.DIMENSION_KEY, be.pos);
        return true;
    }

    public static MapDeviceMenu inventory(int pContainerId, Inventory pPlayerInventory, Container pContainer) {
        return new MapDeviceMenu(pContainerId, pPlayerInventory, pContainer, 3);
    }

    
    public InteractionResult use(BlockState pState, Level world, BlockPos pPos, Player p, InteractionHand pHand, BlockHitResult pHit) {

        if (!world.isClientSide()) {
            var be = world.getBlockEntity(pPos);

            if (be instanceof MapDeviceBE obe) {

                if (MapDimensions.isMap(world)) {
                    if (p.isCrouching()) {
                        PlayerDataCapability.get(p).mapTeleports.teleportHome(p);
                    } else {
                        PlayerDataCapability.get(p).mapTeleports.exitTeleportLogic(p);
                    }
                    return InteractionResult.SUCCESS;
                }
                ItemStack stack = p.getMainHandItem();

                if (stack.is(DungeonEntries.RELIC_KEY.get())) {
                    p.openMenu(new SimpleMenuProvider((i, playerInventory, playerEntity) -> {
                        return inventory(i, playerInventory, obe.inv); // todo why doesnt vanilla have this
                    }, DungeonWords.RELIC_CONTAINER.get()));
                    return InteractionResult.SUCCESS;
                }

                if (DungeonItemNbt.DUNGEON_MAP.has(stack)) {

                    var event = new CanStartMapEvent(stack, p);

                    DungeonExileEvents.CAN_START_MAP.callEvents(event);

                    if (!event.canEnter) {
                        return InteractionResult.SUCCESS;
                    }

                    startNewMap(p, stack, obe);
                } else {
                    if (obe.isActivated()) {
                        joinCurrentMap(p, obe);
                    }
                }

            }
        }

        return InteractionResult.SUCCESS;
    }


    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new MapDeviceBE(pPos, pState);
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
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        return this.use(state, world, pos, player, InteractionHand.MAIN_HAND, hit);
    }

    @Override
    public InteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return this.use(state, world, pos, player, hand, hit);
    }

}
