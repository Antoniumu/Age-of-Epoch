package com.robertx22.world_of_exile.world_gen;

import com.robertx22.age_of_exile.mmorpg.registers.common.SlashBlocks;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.interfaces.data_items.IRarity;
import com.robertx22.age_of_exile.uncommon.utilityclasses.SignUtils;
import com.robertx22.forge_compat.event.entity.living.LivingDeathEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tower bosses are ordinary mobs with boss rarity, pinned to the roof.
 * Killing one starts {@link TowerCollapse}.
 */
public final class TowerBoss {

    public static final String TAG = "aoe_tower_boss";
    private static final String HOME_PREFIX = "aoe_tower_home:";
    private static final int LEASH = 5;
    private static final int SCAN_RADIUS_CHUNKS = 3;

    private static final Set<BlockPos> SPAWNING = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> COLLAPSED = ConcurrentHashMap.newKeySet();

    private TowerBoss() {
    }

    public static void tickLevel(ServerLevel level) {
        if ((level.getGameTime() & 7L) != 0L) {
            return;
        }
        Map<Long, BlockPos> legacyRoof = new HashMap<>();
        Map<Long, BlockPos> campfires = new HashMap<>();
        Set<Long> markerCells = new HashSet<>();
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) {
                continue;
            }
            int cx = player.chunkPosition().x();
            int cz = player.chunkPosition().z();
            for (int dx = -SCAN_RADIUS_CHUNKS; dx <= SCAN_RADIUS_CHUNKS; dx++) {
                for (int dz = -SCAN_RADIUS_CHUNKS; dz <= SCAN_RADIUS_CHUNKS; dz++) {
                    if (!level.getChunkSource().hasChunk(cx + dx, cz + dz)) {
                        continue;
                    }
                    LevelChunk chunk = level.getChunk(cx + dx, cz + dz);
                    List<BlockPos> positions = new ArrayList<>(chunk.getBlockEntitiesPos());
                    for (BlockPos pos : positions) {
                        BlockEntity be = chunk.getBlockEntity(pos);
                        if (be instanceof TowerBossMarkerBlockEntity marker) {
                            markerCells.add(cell(pos));
                            marker.serverTick();
                        } else if (be instanceof SignBlockEntity sign) {
                            tryConvertBossSign(level, pos, sign);
                        } else if (level.getBlockState(pos).is(Blocks.SOUL_CAMPFIRE) && nearTowerMaterial(level, pos)) {
                            campfires.putIfAbsent(cell(pos), pos);
                        } else if (be != null && isOpenTowerSpawner(level, pos)) {
                            long key = cell(pos);
                            BlockPos current = legacyRoof.get(key);
                            if (current == null || pos.getY() > current.getY()) {
                                legacyRoof.put(key, pos);
                            }
                        }
                    }
                }
            }
        }
        for (Map.Entry<Long, BlockPos> entry : legacyRoof.entrySet()) {
            if (markerCells.contains(entry.getKey())) {
                continue;
            }
            BlockPos pos = entry.getValue();
            if (!bossNearby(level, pos)) {
                level.setBlock(pos, SlashBlocks.TOWER_BOSS_MARKER.get().defaultBlockState(), 3);
                markerCells.add(entry.getKey());
            }
        }
        for (Map.Entry<Long, BlockPos> entry : campfires.entrySet()) {
            if (markerCells.contains(entry.getKey()) || legacyRoof.containsKey(entry.getKey())) {
                continue;
            }
            BlockPos pos = entry.getValue();
            if (!bossNearby(level, pos)) {
                level.setBlock(pos, SlashBlocks.TOWER_BOSS_MARKER.get().defaultBlockState(), 3);
            }
        }
    }

    private static long cell(BlockPos pos) {
        return (((long) (pos.getX() >> 5)) << 32) ^ (pos.getZ() >> 5);
    }

    private static boolean isOpenTowerSpawner(ServerLevel level, BlockPos pos) {
        if (!level.getBlockState(pos).is(Blocks.SPAWNER) || !nearTowerMaterial(level, pos)) {
            return false;
        }
        for (int dy = 1; dy <= 4; dy++) {
            if (TowerCollapse.isTowerMaterial(level.getBlockState(pos.above(dy)))) {
                return false;
            }
        }
        return true;
    }

    public static void trySpawnFromMarker(TowerBossMarkerBlockEntity marker) {
        if (!(marker.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (marker.isRemoved()) {
            return;
        }
        BlockPos pos = marker.getBlockPos();
        if (!level.hasNearbyAlivePlayer(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 42.0D)) {
            return;
        }
        if (bossNearby(level, pos)) {
            dismiss(level, marker);
            return;
        }
        if (!SPAWNING.add(pos)) {
            return;
        }
        boolean restoreCampfire = marker.restoreCampfire;
        try {
            if (bossNearby(level, pos) || otherSpawnInProgress(pos)) {
                dismiss(level, marker);
                return;
            }
            level.removeBlock(pos, false);
            if (spawn(level, pos) != null) {
                tell(level, pos, "Страж башни пробудился.", ChatFormatting.DARK_RED);
            } else if (restoreCampfire) {
                level.setBlock(pos, Blocks.SOUL_CAMPFIRE.defaultBlockState(), 3);
            }
        } finally {
            SPAWNING.remove(pos);
        }
    }

    public static void contain(LivingEntity entity) {
        if (entity.level().isClientSide() || !entity.entityTags().contains(TAG) || !entity.isAlive()) {
            return;
        }
        BlockPos home = readHome(entity);
        if (home == null) {
            return;
        }
        if (entity.isPassenger()) {
            entity.stopRiding();
        }
        if (!entity.level().getBlockState(entity.blockPosition()).isAir() && entity.level() instanceof ServerLevel server) {
            BlockPos air = findAir(server, entity.blockPosition());
            if (!air.equals(entity.blockPosition())) {
                entity.teleportTo(air.getX() + 0.5D, air.getY(), air.getZ() + 0.5D);
                entity.setDeltaMovement(Vec3.ZERO);
            }
        }
        double dx = entity.getX() - (home.getX() + 0.5D);
        double dz = entity.getZ() - (home.getZ() + 0.5D);
        boolean outside = dx * dx + dz * dz > (LEASH * LEASH)
            || entity.getY() < home.getY() - 1.0D
            || entity.getY() > home.getY() + 3.5D;
        if (outside) {
            entity.teleportTo(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D);
            entity.setDeltaMovement(Vec3.ZERO);
            entity.resetFallDistance();
            if (entity instanceof Mob mob) {
                mob.getNavigation().stop();
            }
        }
        if ((entity.tickCount % 40) == 0) {
            entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0, false, false));
            if (entity instanceof Mob mob) {
                mob.setPersistenceRequired();
            }
        }
    }

    public static void onDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntityLiving();
        if (entity == null || !entity.entityTags().contains(TAG)) {
            return;
        }
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        if (!COLLAPSED.add(entity.getUUID())) {
            return;
        }
        entity.removeTag(TAG);
        BlockPos home = readHome(entity);
        if (home == null) {
            home = entity.blockPosition();
        }
        TowerCollapse.start(level, home);
        tell(level, home, "Страж пал. Башня рушится.", ChatFormatting.RED);
    }

    private static LivingEntity spawn(ServerLevel level, BlockPos pos) {
        String mobId = Level.NETHER.equals(level.dimension()) ? "minecraft:wither_skeleton" : "minecraft:vindicator";
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(mobId));
        if (type == null) {
            type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse("minecraft:vindicator"));
        }
        if (type == null) {
            return null;
        }
        LivingEntity entity = (LivingEntity) type.create(level, EntitySpawnReason.MOB_SUMMONED);
        if (entity == null) {
            return null;
        }
        BlockPos stand = findAir(level, pos);
        entity.setPos(stand.getX() + 0.5D, stand.getY(), stand.getZ() + 0.5D);
        entity.addTag(TAG);
        entity.addTag(HOME_PREFIX + stand.getX() + "," + stand.getY() + "," + stand.getZ());
        entity.setCustomName(Component.literal("Страж башни").withStyle(ChatFormatting.DARK_RED));
        entity.setCustomNameVisible(true);
        if (entity instanceof Mob mob) {
            mob.setPersistenceRequired();
            mob.setCanPickUpLoot(false);
            try {
                var follow = mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE);
                if (follow != null) {
                    follow.setBaseValue(LEASH + 2.0D);
                }
            } catch (Exception ignored) {
            }
            try {
                mob.finalizeSpawn(level, level.getCurrentDifficultyAt(stand), EntitySpawnReason.MOB_SUMMONED, null);
            } catch (Exception ignored) {
            }
            try {
                mob.getClass().getMethod("restrictTo", BlockPos.class, int.class).invoke(mob, stand, LEASH);
            } catch (Exception ignored) {
            }
        }
        level.addFreshEntityWithPassengers(entity);
        try {
            var data = Load.Unit(entity);
            if (data != null) {
                data.setRarity(IRarity.BOSS_ID);
                data.forceRecalculateStats();
                entity.setHealth(entity.getMaxHealth());
            }
        } catch (Exception ignored) {
        }
        entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0, false, false));
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
            entity.getX(), entity.getY() + 1.0D, entity.getZ(), 24, 0.4D, 0.6D, 0.4D, 0.02D);
        return entity;
    }

    private static BlockPos findAir(ServerLevel level, BlockPos pos) {
        BlockPos[] candidates = new BlockPos[] {
            pos, pos.above(), pos.north(), pos.south(), pos.east(), pos.west(),
            pos.above().north(), pos.above().south(), pos.above().east(), pos.above().west(),
            pos.below()
        };
        for (BlockPos candidate : candidates) {
            if (level.getBlockState(candidate).isAir() && level.getBlockState(candidate.above()).isAir()) {
                return candidate;
            }
        }
        return pos;
    }

    private static void tryConvertBossSign(ServerLevel level, BlockPos pos, SignBlockEntity sign) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(Blocks.SPRUCE_SIGN) && !state.is(Blocks.SPRUCE_WALL_SIGN)) {
            return;
        }
        try {
            if (!SignUtils.has("[boss]", sign)) {
                return;
            }
        } catch (Exception e) {
            return;
        }
        if (!nearTowerMaterial(level, pos)) {
            return;
        }
        level.setBlock(pos, SlashBlocks.TOWER_BOSS_MARKER.get().defaultBlockState(), 3);
    }

    private static boolean nearTowerMaterial(ServerLevel level, BlockPos pos) {
        int found = 0;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (TowerCollapse.isTowerMaterial(level.getBlockState(pos.offset(dx, dy, dz)))) {
                        found++;
                        if (found >= 6) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private static void dismiss(ServerLevel level, TowerBossMarkerBlockEntity marker) {
        BlockPos pos = marker.getBlockPos();
        if (marker.restoreCampfire) {
            level.setBlock(pos, Blocks.SOUL_CAMPFIRE.defaultBlockState(), 3);
        } else {
            level.removeBlock(pos, false);
        }
    }

    private static boolean bossNearby(ServerLevel level, BlockPos pos) {
        AABB box = new AABB(pos).inflate(48.0D);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (entity.entityTags().contains(TAG) && entity.isAlive()) {
                return true;
            }
        }
        return false;
    }

    private static boolean otherSpawnInProgress(BlockPos pos) {
        for (BlockPos other : SPAWNING) {
            if (!other.equals(pos) && other.distSqr(pos) <= 48 * 48) {
                return true;
            }
        }
        return false;
    }

    static BlockPos readHome(LivingEntity entity) {
        for (String tag : entity.entityTags()) {
            if (!tag.startsWith(HOME_PREFIX)) {
                continue;
            }
            String[] parts = tag.substring(HOME_PREFIX.length()).split(",");
            if (parts.length != 3) {
                continue;
            }
            try {
                return new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private static void tell(ServerLevel level, BlockPos pos, String text, ChatFormatting color) {
        Component message = Component.literal(text).withStyle(color);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D) < 80.0D * 80.0D) {
                player.sendSystemMessage(message);
            }
        }
    }
}
