package com.robertx22.age_of_exile.aoe_data.database.stats;

import com.robertx22.age_of_exile.aoe_data.database.stat_effects.StatEffects;
import com.robertx22.age_of_exile.aoe_data.database.stats.base.DatapackStatBuilder;
import com.robertx22.age_of_exile.aoe_data.database.stats.base.EmptyAccessor;
import com.robertx22.age_of_exile.database.data.stats.Stat;
import com.robertx22.age_of_exile.database.data.stats.datapacks.test.DataPackStatAccessor;
import com.robertx22.age_of_exile.uncommon.effectdatas.OnMobKilledByDamageEvent;
import com.robertx22.age_of_exile.uncommon.enumclasses.Elements;
import com.robertx22.age_of_exile.uncommon.interfaces.EffectSides;
import com.robertx22.library_of_exile.registry.ExileRegistryInit;

import static com.robertx22.age_of_exile.database.data.stats.Stat.VAL1;
import static com.robertx22.age_of_exile.database.data.stats.Stat.format;

/**
 * Статы древа «Божественный» (data/mmorpg/talent_tree/divine.json), которые срабатывают на убийстве
 * врага — ветка некроманта.
 * <p>
 * Пассивные статы призыва (урон/здоровье/время жизни прислужников, лимиты, вид зверя) лежат в
 * {@link com.robertx22.age_of_exile.database.data.stats.types.summoning.SummonStat}.
 * <p>
 * Регистрация: этот класс создаётся в {@code GeneratedData} до {@code new Stats().registerAll()},
 * которая и добавляет собранные датапак-статы в сериализацию.
 */
public class DivineStats implements ExileRegistryInit {

    public static void loadClass() {

    }

    /** Шанс поднять убитого врага в виде нежити-прислужника. */
    public static DataPackStatAccessor<EmptyAccessor> RAISE_DEAD_CHANCE = DatapackStatBuilder
        .ofSingle("raise_dead_chance", Elements.Physical)
        .worksWithEvent(OnMobKilledByDamageEvent.ID)
        .setPriority(0)
        .setSide(EffectSides.Source)
        .addEffect(StatEffects.RAISE_DEAD)
        .setLocName(x -> format(VAL1 + "% шанс поднять убитого врага в виде нежити"))
        .setLocDesc(x -> "Поднятый мертвец сражается на вашей стороне, пока не рассеется.")
        .modifyAfterDone(x -> {
            x.is_perc = true;
            x.min = 0;
            x.max = 100;
            x.is_long = true;
        })
        .build();

    /** Взрыв трупа: процент от здоровья убитого врага физическим уроном вокруг него. */
    public static DataPackStatAccessor<EmptyAccessor> CORPSE_EXPLOSION = DatapackStatBuilder
        .ofSingle("corpse_explosion", Elements.Physical)
        .worksWithEvent(OnMobKilledByDamageEvent.ID)
        .setPriority(0)
        .setSide(EffectSides.Source)
        .addEffect(StatEffects.EXPLODE_CORPSE)
        .setLocName(x -> format("Убитые вами враги взрываются, нанося " + VAL1 + "% своего здоровья уроном вокруг"))
        .setLocDesc(x -> "Урон считается от здоровья трупа и проходит через обычные правила урона.")
        .modifyAfterDone(x -> {
            x.is_perc = true;
            x.min = 0;
            x.is_long = true;
        })
        .build();

    @Override
    public void registerAll() {
        loadClass();
    }
}
