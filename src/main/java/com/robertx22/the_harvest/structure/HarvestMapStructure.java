package com.robertx22.the_harvest.structure;

import com.robertx22.library_of_exile.dimension.MapGenerationUTIL;
import com.robertx22.library_of_exile.dimension.structure.SimplePrebuiltMapData;
import com.robertx22.library_of_exile.dimension.structure.SimplePrebuiltMapStructure;
import com.robertx22.library_of_exile.utils.RandomUtils;
import com.robertx22.the_harvest.database.HarvestArena;
import com.robertx22.the_harvest.database.HarvestDatabase;
import com.robertx22.the_harvest.main.HarvestMain;
import net.minecraft.world.level.ChunkPos;

public class HarvestMapStructure extends SimplePrebuiltMapStructure {
    public static int DUNGEON_LENGTH = 8;

    @Override
    public String guid() {
        return HarvestMain.DIMENSION_ID;
    }

    @Override
    public SimplePrebuiltMapData getMap(ChunkPos start) {
        return getArena(start).structure_data;
    }

    public HarvestArena getArena(ChunkPos start) {
        var random = MapGenerationUTIL.createRandom(start);
        var list = HarvestDatabase.getHarvestArenas().getList();
        var ob = RandomUtils.weightedRandom(list, random.nextDouble());
        return ob;
    }

    @Override
    public int getSpawnHeight() {
        return 50;
    }

    public ChunkPos getStartFromCounter(int x, int z) {
        var start = new ChunkPos(x * DUNGEON_LENGTH, z * DUNGEON_LENGTH);
        start = getStartChunkPos(start);
        return start;
    }

    @Override
    protected ChunkPos INTERNALgetStartChunkPos(ChunkPos cp) {
        var newcp = (new ChunkPos(cp.x / DUNGEON_LENGTH * DUNGEON_LENGTH, cp.z / DUNGEON_LENGTH * DUNGEON_LENGTH));
        return newcp;
    }

}
