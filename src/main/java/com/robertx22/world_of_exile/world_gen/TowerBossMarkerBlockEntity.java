package com.robertx22.world_of_exile.world_gen;

import com.robertx22.age_of_exile.mmorpg.registers.common.SlashBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class TowerBossMarkerBlockEntity extends BlockEntity {

    public boolean restoreCampfire = false;

    public TowerBossMarkerBlockEntity(BlockPos pos, BlockState state) {
        super(SlashBlockEntities.TOWER_BOSS_MARKER.get(), pos, state);
    }

    public void serverTick() {
        TowerBoss.trySpawnFromMarker(this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("restore_campfire", restoreCampfire);
        output.store("AgeOfEpochData", CompoundTag.CODEC, tag);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.read("AgeOfEpochData", CompoundTag.CODEC).ifPresent(tag -> {
            restoreCampfire = tag.getBooleanOr("restore_campfire", false);
        });
    }
}
