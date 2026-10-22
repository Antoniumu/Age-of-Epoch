package com.robertx22.forge_compat.event.server;

import com.robertx22.forge_compat.eventbus.api.Event;
import net.minecraft.server.MinecraftServer;

public class ServerStoppedEvent extends Event {
    private final MinecraftServer server;
    public ServerStoppedEvent(MinecraftServer server) { this.server = server; }
    public MinecraftServer getServer() { return server; }
}
