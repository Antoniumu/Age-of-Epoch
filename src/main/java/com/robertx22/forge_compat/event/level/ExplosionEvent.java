package com.robertx22.forge_compat.event.level;

import com.robertx22.forge_compat.eventbus.api.Event;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;

import java.util.List;

public class ExplosionEvent extends Event {
    private final Level level;
    private final Explosion explosion;

    public ExplosionEvent(Level level, Explosion explosion) {
        this.level = level;
        this.explosion = explosion;
    }

    public Level getLevel() { return level; }
    public Explosion getExplosion() { return explosion; }

    public static class Start extends ExplosionEvent {
        public Start(Level level, Explosion explosion) { super(level, explosion); }
    }

    public static class Detonate extends ExplosionEvent {
        private final List<Entity> entityList;
        private final List<BlockPos> blockList;

        public Detonate(Level level, Explosion explosion, List<Entity> entityList, List<BlockPos> blockList) {
            super(level, explosion);
            this.entityList = entityList;
            this.blockList = blockList;
        }

        public List<Entity> getAffectedEntities() { return entityList; }
        public List<BlockPos> getAffectedBlocks() { return blockList; }
    }
}
