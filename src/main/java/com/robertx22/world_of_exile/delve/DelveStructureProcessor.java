package com.robertx22.world_of_exile.delve;

import com.mojang.serialization.MapCodec;
import com.robertx22.mine_and_slash.mmorpg.registers.common.SlashBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/** Convert template-local room/exit/gate coordinates using the SAME transform as blocks. */
public class DelveStructureProcessor implements StructureProcessor {
    public static final MapCodec<DelveStructureProcessor> CODEC = MapCodec.unit(DelveStructureProcessor::new);

    @Override
    public MapCodec<? extends StructureProcessor> codec() { return CODEC; }

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader level, BlockPos targetPosition,
            BlockPos referencePos, BlockPos templateRelativePos, StructureTemplate.StructureBlockInfo info,
            StructurePlaceSettings settings) {
        if (info == null) return null;
        // The shallow soil cap outside the entrance shaft belongs to terrain.
        if (info.state().is(net.minecraft.world.level.block.Blocks.STRUCTURE_VOID)) return null;
        if (!info.state().is(SlashBlocks.DELVE_MARKER.get()) || info.nbt() == null) return info;
        CompoundTag tag = info.nbt().copy();
        CompoundTag data = tag.getCompound("AgeOfEpochData").orElseGet(CompoundTag::new);
        for (String prefix : new String[]{"center", "exit", "reward", "gate"}) {
            if (prefix.equals("gate") && !data.getBooleanOr("has_gate", false)) continue;
            BlockPos local = new BlockPos(data.getIntOr(prefix + "_x", 0), data.getIntOr(prefix + "_y", 0), data.getIntOr(prefix + "_z", 0));
            BlockPos absolute = StructureTemplate.calculateRelativePosition(settings, local).offset(targetPosition);
            data.putInt(prefix + "_x", absolute.getX());
            data.putInt(prefix + "_y", absolute.getY());
            data.putInt(prefix + "_z", absolute.getZ());
        }
        // Rotate the ladder used to replace a locked hatch when its warden dies.
        data.putString("ladder_facing", settings.getRotation().rotate(net.minecraft.core.Direction.WEST).getName());
        tag.put("AgeOfEpochData", data);
        return new StructureTemplate.StructureBlockInfo(info.pos(), info.state(), tag);
    }
}
