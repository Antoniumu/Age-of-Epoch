package com.robertx22.library_of_exile.components;

import net.minecraft.world.entity.EntitySpawnReason;

public enum MySpawnReason {
    NATURAL,
    SPAWNER,
    OTHER;

    public static MySpawnReason get(EntitySpawnReason reason) {

        if (reason == EntitySpawnReason.SPAWNER) {
            return SPAWNER;
        }
        if (reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.CHUNK_GENERATION || reason == EntitySpawnReason.STRUCTURE) {
            return NATURAL;
        }

        return OTHER;
    }

}
