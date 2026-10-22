package com.robertx22.library_of_exile.dimension;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

public class MapChunkGenerator {
    public Holder.Reference<Biome> biome;
    public String mapId;

    public MapChunkGenerator(Holder.Reference<Biome> biome, String mapId) {
        this.biome = biome;
        this.mapId = mapId;
    }
}
