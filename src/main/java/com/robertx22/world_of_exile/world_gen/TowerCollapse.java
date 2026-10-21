package com.robertx22.world_of_exile.world_gen;

import com.robertx22.age_of_exile.mmorpg.registers.common.SlashBlocks;
import com.robertx22.library_of_exile.utils.SoundUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.FluidTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * After the tower boss dies, the structure is removed from the roof downward.
 * Chests are broken open and anything still inside is dropped.
 */
public final class TowerCollapse {

    private static final int HORIZONTAL = 40;
    private static final int DOWN = 96;
    private static final int UP = 16;
    private static final int MAX_BLOCKS = 24000;
    private static final int START_DELAY = 12;
    private static final int TICKS_PER_LAYER = 3;

    private static final List<Job> JOBS = new ArrayList<>();

    private TowerCollapse() {
    }

    public static void start(ServerLevel level, BlockPos origin) {
        Set<BlockPos> blocks = collect(level, origin);
        if (blocks.isEmpty()) {
            return;
        }
        Map<Integer, List<BlockPos>> layers = new HashMap<>();
        for (BlockPos pos : blocks) {
            layers.computeIfAbsent(pos.getY(), y -> new ArrayList<>()).add(pos.immutable());
        }
        List<Integer> ys = new ArrayList<>(layers.keySet());
        ys.sort(Comparator.reverseOrder());
        JOBS.add(new Job(level, origin, layers, ys));
        SoundUtils.playSound(level, origin, SoundEvents.GENERIC_EXPLODE, 0.9F, 0.75F);
    }

    public static void tick(ServerLevel level) {
        Iterator<Job> it = JOBS.iterator();
        while (it.hasNext()) {
            Job job = it.next();
            if (job.level != level) {
                continue;
            }
            if (job.delay > 0) {
                job.delay--;
                continue;
            }
            if (job.next >= job.ys.size()) {
                it.remove();
                continue;
            }
            int y = job.ys.get(job.next++);
            destroyLayer(level, job.layers.get(y), job.origin, job.next);
            job.delay = TICKS_PER_LAYER - 1;
            if (job.next >= job.ys.size()) {
                it.remove();
            }
        }
    }

    public static boolean isTowerMaterial(BlockState state) {
        if (state.isAir()) {
            return false;
        }
        var block = state.getBlock();
        if (block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST || block == Blocks.BARREL || block == Blocks.SPAWNER) {
            return true;
        }
        if (block == SlashBlocks.TOWER_BOSS_MARKER.get()) {
            return true;
        }
        if (state.is(BlockTags.ALL_SIGNS) || state.is(BlockTags.WALL_SIGNS) || state.is(BlockTags.DOORS)
            || state.is(BlockTags.FENCES) || state.is(BlockTags.CAMPFIRES)) {
            return true;
        }
        if (block == Blocks.LADDER || block == Blocks.IRON_BARS || block == Blocks.COBWEB
            || block == Blocks.TORCH || block == Blocks.WALL_TORCH || block == Blocks.SOUL_TORCH
            || block == Blocks.SOUL_WALL_TORCH || block == Blocks.REDSTONE_TORCH || block == Blocks.REDSTONE_WALL_TORCH
            || block == Blocks.SOUL_LANTERN || block == Blocks.LANTERN || block == Blocks.LEVER
            || block == Blocks.BEACON) {
            return true;
        }
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        if (id == null) {
            return false;
        }
        String path = id.getPath();
        return path.contains("stone_brick") || path.contains("smooth_stone") || path.contains("polished_blackstone");
    }

    private static Set<BlockPos> collect(ServerLevel level, BlockPos origin) {
        Set<BlockPos> tower = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        int minX = origin.getX() - HORIZONTAL;
        int maxX = origin.getX() + HORIZONTAL;
        int minZ = origin.getZ() - HORIZONTAL;
        int maxZ = origin.getZ() + HORIZONTAL;
        int minY = Math.max(level.getMinY(), origin.getY() - DOWN);
        int maxY = Math.min(level.getMaxY(), origin.getY() + UP);

        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    queue.add(origin.offset(dx, dy, dz));
                }
            }
        }

        while (!queue.isEmpty() && tower.size() < MAX_BLOCKS) {
            BlockPos pos = queue.poll();
            if (pos.getX() < minX || pos.getX() > maxX || pos.getZ() < minZ || pos.getZ() > maxZ
                || pos.getY() < minY || pos.getY() > maxY) {
                continue;
            }
            if (!tower.add(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (!isTowerMaterial(state)) {
                tower.remove(pos);
                if (state.getFluidState().isSource() && state.getFluidState().is(FluidTags.LAVA)) {
                    if (touchesTower(level, pos, tower)) {
                        tower.add(pos);
                    }
                }
                continue;
            }
            queue.add(pos.above());
            queue.add(pos.below());
            queue.add(pos.north());
            queue.add(pos.south());
            queue.add(pos.east());
            queue.add(pos.west());
        }

        boolean grew = true;
        for (int pass = 0; pass < 3 && grew && tower.size() < MAX_BLOCKS; pass++) {
            grew = false;
            List<BlockPos> snapshot = new ArrayList<>(tower);
            for (BlockPos base : snapshot) {
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dy = -2; dy <= 2; dy++) {
                        for (int dz = -2; dz <= 2; dz++) {
                            if (dx == 0 && dy == 0 && dz == 0) {
                                continue;
                            }
                            BlockPos near = base.offset(dx, dy, dz);
                            if (near.getX() < minX || near.getX() > maxX || near.getZ() < minZ || near.getZ() > maxZ
                                || near.getY() < minY || near.getY() > maxY || tower.contains(near)) {
                                continue;
                            }
                            if (isTowerMaterial(level.getBlockState(near))) {
                                tower.add(near);
                                grew = true;
                            }
                        }
                    }
                }
            }
        }

        List<BlockPos> extra = new ArrayList<>();
        for (BlockPos pos : tower) {
            for (BlockPos near : new BlockPos[]{pos.above(), pos.below(), pos.north(), pos.south(), pos.east(), pos.west()}) {
                BlockState state = level.getBlockState(near);
                if (isContainer(state) || state.is(BlockTags.ALL_SIGNS) || state.getBlock() == Blocks.SPAWNER) {
                    extra.add(near.immutable());
                }
            }
        }
        tower.addAll(extra);
        return tower;
    }

    private static boolean touchesTower(ServerLevel level, BlockPos pos, Set<BlockPos> tower) {
        return tower.contains(pos.above()) || tower.contains(pos.below())
            || tower.contains(pos.north()) || tower.contains(pos.south())
            || tower.contains(pos.east()) || tower.contains(pos.west());
    }

    private static void destroyLayer(ServerLevel level, List<BlockPos> positions, BlockPos origin, int layerIndex) {
        if (positions == null || positions.isEmpty()) {
            return;
        }
        int particles = 0;
        BlockPos soundPos = positions.get(positions.size() / 2);
        for (BlockPos pos : positions) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof Container) {
                spillContainer(level, pos, be);
            }
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            if (particles < 10) {
                level.sendParticles(ParticleTypes.POOF, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    6, 0.35D, 0.2D, 0.35D, 0.02D);
                level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5D, pos.getY() + 0.4D, pos.getZ() + 0.5D,
                    4, 0.25D, 0.15D, 0.25D, 0.01D);
                particles++;
            }
        }
        BlockPos boom = soundPos == null ? origin : soundPos;
        level.sendParticles(ParticleTypes.EXPLOSION, boom.getX() + 0.5D, boom.getY() + 0.5D, boom.getZ() + 0.5D,
            2, 0.4D, 0.2D, 0.4D, 0.0D);
        SoundUtils.playSound(level, boom, SoundType.STONE.getBreakSound(), 1.1F, 0.65F + (layerIndex % 4) * 0.05F);
        if ((layerIndex % 3) == 0) {
            SoundUtils.playSound(level, boom, SoundEvents.GENERIC_EXPLODE, 0.45F, 0.85F);
        }
    }

    private static boolean isContainer(BlockState state) {
        var block = state.getBlock();
        return block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST || block == Blocks.BARREL;
    }

    private static void spillContainer(ServerLevel level, BlockPos pos, BlockEntity be) {
        if (!(be instanceof Container container)) {
            return;
        }
        invokeUnpack(be);
        if (isEmpty(container)) {
            dropGeneratedLoot(level, pos, be);
            return;
        }
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            drop(level, pos, stack.copy());
            container.setItem(i, ItemStack.EMPTY);
        }
    }

    private static boolean isEmpty(Container container) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (!container.getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static void invokeUnpack(BlockEntity be) {
        Class<?> type = be.getClass();
        while (type != null && type != Object.class) {
            for (Method method : type.getDeclaredMethods()) {
                String name = method.getName();
                if (!name.equals("unpackLootTable") && !name.equals("unpackLoot")) {
                    continue;
                }
                try {
                    method.setAccessible(true);
                    Object[] args = new Object[method.getParameterCount()];
                    Class<?>[] params = method.getParameterTypes();
                    for (int i = 0; i < params.length; i++) {
                        if (params[i] == long.class) {
                            args[i] = 0L;
                        } else if (params[i] == int.class) {
                            args[i] = 0;
                        } else if (params[i] == boolean.class) {
                            args[i] = Boolean.FALSE;
                        }
                    }
                    method.invoke(be, args);
                    return;
                } catch (Exception ignored) {
                }
            }
            type = type.getSuperclass();
        }
    }

    private static void dropGeneratedLoot(ServerLevel level, BlockPos pos, BlockEntity be) {
        ResourceKey<LootTable> key = findLootKey(be);
        if (key == null) {
            return;
        }
        ServerPlayer nearest = null;
        double best = 128.0D * 128.0D;
        for (ServerPlayer player : level.players()) {
            double dist = player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ());
            if (dist < best) {
                best = dist;
                nearest = player;
            }
        }
        try {
            LootParams.Builder builder = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.BLOCK_STATE, level.getBlockState(pos))
                .withParameter(LootContextParams.TOOL, ItemStack.EMPTY);
            if (nearest != null) {
                builder.withParameter(LootContextParams.THIS_ENTITY, nearest);
            }
            LootParams params;
            try {
                params = builder.create(LootContextParamSets.CHEST);
            } catch (Exception e) {
                params = builder.create(LootContextParamSets.BLOCK);
            }
            LootTable table = level.getServer().reloadableRegistries().getLootTable(key);
            for (ItemStack stack : table.getRandomItems(params)) {
                drop(level, pos, stack);
            }
        } catch (Exception ignored) {
        }
    }

    @SuppressWarnings("unchecked")
    private static ResourceKey<LootTable> findLootKey(BlockEntity be) {
        Class<?> type = be.getClass();
        while (type != null && type != Object.class) {
            for (Method method : type.getMethods()) {
                if (method.getParameterCount() != 0) {
                    continue;
                }
                String name = method.getName();
                if (!name.toLowerCase().contains("loot")) {
                    continue;
                }
                try {
                    Object value = method.invoke(be);
                    ResourceKey<LootTable> key = asLootKey(value);
                    if (key != null) {
                        return key;
                    }
                } catch (Exception ignored) {
                }
            }
            type = type.getSuperclass();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static ResourceKey<LootTable> asLootKey(Object value) {
        if (value instanceof ResourceKey<?> key && key.registry().equals(Registries.LOOT_TABLE)) {
            return (ResourceKey<LootTable>) key;
        }
        if (value instanceof Identifier id) {
            return ResourceKey.create(Registries.LOOT_TABLE, id);
        }
        if (value instanceof OptionalLike optional) {
            return asLootKey(optional.orElse(null));
        }
        if (value instanceof java.util.Optional<?> optional) {
            return optional.map(TowerCollapse::asLootKey).orElse(null);
        }
        return null;
    }

    private static void drop(ServerLevel level, BlockPos pos, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.6D;
        double z = pos.getZ() + 0.5D;
        ItemEntity entity = new ItemEntity(level, x, y, z, stack);
        entity.setDeltaMovement(
            (level.random.nextDouble() - 0.5D) * 0.25D,
            0.18D + level.random.nextDouble() * 0.12D,
            (level.random.nextDouble() - 0.5D) * 0.25D);
        entity.setPickUpDelay(15);
        level.addFreshEntity(entity);
    }

    private static final class Job {
        final ServerLevel level;
        final BlockPos origin;
        final Map<Integer, List<BlockPos>> layers;
        final List<Integer> ys;
        int next;
        int delay = START_DELAY;

        Job(ServerLevel level, BlockPos origin, Map<Integer, List<BlockPos>> layers, List<Integer> ys) {
            this.level = level;
            this.origin = origin;
            this.layers = layers;
            this.ys = ys;
        }
    }
}
