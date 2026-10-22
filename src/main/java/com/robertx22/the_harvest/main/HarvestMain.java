package com.robertx22.the_harvest.main;

import com.google.common.collect.Lists;
import com.robertx22.library_of_exile.config.map_dimension.MapDimensionConfigDefaults;
import com.robertx22.library_of_exile.config.map_dimension.MapRegisterBuilder;
import com.robertx22.library_of_exile.database.init.LibDatabase;
import com.robertx22.library_of_exile.database.init.PredeterminedResult;
import com.robertx22.library_of_exile.database.mob_list.MobList;
import com.robertx22.library_of_exile.dimension.MapChunkGenEvent;
import com.robertx22.library_of_exile.dimension.MapContentType;
import com.robertx22.library_of_exile.dimension.MapDimensionInfo;
import com.robertx22.library_of_exile.dimension.MapDimensions;
import com.robertx22.library_of_exile.events.base.EventConsumer;
import com.robertx22.library_of_exile.events.base.ExileEvents;
import com.robertx22.library_of_exile.main.ApiForgeEvents;
import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.library_of_exile.registry.helpers.OrderedModConstructor;
import com.robertx22.library_of_exile.registry.register_info.ModRequiredRegisterInfo;
import com.robertx22.library_of_exile.registry.util.ExileRegistryUtil;
import com.robertx22.library_of_exile.unidentified.IdentifiableItems;
import com.robertx22.library_of_exile.utils.RandomUtils;
import com.robertx22.the_harvest.capability.HarvestEntityCap;
import com.robertx22.the_harvest.configs.HarvestConfig;
import com.robertx22.the_harvest.database.HarvestDatabase;
import com.robertx22.the_harvest.item.HarvestItemNbt;
import com.robertx22.the_harvest.item.HarvestMapItem;
import com.robertx22.the_harvest.structure.HarvestMapCap;
import com.robertx22.the_harvest.structure.HarvestMapData;
import com.robertx22.the_harvest.structure.HarvestMapStructure;
import com.robertx22.the_harvest.structure.HarvestWorldData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.data.CachedOutput;
import com.robertx22.forge_compat.data.LootTableProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import com.robertx22.forge_compat.api.distmarker.Dist;
import com.robertx22.forge_compat.common.MinecraftForge;
import com.robertx22.forge_compat.data.GatherDataEvent;
import com.robertx22.forge_compat.event.AttachCapabilitiesEvent;
import com.robertx22.forge_compat.event.entity.living.LivingDeathEvent;
import com.robertx22.forge_compat.event.entity.player.PlayerEvent;
import com.robertx22.forge_compat.eventbus.api.IEventBus;
import com.robertx22.forge_compat.fml.DistExecutor;
import com.robertx22.forge_compat.fml.ModLoadingContext;
import com.robertx22.forge_compat.fml.common.Mod;
import com.robertx22.forge_compat.fml.config.ModConfig;
import com.robertx22.forge_compat.fml.event.lifecycle.FMLClientSetupEvent;
import com.robertx22.forge_compat.fml.event.lifecycle.FMLCommonSetupEvent;
import com.robertx22.forge_compat.fml.javafmlmod.FMLJavaModLoadingContext;
import com.robertx22.forge_compat.registries.ForgeRegistries;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

@Mod("the_harvest")
public class HarvestMain {
    public static boolean RUN_DEV_TOOLS = false;

    public static String MODID = "the_harvest";
    public static String DIMENSION_ID = "the_harvest:harvest";
    public static Identifier DIMENSION_KEY = Identifier.parse(String.valueOf(DIMENSION_ID));

    public static ModRequiredRegisterInfo REGISTER_INFO = new ModRequiredRegisterInfo(MODID);

    public static Identifier id(String id) {
        return Identifier.fromNamespaceAndPath(MODID, id);
    }

    // other
    public static HarvestMapStructure HARVEST_MAP_STRUCTURE = new HarvestMapStructure();
    public static MapDimensionInfo MAP = new MapDimensionInfo(
            DIMENSION_KEY, HARVEST_MAP_STRUCTURE, MapContentType.SIDE_CONTENT, Arrays.asList(), new HarvestMobValidator(),
            new MapDimensionConfigDefaults(3, 2)) {
        @Override
        public void clearMapDataOnFolderWipe(MinecraftServer minecraftServer) {
            HarvestMapCap.get(minecraftServer.overworld()).data = new HarvestWorldData();

        }
    };


    public static void debugMsg(Player p, String s) {
        if (p.isCreative()) {
            p.sendSystemMessage(Component.literal("[Debug Info]:" + s));
        }
    }

    public HarvestMain() {
        final IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        OrderedModConstructor.register(new HarvestModConstructor(MODID), bus);

        DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> {
            bus.addListener(this::clientSetup);
        });


        new MapRegisterBuilder(MAP)
                .chunkGenerator(new EventConsumer<MapChunkGenEvent>() {
                    @Override
                    public void accept(MapChunkGenEvent event) {
                        if (event.mapId.equals("harvest")) {
                            HARVEST_MAP_STRUCTURE.generateInChunk(event.world, event.manager, event.chunk.getPos());
                        }
                    }
                }, id("harvest_chunk_gen"))
                .build();


        if (RUN_DEV_TOOLS) {
            ExileRegistryUtil.setCurrentRegistarMod(HarvestMain.MODID);

            ApiForgeEvents.registerForgeEvent(PlayerEvent.PlayerLoggedInEvent.class, event -> {
                HarvestDatabase.INSTANCE.runDataGen(CachedOutput.NO_CACHE);
            });
        }

        


        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, HarvestConfig.SPEC);


        bus.addListener(this::commonSetupEvent);


        HarvestEntries.CREATIVE_TAB.register(MODID, () -> new CreativeModeTab.Builder(CreativeModeTab.Row.TOP, 2)
                .icon(() -> HarvestEntries.HARVEST_ITEM.get().getDefaultInstance())
                .title(HarvestWords.CREATIVE_TAB.get().withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD))
                .displayItems(new CreativeModeTab.DisplayItemsGenerator() {
                    @Override
                    public void accept(CreativeModeTab.ItemDisplayParameters param, CreativeModeTab.Output output) {
                        for (Item item : ForgeRegistries.ITEMS) {
                            if (ForgeRegistries.ITEMS.getKey(item).getNamespace().equals(HarvestMain.MODID)) {
                                output.accept(item);
                            }
                        }
                    }
                })
                .build());


        HarvestCommands.init();

        ExileEvents.ON_CHEST_LOOTED.register(new EventConsumer<ExileEvents.OnChestLooted>() {
            @Override
            public void accept(ExileEvents.OnChestLooted e) {
                try {

                    float chance = (float) (HarvestConfig.get().MAP_SPAWN_CHANCE_ON_CHEST_LOOT.get() * HarvestConfig.get().getDimChanceMulti(e.player.level()));
                    if (RandomUtils.roll(chance)) {
                        if (!MapDimensions.isMap(e.player.level())) {
                            var empty = mygetEmptySlotsRandomized(e.inventory, new Random());
                            if (!empty.isEmpty()) {
                                int index = RandomUtils.randomFromList(empty);
                                var map = HarvestMapItem.blankMap(HarvestEntries.HARVEST_MAP_ITEM.get().getDefaultInstance(), false);
                                e.inventory.setItem(index, map);
                            }
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });


        IdentifiableItems.register(HarvestEntries.HARVEST_MAP_ITEM.getId(), new IdentifiableItems.Config() {
            @Override
            public boolean isUnidentified(ItemStack stack) {
                return !HarvestItemNbt.HARVEST_MAP.has(stack);
            }

            @Override
            public void identify(Player player, ItemStack stack) {
                HarvestMapItem.blankMap(stack, false);
            }
        });


        ApiForgeEvents.registerForgeEvent(LivingDeathEvent.class, event -> {
            if (event.getEntity().level().isClientSide()) {
                return;
            }
            if (MapDimensions.isMap(event.getEntity().level())) {
                if (HarvestEntityCap.get(event.getEntity()).data.isHarvestSpawn) {

                    float chance = HarvestConfig.get().LOOT_TABLE_CHANCE_PER_MOB.get().floatValue();
                    if (RandomUtils.roll(chance)) {
                        dropFromLootTable(event.getEntity(), HarvestLootTables.LOOT, event.getSource());
                    }
                }
            }
        });

        System.out.println("The Harvest loaded.");

    }

    //copied from livingentity
    // maybe put in lib mod
    protected void dropFromLootTable(LivingEntity en, Identifier table, DamageSource pDamageSource) {
        Identifier resourcelocation = table;
        LootTable loottable = en.level().getServer().reloadableRegistries().getLootTable(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, resourcelocation));
        LootParams.Builder lootparams$builder = (new LootParams.Builder((ServerLevel) en.level()))
                .withParameter(LootContextParams.THIS_ENTITY, en)
                .withParameter(LootContextParams.ORIGIN, en.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, pDamageSource).withOptionalParameter(LootContextParams.ATTACKING_ENTITY, pDamageSource.getEntity())
                .withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, pDamageSource.getDirectEntity());
        LootParams lootparams = lootparams$builder.create(LootContextParamSets.ENTITY);
        
    }

    private static List<Integer> mygetEmptySlotsRandomized(Container inventory, Random rand) {
        List<Integer> list = Lists.newArrayList();

        for (int i = 0; i < inventory.getContainerSize(); ++i) {
            if (inventory.getItem(i)
                    .isEmpty()) {
                list.add(i);
            }
        }

        Collections.shuffle(list, rand);
        return list;
    }

    public static Optional<HarvestMapData> ifMapData(Level level, BlockPos pos) {
        if (level.isClientSide()) {
            return Optional.empty();
        }
        var map = MapDimensions.getInfo(level);
        if (map != null && map.dimensionId.equals(DIMENSION_KEY)) {
            var mapdata = HarvestMapCap.get(level).data.data.getData(HARVEST_MAP_STRUCTURE, pos);
            if (mapdata != null) {
                return Optional.of(mapdata);
            }
        }
        return Optional.empty();
    }

    public void clientSetup(final FMLClientSetupEvent event) {
        HarvestClient.init();
    }

    public void commonSetupEvent(FMLCommonSetupEvent event) {


        ComponentInit.reg();

        MinecraftForge.EVENT_BUS.addGenericListener(Level.class, (Consumer<AttachCapabilitiesEvent<Level>>) x -> {
            x.addCapability(HarvestMapCap.RESOURCE, new HarvestMapCap(x.getObject()));
        });

        MinecraftForge.EVENT_BUS.addGenericListener(Entity.class, (Consumer<AttachCapabilitiesEvent<Entity>>) x -> {
            if (x.getObject() instanceof LivingEntity en) {
                x.addCapability(HarvestEntityCap.RESOURCE, new HarvestEntityCap(en));
            }
        });

    }

    public static PredeterminedResult<MobList> DUNGEON_MOB_SPAWNS = new PredeterminedResult<MobList>() {
        @Override
        public ExileRegistryType getRegistryType() {
            return LibDatabase.MOB_LIST;
        }

        @Override
        public MobList getPredeterminedRandomINTERNAL(Random random, Level level, ChunkPos pos) {
            var arena = HARVEST_MAP_STRUCTURE.getArena(pos);
            return LibDatabase.MobLists().getFilterWrapped(x -> arena.mob_list_tag_check.matches(x).can).random(random.nextDouble());
        }
    };
}
