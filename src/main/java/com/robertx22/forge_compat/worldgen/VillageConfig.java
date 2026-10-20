package com.robertx22.forge_compat.worldgen;

import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

/**
 * Stand-in for 1.16's VillageConfig (jigsaw configuration). Jigsaw structures in
 * 26.3 use PoolElementStructurePool-based codecs; fields are kept for source
 * compatibility and need a semantic pass for worldgen.
 */
public class VillageConfig {
    /** Source-compatibility placeholder; jigsaw codecs need a semantic pass for 26.3. */
    public static final com.mojang.serialization.Codec<VillageConfig> CODEC =
        com.mojang.serialization.Codec.unit(() -> new VillageConfig(null, 0));

    public StructureTemplatePool startPool;
    public int maxSize;

    public VillageConfig(StructureTemplatePool startPool, int maxSize) {
        this.startPool = startPool;
        this.maxSize = maxSize;
    }
}
