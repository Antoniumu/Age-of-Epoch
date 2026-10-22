package com.robertx22.dungeon_realm.main;

import com.google.common.collect.Lists;
import com.robertx22.dungeon_realm.api.DungeonExileEvents;
import com.robertx22.dungeon_realm.capability.DungeonEntityCapability;
import com.robertx22.dungeon_realm.configs.DungeonConfig;
import com.robertx22.dungeon_realm.database.DungeonDatabase;
import com.robertx22.dungeon_realm.event.listeners.NeedPearlListener;
import com.robertx22.dungeon_realm.item.DungeonItemNbt;
import com.robertx22.dungeon_realm.item.DungeonMapGenSettings;
import com.robertx22.dungeon_realm.item.DungeonMapItem;
import com.robertx22.dungeon_realm.structure.*;
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
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.data.CachedOutput;
import com.robertx22.forge_compat.data.LootTableProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import com.robertx22.forge_compat.api.distmarker.Dist;
import com.robertx22.forge_compat.common.MinecraftForge;
import com.robertx22.forge_compat.data.GatherDataEvent;
import com.robertx22.forge_compat.event.AttachCapabilitiesEvent;
import com.robertx22.forge_compat.event.RegisterCommandsEvent;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

@Mod("dungeon_realm")
public class DungeonMain {
    public static boolean RUN_DEV_TOOLS = false;

    public static String MODID = "dungeon_realm";
    public static String DIMENSION_ID = "dungeon_realm:dungeon";
    public static final Logger LOG = LoggerFactory.getLogger(MODID);

    public static Identifier DIMENSION_KEY = Identifier.parse(String.valueOf(DIMENSION_ID));
    public static ModRequiredRegisterInfo REGISTER_INFO = new ModRequiredRegisterInfo(MODID);

    public static Identifier id(String id) {
        return Identifier.fromNamespaceAndPath(MODID, id);
    }

    // other
    public static DungeonMapStructure MAIN_DUNGEON_STRUCTURE = new DungeonMapStructure();

    public static ArenaStructure ARENA = new ArenaStructure();
    public static UberArenaStructure UBER_ARENA = new UberArenaStructure();
    public static RewardStructure REWARD_ROOM = new RewardStructure();

    public static MapDimensionInfo MAP = new MapDimensionInfo(
            DIMENSION_KEY,
            MAIN_DUNGEON_STRUCTURE,
            MapContentType.PRIMARY_CONTENT,
            Arrays.asList(ARENA, UBER_ARENA, REWARD_ROOM),
            new DungeonMobValidator(),
            new MapDimensionConfigDefaults(3, 1)
    ) {

        @Override
        public void clearMapDataOnFolderWipe(MinecraftServer minecraftServer) {
            
            DungeonMapCapability.get(minecraftServer.overworld()).data = new DungeonWorldData();
        }
    };


    public DungeonMain() {

        final IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        OrderedModConstructor.register(new DungeonModConstructor(MODID), bus);

        DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> {
            bus.addListener(this::clientSetup);
        });

        new MapRegisterBuilder(MAP)
                .chunkGenerator(new EventConsumer<MapChunkGenEvent>() {
                    @Override
                    public void accept(MapChunkGenEvent event) {
                        if (event.mapId.equals("dungeon")) {
                            MAIN_DUNGEON_STRUCTURE.generateInChunk(event.world, event.manager, event.chunk.getPos());
                            ARENA.generateInChunk(event.world, event.manager, event.chunk.getPos());
                            UBER_ARENA.generateInChunk(event.world, event.manager, event.chunk.getPos());
                            REWARD_ROOM.generateInChunk(event.world, event.manager, event.chunk.getPos());
                        }
                    }
                }, id("dungeon_chunk_gen"))
                .build();


        if (RUN_DEV_TOOLS) {
            ExileRegistryUtil.setCurrentRegistarMod(DungeonMain.MODID);

            ApiForgeEvents.registerForgeEvent(PlayerEvent.PlayerLoggedInEvent.class, event -> {
                DungeonDatabase.INSTANCE.runDataGen(CachedOutput.NO_CACHE);
            });
        }

        

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, DungeonConfig.SPEC);

        bus.addListener(this::commonSetupEvent);

        ApiForgeEvents.registerForgeEvent(RegisterCommandsEvent.class, event -> {
            DungeonCommands.init(event.getDispatcher());
        });

        DungeonEvents.init();
        DungeonExileEvents.init();

        ExileEvents.ON_CHEST_LOOTED.register(new EventConsumer<ExileEvents.OnChestLooted>() {
            @Override
            public void accept(ExileEvents.OnChestLooted e) {
                try {

                    if (!MapDimensions.isMap(e.player.level())) {
                        float chance = (float) (DungeonConfig.get().DUNGEON_MAP_SPAWN_CHANCE_ON_CHEST_LOOT.get() * DungeonConfig.get().getDimChanceMulti(e.player.level()));
                        if (RandomUtils.roll(chance)) {
                            var empty = mygetEmptySlotsRandomized(e.inventory, new Random());
                            if (!empty.isEmpty()) {
                                int index = RandomUtils.randomFromList(empty);
                                var map = DungeonMapItem.newRandomMapItemStack(new DungeonMapGenSettings());
                                e.inventory.setItem(index, map);
                            }
                        }
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        DungeonEntries.CREATIVE_TAB.register(MODID, () -> new CreativeModeTab.Builder(CreativeModeTab.Row.TOP, 2)
                .icon(() -> DungeonEntries.MAP_DEVICE_ITEM.get().getDefaultInstance())
                .title(DungeonWords.CREATIVE_TAB.get().withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD))
                .displayItems(new CreativeModeTab.DisplayItemsGenerator() {
                    @Override
                    public void accept(CreativeModeTab.ItemDisplayParameters param, CreativeModeTab.Output output) {
                        for (Item item : ForgeRegistries.ITEMS) {
                            if (ForgeRegistries.ITEMS.getKey(item).getNamespace().equals(DungeonMain.MODID)) {
                                output.accept(item);
                            }
                        }
                    }
                })
                .build());

        IdentifiableItems.register(DungeonEntries.DUNGEON_MAP_ITEM.getId(), new IdentifiableItems.Config() {
            @Override
            public boolean isUnidentified(ItemStack stack) {
                return !DungeonItemNbt.DUNGEON_MAP.has(stack);
            }

            @Override
            public void identify(Player player, ItemStack stack) {
                var newstack = DungeonMapItem.newRandomMapItemStack(new DungeonMapGenSettings());
                com.robertx22.forge_compat.common.NbtStackCompat.setTag(stack, com.robertx22.forge_compat.common.NbtStackCompat.getTag(newstack));
            }
        });

        DungeonExileEvents.CAN_ENTER_MAP.register(new NeedPearlListener());
        LOG.info("Dungeon Realm loaded.");
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


    public static Optional<DungeonMapData> ifMapData(Level level, BlockPos pos) {
        return DungeonMapCapability.DATA_GETTER.ifMapData(level, pos, true);
    }

    public static Optional<DungeonMapData> ifMapData(Level level, BlockPos pos, boolean grabConnectedData) {
        return DungeonMapCapability.DATA_GETTER.ifMapData(level, pos, grabConnectedData);
    }

    public void clientSetup(final FMLClientSetupEvent event) {
        DungeonClient.init();
    }

    public void commonSetupEvent(FMLCommonSetupEvent event) {


        ComponentInit.reg();

        MinecraftForge.EVENT_BUS.addGenericListener(Level.class, (Consumer<AttachCapabilitiesEvent<Level>>) x -> {
            x.addCapability(DungeonMapCapability.RESOURCE, new DungeonMapCapability(x.getObject()));
        });

        MinecraftForge.EVENT_BUS.addGenericListener(Entity.class, (Consumer<AttachCapabilitiesEvent<Entity>>) x -> {
            if (x.getObject() instanceof LivingEntity en) {
                x.addCapability(DungeonEntityCapability.RESOURCE, new DungeonEntityCapability(en));
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
            var dungeon = MAIN_DUNGEON_STRUCTURE.getMap(pos).dungeon;
            return LibDatabase.MobLists().getFilterWrapped(x -> dungeon.getDungeonData().mob_list_tag_check.matches(x).can).random(random.nextDouble());
        }
    };
}
