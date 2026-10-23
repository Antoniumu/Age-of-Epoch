package com.robertx22.age_of_exile.dimension;

import com.mojang.serialization.Codec;
import com.robertx22.age_of_exile.uncommon.utilityclasses.ErrorUtils;
import com.robertx22.forge_compat.worldgen.VillageConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

public class DungeonDimensionJigsawFeature {

    public static int HEIGHT = 80;

    public static int DISTANCE = 30;

    public static ChunkPos getSpawnChunkOf(BlockPos p) {
        return getSpawnChunkOf(ChunkPos.containing(p));
    }

    public static ChunkPos getSpawnChunkOf(ChunkPos cp) {

        int x = getNearest(cp.x());
        int z = getNearest(cp.z());

        ChunkPos spawn = new ChunkPos(x, z);

        ErrorUtils.ifFalse(spawn.x() % DISTANCE == 0 && spawn.z() % DISTANCE == 0);

        return spawn;

    }

    static int getNearest(int num) {

        int o = Math.round(num / (float) DISTANCE);
        if (o == 0) {
            o++;
        }
        o *= DISTANCE;
        return o;
    }

    public DungeonDimensionJigsawFeature(Codec<VillageConfig> codec) {
    }
}
