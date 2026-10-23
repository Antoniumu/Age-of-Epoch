package com.robertx22.divine_missions.main;

import com.robertx22.divine_missions.block.ShrineBlock;
import net.minecraft.world.level.block.Block;
import com.robertx22.forge_compat.fml.RegistryObject;

public class ModBlocks {

    public static RegistryObject<Block> SHRINE = DivineDeffered.BLOCKS.register("shrine", () -> new ShrineBlock());

    public static void init() {

    }

}
