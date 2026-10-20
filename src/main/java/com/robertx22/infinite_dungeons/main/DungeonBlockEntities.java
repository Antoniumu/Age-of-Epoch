package com.robertx22.infinite_dungeons.main;

import com.robertx22.infinite_dungeons.block.ControlBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import com.robertx22.forge_compat.fml.RegistryObject;

public class DungeonBlockEntities {

    public static RegistryObject<BlockEntityType<?>> CONTROL_BLOCK = DungeonDeffered.BLOCK_ENTITIES.register("control_block",
        () -> BlockEntityType.Builder.of(ControlBlockEntity::new, DungeonBlocks.CONTROL_BLOCK.get())
            .build(null));

    public static void init() {

    }

}
