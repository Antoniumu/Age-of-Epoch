package com.robertx22.infinite_dungeons.main;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.server.level.ServerLevel;

public class StructurePlacer {

    public static boolean loadStructure(ServerLevel sw, BlockPos pos, StructureTemplate template) {
        StructurePlaceSettings pset = new StructurePlaceSettings();
        pset.setKnownShape(true);
        template.placeInWorldChunk(sw, pos, pset, sw.random);
        return true;
    }

}
