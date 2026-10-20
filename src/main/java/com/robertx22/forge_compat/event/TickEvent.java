package com.robertx22.forge_compat.event;

import com.robertx22.forge_compat.eventbus.api.Event;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

public class TickEvent extends Event {

    public enum Phase { START, END }

    public final Phase phase;

    public TickEvent(Phase phase) {
        this.phase = phase;
    }

    public static class ClientTickEvent extends TickEvent {
        public ClientTickEvent(Phase phase) {
            super(phase);
        }
    }

    public static class ServerTickEvent extends TickEvent {
        public final MinecraftServer server;

        public ServerTickEvent(Phase phase, MinecraftServer server) {
            super(phase);
            this.server = server;
        }
    }

    public static class WorldTickEvent extends TickEvent {
        public final Level world;

        public WorldTickEvent(Phase phase, Level world) {
            super(phase);
            this.world = world;
        }
    }

    public static class PlayerTickEvent extends TickEvent {
        public final net.minecraft.world.entity.player.Player player;

        public PlayerTickEvent(Phase phase, net.minecraft.world.entity.player.Player player) {
            super(phase);
            this.player = player;
        }
    }
}
