package com.robertx22.age_of_exile.database.data.stats.types.summoning;

import com.robertx22.age_of_exile.database.data.stats.Stat;
import com.robertx22.age_of_exile.uncommon.enumclasses.Elements;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Характеристики призыва. Всё, что нужно древу «Божественный»
 * (data/mmorpg/talent_tree/divine.json) и классу {@code com.robertx22.age_of_exile.summoning.MinionUtils}.
 * <p>
 * Общие характеристики (центр древа) действуют на любого прислужника, веточные — только на свою
 * ветку: некромантия поднимает убитых врагов, призыватель держит рядом зверей.
 * <p>
 * Это «старый» тип стата (как {@code DodgeRating}): без событий и эффектов, значения читаются
 * вручную через {@code Load.Unit(player).getUnit().getCalculatedStat(stat)}. Регистрируются в
 * {@code StatsRegister}.
 * <p>
 * Два стата некроманта, которые срабатывают на убийстве («шанс поднять мёртвых» и «взрыв трупа»),
 * сделаны датапак-статами с событиями и эффектами — см.
 * {@code com.robertx22.age_of_exile.aoe_data.database.stats.DivineStats}.
 */
public class SummonStat extends Stat {

    // ================= ОБЩИЕ (центр древа «Божественный») =================

    public static final SummonStat SUMMON_DAMAGE = of("summon_damage",
        "Урон прислужников",
        "Урон всех ваших прислужников.",
        true);

    public static final SummonStat SUMMON_HEALTH = of("summon_health",
        "Здоровье прислужников",
        "Здоровье всех ваших прислужников.",
        true);

    public static final SummonStat SUMMON_DURATION = of("summon_duration",
        "Время жизни прислужников",
        "Как долго живут ваши прислужники, прежде чем рассеяться.",
        true);

    public static final SummonStat SUMMON_SPEED = of("summon_speed",
        "Скорость прислужников",
        "Скорость передвижения всех ваших прислужников.",
        true);

    public static final SummonStat SUMMON_ARMOR = of("summon_armor",
        "Броня прислужников",
        "Броня всех ваших прислужников.",
        false);

    // ================= ВЕТКА НЕКРОМАНТА (левая часть древа) =================

    public static final SummonStat MAX_UNDEAD_MINIONS = of("max_undead_minions",
        "Предел нежити",
        "Сколько поднятых мертвецов может следовать за вами одновременно.",
        false);

    public static final SummonStat UNDEAD_MINION_DAMAGE = of("undead_minion_damage",
        "Урон нежити",
        "Урон поднятых вами мертвецов.",
        true);

    // ================= ВЕТКА ПРИЗЫВАТЕЛЯ (правая часть древа) =================

    public static final SummonStat MAX_BEAST_MINIONS = of("max_beast_minions",
        "Предел зверей",
        "Сколько призванных зверей держится рядом с вами.",
        false);

    public static final SummonStat BEAST_MINION_DAMAGE = of("beast_minion_damage",
        "Урон зверей",
        "Урон призванных вами зверей.",
        true);

    /** Маркер: призывать стаю волков. */
    public static final SummonStat BEAST_PACK_WOLVES = of("beast_pack_wolves",
        "Призыв: волки",
        "Вы призываете волков. Одновременно можно держать только один вид зверей.",
        false);

    /** Маркер: призывать медведей. */
    public static final SummonStat BEAST_PACK_BEAR = of("beast_pack_bear",
        "Призыв: медведь",
        "Вы призываете медведя. Одновременно можно держать только один вид зверей.",
        false);

    /** Маркер: призывать боевого мамонта. */
    public static final SummonStat BEAST_PACK_MAMMOTH = of("beast_pack_mammoth",
        "Призыв: мамонт",
        "Вы призываете боевого мамонта. Одновременно можно держать только один вид зверей.",
        false);

    /** Все статы призыва, регистрируются в {@code StatsRegister}. */
    public static final List<SummonStat> ALL = new ArrayList<>(Arrays.asList(
        SUMMON_DAMAGE,
        SUMMON_HEALTH,
        SUMMON_DURATION,
        SUMMON_SPEED,
        SUMMON_ARMOR,
        MAX_UNDEAD_MINIONS,
        UNDEAD_MINION_DAMAGE,
        MAX_BEAST_MINIONS,
        BEAST_MINION_DAMAGE,
        BEAST_PACK_WOLVES,
        BEAST_PACK_BEAR,
        BEAST_PACK_MAMMOTH
    ));

    private final String id;
    private final String locname;
    private final String locdesc;

    public SummonStat(String id, String locname, String locdesc, boolean is_perc) {
        this.id = id;
        this.locname = locname;
        this.locdesc = locdesc;

        this.is_perc = is_perc;
        this.min = 0;
        this.group = StatGroup.Misc;
        this.icon = "\u2620";
        this.format = com.robertx22.forge_compat.common.CompatChatFormatting.getName(ChatFormatting.LIGHT_PURPLE);
    }

    private static SummonStat of(String id, String locname, String locdesc, boolean is_perc) {
        return new SummonStat(id, locname, locdesc, is_perc);
    }

    @Override
    public Elements getElement() {
        return Elements.Physical;
    }

    @Override
    public String GUID() {
        return id;
    }

    @Override
    public String locNameForLangFile() {
        return locname;
    }

    @Override
    public String locDescForLangFile() {
        return locdesc;
    }
}
