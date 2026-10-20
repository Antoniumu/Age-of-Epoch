package com.robertx22.age_of_exile.dimension;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryLookupCodec;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.WorldGenRegion;
import net.minecraft.world.level.levelgen.structure.StructureManager;
import com.robertx22.forge_compat.worldgen.DimensionStructuresSettings;
import com.robertx22.forge_compat.api.distmarker.Dist;
import com.robertx22.forge_compat.api.distmarker.OnlyIn;

import net.minecraft.core.registries.Registries;
public class VoidChunkGenerator extends ChunkGenerator {
    private final Registry<Biome> biomeRegistry;

    public static Codec<VoidChunkGenerator> VOID_CODED =
        RegistryLookupCodec.create(Registries.BIOME)
            .xmap(VoidChunkGenerator::new, VoidChunkGenerator::getBiomeRegistry)
            .stable()
            .codec();

    public VoidChunkGenerator(Registry<Biome> biomeRegistry) {
        super(new FixedBiomeSource((Biome) biomeRegistry.getOrThrow(Biomes.THE_VOID)), new DimensionStructuresSettings(false));
        this.getSettings()
            .structureConfig()
            .clear();
        this.biomeRegistry = biomeRegistry;
    }

    public Registry<Biome> getBiomeRegistry() {
        return this.biomeRegistry;
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return VOID_CODED;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public ChunkGenerator withSeed(long seed) {
        return this;
    }

    @Override
    public void buildSurfaceAndBedrock(WorldGenRegion region, ChunkAccess chunk) {
    }

    @Override
    public void applyCarvers(long seed, BiomeManager access, ChunkAccess chunk, GenerationStep.Carving carver) {
    }

    @Override
    public void fillFromNoise(LevelAccessor world, StructureManager accessor, ChunkAccess chunk) {

        BlockState blockState = Blocks.STONE.defaultBlockState();
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        if (blockState != null) {
            for (int i = 0; i < 220; ++i) {
                for (int j = 0; j < 16; ++j) {
                    for (int k = 0; k < 16; ++k) {
                        chunk.setBlockState(mutable.set(j, i, k), blockState, false);
                    }
                }
            }
        }
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Type heightmapType) {
        return 0;
    }

    @Override
    public BlockGetter getBaseColumn(int x, int z) {
        return new BlockGetter(new BlockState[0]);
    }

}
