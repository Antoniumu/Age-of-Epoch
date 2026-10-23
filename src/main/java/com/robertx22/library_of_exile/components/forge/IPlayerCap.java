package com.robertx22.library_of_exile.components.forge;

import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.library_of_exile.packets.SyncPlayerCapToClient;
import net.minecraft.world.entity.player.Player;

public interface IPlayerCap extends ICommonCap {

    String getCapIdForSyncing();

    public default void syncToClient(Player player) {
        Packets.sendToClient(player, new SyncPlayerCapToClient(player, getCapIdForSyncing()));
    }
}
