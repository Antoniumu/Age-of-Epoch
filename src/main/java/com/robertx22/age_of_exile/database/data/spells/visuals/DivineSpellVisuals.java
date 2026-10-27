package com.robertx22.age_of_exile.database.data.spells.visuals;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/** Client-side, spell-specific cast visuals for the Divine school. */
public final class DivineSpellVisuals {

    private static final double TAU = Math.PI * 2D;

    private static final Set<String> SPELLS = Set.of(
        "healing_aura",
        "gong_strike",
        "whirlwind",
        "charge",
        "shooting_star",
        "undying_will",
        "pull",
        "awaken_mana",
        "song_of_perseverance",
        "song_of_valor",
        "song_of_vigor",
        "taunt",
        "banish",
        "wish",
        // This HolySpells entry is not currently a node in the Divine school tree,
        // but it still deserves a complete cast visual when it is used.
        "shout_warn"
    );

    private DivineSpellVisuals() {
    }

    public static boolean hasVisual(String spellId) {
        return SPELLS.contains(spellId);
    }

    /**
     * Called only on clients from the server's cast-visual packet. Each spell has
     * its own shape and particle palette rather than relying on generic spell data.
     */
    public static void play(String spellId, LivingEntity caster) {
        if (caster == null || !caster.level().isClientSide() || !hasVisual(spellId)) {
            return;
        }

        Level level = caster.level();
        Vec3 feet = caster.position().add(0D, 0.08D, 0D);
        Vec3 chest = caster.position().add(0D, caster.getBbHeight() * 0.55D, 0D);
        Vec3 head = caster.position().add(0D, caster.getBbHeight() * 0.85D, 0D);
        Vec3 look = caster.getLookAngle().normalize();

        switch (spellId) {
            case "healing_aura" -> {
                ring(level, feet, ParticleTypes.HEART, 28, 1.15D, 0.2D, 0.06D, 0.04D, 0D);
                ring(level, feet, ParticleTypes.HAPPY_VILLAGER, 24, 1.9D, 0.45D, 0.12D, 0.02D, 0.11D);
                spiral(level, feet, ParticleTypes.END_ROD, 24, 0.8D, 2.2D, 1.45D, 0.08D, 0D);
            }
            case "gong_strike" -> {
                ring(level, feet, ParticleTypes.CRIT, 42, 2.5D, 0.25D, 0.05D, 0.5D, 0D);
                ring(level, feet, ParticleTypes.CLOUD, 30, 1.4D, 0.2D, 0.14D, 0.22D, 0.07D);
                burst(level, chest, ParticleTypes.ENCHANTED_HIT, 22, 0.3D, 0.42D);
            }
            case "whirlwind" -> {
                spiral(level, chest, ParticleTypes.CLOUD, 28, 1.05D, 1.9D, 2.1D, 0.09D, 0D);
                spiral(level, chest, ParticleTypes.END_ROD, 24, 1.65D, 2.2D, -2.7D, 0.1D, Math.PI);
            }
            case "charge" -> {
                forwardTrail(level, feet, look, ParticleTypes.CLOUD, 10, 0.15D, 2.7D, 0.45D, 0.2D);
                forwardTrail(level, feet.add(0D, 0.25D, 0D), look, ParticleTypes.END_ROD, 9, 0.12D, 2.9D, 0.25D, 0.16D);
                burst(level, feet, ParticleTypes.CRIT, 7, 0.22D, 0.28D);
            }
            case "shooting_star" -> {
                Vec3 starPathEnd = head.add(look.scale(3.6D));
                forwardTrail(level, head, look, ParticleTypes.END_ROD, 22, 0.22D, 3.6D, 0.35D, 0.28D);
                forwardTrail(level, head, look, ParticleTypes.ENCHANTED_HIT, 12, 0.3D, 3.2D, 0.18D, 0.16D);
                burst(level, starPathEnd, ParticleTypes.HEART, 12, 0.3D, 0.12D);
            }
            case "undying_will" -> {
                sphere(level, chest, ParticleTypes.TOTEM_OF_UNDYING, 42, 1.25D, 0.04D);
                ring(level, feet, ParticleTypes.ENCHANTED_HIT, 30, 1.0D, 0.2D, 0.12D, 0.03D, 0D);
                spiral(level, chest, ParticleTypes.END_ROD, 18, 0.9D, 1.8D, 1.3D, 0.07D, 0.18D);
            }
            case "pull" -> {
                ring(level, feet, ParticleTypes.PORTAL, 48, 6.2D, 0.4D, 0.02D, -0.52D, 0D);
                ring(level, feet, ParticleTypes.REVERSE_PORTAL, 34, 4.4D, 0.7D, 0.03D, -0.38D, 0.12D);
                burst(level, chest, ParticleTypes.CRIT, 20, 0.7D, 0.25D);
            }
            case "awaken_mana" -> {
                spiral(level, feet, ParticleTypes.WITCH, 30, 0.85D, 2.4D, 1.7D, 0.07D, 0D);
                spiral(level, chest, ParticleTypes.ENCHANT, 26, 1.2D, 1.7D, -1.2D, 0.05D, Math.PI);
                ring(level, feet, ParticleTypes.END_ROD, 20, 1.6D, 0.4D, 0.2D, 0.02D, 0.05D);
            }
            case "song_of_valor" -> {
                ring(level, chest, ParticleTypes.CRIT, 32, 2.6D, 0.15D, 0.03D, 0.24D, 0D);
                burst(level, chest, ParticleTypes.ENCHANTED_HIT, 30, 0.5D, 0.28D);
                ring(level, head, ParticleTypes.NOTE, 20, 1.1D, 0D, 0.14D, 0.03D, 0.1D);
            }
            case "song_of_perseverance" -> {
                sphere(level, chest, ParticleTypes.TOTEM_OF_UNDYING, 38, 1.35D, 0.025D);
                ring(level, feet, ParticleTypes.END_ROD, 24, 1.8D, 0.35D, 0.15D, 0.01D, 0D);
                spiral(level, chest, ParticleTypes.ENCHANTED_HIT, 18, 0.75D, 1.8D, 1.8D, 0.04D, Math.PI / 3D);
            }
            case "song_of_vigor" -> {
                spiral(level, feet, ParticleTypes.HEART, 34, 1.0D, 2.5D, 1.35D, 0.05D, 0D);
                burst(level, chest, ParticleTypes.HAPPY_VILLAGER, 28, 0.6D, 0.16D);
                ring(level, feet, ParticleTypes.HEART, 18, 1.8D, 0.3D, 0.08D, 0.1D, 0.08D);
            }
            case "taunt" -> {
                ring(level, chest, ParticleTypes.CLOUD, 36, 3.1D, 0D, 0.04D, 0.32D, 0D);
                burst(level, chest, ParticleTypes.ANGRY_VILLAGER, 24, 1.2D, 0.12D);
                ring(level, feet, ParticleTypes.CRIT, 18, 1.1D, 0.15D, 0.06D, 0.35D, 0.08D);
            }
            case "banish" -> {
                sphere(level, chest, ParticleTypes.REVERSE_PORTAL, 44, 1.5D, 0.025D);
                ring(level, feet, ParticleTypes.SOUL_FIRE_FLAME, 30, 2.4D, 0.25D, 0.2D, 0.04D, 0D);
                spiral(level, feet, ParticleTypes.END_ROD, 20, 1.8D, 2.7D, 1.2D, 0.06D, 0.16D);
            }
            case "wish" -> {
                ring(level, feet, ParticleTypes.HEART, 46, 4.8D, 0.25D, 0.1D, 0.15D, 0D);
                ring(level, feet, ParticleTypes.HAPPY_VILLAGER, 30, 3.8D, 0.7D, 0.2D, 0.03D, 0.1D);
                spiral(level, feet, ParticleTypes.END_ROD, 34, 2.2D, 3D, 1.2D, 0.08D, 0.2D);
                burst(level, chest, ParticleTypes.TOTEM_OF_UNDYING, 20, 0.5D, 0.2D);
            }
            case "shout_warn" -> {
                sphere(level, chest, ParticleTypes.TOTEM_OF_UNDYING, 34, 2.2D, 0.025D);
                ring(level, feet, ParticleTypes.CLOUD, 42, 3.3D, 0.2D, 0.06D, 0.26D, 0D);
                ring(level, head, ParticleTypes.END_ROD, 24, 2.3D, 0.1D, 0.17D, 0D, 0.08D);
            }
            default -> {
                // Defensive fallback: even a newly added Divine skill gets a visible burst.
                ring(level, feet, ParticleTypes.END_ROD, 24, 1.5D, 0.35D, 0.15D, 0.12D, 0D);
                burst(level, chest, ParticleTypes.ENCHANTED_HIT, 12, 0.3D, 0.2D);
            }
        }
    }

    private static void ring(Level level, Vec3 center, SimpleParticleType particle, int count,
                             double radius, double yOffset, double upwardSpeed,
                             double radialSpeed, double phase) {
        for (int i = 0; i < count; i++) {
            double angle = TAU * i / count + phase;
            double xDirection = Math.cos(angle);
            double zDirection = Math.sin(angle);
            double yJitter = (level.getRandom().nextDouble() - 0.5D) * 0.18D;
            Vec3 position = center.add(xDirection * radius, yOffset + yJitter, zDirection * radius);
            Vec3 velocity = new Vec3(xDirection * radialSpeed, upwardSpeed, zDirection * radialSpeed);
            emit(level, particle, position, velocity);
        }
    }

    private static void spiral(Level level, Vec3 center, SimpleParticleType particle, int count,
                               double radius, double height, double turns, double upwardSpeed, double phase) {
        for (int i = 0; i < count; i++) {
            double progress = (i + 0.5D) / count;
            double angle = phase + TAU * turns * progress;
            double xDirection = Math.cos(angle);
            double zDirection = Math.sin(angle);
            Vec3 position = center.add(xDirection * radius, height * progress, zDirection * radius);
            Vec3 velocity = new Vec3(-zDirection * 0.08D, upwardSpeed, xDirection * 0.08D);
            emit(level, particle, position, velocity);
        }
    }

    private static void sphere(Level level, Vec3 center, SimpleParticleType particle,
                               int count, double radius, double speed) {
        for (int i = 0; i < count; i++) {
            double y = 1D - 2D * (i + 0.5D) / count;
            double horizontalRadius = Math.sqrt(1D - y * y);
            double angle = i * 2.399963229728653D;
            Vec3 direction = new Vec3(horizontalRadius * Math.cos(angle), y,
                horizontalRadius * Math.sin(angle));
            emit(level, particle, center.add(direction.scale(radius)), direction.scale(speed));
        }
    }

    private static void burst(Level level, Vec3 center, SimpleParticleType particle,
                              int count, double spread, double speed) {
        for (int i = 0; i < count; i++) {
            double y = level.getRandom().nextDouble() * 2D - 1D;
            double angle = level.getRandom().nextDouble() * TAU;
            double horizontalRadius = Math.sqrt(1D - y * y);
            Vec3 direction = new Vec3(horizontalRadius * Math.cos(angle), y,
                horizontalRadius * Math.sin(angle));
            double startDistance = level.getRandom().nextDouble() * spread;
            emit(level, particle, center.add(direction.scale(startDistance)), direction.scale(speed));
        }
    }

    private static void forwardTrail(Level level, Vec3 origin, Vec3 direction, SimpleParticleType particle,
                                     int count, double start, double length, double spread, double speed) {
        Vec3 forward = direction.normalize();
        Vec3 right = forward.cross(new Vec3(0D, 1D, 0D));
        if (right.lengthSqr() < 1.0E-5D) {
            right = new Vec3(1D, 0D, 0D);
        } else {
            right = right.normalize();
        }
        Vec3 up = right.cross(forward).normalize();

        for (int i = 0; i < count; i++) {
            double progress = count == 1 ? 0D : (double) i / (count - 1);
            double envelope = Math.sin(Math.PI * progress) * spread;
            double lateral = (level.getRandom().nextDouble() - 0.5D) * envelope;
            double vertical = (level.getRandom().nextDouble() - 0.5D) * envelope;
            Vec3 position = origin.add(forward.scale(start + length * progress))
                .add(right.scale(lateral))
                .add(up.scale(vertical));
            Vec3 velocity = forward.scale(speed).add(up.scale(0.035D));
            emit(level, particle, position, velocity);
        }
    }

    private static void emit(Level level, SimpleParticleType particle, Vec3 position, Vec3 velocity) {
        level.addParticle(particle, position.x, position.y, position.z,
            velocity.x, velocity.y, velocity.z);
    }
}
