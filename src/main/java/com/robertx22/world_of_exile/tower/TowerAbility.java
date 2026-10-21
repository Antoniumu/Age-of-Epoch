package com.robertx22.world_of_exile.tower;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Boss abilities.  Every ability is a small, self contained effect with its
 * own cooldown; the roster mixes them up so two towers of the same theme can
 * still play differently (main boss + champions).
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
            level.playSound(null, boss.blockPosition(), net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(),
                net.minecraft.sounds.SoundSource.HOSTILE, 0.7F, 1.2F);
        }
    },

    THUNDER_STOMP(260) {
        @Override
        public void run(ServerLevel level, Mob boss, Player target, TowerTheme theme, int rank) {
            hit(level, boss, 9.0D, damage(rank, 3.0D),
                p -> p.knockback(0.6D, boss.getX() - p.getX(), boss.getZ() - p.getZ(),
                    boss.damageSources().generic(), 0.0F));
            burst(level, boss, ParticleTypes.ELECTRIC_SPARK, 50);
            level.playSound(null, boss.blockPosition(), net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER.value(),
                net.minecraft.sounds.SoundSource.HOSTILE, 0.5F, 1.4F);
        }
    },

    METEOR(280) {
        @Override
        public void run(ServerLevel level, Mob boss, Player target, TowerTheme theme, int rank) {
            if (target == null) {
                return;
            }
            hit(level, boss, 5.0D, damage(rank, 3.5D), p -> p.setRemainingFireTicks(120));
            level.sendParticles(ParticleTypes.LAVA, target.getX(), target.getY() + 1.0D, target.getZ(),
                30, 0.7D, 0.4D, 0.7D, 0.0D);
            level.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + 1.0D, target.getZ(),
                40, 0.8D, 0.6D, 0.8D, 0.04D);
            level.playSound(null, target.blockPosition(), net.minecraft.sounds.SoundEvents.FIRECHARGE_USE.value(),
                net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 0.7F);
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
            boss.addEffect(new MobEffectInstance(MobEffects.SPEED, 20 * 10, 1));
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
                net.minecraft.core.BlockPos pos = net.minecraft.core.BlockPos.containing(x, y, z);
                if (level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()) {
                    level.sendParticles(ParticleTypes.PORTAL, boss.getX(), boss.getY() + 1.0D, boss.getZ(),
                        24, 0.3D, 0.6D, 0.3D, 0.05D);
                    boss.teleportTo(x, y, z);
                    boss.getNavigation().stop();
                    return;
                }
            }
        }
    };

    private static final Map<UUID, Map<TowerAbility, Long>> NEXT = new ConcurrentHashMap<>();

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
        if (target == null) {
            return;
        }
        long now = level.getGameTime();
        Map<TowerAbility, Long> local = NEXT.computeIfAbsent(boss.getUUID(), key -> new ConcurrentHashMap<>());
        if (NEXT.size() > 512) {
            NEXT.entrySet().removeIf(entry -> entry.getValue().values().stream().allMatch(at -> now - at > 20L * 60L));
        }
        if (boss.getHealth() < boss.getMaxHealth() * 0.35F && boss.getRandom().nextInt(25) == 0) {
            // cornered bosses shrug off one cooldown
            TowerAbility[] all = values();
            local.remove(all[boss.getRandom().nextInt(all.length)]);
        }
        for (TowerAbility ability : values()) {
            Long next = local.get(ability);
            if (next == null) {
                // stagger the opening volley so a boss does not dump every
                // ability on the same tick
                local.put(ability, now + 30L + boss.getRandom().nextInt(ability.cooldown));
                continue;
            }
            if (now < next) {
                continue;
            }
            local.put(ability, now + ability.cooldown);
            try {
                ability.run(level, boss, target, theme, rank);
            } catch (Exception ignored) {
            }
        }
    }

    public static void forget(UUID uuid) {
        NEXT.remove(uuid);
    }

    private static int damage(int rank, double base) {
        return (int) Math.round(base * (1.0D + rank * 0.45D));
    }

    private static void hit(ServerLevel level, Mob boss, double radius, float damage, Consumer<Player> extra) {
        for (Player player : targets(level, boss, radius)) {
            player.hurtServer(level, boss.damageSources().magic(), damage);
            if (extra != null) {
                extra.accept(player);
            }
        }
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
        double bestDistance = 26.0D * 26.0D;
        for (Player player : level.getEntitiesOfClass(Player.class, boss.getBoundingBox().inflate(26.0D), TowerAbility::validTarget)) {
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
}
