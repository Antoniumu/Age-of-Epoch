package com.robertx22.library_of_exile.packets;

import com.robertx22.library_of_exile.utils.LibClientOnly;
import net.minecraft.world.entity.player.Player;
import com.robertx22.forge_compat.fml.network.NetworkEvent;

public class ExilePacketContext {

    NetworkEvent.Context forgeCTX;

    public ExilePacketContext(NetworkEvent.Context forgeCTX) {
        this.forgeCTX = forgeCTX;
    }

    public Player getPlayer() {

        Player p = forgeCTX.getSender();

        if (p == null) {
            return LibClientOnly.getPlayer();
        }
        return p;
    }
}