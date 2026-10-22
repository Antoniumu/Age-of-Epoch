package com.robertx22.dungeon_realm.database.data_blocks;

import com.robertx22.dungeon_realm.main.DungeonMain;
import com.robertx22.dungeon_realm.structure.DungeonMapCapability;
import com.robertx22.library_of_exile.database.map_data_block.MapBlockCtx;
import com.robertx22.library_of_exile.database.map_data_block.MapDataBlock;
import com.robertx22.library_of_exile.dimension.structure.MapStructure;
import com.robertx22.library_of_exile.util.wiki.WikiEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public class SecondaryStructureSpawnPosMB extends MapDataBlock {

    public SecondaryStructureSpawnPosMB(String id) {
        super(id, id);
    }

    @Override
    public Class<?> getClassForSerialization() {
        return SecondaryStructureSpawnPosMB.class;
    }

    @Override
    public void processImplementationINTERNAL(String key, BlockPos pos, Level world, CompoundTag nbt, MapBlockCtx ctx) {

        int count = 0;

        for (MapStructure struc : DungeonMain.MAP.secondaryStructures) {
            if (struc.isInside((ServerLevel) world, pos)) {
                DungeonMapCapability.get(world).data.data.getData(DungeonMain.MAIN_DUNGEON_STRUCTURE, pos).spawnPositions.put(struc.guid(), pos.asLong());
                count++;
            }
        }
        if (count > 1) {
            System.out.println("dssd");
        }

    }

    @Override
    public WikiEntry getWikiEntry() {
        return WikiEntry.of("In case your boss arena, uber arena or reward room have very weird spawn positions, you can force players to teleport to this position instead.");
    }
}
