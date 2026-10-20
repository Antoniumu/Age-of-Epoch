package com.robertx22.forge_compat.fml.event.server;

import com.robertx22.forge_compat.eventbus.api.Event;
import net.minecraft.server.MinecraftServer;

public class FMLServerStoppedEvent extends Event {
    private final MinecraftServer server;

    public FMLServerStoppedEvent(MinecraftServer server) {
        this.server = server;
    }

    public MinecraftServer getServer() {
        return server;
    }
}
