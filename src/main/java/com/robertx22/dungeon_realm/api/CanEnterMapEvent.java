package com.robertx22.dungeon_realm.api;

import com.robertx22.dungeon_realm.block_entity.MapDeviceBE;
import com.robertx22.library_of_exile.events.base.ExileEvent;
import net.minecraft.world.entity.player.Player;

public class CanEnterMapEvent extends ExileEvent {

    public Player p;
    public boolean canEnter = true;
    public MapDeviceBE mapDevice;

    public CanEnterMapEvent(Player p, MapDeviceBE mapDevice) {
        this.p = p;
        this.mapDevice = mapDevice;
    }
}
