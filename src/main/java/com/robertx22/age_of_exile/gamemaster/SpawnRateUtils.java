package com.robertx22.age_of_exile.gamemaster;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.util.RandomSource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Shared helpers for applying GameMaster spawn rates to a nearby player. */
public final class SpawnRateUtils {

    /** Vanilla natural spawning does not occur more than 128 blocks from a player. */
    private static final double NATURAL_SPAWN_RADIUS = 128.0;

    /** Cache one roll per world/chunk/player/tick, so spawn retries cannot undo the rate reduction. */
    private static final Map<ServerLevel, NaturalSpawnDecisionCache> NATURAL_SPAWN_DECISIONS = new WeakHashMap<>();

    private SpawnRateUtils() {
    }

    /** Only hostile mob types are rate-controlled; animals, ambient mobs and projectiles are untouched. */
    public static boolean isRateControlledHostile(EntityType<?> type) {
        if (type == null || type.getCategory() != MobCategory.MONSTER) {
            return false;
        }

        String path = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath();
        return !isBossId(path);
    }

    /** Vanilla bosses are not affected, even if a data pack puts one in a regular spawner. */
    public static boolean isBossId(String entityPath) {
        if (entityPath == null) {
            return false;
        }
        String path = entityPath.toLowerCase(java.util.Locale.ROOT);
        return path.equals("ender_dragon")
            || path.equals("wither")
            || path.equals("warden")
            || path.equals("elder_guardian")
            || path.contains("boss");
    }

    /**
     * Roll one natural-spawn opportunity using the nearest eligible survival player.
     * A null player (for example, no players in the dimension) leaves vanilla unchanged.
     */
    public static boolean allowNaturalSpawn(ServerLevel level, EntityType<?> type, BlockPos pos, RandomSource random) {
        if (!isRateControlledHostile(type) || !GameMaster.isActive()) {
            return true;
        }

        ServerPlayer player = nearestEligiblePlayer(level, pos.getX() + 0.5, pos.getY() + 0.5,
            pos.getZ() + 0.5, NATURAL_SPAWN_RADIUS);
        if (player == null) {
            return true;
        }

        long chunkKey = ((long) (pos.getX() >> 4) << 32) | ((pos.getZ() >> 4) & 0xffffffffL);
        long gameTime = level.getGameTime();
        NaturalSpawnDecisionKey key = new NaturalSpawnDecisionKey(chunkKey, player.getUUID());
        synchronized (NATURAL_SPAWN_DECISIONS) {
            NaturalSpawnDecisionCache cache = NATURAL_SPAWN_DECISIONS.computeIfAbsent(level,
                ignored -> new NaturalSpawnDecisionCache());
            if (cache.gameTime != gameTime) {
                cache.gameTime = gameTime;
                cache.decisions.clear();
            }
            Boolean allowed = cache.decisions.get(key);
            if (allowed != null) {
                return allowed;
            }
            boolean roll = allowSpawnAttempt(player, random);
            cache.decisions.put(key, roll);
            return roll;
        }
    }

    /**
     * Find the nearest living, non-creative player within {@code maxRange}.
     * The block spawner uses its own vanilla activation radius here.
     */
    public static ServerPlayer nearestEligiblePlayer(ServerLevel level, double x, double y, double z,
                                                       double maxRange) {
        if (level == null || maxRange < 0) {
            return null;
        }

        double maxDistanceSqr = maxRange * maxRange;
        double nearestDistanceSqr = maxDistanceSqr;
        ServerPlayer nearest = null;
        for (ServerPlayer player : level.players()) {
            if (!player.isAlive() || player.isSpectator() || player.isCreative()) {
                continue;
            }
            double distanceSqr = player.distanceToSqr(x, y, z);
            if (distanceSqr <= nearestDistanceSqr) {
                nearestDistanceSqr = distanceSqr;
                nearest = player;
            }
        }
        return nearest;
    }

    /**
     * Returns false for a spawn opportunity rejected by GameMaster. Rates are probabilities
     * in [0, 1], so a multiplier of 0.5 means about half of the opportunities are skipped.
     */
    public static boolean allowSpawnAttempt(ServerPlayer player, RandomSource random) {
        if (player == null || !GameMaster.isActive()) {
            return true;
        }

        double rate = GameMaster.get().spawnRateMultiplier(player);
        if (rate >= 1.0) {
            return true;
        }
        if (rate <= 0.0) {
            return false;
        }
        return random.nextDouble() < rate;
    }

    private record NaturalSpawnDecisionKey(long chunkKey, UUID playerId) {
    }

    private static final class NaturalSpawnDecisionCache {
        private long gameTime = Long.MIN_VALUE;
        private final Map<NaturalSpawnDecisionKey, Boolean> decisions = new HashMap<>();
    }
}
