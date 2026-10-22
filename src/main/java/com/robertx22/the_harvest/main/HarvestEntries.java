package com.robertx22.the_harvest.main;

import com.robertx22.library_of_exile.database.relic.relic_type.RelicItem;
import com.robertx22.the_harvest.block.HarvestBlock;
import com.robertx22.the_harvest.block.HarvestMobSpawnerBlock;
import com.robertx22.the_harvest.block_entity.HarvestBE;
import com.robertx22.the_harvest.block_entity.HarvestSpawnerBE;
import com.robertx22.the_harvest.item.HarvestMapItem;
import com.robertx22.the_harvest.item.MatItem;
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

public class HarvestEntries {
    // registars
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, HarvestMain.MODID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, HarvestMain.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, HarvestMain.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TAB = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, HarvestMain.MODID);

    // blocks
    public static RegistryObject<HarvestMobSpawnerBlock> SPAWNER_BLOCK = BLOCKS.register("harvest_spawner", () -> new HarvestMobSpawnerBlock());
    public static RegistryObject<HarvestBlock> HARVEST_BLOCK = BLOCKS.register("harvest", () -> new HarvestBlock());

    // block entities
    public static RegistryObject<BlockEntityType<HarvestSpawnerBE>> SPAWNER_BE = BLOCK_ENTITIES.register("harvest_spawner", () -> net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(HarvestSpawnerBE::new, SPAWNER_BLOCK.get()).build());
    public static RegistryObject<BlockEntityType<HarvestBE>> HARVEST_BE = BLOCK_ENTITIES.register("harvest", () -> net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(HarvestBE::new, HARVEST_BLOCK.get()).build());


    // items
    public static RegistryObject<BlockItem> HARVEST_ITEM = ITEMS.register("harvest", () -> new BlockItem(HARVEST_BLOCK.get(), new Item.Properties().stacksTo(64)));
    public static RegistryObject<HarvestMapItem> HARVEST_MAP_ITEM = ITEMS.register("harvest_map", () -> new HarvestMapItem());


    public static RegistryObject<MatItem> BLUE = ITEMS.register("lucid", () -> new MatItem("Lucid"));
    public static RegistryObject<MatItem> GREEN = ITEMS.register("chaotic", () -> new MatItem("Chaotic"));
    public static RegistryObject<MatItem> PURPLE = ITEMS.register("primal", () -> new MatItem("Primal"));

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
