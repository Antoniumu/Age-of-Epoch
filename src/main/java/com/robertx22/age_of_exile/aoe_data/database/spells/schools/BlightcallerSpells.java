package com.robertx22.age_of_exile.aoe_data.database.spells.schools;

import com.robertx22.age_of_exile.aoe_data.database.exile_effects.adders.NegativeEffects;
import com.robertx22.age_of_exile.aoe_data.database.spells.PartBuilder;
import com.robertx22.age_of_exile.aoe_data.database.spells.SpellBuilder;
import com.robertx22.age_of_exile.aoe_data.database.spells.SpellCalcs;
import com.robertx22.age_of_exile.database.data.spells.SpellTag;
import com.robertx22.age_of_exile.database.data.spells.components.SpellConfiguration;
import com.robertx22.age_of_exile.database.data.spells.components.actions.SpellAction;
import com.robertx22.age_of_exile.database.data.spells.map_fields.MapField;
import com.robertx22.age_of_exile.mmorpg.registers.common.SlashEntities;
import com.robertx22.age_of_exile.summoning.MinionUtils;
import com.robertx22.age_of_exile.uncommon.enumclasses.Elements;
import com.robertx22.library_of_exile.registry.ExileRegistryInit;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.Arrays;

/**
 * Школа «Скверноводец». Открывается с 50 уровня: все заклинания стоят в ряду 6 (50 уровень).
 * Иконки: textures/gui/spells/icons/<id>.png
 */
public class BlightcallerSpells implements ExileRegistryInit {

    public static String RAISE_DEAD = "bc_raise_dead";
    public static String CORPSE_BURST = "bc_corpse_burst";
    public static String WOLF_PACK = "bc_wolf_pack";
    public static String PLAGUE_CLOUD = "bc_plague_cloud";
    public static String BLIGHT_BOLT = "bc_blight_bolt";

    @Override
    public void registerAll() {

        // Пробуждение павших: призывает нежить (лимит — стат MAX_UNDEAD_MINIONS)
        SpellBuilder.of(RAISE_DEAD, SpellConfiguration.Builder.instant(15, 20 * 20)
                .setScaleManaToPlayer(), "Raise the Fallen", Arrays.asList(SpellTag.damage))
            .manualDesc("Raises an undead minion to fight for you.")
            .onCast(PartBuilder.playSound(SoundEvents.ILLUSIONER_CAST_SPELL, 1D, 0.8D))
            .onCast(PartBuilder.justAction(SpellAction.SUMMON_MINION.create(MinionUtils.Kind.UNDEAD.id)))
            .build();

        // Взрыв останков: урон по области вокруг кастера
        SpellBuilder.of(CORPSE_BURST, SpellConfiguration.Builder.instant(12, 20 * 12)
                .setSwingArm(), "Corpse Burst", Arrays.asList(SpellTag.area, SpellTag.damage))
            .manualDesc("Bursts the remains around you, dealing " + SpellCalcs.CURSE.getLocDmgTooltip()
                + " " + Elements.Elemental.getIconNameDmg() + " to nearby enemies.")
            .onCast(PartBuilder.playSound(SoundEvents.WITHER_SKELETON_HURT, 1D, 0.7D))
            .onCast(PartBuilder.damageInAoe(SpellCalcs.CURSE, Elements.Elemental, 4D))
            .build();

        // Стая волков: призывает зверей (лимит — стат MAX_BEAST_MINIONS)
        SpellBuilder.of(WOLF_PACK, SpellConfiguration.Builder.instant(20, 20 * 30)
                .setScaleManaToPlayer(), "Wolf Pack", Arrays.asList(SpellTag.damage))
            .manualDesc("Calls a pack of wolves to fight beside you.")
            .onCast(PartBuilder.playSound(SoundEvents.ILLUSIONER_CAST_SPELL, 1D, 1D))
            .onCast(PartBuilder.justAction(SpellAction.SUMMON_MINION.create(MinionUtils.Kind.WOLF.id)))
            .onCast(PartBuilder.justAction(SpellAction.SUMMON_MINION.create(MinionUtils.Kind.WOLF.id)))
            .onCast(PartBuilder.justAction(SpellAction.SUMMON_MINION.create(MinionUtils.Kind.WOLF.id)))
            .build();

        // Облако скверны: проклятое облако, яд и урон по области
        SpellBuilder.of(PLAGUE_CLOUD, SpellConfiguration.Builder.nonInstant(14, 20 * 25, 20)
                .setSwingArm(), "Plague Cloud", Arrays.asList(SpellTag.area, SpellTag.curse))
            .manualDesc("Creates a cloud of blight that poisons enemies inside it.")
            .onCast(PartBuilder.justAction(SpellAction.SUMMON_AT_SIGHT.create(SlashEntities.SIMPLE_PROJECTILE.get(), 1D, 0D)))
            .onExpire(PartBuilder.justAction(SpellAction.SUMMON_BLOCK.create(Blocks.AIR, 1D)
                .put(MapField.ENTITY_NAME, "block")
                .put(MapField.BLOCK_FALL_SPEED, 0D)
                .put(MapField.FIND_NEAREST_SURFACE, false)
                .put(MapField.IS_BLOCK_FALLING, false)))
            .onExpire("block", PartBuilder.damageInAoe(SpellCalcs.POISON_CLOUD, Elements.Earth, 3D))
            .onExpire("block", PartBuilder.addExileEffectToEnemiesInAoe(NegativeEffects.POISON.resourcePath, 3D, 20 * 10D)
                .addPerEntityHit(
                    PartBuilder.justAction(SpellAction.PARTICLES_IN_RADIUS.create(ParticleTypes.SMOKE, 40D, 0.3D)
                        .put(MapField.HEIGHT, 2.2D)
                    )))
            .build();

        // Тёмный снаряд: летит вперёд и бьёт по первой цели
        SpellBuilder.of(BLIGHT_BOLT, SpellConfiguration.Builder.instant(10, 20 * 4)
                .setSwingArm(), "Blight Bolt", Arrays.asList(SpellTag.projectile, SpellTag.damage))
            .manualDesc("Fires a bolt of cursed energy that damages the first enemy it hits.")
            .onCast(PartBuilder.justAction(SpellAction.SUMMON_PROJECTILE.create(Items.AIR, 1D, 2D, SlashEntities.SIMPLE_PROJECTILE.get(), 20D, false)
                .put(MapField.IS_SILENT, true)
            ))
            .onHit(PartBuilder.damage(SpellCalcs.POISON_BALL, Elements.Elemental))
            .build();
    }
}
