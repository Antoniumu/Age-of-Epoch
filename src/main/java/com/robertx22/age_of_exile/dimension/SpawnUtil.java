package com.robertx22.age_of_exile.dimension;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class SpawnUtil {

    public static boolean canPlaceMob(Level world, EntityType type, BlockPos p) {
        AABB box = type.getAABB(p.getX(), p.getY(), p.getZ());
        boolean can = world.noCollision(box);
        return can;
    }

    public static boolean canPlaceBlock(Level world, BlockPos p) {
        if (!world.isEmptyBlock(p.above()) || !world.isEmptyBlock(p) || !world.getBlockState(p.below())
            .isRedstoneConductor(world, p.below())) {

            return false;
        }
        return true;
    }
}
