package com.robertx22.world_of_exile.world_gen;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

public class TowerDestroyer {

    public static final ConcurrentLinkedDeque<TowerDestroyer> LIST = new ConcurrentLinkedDeque<>();

    public static void start(Level world, BlockPos topPos) {
        if (world != null && !world.isClientSide()) {
            LIST.add(new TowerDestroyer(topPos, world));
        }
    }

    public static void tickAll(Level world) {
        if (world == null || world.isClientSide()) {
            return;
        }
        LIST.forEach(x -> x.onTick(world));
        LIST.removeIf(x -> x.isDone(world));
    }

    private BlockPos pos;
    private final Level world;
    private boolean started = false;
    private int tick = 0;
    private int timesDidntDestroy = 0;

    private static final List<Block> EXTRA_BLOCKS = List.of(
        Blocks.COBWEB,
        Blocks.LAVA,
        Blocks.WATER,
        Blocks.SMOOTH_STONE,
        Blocks.SPAWNER,
        Blocks.CHEST,
        Blocks.LADDER,
        Blocks.OBSIDIAN,
        Blocks.IRON_BARS,
        Blocks.BLACKSTONE,
        Blocks.POLISHED_BLACKSTONE,
        Blocks.POLISHED_BLACKSTONE_BRICKS,
        Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS,
        Blocks.CHISELED_POLISHED_BLACKSTONE,
        Blocks.GILDED_BLACKSTONE,
        Blocks.NETHER_BRICKS,
        Blocks.LANTERN,
        Blocks.SOUL_LANTERN,
        Blocks.TORCH,
        Blocks.WALL_TORCH
    );

    public TowerDestroyer(BlockPos pos, Level world) {
        this.pos = pos;
        this.world = world;
    }

    public boolean isDone(Level world) {
        if (!this.world.equals(world)) {
            return false;
        }
        if (pos.getY() < world.getMinY() + 10) {
            return true;
        }
        return timesDidntDestroy > 4;
    }

    private static boolean shouldDestroyBlock(BlockState state) {
        if (state.isAir()) {
            return false;
        }
        if (state.is(BlockTags.ALL_SIGNS)
            || state.is(BlockTags.STONE_BRICKS)
            || state.is(BlockTags.STAIRS)
            || state.is(BlockTags.SLABS)
            || state.is(BlockTags.WALLS)
            || state.is(BlockTags.FENCES)
            || state.is(BlockTags.PLANKS)) {
            return true;
        }
        Block block = state.getBlock();
        return EXTRA_BLOCKS.contains(block);
    }

    public void onTick(Level world) {
        try {
            if (!this.world.equals(world)) {
                return;
            }

            tick++;

            if (!started && tick % 20 == 0) {
                world.playSound(null, pos, SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            if (!started && tick > 160) {
                started = true;
                return;
            }

            if (started && tick % 20 == 0) {
                boolean destroyed = false;

                for (int x = -10; x <= 10; x++) {
                    for (int z = -10; z <= 10; z++) {
                        BlockPos p = new BlockPos(pos.getX() + x, pos.getY(), pos.getZ() + z);
                        BlockState state = world.getBlockState(p);
                        Block block = state.getBlock();

                        if (block == Blocks.CHEST) {
                            try {
                                BlockEntity be = world.getBlockEntity(p);
                                if (be instanceof ChestBlockEntity chest) {
                                    chest.setLootTable(null, 0L);
                                }
                                world.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
                                destroyed = true;
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else if (shouldDestroyBlock(state)) {
                            destroyed = true;
                            world.destroyBlock(p, false);
                            world.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
                        }
                    }
                }

                if (!destroyed) {
                    timesDidntDestroy++;
                } else {
                    timesDidntDestroy = 0;
                }

                pos = pos.below(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
