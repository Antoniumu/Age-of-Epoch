package com.robertx22.ancient_obelisks.main;

import com.robertx22.ancient_obelisks.block.ObeliskBlock;
import com.robertx22.ancient_obelisks.block.ObeliskMobSpawnerBlock;
import com.robertx22.ancient_obelisks.block.ObeliskRewardBlock;
import com.robertx22.ancient_obelisks.block_entity.ObeliskBE;
import com.robertx22.ancient_obelisks.block_entity.ObeliskMobSpawnerBE;
import com.robertx22.ancient_obelisks.block_entity.ObeliskRewardBE;
import com.robertx22.ancient_obelisks.item.EssenceItem;
import com.robertx22.ancient_obelisks.item.ObeliskMapItem;
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

public class ObeliskEntries {
    // registars
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ObelisksMain.MODID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ObelisksMain.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ObelisksMain.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TAB = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ObelisksMain.MODID);

    // blocks
    public static RegistryObject<ObeliskMobSpawnerBlock> SPAWNER_BLOCK = BLOCKS.register("obelisk_spawner", () -> new ObeliskMobSpawnerBlock());
    public static RegistryObject<ObeliskBlock> OBELISK_BLOCK = BLOCKS.register("obelisk", () -> new ObeliskBlock());
    public static RegistryObject<ObeliskRewardBlock> OBELISK_REWARD_BLOCK = BLOCKS.register("obelisk_reward", () -> new ObeliskRewardBlock());

    // block entities
    public static RegistryObject<BlockEntityType<ObeliskMobSpawnerBE>> SPAWNER_BE = BLOCK_ENTITIES.register("obelisk_spawner", () -> net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(ObeliskMobSpawnerBE::new, SPAWNER_BLOCK.get()).build());
    public static RegistryObject<BlockEntityType<ObeliskBE>> OBELISK_BE = BLOCK_ENTITIES.register("obelisk", () -> net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(ObeliskBE::new, OBELISK_BLOCK.get()).build());
    public static RegistryObject<BlockEntityType<ObeliskRewardBE>> OBELISK_REWARD_BE = BLOCK_ENTITIES.register("obelisk_reward", () -> net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(ObeliskRewardBE::new, OBELISK_REWARD_BLOCK.get()).build());


    // items
    public static RegistryObject<BlockItem> OBELISK_ITEM = ITEMS.register("obelisk", () -> new BlockItem(OBELISK_BLOCK.get(), new Item.Properties().stacksTo(64)));
    public static RegistryObject<ObeliskMapItem> OBELISK_MAP_ITEM = ITEMS.register("obelisk_map", () -> new ObeliskMapItem());
    // public static RegistryObject<HomeItem> HOME_TP_BACK = ITEMS.register("home", () -> new HomeItem());


    public static RegistryObject<EssenceItem> WRATH = ITEMS.register("ancient_wrath", () -> new EssenceItem("Wrath"));
    public static RegistryObject<EssenceItem> ENVY = ITEMS.register("ancient_envy", () -> new EssenceItem("Envy"));
    public static RegistryObject<EssenceItem> GREED = ITEMS.register("ancient_greed", () -> new EssenceItem("Greed"));

    public static RegistryObject<Item> RELIC = ITEMS.register("relic", () -> new RelicItem());


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
