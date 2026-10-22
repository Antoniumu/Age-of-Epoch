package com.robertx22.dungeon_realm.structure;

import com.robertx22.dungeon_realm.configs.DungeonConfig;
import com.robertx22.dungeon_realm.database.DungeonDatabase;
import com.robertx22.library_of_exile.dimension.MapGenerationUTIL;
import com.robertx22.library_of_exile.dimension.structure.dungeon.DungeonBuilder;
import com.robertx22.library_of_exile.dimension.structure.dungeon.DungeonStructure;
import net.minecraft.world.level.ChunkPos;

import java.util.stream.Collectors;

public class DungeonMapStructure extends DungeonStructure {


    @Override
    public String guid() {
        return "dungeon";
    }

    @Override
    public DungeonBuilder getMap(ChunkPos cp) {
        var start = getStartChunkPos(cp);
        DungeonBuilder b = new DungeonBuilder(dungeonSettings(start));
        return b;
    }

    public static DungeonBuilder.Settings dungeonSettings(ChunkPos pos) {
        var settings = new DungeonBuilder.Settings(
                MapGenerationUTIL.createRandom(pos),
                DungeonConfig.get().MIN_MAP_ROOMS.get(),
                DungeonConfig.get().MAX_MAP_ROOMS.get(),
                DungeonDatabase.Dungeons().getFilterWrapped(x -> true).list.stream().collect(Collectors.toList()));


        // todo
        // settings.possibleDungeons = Arrays.asList(DungeonDungeons.INSTANCE.NIGHT_TERROR.get());

        return settings;
    }

    public ChunkPos getStartFromCounter(int x, int z) {
        var start = new ChunkPos(x * DUNGEON_LENGTH, z * DUNGEON_LENGTH);
        start = getStartChunkPos(start);
        return start;
    }

    @Override
    public int getSpawnHeight() {
        return 50;
    }

    public static int DUNGEON_LENGTH = 30;

    @Override
    protected ChunkPos INTERNALgetStartChunkPos(ChunkPos cp) {
        int chunkX = cp.x;
        int chunkZ = cp.z;
        int distToEntranceX = 11 - (chunkX % DUNGEON_LENGTH);
        int distToEntranceZ = 11 - (chunkZ % DUNGEON_LENGTH);
        chunkX += distToEntranceX;
        chunkZ += distToEntranceZ;
        return new ChunkPos(chunkX, chunkZ);
    }
}
