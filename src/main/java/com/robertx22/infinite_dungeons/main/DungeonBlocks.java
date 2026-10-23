package com.robertx22.infinite_dungeons.main;

import com.robertx22.infinite_dungeons.block.ControlBlock;
import com.robertx22.forge_compat.fml.RegistryObject;

public class DungeonBlocks {

    public static RegistryObject<ControlBlock> CONTROL_BLOCK = DungeonDeffered.BLOCKS.register("control_block", () -> new ControlBlock());

    public static void init() {

    }

}
