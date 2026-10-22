package com.robertx22.forge_compat.event.entity.living;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;

public class LivingDestroyBlockEvent extends LivingEvent {
    private final BlockPos pos;
    private final BlockState state;

    public LivingDestroyBlockEvent(LivingEntity entity, BlockPos pos, BlockState state) {
        super(entity);
        this.pos = pos;
        this.state = state;
    }

    public BlockPos getPos() { return pos; }
    public BlockState getState() { return state; }
}
