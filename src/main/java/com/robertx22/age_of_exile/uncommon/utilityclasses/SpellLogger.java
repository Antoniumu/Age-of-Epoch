package com.robertx22.age_of_exile.uncommon.utilityclasses;

import net.minecraft.world.entity.LivingEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class SpellLogger {

    public static final Logger LOGGER = LogManager.getLogger("AgeOfEpoch-Spells");

    private SpellLogger() {}

    public static void logCastAttempt(LivingEntity caster, String spellId, int manaCost, boolean success, String reason) {
        String entityName = caster != null ? caster.getName().getString() : "Unknown";
        if (success) {
            LOGGER.info("[SpellCast] {} successfully started casting spell '{}' (mana cost: {})",
                    entityName, spellId, manaCost);
        } else {
            LOGGER.warn("[SpellCast] {} failed to cast spell '{}' (reason: {})",
                    entityName, spellId, reason);
        }
    }

    public static void logSpellExecute(LivingEntity caster, String spellId, int timesToCast) {
        String entityName = caster != null ? caster.getName().getString() : "Unknown";
        LOGGER.info("[SpellExecute] {} casted spell '{}' (times: {})",
                entityName, spellId, timesToCast);
    }

    public static void logProjectileShot(LivingEntity caster, String spellId, String projectileType,
                                        double speed, int amount) {
        String entityName = caster != null ? caster.getName().getString() : "Unknown";
        LOGGER.info("[SpellProjectile] {} summoned/shot {} projectile(s) of type '{}' for spell '{}' (speed: {})",
                entityName, amount, projectileType, spellId, speed);
    }

    public static void logCastCancelled(LivingEntity caster, String spellId, int refundedMana, String reason) {
        String entityName = caster != null ? caster.getName().getString() : "Unknown";
        LOGGER.info("[SpellCancel] Cast of '{}' by {} was cancelled (refunded mana: {}, reason: {})",
                spellId, entityName, refundedMana, reason);
    }
}
