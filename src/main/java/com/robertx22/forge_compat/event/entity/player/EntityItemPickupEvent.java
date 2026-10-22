package com.robertx22.forge_compat.event.entity.player;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;

public class EntityItemPickupEvent extends PlayerEvent {
    private final ItemEntity item;
    public EntityItemPickupEvent(Player player, ItemEntity item) {
        super(player);
        this.item = item;
    }
    public ItemEntity getItem() { return item; }
}
