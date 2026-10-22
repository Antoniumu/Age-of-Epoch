package com.robertx22.the_harvest.item;

import com.robertx22.the_harvest.structure.HarvestMapCap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public class HarvestItemMapData {


    public int x = 0;
    public int z = 0;


    public boolean relic = false;

    public ChunkPos getOrSetStartPos(Level world, ItemStack stack) {

        if (x == 0 && z == 0) {
            var start = HarvestMapCap.get(world).data.counter.getNextAndIncrement();
            x = start.x;
            z = start.z;
        }
        HarvestItemNbt.HARVEST_MAP.saveTo(stack, this);
        return new ChunkPos(x, z);
    }
}
