package com.robertx22.age_of_exile.dimension;

import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.world_of_exile.config.FeatureConfig;
import com.robertx22.world_of_exile.main.structures.base.StructureWrapper;
import com.robertx22.world_of_exile.world_gen.AbstractPool;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.Structure;
import com.robertx22.forge_compat.worldgen.VillageConfig;
import net.minecraft.world.level.levelgen.structure.templatesystem.ProcessorLists;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import com.robertx22.forge_compat.worldgen.StructureSeparationSettings;

public class DungeonDimensionJigsaw extends StructureWrapper {

    public static void initStatics() {

    }

    public static FeatureConfig config;

    static class MyHackyFastFix extends FeatureConfig.MyStructureConfig {
        @Override
        public StructureSeparationSettings get() {
            return new StructureSeparationSettings(1, 0, 0);
        }
    }

    static {
        config = new FeatureConfig(new MyHackyFastFix());
    }

    public DungeonDimensionJigsaw() {
        super(x -> x.getCategory()
                .equals(Biome.Category.THEEND)
            , DimensionIds.DUNGEON_STRUCTURE, false, config, GenerationStep.Decoration.SURFACE_STRUCTURES);
    }

    @Override
    public Structure createConfiguredFeature() {
        return feature.get()
            .configured(new VillageConfig(() -> {
                return this.startPool;
            }, 3));
    }

    @Override
    public Structure<VillageConfig> createFeature() {
        return new DungeonDimensionJigsawFeature(VillageConfig.CODEC);
    }

    @Override
    public StructureTemplatePool createPoolAndInitPools() {
        AbstractPool startBuilder = new Pool(SlashRef.id("dungeon/starts"));
        startBuilder.add(SlashRef.id("mossy/mossy_start"));
        startBuilder.add(SlashRef.id("desert/desert_start"));

        AbstractPool stone = new Pool(SlashRef.id("stone"));
        stone.add(SlashRef.id("stone/1"));
        stone.add(SlashRef.id("stone/2"));
        stone.add(SlashRef.id("stone/3"));
        stone.add(SlashRef.id("stone/4"));
        stone.add(SlashRef.id("stone/5"));

        stone.build();

        return startBuilder.build();
    }

    static class Pool extends AbstractPool {

        public Pool(ResourceLocation poolId) {
            super(poolId);
        }

        @Override
        public StructureProcessorList processorList() {
            return ProcessorLists.EMPTY;
        }
    }

}
