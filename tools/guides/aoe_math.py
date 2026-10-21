#!/usr/bin/env python3
"""
Age of Epoch — модель арифметики игры (Python-зеркало Java-кода).

Все формулы повторяют соответствующие классы мода:
  * GameBalanceConfig / LevelScalingConfig / StatScaling  -> скейлинг статов
  * InCalcStatData.calcValue()                            -> сборка FLAT/PERCENT/MULTI
  * IUsableStat.getUsableValue()                          -> броня/уклонение/сопротивления
  * DatapackStats (CoreStat)                              -> вклад характеристик
  * BaseStatsAdder.playerStats()                          -> база игрока
  * ValueCalculation / ScalingCalc                        -> урон заклинаний
  * HealthUtils                                           -> реальное HP и пересчёт урона

Скрипт используется генератором гайдов (build_guides.py) и для отчёта GUIDES.md.
"""

MAX_LEVEL = 50
NORMAL_BASE, NORMAL_PER_LEVEL = 1.0, 0.2      # NORMAL_STAT_SCALING
SLOW_BASE, SLOW_PER_LEVEL = 1.0, 0.01         # SLOW_STAT_SCALING
CORE_BASE, CORE_PER_LEVEL = 1.0, 0.05         # CORE_STAT_SCALING
STAT_REQ_BASE, STAT_REQ_PER_LEVEL = 2.0, 2.0  # STAT_REQ_SCALING

STAT_POINTS_PER_LEVEL = 2
TALENT_POINTS_AT_MAX_LEVEL = 100
STARTING_TALENT_POINTS = 1
SPELL_POINTS_PER_LEVEL = 2


def _multi(base, per_level, lvl, cap=True):
    l = max(1, min(lvl, MAX_LEVEL)) if cap else lvl
    return base + per_level * (l - 1)


def normal(lvl):
    """StatScaling.NORMAL — основной скейлинг флэт-статов (HP, броня, урон оружия)."""
    return _multi(NORMAL_BASE, NORMAL_PER_LEVEL, lvl, cap=False)


def slow(lvl):
    return _multi(SLOW_BASE, SLOW_PER_LEVEL, lvl)


def core_stat(lvl):
    return _multi(CORE_BASE, CORE_PER_LEVEL, lvl)


def stat_req(lvl):
    return _multi(STAT_REQ_BASE, STAT_REQ_PER_LEVEL, lvl)


# ---------------------------------------------------------------- ресурсы ------------------------------------------------
def stat_points(level):
    """StatPointsData.getFreePoints: total = level * 2."""
    return level * STAT_POINTS_PER_LEVEL


def talent_points(level):
    """TalentsData.getFreePoints: 1 + 100 * (level / 50)."""
    return int(STARTING_TALENT_POINTS + TALENT_POINTS_AT_MAX_LEVEL * (level / MAX_LEVEL))


def spell_points(level):
    return int(SPELL_POINTS_PER_LEVEL * level)


# ---------------------------------------------------------------- характеристики ------------------------------------------
# DatapackStats: CoreStatData -> OptScaleExactStat(v, stat, FLAT), всё скейлится NORMAL.
CORE_STATS = {
    "strength":   [("physical_weapon_damage", 0.10), ("armor", 1.0)],
    "dexterity":  [("physical_weapon_damage", 0.10), ("dodge", 1.0)],
    "intelligence": [("spell_power", 0.10), ("fire_resist", 1.0),
                     ("water_resist", 1.0), ("earth_resist", 1.0)],
    "vitality":   [("health", 2.0), ("health_regen", 0.10)],
    "wisdom":     [("mana", 2.0), ("mana_regen", 0.10)],
    "agility":    [("energy", 2.0), ("energy_regen", 0.10)],
}

BUDGET_STATS = ["strength", "dexterity", "intelligence", "vitality", "wisdom", "agility"]


def core_stat_gain(stat_id, lvl):
    """Сколько «внутренних единиц» даёт 1 очко характеристики на уровне lvl."""
    m = normal(lvl)
    return [(sid, v * m) for sid, v in CORE_STATS[stat_id]]


# ---------------------------------------------------------------- защита ---------------------------------------------------
def usable_value(value, value_needed_at_lvl1, max_multi, lvl, scaling=normal):
    """IUsableStat.getUsableValue — гиперболическая кривая с асимптотой max_multi."""
    needed = scaling(lvl) * value_needed_at_lvl1
    if needed <= 0:
        return max_multi
    ratio = value / needed
    perc = ratio / (ratio + 1.5)
    return min(perc, 1.0) * max_multi


def armor_reduction(armor, lvl):
    # Armor: need 20 @ lvl1, max 80%, NORMAL scaling
    return usable_value(armor, 20.0, 0.8, lvl)


def dodge_chance(dodge, lvl):
    # DodgeRating: need 15 @ lvl1, max 80%
    return usable_value(dodge, 15.0, 0.8, lvl)


def resist_reduction(resist, lvl):
    # ElementalResist: need 20 @ lvl1, max 80%
    return usable_value(resist, 20.0, 0.8, lvl)


def armor_needed_for(percent, lvl):
    """Обратная формула: сколько брони нужно для X% снижения урона."""
    if percent >= 0.8:
        percent = 0.7999
    target = percent / 0.8
    ratio = 1.5 * target / (1 - target)
    return ratio * normal(lvl) * 20.0


def dodge_needed_for(percent, lvl):
    if percent >= 0.8:
        percent = 0.7999
    target = percent / 0.8
    ratio = 1.5 * target / (1 - target)
    return ratio * normal(lvl) * 15.0


# ---------------------------------------------------------------- ресурсы игрока -------------------------------------------
BASE = {
    # BaseStatsAdder.playerStats()
    "health_scaled": 10, "health_flat": 0,
    "mana_scaled": 30, "mana_flat": 10,
    "energy_scaled": 30, "energy_flat": 0,
    "spell_power_scaled": 1, "spell_power_flat": 3,
    "attack_damage_scaled": 1, "attack_damage_flat": 3,
    "hp_regen_scaled": 1, "hp_regen_flat": 2,
    "mana_regen_scaled": 1, "mana_regen_flat": 2,
    "energy_regen_scaled": 1, "energy_regen_flat": 2,
}


def base_stat(stat_id, lvl):
    m = normal(lvl)
    if stat_id == "health":
        return BASE["health_scaled"] * m + BASE["health_flat"]
    if stat_id == "mana":
        return BASE["mana_scaled"] * m + BASE["mana_flat"]
    if stat_id == "energy":
        return BASE["energy_scaled"] * m + BASE["energy_flat"]
    if stat_id == "spell_power":
        return BASE["spell_power_scaled"] * m + BASE["spell_power_flat"]
    if stat_id == "physical_weapon_damage":
        return BASE["attack_damage_scaled"] * m + BASE["attack_damage_flat"]
    if stat_id == "health_regen":
        return BASE["hp_regen_scaled"] * m + BASE["hp_regen_flat"]
    if stat_id == "mana_regen":
        return BASE["mana_regen_scaled"] * m + BASE["mana_regen_flat"]
    if stat_id == "energy_regen":
        return BASE["energy_regen_scaled"] * m + BASE["energy_regen_flat"]
    return 0.0


# ---------------------------------------------------------------- урон -----------------------------------------------------
def spell_damage(spell_power, spell_level, max_lvl, lo, hi, base_lo=0, base_hi=0, caster_lvl=1):
    """
    ValueCalculation.getCalculatedValue:
        stat_scaling -> stat_value * multi(spell_level), multi = lo + (hi-lo)/max_lvl * level
        base         -> base_value * NORMAL(caster_lvl)
    """
    multi = lo + (hi - lo) / max_lvl * spell_level
    dmg = spell_power * multi
    if base_hi > 0:
        base_val = base_lo + (base_hi - base_lo) / max_lvl * spell_level
        dmg += base_val * normal(caster_lvl)
    return dmg


def attack_damage_total(phys_weapon_damage, gear_bonus=0.0):
    """Базовый удар: AttackDamage(physical) + урон оружия (data.getAmount())."""
    return phys_weapon_damage + gear_bonus


def crit_average(damage, crit_chance, crit_damage):
    """Средний урон с учётом крита: base * (1 + cc/100 * cd/100)."""
    cc = max(0.0, min(100.0, crit_chance)) / 100.0
    return damage * (1 + cc * crit_damage / 100.0)


def incoming_after_mitigation(dmg, armor, resist, lvl, element="physical", pene=0.0):
    if element == "physical":
        mit = armor_reduction(max(0.0, armor - pene), lvl)
    else:
        mit = resist_reduction(max(0.0, resist - pene), lvl)
    return dmg * (1 - mit)


def effective_hp(hp, armor, resist, lvl):
    """EHP против физического удара — сколько «сырого» урона держит персонаж."""
    return hp / max(0.01, 1 - armor_reduction(armor, lvl))


def mana_shield_split(mana, absorbed_percent):
    """DamageAbsorbedByMana: часть урона уходит в ману (числа берём из статов)."""
    return absorbed_percent


# ---------------------------------------------------------------- таблицы ------------------------------------------------
def scaling_table():
    print("Ур | NORMAL | SLOW | CORE | очки статов | таланты | скиллы")
    for lvl in [1, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50]:
        print(f"{lvl:2d} | {normal(lvl):6.2f} | {slow(lvl):4.2f} | {core_stat(lvl):4.2f} |"
              f" {stat_points(lvl):11d} | {talent_points(lvl):7d} | {spell_points(lvl):7d}")


def core_stat_table():
    print("\n1 очко характеристики -> статы (на уровнях 1 / 25 / 50)")
    for sid in BUDGET_STATS:
        row = []
        for lvl in (1, 25, 50):
            row.append(", ".join(f"{k}+{v:.2f}" for k, v in core_stat_gain(sid, lvl)))
        print(f"{sid:13s} L1: {row[0]}")
        print(f"{'':13s} L25: {row[1]}")
        print(f"{'':13s} L50: {row[2]}")


def armor_curve_table():
    print("\nБроня/уклонение/сопротивление -> % снижения (нужен NORMAL-скейл)")
    print("  ед. |  L10  |  L25  |  L40  |  L50")
    for v in [50, 100, 200, 400, 800, 1200, 2000, 3000]:
        print(f"{v:5d} | {armor_reduction(v,10)*100:5.1f} | {armor_reduction(v,25)*100:5.1f} |"
              f" {armor_reduction(v,40)*100:5.1f} | {armor_reduction(v,50)*100:5.1f}")


if __name__ == "__main__":
    scaling_table()
    core_stat_table()
    armor_curve_table()
    print("\nБроня для 40/60/70% на L50:",
          [round(armor_needed_for(p, 50)) for p in (0.4, 0.6, 0.7)])
    print("Уклонение для 40/60/70% на L50:",
          [round(dodge_needed_for(p, 50)) for p in (0.4, 0.6, 0.7)])
