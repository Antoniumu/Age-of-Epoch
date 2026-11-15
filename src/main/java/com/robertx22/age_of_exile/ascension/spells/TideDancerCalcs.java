package com.robertx22.age_of_exile.ascension.spells;

import com.robertx22.age_of_exile.database.data.value_calc.ValueCalcBuilder;
import com.robertx22.age_of_exile.database.data.value_calc.ValueCalculation;

/**
 * Уникальные калькуляторы «Танцора Прилива». Вознесённые классы — ультимативная
 * сила поздней игры: коэффициенты заметно выше любых школьных заклинаний
 * (для сравнения, лучшие школьные AoE ~0.5-1.9 от атаки; тут всплеск до 3.0).
 */
public class TideDancerCalcs {

    public static void init() {
    }

    // Кнут Гребня — хлёсткая дуга: смесь атаки оружием и силы волн.
    public static ValueCalculation WHIP_LASH = ValueCalcBuilder.of("tide_whip_lash")
        .attackScaling(0.9F, 1.7F)
        .spellScaling(0.6F, 1.3F)
        .build();

    // Скользящий Бурун — урон всем, сквозь кого прошёл рывок.
    public static ValueCalculation SLIPSTREAM = ValueCalcBuilder.of("tide_slipstream")
        .attackScaling(0.7F, 1.3F)
        .build();

    // Вуаль Ликования — объём щита из спрессованной воды.
    public static ValueCalculation VEIL_SHIELD = ValueCalcBuilder.of("tide_veil_shield")
        .baseValue(35, 160)
        .spellScaling(0.5F, 1.1F)
        .build();

    // Хватка Девятого Вала — стягивание + удар водобоя.
    public static ValueCalculation UNDERTOW = ValueCalcBuilder.of("tide_undertow")
        .attackScaling(0.8F, 1.6F)
        .spellScaling(0.6F, 1.4F)
        .build();

    // Вальс Пучины — тики водоворота.
    public static ValueCalculation MAELSTROM_TICK = ValueCalcBuilder.of("tide_maelstrom_tick")
        .spellScaling(0.5F, 1.0F)
        .attackScaling(0.3F, 0.6F)
        .build();

    // Вальс Пучины — финальный разрыв (ультимативный всплеск).
    public static ValueCalculation MAELSTROM_BURST = ValueCalcBuilder.of("tide_maelstrom_burst")
        .attackScaling(1.6F, 3.0F)
        .spellScaling(1.0F, 2.2F)
        .build();

    // Несдержанная Радость — волна эйфории при активации.
    public static ValueCalculation JOY_SURGE = ValueCalcBuilder.of("tide_joy_surge")
        .baseValue(25, 90)
        .spellScaling(0.35F, 0.8F)
        .build();
}
