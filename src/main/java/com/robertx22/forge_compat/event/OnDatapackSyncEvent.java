package com.robertx22.forge_compat.event;

import com.robertx22.forge_compat.eventbus.api.Event;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

public class OnDatapackSyncEvent extends Event {
    private final PlayerList playerList;
    private final ServerPlayer player;

    public OnDatapackSyncEvent(PlayerList playerList, ServerPlayer player) {
        this.playerList = playerList;
        this.player = player;
    }

    public PlayerList getPlayerList() { return playerList; }
    public ServerPlayer getPlayer() { return player; }
}
