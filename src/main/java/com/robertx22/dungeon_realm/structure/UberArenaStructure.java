package com.robertx22.dungeon_realm.structure;

import com.robertx22.dungeon_realm.database.DungeonDatabase;
import com.robertx22.dungeon_realm.database.uber_arena.UberBossArena;
import com.robertx22.dungeon_realm.main.DungeonMain;
import com.robertx22.library_of_exile.dimension.MapGenerationUTIL;
import com.robertx22.library_of_exile.dimension.structure.SimplePrebuiltMapData;
import com.robertx22.library_of_exile.dimension.structure.SimplePrebuiltMapStructure;
import com.robertx22.library_of_exile.utils.RandomUtils;
import net.minecraft.world.level.ChunkPos;

public class UberArenaStructure extends SimplePrebuiltMapStructure {

    @Override
    public String guid() {
        return "uber_boss_arena";
    }

    public UberBossArena getUber(ChunkPos cp) {
        var start = getStartChunkPos(cp);
        var random = MapGenerationUTIL.createRandom(start);
        var list = DungeonDatabase.UberBoss().getList();
        var ob = RandomUtils.weightedRandom(list, random.nextDouble());
        return ob;
    }

    @Override
    public SimplePrebuiltMapData getMap(ChunkPos start) {
        return getUber(start).structure_data;
    }

    @Override
    public int getSpawnHeight() {
        return 85 + 50;
    }


    @Override
    protected ChunkPos INTERNALgetStartChunkPos(ChunkPos cp) {
        return DungeonMain.MAIN_DUNGEON_STRUCTURE.INTERNALgetStartChunkPos(cp);
    }

}
