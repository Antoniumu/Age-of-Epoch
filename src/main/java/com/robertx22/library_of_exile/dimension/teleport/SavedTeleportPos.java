package com.robertx22.library_of_exile.dimension.teleport;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public class SavedTeleportPos {

    public long pos = 0;
    public String dim = "";

    public Identifier getDimensionId() {
        return Identifier.parse(String.valueOf(dim));
    }

    public BlockPos getPos() {
        return BlockPos.of(pos);

    }

    public static SavedTeleportPos from(Identifier dim, BlockPos pos) {
        SavedTeleportPos d = new SavedTeleportPos();
        d.pos = pos.asLong();
        d.dim = dim.toString();
        return d;
    }

    public void setFrom(Player p) {
        this.pos = p.blockPosition().asLong();
        this.dim = p.level().dimension().identifier().toString();
    }

    public boolean isEmpty() {
        return pos == 0 && dim.isEmpty();
    }
}
