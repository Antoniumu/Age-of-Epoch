package com.robertx22.world_of_exile.main.structures.base;

import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.world_of_exile.config.FeatureConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

import java.util.function.Predicate;

/**
 * Compile-time stub of World of Exile's StructureWrapper. The dungeon-jigsaw
 * worldgen built on this needs a semantic pass for 26.3 (see PORTING.md).
 */
public abstract class StructureWrapper {

    public StructureTemplatePool startPool;
    public final ResourceLocation id;
    public final Predicate<Biome> biomeCheck;
    public final boolean flag;
    public final FeatureConfig config;
    public final GenerationStep.Decoration step;

    public StructureWrapper(Predicate<Biome> biomeCheck, ResourceLocation id, boolean flag,
                            FeatureConfig config, GenerationStep.Decoration step) {
        this.biomeCheck = biomeCheck;
        this.id = id;
        this.flag = flag;
        this.config = config;
        this.step = step;
    }

    public abstract Structure createConfiguredFeature();

    public abstract Structure createFeature();

    public abstract StructureTemplatePool createPoolAndInitPools();
}
