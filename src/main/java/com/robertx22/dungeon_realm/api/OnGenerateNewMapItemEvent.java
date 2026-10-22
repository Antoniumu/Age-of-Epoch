package com.robertx22.dungeon_realm.api;

import com.robertx22.library_of_exile.events.base.ExileEvent;
import net.minecraft.world.item.ItemStack;

public class OnGenerateNewMapItemEvent extends ExileEvent {

    public ItemStack mapStack;

    public OnGenerateNewMapItemEvent(ItemStack mapStack) {
        this.mapStack = mapStack;
    }
}
