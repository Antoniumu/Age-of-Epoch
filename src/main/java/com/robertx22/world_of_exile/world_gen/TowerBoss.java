package com.robertx22.world_of_exile.world_gen;

import com.robertx22.age_of_exile.mmorpg.registers.common.SlashBlocks;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.questbook.QuestBookService;
import com.robertx22.age_of_exile.uncommon.interfaces.data_items.IRarity;
import com.robertx22.age_of_exile.uncommon.utilityclasses.SignUtils;
import com.robertx22.forge_compat.event.entity.living.LivingDeathEvent;
import com.robertx22.world_of_exile.tower.TowerAbility;
import com.robertx22.world_of_exile.tower.TowerBossDef;
import com.robertx22.world_of_exile.tower.TowerMaterials;
import com.robertx22.world_of_exile.tower.TowerRoster;
import com.robertx22.world_of_exile.tower.TowerTheme;
import com.robertx22.world_of_exile.tower.TowerThemes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
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
import net.minecraft.util.RandomSource;

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
 * but he cannot leave the tower. Containment never builds blocks under him.
 * Killing him starts {@link TowerCollapse}.
 */
public final class TowerBoss {

    public static final String TAG = "aoe_tower_boss";
    public static final String CHAMPION_TAG = "aoe_tower_champion";
    public static final String MOB_TAG = "aoe_tower_mob";
    private static final String HOME_PREFIX = "aoe_tower_home:";
    public static final String THEME_TAG_PREFIX = "aoe_tower_theme:";
    public static final String DEFINITION_TAG_PREFIX = "aoe_tower_def:";
    private static final String RANK_PREFIX = "aoe_tower_rank:";
    private static final int SCAN_RADIUS_CHUNKS = 3;
    private static final int SAME_TOWER = 48;

    private static final Set<BlockPos> SPAWNING = ConcurrentHashMap.newKeySet();
    private static final Set<Long> SPAWNING_COLUMNS = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> COLLAPSED = ConcurrentHashMap.newKeySet();
    private record TowerKey(ResourceKey<Level> dimension, BlockPos pos) {}

    private static final Map<TowerKey, Long> COLLAPSED_AT = new ConcurrentHashMap<>();
    private static final Map<TowerKey, Arena> ARENAS = new ConcurrentHashMap<>();

    private TowerBoss() {
    }

    public static void tickLevel(ServerLevel level) {
        if ((level.getGameTime() & 7L) != 0L) {
            return;
        }
        List<TowerBossMarkerBlockEntity> markers = new ArrayList<>();
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
                        }
                    }
                }
            }
        }
        keepOneMarkerPerTower(level, markers);
        // Themed vanilla spawners own their spawn cadence and mob selection.
        // Never turn decorative campfires into bosses or pump arbitrary spawners.
        aggroTowerMobs(level);
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

    public static void trySpawnFromMarker(TowerBossMarkerBlockEntity marker) {
        if (!(marker.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (marker.isRemoved() || level.getGameTime() % 20 != 0) {
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
            if (spawn(level, pos, marker.theme) == null) {
                // Keep the encounter retryable when every valid spot is occupied.
                level.setBlock(pos, SlashBlocks.TOWER_BOSS_MARKER.get().defaultBlockState(), 3);
                if (level.getBlockEntity(pos) instanceof TowerBossMarkerBlockEntity retry) {
                    retry.theme = marker.theme;
                    retry.restoreCampfire = restoreCampfire;
                    retry.setChanged();
                }
            }
        } finally {
            SPAWNING.remove(pos);
            SPAWNING_COLUMNS.remove(columnKey);
        }
    }

    public static void contain(LivingEntity entity) {
        if (entity.level().isClientSide() || !entity.isAlive()) {
            return;
        }
        if (entity instanceof Mob mob && (entity.entityTags().contains(TAG) || entity.entityTags().contains(MOB_TAG))) {
            protectFromSun(mob);
        }
        if (!entity.entityTags().contains(TAG) || !(entity.level() instanceof ServerLevel level)) {
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
        boolean outside = entity.getX() < arena.minX + 0.2D || entity.getX() > arena.maxX + 0.8D
            || entity.getZ() < arena.minZ + 0.2D || entity.getZ() > arena.maxZ + 0.8D
            || entity.getY() < arena.minY - 0.2D || entity.getY() > arena.maxY + 3.0D;
        // Normal jumps, knockback, stairs and falls must not create platforms or
        // trigger per-tick teleports. Only rescue an escape, at most once a second.
        if (outside && entity.tickCount % 20 == 0) {
            BlockPos stand = findStand(level, entity, home, arena);
            if (stand != null) {
                entity.teleportTo(stand.getX() + 0.5D, stand.getY(), stand.getZ() + 0.5D);
                entity.setDeltaMovement(Vec3.ZERO);
                entity.resetFallDistance();
                if (entity instanceof Mob mob) {
                    mob.getNavigation().stop();
                }
            }
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
        if (entity instanceof Mob mob && (entity.tickCount % 10) == 0) {
            TowerTheme theme = themeOf(entity);
            if (theme == null) {
                theme = TowerThemes.forDimension(level);
            }
            TowerAbility.tick(level, mob, theme, rankOf(entity));
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
        BlockPos home = readHome(entity);
        if (home == null) {
            home = entity.blockPosition();
        }
        if (event.getSource() != null && event.getSource().getEntity() instanceof LivingEntity killer) {
            QuestBookService.onTowerBossKilled(entity, killer);
        }
        TowerAbility.forget(entity.getUUID());
        if (entity.entityTags().contains(CHAMPION_TAG)) {
            entity.removeTag(TAG);
            tell(level, home, "Прислужник стража пал.", ChatFormatting.YELLOW);
            return;
        }
        if (!COLLAPSED.add(entity.getUUID())) {
            return;
        }
        entity.removeTag(TAG);
        COLLAPSED_AT.put(new TowerKey(level.dimension(), home.immutable()), level.getGameTime());
        TowerCollapse.start(level, home);
        tell(level, home, "Страж пал. Башня рушится.", ChatFormatting.RED);
    }

    /**
     * Spawns the tower's main boss - picked from the theme roster by the rank
     * derived from the tower's height and the biome danger - plus its
     * champions.  Champions are weaker named bosses with their own abilities
     * but do not collapse the tower when they die.
     */
    private static LivingEntity spawn(ServerLevel level, BlockPos pos, String themeId) {
        TowerTheme theme = TowerThemes.exists(themeId) ? TowerThemes.byId(themeId) : TowerThemes.forDimension(level);
        int rank = rankAt(level, pos, theme);
        RandomSource random = level.getRandom();
        TowerBossDef def = TowerRoster.mainBoss(theme.id(), rank, random);
        LivingEntity boss = createBoss(level, pos, pos, def, theme, rank, false);
        if (boss == null) {
            return null;
        }
        int championCount = Math.min(3, Math.max(1, rank - 1));
        int spawnedChampions = 0;
        for (TowerBossDef champion : TowerRoster.champions(theme.id(), rank, championCount, def, random)) {
            BlockPos spot = pos.offset(random.nextInt(9) - 4, random.nextInt(3) - 1, random.nextInt(9) - 4);
            if (createBoss(level, spot, pos, champion, theme, Math.max(1, rank - 1), true) != null) {
                spawnedChampions++;
            }
        }
        String suffix = spawnedChampions > 0 ? " Со свитой из " + spawnedChampions + " прислужников." : "";
        tell(level, pos, "Страж «" + theme.name() + "» пробудился: " + def.name() + "." + suffix,
            rank >= 3 ? ChatFormatting.DARK_RED : ChatFormatting.GOLD);
        return boss;
    }

    private static LivingEntity createBoss(ServerLevel level, BlockPos pos, BlockPos home, TowerBossDef def, TowerTheme theme,
                                           int rank, boolean champion) {
        EntityType<?> type = null;
        try {
            type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(def.entity()));
        } catch (Exception ignored) {
        }
        if (type == null) {
            type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(
                Level.NETHER.equals(level.dimension()) ? "minecraft:wither_skeleton" : "minecraft:vindicator"));
        }
        if (type == null) {
            return null;
        }
        Entity created = type.create(level, EntitySpawnReason.MOB_SUMMONED);
        if (!(created instanceof LivingEntity entity)) {
            return null;
        }
        // Finalization can change dimensions (e.g. slimes or baby zombies).
        // Check the final bounding box, not the type's default dimensions.
        entity.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        if (entity instanceof Mob mob) {
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.MOB_SUMMONED, null);
        }
        Arena arena = arenaOf(level, home);
        BlockPos stand = findStand(level, entity, pos, arena);
        if (stand == null) {
            return null; // No air-only fallback: never spawn floating or inside blocks.
        }
        entity.setPos(stand.getX() + 0.5D, stand.getY(), stand.getZ() + 0.5D);
        entity.addTag(TAG);
        entity.addTag(HOME_PREFIX + home.getX() + "," + home.getY() + "," + home.getZ());
        entity.addTag(THEME_TAG_PREFIX + theme.id());
        entity.addTag(DEFINITION_TAG_PREFIX + def.id());
        entity.addTag(RANK_PREFIX + rank);
        if (champion) {
            entity.addTag(CHAMPION_TAG);
        }
        ChatFormatting colour = rank >= 4 ? ChatFormatting.DARK_PURPLE
            : rank == 3 ? ChatFormatting.DARK_RED
            : rank == 2 ? ChatFormatting.GOLD
            : ChatFormatting.YELLOW;
        String title = champion ? def.name() + " (прислужник)" : def.name();
        entity.setCustomName(Component.literal(title).withStyle(colour));
        entity.setCustomNameVisible(true);
        if (entity instanceof Mob mob) {
            mob.setPersistenceRequired();
            mob.setCanPickUpLoot(false);
            equip(mob, def);
            arm(mob);
            Player target = nearestFighter(level, entity, arena);
            if (target != null) {
                mob.setTarget(target);
                mob.setAggressive(true);
            }
        }
        level.addFreshEntityWithPassengers(entity);
        // Множители дефа (HP/урон) применяются ДО пересчёта статов: HP мода
        // считается от ванильного максимума, и при старом порядке (сначала
        // пересчёт, потом scale) таблица HP «прыгала» после первого удара —
        // см. BALANCE-AUDIT §9.3.
        scale(entity, def, rank, champion);
        try {
            var data = Load.Unit(entity);
            if (data != null) {
                data.setLevel(Math.max(data.getLevel(), rank + 2));
                data.setRarity(IRarity.BOSS_ID);
                data.forceRecalculateStats();
            }
        } catch (Exception ignored) {
        }
        entity.setHealth(entity.getMaxHealth());
        entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0, false, false));
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
            entity.getX(), entity.getY() + 1.0D, entity.getZ(), 24, 0.4D, 0.6D, 0.4D, 0.02D);
        return entity;
    }

    /** Applies the roster multipliers on top of the vanilla mob attributes. */
    private static void scale(LivingEntity entity, TowerBossDef def, int rank, boolean champion) {
        double rankBonus = 1.0D + (rank - 1) * 0.12D;
        double championPenalty = champion ? 0.55D : 1.0D;
        try {
            var health = entity.getAttribute(Attributes.MAX_HEALTH);
            if (health != null) {
                health.setBaseValue(health.getBaseValue() * def.health() * rankBonus * championPenalty);
            }
            var damage = entity.getAttribute(Attributes.ATTACK_DAMAGE);
            if (damage != null) {
                damage.setBaseValue(Math.max(damage.getBaseValue(), 4.0D) * def.damage()
                    * (1.0D + (rank - 1) * 0.08D) * championPenalty);
            }
            var speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) {
                speed.setBaseValue(speed.getBaseValue() * Math.max(0.6D, def.speed()));
            }
            var armor = entity.getAttribute(Attributes.ARMOR);
            if (armor != null) {
                armor.setBaseValue(armor.getBaseValue() + def.armor() * championPenalty);
            }
            var knockback = entity.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
            if (knockback != null) {
                knockback.setBaseValue(Math.min(0.9D, 0.15D + rank * 0.12D));
            }
            var follow = entity.getAttribute(Attributes.FOLLOW_RANGE);
            if (follow != null) {
                follow.setBaseValue(48.0D);
            }
        } catch (Exception ignored) {
        }
    }

    private static void equip(Mob mob, TowerBossDef def) {
        if (def.weapon() == null || def.weapon().isEmpty()) {
            return;
        }
        try {
            var item = BuiltInRegistries.ITEM.getValue(Identifier.parse(def.weapon()));
            if (item != null) {
                mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(item));
                mob.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
            }
        } catch (Exception ignored) {
        }
    }

    private static int rankAt(ServerLevel level, BlockPos pos, TowerTheme theme) {
        int height = towerHeight(level, pos);
        int bonus = height >= 50 ? 2 : height >= 38 ? 1 : 0;
        int rank = (theme.baseRank() - 1) + bonus;
        return Math.max(1, Math.min(4, rank));
    }

    /** Height of the structure the boss was caged in, used for its rank. */
    public static int towerHeight(ServerLevel level, BlockPos pos) {
        int lowest = pos.getY();
        int gap = 0;
        int limit = Math.max(level.getMinY(), pos.getY() - 140);
        for (int y = pos.getY(); y > limit; y--) {
            BlockState state = level.getBlockState(new BlockPos(pos.getX(), y, pos.getZ()));
            if (TowerCollapse.isTowerMaterial(state)) {
                lowest = y;
                gap = 0;
            } else if (state.isAir()) {
                gap++;
                if (gap > 6) {
                    break;
                }
            } else {
                break;
            }
        }
        return pos.getY() - lowest;
    }

    public static TowerTheme themeOf(LivingEntity entity) {
        for (String tag : entity.entityTags()) {
            if (tag.startsWith(THEME_TAG_PREFIX)) {
                return TowerThemes.byId(tag.substring(THEME_TAG_PREFIX.length()));
            }
        }
        return null;
    }

    private static int rankOf(LivingEntity entity) {
        for (String tag : entity.entityTags()) {
            if (tag.startsWith(RANK_PREFIX)) {
                try {
                    return Integer.parseInt(tag.substring(RANK_PREFIX.length()));
                } catch (NumberFormatException ignored) {
                    return 1;
                }
            }
        }
        return 1;
    }

    /** Persist the selected roster entry so reloaded bosses keep only their own abilities. */
    public static List<TowerAbility> abilitiesOf(LivingEntity entity) {
        TowerTheme theme = themeOf(entity);
        if (theme == null) {
            return List.of();
        }
        for (TowerBossDef def : TowerRoster.all(theme.id())) {
            if (entity.entityTags().contains(DEFINITION_TAG_PREFIX + def.id())) {
                return def.abilities();
            }
        }
        // Migrate named bosses already present in saves from before the def tag.
        Component name = entity.getCustomName();
        if (name != null) {
            for (TowerBossDef def : TowerRoster.all(theme.id())) {
                if (name.getString().equals(def.name()) || name.getString().equals(def.name() + " (прислужник)")) {
                    entity.addTag(DEFINITION_TAG_PREFIX + def.id());
                    return def.abilities();
                }
            }
        }
        return List.of();
    }

    /** BLINK requires existing ground and a destination clear of walls. */
    public static boolean blink(ServerLevel level, Mob boss, BlockPos desired) {
        BlockPos home = readHome(boss);
        if (home == null) {
            return false;
        }
        Arena arena = arenaOf(level, home);
        if (!arena.contains(desired)) {
            return false;
        }
        BlockPos stand = findStand(level, boss, desired, arena);
        if (stand == null) {
            return false;
        }
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,
            boss.getX(), boss.getY() + 1.0D, boss.getZ(), 24, 0.3D, 0.6D, 0.3D, 0.05D);
        boss.teleportTo(stand.getX() + 0.5D, stand.getY(), stand.getZ() + 0.5D);
        boss.setDeltaMovement(Vec3.ZERO);
        boss.resetFallDistance();
        boss.getNavigation().stop();
        return true;
    }

    /** Called by {@link TowerAbility#SUMMON_MINIONS}. */
    public static LivingEntity summonMinion(ServerLevel level, LivingEntity boss, TowerTheme theme) {
        if (level.getEntitiesOfClass(Mob.class, boss.getBoundingBox().inflate(48.0D),
            mob -> mob.isAlive() && mob.entityTags().contains(MOB_TAG)).size() >= 12) {
            return null;
        }
        String mobId = theme.randomMob(level.getRandom());
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(mobId));
        if (type == null) {
            return null;
        }
        Entity created = type.create(level, EntitySpawnReason.MOB_SUMMONED);
        if (!(created instanceof Mob mob)) {
            return null;
        }
        RandomSource random = level.getRandom();
        BlockPos home = readHome(boss);
        if (home == null) {
            home = boss.blockPosition();
        }
        BlockPos stand = findStand(level, mob,
            boss.blockPosition().offset(random.nextInt(7) - 3, 0, random.nextInt(7) - 3), arenaOf(level, home));
        if (stand == null) {
            return null;
        }
        mob.setPos(stand.getX() + 0.5D, stand.getY(), stand.getZ() + 0.5D);
        mob.addTag(MOB_TAG);
        mob.setPersistenceRequired();
        arm(mob);
        level.addFreshEntity(mob);
        Player target = nearestPlayer(level, stand, 24.0D);
        if (target != null) {
            mob.setTarget(target);
            mob.setAggressive(true);
        }
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,
            mob.getX(), mob.getY() + 0.6D, mob.getZ(), 12, 0.3D, 0.3D, 0.3D, 0.02D);
        return mob;
    }

    /** Called by {@link TowerAbility#SUMMON_ELITE}. */
    public static LivingEntity summonChampion(ServerLevel level, LivingEntity boss, TowerTheme theme, int rank) {
        // Champions cannot recursively summon more champions.
        if (boss.entityTags().contains(CHAMPION_TAG)
            || level.getEntitiesOfClass(Mob.class, boss.getBoundingBox().inflate(48.0D),
                mob -> mob.isAlive() && mob.entityTags().contains(CHAMPION_TAG)).size() >= 3) {
            return null;
        }
        TowerBossDef def = TowerRoster.mainBoss(theme.id(), Math.max(1, rank), level.getRandom());
        BlockPos home = readHome(boss);
        return createBoss(level, boss.blockPosition(), home != null ? home : boss.blockPosition(),
            def, theme, Math.max(1, rank), true);
    }

    /** Find existing ground and clearance for the actual entity, including wide/tall bosses. */
    private static BlockPos findStand(ServerLevel level, LivingEntity entity, BlockPos from, Arena arena) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (int dy = -6; dy <= 4; dy++) {
            for (int dx = -8; dx <= 8; dx++) {
                for (int dz = -8; dz <= 8; dz++) {
                    BlockPos candidate = from.offset(dx, dy, dz);
                    double dist = candidate.distSqr(from);
                    if (dist >= bestDist || !arena.contains(candidate)) {
                        continue;
                    }
                    AABB box = entity.getBoundingBox().move(
                        candidate.getX() + 0.5D - entity.getX(), candidate.getY() - entity.getY(),
                        candidate.getZ() + 0.5D - entity.getZ());
                    if (box.minX < arena.minX || box.maxX > arena.maxX + 1
                        || box.minZ < arena.minZ || box.maxZ > arena.maxZ + 1
                        || box.maxY > arena.maxY + 3) {
                        continue;
                    }
                    boolean supported = true;
                    // Require a full, dry floor across the footprint, not a torch,
                    // ladder, fluid or a block under just one corner of a large mob.
                    for (int x = (int) Math.floor(box.minX); x <= (int) Math.floor(box.maxX - 0.001D); x++) {
                        for (int z = (int) Math.floor(box.minZ); z <= (int) Math.floor(box.maxZ - 0.001D); z++) {
                            BlockPos floor = new BlockPos(x, candidate.getY() - 1, z);
                            if (!level.getChunkSource().hasChunk(x >> 4, z >> 4)
                                || !level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)
                                || !level.getFluidState(floor.above()).isEmpty()
                                || !level.getFluidState(floor.above(2)).isEmpty()
                                || level.getBlockState(floor).is(Blocks.MAGMA_BLOCK)
                                || level.getBlockState(floor).is(Blocks.CAMPFIRE)
                                || level.getBlockState(floor).is(Blocks.SOUL_CAMPFIRE)) {
                                supported = false;
                                break;
                            }
                        }
                        if (!supported) {
                            break;
                        }
                    }
                    if (!supported || !level.noCollision(box)
                        || !level.getEntitiesOfClass(LivingEntity.class, box,
                            other -> other != entity && other.isAlive()).isEmpty()) {
                        continue;
                    }
                    best = candidate;
                    bestDist = dist;
                }
            }
        }
        return best;
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
                    if (TowerMaterials.isTowerSignature(level.getBlockState(pos.offset(dx, dy, dz)))) {
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
            Map.Entry<TowerKey, Long> entry = iterator.next();
            if (!entry.getKey().dimension().equals(level.dimension())) {
                continue;
            }
            if (now - entry.getValue() > 20L * 60L * 15L) {
                iterator.remove();
                continue;
            }
            double dx = entry.getKey().pos().getX() - pos.getX();
            double dz = entry.getKey().pos().getZ() - pos.getZ();
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
                if (!mob.entityTags().contains(MOB_TAG)) {
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

    /** Vanilla undead do not ignite in daylight with head equipment. Repair the
     * encounter helmet so sunlight cannot wear it out; this is NOT fire immunity. */
    private static void protectFromSun(Mob mob) {
        ItemStack head = mob.getItemBySlot(EquipmentSlot.HEAD);
        if (head.isEmpty()) {
            mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            mob.setDropChance(EquipmentSlot.HEAD, 0.0F);
            // Also migrate already-spawned, burning tower defenders on first equip.
            mob.setRemainingFireTicks(0);
        } else if (head.isDamageableItem() && head.getDamageValue() > 0) {
            head.setDamageValue(0);
        }
    }

    private static void arm(Mob mob) {
        protectFromSun(mob);
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

    private static Arena arenaOf(ServerLevel level, BlockPos home) {
        TowerKey key = new TowerKey(level.dimension(), home.immutable());
        Arena cached = ARENAS.get(key);
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
        int lowest = home.getY() - Math.min(120, towerHeight(level, home) + 6);
        while (!queue.isEmpty() && found < 8000) {
            BlockPos cursor = queue.removeFirst();
            if (!seen.add(cursor)) {
                continue;
            }
            if (Math.abs(cursor.getX() - home.getX()) > 20 || Math.abs(cursor.getZ() - home.getZ()) > 20) {
                continue;
            }
            if (cursor.getY() < lowest || cursor.getY() > home.getY() + 24) {
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
        ARENAS.put(key, arena);
        if (ARENAS.size() > 256) {
            long now = level.getGameTime();
            ARENAS.entrySet().removeIf(entry -> entry.getValue().expires < now);
        }
        return arena;
    }

    private static boolean solid(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.isAir() && !state.getCollisionShape(level, pos).isEmpty();
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
