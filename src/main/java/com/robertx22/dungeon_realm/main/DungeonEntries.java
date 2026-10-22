package com.robertx22.dungeon_realm.main;

import com.robertx22.dungeon_realm.block.CustomSpawnTpBlock;
import com.robertx22.dungeon_realm.block.MapDeviceBlock;
import com.robertx22.dungeon_realm.block.UberBossAltarBlock;
import com.robertx22.dungeon_realm.block_entity.MapDeviceBE;
import com.robertx22.dungeon_realm.item.DungeonMapItem;
import com.robertx22.dungeon_realm.item.TeleportBackItem;
import com.robertx22.library_of_exile.database.relic.relic_type.RelicItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import com.robertx22.forge_compat.eventbus.api.IEventBus;
import com.robertx22.forge_compat.fml.javafmlmod.FMLJavaModLoadingContext;
import com.robertx22.forge_compat.registries.DeferredRegister;
import com.robertx22.forge_compat.registries.ForgeRegistries;
import com.robertx22.forge_compat.fml.RegistryObject;

public class DungeonEntries {
    // registars
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, DungeonMain.MODID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, DungeonMain.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, DungeonMain.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TAB = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, DungeonMain.MODID);

    // blocks
    public static RegistryObject<MapDeviceBlock> MAP_DEVICE_BLOCK = BLOCKS.register("map_device", () -> new MapDeviceBlock());
    public static RegistryObject<CustomSpawnTpBlock> UBER_TELEPORT = BLOCKS.register("uber_teleport", () -> new CustomSpawnTpBlock(() -> DungeonMain.UBER_ARENA));
    public static RegistryObject<CustomSpawnTpBlock> BOSS_TELEPORT = BLOCKS.register("boss_teleport", () -> new CustomSpawnTpBlock(() -> DungeonMain.ARENA));
    public static RegistryObject<CustomSpawnTpBlock> REWARD_TELEPORT = BLOCKS.register("reward_teleport", () -> new CustomSpawnTpBlock(() -> DungeonMain.REWARD_ROOM));
    public static RegistryObject<UberBossAltarBlock> UBER_ALTAR = BLOCKS.register("uber_boss_altar", () -> new UberBossAltarBlock());

    // block entities
    public static RegistryObject<BlockEntityType<MapDeviceBE>> MAP_DEVICE_BE = BLOCK_ENTITIES.register("map_device", () -> net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(MapDeviceBE::new, MAP_DEVICE_BLOCK.get()).build());


    // items
    public static RegistryObject<BlockItem> MAP_DEVICE_ITEM = ITEMS.register("map_device", () -> new BlockItem(MAP_DEVICE_BLOCK.get(), new Item.Properties().stacksTo(64)));
    public static RegistryObject<DungeonMapItem> DUNGEON_MAP_ITEM = ITEMS.register("dungeon_map", () -> new DungeonMapItem());
    public static RegistryObject<Item> UBER_FRAGMENT = ITEMS.register("uber_fragment", () -> new Item(new Item.Properties().stacksTo(64)));
    public static RegistryObject<TeleportBackItem> HOME_TP_BACK = ITEMS.register("home_pearl", () -> new TeleportBackItem());
    public static RegistryObject<Item> RELIC_KEY = ITEMS.register("relic_key", () -> new Item(new Item.Properties().stacksTo(1)));
    public static RegistryObject<Item> RELIC_ITEM = ITEMS.register("general_relic", () -> new RelicItem());


    public static void initDeferred() {
        final IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(bus);
        CREATIVE_TAB.register(bus);
        BLOCKS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }

    public static void init() {

    }
}
