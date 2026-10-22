package com.robertx22.library_of_exile.components;

import net.minecraft.world.entity.EntitySpawnReason;

public enum MySpawnReason {
    NATURAL,
    SPAWNER,
    OTHER;

    public static MySpawnReason get(net.minecraft.world.entity.EntitySpawnReason reason) {

        if (reason == net.minecraft.world.entity.EntitySpawnReason.SPAWNER) {
            return SPAWNER;
        }
        if (reason == net.minecraft.world.entity.EntitySpawnReason.NATURAL || reason == net.minecraft.world.entity.EntitySpawnReason.CHUNK_GENERATION || reason == net.minecraft.world.entity.EntitySpawnReason.STRUCTURE) {
            return NATURAL;
        }

        return OTHER;
    }

}
