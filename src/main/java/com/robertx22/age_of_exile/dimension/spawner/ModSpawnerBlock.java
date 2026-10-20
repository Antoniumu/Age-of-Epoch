package com.robertx22.age_of_exile.dimension.spawner;

import com.robertx22.age_of_exile.vanilla_mc.blocks.bases.OpaqueBlock;
import net.minecraft.world.level.block.EntityBlock;
import com.robertx22.forge_compat.material.Material;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.BlockGetter;

public class ModSpawnerBlock extends OpaqueBlock implements EntityBlock {

    public ModSpawnerBlock() {
        super(Properties.of(Material.STONE)
            .strength(5F, 2));
    }

    @Override
    public BlockEntity newBlockEntity(BlockGetter world) {
        return new ModSpawnerBlockEntity();
    }

}

