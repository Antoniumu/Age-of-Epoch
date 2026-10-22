package com.robertx22.world_of_exile.world_gen;

import com.robertx22.mine_and_slash.mmorpg.registers.common.SlashBlockEntities;
import com.robertx22.forge_compat.blocks.ITickableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class TowerBossMarkerBlockEntity extends BlockEntity implements ITickableBlockEntity {

    public boolean restoreCampfire = false;

    /** Tower theme id baked into the structure; empty means "use the biome". */
    public String theme = "";

    public TowerBossMarkerBlockEntity(BlockPos pos, BlockState state) {
        super(SlashBlockEntities.TOWER_BOSS_MARKER.get(), pos, state);
    }

    @Override
    public void tick() {
        serverTick();
    }

    public void serverTick() {
        TowerBoss.trySpawnFromMarker(this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("restore_campfire", restoreCampfire);
        tag.putString("theme", theme);
        output.store("AgeOfEpochData", CompoundTag.CODEC, tag);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.read("AgeOfEpochData", CompoundTag.CODEC).ifPresent(tag -> {
            restoreCampfire = tag.getBooleanOr("restore_campfire", false);
            theme = tag.getStringOr("theme", "");
        });
    }
}
