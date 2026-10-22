package com.robertx22.dungeon_realm.api;

import com.robertx22.library_of_exile.dimension.MapDimensionInfo;
import com.robertx22.library_of_exile.events.base.ExileEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;

public class OnStartMapEvent extends ExileEvent {
    public Player p;
    public ItemStack stack;
    public ChunkPos startChunkPos;
    public MapDimensionInfo mapInfo;

    public OnStartMapEvent(Player p, ItemStack stack, ChunkPos startChunkPos, MapDimensionInfo mapInfo) {
        this.p = p;
        this.stack = stack;
        this.startChunkPos = startChunkPos;
        this.mapInfo = mapInfo;
    }
}
