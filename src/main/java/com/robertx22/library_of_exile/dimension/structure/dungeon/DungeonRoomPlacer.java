package com.robertx22.library_of_exile.dimension.structure.dungeon;

import com.mojang.serialization.MapCodec;
import com.robertx22.library_of_exile.dimension.structure.MapStructure;
import com.robertx22.library_of_exile.events.base.ExileEvents;
import com.robertx22.library_of_exile.main.ExileLog;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.ArrayList;
import java.util.List;

public class DungeonRoomPlacer {

    public static boolean generatePiece(LevelAccessor world, BlockPos position, RandomSource random, Rotation rota, Identifier id) {

        var opt = world.getServer().getStructureTemplateManager().get(id);
        if (opt.isEmpty()) {
            ExileLog.get().warn("FATAL ERROR: Structure does not exist (" + id + ")");
            return false;
        }
        var template = opt.get();

        if (template.getSize().getX() > 16 || template.getSize().getZ() > 16) {
            ExileLog.get().warn("FATAL ERROR: Structure is bigger than possible (" + id + ") " + template.getSize().toString());
            return false;
        }

        StructurePlaceSettings settings = new StructurePlaceSettings()
            .setMirror(Mirror.NONE)
            .setRotation(rota)
            .setIgnoreEntities(false);

        if (rota == Rotation.COUNTERCLOCKWISE_90) {
            settings.setRotation(Rotation.COUNTERCLOCKWISE_90);
            position = position.offset(0, 0, template.getSize().getZ() - 1);
        } else if (rota == Rotation.CLOCKWISE_90) {
            settings.setRotation(Rotation.CLOCKWISE_90);
            position = position.offset(template.getSize().getX() - 1, 0, 0);
        } else if (rota == Rotation.CLOCKWISE_180) {
            settings.setRotation(Rotation.CLOCKWISE_180);
            position = position.offset(template.getSize().getX() - 1, 0, template.getSize().getZ() - 1);
        } else {
            settings.setRotation(Rotation.NONE);
        }

        List<StructureTemplate.StructureBlockInfo> dataBlocks = new ArrayList<>();
        settings.addProcessor(new StructureProcessor() {
            @Override
            public StructureTemplate.StructureBlockInfo processBlock(
                LevelReader levelReader,
                BlockPos targetPos,
                BlockPos refPos,
                BlockPos templateRelativePos,
                StructureTemplate.StructureBlockInfo processedInfo,
                StructurePlaceSettings placeSettings
            ) {
                if (processedInfo != null && (processedInfo.state().is(Blocks.COMMAND_BLOCK) || processedInfo.state().is(Blocks.STRUCTURE_BLOCK))) {
                    dataBlocks.add(processedInfo);
                }
                return processedInfo;
            }

            @Override
            public MapCodec<? extends StructureProcessor> codec() {
                return null;
            }
        });

        boolean done = template.placeInWorld((ServerLevelAccessor) world, position, position, settings, random, 2);

        for (StructureTemplate.StructureBlockInfo block : dataBlocks) {
            ExileEvents.DUNGEON_DATA_BLOCK_PLACED.callEvents(new ExileEvents.DungeonDataBlockPlaced(world, block.pos(), block, id));
        }

        return done;
    }

    public static boolean generateStructure(MapStructure struc, DungeonBuilder builder, LevelAccessor world, ChunkPos cpos) {

        builder.build();

        if (!builder.builtDungeon.hasRoomForChunk(struc, cpos)) {
            return false;
        }
        BuiltRoom room = builder.builtDungeon.getRoomForChunk(struc, cpos);
        if (room == null) {
            return false;
        }

        BlockPos position = new BlockPos(cpos.getMinBlockX(), 50, cpos.getMinBlockZ());
        generatePiece(world, position, world.getRandom(), room.data.rotation, room.getStructure());
        return true;
    }
}
