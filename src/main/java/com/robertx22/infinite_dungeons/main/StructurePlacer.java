package com.robertx22.infinite_dungeons.main;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class StructurePlacer {

    public static boolean loadStructure(ServerLevel sw, BlockPos pos, StructureTemplate template) {
        StructurePlaceSettings pset = new StructurePlaceSettings();
        pset.setKnownShape(true);
        template.placeInWorld(sw, pos, pos, pset, sw.getRandom(), 2);
        return true;
    }

}
