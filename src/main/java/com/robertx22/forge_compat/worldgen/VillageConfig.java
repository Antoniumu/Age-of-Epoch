package com.robertx22.forge_compat.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

import java.util.function.Supplier;

public class VillageConfig implements FeatureConfiguration {

    public static final Codec<VillageConfig> CODEC =
        Codec.unit(new VillageConfig(null, 0));

    private final Supplier<StructureTemplatePool> startPool;
    private final int maxDepth;

    public VillageConfig(Supplier<StructureTemplatePool> startPool, int maxDepth) {
        this.startPool = startPool;
        this.maxDepth = maxDepth;
    }

    public Supplier<StructureTemplatePool> startPool() { return startPool; }

    public int maxDepth() { return maxDepth; }
}
