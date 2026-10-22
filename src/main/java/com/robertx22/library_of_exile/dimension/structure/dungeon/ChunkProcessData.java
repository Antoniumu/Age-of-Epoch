package com.robertx22.library_of_exile.dimension.structure.dungeon;


import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class ChunkProcessData {

    public ChunkProcessData(ChunkAccess chunk) {
        this.chunk = chunk;
    }

    public ChunkAccess chunk;
    public boolean chanceChest = false;

    public int map_reward_chests = 0;


    public void iterateBlocks(Function<BlockPos, BlockPos> function) {

        for (int x = 0; x < 16; x++) {
            for (int y = -16; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    function.apply(new BlockPos(chunk.getPos().getMinBlockX(), 0, chunk.getPos().getMinBlockZ()).offset(x, y, z));
                }
            }
        }

    }

    // todo get rid of this maybe, or improve it
    public List<BlockPos> getBlockPosList() {


        List<BlockPos> list = new ArrayList<>();

        var bpos = new BlockPos(chunk.getPos().getMinBlockX(), 0, chunk.getPos().getMinBlockZ());
        for (int x = 0; x < 16; x++) {
            for (int y = -16; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    list.add(bpos.offset(x, y, z));
                }
            }
        }

        return list;
    }

}
