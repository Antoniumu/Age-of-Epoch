package com.robertx22.forge_compat.fml.server;

import net.minecraft.server.MinecraftServer;

public class ServerLifecycleHooks {

    private static MinecraftServer currentServer;

    public static MinecraftServer getCurrentServer() { return currentServer; }

    public static void setCurrentServer(MinecraftServer server) { currentServer = server; }
}
