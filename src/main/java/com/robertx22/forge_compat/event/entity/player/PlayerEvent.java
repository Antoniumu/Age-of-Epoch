package com.robertx22.forge_compat.event.entity.player;

import com.robertx22.forge_compat.eventbus.api.Event;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class PlayerEvent extends Event {
    private final Player player;
    public PlayerEvent(Player player) { this.player = player; }
    public Player getEntity() { return player; }

    public static class PlayerLoggedInEvent extends PlayerEvent {
        public PlayerLoggedInEvent(Player player) { super(player); }
    }
    public static class PlayerLoggedOutEvent extends PlayerEvent {
        public PlayerLoggedOutEvent(Player player) { super(player); }
    }
    public static class PlayerRespawnEvent extends PlayerEvent {
        private final boolean endConquered;
        public PlayerRespawnEvent(Player player, boolean endConquered) { super(player); this.endConquered = endConquered; }
        public boolean isEndConquered() { return endConquered; }
    }
    public static class StartTracking extends PlayerEvent {
        private final Entity target;
        public StartTracking(Player player, Entity target) { super(player); this.target = target; }
        public Entity getTarget() { return target; }
    }
    public static class ItemCraftedEvent extends PlayerEvent {
        private final ItemStack crafting;
        private final Container inventory;
        public ItemCraftedEvent(Player player, ItemStack crafting, Container inventory) {
            super(player);
            this.crafting = crafting;
            this.inventory = inventory;
        }
        public ItemStack getCrafting() { return crafting; }
        public Container getInventory() { return inventory; }
    }
    public static class Clone extends PlayerEvent {
        private final Player original;
        private final boolean wasDeath;
        public Clone(Player original, Player newPlayer, boolean wasDeath) {
            super(newPlayer);
            this.original = original;
            this.wasDeath = wasDeath;
        }
        public Player getOriginal() { return original; }
        public boolean isWasDeath() { return wasDeath; }
    }
}
