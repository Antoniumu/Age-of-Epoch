package com.robertx22.library_of_exile.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MapChunkGenerator extends ChunkGenerator {
    public static final MapCodec<MapChunkGenerator> MAP_CODEC = RecordCodecBuilder.mapCodec(b -> b.group(
        RegistryOps.retrieveElement(Biomes.THE_VOID),
        Codec.STRING.fieldOf("map_id").forGetter(x -> x.mapId)
    ).apply(b, b.stable(MapChunkGenerator::new)));

    public static final Codec<MapChunkGenerator> CODEC = MAP_CODEC.codec();

    public Holder.Reference<Biome> biome;
    public String mapId;

    public MapChunkGenerator(Holder.Reference<Biome> biome, String mapId) {
        super(new FixedBiomeSource(biome));
        this.biome = biome;
        this.mapId = mapId;
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return MAP_CODEC;
    }

    @Override
    public void applyCarvers(WorldGenRegion pLevel, long pSeed, RandomState pRandom, BiomeManager pBiomeManager, StructureManager pStructureManager, ChunkAccess pChunk) {
    }

    @Override
    public void buildSurface(WorldGenRegion pLevel, StructureManager pStructureManager, RandomState pRandom, ChunkAccess pChunk) {
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion pLevel) {
    }

    @Override
    public int getGenDepth() {
        return 384;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState pRandomState, StructureManager pStructureManager, ChunkAccess pChunk) {
        return CompletableFuture.completedFuture(pChunk);
    }

    @Override
    public int getSeaLevel() {
        return -63;
    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public int getBaseHeight(int pX, int pZ, Heightmap.Types pType, LevelHeightAccessor pLevel, RandomState pRandom) {
        return pLevel.getMinY();
    }

    @Override
    public NoiseColumn getBaseColumn(int pX, int pZ, LevelHeightAccessor pLevel, RandomState pRandom) {
        return new NoiseColumn(pLevel.getMinY(), new BlockState[]{Blocks.AIR.defaultBlockState()});
    }

    @Override
    public void addDebugScreenInfo(List<String> pInfo, RandomState pRandom, BlockPos pPos) {
    }

    public void createStructures(RegistryAccess pRegistryAccess, Object pStructureState, StructureManager pStructureManager, ChunkAccess pChunk, StructureTemplateManager pStructureTemplateManager) {
    }
}
