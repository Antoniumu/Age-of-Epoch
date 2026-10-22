package com.robertx22.age_of_exile.mmorpg;

import com.robertx22.age_of_exile.gamemaster.GameMaster;
import com.robertx22.age_of_exile.gamemaster.GameMasterCommand;
import com.robertx22.age_of_exile.gamemaster.GameMasterEvents;
import com.robertx22.age_of_exile.guides.GuideLimit;
import com.robertx22.age_of_exile.guides.GuidesPacket;
import com.robertx22.age_of_exile.questbook.QuestBookEvents;
import com.robertx22.age_of_exile.questbook.QuestBookPacket;
import com.robertx22.age_of_exile.questbook.QuestBookService;
import com.robertx22.age_of_exile.simulation.SimCommand;
import com.robertx22.age_of_exile.simulation.SimulationPacket;
import com.robertx22.ancient_obelisks.main.ObelisksMain;
import com.robertx22.dungeon_realm.main.DungeonMain;
import com.robertx22.forge_compat.common.MinecraftForge;
import com.robertx22.forge_compat.common.capabilities.CapabilityProvider;
import com.robertx22.forge_compat.event.AddReloadListenerEvent;
import com.robertx22.forge_compat.event.OnDatapackSyncEvent;
import com.robertx22.forge_compat.event.RegisterCommandsEvent;
import com.robertx22.forge_compat.event.TickEvent;
import com.robertx22.forge_compat.event.entity.EntityJoinWorldEvent;
import com.robertx22.forge_compat.event.entity.player.AttackEntityEvent;
import com.robertx22.forge_compat.event.entity.player.PlayerEvent;
import com.robertx22.forge_compat.event.entity.player.PlayerInteractEvent;
import com.robertx22.forge_compat.event.level.BlockEvent;
import com.robertx22.forge_compat.eventbus.api.IEventBus;
import com.robertx22.forge_compat.fml.event.lifecycle.FMLCommonSetupEvent;
import com.robertx22.forge_compat.fml.event.lifecycle.InterModEnqueueEvent;
import com.robertx22.forge_compat.fml.event.lifecycle.InterModProcessEvent;
import com.robertx22.forge_compat.fml.event.server.FMLServerStartedEvent;
import com.robertx22.forge_compat.fml.event.server.FMLServerStartingEvent;
import com.robertx22.forge_compat.fml.event.server.FMLServerStoppedEvent;
import com.robertx22.forge_compat.fml.javafmlmod.FMLJavaModLoadingContext;
import com.robertx22.forge_compat.fml.server.ServerLifecycleHooks;
import com.robertx22.forge_compat.registries.PropertiesIdHelper;
import com.robertx22.library_of_exile.components.EntityInfoComponent;
import com.robertx22.library_of_exile.events.base.EventConsumer;
import com.robertx22.library_of_exile.events.base.ExileEvents;
import com.robertx22.library_of_exile.main.CommonInit;
import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.mine_and_slash.mmorpg.MMORPG;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.the_harvest.main.HarvestMain;
import com.robertx22.world_of_exile.world_gen.TowerBoss;
import com.robertx22.world_of_exile.world_gen.TowerCollapse;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class AgeOfEpochMod implements ModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("AgeOfEpoch");
    private static boolean bootstrapped = false;

    public static CommonInit LIBRARY_OF_EXILE;
    public static MMORPG MINE_AND_SLASH;
    public static DungeonMain DUNGEON_REALM;
    public static ObelisksMain ANCIENT_OBELISKS;
    public static HarvestMain THE_HARVEST;

    /**
     * Deterministic core initialization of all 6 mods + custom Age-of-Epoch systems.
     * Safe to invoke both at Fabric runtime AND from HeadlessDataGenRunner in OpenJDK 25!
     */
    public static synchronized void bootstrapCommon() {
        if (bootstrapped) {
            return;
        }
        bootstrapped = true;

        LOGGER.info("[Age of Epoch] Bootstrapping unified 6-mod Mine and Slash suite for MC 26.3 (Java 25)...");

        // 1. Construct all 6 mods in strict dependency order
        CommonInit.initDeferred();
        LIBRARY_OF_EXILE = new CommonInit();       // library_of_exile + orbs_of_crafting
        MINE_AND_SLASH = new MMORPG();             // mine_and_slash (mmorpg)
        DUNGEON_REALM = new DungeonMain();         // dungeon_realm
        ANCIENT_OBELISKS = new ObelisksMain();     // ancient_obelisks
        THE_HARVEST = new HarvestMain();           // the_harvest

        // 2. Register preserved custom Age-of-Epoch systems (Towers, Delves, GameMaster, Guides, QuestBook, Simulation)
        GameMasterEvents.register();
        QuestBookEvents.register();

        Packets.registerClientToServerPacket(MMORPG.NETWORK, new GuidesPacket(), 901);
        Packets.registerClientToServerPacket(MMORPG.NETWORK, new QuestBookPacket(), 902);
        Packets.registerClientToServerPacket(MMORPG.NETWORK, new SimulationPacket(), 903);

        // Hook GameMaster adaptive XP multiplier directly into 2025 ExileEvents.MOB_EXP_DROP
        ExileEvents.MOB_EXP_DROP.register(new EventConsumer<ExileEvents.OnMobExpDrop>() {
            @Override
            public void accept(ExileEvents.OnMobExpDrop event) {
                if (event.mobKilled != null && event.mobKilled.level() != null) {
                    var nearest = event.mobKilled.level().getNearestPlayer(event.mobKilled, 64.0D);
                    if (nearest instanceof ServerPlayer sp) {
                        event.exp *= (float) GameMaster.get().multiplierFor(sp);
                    }
                }
            }
        });

        // Hook Boss kills into QuestBook progression
        ExileEvents.MOB_DEATH.register(new EventConsumer<ExileEvents.OnMobDeath>() {
            @Override
            public void accept(ExileEvents.OnMobDeath event) {
                if (event.killer instanceof ServerPlayer sp && event.mob != null) {
                    var typeKey = BuiltInRegistries.ENTITY_TYPE.getKey(event.mob.getType());
                    QuestBookService.onUberBossKilled(sp, typeKey.toString());
                }
            }
        });

        // 3. Fire CommonSetup, InterModEnqueue & InterModProcess across mod bus
        com.robertx22.forge_compat.registries.DeferredRegister.flushAll();
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.post(new FMLCommonSetupEvent());
        com.robertx22.forge_compat.registries.DeferredRegister.flushAll();
        modBus.post(new InterModEnqueueEvent());
        modBus.post(new InterModProcessEvent());

        // 4. Seal late DataComponent initializers after registration completes
        PropertiesIdHelper.cleanupDataComponentInitializers();

        LOGGER.info("[Age of Epoch] Unified 6-mod bootstrap complete!");
    }

    @Override
    public void onInitialize() {
        bootstrapCommon();
        registerFabricBridges();
    }

    private static void registerFabricBridges() {
        // 1. Server Datapack Reload Bridge -> runs every ExileRegistryType BaseDataPackLoader synchronously
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return Identifier.fromNamespaceAndPath(SlashRef.MODID, "exile_datapack_loader");
            }

            @Override
            public void onResourceManagerReload(ResourceManager manager) {
                runAllExileReloadListeners(manager);
            }
        });

        // 2. Commands Bridge -> all 6 mods' commands + /gamemaster + /sim
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            MinecraftForge.EVENT_BUS.post(new RegisterCommandsEvent(dispatcher, registryAccess, environment));
            GameMasterCommand.register(dispatcher);
            SimCommand.register(dispatcher);
        });

        // 3. Server Lifecycle + World Capability Persistence Bridge
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            ServerLifecycleHooks.setCurrentServer(server);
            MinecraftForge.EVENT_BUS.post(new FMLServerStartingEvent(server));
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ServerLifecycleHooks.setCurrentServer(server);
            GuideLimit.loadFromDisk();
            loadWorldCapabilities(server);
            MinecraftForge.EVENT_BUS.post(new FMLServerStartedEvent(server));
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(thisServer ->
            saveWorldCapabilities(thisServer));

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            saveWorldCapabilities(server);
            MinecraftForge.EVENT_BUS.post(new FMLServerStoppedEvent(server));
            ServerLifecycleHooks.setCurrentServer(null);
        });

        // 4. Server, Level & Player Tick Bridges (+ TowerCollapse tick + periodic world cap autosave)
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            MinecraftForge.EVENT_BUS.post(new TickEvent.ServerTickEvent(TickEvent.Phase.START, server));
            for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
                MinecraftForge.EVENT_BUS.post(new TickEvent.PlayerTickEvent(TickEvent.Phase.START, sp));
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
                MinecraftForge.EVENT_BUS.post(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, sp));
            }
            MinecraftForge.EVENT_BUS.post(new TickEvent.ServerTickEvent(TickEvent.Phase.END, server));
            if (server.getTickCount() % 6000 == 0) {
                saveWorldCapabilities(server);
            }
        });

        ServerTickEvents.START_LEVEL_TICK.register(world ->
            MinecraftForge.EVENT_BUS.post(new TickEvent.WorldTickEvent(TickEvent.Phase.START, world)));

        ServerTickEvents.END_LEVEL_TICK.register(world -> {
            MinecraftForge.EVENT_BUS.post(new TickEvent.WorldTickEvent(TickEvent.Phase.END, world));
            TowerCollapse.tick(world);
        });

        // 5. Player Join / Disconnect / Clone & Entity Tracking Bridges
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer sp = handler.player;
            ExileEvents.ON_PLAYER_LOGIN.callEvents(new ExileEvents.OnPlayerLogin(sp));
            MinecraftForge.EVENT_BUS.post(new OnDatapackSyncEvent(server.getPlayerList(), sp));
            MinecraftForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(sp));
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
            MinecraftForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(handler.player)));

        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            CompoundTag caps = new CompoundTag();
            CapabilityProvider.saveAll(oldPlayer, caps);
            CapabilityProvider.loadAll(newPlayer, caps);
            MinecraftForge.EVENT_BUS.post(new PlayerEvent.Clone(newPlayer, oldPlayer, !alive));
        });

        EntityTrackingEvents.START_TRACKING.register((trackedEntity, player) ->
            MinecraftForge.EVENT_BUS.post(new PlayerEvent.StartTracking(player, trackedEntity)));

        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof LivingEntity living) {
                EntityInfoComponent.get(living).spawnInit(entity);
            }
            MinecraftForge.EVENT_BUS.post(new EntityJoinWorldEvent(entity, world));
        });

        // 6. Block & Entity Interaction Bridges (+ TowerBoss block-break protection)
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            AttackEntityEvent ev = new AttackEntityEvent(player, entity);
            MinecraftForge.EVENT_BUS.post(ev);
            return ev.isCanceled() ? InteractionResult.FAIL : InteractionResult.PASS;
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            PlayerInteractEvent.RightClickItem ev = new PlayerInteractEvent.RightClickItem(player, hand, world);
            MinecraftForge.EVENT_BUS.post(ev);
            return ev.isCanceled() ? InteractionResult.FAIL : InteractionResult.PASS;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            PlayerInteractEvent.RightClickBlock ev = new PlayerInteractEvent.RightClickBlock(player, hand, world, hitResult.getBlockPos());
            MinecraftForge.EVENT_BUS.post(ev);
            return ev.isCanceled() ? InteractionResult.FAIL : InteractionResult.PASS;
        });

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            PlayerInteractEvent.EntityInteract ev = new PlayerInteractEvent.EntityInteract(player, hand, entity);
            MinecraftForge.EVENT_BUS.post(ev);
            return ev.isCanceled() ? InteractionResult.FAIL : InteractionResult.PASS;
        });

        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (world instanceof ServerLevel sl && player != null && TowerBoss.denyBreak(sl, pos, player)) {
                return false;
            }
            BlockEvent.BreakEvent ev = new BlockEvent.BreakEvent(world, pos, state, player);
            MinecraftForge.EVENT_BUS.post(ev);
            return !ev.isCanceled();
        });
    }

    public static int runAllExileReloadListeners(ResourceManager manager) {
        AddReloadListenerEvent reloadEvent = new AddReloadListenerEvent();
        MinecraftForge.EVENT_BUS.post(reloadEvent);
        PreparableReloadListener.PreparationBarrier immediateBarrier = new PreparableReloadListener.PreparationBarrier() {
            @Override
            public <T> CompletableFuture<T> wait(T backgroundResult) {
                return CompletableFuture.completedFuture(backgroundResult);
            }
        };
        int count = 0;
        for (PreparableReloadListener listener : reloadEvent.listeners) {
            listener.reload(
                immediateBarrier,
                manager,
                Runnable::run,
                Runnable::run
            ).join();
            count++;
        }
        ExileEvents.AFTER_DATABASE_LOADED.callEvents(new ExileEvents.AfterDatabaseLoaded());
        return count;
    }

    private static Path worldCapPath(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("data").resolve("age_of_epoch_world_caps.nbt");
    }

    public static void saveWorldCapabilities(MinecraftServer server) {
        try {
            if (server == null || server.overworld() == null) return;
            CompoundTag tag = new CompoundTag();
            CapabilityProvider.saveAll(server.overworld(), tag);
            Path path = worldCapPath(server);
            Files.createDirectories(path.getParent());
            NbtIo.writeCompressed(tag, path);
        } catch (Exception e) {
            LOGGER.error("[Age of Epoch] Failed saving world capabilities", e);
        }
    }

    public static void loadWorldCapabilities(MinecraftServer server) {
        try {
            if (server == null || server.overworld() == null) return;
            Path path = worldCapPath(server);
            if (Files.exists(path)) {
                CompoundTag tag = NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap());
                CapabilityProvider.loadAll(server.overworld(), tag);
            }
        } catch (Exception e) {
            LOGGER.error("[Age of Epoch] Failed loading world capabilities", e);
        }
    }
}
