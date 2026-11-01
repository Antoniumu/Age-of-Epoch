package com.robertx22.world_of_exile.tower;

import com.robertx22.world_of_exile.world_gen.TowerBoss;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Boss abilities. Every ability is a small, self-contained effect with its own
 * cooldown; the roster mixes them up so two towers of the same theme can still
 * play differently (main boss + champions).
 *
 * <p>The newer set (ROOF_SMASH, LEAP, PILLAR_BREAK, GRAPPLE_PULL, LINE_CHARGE,
 * CEILING_BARRAGE, PHASE_ENRAGE) closes the "hole-in-ceiling / pillar-cheese /
 * wall-turret" exploits by letting the boss SMASH the cover, LEAP to the
 * camper, BREAK the pillar, PULL the target, or drop a barrage on anyone who
 * hides out of melee reach.</p>
 */
public enum TowerAbility {

    FIRE_NOVA(180) {
        @Override
        public void run(ServerLevel level, Mob boss, Player target, TowerTheme theme, int rank) {
            hit(level, boss, 7.0D, damage(rank, 3.0D), p -> p.setRemainingFireTicks(80));
            burst(level, boss, ParticleTypes.FLAME, 40);
        }
    },

    FROST_NOVA(170) {
        @Override
        public void run(ServerLevel level, Mob boss, Player target, TowerTheme theme, int rank) {
            hit(level, boss, 7.0D, damage(rank, 2.5D),
                p -> p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20 * 6, 2)));
            burst(level, boss, ParticleTypes.SNOWFLAKE, 40);
        }
    },

    POISON_CLOUD(200) {
        @Override
        public void run(ServerLevel level, Mob boss, Player target, TowerTheme theme, int rank) {
            hit(level, boss, 6.0D, damage(rank, 1.5D),
                p -> p.addEffect(new MobEffectInstance(MobEffects.POISON, 20 * 7, 1)));
            burst(level, boss, ParticleTypes.ITEM_SLIME, 30);
        }
    },

    WITHER_CURSE(220) {
        @Override
        public void run(ServerLevel level, Mob boss, Player target, TowerTheme theme, int rank) {
            hit(level, boss, 6.5D, damage(rank, 2.0D),
                p -> p.addEffect(new MobEffectInstance(MobEffects.WITHER, 20 * 6, 1)));
            burst(level, boss, ParticleTypes.SOUL, 34);
        }
    },

    CURSE_OF_WEAKNESS(240) {
        @Override
        public void run(ServerLevel level, Mob boss, Player target, TowerTheme theme, int rank) {
            hit(level, boss, 8.0D, damage(rank, 1.0D),
                p -> p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 8, 1)));
            burst(level, boss, ParticleTypes.WITCH, 26);
        }
    },

    LEECH(200) {
        @Override
        public void run(ServerLevel level, Mob boss, Player target, TowerTheme theme, int rank) {
            int before = countTargets(level, boss, 7.0D);
            hit(level, boss, 7.0D, damage(rank, 2.0D), null);
            if (before > 0) {
                boss.heal(boss.getMaxHealth() * (0.05F * before));
            }
            burst(level, boss, ParticleTypes.DAMAGE_INDICATOR, 18);
        }
    },

    KNOCKBACK_SLAM(150) {
        @Override
        public void run(ServerLevel level, Mob boss, Player target, TowerTheme theme, int rank) {
            for (Player player : targets(level, boss, 6.0D)) {
                player.hurtServer(level, boss.damageSources().magic(), damage(rank, 2.5D));
                player.knockback(0.9D, boss.getX() - player.getX(), boss.getZ() - player.getZ(),
                    boss.damageSources().generic(), 0.1F);
            }
            burst(level, boss, ParticleTypes.EXPLOSION, 6);
            level.playSound(null, boss.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.HOSTILE, 0.7F, 1.2F);
        }
    },

    THUNDER_STOMP(260) {
        @Override
        public void run(ServerLevel level, Mob boss, Player target, TowerTheme theme, int rank) {
            hit(level, boss, 9.0D, damage(rank, 3.0D),
                p -> p.knockback(0.6D, boss.getX() - p.getX(), boss.getZ() - p.getZ(),
                    boss.damageSources().generic(), 0.0F));
            burst(level, boss, ParticleTypes.ELECTRIC_SPARK, 50);
            level.playSound(null, boss.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER,
                SoundSource.HOSTILE, 0.5F, 1.4F);
        }
    },

    METEOR(280) {
        @Override
        public void run(ServerLevel level, Mob boss, Player target, TowerTheme theme, int rank) {
            if (target == null) {
                return;
            }
            smashAt(level, boss, target.blockPosition(), damage(rank, 3.5D), 2.5D, true);
            level.playSound(null, target.blockPosition(), SoundEvents.FIRECHARGE_USE,
                SoundSource.HOSTILE, 1.0F, 0.7F);
        }
    },

    SUMMON_MINIONS(280) {
        @Override
        public void run(ServerLevel level, Mob boss, Player target, TowerTheme theme, int rank) {
            int count = 1 + Math.min(3, rank);
            for (int i = 0; i < count; i++) {
                com.robertx22.world_of_exile.world_gen.TowerBoss.summonMinion(level, boss, theme);
            }
            burst(level, boss, ParticleTypes.POOF, 24);
        }
    },

    SUMMON_ELITE(420) {
        @Override
        public void run(ServerLevel level, Mob boss, Player target, TowerTheme theme, int rank) {
            com.robertx22.world_of_exile.world_gen.TowerBoss.summonChampion(level, boss, theme, Math.max(1, rank - 1));
            burst(level, boss, ParticleTypes.SOUL_FIRE_FLAME, 30);
        }
    },

    HEAL_SELF(320) {
        @Override
        public void run(ServerLevel level, Mob boss, Player target, TowerTheme theme, int rank) {
            boss.heal(boss.getMaxHealth() * 0.18F);
            boss.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 5, 1));
            burst(level, boss, ParticleTypes.HEART, 12);
        }
    },

    SHIELD(340) {
        @Override
        public void run(ServerLevel level, Mob boss, Player target, TowerTheme theme, int rank) {
            boss.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 20 * 8, 1 + rank / 2));
            boss.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 8, 1));
            burst(level, boss, ParticleTypes.ENCHANT, 26);
        }
    },

    RAGE(220) {
        @Override
        public void run(ServerLevel level, Mob boss, Player target, TowerTheme theme, int rank) {
            if (boss.getHealth() > boss.getMaxHealth() * 0.55F) {
                return;
            }
            boss.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 20 * 10, 1 + rank / 2));
            boss.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 10, 1));
            burst(level, boss, ParticleTypes.ANGRY_VILLAGER, 10);
        }
    },

    BLINK(200) {
        @Override
        public void run(ServerLevel level, Mob boss, Player target, TowerTheme theme, int rank) {
            if (target == null) {
                return;
            }
            for (int attempt = 0; attempt < 10; attempt++) {
                double angle = level.getRandom().nextDouble() * Math.PI * 2.0D;
                double distance = 3.0D + level.getRandom().nextDouble() * 3.0D;
                double x = target.getX() + Math.cos(angle) * distance;
                double z = target.getZ() + Math.sin(angle) * distance;
                double y = target.getY() + (level.getRandom().nextInt(3) - 1);
                BlockPos pos = BlockPos.containing(x, y, z);
                if (TowerBoss.blink(level, boss, pos)) {
                    return;
                }
            }
        }
    },

    // ---------------------------------------------------------------- anti-abuse moves

    /**
     * Boss slams the ceiling above it: breaks a 3x3 ring of roof blocks up to 6
     * blocks overhead and deals AOE damage in that ring. This is the direct
     * punishment for the "dig a hole in the roof and shoot down" cheese — the
     * boss punches a hole back and hits the camper through it. After the smash
     * the boss also LEAPS up onto the roof if a camper stands there.
     */
    ROOF_SMASH(320) {
        @Override
        public void run(ServerLevel level, Mob boss, Player target, TowerTheme theme, int rank) {
            BlockPos center = boss.blockPosition();
            // find the first solid ceiling above the boss
            int roofY = -1;
            for (int y = boss.blockPosition().getY() + 1; y < boss.blockPosition().getY() + 10; y++) {
                BlockPos at = new BlockPos(center.getX(), y, center.getZ());
                BlockState st = level.getBlockState(at);
                if (!st.isAir() && st.getCollisionShape(level, at).isEmpty() == false) {
                    roofY = y;
                    break;
                }
            }
            if (roofY > 0) {
                breakRing(level, new BlockPos(center.getX(), roofY, center.getZ()), 2, true);
                burst(level, boss, ParticleTypes.EXPLOSION, 40);
                level.playSound(null, boss.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                    SoundSource.HOSTILE, 1.2F, 0.6F);
            }
            // Damage anyone in a wide cylinder above the boss (roof campers)
            for (Player p : level.getEntitiesOfClass(Player.class,
                new AABB(center.getX() - 4, boss.getY(), center.getZ() - 4,
                    center.getX() + 5, boss.getY() + 12, center.getZ() + 5),
                TowerAbility::validTarget)) {
                if (p.getY() > boss.getY() + 1.5D) {
                    p.hurtServer(level, boss.damageSources().magic(), damage(rank, 4.5D));
                    p.knockback(0.6D, 0.0D, 1.2D, boss.damageSources().generic(), 0.2F);
                    spawnHitParticles(level, p, ParticleTypes.LAVA);
                }
            }
        }
    },

    /**
     * Boss leaps to the target and slams the ground, causing AOE damage +
     * knockback and breaking any pillar blocks directly under the target
     * (destroying the "tower up and shoot" cheese). Boss physically moves
     * through the air to the target instead of teleporting.
     */
    LEAP(260) {
        @Override
        public void run(ServerLevel level, Mob boss, Player target, TowerTheme theme, int rank) {
            if (target == null) {
                return;
            }
            // Launch the boss into the air toward the target
            Vec3 dir = target.position().subtract(boss.position());
            double dist = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
            if (dist < 1.0D) dist = 1.0D;
            double vx = dir.x / dist * 1.1D;
            double vz = dir.z / dist * 1.1D;
            boss.setDeltaMovement(vx, 0.75D + Math.min(0.5D, dist / 16.0D), vz);
            burst(level, boss, ParticleTypes.CLOUD, 15);
            // queue ground-pound effect on the way down (done via LEAP_LANDING in tick).
            LEAP_LAND_TIME.put(boss.getUUID(), level.getGameTime() + Math.max(8L, (long) (dist * 0.8D)));
        }
    },

    /**
     * A focused pull: if the target is higher than the boss or more than 5
     * blocks away behind cover, yank them toward the boss (sets velocity).
     * This pulls roof-campers off their ledge and yanks pillar-archers into
     * melee.
     */
    GRAPPLE_PULL(240) {
        @Override
        public void run(ServerLevel level, Mob boss, Player target, TowerTheme theme, int rank) {
            if (target == null) {
                return;
            }
            Vec3 to = boss.position().subtract(target.position()).normalize().scale(1.6D);
            // yank upward a little first so the target clears the pillar edge
            target.setDeltaMovement(to.x, Math.max(to.y, 0.6D), to.z);
// velocity set via setDeltaMovement; server syncs on next tick.
            // damage scales with how far the camper was
            double dy = target.getY() - boss.getY();
            if (dy > 2.0D) {
                target.hurtServer(level, boss.damageSources().magic(), damage(rank, 2.0D));
            }
            // chain-particles from boss to target
            for (int i = 1; i <= 8; i++) {
                double t = i / 8.0D;
                double x = Mth.lerp(t, boss.getX(), target.getX());
                double y = Mth.lerp(t, boss.getY() + 1.0D, target.getY() + 1.0D);
                double z = Mth.lerp(t, boss.getZ(), target.getZ());
                level.sendParticles(ParticleTypes.SOUL, x, y, z, 1, 0, 0, 0, 0);
            }
            level.playSound(null, boss.blockPosition(), SoundEvents.FISHING_BOBBER_RETRIEVE,
                SoundSource.HOSTILE, 1.0F, 0.8F);
        }
    },

    /**
     * Line charge: boss rushes forward several blocks, damaging anything in a
     * narrow line and destroying thin blocking blocks (cobblestone walls
     * players stack between themselves and the boss).
     */
    LINE_CHARGE(220) {
        @Override
        public void run(ServerLevel level, Mob boss, Player target, TowerTheme theme, int rank) {
            if (target == null) {
                return;
            }
            Vec3 dir = new Vec3(target.getX() - boss.getX(), 0.0D, target.getZ() - boss.getZ()).normalize();
            double sx = boss.getX(), sz = boss.getZ(), sy = boss.getY();
            // dash up to 8 blocks forward
            for (int step = 1; step <= 8; step++) {
                double x = sx + dir.x * step;
                double z = sz + dir.z * step;
                BlockPos at = BlockPos.containing(x, sy, z);
                // smash one blocking block per column (cover-cheese)
                BlockState st = level.getBlockState(at);
                if (!st.isAir() && !st.getCollisionShape(level, at).isEmpty() && isBreakableForCharge(st)) {
                    level.destroyBlock(at, true, boss);
                }
                // damage entities in a 1.5-wide box at this step
                for (Player p : level.getEntitiesOfClass(Player.class,
                    new AABB(x - 0.8D, sy - 0.5D, z - 0.8D, x + 0.8D, sy + 2.5D, z + 0.8D),
                    TowerAbility::validTarget)) {
                    p.hurtServer(level, boss.damageSources().mobAttack(boss), damage(rank, 2.0D));
                    p.knockback(0.4D, -dir.x, -dir.z, boss.damageSources().generic(), 0.1F);
                }
                level.sendParticles(ParticleTypes.CRIT, x, sy + 1.0D, z, 4, 0.2D, 0.2D, 0.2D, 0.01D);
            }
            // move boss to the end of the charge
            BlockPos dest = BlockPos.containing(sx + dir.x * 6, sy, sz + dir.z * 6);
            TowerBoss.blink(level, boss, dest);
            level.playSound(null, boss.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK,
                SoundSource.HOSTILE, 0.9F, 1.0F);
        }
    },

    /**
     * Ceiling barrage: for mage-type bosses (witch/evoker/blaze). Drops
     * several meteors at and around the target — and specifically at any
     * solid pillar blocks the target might be standing on, or on the roof
     * above the boss. This makes pure-range sniping from above impossible
     * without cover being destroyed.
     */
    CEILING_BARRAGE(320) {
        @Override
        public void run(ServerLevel level, Mob boss, Player target, TowerTheme theme, int rank) {
            if (target == null) {
                return;
            }
            int meteors = 3 + rank;
            for (int i = 0; i < meteors; i++) {
                double ox = target.getX() + (level.getRandom().nextDouble() - 0.5D) * 4.0D;
                double oz = target.getZ() + (level.getRandom().nextDouble() - 0.5D) * 4.0D;
                double oy = target.getY() + (level.getRandom().nextInt(2));
                // Also aim directly at any known pillar block under the target
                if (i == 0) { ox = target.getX(); oz = target.getZ(); oy = target.getY(); }
                BlockPos impact = BlockPos.containing(ox, oy, oz);
                // break the block the target is standing on (pillar cheese)
                BlockPos below = impact.below();
                BlockState belowSt = level.getBlockState(below);
                if (!belowSt.isAir() && isBreakableForCharge(belowSt)
                    && Math.abs(below.getY() - boss.blockPosition().getY()) <= 10
                    && below.getY() > boss.blockPosition().getY()) {
                    level.destroyBlock(below, true, boss);
                }
                smashAt(level, boss, impact, damage(rank, 2.0D), 2.0D, true);
            }
            level.playSound(null, boss.blockPosition(), SoundEvents.WITHER_SHOOT,
                SoundSource.HOSTILE, 1.0F, 0.6F);
        }
    },

    /**
     * Pillar break: a single, targeted snap of the block directly under a
     * high-standing target. Used when the boss detects a player on a self-built
     * pillar inside the arena.
     */
    PILLAR_BREAK(180) {
        @Override
        public void run(ServerLevel level, Mob boss, Player target, TowerTheme theme, int rank) {
            if (target == null) {
                return;
            }
            BlockPos under = target.blockPosition().below();
            // travel down up to 3 blocks under the target looking for the pillar top
            for (int d = 0; d < 4; d++) {
                BlockPos at = under.below(d);
                BlockState st = level.getBlockState(at);
                if (!st.isAir() && isBreakableForCharge(st) && at.getY() > boss.blockPosition().getY()) {
                    level.destroyBlock(at, true, boss);
                    level.sendParticles(ParticleTypes.EXPLOSION, at.getX() + 0.5D, at.getY() + 0.5D, at.getZ() + 0.5D,
                        8, 0.3D, 0.3D, 0.3D, 0.05D);
                }
            }
            target.hurtServer(level, boss.damageSources().magic(), damage(rank, 1.5D));
        }
    },

    /**
     * Phase-enrage: below 35% HP, clear all cooldowns and apply strength/speed
     * + resistance for a short window, with particle fanfare. One-shot per
     * boss.
     */
    PHASE_ENRAGE(99999) { // handled specially in tick()
        @Override
        public void run(ServerLevel level, Mob boss, Player target, TowerTheme theme, int rank) {
            // reset all cooldowns for this boss so the next volley fires immediately
            Map<TowerAbility, Long> l = NEXT.get(boss.getUUID());
            if (l != null) { l.clear(); }
            boss.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 20 * 5, 0));
            boss.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 10, 2));
            boss.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 20 * 10, 1));
            try {
                var attr = boss.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
                if (attr != null) attr.setBaseValue(Math.min(0.95D, attr.getBaseValue() + 0.3D));
            } catch (Exception ignored) {}
            burst(level, boss, ParticleTypes.ANGRY_VILLAGER, 40);
            burst(level, boss, ParticleTypes.SOUL_FIRE_FLAME, 40);
            level.playSound(null, boss.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL,
                SoundSource.HOSTILE, 1.5F, 0.7F);
            boss.addTag("aoe_phase_enraged");
        }
    };

    // ---------------------------------------------------------------- shared state
    private static final Map<UUID, Map<TowerAbility, Long>> NEXT = new ConcurrentHashMap<>();
    /** Boss UUID -> gameTime at which to land the leap (ground pound). */
    private static final Map<UUID, Long> LEAP_LAND_TIME = new ConcurrentHashMap<>();
    /** Boss UUID -> whether phase enrage has already triggered. */
    private static final Set<UUID> PHASE_TRIGGERED = ConcurrentHashMap.newKeySet();

    private final int cooldown;

    TowerAbility(int cooldown) {
        this.cooldown = cooldown;
    }

    public int cooldown() {
        return cooldown;
    }

    public abstract void run(ServerLevel level, Mob boss, Player target, TowerTheme theme, int rank);

    public static void tick(ServerLevel level, Mob boss, TowerTheme theme, int rank) {
        Player target = nearestTarget(level, boss);
        // handle leap landings regardless of current target
        Long landAt = LEAP_LAND_TIME.get(boss.getUUID());
        if (landAt != null && level.getGameTime() >= landAt) {
            LEAP_LAND_TIME.remove(boss.getUUID());
            groundPound(level, boss, rank);
        }
        if (target == null) {
            return;
        }
        List<TowerAbility> abilities = TowerBoss.abilitiesOf(boss);
        if (abilities.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        Map<TowerAbility, Long> local = NEXT.computeIfAbsent(boss.getUUID(), key -> new ConcurrentHashMap<>());
        if (NEXT.size() > 512) {
            NEXT.entrySet().removeIf(entry -> entry.getValue().values().stream().allMatch(at -> now - at > 20L * 60L));
        }
        // Phase enrage: one-off trigger when the boss first drops to <35% HP.
        if (boss.getHealth() < boss.getMaxHealth() * 0.35F
            && abilities.contains(PHASE_ENRAGE)
            && !PHASE_TRIGGERED.contains(boss.getUUID())) {
            PHASE_TRIGGERED.add(boss.getUUID());
            try { PHASE_ENRAGE.run(level, boss, target, theme, rank); } catch (Exception ignored) {}
        }
        // cornered bosses shrug off one cooldown
        if (boss.getHealth() < boss.getMaxHealth() * 0.35F && boss.getRandom().nextInt(25) == 0) {
            local.remove(abilities.get(boss.getRandom().nextInt(abilities.size())));
        }
        for (TowerAbility ability : abilities) {
            if (ability == PHASE_ENRAGE) continue; // fired once on threshold above
            Long next = local.get(ability);
            if (next == null) {
                local.put(ability, now + 30L + boss.getRandom().nextInt(ability.cooldown));
                continue;
            }
            if (now < next) continue;
            local.put(ability, now + ability.cooldown);
            try { ability.run(level, boss, target, theme, rank); }
            catch (Exception ignored) { }
        }
    }

    public static void forget(UUID uuid) {
        NEXT.remove(uuid);
        LEAP_LAND_TIME.remove(uuid);
        PHASE_TRIGGERED.remove(uuid);
    }

    // ---------------------------------------------------------------- helpers

    private static int damage(int rank, double base) {
        return (int) Math.round(base * (1.0D + rank * 0.45D));
    }

    private static void hit(ServerLevel level, Mob boss, double radius, float damage, Consumer<Player> extra) {
        for (Player player : targets(level, boss, radius)) {
            player.hurtServer(level, boss.damageSources().magic(), damage);
            if (extra != null) extra.accept(player);
        }
    }

    private static void smashAt(ServerLevel level, Mob boss, BlockPos impact, float damage, double radius, boolean fire) {
        boolean anyTarget = false;
        for (Player p : level.getEntitiesOfClass(Player.class, new AABB(impact).inflate(radius),
            TowerAbility::validTarget)) {
            p.hurtServer(level, boss.damageSources().magic(), damage);
            if (fire) p.setRemainingFireTicks(100);
            anyTarget = true;
            spawnHitParticles(level, p, ParticleTypes.LAVA);
        }
        level.sendParticles(ParticleTypes.LAVA, impact.getX() + 0.5D, impact.getY() + 1.0D, impact.getZ() + 0.5D,
            30, 0.7D, 0.4D, 0.7D, 0.03D);
        level.sendParticles(ParticleTypes.FLAME, impact.getX() + 0.5D, impact.getY() + 1.0D, impact.getZ() + 0.5D,
            40, 0.8D, 0.6D, 0.8D, 0.04D);
        if (anyTarget) {
            level.playSound(null, impact, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.8F, 1.1F);
        }
    }

    private static void groundPound(ServerLevel level, Mob boss, int rank) {
        // Boss has landed from LEAP. AOE damage + knockback + break weak blocks
        // around the landing point.
        BlockPos pos = boss.blockPosition();
        for (Player p : targets(level, boss, 5.0D)) {
            p.hurtServer(level, boss.damageSources().mobAttack(boss), damage(rank, 4.0D));
            p.knockback(1.1D, boss.getX() - p.getX(), boss.getZ() - p.getZ(),
                boss.damageSources().generic(), 0.3F);
        }
        breakRing(level, pos, 2, false);
        burst(level, boss, ParticleTypes.EXPLOSION, 40);
        level.sendParticles(ParticleTypes.CLOUD, boss.getX(), boss.getY() + 0.5D, boss.getZ(), 30, 1.2D, 0.2D, 1.2D, 0.02D);
        level.playSound(null, pos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.2F, 0.7F);
        boss.setDeltaMovement(Vec3.ZERO);
// velocity set via setDeltaMovement; server syncs on next tick.
    }

    /** Break a ring of weak blocks around center at its Y-level (ceiling or floor). */
    private static void breakRing(ServerLevel level, BlockPos center, int radius, boolean above) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                BlockPos at = center.offset(dx, 0, dz);
                if (!above) {
                    // floor pound — dig a 1-block deep crater
                    BlockState st = level.getBlockState(at.below());
                    if (!st.isAir() && isBreakableForCharge(st)
                        && !st.is(Blocks.CHEST) && !st.is(Blocks.TRAPPED_CHEST)
                        && !st.is(Blocks.SPAWNER) && !st.is(Blocks.BARREL)) {
                        level.destroyBlock(at.below(), true);
                    }
                } else {
                    // ceiling smash — break the roof block and one below if needed
                    for (int dy = 0; dy <= 1; dy++) {
                        BlockPos bp = at.above(dy);
                        BlockState st = level.getBlockState(bp);
                        if (!st.isAir() && isBreakableForCharge(st)) {
                            level.destroyBlock(bp, true);
                        } else if (!st.isAir()) break;
                    }
                }
            }
        }
    }

    /** Blocks the boss smashes through during LEAP/LINE_CHARGE/PILLAR_BREAK/ROOF_SMASH.
     *  We keep bedrock/obsidian/ancient_debris/chests/spawner/end_portal/end_gateway/
     *  respawn_anchor/barrier/structure_void safe. */
    private static boolean isBreakableForCharge(BlockState state) {
        if (state.isAir()) return false;
        float hardness;
        try { hardness = state.getDestroySpeed(null, BlockPos.ZERO); }
        catch (Exception e) { hardness = 1.0F; }
        if (hardness < 0F) return false; // unbreakable
        if (state.is(Blocks.BEDROCK) || state.is(Blocks.END_PORTAL_FRAME)
            || state.is(Blocks.END_PORTAL) || state.is(Blocks.END_GATEWAY)
            || state.is(Blocks.NETHER_PORTAL) || state.is(Blocks.BARRIER)
            || state.is(Blocks.STRUCTURE_VOID) || state.is(Blocks.JIGSAW)
            || state.is(Blocks.RESPAWN_ANCHOR) || state.is(Blocks.ENCHANTING_TABLE)
            || state.is(Blocks.ENDER_CHEST) || state.is(Blocks.CHEST)
            || state.is(Blocks.TRAPPED_CHEST) || state.is(Blocks.BARREL)
            || state.is(Blocks.SPAWNER) || state.is(Blocks.COMMAND_BLOCK)
            || state.is(Blocks.REPEATING_COMMAND_BLOCK) || state.is(Blocks.CHAIN_COMMAND_BLOCK)
            || state.is(Blocks.ANVIL) || state.is(Blocks.CHIPPED_ANVIL)
            || state.is(Blocks.DAMAGED_ANVIL)) {
            return false;
        }
        return hardness <= 50.0F;
    }

    private static int countTargets(ServerLevel level, Mob boss, double radius) {
        return targets(level, boss, radius).size();
    }

    private static List<Player> targets(ServerLevel level, Mob boss, double radius) {
        AABB box = boss.getBoundingBox().inflate(radius);
        List<Player> list = new ArrayList<>();
        for (Player player : level.getEntitiesOfClass(Player.class, box, TowerAbility::validTarget)) {
            list.add(player);
        }
        return list;
    }

    private static Player nearestTarget(ServerLevel level, Mob boss) {
        Player best = null;
        double bestDistance = 32.0D * 32.0D;
        for (Player player : level.getEntitiesOfClass(Player.class, boss.getBoundingBox().inflate(32.0D), TowerAbility::validTarget)) {
            double distance = player.distanceToSqr(boss);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = player;
            }
        }
        return best;
    }

    private static boolean validTarget(Player player) {
        return player.isAlive() && !player.isSpectator() && !player.isCreative();
    }

    private static void burst(ServerLevel level, LivingEntity boss, net.minecraft.core.particles.SimpleParticleType particle, int count) {
        level.sendParticles(particle, boss.getX(), boss.getY() + 1.0D, boss.getZ(), count, 0.5D, 0.7D, 0.5D, 0.02D);
    }

    private static void spawnHitParticles(ServerLevel level, LivingEntity target, net.minecraft.core.particles.SimpleParticleType particle) {
        level.sendParticles(particle, target.getX(), target.getY() + target.getBbHeight() * 0.75D, target.getZ(),
            12, 0.4D, 0.4D, 0.4D, 0.03D);
    }
}
