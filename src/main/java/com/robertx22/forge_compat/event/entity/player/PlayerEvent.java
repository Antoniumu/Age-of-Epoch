package com.robertx22.forge_compat.event.entity.player;

import com.robertx22.forge_compat.eventbus.api.Event;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class PlayerEvent extends Event {
    private final Player player;

    public PlayerEvent(Player player) {
        this.player = player;
    }

    public Player getPlayer() { return player; }

    public static class PlayerLoggedInEvent extends PlayerEvent {
        public PlayerLoggedInEvent(Player player) { super(player); }
    }

    public static class PlayerLoggedOutEvent extends PlayerEvent {
        public PlayerLoggedOutEvent(Player player) { super(player); }
    }

    public static class PlayerChangedDimensionEvent extends PlayerEvent {
        public PlayerChangedDimensionEvent(Player player) { super(player); }
    }

    public static class Clone extends PlayerEvent {
        private final Player original;
        private final boolean wasDeath;

        public Clone(Player player, Player original, boolean wasDeath) {
            super(player);
            this.original = original;
            this.wasDeath = wasDeath;
        }

        public Player getOriginal() { return original; }

        public boolean isWasDeath() { return wasDeath; }
    }

    public static class StartTracking extends PlayerEvent {
        private final Entity target;

        public StartTracking(Player player, Entity target) {
            super(player);
            this.target = target;
        }

        public Entity getTarget() { return target; }
    }

    public static class StopTracking extends PlayerEvent {
        private final Entity target;

        public StopTracking(Player player, Entity target) {
            super(player);
            this.target = target;
        }

        public Entity getTarget() { return target; }
    }
}
