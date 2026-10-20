package com.robertx22.age_of_exile.mmorpg;

import com.robertx22.forge_compat.common.MinecraftForge;
import com.robertx22.forge_compat.event.AddReloadListenerEvent;
import com.robertx22.forge_compat.event.TickEvent;
import com.robertx22.forge_compat.event.entity.EntityJoinWorldEvent;
import com.robertx22.forge_compat.event.entity.living.LivingDeathEvent;
import com.robertx22.forge_compat.event.entity.player.AttackEntityEvent;
import com.robertx22.forge_compat.event.entity.player.PlayerEvent;
import com.robertx22.forge_compat.event.entity.player.PlayerInteractEvent;
import com.robertx22.forge_compat.fml.event.lifecycle.FMLCommonSetupEvent;
import com.robertx22.forge_compat.fml.event.lifecycle.FMLClientSetupEvent;
import com.robertx22.forge_compat.fml.event.lifecycle.InterModEnqueueEvent;
import com.robertx22.forge_compat.fml.event.lifecycle.InterModProcessEvent;
import com.robertx22.forge_compat.fml.event.server.FMLServerStartedEvent;
import com.robertx22.forge_compat.fml.event.server.FMLServerStartingEvent;
import com.robertx22.forge_compat.fml.event.server.FMLServerStoppedEvent;
import com.robertx22.forge_compat.fml.javafmlmod.FMLJavaModLoadingContext;
import com.robertx22.forge_compat.fml.network.simple.SimpleChannel;
import com.robertx22.forge_compat.fml.server.ServerLifecycleHooks;
import com.robertx22.forge_compat.eventbus.api.IEventBus;
import com.robertx22.library_of_exile.main.CommonInit;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Fabric integration layer: runs the merged mod initialization, fires the
 * Forge-shaped lifecycle/game events the ported code listens to, and keeps the
 * SimpleChannel connection map up to date.
 */
public class FabricBootstrap {

    private static boolean initialized = false;

    /** Called by MMORPG.onInitialize() after the mod constructor has run. */
    public static void finishInit() {
        if (initialized) return;
        initialized = true;

        // companion mods merged into this jar
        new CommonInit();
        try {
            new com.robertx22.divine_missions.main.CommonInit();
        } catch (Throwable t) {
            t.printStackTrace();
        }
        try {
            new com.robertx22.infinite_dungeons.main.CommonInit();
        } catch (Throwable t) {
            t.printStackTrace();
        }

        try {
            com.robertx22.age_of_exile.database.base.CreativeTabs.registerAll();
        } catch (Throwable t) {
            t.printStackTrace();
        }

        // lifecycle events on the mod event bus
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.post(new FMLCommonSetupEvent());
        bus.post(new InterModEnqueueEvent());
        bus.post(new InterModProcessEvent());

        com.robertx22.forge_compat.registries.PropertiesIdHelper.cleanupDataComponentInitializers();

        registerFabricHooks();
    }

    public static void fireClientSetup() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.post(new FMLClientSetupEvent());
    }

    private static void registerFabricHooks() {
        // ---- server lifecycle ----
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            ServerLifecycleHooks.setCurrentServer(server);
            MinecraftForge.EVENT_BUS.post(new FMLServerStartingEvent(server));
        });
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ServerLifecycleHooks.setCurrentServer(server);
            MinecraftForge.EVENT_BUS.post(new FMLServerStartedEvent(server));
            CommonInit.onDatapacksReloaded();
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            MinecraftForge.EVENT_BUS.post(new FMLServerStoppedEvent(server));
            ServerLifecycleHooks.setCurrentServer(null);
        });

        // ---- datapack reload -> AddReloadListenerEvent ----
        ResourceManagerHelper.get(PackType.SERVER_DATA)
            .registerReloadListener(new SimpleSynchronousResourceReloadListener() {
                @Override
                public Identifier getFabricId() {
                    return Identifier.fromNamespaceAndPath(SlashRef.MODID, "reload_bridge");
                }

                @Override
                public void onResourceManagerReload(ResourceManager resourceManager) {
                    try {
                        com.robertx22.library_of_exile.registry.ExileRegistryType.getAllInRegisterOrder()
                            .forEach(type -> {
                                if (type.getLoader() != null) {
                                    type.getLoader().reloadSync(resourceManager);
                                }
                            });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    MinecraftForge.EVENT_BUS.post(new AddReloadListenerEvent());
                }
            });

        // ---- player connection / lifecycle ----
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            try {
                com.robertx22.library_of_exile.events.base.ExileEvents.ON_PLAYER_LOGIN.callEvents(
                    new com.robertx22.library_of_exile.events.base.ExileEvents.OnPlayerLogin(player)
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
            MinecraftForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(player));
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer player = handler.player;
            MinecraftForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(player));
        });
        net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            MinecraftForge.EVENT_BUS.post(new PlayerEvent.Clone(newPlayer, oldPlayer, !alive));
        });

        // ---- entity join ----
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof LivingEntity living) {
                try {
                    com.robertx22.library_of_exile.components.EntityInfoComponent.get(living)
                        .spawnInit(entity);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            MinecraftForge.EVENT_BUS.post(new EntityJoinWorldEvent(entity, world));
        });

        // ---- ticks ----
        ServerTickEvents.START_SERVER_TICK.register(server ->
            MinecraftForge.EVENT_BUS.post(new TickEvent.ServerTickEvent(TickEvent.Phase.START, server)));
        ServerTickEvents.END_SERVER_TICK.register(server ->
            MinecraftForge.EVENT_BUS.post(new TickEvent.ServerTickEvent(TickEvent.Phase.END, server)));
        ServerTickEvents.START_LEVEL_TICK.register(world ->
            MinecraftForge.EVENT_BUS.post(new TickEvent.WorldTickEvent(TickEvent.Phase.START, world)));
        ServerTickEvents.END_LEVEL_TICK.register(world ->
            MinecraftForge.EVENT_BUS.post(new TickEvent.WorldTickEvent(TickEvent.Phase.END, world)));

        // ---- interactions ----
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            AttackEntityEvent event = new AttackEntityEvent(player, entity);
            MinecraftForge.EVENT_BUS.post(event);
            return event.isCanceled() ? InteractionResult.FAIL : InteractionResult.PASS;
        });
        UseItemCallback.EVENT.register((player, world, hand) -> {
            PlayerInteractEvent.RightClickItem event = new PlayerInteractEvent.RightClickItem(player, hand, world);
            MinecraftForge.EVENT_BUS.post(event);
            return event.isCanceled() ? InteractionResult.FAIL : InteractionResult.PASS;
        });
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            PlayerInteractEvent.RightClickBlock event =
                new PlayerInteractEvent.RightClickBlock(player, hand, world, hitResult.getBlockPos());
            MinecraftForge.EVENT_BUS.post(event);
            return event.isCanceled() ? InteractionResult.FAIL : InteractionResult.PASS;
        });
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            PlayerInteractEvent.EntityInteract event =
                new PlayerInteractEvent.EntityInteract(player, hand, entity);
            MinecraftForge.EVENT_BUS.post(event);
            return event.isCanceled() ? InteractionResult.FAIL : InteractionResult.PASS;
        });
    }
}
