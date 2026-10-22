package com.robertx22.forge_compat.event.level;

import com.robertx22.forge_compat.eventbus.api.Event;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEvent extends Event {
    private final LevelAccessor level;
    private final BlockPos pos;
    private final BlockState state;

    public BlockEvent(LevelAccessor level, BlockPos pos, BlockState state) {
        this.level = level;
        this.pos = pos;
        this.state = state;
    }

    public LevelAccessor getLevel() { return level; }
    public BlockPos getPos() { return pos; }
    public BlockState getState() { return state; }

    public static class BreakEvent extends BlockEvent {
        private final Player player;
        private int exp = 0;

        public BreakEvent(LevelAccessor level, BlockPos pos, BlockState state, Player player) {
            super(level, pos, state);
            this.player = player;
        }

        public Player getPlayer() { return player; }
        public int getExpToDrop() { return exp; }
        public void setExpToDrop(int exp) { this.exp = exp; }
    }

    public static class EntityPlaceEvent extends BlockEvent {
        private final Entity entity;
        private final BlockState placedBlock;

        public EntityPlaceEvent(LevelAccessor level, BlockPos pos, BlockState placedBlock, Entity entity) {
            super(level, pos, placedBlock);
            this.entity = entity;
            this.placedBlock = placedBlock;
        }

        public Entity getEntity() { return entity; }
        public BlockState getPlacedBlock() { return placedBlock; }
    }
}
