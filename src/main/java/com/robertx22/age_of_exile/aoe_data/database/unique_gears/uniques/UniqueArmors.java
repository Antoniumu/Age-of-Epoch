package com.robertx22.age_of_exile.aoe_data.database.unique_gears.uniques;

import com.robertx22.age_of_exile.aoe_data.database.base_gear_types.BaseGearTypes;
import com.robertx22.age_of_exile.aoe_data.database.stats.Stats;
import com.robertx22.age_of_exile.aoe_data.database.unique_gears.UniqueGearBuilder;
import com.robertx22.age_of_exile.aoe_data.database.unique_gears.uniques.armors.HelmetUniques;
import com.robertx22.age_of_exile.database.data.StatModifier;
import com.robertx22.age_of_exile.database.data.stats.types.defense.Armor;
import com.robertx22.age_of_exile.database.data.stats.types.defense.DodgeRating;
import com.robertx22.age_of_exile.database.data.stats.types.generated.ElementalResist;
import com.robertx22.age_of_exile.database.data.stats.types.resources.health.Health;
import com.robertx22.age_of_exile.uncommon.enumclasses.Elements;
import com.robertx22.age_of_exile.uncommon.enumclasses.ModType;
import com.robertx22.library_of_exile.registry.ExileRegistryInit;

import java.util.Arrays;

/**
 * Уникальная броня.
 *
 * <p>Раньше на всю игру был один уникальный шлем и ни одного уникального нагрудника,
 * поножей или ботинок — три из четырёх слотов брони не имели «находки-цели».
 * Здесь по одному предмету на слот, с разными ролями: танк, уклонение, сопротивление.
 */
public class UniqueArmors implements ExileRegistryInit {

    @Override
    public void registerAll() {
        new HelmetUniques().registerAll();

        UniqueGearBuilder.of(
                "iron_bulwark",
                "Iron Bulwark",
                BaseGearTypes.CHEST)
            .setReplacesName()
            .stats(Arrays.asList(
                new StatModifier(25, 45, Armor.getInstance(), ModType.PERCENT),
                new StatModifier(10, 20, Health.getInstance(), ModType.PERCENT),
                new StatModifier(6, 12, new ElementalResist(Elements.Elemental), ModType.PERCENT),
                new StatModifier(-10, -5, DodgeRating.getInstance(), ModType.PERCENT)
            ))
            .devComment("tank chest, slow and heavy")
            .build();

        UniqueGearBuilder.of(
                "swift_stride",
                "Swift Stride",
                BaseGearTypes.PANTS)
            .setReplacesName()
            .stats(Arrays.asList(
                new StatModifier(20, 35, DodgeRating.getInstance(), ModType.PERCENT),
                new StatModifier(5, 10, Stats.ATTACK_SPEED.get(), ModType.FLAT),
                new StatModifier(8, 15, Health.getInstance(), ModType.PERCENT),
                new StatModifier(-20, -10, Armor.getInstance(), ModType.PERCENT)
            ))
            .devComment("dodge pants")
            .build();

        UniqueGearBuilder.of(
                "pathfinder",
                "Pathfinder",
                BaseGearTypes.BOOTS)
            .setReplacesName()
            .stats(Arrays.asList(
                new StatModifier(6, 12, new ElementalResist(Elements.Fire), ModType.PERCENT),
                new StatModifier(6, 12, new ElementalResist(Elements.Water), ModType.PERCENT),
                new StatModifier(6, 12, new ElementalResist(Elements.Earth), ModType.PERCENT),
                new StatModifier(5, 10, DodgeRating.getInstance(), ModType.PERCENT),
                new StatModifier(5, 10, Health.getInstance(), ModType.PERCENT)
            ))
            .devComment("all-res boots")
            .build();
    }
}
