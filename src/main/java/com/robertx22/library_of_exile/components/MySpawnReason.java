package com.robertx22.library_of_exile.components;

import net.minecraft.world.entity.EntitySpawnReason;

public enum MySpawnReason {
    NATURAL,
    SPAWNER,
    OTHER;

    public static MySpawnReason get(EntitySpawnReason reason) {

        if (reason == EntitySpawnReason.SPAWNER || reason == EntitySpawnReason.TRIAL_SPAWNER) {
            return SPAWNER;
        }
        if (reason == EntitySpawnReason.BREEDING || reason == EntitySpawnReason.BUCKET || reason == EntitySpawnReason.DISPENSER) {
            return OTHER;
        }

        return NATURAL;
    }

}
