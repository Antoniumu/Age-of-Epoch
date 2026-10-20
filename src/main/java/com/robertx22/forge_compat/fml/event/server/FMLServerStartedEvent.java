package com.robertx22.forge_compat.fml.event.server;

import com.robertx22.forge_compat.eventbus.api.Event;
import net.minecraft.server.MinecraftServer;

public class FMLServerStartedEvent extends Event {
    private final MinecraftServer server;

    public FMLServerStartedEvent(MinecraftServer server) {
        this.server = server;
    }

    public MinecraftServer getServer() {
        return server;
    }
}
