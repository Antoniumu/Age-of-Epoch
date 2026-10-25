package com.robertx22.age_of_exile.aoe_data.database.spells.schools;

import com.robertx22.age_of_exile.aoe_data.database.exile_effects.adders.BeneficialEffects;
import com.robertx22.age_of_exile.aoe_data.database.exile_effects.adders.NegativeEffects;
import com.robertx22.age_of_exile.aoe_data.database.spells.PartBuilder;
import com.robertx22.age_of_exile.aoe_data.database.spells.SpellBuilder;
import com.robertx22.age_of_exile.aoe_data.database.spells.SpellCalcs;
import com.robertx22.age_of_exile.aoe_data.database.stats.base.EffectCtx;
import com.robertx22.age_of_exile.database.data.spells.SetAdd;
import com.robertx22.age_of_exile.database.data.spells.SpellTag;
import com.robertx22.age_of_exile.database.data.spells.components.ComponentPart;
import com.robertx22.age_of_exile.database.data.spells.components.SpellConfiguration;
import com.robertx22.age_of_exile.database.data.spells.components.actions.AggroAction;
import com.robertx22.age_of_exile.database.data.spells.components.actions.ExileEffectAction;
import com.robertx22.age_of_exile.database.data.spells.components.actions.SpellAction;
import com.robertx22.age_of_exile.database.data.spells.components.actions.vanity.ParticleInRadiusAction;
import com.robertx22.age_of_exile.database.data.spells.components.actions.vanity.ParticleMotion;
import com.robertx22.age_of_exile.database.data.spells.components.conditions.EffectCondition;
import com.robertx22.age_of_exile.database.data.spells.components.selectors.TargetSelector;
import com.robertx22.age_of_exile.database.data.spells.map_fields.MapField;
import com.robertx22.age_of_exile.database.data.spells.spell_classes.CastingWeapon;
import com.robertx22.age_of_exile.mmorpg.registers.common.SlashBlocks;
import com.robertx22.age_of_exile.mmorpg.registers.common.SlashEntities;
import com.robertx22.age_of_exile.mmorpg.registers.common.SlashPotions;
import com.robertx22.age_of_exile.mmorpg.registers.common.SlashSounds;
import com.robertx22.age_of_exile.uncommon.enumclasses.Elements;
import com.robertx22.age_of_exile.uncommon.enumclasses.PlayStyle;
import com.robertx22.age_of_exile.uncommon.utilityclasses.AllyOrEnemy;
import com.robertx22.age_of_exile.uncommon.utilityclasses.EntityFinder;
import com.robertx22.library_of_exile.registry.ExileRegistryInit;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Items;

import java.util.Arrays;

public class HolySpells implements ExileRegistryInit {

    public static String HEALING_AURA_ID = "healing_aura";
    public static String WISH = "wish";
    public static String UNDYING_WILL = "undying_will";
    public static String BANISH = "banish";
    public static String INSPIRATION = "awaken_mana";
    public static String SHOOTING_STAR = "shooting_star";

    public static String CHARGE_ID = "charge";
    public static String GONG_STRIKE_ID = "gong_strike";
    public static String WHIRLWIND = "whirlwind";
    public static String TAUNT = "taunt";
    public static String SHOUT_WARN = "shout_warn";
    public static String PULL = "pull";

    public static String HYMN_OF_VALOR = "song_of_valor";
    public static String HYMN_OF_PERSERVANCE = "song_of_perseverance";
    public static String HYMN_OF_VIGOR = "song_of_vigor";

    @Override
    public void registerAll() {

        song(HYMN_OF_VALOR, "Hymn of Valor", BeneficialEffects.VALOR, ParticleTypes.CRIT);
        song(HYMN_OF_PERSERVANCE, "Hymn of Perseverance", BeneficialEffects.PERSEVERANCE, ParticleTypes.TOTEM_OF_UNDYING);
        song(HYMN_OF_VIGOR, "Hymn of Vigor", BeneficialEffects.VIGOR, ParticleTypes.HEART);

        SpellBuilder.of(WHIRLWIND, SpellConfiguration.Builder.multiCast(10, 0, 100, 10)
                    .setSwingArm(), "Whirlwind",
                Arrays.asList(SpellTag.technique, SpellTag.area, SpellTag.damage))
            .manualDesc("Strike enemies around you for " +
                SpellCalcs.WHIRLWIND.getLocDmgTooltip(Elements.Fire) + ". Scales with attack speed.")

            .attackStyle(PlayStyle.melee)
            .weaponReq(CastingWeapon.MELEE_WEAPON)
            .onCast(PartBuilder.giveSelfEffect(SlashPotions.KNOCKBACK_RESISTANCE.get(), 100D))
            .onCast(PartBuilder.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1D, 1D))
            .onCast(PartBuilder.groundEdgeParticles(ParticleTypes.EFFECT, 100D, 2D, 0.5D))
            .onCast(PartBuilder.groundEdgeParticles(ParticleTypes.CLOUD, 90D, 1.8D, 0.7D, ParticleMotion.Upwards))
            .onCast(PartBuilder.groundEdgeParticles(ParticleTypes.END_ROD, 90D, 2.2D, 0.5D, ParticleMotion.OutwardMotion))
            .onCast(PartBuilder.damageInAoe(SpellCalcs.WHIRLWIND, Elements.Physical, 1.5D)
                .addPerEntityHit(PartBuilder.groundEdgeParticles(ParticleTypes.EFFECT, 50D, 0.5D, 0.1D))
                .addPerEntityHit(PartBuilder.groundEdgeParticles(ParticleTypes.CRIT, 60D, 0.7D, 0.2D, ParticleMotion.OutwardMotion))
                .addPerEntityHit(PartBuilder.groundEdgeParticles(ParticleTypes.ENCHANTED_HIT, 40D, 0.6D, 0.2D, ParticleMotion.Upwards))
            )
            .build();

        SpellBuilder.of(CHARGE_ID, SpellConfiguration.Builder.multiCast(10, 20 * 10, 60, 60)
                    .setScaleManaToPlayer(), "Charge",
                Arrays.asList(SpellTag.area, SpellTag.damage, SpellTag.movement))
            .manualDesc(
                "Charge in a direction, stopping upon first enemy hit to deal "
                    + SpellCalcs.CHARGE.getLocDmgTooltip() + " " + Elements.Physical.getIconNameDmg() + " in radius."

            )
            .attackStyle(PlayStyle.melee)
            .weaponReq(CastingWeapon.MELEE_WEAPON)
            .onCast(PartBuilder.playSound(SoundEvents.ANCIENT_DEBRIS_STEP, 1D, 1D))
            .onCast(PartBuilder.justAction(SpellAction.SET_ADD_MOTION.create(SetAdd.ADD, 0.2D, ParticleMotion.CasterLook)
                    .put(MapField.IGNORE_Y, true))
                .addTarget(TargetSelector.CASTER.create()))
            .onCast(PartBuilder.groundEdgeParticles(ParticleTypes.CLOUD, 20D, 1D, 0.5D))
            .onCast(PartBuilder.groundEdgeParticles(ParticleTypes.EXPLOSION, 1D, 1D, 0.5D))
            .onCast(PartBuilder.groundEdgeParticles(ParticleTypes.END_ROD, 12D, 1D, 0.3D, ParticleMotion.OutwardMotion))
            .onCast(PartBuilder.groundEdgeParticles(ParticleTypes.CRIT, 9D, 0.9D, 0.3D, ParticleMotion.Upwards))
            .onCast(PartBuilder.damageInAoe(SpellCalcs.CHARGE, Elements.Physical, 1.75D)
                .addPerEntityHit(PartBuilder.playSound(SoundEvents.ANVIL_LAND, 1D, 1D))
                .addPerEntityHit(PartBuilder.groundEdgeParticles(ParticleTypes.EFFECT, 100D, 0.5D, 0.1D))
                .addPerEntityHit(PartBuilder.groundEdgeParticles(ParticleTypes.CLOUD, 100D, 0.5D, 0.1D))
                .addPerEntityHit(PartBuilder.groundEdgeParticles(ParticleTypes.ENCHANTED_HIT, 60D, 0.8D, 0.2D, ParticleMotion.OutwardMotion))
                .addPerEntityHit(PartBuilder.groundEdgeParticles(ParticleTypes.EXPLOSION, 3D, 0.8D, 0.1D))
                .addPerEntityHit(PartBuilder.cancelSpell())
            )
            .build();

        SpellBuilder.of(TAUNT, SpellConfiguration.Builder.instant(0, 20 * 30)
                    .setSwingArm(), "Taunt",
                Arrays.asList(SpellTag.area))
            .manualDesc(
                "Shout, making enemies nearby want to attack you. " +
                    "Generates " + SpellCalcs.TAUNT.getLocDmgTooltip() + " threat."
            )
            .attackStyle(PlayStyle.melee)
            .weaponReq(CastingWeapon.MELEE_WEAPON)
            .onCast(PartBuilder.playSound(SoundEvents.SHIELD_BLOCK, 1D, 1D))
            .onCast(PartBuilder.justAction(SpellAction.AGGRO.create(SpellCalcs.TAUNT, AggroAction.Type.AGGRO))
                .addTarget(TargetSelector.AOE.create(3D, EntityFinder.SelectionType.RADIUS, AllyOrEnemy.enemies)))
            .onCast(PartBuilder.aoeParticles(ParticleTypes.CLOUD, 60D, 3D))
            .onCast(PartBuilder.aoeParticles(ParticleTypes.ANGRY_VILLAGER, 60D, 3D))
            .onCast(PartBuilder.groundEdgeParticles(ParticleTypes.CRIT, 80D, 3.4D, 0.3D, ParticleMotion.OutwardMotion))
            .onCast(PartBuilder.groundEdgeParticles(ParticleTypes.END_ROD, 40D, 3D, 0.4D, ParticleMotion.Upwards))

            .build();
        SpellBuilder.of(SHOUT_WARN, SpellConfiguration.Builder.instant(10, 60 * 20), "Warning Shout",
                Arrays.asList(SpellTag.area, SpellTag.shout, SpellTag.shield))

            .manualDesc(
                "Let out a warning shout, giving a "
                    + SpellCalcs.SHOUT_WARN.getLocDmgTooltip() + " Shield to all nearby allies.")

            .attackStyle(PlayStyle.melee)
            .onCast(PartBuilder.playSound(SoundEvents.ENDER_DRAGON_GROWL, 1D, 1D))
            .onCast(PartBuilder.giveShieldInRadius(10D, SpellCalcs.SHOUT_WARN, 10D))
            .onCast(PartBuilder.aoeParticles(ParticleTypes.CLOUD, 90D, 10D))
            .onCast(PartBuilder.groundEdgeParticles(ParticleTypes.END_ROD, 90D, 10D, 0.6D, ParticleMotion.Upwards))
            .onCast(PartBuilder.aoeParticles(ParticleTypes.TOTEM_OF_UNDYING, 40D, 10D))
            .build();

        SpellBuilder.of(PULL, SpellConfiguration.Builder.instant(5, 60 * 20), "Pull",
                Arrays.asList(SpellTag.technique, SpellTag.area, SpellTag.damage))
            .manualDesc(
                "Pull enemies in area to you, dealing " +
                    SpellCalcs.PULL.getLocDmgTooltip() + " " +
                    Elements.Physical.getIconNameDmg() + " and slowing them."
            )
            .attackStyle(PlayStyle.melee)
            .onCast(PartBuilder.playSound(SoundEvents.ANVIL_HIT, 1D, 1D))
            .onCast(PartBuilder.justAction(SpellAction.TP_TARGET_TO_SELF.create())
                .addActions(SpellAction.POTION.createGive(MobEffects.SLOWNESS, 20D * 5))
                .addActions(SpellAction.DEAL_DAMAGE.create(SpellCalcs.PULL, Elements.Physical))
                .addActions(SpellAction.EXILE_EFFECT.create(NegativeEffects.STUN.resourcePath, ExileEffectAction.GiveOrTake.GIVE_STACKS, 20D * 2))
                .addTarget(TargetSelector.AOE.create(8D, EntityFinder.SelectionType.RADIUS, AllyOrEnemy.enemies)))
            .onCast(PartBuilder.groundEdgeParticles(ParticleTypes.CRIT, 140D, 6D, 0.2D))
            .onCast(inwardEdgeParticles(ParticleTypes.PORTAL, 120D, 8D, 0.08D))
            .onCast(inwardEdgeParticles(ParticleTypes.REVERSE_PORTAL, 80D, 5D, 0.1D))
            .build();

        SpellBuilder.of(GONG_STRIKE_ID, SpellConfiguration.Builder.instant(8, 20 * 10)
                    .setSwingArm(), "Gong Strike",
                Arrays.asList(SpellTag.technique, SpellTag.area, SpellTag.damage))
            .manualDesc("Bash enemies around you for " +
                SpellCalcs.GONG_STRIKE.getLocDmgTooltip(Elements.Physical))

            .attackStyle(PlayStyle.melee)
            .weaponReq(CastingWeapon.MELEE_WEAPON)

            .onCast(PartBuilder.playSound(SoundEvents.ANVIL_PLACE, 1D, 1D))
            .onCast(PartBuilder.playSound(SoundEvents.GENERIC_EXPLODE.value(), 1D, 1D))

            .onCast(PartBuilder.damageInFront(SpellCalcs.GONG_STRIKE, Elements.Physical, 2D, 3D))
            .onCast(PartBuilder.addExileEffectToEnemiesInFront(NegativeEffects.STUN.resourcePath, 2D, 2D, 20D * 3))

            .onCast(PartBuilder.groundEdgeParticles(ParticleTypes.CLOUD, 360D, 2D, 0.2D))
            .onCast(PartBuilder.groundEdgeParticles(ParticleTypes.EXPLOSION, 8D, 2D, 0.1D))
            .onCast(PartBuilder.groundEdgeParticles(ParticleTypes.CRIT, 100D, 2.5D, 0.4D, ParticleMotion.OutwardMotion))
            .onCast(PartBuilder.groundEdgeParticles(ParticleTypes.END_ROD, 80D, 2.2D, 0.4D, ParticleMotion.Upwards))

            .build();

        SpellBuilder.of(UNDYING_WILL, SpellConfiguration.Builder.instant(7, 20 * 60)
                    .setScaleManaToPlayer(), "Undying Will",
                Arrays.asList())
            .manualDesc("Undying Will for 10 s: you take far less damage and regenerate health.")
            .attackStyle(PlayStyle.melee)
            .weaponReq(CastingWeapon.ANY_WEAPON)
            .onCast(PartBuilder.playSound(SoundEvents.RAVAGER_ROAR, 1D, 1D))
            .onCast(PartBuilder.giveSelfExileEffect(BeneficialEffects.UNDYING_WILL, 20D * 10))
            .onCast(PartBuilder.aoeParticles(ParticleTypes.ENCHANTED_HIT, 100D, 1D))
            .onCast(PartBuilder.aoeParticles(ParticleTypes.ENCHANT, 100D, 1D))
            .onCast(PartBuilder.aoeParticles(ParticleTypes.TOTEM_OF_UNDYING, 80D, 1.2D))
            .onCast(PartBuilder.groundEdgeParticles(ParticleTypes.END_ROD, 40D, 1.5D, 0.4D, ParticleMotion.Upwards))
            .build();

        SpellBuilder.of(BANISH, SpellConfiguration.Builder.instant(10, 20 * 45)
                .setScaleManaToPlayer(), "Banish", Arrays.asList())
            .manualDesc(
                "Summon a Magic circle that banishes enemies in the area, levitating them for a certain duration and applying a debuff.")
            .onCast(PartBuilder.playSound(SoundEvents.ILLUSIONER_CAST_SPELL, 1D, 1D))
            .onCast(PartBuilder.justAction(SpellAction.SUMMON_AT_SIGHT.create(SlashEntities.SIMPLE_PROJECTILE.get(), 1D, 0D)))
            .onExpire(PartBuilder.justAction(SpellAction.SUMMON_BLOCK.create(SlashBlocks.GLYPH.get(), 20D * 5)
                .put(MapField.ENTITY_NAME, "block")
                .put(MapField.BLOCK_FALL_SPEED, 0D)
                .put(MapField.FIND_NEAREST_SURFACE, false)
                .put(MapField.IS_BLOCK_FALLING, false)))
            .onExpire(PartBuilder.aoeParticles(ParticleTypes.REVERSE_PORTAL, 80D, 3D))
            .onExpire(PartBuilder.groundEdgeParticles(ParticleTypes.END_ROD, 40D, 3D, 0.4D, ParticleMotion.Upwards))
            .onExpire(PartBuilder.aoeParticles(ParticleTypes.EXPLOSION, 3D, 1D))

            .onTick("block", PartBuilder.justAction(SpellAction.SET_ADD_MOTION.create(SetAdd.SET, 0.1D, ParticleMotion.Upwards))
                .addActions(SpellAction.EXILE_EFFECT.create(NegativeEffects.ELE_WEAKNESS.resourcePath, ExileEffectAction.GiveOrTake.GIVE_STACKS, 60D))
                .onTick(1D)
                .addTarget(TargetSelector.AOE.create(3D, EntityFinder.SelectionType.RADIUS, AllyOrEnemy.enemies)))

            .onTick("block", PartBuilder.playSound(SoundEvents.SOUL_SOIL_HIT, 0.5D, 1D)
                .addCondition(EffectCondition.EVERY_X_TICKS.create(40D)))

            .onTick("block", PartBuilder.groundEdgeParticles(ParticleTypes.SOUL_FIRE_FLAME, 20D, 3D, 0.5D)
                .onTick(3D))
            .onTick("block", PartBuilder.groundEdgeParticles(ParticleTypes.PORTAL, 16D, 3.2D, 0.6D, ParticleMotion.Upwards)
                .onTick(3D))
            .onTick("block", PartBuilder.groundEdgeParticles(ParticleTypes.END_ROD, 10D, 2.8D, 0.8D, ParticleMotion.Upwards)
                .onTick(3D))
            .build();

        SpellBuilder.of(INSPIRATION, SpellConfiguration.Builder.instant(0, 300 * 20), "Inspiration",
                Arrays.asList(SpellTag.heal)
            )
            .manualDesc("Restores " + SpellCalcs.AWAKEN_MANA.getLocDmgTooltip() + " mana.")
            .weaponReq(CastingWeapon.ANY_WEAPON)
            .onCast(PartBuilder.playSound(SoundEvents.CHORUS_FRUIT_TELEPORT, 1D, 1D))
            .onCast(PartBuilder.aoeParticles(ParticleTypes.WITCH, 100D, 1.5D))
            .onCast(PartBuilder.aoeParticles(ParticleTypes.HEART, 40D, 1.5D))
            .onCast(PartBuilder.aoeParticles(ParticleTypes.ENCHANT, 80D, 1.5D))
            .onCast(PartBuilder.groundEdgeParticles(ParticleTypes.END_ROD, 60D, 1.8D, 0.4D, ParticleMotion.Upwards))
            .onCast(PartBuilder.restoreManaToCaster(SpellCalcs.AWAKEN_MANA))
            .build();

        SpellBuilder.of(SHOOTING_STAR, SpellConfiguration.Builder.instant(10, 20)
                    .setSwingArm()
                    .applyCastSpeedToCooldown(), "Shooting Star",
                Arrays.asList(SpellTag.projectile, SpellTag.heal))
            .manualDesc("Shoots a star that heals allies for " + SpellCalcs.SHOOTING_STAR.getLocDmgTooltip() + " health on hit.")

            .weaponReq(CastingWeapon.MAGE_WEAPON)
            .onCast(PartBuilder.playSound(SoundEvents.BEACON_ACTIVATE, 1D, 1.7D))
            .onCast(PartBuilder.justAction(SpellAction.SUMMON_PROJECTILE.create(Items.NETHER_STAR, 1D, 1D, SlashEntities.SIMPLE_PROJECTILE.get(), 20D, false)
                .put(MapField.HITS_ALLIES, true)))
            .onTick(PartBuilder.aoeParticles(ParticleTypes.CRIT, 4D, 0.5D)
                .onTick(1D))
            .onTick(PartBuilder.aoeParticles(ParticleTypes.ENCHANT, 4D, 0.7D)
                .onTick(1D))
            .onTick(PartBuilder.aoeParticles(ParticleTypes.END_ROD, 4D, 0.7D)
                .onTick(1D))
            .onExpire(PartBuilder.healInAoe(SpellCalcs.SHOOTING_STAR, 2D))
            .onExpire(PartBuilder.aoeParticles(ParticleTypes.SOUL_FIRE_FLAME, 20D, 1D))
            .onExpire(PartBuilder.aoeParticles(ParticleTypes.HEART, 40D, 1.5D))
            .onExpire(PartBuilder.aoeParticles(ParticleTypes.HAPPY_VILLAGER, 40D, 1.5D))
            .onExpire(PartBuilder.groundEdgeParticles(ParticleTypes.END_ROD, 30D, 1.5D, 0.4D, ParticleMotion.Upwards))
            .build();

        SpellBuilder.of(HEALING_AURA_ID, SpellConfiguration.Builder.multiCast(15, 20 * 30, 60, 3), "Healing Aura",
                Arrays.asList(SpellTag.heal))
            .manualDesc(
                "Heal allies around you for " + SpellCalcs.HEALING_AURA.getLocDmgTooltip() +
                    " health")

            .weaponReq(CastingWeapon.ANY_WEAPON)
            .onCast(PartBuilder.playSound(SlashSounds.BUFF.get(), 1D, 1D))
            .onCast(PartBuilder.groundParticles(ParticleTypes.COMPOSTER, 80D, 2D, 0.6D))
            .onCast(PartBuilder.groundParticles(ParticleTypes.HEART, 40D, 2D, 0.6D))
            .onCast(PartBuilder.groundParticles(ParticleTypes.HAPPY_VILLAGER, 60D, 2D, 0.8D))
            .onCast(PartBuilder.groundEdgeParticles(ParticleTypes.END_ROD, 60D, 2.5D, 0.5D, ParticleMotion.Upwards))
            .onCast(PartBuilder.healInAoe(SpellCalcs.HEALING_AURA, 2D))
            .build();

        SpellBuilder.of(WISH, SpellConfiguration.Builder.instant(20, 20 * 60), "Wish",
                Arrays.asList(SpellTag.heal))
            .manualDesc(
                "Heal allies around you for " + SpellCalcs.HEALING_AURA.getLocDmgTooltip() +
                    " health")
            .weaponReq(CastingWeapon.ANY_WEAPON)
            .onCast(PartBuilder.playSound(SlashSounds.BUFF.get(), 1D, 1D))
            .onCast(PartBuilder.groundParticles(ParticleTypes.COMPOSTER, 75D, 5D, 0.6D))
            .onCast(PartBuilder.groundParticles(ParticleTypes.HEART, 90D, 5D, 0.6D))
            .onCast(PartBuilder.groundEdgeParticles(ParticleTypes.END_ROD, 100D, 5.5D, 0.5D, ParticleMotion.Upwards))
            .onCast(PartBuilder.groundParticles(ParticleTypes.HAPPY_VILLAGER, 70D, 5D, 0.8D))
            .onCast(PartBuilder.healInAoe(SpellCalcs.WISH, 5D))
            .build();

    }

    private static ComponentPart inwardEdgeParticles(ParticleType<?> particle, double count, double radius, double motionMulti) {
        return PartBuilder.Particle.builder(particle, count, radius)
            .set(MapField.PARTICLE_SHAPE, ParticleInRadiusAction.Shape.HORIZONTAL_CIRCLE_EDGE.name())
            .set(MapField.Y_RANDOM, 0.7D)
            .set(MapField.HEIGHT, 0.5D)
            .set(MapField.MOTION, ParticleMotion.InwardMotion.name())
            .set(MapField.MOTION_MULTI, motionMulti)
            .build();
    }

    static void song(String id, String name, EffectCtx effect, ParticleType<?> accent) {

        SpellBuilder.of(id, SpellConfiguration.Builder.nonInstant(10, 20 * 10, 30)
                , name,
                Arrays.asList(SpellTag.area, SpellTag.song))
            .manualDesc(
                "Give a stack of " + effect.locname + " to all allies around you."
            )
            .onCast(PartBuilder.playSound(SoundEvents.NOTE_BLOCK_CHIME, 1D, 1D))
            .onCast(PartBuilder.aoeParticles(ParticleTypes.NOTE, 120D, 3D))
            .onCast(PartBuilder.groundEdgeParticles(accent, 100D, 3.2D, 0.3D, ParticleMotion.OutwardMotion))
            .onCast(PartBuilder.aoeParticles(ParticleTypes.HAPPY_VILLAGER, 70D, 3D))
            .onCast(PartBuilder.groundEdgeParticles(ParticleTypes.END_ROD, 60D, 3D, 0.4D, ParticleMotion.Upwards))
            .onCast(PartBuilder.giveExileEffectToAlliesInRadius(5D, effect.resourcePath, 20 * 30D))
            .build();
    }
}
