package com.robertx22.age_of_exile.ascension.spells;

import com.robertx22.age_of_exile.aoe_data.database.spells.PartBuilder;
import com.robertx22.age_of_exile.aoe_data.database.spells.SpellBuilder;
import com.robertx22.age_of_exile.ascension.AscensionClasses;
import com.robertx22.age_of_exile.ascension.effects.TideEffects;
import com.robertx22.age_of_exile.ascension.fx.TideFx;
import com.robertx22.age_of_exile.ascension.fx.TideShapeAction;
import com.robertx22.age_of_exile.database.data.spells.SpellTag;
import com.robertx22.age_of_exile.database.data.spells.components.Spell;
import com.robertx22.age_of_exile.database.data.spells.components.SpellConfiguration;
import com.robertx22.age_of_exile.database.data.spells.components.actions.SpellAction;
import com.robertx22.age_of_exile.database.data.spells.map_fields.MapField;
import com.robertx22.age_of_exile.database.data.spells.spell_classes.CastingWeapon;
import com.robertx22.age_of_exile.mmorpg.registers.common.SlashEntities;
import com.robertx22.age_of_exile.uncommon.enumclasses.Elements;
import com.robertx22.age_of_exile.uncommon.enumclasses.PlayStyle;
import com.robertx22.age_of_exile.uncommon.utilityclasses.DashUtils;
import com.robertx22.library_of_exile.registry.ExileRegistryInit;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;

import java.util.Arrays;
import java.util.Collections;

/**
 * Навыки Вознесённого класса «Танцор Прилива» (50+). Каждое заклинание собрано
 * заново: собственные цепочки активаций, собственные калькуляторы и собственные
 * 3D-фигуры частиц {@link TideFx}. Это ультимативные умения — по цифрам и по
 * зрелищности они выше школьных заклинаний.
 *
 * <p>Изучение идёт НЕ через школы: ранги проставляет {@code AscensionLogic}
 * в {@code SpellsData.extra_lvls}, поэтому хотбар и каст движка работают как обычно.
 */
public class TideDancerSpells implements ExileRegistryInit {

    /**
     * Умение ограничено 3 рангами Вознесения (ранги проставляет AscensionLogic).
     */
    private static Spell capped(Spell s, int lvl) {
        s.max_lvl = lvl;
        return s;
    }

    @Override
    public void registerAll() {

        // 1) КНУТ ГРЕБНЯ — хлёст уводяного бича веером перед собой.
        capped(SpellBuilder.of(AscensionClasses.TideDancerSkillIds.WHIP,
                    SpellConfiguration.Builder.instant(18, 20 * 6)
                        .setSwingArm(),
                    "Whip of the Crest",
                    Arrays.asList(SpellTag.damage, SpellTag.technique))
                .attackStyle(PlayStyle.melee)
                .weaponReq(CastingWeapon.ANY_WEAPON)
                .manualDesc("Lash the living tide-whip in a wide arc before you for "
                    + TideDancerCalcs.WHIP_LASH.getLocDmgTooltip(Elements.Water)
                    + " and subject victims to Submersion. Rank empowers the arc.")
                .addEffectToTooltip(TideEffects.SUBMERGED)

                .onCast(PartBuilder.playSound(SoundEvents.TRIDENT_THROW, 1.0D, 1.25D))
                .onCast(PartBuilder.playSound(SoundEvents.PLAYER_SPLASH, 0.9D, 1.1D))

                .onCast(TideFx.cast(TideShapeAction.Shape.WHIP_ARC, 110, 3.4D, 1.1D))
                .onCast(TideFx.castBurst(TideShapeAction.Shape.WAVE_RING, 46, 1.6D, 0.35D, 0.6D))
                .onCast(PartBuilder.aoeParticles(ParticleTypes.SPLASH, 60D, 2.2D))

                .onCast(PartBuilder.damageInFront(TideDancerCalcs.WHIP_LASH, Elements.Water, 4.6D, 2.4D)
                    .addPerEntityHit(PartBuilder.playSoundPerTarget(SoundEvents.TRIDENT_HIT, 1.0D, 1.0D))
                    .addPerEntityHit(PartBuilder.aoeParticles(ParticleTypes.SPLASH, 24D, 0.8D)))

                .onCast(PartBuilder.addExileEffectToEnemiesInFront(TideEffects.SUBMERGED.resourcePath, 4.6D, 2.4D, 20D * 5))
                .build(),
            AscensionClasses.MAX_RANK);

        // 2) СКОЛЬЗЯЩИЙ БУРУН — рывок сквозь врага, два заряда, шлейф-спираль.
        capped(SpellBuilder.of(AscensionClasses.TideDancerSkillIds.DASH,
                    SpellConfiguration.Builder.instant(14, 20 * 6)
                        .setSwingArm()
                        .setChargesAndRegen(AscensionClasses.TideDancerSkillIds.DASH, 2, 20 * 9),
                    "Slipstream Surge",
                    Arrays.asList(SpellTag.movement, SpellTag.damage))
                .attackStyle(PlayStyle.melee)
                .weaponReq(CastingWeapon.ANY_WEAPON)
                .manualDesc("Ride the slipstream: surge forward, slicing every foe you pass for "
                    + TideDancerCalcs.SLIPSTREAM.getLocDmgTooltip(Elements.Water)
                    + ". Stores 2 charges.")

                .onCast(PartBuilder.playSound(SoundEvents.CHORUS_FRUIT_TELEPORT, 0.8D, 1.4D))
                .onCast(PartBuilder.playSound(SoundEvents.PLAYER_SPLASH, 1.0D, 1.3D))

                .onCast(PartBuilder.pushCaster(DashUtils.Way.FORWARDS, DashUtils.Strength.LARGE_DISTANCE))
                .onCast(PartBuilder.giveSelfEffect(MobEffects.MOVEMENT_SPEED, 20D * 3, 1D))

                .onCast(PartBuilder.damageInFront(TideDancerCalcs.SLIPSTREAM, Elements.Water, 5.5D, 2.0D)
                    .addPerEntityHit(PartBuilder.aoeParticles(ParticleTypes.SPLASH, 18D, 0.6D)))

                .onCast(TideFx.cast(TideShapeAction.Shape.HELIX_DOUBLE, 130, 1.5D, 2.3D))
                .onCast(TideFx.castBurst(TideShapeAction.Shape.WAVE_RING, 70, 2.2D, 0.4D, 0.8D))
                .build(),
            AscensionClasses.MAX_RANK);

        // 3) ВУАЛЬ ЛИКОВАНИЯ — сфера-покров: щит, броня, уклонение; враги тонут.
        capped(SpellBuilder.of(AscensionClasses.TideDancerSkillIds.VEIL,
                    SpellConfiguration.Builder.instant(30, 20 * 26)
                        .setSwingArm(),
                    "Jubilant Veil",
                    Arrays.asList(SpellTag.shield, SpellTag.area))
                .weaponReq(CastingWeapon.ANY_WEAPON)
                .manualDesc("Close yourself in a buoyant sphere for 8 seconds: shield of "
                    + TideDancerCalcs.VEIL_SHIELD.getLocDmgTooltip()
                    + ", armor and dodge of the Veil. Foes around you begin to Submerge.")
                .addEffectToTooltip(TideEffects.JUBILANT_VEIL)
                .addEffectToTooltip(TideEffects.SUBMERGED)

                .onCast(PartBuilder.playSound(SoundEvents.BEACON_ACTIVATE, 0.9D, 1.5D))
                .onCast(PartBuilder.playSound(SoundEvents.SPLASH_POTION_BREAK, 0.9D, 0.8D))

                .onCast(PartBuilder.giveShieldInRadius(2.5D, TideDancerCalcs.VEIL_SHIELD, 8D))
                .onCast(PartBuilder.giveSelfExileEffect(TideEffects.JUBILANT_VEIL, 20D * 8))
                .onCast(PartBuilder.addExileEffectToEnemiesInAoe(TideEffects.SUBMERGED.resourcePath, 3.6D, 20D * 4))

                .onCast(TideFx.castBurst(TideShapeAction.Shape.SPHERE_SHELL, 190, 2.1D, 1.0D, 1.1D))
                .onCast(TideFx.cast(TideShapeAction.Shape.TORUS, 130, 2.5D, 0.7D))
                .onCast(PartBuilder.aoeParticles(ParticleTypes.SPLASH, 50D, 2.4D))
                .build(),
            AscensionClasses.MAX_RANK);

        // 4) НЕСДЕРЖАННАЯ РАДОСТЬ — боевой транс: скорость рук и ног, кража жизни.
        capped(SpellBuilder.of(AscensionClasses.TideDancerSkillIds.JOY,
                    SpellConfiguration.Builder.instant(35, 20 * 32)
                        .setSwingArm(),
                    "Unbound Joy",
                    Collections.singletonList(SpellTag.shout))
                .weaponReq(CastingWeapon.ANY_WEAPON)
                .manualDesc("Unleash euphoria for 14 seconds: attack speed, movement and twin lifesteal surge. "
                    + "A wave of rapture restores "
                    + TideDancerCalcs.JOY_SURGE.getLocDmgTooltip()
                    + " health on cast.")
                .addEffectToTooltip(TideEffects.UNBOUND_JOY)

                .onCast(PartBuilder.playSound(SoundEvents.PLAYER_LEVELUP, 0.9D, 1.6D))
                .onCast(PartBuilder.playSound(SoundEvents.BEACON_ACTIVATE, 0.8D, 1.35D))

                .onCast(PartBuilder.giveSelfExileEffect(TideEffects.UNBOUND_JOY, 20D * 14))
                .onCast(PartBuilder.healCaster(TideDancerCalcs.JOY_SURGE))

                .onCast(TideFx.cast(TideShapeAction.Shape.PRISM_GEYSER, 150, 1.2D, 2.0D))
                .onCast(TideFx.cast(TideShapeAction.Shape.HELIX_DOUBLE, 110, 0.9D, 2.4D))
                .onCast(TideFx.castBurst(TideShapeAction.Shape.SPHERE_SHELL, 70, 1.2D, 1.0D, 0.7D))
                .build(),
            AscensionClasses.MAX_RANK);

        // 5) ХВАТКА ДЕВЯТОГО ВАЛА — водоворотная хватка: стянуть врагов к себе.
        capped(SpellBuilder.of(AscensionClasses.TideDancerSkillIds.GRASP,
                    SpellConfiguration.Builder.instant(40, 20 * 18)
                        .setSwingArm(),
                    "Maelstrom Grasp",
                    Arrays.asList(SpellTag.area, SpellTag.damage))
                .attackStyle(PlayStyle.melee)
                .weaponReq(CastingWeapon.ANY_WEAPON)
                .manualDesc("The Ninth Wave drags every foe within 6.5 blocks onto your blade and drowns them for "
                    + TideDancerCalcs.UNDERTOW.getLocDmgTooltip(Elements.Water) + ".")

                .onCast(PartBuilder.playSound(SoundEvents.END_PORTAL_SPAWN, 0.45D, 1.6D))
                .onCast(PartBuilder.playSound(SoundEvents.PLAYER_SPLASH, 1.0D, 0.7D))

                .onCast(PartBuilder.empty()
                    .enemiesInRadius(6.5D)
                    .addActions(SpellAction.TP_TARGET_TO_SELF.create()))
                .onCast(PartBuilder.damageInAoe(TideDancerCalcs.UNDERTOW, Elements.Water, 5.5D)
                    .addPerEntityHit(PartBuilder.aoeParticles(ParticleTypes.SPLASH, 22D, 0.9D)))
                .onCast(PartBuilder.addExileEffectToEnemiesInAoe(TideEffects.SUBMERGED.resourcePath, 6.5D, 20D * 6))

                .onCast(TideFx.cast(TideShapeAction.Shape.FUNNEL_VORTEX, 240, 4.6D, 2.6D))
                .onCast(TideFx.castBurst(TideShapeAction.Shape.COLLAPSE_RING, 190, 6.0D, 0.5D, 1.2D))
                .build(),
            AscensionClasses.MAX_RANK);

        // 6) АПОФЕОЗ: ВАЛЬС ПУЧИНЫ — ультимейт. Водоворот в точке взгляда:
        // затягивает, рвёт водой, и в финале пучина вспарывает себя взрывом.
        capped(SpellBuilder.of(AscensionClasses.TideDancerSkillIds.ULT,
                    SpellConfiguration.Builder.nonInstant(60, 20 * 90, 25)
                        .setSwingArm(),
                    "Apotheosis: Waltz of the Abyss",
                    Arrays.asList(SpellTag.area, SpellTag.damage))
                .weaponReq(CastingWeapon.ANY_WEAPON)
                .manualDesc("Summon the Waltz at your gaze for 6 seconds: it devours foes with vortex tick damage "
                    + TideDancerCalcs.MAELSTROM_TICK.getLocDmgTooltip(Elements.Water)
                    + " and pulls them into its eye, then ruptures for "
                    + TideDancerCalcs.MAELSTROM_BURST.getLocDmgTooltip(Elements.Water) + ".")

                .onCast(PartBuilder.playSound(SoundEvents.END_PORTAL_SPAWN, 0.9D, 0.7D))
                .onCast(TideFx.castBurst(TideShapeAction.Shape.WAVE_RING, 160, 3.2D, 0.5D, 1.0D))

                .onCast(PartBuilder.justAction(SpellAction.SUMMON_AT_SIGHT.create(SlashEntities.SIMPLE_PROJECTILE.get(), 1D, 7D)))

                .onExpire(PartBuilder.justAction(SpellAction.SUMMON_BLOCK.create(Blocks.AIR, 20D * 6)
                    .put(MapField.ENTITY_NAME, "maelstrom")
                    .put(MapField.BLOCK_FALL_SPEED, 0D)
                    .put(MapField.FIND_NEAREST_SURFACE, true)
                    .put(MapField.IS_BLOCK_FALLING, false)))

                .onTick("maelstrom", TideFx.tick(2D, TideShapeAction.Shape.FUNNEL_VORTEX, 70, 4.2D, 3.0D))
                .onTick("maelstrom", TideFx.tick(10D, TideShapeAction.Shape.COLLAPSE_RING, 90, 5.0D, 0.6D))
                .onTick("maelstrom", PartBuilder.onTickDamageInAoe(20D, TideDancerCalcs.MAELSTROM_TICK, Elements.Water, 5.0D))
                .onTick("maelstrom", PartBuilder.empty()
                    .enemiesInRadius(3.4D)
                    .addActions(SpellAction.TP_TARGET_TO_SELF.create())
                    .onTick(25D))
                .onTick("maelstrom", PartBuilder.particleOnTick(5D, ParticleTypes.SPLASH, 26D, 3.6D))

                .onExpire("maelstrom", PartBuilder.playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.0D, 0.8D))
                .onExpire("maelstrom", PartBuilder.playSound(SoundEvents.GLASS_BREAK, 1.0D, 0.9D))
                .onExpire("maelstrom", PartBuilder.damageInAoe(TideDancerCalcs.MAELSTROM_BURST, Elements.Water, 6.0D)
                    .addPerEntityHit(PartBuilder.aoeParticles(ParticleTypes.SPLASH, 40D, 1.2D)))
                .onExpire("maelstrom", TideFx.castBurst(TideShapeAction.Shape.SPHERE_SHELL, 420, 5.0D, 2.2D, 1.6D))
                .onExpire("maelstrom", TideFx.cast(TideShapeAction.Shape.PRISM_GEYSER, 220, 2.0D, 2.6D))
                .onExpire("maelstrom", PartBuilder.aoeParticles(ParticleTypes.EXPLOSION, 3D, 2.0D))
                .build(),
            AscensionClasses.MAX_RANK);
    }
}
