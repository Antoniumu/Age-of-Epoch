#!/usr/bin/env python3
"""
Age of Epoch — реестр множителей и их «стек» на конкретном уровне.

Скрипт отвечает на вопрос «из чего складывается итоговое число» для:
  * опыта с моба,
  * HP и урона моба,
  * HP / энергии / маны игрока,
  * шанса дропа,
  * Благосклонности (favor).

Все формулы — из кода порта. Константы «до» (BEFORE) — апстрим (main),
«после» (AFTER) — текущее состояние кода. Балансные правки откачены,
поэтому обе колонки совпадают: это и есть проверка, что откат полный.

Запуск:  python3 tools/balance/multiplier_stack.py
"""

MAX_LEVEL = 50

# --------------------------------------------------------------------------
# «после» (текущее состояние кода)
# --------------------------------------------------------------------------

AFTER = dict(
    player_hp_per_level=0.0,        # добавки нет (апстрим)
    mob_extra_per_level=0.02,       # ServerContainer.EXTRA_MOB_STATS_PER_LEVEL
    energy_cost_per_level=0.2,      # cost = slot.energy_cost * N(lvl)
    mana_cost_per_level=0.2,        # GameBalanceConfig.MANA_COST_SCALING
    regen_percent=0.0,              # процентного регена нет
    core_stat_factor=1.0,           # CoreStat: полный уровень
    favor_start=100,
    favor_per_kill=0.25,
    favor_per_chest=20,
    favor_none_exp=0.25,
    unique_weight=25,
    tower_hp_tuning=1.0,            # множители ложатся на ванильный HP
    overworld_scales=False,
)

# --------------------------------------------------------------------------
# «до» (апстрим / состояние до правок)
# --------------------------------------------------------------------------

BEFORE = dict(
    player_hp_per_level=0.0,        # добавки не было
    mob_extra_per_level=0.02,
    energy_cost_per_level=0.2,      # cost = slot.energy_cost * N(lvl)
    mana_cost_per_level=0.2,
    regen_percent=0.0,
    core_stat_factor=1.0,           # вклад очков умножался на полный N(lvl)
    favor_start=100,
    favor_per_kill=0.25,
    favor_per_chest=20,
    favor_none_exp=0.25,
    unique_weight=25,
    tower_hp_tuning=1.0,            # множители ложились на ванильный HP
    overworld_scales=False,
)

# общие (не менялись)
WEAPON_ENERGY = 4          # sword
MANA_POOL_BASE, MANA_POOL_SCALED = 10, 30
HP_BASE_FLAT, HP_BASE_SCALED = 20, 10
BASE_AD_FLAT, BASE_AD_SCALED = 3, 1
REGEN_FLAT, REGEN_SCALED = 2, 1
REGEN_TICK_SECONDS = 3.0

MOB_RARITY = {
    "common": dict(hp=1.0, dmg=1.0, stat=1.0, exp=1.0, loot=1.0, weight=1000, min_lvl=0),
    "elite": dict(hp=3.0, dmg=2.0, stat=3.0, exp=2.5, loot=2.0, weight=100, min_lvl=5),
    "champ": dict(hp=5.0, dmg=2.0, stat=3.5, exp=4.0, loot=4.0, weight=50, min_lvl=15),
    "boss": dict(hp=10.0, dmg=2.4, stat=3.75, exp=10.0, loot=6.0, weight=2, min_lvl=20),
}

GEAR_RARITY = dict(common=5000, uncommon=2000, rare=300, epic=100)

DIFFICULTY_CAP = 100.0
DIFF_HP_PER_POINT = 0.01
DIFF_DMG_PER_POINT = 0.01
DIFF_LOOT_PER_POINT = 0.01

DROP_RATE = dict(gear=7.0, gem=0.5, ingredient=5.0, rune=0.05, currency=0.2)

# GameMaster
GM_CATCHUP_PER_LEVEL = 0.25
GM_CATCHUP_MAX_BONUS = 2.0
GM_CATCHUP_PENALTY_PER_LEVEL = 0.05
GM_CATCHUP_MIN = 0.5
GM_EXP_CLAMP = (0.05, 5.0)
GM_AUTO_EXP = (0.35, 3.0)
GM_AUTO_HP = (0.7, 1.35)
GM_AUTO_DMG = (0.7, 1.3)
GM_AUTO_LOOT = (1.0, 1.35)


# --------------------------------------------------------------------------
# базовые формулы
# --------------------------------------------------------------------------

def N(lvl):
    """NORMAL_STAT_SCALING: 1 + 0.2·(lvl−1). На 50 ур. = 10.8."""
    return 1.0 + 0.2 * (lvl - 1)


def core_level(lvl, state):
    """CoreStat.getMods: эффективный уровень для вложенных очков."""
    return 1 + round((lvl - 1) * state["core_stat_factor"])


def usable(value, need, max_multi):
    r = value / need
    return min(1.0, r / (r + 1.5)) * max_multi


# --------------------------------------------------------------------------
# опыт
# --------------------------------------------------------------------------

def exp_required(lvl):
    return int((10.0 * N(lvl)) ** 2.1)


def mob_exp_base(lvl):
    return int(10 + (3.0 * N(lvl)) ** 1.1)


def exp_chain(lvl, state, vanilla_hp=20, rarity="common", entity_cfg=1.0,
              favor_multi=1.0, dimension_multi=1.0, exp_gain_multi=1.0, gm=1.0):
    """
    OnMobDeathDrops.GiveExp: бонусы СКЛАДЫВАЮТСЯ (exp += (m−1)·base),
    а множитель GameMaster применяется позже и уже умножением.
    """
    base = mob_exp_base(lvl)
    parts = {
        "база (10 + (3N)^1.1)": base,
        "HP-моб (1 + HP/40)": 1 + vanilla_hp / 40.0,
        "дистанция уровня": 1.0,
        "редкость моба": MOB_RARITY[rarity]["exp"],
        "конфиг моба": entity_cfg,
        "favor-ранг": favor_multi,
        "измерение": dimension_multi,
        "exp_gain_multi": exp_gain_multi,
    }
    additive = 1.0 + sum(v - 1.0 for v in parts.values() if v != base)
    total = base * additive * gm
    return base, additive, total, parts


# --------------------------------------------------------------------------
# моб
# --------------------------------------------------------------------------

def mob_hp(lvl, state, vanilla_hp=20, rarity="common", diff=0.0, auto=1.0):
    rar = MOB_RARITY[rarity]
    hp = vanilla_hp * rar["hp"] * (1 + state["mob_extra_per_level"] * lvl) * N(lvl)
    hp *= 1 + diff * DIFF_HP_PER_POINT
    return hp * auto


def mob_hit(lvl, state, vanilla_dmg=3, rarity="common", diff=0.0, map_dmg=1.0,
            entity_cfg=1.0, auto=1.0):
    rar = MOB_RARITY[rarity]
    dmg = vanilla_dmg * rar["dmg"] * N(lvl)
    dmg *= 1 + diff * DIFF_DMG_PER_POINT
    return dmg * map_dmg * entity_cfg * auto


def player_hp(lvl, state):
    return HP_BASE_FLAT + HP_BASE_SCALED * N(lvl) + state["player_hp_per_level"] * (lvl - 1)


def player_ad(lvl):
    return BASE_AD_FLAT + BASE_AD_SCALED * N(lvl)


# --------------------------------------------------------------------------
# ресурсы
# --------------------------------------------------------------------------

def energy_pool(lvl):
    return MANA_POOL_BASE + MANA_POOL_SCALED * N(lvl)


def energy_cost(lvl, state):
    return WEAPON_ENERGY * (1 + state["energy_cost_per_level"] * (lvl - 1))


def energy_regen_per_tick(lvl, state):
    return REGEN_FLAT + REGEN_SCALED * N(lvl) + energy_pool(lvl) * state["regen_percent"]


def fireball_cost(lvl, state):
    base = 2.5 + (2.5 / MAX_LEVEL) * min(lvl, MAX_LEVEL)
    return base * (1 + state["mana_cost_per_level"] * (min(lvl, MAX_LEVEL) - 1))


# --------------------------------------------------------------------------
# дроп
# --------------------------------------------------------------------------

def loot_modifier(lvl, state, rarity="common", vanilla_hp=20, diff=0.0, auto_loot=1.0):
    """LootInfo.setup: те же бонусы складываются, затем множатся на auto/loot-множители."""
    health = 1 + vanilla_hp / 40.0
    additive = 1.0 + (health - 1) + (MOB_RARITY[rarity]["loot"] - 1)
    return additive * (1 + diff * DIFF_LOOT_PER_POINT) * auto_loot


def gear_rarity_chances(state):
    weights = dict(GEAR_RARITY)
    weights["unique"] = state["unique_weight"]
    total = sum(weights.values())
    return {k: v / total for k, v in weights.items()}


# --------------------------------------------------------------------------
# отчёты
# --------------------------------------------------------------------------

def table(header, rows, aligns=None):
    print(header)
    print("| " + " | ".join(rows[0]) + " |")
    print("|" + "|".join(aligns or ["---:"] * len(rows[0])) + "|")
    for row in rows[1:]:
        print("| " + " | ".join(str(x) for x in row) + " |")
    print()


def report_mobs():
    rows = [["Ур.", "HP моба «до»", "HP моба «сейчас»", "Удар моба", "HP игрока «до»",
             "HP игрока «сейчас»", "Уд. до смерти (было)", "Уд. до смерти (сейчас)"]]
    for lvl in (1, 10, 25, 50):
        before = mob_hp(lvl, BEFORE)
        after = mob_hp(lvl, AFTER)
        hit = mob_hit(lvl, AFTER)
        hp_b, hp_a = player_hp(lvl, BEFORE), player_hp(lvl, AFTER)
        mit = usable(10.5 * 2.8 * N(lvl), 20 * N(lvl), 0.8)
        rows.append([lvl, f"{before:.0f}", f"{after:.0f}", f"{hit:.0f}", f"{hp_b:.0f}", f"{hp_a:.0f}",
                     f"{hp_b/(hit*(1-mit)):.1f}", f"{hp_a/(hit*(1-mit)):.1f}"])
    table("### Мобы и игрок (обычный моб, без сложности, базовый гир)\n", rows)


def report_resources():
    rows = [["Ур.", "Пул энергии", "Удар (было)", "Удар (сейчас)", "Реген/с (было)",
             "Реген/с (сейчас)", "Уд/с (было)", "Уд/с (сейчас)", "Фаербол (было)", "Фаербол (сейчас)"]]
    for lvl in (1, 10, 25, 50):
        pool = energy_pool(lvl)
        c_b, c_a = energy_cost(lvl, BEFORE), energy_cost(lvl, AFTER)
        r_b = (REGEN_FLAT + REGEN_SCALED * N(lvl)) / REGEN_TICK_SECONDS
        r_a = energy_regen_per_tick(lvl, AFTER) / REGEN_TICK_SECONDS
        rows.append([lvl, f"{pool:.0f}", f"{c_b:.1f}", f"{c_a:.1f}", f"{r_b:.2f}", f"{r_a:.2f}",
                     f"{r_b/c_b:.3f}", f"{r_a/c_a:.3f}",
                     f"{fireball_cost(lvl, BEFORE):.1f}", f"{fireball_cost(lvl, AFTER):.1f}"])
    table("### Ресурсы игрока\n", rows)


def report_exp():
    rows = [["Ур.", "База опыта моба", "Бонусы (сумма)", "Итог без GameMaster", "Итог с автобалансом ×1.5"]]
    for lvl in (1, 10, 25, 50):
        base, additive, total, _ = exp_chain(lvl, AFTER, entity_cfg=0.75)  # зомби: 0.75
        rows.append([lvl, base, f"×{additive:.2f}", f"{total:.1f}", f"{total*1.5:.1f}"])
    table("### Опыт с одного моба (зомби, свой уровень, favor normal)\n", rows)


def report_loot():
    rows = [["Редкость моба", "Модификатор «до»", "Модификатор «сейчас»", "Шанс гира «до»", "Шанс гира «сейчас»"]]
    for rarity in MOB_RARITY:
        m_b = loot_modifier(0, BEFORE, rarity)
        m_a = loot_modifier(0, AFTER, rarity)
        rows.append([rarity, f"×{m_b:.2f}", f"×{m_a:.2f}",
                     f"{DROP_RATE['gear']*m_b:.1f}%", f"{DROP_RATE['gear']*m_a:.1f}%"])
    table("### Шанс гира на убийство (числа ServerContainer — проценты)\n", rows)

    ch_b = gear_rarity_chances(BEFORE)
    ch_a = gear_rarity_chances(AFTER)
    rows = [["Редкость гира", "Шанс «до»", "Шанс «сейчас»"]]
    for key in ("common", "uncommon", "rare", "epic", "unique"):
        rows.append([key, f"{ch_b[key]*100:.2f}%", f"{ch_a[key]*100:.2f}%"])
    table("### Веса редкостей гира\n", rows)


def report_multiplier_map():
    rows = [
        ["Слой", "Где", "Формула", "Диапазон"],
        ["Опыт: база", "LevelUtils.getBaseExpMobReward", "10 + (3·N)^1.1", "13 → 54"],
        ["Опыт: HP моба", "LootUtils.getMobHealthBasedLootMulti", "1 + HP/40", "×1.05…×1.5"],
        ["Опыт: дистанция", "LootUtils.getLevelDistancePunishmentMulti", "1 − 0.1·Δ (мин 0.1)", "×0.1…×1"],
        ["Опыт: редкость", "MobRarity.exp_multi", "1 / 2.5 / 4 / 10", "×1…×10"],
        ["Опыт: конфиг моба", "EntityConfig.exp_multi", "по типу моба", "×0…×1"],
        ["Опыт: favor", "FavorRank.exp_multi", "0.5 / 1 / 1 / 1.1 / 1.25", "×0.5…×1.25"],
        ["Опыт: измерение", "DimensionConfig.exp_multi", "по измерению", "×1"],
        ["Опыт: сервер", "ServerContainer.EXP_GAIN_MULTI", "конфиг", "×0…×1000 (1)"],
        ["Опыт: GameMaster", "GameMaster.computeMultiplier", "темп × догоняющий × автобаланс", "×0.05…×5"],
        ["Моб HP", "MobStatUtils.getMobBaseStats", "HP×редкость×(1+k·lvl)×N", "20 → 324"],
        ["Моб урон", "EntityData.mobBasicAttack", "ваниль×редкость×N×сложность", "3 → 32"],
        ["Моб: авто", "GameMaster.mobHealthMultiplier", "среднее по онлайну", "×0.7…×1.35"],
        ["Моб: урон авто", "GameMaster.mobDamageMultiplier", "среднее по онлайну", "×0.7…×1.3"],
        ["Сложность: HP/урон", "ScalingPlayerDiffData", "1 + diff·0.01", "×1…×2"],
        ["Сложность: лут", "ScalingPlayerDiffData.getLootMulti", "1 + diff·0.01", "×1…×2"],
        ["Дроп: авто", "GameMaster.lootMultiplier", "среднее по онлайну", "×1…×1.35"],
        ["Игрок HP", "Unit.addVanillaHpToStats", "20 + 10·N + 1.5·(lvl−1)", "30 → 202"],
        ["Игрок AD", "BaseStatsAdder + оружие", "3 + N + 3·statMulti·N", "7 → 43"],
        ["Броня", "IUsableStat.getUsableValue", "r/(r+1.5) · 0.8, r = v/(20N)", "0…80%"],
        ["Энергия: пул", "BaseStatsAdder", "10 + 30·N", "40 → 334"],
        ["Энергия: удар", "EntityData.attackWithWeapon", "cost · (1+0.05·(lvl−1))", "4 → 14"],
        ["Энергия: реген", "RestoreResourceEvent", "(2+N) + 8% пула за 3 с", "5.4 → 38.7"],
        ["Спавн", "GameMaster.spawnRateMultiplier", "ручной · 100/(100+favor)", "×0.05…×1"],
        ["Башни: HP", "TowerBoss.scale", "20·def.health·0.65·(1+0.12(rank−1))", "×0.85…×3.2"],
        ["Башни: урон", "TowerBoss.scale", "2·def.damage·(1+0.08(rank−1))", "×1.1…×1.9"],
        ["Делвы", "DelveMarkerBlockEntity", "HP ×(1+0.15·depth), урон ×(1+0.1·depth)", "×1.15…×1.6"],
        ["Данж: сложность", "DifficultyAdders", "HP/урон ×1 / 1.5 / 2", "×1…×2"],
        ["Данж: отряд", "TeamSizeMobStrength", "+200/+100 HP при соло", "+200…+500% HP"],
    ]
    table("### Карта множителей\n", rows, aligns=["---", "---", "---", "---"])


def main():
    print("# Age of Epoch — реестр множителей\n")
    report_multiplier_map()
    report_exp()
    report_mobs()
    report_resources()
    report_loot()
    print("Подробные пояснения и «было → стало» — в MULTIPLIERS.md.\n")


if __name__ == "__main__":
    main()
