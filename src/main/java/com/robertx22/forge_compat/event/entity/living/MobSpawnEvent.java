package com.robertx22.forge_compat.event.entity.living;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ServerLevelAccessor;

public class MobSpawnEvent extends LivingEvent {
    private final ServerLevelAccessor level;
    private final double x, y, z;

    public MobSpawnEvent(Mob mob, ServerLevelAccessor level, double x, double y, double z) {
        super(mob);
        this.level = level;
        this.x = x; this.y = y; this.z = z;
    }

    @Override public Mob getEntity() { return (Mob) super.getEntity(); }
    public ServerLevelAccessor getLevel() { return level; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }

    public static class FinalizeSpawn extends MobSpawnEvent {
        private final EntitySpawnReason spawnType;
        private boolean spawnCancelled = false;

        public FinalizeSpawn(Mob mob, ServerLevelAccessor level, double x, double y, double z, EntitySpawnReason spawnType) {
            super(mob, level, x, y, z);
            this.spawnType = spawnType;
        }
        public EntitySpawnReason getSpawnType() { return spawnType; }
        public void setSpawnCancelled(boolean c) { this.spawnCancelled = c; }
        public boolean isSpawnCancelled() { return spawnCancelled; }
    }

    public static class PositionCheck extends MobSpawnEvent {
        private final EntitySpawnReason spawnType;
        public PositionCheck(Mob mob, ServerLevelAccessor level, EntitySpawnReason spawnType) {
            super(mob, level, mob.getX(), mob.getY(), mob.getZ());
            this.spawnType = spawnType;
        }
        public EntitySpawnReason getSpawnType() { return spawnType; }
    }

    public static class SpawnPlacementCheck extends com.robertx22.forge_compat.eventbus.api.Event {
        private final EntityType<?> entityType;
        private final ServerLevelAccessor level;
        private final EntitySpawnReason spawnType;
        private final BlockPos pos;
        private final RandomSource random;

        public SpawnPlacementCheck(EntityType<?> entityType, ServerLevelAccessor level, EntitySpawnReason spawnType, BlockPos pos, RandomSource random) {
            this.entityType = entityType;
            this.level = level;
            this.spawnType = spawnType;
            this.pos = pos;
            this.random = random;
        }
        public EntityType<?> getEntityType() { return entityType; }
        public ServerLevelAccessor getLevel() { return level; }
        public EntitySpawnReason getSpawnType() { return spawnType; }
        public BlockPos getPos() { return pos; }
        public RandomSource getRandom() { return random; }
    }
}
