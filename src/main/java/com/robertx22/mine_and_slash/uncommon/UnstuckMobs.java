package com.robertx22.mine_and_slash.uncommon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.Arrays;
import java.util.List;

public class UnstuckMobs {

    static List<Direction> dirs = Arrays.asList(Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.UP, Direction.DOWN);

    private static boolean isEntityStuckInWall(Entity en) {
        if (en.noPhysics) {
            return false;
        }
        BlockPos eyePos = BlockPos.containing(en.getEyePosition());
        return !en.level().isEmptyBlock(eyePos) && !en.level().noCollision(en.getBoundingBox().deflate(1.0E-7D));
    }

    public static void unstuckFromWalls(Entity en) {
        if (isEntityStuckInWall(en)) {
            for (Direction dir : dirs) {
                for (int i = 1; i < 3; i++) {
                    if (en.level().isEmptyBlock(en.blockPosition().relative(dir))) {
                        var tp = en.blockPosition().relative(dir, i);
                        en.teleportTo(tp.getX() + 0.5D, tp.getY(), tp.getZ() + 0.5D);
                        break;
                    }
                }
            }
        }
        if (isEntityStuckInWall(en)) {
            en.setInvulnerableTime(0);
            if (en.level() instanceof ServerLevel sl) {
                en.hurtServer(sl, en.damageSources().fellOutOfWorld(), 999999);
            }
            en.discard();
        }
    }
}
