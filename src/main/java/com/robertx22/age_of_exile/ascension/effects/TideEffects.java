package com.robertx22.age_of_exile.ascension.effects;

import com.robertx22.age_of_exile.aoe_data.database.exile_effects.ExileEffectBuilder;
import com.robertx22.age_of_exile.aoe_data.database.spells.PartBuilder;
import com.robertx22.age_of_exile.aoe_data.database.spells.SpellBuilder;
import com.robertx22.age_of_exile.aoe_data.database.stats.Stats;
import com.robertx22.age_of_exile.aoe_data.database.stats.base.EffectCtx;
import com.robertx22.age_of_exile.ascension.fx.TideFx;
import com.robertx22.age_of_exile.ascension.fx.TideShapeAction;
import com.robertx22.age_of_exile.database.data.exile_effects.EffectTags;
import com.robertx22.age_of_exile.database.data.exile_effects.EffectType;
import com.robertx22.age_of_exile.database.data.exile_effects.VanillaStatData;
import com.robertx22.age_of_exile.database.data.stats.types.defense.Armor;
import com.robertx22.age_of_exile.database.data.stats.types.defense.DodgeRating;
import com.robertx22.age_of_exile.database.data.stats.types.generated.ElementalResist;
import com.robertx22.age_of_exile.uncommon.enumclasses.Elements;
import com.robertx22.age_of_exile.uncommon.enumclasses.ModType;
import com.robertx22.library_of_exile.registry.ExileRegistryInit;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

/**
 * Эффекты Вознесённого класса «Танцор Прилива». Уникальны для этого пути:
 * сильнее обычных школьных эффектов и сопровождаются объёмными FX прилива.
 *
 * <p>Номера EffectCtx должны быть уникальны в пределах типа — ванильные
 * аддеры занимают 1..33, здесь используем 80+.
 */
public class TideEffects implements ExileRegistryInit {

    public static EffectCtx UNBOUND_JOY = new EffectCtx("unbound_joy_buff", "Несдержанная Радость", 80, Elements.Water, EffectType.beneficial);
    public static EffectCtx JUBILANT_VEIL = new EffectCtx("jubilant_veil_buff", "Вуаль Ликования", 81, Elements.Water, EffectType.beneficial);
    public static EffectCtx SUBMERGED = new EffectCtx("submerged_tide", "Утопление", 80, Elements.Water, EffectType.negative);

    @Override
    public void registerAll() {

        // Боевой транс Танцора: быстрые руки, бег по воде, кража жизни обоих типов.
        // Пока действует — за кастером курсирует двойная спираль живой воды.
        ExileEffectBuilder.of(UNBOUND_JOY)
            .stat(10, 25, Stats.ATTACK_SPEED.get(), ModType.FLAT)
            .stat(3, 6, Stats.LIFESTEAL.get(), ModType.FLAT)
            .stat(3, 6, Stats.SPELL_LIFESTEAL.get(), ModType.FLAT)
            .vanillaStat(VanillaStatData.create(Attributes.MOVEMENT_SPEED, 0.15F, ModType.PERCENT, UUID.fromString("a5e1c9d2-4b7a-4d3e-9f01-2c3d4e5f6a01")))
            .spell(SpellBuilder.forEffect()
                .onTick(TideFx.tick(6D, TideShapeAction.Shape.HELIX_DOUBLE, 4, 0.5D, 1.6D))
                .buildForEffect())
            .maxStacks(1)
            .addTags(EffectTags.offensive, EffectTags.positive)
            .build();

        // Вуаль: полупрозрачная сфера воды — уклонение, броня, толчок волн.
        ExileEffectBuilder.of(JUBILANT_VEIL)
            .stat(12, 25, DodgeRating.getInstance(), ModType.PERCENT)
            .stat(10, 20, Armor.getInstance(), ModType.PERCENT)
            .vanillaStat(VanillaStatData.create(Attributes.KNOCKBACK_RESISTANCE, 0.5F, ModType.FLAT, UUID.fromString("a5e1c9d2-4b7a-4d3e-9f01-2c3d4e5f6a02")))
            .maxStacks(1)
            .addTags(EffectTags.defensive, EffectTags.positive)
            .build();

        // Утопление: жертву тянет под воду — медлительность и брешь в защите от воды.
        ExileEffectBuilder.of(SUBMERGED)
            .stat(-20, -35, new ElementalResist(Elements.Water), ModType.FLAT)
            .vanillaStat(VanillaStatData.create(Attributes.MOVEMENT_SPEED, -0.25F, ModType.PERCENT, UUID.fromString("a5e1c9d2-4b7a-4d3e-9f01-2c3d4e5f6a03")))
            .maxStacks(1)
            .addTags(EffectTags.negative)
            .build();
    }
}
