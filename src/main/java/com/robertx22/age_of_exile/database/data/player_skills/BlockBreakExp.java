package com.robertx22.age_of_exile.database.data.player_skills;

import net.minecraft.world.level.block.Block;
import net.minecraft.resources.Identifier;
import net.minecraft.core.Registry;

import net.minecraft.core.registries.BuiltInRegistries;
public class BlockBreakExp {

    public float exp = 0;
    public String block_id = "";

    public BlockBreakExp(float exp, Block block) {
        this.exp = exp;
        this.block_id = BuiltInRegistries.BLOCK.getKey(block)
            .toString();
    }

    public Block getBlock() {
        return BuiltInRegistries.BLOCK.getValue(Identifier.parse(block_id));
    }

    public BlockBreakExp() {
    }
}
