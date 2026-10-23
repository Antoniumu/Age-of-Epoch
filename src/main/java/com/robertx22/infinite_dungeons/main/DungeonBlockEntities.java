package com.robertx22.infinite_dungeons.main;

import com.robertx22.forge_compat.fml.RegistryObject;
import com.robertx22.infinite_dungeons.block.ControlBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class DungeonBlockEntities {

    public static RegistryObject<BlockEntityType<?>> CONTROL_BLOCK = DungeonDeffered.BLOCK_ENTITIES.register("control_block",
        () -> FabricBlockEntityTypeBuilder.create(ControlBlockEntity::new, DungeonBlocks.CONTROL_BLOCK.get()).build());

    public static void init() {

    }

}
