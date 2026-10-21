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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One tower guardian per structure. He can walk the interior and attack,
 * but he cannot leave the tower, and the floor under him cannot be dug out.
 * Killing him starts {@link TowerCollapse}.
 */
public final class TowerBoss {

    public static final String TAG = "aoe_tower_boss";
    public static final String MOB_TAG = "aoe_tower_mob";
    private static final String HOME_PREFIX = "aoe_tower_home:";
    private static final int SCAN_RADIUS_CHUNKS = 3;
    private static final int SAME_TOWER = 48;

    private static final Set<BlockPos> SPAWNING = ConcurrentHashMap.newKeySet();
    private static final Set<Long> SPAWNING_COLUMNS = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> COLLAPSED = ConcurrentHashMap.newKeySet();
    private static final Map<BlockPos, Long> COLLAPSED_AT = new ConcurrentHashMap<>();
    private static final Map<BlockPos, Long> SPAWNER_NEXT = new ConcurrentHashMap<>();
    private static final Map<BlockPos, Arena> ARENAS = new ConcurrentHashMap<>();

    private TowerBoss() {
    }

    public static void tickLevel(ServerLevel level) {
        if ((level.getGameTime() & 7L) != 0L) {
            return;
        }
        List<TowerBossMarkerBlockEntity> markers = new ArrayList<>();
        List<BlockPos> campfires = new ArrayList<>();
        List<BlockPos> openSpawners = new ArrayList<>();
        List<BlockPos> towerSpawners = new ArrayList<>();
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
                            markers.add(marker);
                        } else if (be instanceof SignBlockEntity sign) {
                            tryConvertBossSign(level, pos, sign);
                        } else if (level.getBlockState(pos).is(Blocks.SOUL_CAMPFIRE) && nearTowerMaterial(level, pos)) {
                            campfires.add(pos);
                        } else if (be != null && level.getBlockState(pos).is(Blocks.SPAWNER) && nearTowerMaterial(level, pos)) {
                            towerSpawners.add(pos);
                            if (isOpenTowerSpawner(level, pos)) {
                                openSpawners.add(pos);
                            }
                        }
                    }
                }
            }
        }
        keepOneMarkerPerTower(level, markers);
        List<BlockPos> liveMarkers = new ArrayList<>();
        for (TowerBossMarkerBlockEntity marker : markers) {
            if (!marker.isRemoved() && level.getBlockState(marker.getBlockPos()).is(SlashBlocks.TOWER_BOSS_MARKER.get())) {
                liveMarkers.add(marker.getBlockPos());
            }
        }
        placeOneFallbackMarker(level, openSpawners, campfires, liveMarkers);
        aggroTowerMobs(level);
        pumpTowerSpawners(level, towerSpawners);
    }

    private static void keepOneMarkerPerTower(ServerLevel level, List<TowerBossMarkerBlockEntity> markers) {
        Set<BlockPos> seen = new HashSet<>();
        markers.removeIf(marker -> marker.isRemoved() || !seen.add(marker.getBlockPos()));
        List<BlockPos> kept = new ArrayList<>();
        markers.sort((a, b) -> Integer.compare(b.getBlockPos().getY(), a.getBlockPos().getY()));
        for (TowerBossMarkerBlockEntity marker : markers) {
            if (marker.isRemoved()) {
                continue;
            }
            BlockPos pos = marker.getBlockPos();
            if (collapsedNear(level, pos) || bossInRange(level, pos) || nearAny(kept, pos)) {
                dismiss(level, marker);
                continue;
            }
            kept.add(pos);
            marker.serverTick();
        }
    }

    private static void placeOneFallbackMarker(ServerLevel level, List<BlockPos> openSpawners, List<BlockPos> campfires, List<BlockPos> liveMarkers) {
        List<BlockPos> placed = new ArrayList<>(liveMarkers);
        List<BlockPos> candidates = new ArrayList<>(openSpawners);
        candidates.addAll(campfires);
        candidates.sort((a, b) -> Integer.compare(b.getY(), a.getY()));
        for (BlockPos pos : candidates) {
            if (!level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
                continue;
            }
            if (collapsedNear(level, pos) || bossInRange(level, pos) || nearAny(placed, pos)) {
                continue;
            }
            boolean campfire = level.getBlockState(pos).is(Blocks.SOUL_CAMPFIRE);
            level.setBlock(pos, SlashBlocks.TOWER_BOSS_MARKER.get().defaultBlockState(), 3);
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TowerBossMarkerBlockEntity marker) {
                marker.restoreCampfire = campfire;
            }
            placed.add(pos);
        }
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
        if (collapsedNear(level, pos) || bossInRange(level, pos) || spawnBusy(pos)) {
            dismiss(level, marker);
            return;
        }
        long columnKey = column(pos);
        if (!SPAWNING.add(pos)) {
            return;
        }
        if (!SPAWNING_COLUMNS.add(columnKey)) {
            SPAWNING.remove(pos);
            return;
        }
        boolean restoreCampfire = marker.restoreCampfire;
        try {
            if (collapsedNear(level, pos) || bossInRange(level, pos)) {
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
            SPAWNING_COLUMNS.remove(columnKey);
        }
    }

    public static void contain(LivingEntity entity) {
        if (entity.level().isClientSide() || !entity.entityTags().contains(TAG) || !entity.isAlive()) {
            return;
        }
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        BlockPos home = readHome(entity);
        if (home == null) {
            return;
        }
        if (entity.isPassenger()) {
            entity.stopRiding();
        }
        Arena arena = arenaOf(level, home);
        BlockPos below = entity.blockPosition().below();
        boolean hole = arena.contains(below) && level.getBlockState(below).isAir();
        boolean outside = entity.getX() < arena.minX + 0.2D || entity.getX() > arena.maxX + 0.8D
            || entity.getZ() < arena.minZ + 0.2D || entity.getZ() > arena.maxZ + 0.8D
            || entity.getY() < arena.minY - 0.2D || entity.getY() > arena.maxY + 3.0D;
        boolean falling = entity.getDeltaMovement().y < -0.35D && !solid(level, below);
        if (outside || falling) {
            if (hole) {
                level.setBlock(below, supportBlock(level, below), 3);
            }
            BlockPos stand = nearestStand(level, arena, entity.blockPosition());
            entity.teleportTo(stand.getX() + 0.5D, stand.getY(), stand.getZ() + 0.5D);
            entity.setDeltaMovement(Vec3.ZERO);
            entity.resetFallDistance();
            if (entity instanceof Mob mob) {
                mob.getNavigation().stop();
            }
        } else if (hole && entity.getDeltaMovement().y <= 0.08D) {
            level.setBlock(below, supportBlock(level, below), 3);
        }
        if (entity instanceof Mob mob) {
            if ((entity.tickCount % 10) == 0) {
                arm(mob);
            }
            Player target = nearestFighter(level, entity, arena);
            if (target != null && (!(mob.getTarget() instanceof Player living) || !living.isAlive() || !canBeHunted(living) || !arena.contains(living.blockPosition()))) {
                mob.setTarget(target);
                mob.setAggressive(true);
            }
        }
        if ((entity.tickCount % 40) == 0) {
            entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0, false, false));
            if (entity instanceof Mob mob) {
                mob.setPersistenceRequired();
            }
        }
    }

    public static boolean denyBreak(ServerLevel level, BlockPos pos, Player player) {
        if (player.isCreative() || player.isSpectator()) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.is(Blocks.CHEST) || state.is(Blocks.TRAPPED_CHEST)
            || state.is(Blocks.BARREL) || state.is(Blocks.SPAWNER)) {
            return false;
        }
        if (!solid(level, pos) && !TowerCollapse.isTowerMaterial(state)) {
            return false;
        }
        AABB box = new AABB(pos).inflate(SAME_TOWER, 120.0D, SAME_TOWER);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e.entityTags().contains(TAG))) {
            BlockPos home = readHome(entity);
            if (home == null) {
                home = entity.blockPosition();
            }
            Arena arena = arenaOf(level, home);
            if (!arena.contains(pos) || pos.getY() > entity.blockPosition().getY()) {
                continue;
            }
            boolean floor = !solid(level, pos.above());
            boolean underFeet = Math.abs(pos.getX() - entity.blockPosition().getX()) <= 2
                && Math.abs(pos.getZ() - entity.blockPosition().getZ()) <= 2
                && pos.getY() >= entity.blockPosition().getY() - 3;
            if (floor || underFeet) {
                return true;
            }
        }
        return false;
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
        COLLAPSED_AT.put(home.immutable(), level.getGameTime());
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
                mob.finalizeSpawn(level, level.getCurrentDifficultyAt(stand), EntitySpawnReason.MOB_SUMMONED, null);
            } catch (Exception ignored) {
            }
            arm(mob);
            Player target = nearestFighter(level, entity, arenaOf(level, stand));
            if (target != null) {
                mob.setTarget(target);
                mob.setAggressive(true);
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

    private static long column(BlockPos pos) {
        return (((long) (pos.getX() >> 5)) << 32) ^ (pos.getZ() >> 5);
    }

    private static boolean columnBusy(BlockPos pos) {
        int cx = pos.getX() >> 5;
        int cz = pos.getZ() >> 5;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                long key = (((long) (cx + dx)) << 32) ^ (cz + dz);
                if (SPAWNING_COLUMNS.contains(key)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean spawnBusy(BlockPos pos) {
        if (columnBusy(pos)) {
            return true;
        }
        for (BlockPos other : SPAWNING) {
            if (other.equals(pos)) {
                continue;
            }
            double dx = other.getX() - pos.getX();
            double dz = other.getZ() - pos.getZ();
            if (dx * dx + dz * dz <= (double) SAME_TOWER * SAME_TOWER) {
                return true;
            }
        }
        return false;
    }

    private static boolean bossInRange(ServerLevel level, BlockPos pos) {
        AABB box = new AABB(
            pos.getX() - SAME_TOWER, level.getMinY(), pos.getZ() - SAME_TOWER,
            pos.getX() + SAME_TOWER + 1, level.getMaxY(), pos.getZ() + SAME_TOWER + 1);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (entity.isAlive() && entity.entityTags().contains(TAG)) {
                return true;
            }
        }
        return false;
    }

    private static boolean collapsedNear(ServerLevel level, BlockPos pos) {
        long now = level.getGameTime();
        boolean hit = false;
        var iterator = COLLAPSED_AT.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Long> entry = iterator.next();
            if (now - entry.getValue() > 20L * 60L * 15L) {
                iterator.remove();
                continue;
            }
            double dx = entry.getKey().getX() - pos.getX();
            double dz = entry.getKey().getZ() - pos.getZ();
            if (dx * dx + dz * dz <= (double) SAME_TOWER * SAME_TOWER) {
                hit = true;
            }
        }
        return hit;
    }

    private static boolean nearAny(List<BlockPos> positions, BlockPos pos) {
        for (BlockPos other : positions) {
            double dx = other.getX() - pos.getX();
            double dz = other.getZ() - pos.getZ();
            if (dx * dx + dz * dz <= (double) SAME_TOWER * SAME_TOWER) {
                return true;
            }
        }
        return false;
    }

    private static void aggroTowerMobs(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            if (!canBeHunted(player)) {
                continue;
            }
            AABB box = player.getBoundingBox().inflate(28.0D);
            for (Mob mob : level.getEntitiesOfClass(Mob.class, box, m -> m.isAlive() && !m.entityTags().contains(TAG))) {
                if (mob.getType().getCategory() != MobCategory.MONSTER && !mob.entityTags().contains(MOB_TAG)) {
                    continue;
                }
                if (!mob.entityTags().contains(MOB_TAG) && !inTower(level, mob.blockPosition())) {
                    continue;
                }
                if (mob.getTarget() instanceof Player target && target.isAlive() && canBeHunted(target)) {
                    continue;
                }
                mob.setTarget(player);
                mob.setAggressive(true);
                mob.setPersistenceRequired();
                arm(mob);
            }
        }
    }

    private static void pumpTowerSpawners(ServerLevel level, List<BlockPos> spawners) {
        long now = level.getGameTime();
        for (BlockPos pos : spawners) {
            if (!level.hasNearbyAlivePlayer(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 20.0D)) {
                continue;
            }
            Long next = SPAWNER_NEXT.get(pos);
            if (next != null && now < next) {
                continue;
            }
            AABB box = new AABB(pos).inflate(10.0D);
            int hostiles = level.getEntitiesOfClass(Mob.class, box, m -> m.isAlive() && m.getType().getCategory() == MobCategory.MONSTER).size();
            if (hostiles >= 4) {
                SPAWNER_NEXT.put(pos.immutable(), now + 80L);
                continue;
            }
            LivingEntity spawned = spawnTowerMob(level, pos);
            SPAWNER_NEXT.put(pos.immutable(), now + (spawned == null ? 40L : 160L));
        }
        if (SPAWNER_NEXT.size() > 512) {
            SPAWNER_NEXT.entrySet().removeIf(entry -> now - entry.getValue() > 20L * 60L);
        }
    }

    private static LivingEntity spawnTowerMob(ServerLevel level, BlockPos pos) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(mobFor(level)));
        if (type == null) {
            return null;
        }
        Entity created = type.create(level, EntitySpawnReason.SPAWNER);
        if (!(created instanceof Mob mob)) {
            return null;
        }
        BlockPos stand = findAir(level, pos.above());
        mob.setPos(stand.getX() + 0.5D, stand.getY(), stand.getZ() + 0.5D);
        mob.addTag(MOB_TAG);
        mob.setPersistenceRequired();
        try {
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(stand), EntitySpawnReason.SPAWNER, null);
        } catch (Exception ignored) {
        }
        arm(mob);
        level.addFreshEntity(mob);
        Player target = nearestPlayer(level, stand, 28.0D);
        if (target != null) {
            mob.setTarget(target);
            mob.setAggressive(true);
        }
        return mob;
    }

    private static String mobFor(ServerLevel level) {
        if (Level.NETHER.equals(level.dimension())) {
            return level.getRandom().nextBoolean() ? "minecraft:wither_skeleton" : "minecraft:blaze";
        }
        String[] mobs = {
            "minecraft:zombie", "minecraft:skeleton", "minecraft:vindicator",
            "minecraft:pillager", "minecraft:witch", "minecraft:cave_spider"
        };
        return mobs[level.getRandom().nextInt(mobs.length)];
    }

    private static void arm(Mob mob) {
        try {
            if (mob.getMainHandItem().isEmpty() && mob.entityTags().contains(TAG)) {
                mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
                mob.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
            }
            var follow = mob.getAttribute(Attributes.FOLLOW_RANGE);
            if (follow != null && follow.getBaseValue() < 32.0D) {
                follow.setBaseValue(32.0D);
            }
            double damageFloor = mob.entityTags().contains(TAG) ? 8.0D : 6.0D;
            var damage = mob.getAttribute(Attributes.ATTACK_DAMAGE);
            if (damage != null && damage.getBaseValue() < damageFloor) {
                damage.setBaseValue(damageFloor);
            }
        } catch (Exception ignored) {
        }
    }

    private static Player nearestFighter(ServerLevel level, LivingEntity from, Arena arena) {
        Player best = null;
        double bestDist = 32.0D * 32.0D;
        for (ServerPlayer player : level.players()) {
            if (!canBeHunted(player) || !arena.contains(player.blockPosition())) {
                continue;
            }
            double dist = player.distanceToSqr(from);
            if (dist < bestDist) {
                bestDist = dist;
                best = player;
            }
        }
        return best;
    }

    private static Player nearestPlayer(ServerLevel level, BlockPos pos, double range) {
        Player best = null;
        double bestDist = range * range;
        for (ServerPlayer player : level.players()) {
            if (!canBeHunted(player)) {
                continue;
            }
            double dist = player.distanceToSqr(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
            if (dist < bestDist) {
                bestDist = dist;
                best = player;
            }
        }
        return best;
    }

    private static boolean canBeHunted(Player player) {
        return player != null && player.isAlive() && !player.isSpectator() && !player.isCreative();
    }

    private static boolean inTower(ServerLevel level, BlockPos pos) {
        if (nearTowerMaterial(level, pos) || TowerCollapse.isTowerMaterial(level.getBlockState(pos.below()))) {
            return true;
        }
        for (int dx = -6; dx <= 6; dx += 3) {
            for (int dz = -6; dz <= 6; dz += 3) {
                if (TowerCollapse.isTowerMaterial(level.getBlockState(pos.offset(dx, -1, dz)))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Arena arenaOf(ServerLevel level, BlockPos home) {
        Arena cached = ARENAS.get(home);
        if (cached != null && level.getGameTime() < cached.expires) {
            return cached;
        }
        int minX = home.getX();
        int maxX = home.getX();
        int minY = home.getY();
        int maxY = home.getY();
        int minZ = home.getZ();
        int maxZ = home.getZ();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        queue.add(home);
        int found = 0;
        while (!queue.isEmpty() && found < 8000) {
            BlockPos cursor = queue.removeFirst();
            if (!seen.add(cursor)) {
                continue;
            }
            if (Math.abs(cursor.getX() - home.getX()) > 40 || Math.abs(cursor.getZ() - home.getZ()) > 40) {
                continue;
            }
            if (cursor.getY() < home.getY() - 120 || cursor.getY() > home.getY() + 24) {
                continue;
            }
            if (!cursor.equals(home) && !TowerCollapse.isTowerMaterial(level.getBlockState(cursor))) {
                continue;
            }
            found++;
            minX = Math.min(minX, cursor.getX());
            maxX = Math.max(maxX, cursor.getX());
            minY = Math.min(minY, cursor.getY());
            maxY = Math.max(maxY, cursor.getY());
            minZ = Math.min(minZ, cursor.getZ());
            maxZ = Math.max(maxZ, cursor.getZ());
            queue.add(cursor.above());
            queue.add(cursor.below());
            queue.add(cursor.north());
            queue.add(cursor.south());
            queue.add(cursor.east());
            queue.add(cursor.west());
        }
        if (found < 12) {
            minX = home.getX() - 8;
            maxX = home.getX() + 8;
            minZ = home.getZ() - 8;
            maxZ = home.getZ() + 8;
            minY = home.getY() - 24;
            maxY = home.getY() + 4;
        }
        Arena arena = new Arena(minX, maxX, minY, maxY, minZ, maxZ, level.getGameTime() + 200L);
        ARENAS.put(home, arena);
        if (ARENAS.size() > 256) {
            long now = level.getGameTime();
            ARENAS.entrySet().removeIf(entry -> entry.getValue().expires < now);
        }
        return arena;
    }

    private static BlockPos nearestStand(ServerLevel level, Arena arena, BlockPos from) {
        int x = Math.max(arena.minX, Math.min(arena.maxX, from.getX()));
        int y = Math.max(arena.minY, Math.min(arena.maxY, from.getY()));
        int z = Math.max(arena.minZ, Math.min(arena.maxZ, from.getZ()));
        BlockPos clamped = new BlockPos(x, y, z);
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (int dy = 3; dy >= -8; dy--) {
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    BlockPos candidate = clamped.offset(dx, dy, dz);
                    if (!arena.contains(candidate) || !arena.contains(candidate.above())) {
                        continue;
                    }
                    if (!level.getBlockState(candidate).isAir() || !level.getBlockState(candidate.above()).isAir()) {
                        continue;
                    }
                    if (!solid(level, candidate.below())) {
                        continue;
                    }
                    double dist = candidate.distSqr(from);
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = candidate;
                    }
                }
            }
        }
        return best != null ? best : findAir(level, clamped);
    }

    private static boolean solid(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.isAir() && !state.getCollisionShape(level, pos).isEmpty();
    }

    private static BlockState supportBlock(ServerLevel level, BlockPos pos) {
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockState state = level.getBlockState(pos.offset(dx, dy, dz));
                    if (state.isAir()) {
                        continue;
                    }
                    Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
                    if (id == null) {
                        continue;
                    }
                    String path = id.getPath();
                    if (path.contains("stone_brick") || path.contains("blackstone") || path.contains("nether_brick") || path.contains("smooth_stone")) {
                        return state.getBlock().defaultBlockState();
                    }
                }
            }
        }
        return Level.NETHER.equals(level.dimension())
            ? Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
            : Blocks.STONE_BRICKS.defaultBlockState();
    }

    private static final class Arena {
        final int minX;
        final int maxX;
        final int minY;
        final int maxY;
        final int minZ;
        final int maxZ;
        final long expires;

        Arena(int minX, int maxX, int minY, int maxY, int minZ, int maxZ, long expires) {
            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
            this.minZ = minZ;
            this.maxZ = maxZ;
            this.expires = expires;
        }

        boolean contains(BlockPos pos) {
            return pos.getX() >= minX && pos.getX() <= maxX
                && pos.getY() >= minY && pos.getY() <= maxY + 2
                && pos.getZ() >= minZ && pos.getZ() <= maxZ;
        }
    }
}
