package com.robertx22.age_of_exile.tpa;

import com.robertx22.forge_compat.event.TickEvent;
import com.robertx22.forge_compat.event.entity.player.PlayerEvent;
import com.robertx22.forge_compat.fml.event.server.FMLServerStartedEvent;
import com.robertx22.forge_compat.fml.event.server.FMLServerStoppedEvent;
import com.robertx22.library_of_exile.main.ForgeEvents;
import net.minecraft.server.level.ServerPlayer;

/** Lifecycle hooks for transient TPA requests. */
public final class TpaAddon {

    private static boolean initialized = false;

    private TpaAddon() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        ForgeEvents.registerForgeEvent(TickEvent.ServerTickEvent.class, event -> {
            if (event.phase == TickEvent.Phase.END) {
                TpaManager.onServerTick(event.server);
            }
        });

        ForgeEvents.registerForgeEvent(PlayerEvent.PlayerLoggedOutEvent.class, event -> {
            if (event.getPlayer() instanceof ServerPlayer player) {
                TpaManager.onLogout(player);
            }
        });

        ForgeEvents.registerForgeEvent(FMLServerStartedEvent.class, event -> TpaManager.onServerStarted());
        ForgeEvents.registerForgeEvent(FMLServerStoppedEvent.class, event -> TpaManager.onServerStopped());
    }
}
