package com.robertx22.dungeon_realm.structure;

public enum MobSpawnBlockKind {
    MOB {
        @Override
        public void incrementDataBlockCount(DungeonMapData data) {
            data.mobDataBlockCount++;
        }
    },
    MOB_PACK {
        @Override
        public void incrementDataBlockCount(DungeonMapData data) {
            data.packDataBlockCount++;
        }
    },
    ELITE {
        @Override
        public void incrementDataBlockCount(DungeonMapData data) {
            data.eliteDataBlockCount++;
        }
    },
    ELITE_PACK {
        @Override
        public void incrementDataBlockCount(DungeonMapData data) {
            data.elitePackDataBlockCount++;
        }
    },
    MINI_BOSS {
        @Override
        public void incrementDataBlockCount(DungeonMapData data) {
            data.miniBossDataBlockCount++;
        }
    };

    public abstract void incrementDataBlockCount(DungeonMapData data);

}
