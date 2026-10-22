package com.robertx22.dungeon_realm.api;

import com.robertx22.library_of_exile.events.base.ExileEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class CanStartMapEvent extends ExileEvent {

    public ItemStack stack;
    public Player p;
    public boolean canEnter = true;

    public CanStartMapEvent(ItemStack stack, Player p) {
        this.stack = stack;
        this.p = p;
    }
}
