package com.robertx22.age_of_exile.dimension;

import net.minecraft.core.Registry;
import net.minecraft.world.level.biome.Biome;

public class VoidChunkGenerator {
    private final Registry<Biome> biomeRegistry;

    public VoidChunkGenerator(Registry<Biome> biomeRegistry) {
        this.biomeRegistry = biomeRegistry;
    }

    public Registry<Biome> getBiomeRegistry() {
        return this.biomeRegistry;
    }
}
