package com.robertx22.library_of_exile.dimension;

import com.robertx22.library_of_exile.events.base.EventConsumer;
import net.minecraft.resources.Identifier;

public class MapChunkGens {
    public static void init() {}

    public static void registerMapChunkGenerator(Identifier id, EventConsumer<MapChunkGenEvent> e) {
        MapGenEvents.MAP_CHUNK_GEN.register(e);
    }
}
