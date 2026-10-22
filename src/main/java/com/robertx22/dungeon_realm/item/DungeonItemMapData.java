package com.robertx22.dungeon_realm.item;

import com.robertx22.dungeon_realm.structure.DungeonMapCapability;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;


// todo
public class DungeonItemMapData {

    public int x = 0;
    public int z = 0;

    public int bonus_contents = 1;

    public boolean uber = false;


    public ChunkPos getOrSetStartPos(Level world, ItemStack stack) {

        if (x == 0 && z == 0) {
            var start = DungeonMapCapability.get(world).data.counter.getNextAndIncrement();
            x = start.x;
            z = start.z;
        }
        DungeonItemNbt.DUNGEON_MAP.saveTo(stack, this);
        return new ChunkPos(x, z);
    }

}
