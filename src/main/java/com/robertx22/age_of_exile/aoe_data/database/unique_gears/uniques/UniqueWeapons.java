package com.robertx22.age_of_exile.aoe_data.database.unique_gears.uniques;

import com.robertx22.age_of_exile.aoe_data.database.base_gear_types.BaseGearTypes;
import com.robertx22.age_of_exile.aoe_data.database.stats.Stats;
import com.robertx22.age_of_exile.aoe_data.database.unique_gears.UniqueGearBuilder;
import com.robertx22.age_of_exile.database.data.StatModifier;
import com.robertx22.age_of_exile.database.data.stats.types.offense.SpellDamage;
import com.robertx22.age_of_exile.database.data.stats.types.resources.health.Health;
import com.robertx22.age_of_exile.database.data.stats.types.resources.mana.ManaRegen;
import com.robertx22.age_of_exile.saveclasses.unit.ResourceType;
import com.robertx22.age_of_exile.uncommon.enumclasses.Elements;
import com.robertx22.age_of_exile.uncommon.enumclasses.ModType;
import com.robertx22.age_of_exile.uncommon.enumclasses.WeaponTypes;
import com.robertx22.library_of_exile.registry.ExileRegistryInit;

import java.util.Arrays;

/**
 * Уникальное оружие.
 *
 * <p>До этой правки уникаты были только на кольца, амулеты и шлем: из 11 дропаемых
 * предметов ни одного оружия, то есть «главный» слот персонажа не имел ни одной
 * находки-цели. Здесь четыре оружия — по одному на каждый стиль (меч, топор, лук,
 * посох), каждое с узнаваемой ролью: крит-меч, площадной топор, дальний лук,
 * магический посох.
 */
public class UniqueWeapons implements ExileRegistryInit {

    @Override
    public void registerAll() {

        // Крит-меч: «нашёл — и удары стали другими».
        UniqueGearBuilder.of(
                "bloodletter",
                "Bloodletter",
                BaseGearTypes.SWORD)
            .setReplacesName()
            .stats(Arrays.asList(
                new StatModifier(6, 12, Stats.WEAPON_DAMAGE.get(WeaponTypes.sword), ModType.FLAT),
                new StatModifier(4, 7, Stats.CRIT_CHANCE.get(), ModType.FLAT),
                new StatModifier(25, 45, Stats.CRIT_DAMAGE.get(), ModType.FLAT),
                new StatModifier(4, 8, Stats.RESOURCE_ON_KILL.get(ResourceType.health), ModType.FLAT),
                new StatModifier(-4, -2, Health.getInstance(), ModType.PERCENT)
            ))
            .devComment("crit sword, pays with % hp")
            .build();

        // Площадной топор для толпы: медленнее, но бьёт по всем.
        UniqueGearBuilder.of(
                "worldsplitter",
                "Worldsplitter",
                BaseGearTypes.AXE)
            .setReplacesName()
            .stats(Arrays.asList(
                new StatModifier(8, 16, Stats.WEAPON_DAMAGE.get(WeaponTypes.axe), ModType.FLAT),
                new StatModifier(15, 30, Stats.AREA_DAMAGE.get(), ModType.FLAT),
                new StatModifier(10, 18, Stats.ELEMENTAL_DAMAGE.get(Elements.Earth), ModType.FLAT),
                new StatModifier(-8, -4, Stats.ATTACK_SPEED.get(), ModType.FLAT)
            ))
            .devComment("area axe for packs")
            .build();

        // Лук: дальний стиль, урон снарядов и скорость.
        UniqueGearBuilder.of(
                "whisperwind",
                "Whisperwind",
                BaseGearTypes.BOW)
            .setReplacesName()
            .stats(Arrays.asList(
                new StatModifier(5, 10, Stats.WEAPON_DAMAGE.get(WeaponTypes.bow), ModType.FLAT),
                new StatModifier(20, 40, Stats.PROJECTILE_DAMAGE.get(), ModType.FLAT),
                new StatModifier(5, 12, Stats.ATTACK_SPEED.get(), ModType.FLAT),
                new StatModifier(8, 15, Stats.CRIT_DAMAGE.get(), ModType.FLAT)
            ))
            .devComment("ranged speed bow")
            .build();

        // Посох: магия, мана и стихийный урон заклинаний.
        UniqueGearBuilder.of(
                "starfall",
                "Starfall",
                BaseGearTypes.STAFF)
            .setReplacesName()
            .stats(Arrays.asList(
                new StatModifier(15, 30, SpellDamage.getInstance(), ModType.FLAT),
                new StatModifier(12, 25, Stats.ELEMENTAL_SPELL_DAMAGE.get(Elements.Elemental), ModType.FLAT),
                new StatModifier(6, 12, ManaRegen.getInstance(), ModType.PERCENT),
                new StatModifier(5, 10, Stats.SPELL_CRIT_CHANCE.get(), ModType.FLAT),
                new StatModifier(-6, -3, Health.getInstance(), ModType.PERCENT)
            ))
            .devComment("glass cannon caster staff")
            .build();
    }
}
