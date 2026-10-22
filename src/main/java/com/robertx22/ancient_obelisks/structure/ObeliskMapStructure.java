package com.robertx22.ancient_obelisks.structure;

import com.robertx22.ancient_obelisks.database.Obelisk;
import com.robertx22.ancient_obelisks.database.ObeliskDatabase;
import com.robertx22.ancient_obelisks.main.ObelisksMain;
import com.robertx22.library_of_exile.dimension.MapGenerationUTIL;
import com.robertx22.library_of_exile.dimension.structure.SimplePrebuiltMapData;
import com.robertx22.library_of_exile.dimension.structure.SimplePrebuiltMapStructure;
import com.robertx22.library_of_exile.utils.RandomUtils;
import net.minecraft.world.level.ChunkPos;

public class ObeliskMapStructure extends SimplePrebuiltMapStructure {
    public static int DUNGEON_LENGTH = 10;

    @Override
    public String guid() {
        return ObelisksMain.DIMENSION_ID;
    }

    @Override
    public SimplePrebuiltMapData getMap(ChunkPos start) {
        return getObelisk(start).structure_data;
    }

    public Obelisk getObelisk(ChunkPos start) {
        var random = MapGenerationUTIL.createRandom(start);
        var list = ObeliskDatabase.getObelisks().getList();
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
