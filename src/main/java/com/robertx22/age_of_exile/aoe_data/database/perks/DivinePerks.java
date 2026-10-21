package com.robertx22.age_of_exile.aoe_data.database.perks;

import com.robertx22.age_of_exile.aoe_data.database.stats.DivineStats;
import com.robertx22.age_of_exile.database.OptScaleExactStat;
import com.robertx22.age_of_exile.database.data.perks.Perk;
import com.robertx22.age_of_exile.database.data.stats.types.summoning.SummonStat;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.age_of_exile.uncommon.enumclasses.ModType;
import com.robertx22.library_of_exile.registry.ExileRegistryInit;
import net.minecraft.resources.Identifier;

import java.util.Arrays;

/**
 * Перки древа «Божественный» — {@code data/mmorpg/talent_tree/divine.json}.
 * <p>
 * Страница поделена на три части:
 * <ul>
 * <li><b>слева</b> — ветка некроманта (игра от призыва убитых врагов);</li>
 * <li><b>в центре</b> — общие перки, нужные обоим билдам;</li>
 * <li><b>справа</b> — ветка призывателя (постоянные звери: стая волков, медведь или боевой мамонт).</li>
 * </ul>
 * Ветки взаимоисключающие: у всех их узлов стоит {@link Perk#exclusive_branch}, поэтому взять
 * некроманта и призывателя одновременно нельзя (см. {@code Perk.isLockedToPlayer}). Внутри ветки
 * призывателя вид зверя выбирается одним из трёх ключевых талантов ({@code one_of_a_kind}).
 * <p>
 * Сами механики — в {@code com.robertx22.age_of_exile.summoning.MinionUtils}.
 */
public class DivinePerks implements ExileRegistryInit {

    /** Ид ветки некроманта (для {@link Perk#exclusive_branch}). */
    public static final String BRANCH_NECROMANCER = "necromancer";
    /** Ид ветки призывателя (для {@link Perk#exclusive_branch}). */
    public static final String BRANCH_SUMMONER = "summoner";
    /** Одновременно можно держать только один вид зверей. */
    public static final String ONE_OF_A_KIND_BEAST = "divine_beast";

    @Override
    public void registerAll() {

        // ======================= ОБЩИЕ (центр страницы) =======================

        perk("div_shared_damage", "Узы власти", Perk.PerkType.STAT, "textures/gui/stat_icons/misc/magic_dmg.png",
            new OptScaleExactStat(8, SummonStat.SUMMON_DAMAGE, ModType.FLAT));

        perk("div_shared_health", "Духовная связь", Perk.PerkType.STAT, "textures/gui/stat_icons/main/health.png",
            new OptScaleExactStat(10, SummonStat.SUMMON_HEALTH, ModType.FLAT));

        perk("div_shared_duration", "Долгий срок", Perk.PerkType.STAT, "textures/gui/stat_icons/misc/cast_speed.png",
            new OptScaleExactStat(15, SummonStat.SUMMON_DURATION, ModType.FLAT));

        perk("div_shared_armor", "Костяной панцирь", Perk.PerkType.STAT, "textures/gui/stat_icons/main/armor.png",
            new OptScaleExactStat(8, SummonStat.SUMMON_ARMOR, ModType.FLAT).scale());

        perk("div_shared_speed", "Резвость слуг", Perk.PerkType.STAT, "textures/gui/stat_icons/misc/attack_speed.png",
            new OptScaleExactStat(8, SummonStat.SUMMON_SPEED, ModType.FLAT));

        level(perk("div_shared_damage_big", "Воля хозяина", Perk.PerkType.SPECIAL, "textures/gui/talent_icons/vessel.png",
            new OptScaleExactStat(15, SummonStat.SUMMON_DAMAGE, ModType.FLAT),
            new OptScaleExactStat(5, SummonStat.SUMMON_HEALTH, ModType.FLAT)), 20);

        level(perk("div_shared_health_big", "Плоть и кость", Perk.PerkType.SPECIAL, "textures/gui/talent_icons/mighty_shield.png",
            new OptScaleExactStat(18, SummonStat.SUMMON_HEALTH, ModType.FLAT),
            new OptScaleExactStat(10, SummonStat.SUMMON_ARMOR, ModType.FLAT).scale()), 20);

        level(perk("divine_command", "Власть над слугами", Perk.PerkType.MAJOR, "textures/gui/talent_icons/oath.png",
            new OptScaleExactStat(12, SummonStat.SUMMON_DAMAGE, ModType.FLAT),
            new OptScaleExactStat(12, SummonStat.SUMMON_HEALTH, ModType.FLAT),
            new OptScaleExactStat(20, SummonStat.SUMMON_DURATION, ModType.FLAT)), 30);

        // ======================= НЕКРОМАНТ (левая часть) =======================

        Perk necromancer = level(perk("divine_necromancer", "Путь некроманта", Perk.PerkType.MAJOR, "textures/gui/talent_icons/necronomicon.png",
            new OptScaleExactStat(15, DivineStats.RAISE_DEAD_CHANCE.get(), ModType.FLAT),
            new OptScaleExactStat(1, SummonStat.MAX_UNDEAD_MINIONS, ModType.FLAT)), 1);
        necromancer.is_entry = true;
        necromancer.exclusive_branch = BRANCH_NECROMANCER;

        branch(perk("div_necro_raise", "Восстание мёртвых", Perk.PerkType.STAT, "textures/gui/stat_icons/misc/magic_dmg.png",
            new OptScaleExactStat(10, DivineStats.RAISE_DEAD_CHANCE.get(), ModType.FLAT)), BRANCH_NECROMANCER);

        branch(perk("div_necro_legion", "Легион", Perk.PerkType.STAT, "textures/gui/stat_icons/misc/area_dmg.png",
            new OptScaleExactStat(1, SummonStat.MAX_UNDEAD_MINIONS, ModType.FLAT)), BRANCH_NECROMANCER);

        branch(perk("div_necro_fury", "Холодная ярость", Perk.PerkType.STAT, "textures/gui/stat_icons/misc/melee_dmg.png",
            new OptScaleExactStat(12, SummonStat.UNDEAD_MINION_DAMAGE, ModType.FLAT)), BRANCH_NECROMANCER);

        branch(perk("div_necro_flesh", "Гнилая плоть", Perk.PerkType.STAT, "textures/gui/stat_icons/main/health.png",
            new OptScaleExactStat(12, SummonStat.SUMMON_HEALTH, ModType.FLAT)), BRANCH_NECROMANCER);

        branch(level(perk("div_necro_lord", "Властелин мертвецов", Perk.PerkType.SPECIAL, "textures/gui/talent_icons/big_heart_of_dark.png",
            new OptScaleExactStat(20, SummonStat.UNDEAD_MINION_DAMAGE, ModType.FLAT),
            new OptScaleExactStat(10, SummonStat.SUMMON_DAMAGE, ModType.FLAT)), 25), BRANCH_NECROMANCER);

        branch(level(perk("div_necro_corpse", "Взрыв трупа", Perk.PerkType.SPECIAL, "textures/gui/talent_icons/bone_collector.png",
            new OptScaleExactStat(25, DivineStats.CORPSE_EXPLOSION.get(), ModType.FLAT)), 20), BRANCH_NECROMANCER);

        branch(level(perk("div_necro_chain_death", "Цепная смерть", Perk.PerkType.SPECIAL, "textures/gui/talent_icons/bone_grinder.png",
            new OptScaleExactStat(35, DivineStats.CORPSE_EXPLOSION.get(), ModType.FLAT),
            new OptScaleExactStat(8, DivineStats.RAISE_DEAD_CHANCE.get(), ModType.FLAT)), 30), BRANCH_NECROMANCER);

        // ======================= ПРИЗЫВАТЕЛЬ (правая часть) =======================

        Perk summoner = level(perk("divine_summoner", "Путь призывателя", Perk.PerkType.MAJOR, "textures/gui/talent_icons/pack_leader.png",
            new OptScaleExactStat(1, SummonStat.MAX_BEAST_MINIONS, ModType.FLAT),
            new OptScaleExactStat(10, SummonStat.BEAST_MINION_DAMAGE, ModType.FLAT)), 1);
        summoner.is_entry = true;
        summoner.exclusive_branch = BRANCH_SUMMONER;

        beast(perk("div_summ_wolves", "Стая волков", Perk.PerkType.MAJOR, "textures/gui/talent_icons/wolf.png",
            new OptScaleExactStat(3, SummonStat.MAX_BEAST_MINIONS, ModType.FLAT),
            new OptScaleExactStat(1, SummonStat.BEAST_PACK_WOLVES, ModType.FLAT)), 5);

        beast(perk("div_summ_bear", "Медведь-хранитель", Perk.PerkType.MAJOR, "textures/gui/talent_icons/bear.png",
            new OptScaleExactStat(1, SummonStat.MAX_BEAST_MINIONS, ModType.FLAT),
            new OptScaleExactStat(25, SummonStat.SUMMON_HEALTH, ModType.FLAT),
            new OptScaleExactStat(1, SummonStat.BEAST_PACK_BEAR, ModType.FLAT)), 5);

        beast(perk("div_summ_mammoth", "Боевой мамонт", Perk.PerkType.MAJOR, "textures/gui/talent_icons/predator.png",
            new OptScaleExactStat(1, SummonStat.MAX_BEAST_MINIONS, ModType.FLAT),
            new OptScaleExactStat(50, SummonStat.SUMMON_HEALTH, ModType.FLAT),
            new OptScaleExactStat(1, SummonStat.BEAST_PACK_MAMMOTH, ModType.FLAT)), 25);

        branch(perk("div_summ_fangs", "Звериный оскал", Perk.PerkType.STAT, "textures/gui/stat_icons/misc/melee_dmg.png",
            new OptScaleExactStat(12, SummonStat.BEAST_MINION_DAMAGE, ModType.FLAT)), BRANCH_SUMMONER);

        branch(perk("div_summ_paws", "Быстрые лапы", Perk.PerkType.STAT, "textures/gui/stat_icons/misc/attack_speed.png",
            new OptScaleExactStat(10, SummonStat.SUMMON_SPEED, ModType.FLAT)), BRANCH_SUMMONER);

        branch(perk("div_summ_hide", "Густая шерсть", Perk.PerkType.STAT, "textures/gui/stat_icons/main/armor.png",
            new OptScaleExactStat(10, SummonStat.SUMMON_ARMOR, ModType.FLAT).scale()), BRANCH_SUMMONER);

        branch(level(perk("div_summ_alpha", "Вожак стаи", Perk.PerkType.SPECIAL, "textures/gui/talent_icons/unleashed.png",
            new OptScaleExactStat(25, SummonStat.BEAST_MINION_DAMAGE, ModType.FLAT),
            new OptScaleExactStat(10, SummonStat.SUMMON_DAMAGE, ModType.FLAT)), 25), BRANCH_SUMMONER);
    }

    private static Perk perk(String id, String locname, Perk.PerkType type, String iconPath, OptScaleExactStat... stats) {
        Perk perk = new Perk();
        perk.identifier = id;
        perk.locname = locname;
        perk.type = type;
        perk.icon = Identifier.fromNamespaceAndPath(SlashRef.MODID, iconPath)
            .toString();
        perk.stats = Arrays.asList(stats);
        perk.addToSerializables();
        return perk;
    }

    private static Perk level(Perk perk, int lvl_req) {
        perk.lvl_req = lvl_req;
        return perk;
    }

    private static Perk branch(Perk perk, String branch) {
        perk.exclusive_branch = branch;
        return perk;
    }

    private static Perk beast(Perk perk, int lvl_req) {
        perk.one_of_a_kind = ONE_OF_A_KIND_BEAST;
        perk.exclusive_branch = BRANCH_SUMMONER;
        perk.lvl_req = lvl_req;
        return perk;
    }
}
