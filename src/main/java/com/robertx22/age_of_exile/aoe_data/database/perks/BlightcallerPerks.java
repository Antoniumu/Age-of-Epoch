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
 * Перки древа «Скверноводец» — {@code data/mmorpg/talent_tree/blightcaller.json}.
 * <p>
 * Класс устроен так же, как «Божественный»: та же страница в три части — слева ветка некроманта,
 * в центре общие перки, справа ветка призывателя. Механики общие ({@link SummonStat},
 * {@link DivineStats}, {@code MinionUtils}), отличаются названия, иконки и порядок узлов.
 * <p>
 * Ветки и вид зверя — те же ид, что у «Божественного» ({@link DivinePerks#BRANCH_NECROMANCER},
 * {@link DivinePerks#BRANCH_SUMMONER}, {@link DivinePerks#ONE_OF_A_KIND_BEAST}). Выбор и
 * блокировка считаются по всему персонажу ({@code TalentsData.getChosenExclusiveBranch}), поэтому
 * призывателя нельзя взять одновременно в двух классах, а зверь у персонажа всегда один.
 */
public class BlightcallerPerks implements ExileRegistryInit {

    @Override
    public void registerAll() {

        // ======================= ОБЩИЕ (центр страницы) =======================

        perk("bc_shared_damage", "Гниющая связь", Perk.PerkType.STAT, "textures/gui/stat_icons/misc/magic_dmg.png",
            new OptScaleExactStat(8, SummonStat.SUMMON_DAMAGE, ModType.FLAT));

        perk("bc_shared_health", "Теневая кровь", Perk.PerkType.STAT, "textures/gui/stat_icons/main/health.png",
            new OptScaleExactStat(10, SummonStat.SUMMON_HEALTH, ModType.FLAT));

        perk("bc_shared_duration", "Вечный дозор", Perk.PerkType.STAT, "textures/gui/stat_icons/misc/cast_speed.png",
            new OptScaleExactStat(15, SummonStat.SUMMON_DURATION, ModType.FLAT));

        perk("bc_shared_armor", "Чешуя скверны", Perk.PerkType.STAT, "textures/gui/stat_icons/main/armor.png",
            new OptScaleExactStat(8, SummonStat.SUMMON_ARMOR, ModType.FLAT).scale());

        perk("bc_shared_speed", "Нечестивая прыть", Perk.PerkType.STAT, "textures/gui/stat_icons/misc/attack_speed.png",
            new OptScaleExactStat(8, SummonStat.SUMMON_SPEED, ModType.FLAT));

        level(perk("bc_shared_damage_big", "Чёрная воля", Perk.PerkType.SPECIAL, "textures/gui/talent_icons/dark_will.png",
            new OptScaleExactStat(15, SummonStat.SUMMON_DAMAGE, ModType.FLAT),
            new OptScaleExactStat(5, SummonStat.SUMMON_HEALTH, ModType.FLAT)), 20);

        level(perk("bc_shared_health_big", "Закалённые кости", Perk.PerkType.SPECIAL, "textures/gui/talent_icons/bone_armor.png",
            new OptScaleExactStat(18, SummonStat.SUMMON_HEALTH, ModType.FLAT),
            new OptScaleExactStat(10, SummonStat.SUMMON_ARMOR, ModType.FLAT).scale()), 20);

        level(perk("bc_command", "Владыка скверны", Perk.PerkType.MAJOR, "textures/gui/talent_icons/blight_lord.png",
            new OptScaleExactStat(12, SummonStat.SUMMON_DAMAGE, ModType.FLAT),
            new OptScaleExactStat(12, SummonStat.SUMMON_HEALTH, ModType.FLAT),
            new OptScaleExactStat(20, SummonStat.SUMMON_DURATION, ModType.FLAT)), 30);

        // ======================= НЕКРОМАНТ (левая часть) =======================

        Perk necromancer = perk("bc_necro_entry", "Путь осквернителя", Perk.PerkType.MAJOR, "textures/gui/talent_icons/blight_vial.png",
            new OptScaleExactStat(15, DivineStats.RAISE_DEAD_CHANCE.get(), ModType.FLAT),
            new OptScaleExactStat(1, SummonStat.MAX_UNDEAD_MINIONS, ModType.FLAT));
        necromancer.is_entry = true;
        necromancer.exclusive_branch = DivinePerks.BRANCH_NECROMANCER;

        branch(perk("bc_necro_raise", "Пробуждение павших", Perk.PerkType.STAT, "textures/gui/stat_icons/misc/magic_dmg.png",
            new OptScaleExactStat(10, DivineStats.RAISE_DEAD_CHANCE.get(), ModType.FLAT)), DivinePerks.BRANCH_NECROMANCER);

        branch(perk("bc_necro_legion", "Орда мертвецов", Perk.PerkType.STAT, "textures/gui/stat_icons/misc/area_dmg.png",
            new OptScaleExactStat(1, SummonStat.MAX_UNDEAD_MINIONS, ModType.FLAT)), DivinePerks.BRANCH_NECROMANCER);

        branch(perk("bc_necro_fury", "Тлетворный гнев", Perk.PerkType.STAT, "textures/gui/stat_icons/misc/melee_dmg.png",
            new OptScaleExactStat(12, SummonStat.UNDEAD_MINION_DAMAGE, ModType.FLAT)), DivinePerks.BRANCH_NECROMANCER);

        branch(perk("bc_necro_flesh", "Тленная плоть", Perk.PerkType.STAT, "textures/gui/stat_icons/main/health.png",
            new OptScaleExactStat(12, SummonStat.SUMMON_HEALTH, ModType.FLAT)), DivinePerks.BRANCH_NECROMANCER);

        branch(level(perk("bc_necro_lord", "Владыка нежити", Perk.PerkType.SPECIAL, "textures/gui/talent_icons/death_crown.png",
            new OptScaleExactStat(20, SummonStat.UNDEAD_MINION_DAMAGE, ModType.FLAT),
            new OptScaleExactStat(10, SummonStat.SUMMON_DAMAGE, ModType.FLAT)), 25), DivinePerks.BRANCH_NECROMANCER);

        branch(level(perk("bc_necro_corpse", "Взрыв останков", Perk.PerkType.SPECIAL, "textures/gui/talent_icons/corpse_burst.png",
            new OptScaleExactStat(25, DivineStats.CORPSE_EXPLOSION.get(), ModType.FLAT)), 20), DivinePerks.BRANCH_NECROMANCER);

        branch(level(perk("bc_necro_chain_death", "Цепной мор", Perk.PerkType.SPECIAL, "textures/gui/talent_icons/plague_chain.png",
            new OptScaleExactStat(35, DivineStats.CORPSE_EXPLOSION.get(), ModType.FLAT),
            new OptScaleExactStat(8, DivineStats.RAISE_DEAD_CHANCE.get(), ModType.FLAT)), 30), DivinePerks.BRANCH_NECROMANCER);

        // ======================= ПРИЗЫВАТЕЛЬ (правая часть) =======================

        Perk summoner = perk("bc_summ_entry", "Путь зверолова", Perk.PerkType.MAJOR, "textures/gui/talent_icons/beast_claw.png",
            new OptScaleExactStat(1, SummonStat.MAX_BEAST_MINIONS, ModType.FLAT),
            new OptScaleExactStat(10, SummonStat.BEAST_MINION_DAMAGE, ModType.FLAT));
        summoner.is_entry = true;
        summoner.exclusive_branch = DivinePerks.BRANCH_SUMMONER;

        beast(perk("bc_summ_wolves", "Серая стая", Perk.PerkType.MAJOR, "textures/gui/talent_icons/grey_wolf.png",
            new OptScaleExactStat(3, SummonStat.MAX_BEAST_MINIONS, ModType.FLAT),
            new OptScaleExactStat(1, SummonStat.BEAST_PACK_WOLVES, ModType.FLAT)), 5);

        beast(perk("bc_summ_bear", "Бурый страж", Perk.PerkType.MAJOR, "textures/gui/talent_icons/bear.png",
            new OptScaleExactStat(1, SummonStat.MAX_BEAST_MINIONS, ModType.FLAT),
            new OptScaleExactStat(25, SummonStat.SUMMON_HEALTH, ModType.FLAT),
            new OptScaleExactStat(1, SummonStat.BEAST_PACK_BEAR, ModType.FLAT)), 5);

        beast(perk("bc_summ_mammoth", "Мамонт разбойников", Perk.PerkType.MAJOR, "textures/gui/talent_icons/predator.png",
            new OptScaleExactStat(1, SummonStat.MAX_BEAST_MINIONS, ModType.FLAT),
            new OptScaleExactStat(50, SummonStat.SUMMON_HEALTH, ModType.FLAT),
            new OptScaleExactStat(1, SummonStat.BEAST_PACK_MAMMOTH, ModType.FLAT)), 25);

        branch(perk("bc_summ_fangs", "Острые клыки", Perk.PerkType.STAT, "textures/gui/stat_icons/misc/melee_dmg.png",
            new OptScaleExactStat(12, SummonStat.BEAST_MINION_DAMAGE, ModType.FLAT)), DivinePerks.BRANCH_SUMMONER);

        branch(perk("bc_summ_paws", "Резвые лапы", Perk.PerkType.STAT, "textures/gui/stat_icons/misc/attack_speed.png",
            new OptScaleExactStat(10, SummonStat.SUMMON_SPEED, ModType.FLAT)), DivinePerks.BRANCH_SUMMONER);

        branch(perk("bc_summ_hide", "Колючая шкура", Perk.PerkType.STAT, "textures/gui/stat_icons/main/armor.png",
            new OptScaleExactStat(10, SummonStat.SUMMON_ARMOR, ModType.FLAT).scale()), DivinePerks.BRANCH_SUMMONER);

        branch(level(perk("bc_summ_alpha", "Альфа-хищник", Perk.PerkType.SPECIAL, "textures/gui/talent_icons/unleashed.png",
            new OptScaleExactStat(25, SummonStat.BEAST_MINION_DAMAGE, ModType.FLAT),
            new OptScaleExactStat(10, SummonStat.SUMMON_DAMAGE, ModType.FLAT)), 25), DivinePerks.BRANCH_SUMMONER);
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
        perk.one_of_a_kind = DivinePerks.ONE_OF_A_KIND_BEAST;
        perk.exclusive_branch = DivinePerks.BRANCH_SUMMONER;
        perk.lvl_req = lvl_req;
        return perk;
    }
}
