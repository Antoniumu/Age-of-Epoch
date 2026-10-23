#!/usr/bin/env python3
"""
Age of Epoch — численная модель баланса.

Все формулы взяты из кода порта (значения совпадают с апстримом Age of Exile):

  LevelUtils.getExpRequiredForLevel    -> (10 * N(lvl)) ** 2.1
  LevelUtils.getBaseExpMobReward       -> 10 + (3 * N(lvl)) ** 1.1
  GameBalanceConfig.NORMAL_STAT_SCALING-> N(lvl) = 1 + 0.2 * (lvl - 1)   (cap: нет)
  BaseStatsAdder                       -> Health 10*N + ванильные 20 HP, Mana/Energy 30*N + 10
  MobStatUtils.getMobBaseStats         -> HP = vanillaHP * rarityHP * (1 + 0.02*lvl) * N
  EntityData.mobBasicAttack            -> dmg = vanillaDmg * rarityDmg * N
  IUsableStat.getUsableValue           -> (v/need) / (v/need + 1.5) * maxMulti
  Stats / gear                         -> см. константы ниже

Скрипт ничего не меняет в игре — это офлайн-калькулятор, чтобы цифры в
BALANCE-AUDIT.md можно было перепроверить и пересчитать после правок.

ВНИМАНИЕ: константы ниже соответствуют текущему состоянию кода — то есть уже
ПОСЛЕ правок из BALANCE-AUDIT.md §13 (энергия и мана ×(1+0.05·(lvl−1)), реген
+8% пула за тик, +1.5 HP за уровень, core-статы с шагом 0.25 уровня, favor 500,
мобы +1%/уровень). Исторические цифры «до правок» остались в аудите.
"""

MAX_LEVEL = 50
LEVELS_PER_TIER = 10

# --------------------------------------------------------------------------
# базовые формулы
# --------------------------------------------------------------------------


def N(lvl):
    """NORMAL_STAT_SCALING, апстрим: base 1, +0.2 за уровень, без капа."""
    return 1.0 + 0.2 * (lvl - 1)


def exp_required(lvl):
    return int((10.0 * N(lvl)) ** 2.1)


def mob_exp(lvl):
    return int(10 + (3.0 * N(lvl)) ** 1.1)


def usable(value, need, max_multi):
    if need <= 0:
        return 0.0
    r = value / need
    return min(1.0, r / (r + 1.5)) * max_multi


# --------------------------------------------------------------------------
# персонаж
# --------------------------------------------------------------------------

# BaseStatsAdder: scaled(Health, 10) + ванильные 20 HP (Unit.addVanillaHpToStats)
BASE_HP_AT_LVL1 = 20 + 10 * N(1)
BASE_MANA = 10 + 30
BASE_ENERGY = 10 + 30
MANA_REGEN_TICK_SECONDS = 3.0       # OnServerTick "regen", 60 тиков
ENERGY_REGEN_TICK_SECONDS = 3.0

# оружие: BaseGearBuilder.weapon -> getAttackDamageStat(FULL) = 2..4 * statMulti
# + BaseStatsAdder: AttackDamage(Physical) = 1*N (scaled) + 3 (flat)
WEAPON_STAT_MULTI = {          # WeaponTypes.statMulti
    "sword": 0.9, "axe": 1.25, "staff": 1.0, "scepter": 0.9,
    "bow": 0.8, "crossbow": 0.9,
}
WEAPON_APS = {                 # WeaponTypes.atkPerSec
    "sword": 1.6, "axe": 1.5, "staff": 1.2, "scepter": 1.2,
    "bow": 1.0, "crossbow": 1.0,
}
# GearSlots energy_cost
WEAPON_ENERGY = {"sword": 4, "axe": 5, "staff": 3, "scepter": 3, "bow": 8, "crossbow": 7}

# броня: GearDataHelper — base 6..15 * слот (helmet .5, chest 1, pants .8, boots .5)
ARMOR_SLOT_MULTI = 0.5 + 1.0 + 0.8 + 0.5     # 2.8
ARMOR_AVG_ROLL = 10.5                        # среднее 6..15
BASE_ARMOR = ARMOR_AVG_ROLL * ARMOR_SLOT_MULTI   # 29.4 до умножения на N
BASE_RESIST = BASE_ARMOR                          # ELE_DEF те же числа
ARMOR_MAX_MULTI = 0.8
ARMOR_NEED_AT_1 = 20
RESIST_MAX_MULTI = 0.8
RESIST_NEED_AT_1 = 20
DODGE_MAX_MULTI = 0.8
DODGE_NEED_AT_1 = 15


def player_hp(lvl, extra_flat=0.0):
    # Unit.addVanillaHpToStats: + PLAYER_HEALTH_PER_LEVEL (1.5) за уровень
    return 20 + 10 * N(lvl) + 1.5 * (lvl - 1) + extra_flat * N(lvl)


def player_attack_damage(lvl, weapon="sword", weapon_ilvl=None):
    ilvl = weapon_ilvl or lvl
    avg_weapon = 3.0 * WEAPON_STAT_MULTI[weapon] * N(ilvl)
    return 3.0 + 1.0 * N(lvl) + avg_weapon


def armor_mitigation(lvl, armor_value=None):
    if armor_value is None:
        armor_value = BASE_ARMOR * N(lvl)
    need = ARMOR_NEED_AT_1 * N(lvl)
    return usable(armor_value, need, ARMOR_MAX_MULTI)


def resist_mitigation(lvl, resist_value=None):
    if resist_value is None:
        resist_value = BASE_RESIST * N(lvl)
    need = RESIST_NEED_AT_1 * N(lvl)
    return usable(resist_value, need, RESIST_MAX_MULTI)


def mob_hp(lvl, vanilla_hp=20, rarity_hp=1.0):
    # ServerContainer.EXTRA_MOB_STATS_PER_LEVEL = 0.01 (было 0.02)
    return vanilla_hp * rarity_hp * (1 + 0.01 * lvl) * N(lvl)


def mob_hit(lvl, vanilla_dmg=3, rarity_dmg=1.0, diff_dmg=1.0, entity_cfg_dmg=1.0):
    return vanilla_dmg * rarity_dmg * diff_dmg * entity_cfg_dmg * N(lvl)


def mob_armor(lvl, stat_multi=1.0):
    """MobStatUtils.getMobBaseStats: Armor = 10 * rarity.stat_multi, scaled by N."""
    return 10.0 * stat_multi * N(lvl)


def mob_armor_mitigation(lvl, rarity="common"):
    return usable(mob_armor(lvl, MOB_RARITY[rarity]["stat"]), 20 * N(lvl), ARMOR_MAX_MULTI)


# редкости мобов (MobRaritiesAdder)
MOB_RARITY = {
    # stat = rar.stat_multi (идёт в броню/сопротивления моба)
    "common": dict(weight=1000, min_lvl=0, hp=1.0, dmg=1.0, stat=1.0, exp=1.0, loot=1.0, affixes=0),
    "elite":  dict(weight=100,  min_lvl=5, hp=3.0, dmg=2.0, stat=3.0, exp=2.5, loot=2.0, affixes=1),
    "champ":  dict(weight=50,  min_lvl=15, hp=5.0, dmg=2.0, stat=3.5, exp=4.0, loot=4.0, affixes=2),
    "boss":   dict(weight=2,   min_lvl=20, hp=10.0, dmg=2.4, stat=3.75, exp=10.0, loot=6.0, affixes=2),
}


def rarity_chances(lvl):
    pool = {k: v for k, v in MOB_RARITY.items() if lvl >= v["min_lvl"] or lvl >= MAX_LEVEL}
    total = sum(v["weight"] for v in pool.values())
    return {k: v["weight"] / total for k, v in pool.items()}


# --------------------------------------------------------------------------
# ресурсы: энергия / мана
# --------------------------------------------------------------------------

def energy_pool(lvl):
    return 10 + 30 * N(lvl)


# RestoreResourceEvent.getAmountWithPercentRegen: + max * 8% за тик (60 тиков = 3 с)
REGEN_PERCENT_PER_TICK = 0.08


def energy_regen_per_tick(lvl):
    return 2 + 1 * N(lvl) + energy_pool(lvl) * REGEN_PERCENT_PER_TICK


# GameBalanceConfig.ENERGY_COST_SCALING = (1, 0.05) — стоимость удара почти не растёт
def energy_cost_swing(lvl, weapon="sword"):
    return WEAPON_ENERGY[weapon] * (1 + 0.05 * (lvl - 1))


def mana_pool(lvl):
    return 10 + 30 * N(lvl)


def mana_regen_per_tick(lvl):
    return 2 + 1 * N(lvl) + mana_pool(lvl) * REGEN_PERCENT_PER_TICK


def fireball_cost(lvl):
    """LeveledValue(0.5*5, 1*5) * MANA_COST_SCALING (теперь 1 + 0.05·(lvl−1))."""
    base = 2.5 + (2.5 / MAX_LEVEL) * min(lvl, MAX_LEVEL)
    return base * (1 + 0.05 * (min(lvl, MAX_LEVEL) - 1))


def fireball_damage(lvl, spell_power=None):
    """SpellCalcs.FIREBALL: spellScaling(0.5, 1.0) от SpellPower."""
    if spell_power is None:
        spell_power = 3 + 1 * N(lvl) + 4.0 * N(lvl)   # база + средний посох (3..5)
    frac = 0.5 + (1.0 - 0.5) / MAX_LEVEL * min(lvl, MAX_LEVEL)
    return spell_power * frac


# --------------------------------------------------------------------------
# дроп и благосклонность (favor)
# --------------------------------------------------------------------------

DROP_RATE = {"gear": 7.0, "gem": 0.5, "ingredient": 5.0, "rune": 0.05, "currency": 0.2}
GEAR_RARITY_WEIGHT = {"common": 5000, "uncommon": 2000, "rare": 300, "epic": 100, "unique": 100}


def loot_modifier(lvl, rarity="common", vanilla_hp=20):
    m = 1.0
    m += (1 + vanilla_hp / 40.0) - 1.0          # getMobHealthBasedLootMulti
    m += MOB_RARITY[rarity]["loot"] - 1.0       # LootMultiplier
    return m


def drop_chance(lvl, kind, rarity="common"):
    return DROP_RATE[kind] * loot_modifier(lvl, rarity)


def gear_rarity_chances():
    total = sum(GEAR_RARITY_WEIGHT.values())
    return {k: v / total for k, v in GEAR_RARITY_WEIGHT.items()}


FAVOR_START = 500
FAVOR_PER_KILL = 0.1
FAVOR_PER_ITEM = 1.0
FAVOR_PER_CHEST = 50
FAVOR_NONE_EXP_MULTI = 0.5


# --------------------------------------------------------------------------
# характеристики (StatPointsData + CoreStat)
# --------------------------------------------------------------------------

STAT_POINTS_PER_LEVEL = 2
TALENT_POINTS_AT_MAX_LEVEL = 100
SPELL_POINTS_PER_LEVEL = 2

CORE_STAT_EFFECT = {   # на 1 очко, до умножения на N(effectiveLevel)
    "STR": dict(ad=0.15, armor=1.0),
    "DEX": dict(ad=0.15, dodge=1.0),
    "INT": dict(spell_power=0.15, resist_each=1.0),
    "VIT": dict(hp=1.5, hp_regen=0.1),
    "WIS": dict(mana=2.0, mana_regen=0.1),
    "AGI": dict(energy=2.0, energy_regen=0.1),
}

CORE_STAT_LEVEL_FACTOR = 0.25


def core_effective_level(lvl):
    """CoreStat.getMods: эффективный уровень = 1 + round((lvl−1)·0.25)."""
    return 1 + round((lvl - 1) * CORE_STAT_LEVEL_FACTOR)


# --------------------------------------------------------------------------
# отчёты
# --------------------------------------------------------------------------

def table_exp():
    print("### Опыт и число убийств\n")
    print("| Уровень | Нужно опыта | Опыт за моба | Убийств на уровень | Всего убийств |")
    print("|---:|---:|---:|---:|---:|")
    total = 0
    for lvl in range(1, MAX_LEVEL):
        need = exp_required(lvl + 1)
        per = mob_exp(lvl)
        kills = need / per
        total += kills
        if lvl in (1, 5, 10, 15, 20, 25, 30, 35, 40, 45, 49):
            print(f"| {lvl} | {need:,} | {per} | {kills:,.0f} | {total:,.0f} |".replace(",", " "))
    print()
    print(f"Всего убийств 1→50: **{total:,.0f}**".replace(",", " "))
    print(f"Опыт за 1→50: **{sum(exp_required(l) for l in range(2, MAX_LEVEL + 1)):,}**".replace(",", " "))
    print()


def table_combat():
    print("### Бой: обычный зомби, базовая экипировка\n")
    print("| Ур. | HP игрока | Урон игрока | Моб HP | Моб броня | Удар моба | Броня игрока | Ударов до смерти игрока | Ударов до убийства моба | Время убийства (сек) |")
    print("|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|")
    for lvl in (1, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50):
        hp = player_hp(lvl)
        ad = player_attack_damage(lvl, "sword")
        mhp = mob_hp(lvl)
        mhit = mob_hit(lvl)
        mit = armor_mitigation(lvl)
        mob_mit = mob_armor_mitigation(lvl)
        hits_to_die = hp / (mhit * (1 - mit))
        hits_to_kill = mhp / (ad * (1 - mob_mit))
        ttk = mhp / (ad * (1 - mob_mit) * WEAPON_APS["sword"])
        print(f"| {lvl} | {hp:.0f} | {ad:.0f} | {mhp:.0f} | {mob_mit*100:.0f}% | {mhit:.0f} | {mit*100:.0f}% | {hits_to_die:.1f} | {hits_to_kill:.1f} | {ttk:.1f} |")
    print()


def table_resources():
    print("### Энергия и мана\n")
    print("| Ур. | Пул энергии | Удар стоит | Реген / 3 с | Ударов в запасе | Ударов в сек (реген) | Пул маны | Фаербол стоит | Кастов в запасе | Кастов в сек (реген) |")
    print("|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|")
    for lvl in (1, 5, 10, 20, 30, 40, 50):
        ep = energy_pool(lvl)
        cost = energy_cost_swing(lvl, "sword")
        regen = energy_regen_per_tick(lvl) / MANA_REGEN_TICK_SECONDS
        burst = ep / cost
        sustained = regen / cost
        mp = mana_pool(lvl)
        fc = fireball_cost(lvl)
        casts_burst = mp / fc
        casts_sustained = (mana_regen_per_tick(lvl) / MANA_REGEN_TICK_SECONDS) / fc
        print(f"| {lvl} | {ep:.0f} | {cost:.0f} | {energy_regen_per_tick(lvl):.1f} | {burst:.1f} | {sustained:.3f} "
              f"| {mp:.0f} | {fc:.1f} | {casts_burst:.1f} | {casts_sustained:.3f} |")
    print()


def table_stats():
    print("### 100 очков характеристик (уровень 50)\n")
    print("| Вложено | Эффект на 50 ур. | Сравнение с базой |")
    print("|---|---|---|")
    n50 = N(core_effective_level(50))
    base_hp = player_hp(50)
    base_ad = player_attack_damage(50)
    for stat in ("VIT", "STR", "DEX", "INT", "WIS", "AGI"):
        eff = CORE_STAT_EFFECT[stat]
        parts = []
        if "hp" in eff:
            hp = 100 * eff["hp"] * n50
            parts.append(f"+{hp:.0f} HP (×{hp/base_hp:.1f} к базовым {base_hp:.0f})")
        if "ad" in eff:
            ad = 100 * eff["ad"] * n50
            parts.append(f"+{ad:.0f} урона (~+{ad/base_ad*100:.0f}% к {base_ad:.0f})")
        if "spell_power" in eff:
            sp = 100 * eff["spell_power"] * n50
            parts.append(f"+{sp:.0f} силы заклинаний")
        if "resist_each" in eff:
            res = 100 * eff["resist_each"] * n50
            parts.append(f"+{res:.0f} сопр. каждому элементу (≈{usable(res, 20*n50, 0.8)*100:.0f}% снижения; кап 75% = ~{20*n50*22.5:.0f})")
        if "dodge" in eff:
            dg = 100 * eff["dodge"] * n50
            parts.append(f"+{dg:.0f} уклонения")
        if "mana" in eff:
            parts.append(f"+{100*eff['mana']*n50:.0f} маны")
        if "energy" in eff:
            parts.append(f"+{100*eff['energy']*n50:.0f} энергии")
        print(f"| 100 {stat} | {'; '.join(parts)} | |")
    print()


def table_loot():
    print("### Дроп на убийство (обычный моб, свой уровень)\n")
    print("| Редкость | Шанс редкости | Модификатор лута | Шанс гира (soul) | Уник за 1000 убийств |")
    print("|---|---:|---:|---:|---:|")
    rar = gear_rarity_chances()
    for mob_rarity in ("common", "elite", "champ", "boss"):
        mod = loot_modifier(50, mob_rarity)
        chance = DROP_RATE["gear"] * mod / 100.0
        uniq = chance * rar["unique"] * 1000
        print(f"| {mob_rarity} | {rarity_chances(50)[mob_rarity]*100:.1f}% | ×{mod:.1f} | {chance*100:.1f}% | {uniq:.2f} |")
    print()
    print("Ожидаемый дроп за 100 убийств обычных мобов того же уровня:")
    for kind, rate in DROP_RATE.items():
        print(f"- {kind}: {rate * loot_modifier(50) :.1f}")
    print()
    print("Редкости гира: " + ", ".join(f"{k} {v*100:.2f}%" for k, v in rar.items()))
    print()


def table_favor():
    print("### Благосклонность (favor)\n")
    print(f"Старт: {FAVOR_START}. Убийство: −{FAVOR_PER_KILL}. Поднятый предмет: −{FAVOR_PER_ITEM}. Сундук: +{FAVOR_PER_CHEST}.")
    kills_to_zero = FAVOR_START / FAVOR_PER_KILL
    print(f"Без лута хватает на **{kills_to_zero:.0f}** убийств.")
    items = sum(DROP_RATE[k] * loot_modifier(50) for k in DROP_RATE) / 100.0
    print(f"Предметов с убийства: ~{items:.2f}; расход на убийство с лутом: ~{FAVOR_PER_KILL + items*FAVOR_PER_ITEM:.2f}")
    print(f"С учётом лута хватает на **{FAVOR_START/(FAVOR_PER_KILL+items):.0f}** убийств.")
    print(f"Ниже 1 favor — ранг «none»: ×{FAVOR_NONE_EXP_MULTI} опыта, без уникальных/самоцветов/рун/валюты, редкие и эпические вещи не падают.")
    print()


def table_difficulty():
    print("### Авто-сложность (ScalingPlayerDiffData)\n")
    print("Рост: 0.005 за убийство + 0.0005/сек (1.8/час). Максимум 100.")
    print()
    print("| Часов игры | Убийств (~200/ч) | Сложность | HP/урон мобов |")
    print("|---:|---:|---:|---:|")
    for hours in (1, 5, 10, 25, 50, 100):
        kills = 200 * hours
        diff = min(100.0, kills * 0.005 + hours * 3600 * 0.0005)
        print(f"| {hours} | {kills} | {diff:.0f} | +{diff:.0f}% |")
    print()


def table_endgame():
    print("### Эндгейм-боссы (уровень 50)\n")
    base_hp = mob_hp(50, vanilla_hp=20, rarity_hp=MOB_RARITY["boss"]["hp"])
    base_dmg = mob_hit(50, vanilla_dmg=3, rarity_dmg=MOB_RARITY["boss"]["dmg"])
    print(f"| Босс | HP | Удар | Ударов игроку ({player_hp(50):.0f} HP, 40% брони) | Ударов боссу (урон игрока {player_attack_damage(50, 'sword'):.0f}) |")
    print("|---|---:|---:|---:|---:|")
    rows = [
        ("Моб-босс (редкость boss)", 1.0, 1.0),
        ("Башня, ранг IV (ravager, ×3.4/×1.9)", 3.4, 1.9),
        ("Делв, страж (×2.5 HP, +2 ур.)", 2.5, 1.0),
    ]
    for name, hp_multi, dmg_multi in rows:
        hp = base_hp * hp_multi
        dmg = base_dmg * dmg_multi
        hits_player = player_hp(50) / (dmg * (1 - armor_mitigation(50)))
        hits_boss = hp / (player_attack_damage(50, "sword") * (1 - mob_armor_mitigation(50, "boss")))
        print(f"| {name} | {hp:,.0f} | {dmg:,.0f} | {hits_player:.1f} | {hits_boss:,.0f} |".replace(",", " "))
    print()
    print("Примечание: TowerBoss теперь масштабирует ванильный MAX_HEALTH *до* forceRecalculateStats(), "
          "поэтому HP мода считается уже от новой базы (раньше был скачок после первого удара). "
          "Таблица башни — оценка через множитель редкости boss, без аффиксов арены.")
    print()


def table_armor_curve():
    print("### Кривая брони и сопротивлений (уровень 50, нужно 20×N = 216)\n")
    print("| Броня | Снижение физ. урона | Сопротивление | Снижение элемента |")
    print("|---:|---:|---:|---:|")
    for value in (0, 108, 216, 432, 864, 1728):
        print(f"| {value} | {usable(value, 20*N(50), 0.8)*100:.0f}% | {value} | {usable(value, 20*N(50), 0.8)*100:.0f}% |")
    print()


def table_pacing():
    print("### Время на уровень (оценка)\n")
    print("«Потолок» — DPS без учёта энергии (как если бы ресурс не тратился).")
    print("«С энергией» — темп ограничен регеном энергии (OnServerTick, 60 тиков).\n")
    print("| Ур. | HP моба | DPS потолок | Убийств на ур. | Часов (потолок) | Ударов/сек с энергией | Часов (с энергией) |")
    print("|---:|---:|---:|---:|---:|---:|---:|")
    total_fast = 0.0
    total_slow = 0.0
    for lvl in range(1, MAX_LEVEL):
        mhp = mob_hp(lvl) * (1 + 0.2)          # +20% брони моба (common)
        dps = player_attack_damage(lvl, "sword") * WEAPON_APS["sword"]
        kill_time = mhp / dps
        kills = exp_required(lvl + 1) / mob_exp(lvl)
        hours = kills * (kill_time + 4.0) / 3600.0
        total_fast += hours

        regen = energy_regen_per_tick(lvl) / ENERGY_REGEN_TICK_SECONDS
        cost = energy_cost_swing(lvl, "sword")
        sustained = regen / cost
        dps_sustained = player_attack_damage(lvl, "sword") * sustained
        kill_sustained = mhp / dps_sustained
        hours_sustained = kills * (kill_sustained + 4.0) / 3600.0
        total_slow += hours_sustained
        if lvl in (1, 5, 10, 15, 20, 25, 30, 35, 40, 45, 49):
            print(f"| {lvl} | {mhp:.0f} | {dps:.0f} | {kills:,.0f} | {hours:.2f} | {sustained:.2f} | {hours_sustained:.2f} |".replace(",", " "))
    print()
    print(f"Итого 1→50: **{total_fast:.1f} ч** без лимита энергии и **{total_slow:.1f} ч** с её регеном.")
    print()


# --------------------------------------------------------------------------
# автобаланс GameMaster (AutoBalance.java)
# --------------------------------------------------------------------------

AUTO_START_MINUTES = 14.0
AUTO_END_MINUTES = 70.0
AUTO_CURVE = 1.0
AUTO_DEAD_ZONE = 0.2
AUTO_EXP_MIN = 0.35
AUTO_EXP_MAX = 3.0


def auto_target_minutes(lvl, max_level=MAX_LEVEL):
    """Геометрическая интерполяция между 14 и 70 минутами на уровень."""
    t = max(0.0, min(1.0, (lvl - 1) / (max_level - 1)))
    t = t ** AUTO_CURVE
    return AUTO_START_MINUTES * (AUTO_END_MINUTES / AUTO_START_MINUTES) ** t


def auto_total_hours(max_level=MAX_LEVEL):
    return sum(auto_target_minutes(l) for l in range(1, max_level)) / 60.0


def auto_exp_for_ratio(ratio):
    """AutoBalance.evaluate: вне мёртвой зоны опыт = ratio^0.8, затем clamp."""
    if 1 - AUTO_DEAD_ZONE <= ratio <= 1 + AUTO_DEAD_ZONE:
        return 1.0
    return max(AUTO_EXP_MIN, min(AUTO_EXP_MAX, ratio ** 0.8))


def table_autobalance():
    print("### Автобаланс: целевой коридор и реакция на темп\n")
    print(f"Коридор: {AUTO_START_MINUTES:.0f} мин на 1 ур. → {AUTO_END_MINUTES:.0f} мин на 50 ур., "
          f"мёртвая зона ±{AUTO_DEAD_ZONE*100:.0f}%.")
    print(f"Суммарно до максимума: **{auto_total_hours():.1f} ч** (сумма целевых времён уровней).\n")
    print("| Ур. | Цель, мин | Суммарно, ч |")
    print("|---:|---:|---:|")
    total = 0.0
    for lvl in range(1, MAX_LEVEL):
        total += auto_target_minutes(lvl)
        if lvl in (1, 5, 10, 15, 20, 25, 30, 35, 40, 45, 49):
            print(f"| {lvl} | {auto_target_minutes(lvl):.0f} | {total/60:.1f} |")
    print()
    print("Реакция множителя опыта на фактический темп (мёртвая зона ±20%):\n")
    print("| Факт / цель | Множитель опыта | Что видит игрок |")
    print("|---:|---:|---|")
    for ratio in (0.25, 0.5, 0.8, 1.0, 1.2, 1.5, 2.0, 3.0):
        mult = auto_exp_for_ratio(ratio)
        if ratio > 1.2:
            note = "медленнее цели (или застрял): опыт выше"
        elif ratio < 0.8:
            note = "быстрее цели: опыт ниже"
        else:
            note = "в коридоре: без изменений"
        print(f"| ×{ratio:.2f} | ×{mult:.2f} | {note} |")
    print()

if __name__ == "__main__":
    print("# Age of Epoch — расчёт баланса\n")
    print("Формулы: `N(lvl) = 1 + 0.2 × (lvl − 1)`, на 50 уровне N = 10.8.\n")
    table_exp()
    table_combat()
    table_resources()
    table_stats()
    table_loot()
    table_favor()
    table_difficulty()
    table_armor_curve()
    table_endgame()
    table_pacing()
    table_autobalance()
