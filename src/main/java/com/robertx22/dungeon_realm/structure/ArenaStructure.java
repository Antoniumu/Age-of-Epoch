package com.robertx22.dungeon_realm.structure;

import com.robertx22.dungeon_realm.database.DungeonDatabase;
import com.robertx22.dungeon_realm.database.boss_arena.BossArena;
import com.robertx22.dungeon_realm.main.DungeonMain;
import com.robertx22.library_of_exile.dimension.MapGenerationUTIL;
import com.robertx22.library_of_exile.dimension.structure.SimplePrebuiltMapData;
import com.robertx22.library_of_exile.dimension.structure.SimplePrebuiltMapStructure;
import com.robertx22.library_of_exile.utils.RandomUtils;
import net.minecraft.world.level.ChunkPos;

public class ArenaStructure extends SimplePrebuiltMapStructure {

    @Override
    public String guid() {
        return "boss_arena";
    }

    public BossArena getArena(ChunkPos cp) {
        var start = getStartChunkPos(cp);
        var random = MapGenerationUTIL.createRandom(start);
        var list = DungeonDatabase.BossArena().getList();
        var ob = RandomUtils.weightedRandom(list, random.nextDouble());
        return ob;
    }

    @Override
    public SimplePrebuiltMapData getMap(ChunkPos start) {
        return getArena(start).structure;
    }

    @Override
    public int getSpawnHeight() {
        return -60;
    }


    @Override
    protected ChunkPos INTERNALgetStartChunkPos(ChunkPos cp) {
        return DungeonMain.MAIN_DUNGEON_STRUCTURE.INTERNALgetStartChunkPos(cp);
    }

}
