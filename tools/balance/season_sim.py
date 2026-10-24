#!/usr/bin/env python3
"""Симуляция сезона Age of Epoch для компании 4-6 человек.

Считает не «на глаз», а по тем же константам, что в коде:

  * темп — целевые минуты на уровень из AutoBalance (14 -> 70 мин), то есть
    состояние, к которому «режиссёр» приводит игрока;
  * опыт — LevelUtils.getExpRequiredForLevel / getBaseExpMobReward;
  * дроп — ServerContainer.*_DROPRATE (это ПРОЦЕНТЫ на событие лута);
  * pity — Retention (уникаты 250->900 по дропам гира, руны 200->700,
    самоцветы 120->400 по убийствам);
  * крафт — 100 - 10*(n-1)% успеха, редкость по весам 2000/300/100 при n>=4,
    потолок статов min(по редкости, число ингредиентов), опыт 25*n;
  * ежедневки — Retention: 3 чередующихся типа, награда 120*(1+lvl/50).

Запуск: python3 tools/balance/season_sim.py
"""

import math
import random
import statistics
import sys
import os
from collections import Counter, defaultdict

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import balance_model as bm  # noqa: E402

RNG_SEED = 20261024
TRIALS = 80

# --- компания -------------------------------------------------------------
PLAYERS = 6
SESSIONS_PER_WEEK = 3          # пн / ср / пт
HOURS_PER_SESSION = 2.0
WEEKS = 8
WEEKLY_GOAL_KILLS = 6000

# --- дроп (проценты на событие лута, без множителя моба) -------------------
GEAR_DROP = 7.0
GEM_DROP = 1.5
RUNE_DROP = 0.15
INGREDIENT_DROP = 5.0
CURRENCY_DROP = 0.2

# --- pity ----------------------------------------------------------------
UNIQUE_PITY = (250, 900)       # по «дропам гира», счётчик в убийствах
RUNE_PITY = (200, 700)         # по убийствам
GEM_PITY = (120, 400)          # по убийствам

GEAR_WEIGHTS = {"common": 5000, "uncommon": 2000, "rare": 300, "epic": 100,
                "unique": 100, "runeword": 0}
UNIQUE_POOL = 18               # 7 колец, 3 амулета, шлем, 4 оружия, 3 брони

# 15 рун: (id, вес, тир)
RUNES = [("yun", 800, 4), ("ven", 800, 4), ("oru", 800, 4), ("wir", 500, 4),
         ("nos", 1000, 1), ("mos", 1000, 1), ("ita", 1000, 1),
         ("cen", 1000, 2), ("fey", 1000, 2), ("dos", 1000, 2),
         ("ano", 1000, 2), ("toq", 1000, 2),
         ("eno", 1000, 3), ("har", 1000, 3), ("xer", 1000, 3)]

RUNEWORDS = {
    "heavenly_rainbow (лук)": ["cen", "dos", "yun"],
    "bone_shatterer (шлем)": ["ita", "dos", "toq"],
    "heavenly_tear (амулет)": ["toq", "dos", "yun", "wir"],
    "air_disaster (кольцо)": ["ano", "oru", "dos"],
    "playful_hope (кольцо)": ["yun", "fey", "oru"],
    "insight (посох)": ["yun", "mos", "oru", "ven"],
    "skull_cleaver (меч)": ["ano", "toq", "mos", "har"],
}

# ранги: (tier, вес); дробные пороги из GemRank.lvlToDrop * MAX_LEVEL.
# В Java ранг одинаково часто выпадает для каждого из 9 типов камня.
GEM_RANKS = [(0, 100999), (1, 25999), (2, 5000), (3, 1000), (4, 400), (5, 250), (6, 200)]
GEM_DROP_LEVEL_FRACTIONS = [0.0, 0.1, 0.2, 0.5, 0.75, 0.9, 0.95]
GEM_MIN_LEVEL = {rank: max(1, math.ceil(fraction * bm.MAX_LEVEL))
                 for rank, fraction in enumerate(GEM_DROP_LEVEL_FRACTIONS)}
GEM_UPGRADE_CHANCES = {0: 100, 1: 75, 2: 50, 3: 30, 4: 15, 5: 10}
GEM_TYPES = 9

CRAFT_N = 4
CRAFT_EXP_PER_INGREDIENT = 40
CRAFT_SUCCESS = 100 - 10 * (CRAFT_N - 1)
CRAFT_RARITY_WEIGHTS = [("uncommon", 2000), ("rare", 300), ("epic", 100)]
CRAFT_STAT_CAP = {"common": 2, "uncommon": 3, "rare": 4, "epic": 5}


def kills_for_level(level):
    """Сколько убийств нужно на переход с level на level+1 (бом, своего уровня)."""
    return bm.exp_required(level + 1) / bm.mob_exp(level)


def auto_target_minutes(level, start=14.0, end=70.0, curve=1.0):
    """AutoBalance.targetMinutes: геометрическая интерполяция 14 -> 70 мин."""
    max_level = bm.MAX_LEVEL
    t = min(1.0, max(0.0, (level - 1) / float(max_level - 1)))
    t = t ** curve
    return start * ((end / start) ** t)


def tier_of_level(level):
    """GameBalanceConfig.getTierMap: 0-9 -> 0, 10-19 -> 1, ... 50+ -> 5."""
    return min(5, max(0, int(level // 10)))


def tier_to_level(tier):
    """LevelUtils.tierToLevel: tier 0 starts at level 1; later tiers at 10N."""
    return max(1, tier * 10)


def profession_exp_required(level):
    exponent = min(0.15, max(0.0, 0.1 * level / bm.MAX_LEVEL))
    return int((25 + 20 * level) ** (1 + exponent))


def pity_ramp(n, start, full):
    if n < start:
        return 0.0
    return 0.9 * min(1.0, (n - start) / float(full - start))


def weighted_choice(rng, pairs):
    total = sum(w for _, w in pairs)
    roll = rng.uniform(0, total)
    acc = 0.0
    for value, w in pairs:
        acc += w
        if roll <= acc:
            return value
    return pairs[-1][0]


def gear_rarity(rng):
    return weighted_choice(rng, list(GEAR_WEIGHTS.items()))


def daily_target(kind, level, cap=40):
    if kind == 1:
        return int(min(6, max(2, 2 + level // 10)))
    if kind == 2:
        return int(min(6, max(2, 2 + level // 10)))
    return int(min(cap, max(12, round(12 + level * 1.5))))


class Player:
    def __init__(self, rng):
        self.rng = rng
        self.level = 1
        self.exp = 0
        self.kills = 0
        self.minutes = 0.0

        self.since_unique = 0
        self.since_rune = 0
        self.since_gem = 0

        self.gear = 0
        self.uniques = 0
        self.codex = set()
        self.runes = Counter()              # остаток после создания рунных слов
        self.runes_found = 0                # все выпавшие руны за сезон
        self.gems = Counter()                 # прямые выпадения по рангу
        self.gem_inventory = Counter()        # (тип камня, ранг), после апгрейдов
        self.gem_upgrade_attempts = Counter()
        self.ingredients = Counter()  # ingredient tier -> item count
        self.currency = 0
        self.favor = 500.0

        self.crafts = 0
        self.crafts_ok = 0
        self.crafted_stats = 0
        self.prof_level = 1
        self.prof_exp = 0
        self.prof_level_at_player50 = None
        self.crafts_at_player50 = None
        self.crafts_ok_at_player50 = None

        self.first = {}
        self.runewords_done = []
        self.seal = False

    # ------------------------------------------------------------------
    def loot_event(self):
        rng = self.rng
        mod = bm.loot_modifier(self.level)
        self.kills += 1
        self.since_unique += 1
        self.since_rune += 1
        self.since_gem += 1

        # валюта
        if self.level >= 11 and rng.random() * 100 < CURRENCY_DROP * mod:
            self.currency += 1

        # ингредиенты
        if rng.random() * 100 < INGREDIENT_DROP * mod:
            self.ingredients[tier_of_level(self.level)] += rng.randint(1, 3)

        # гир-душа
        if rng.random() * 100 < GEAR_DROP * mod:
            self.gear += 1
            base = 100.0 / sum(GEAR_WEIGHTS.values())
            if rng.random() < base + pity_ramp(self.since_unique, *UNIQUE_PITY):
                self.since_unique = 0
                self.uniques += 1
                self.codex.add(rng.randrange(UNIQUE_POOL))
                self.mark("unique")
            else:
                gear_rarity(rng)

        # самоцвет
        if (rng.random() * 100 < GEM_DROP * mod
                or rng.random() < pity_ramp(self.since_gem, *GEM_PITY)):
            self.since_gem = 0
            pool = [(rank, weight) for rank, weight in GEM_RANKS
                    if GEM_MIN_LEVEL[rank] <= self.level]
            rank = weighted_choice(rng, pool)
            gem_type = rng.randrange(GEM_TYPES)
            self.gems[rank] += 1
            self.gem_inventory[(gem_type, rank)] += 1
            self.try_gem_upgrades(gem_type)
            self.mark("gem")

        # руна
        if (rng.random() * 100 < RUNE_DROP * mod
                or rng.random() < pity_ramp(self.since_rune, *RUNE_PITY)):
            self.since_rune = 0
            tier = tier_of_level(self.level)
            pool = [(r[0], r[1]) for r in RUNES if r[2] <= tier]
            if pool:
                self.runes[weighted_choice(rng, pool)] += 1
                self.runes_found += 1
            self.mark("rune")

        self.favor = max(0.0, self.favor - 0.1)

    def mark(self, key):
        self.first.setdefault(key, self.minutes)

    def try_gem_upgrades(self, gem_type):
        """Автоматически пробует все доступные 3→1 апгрейды этого типа.

        Это модель оптимального игрока, который использует каждую доступную попытку;
        3 камня расходуются и при неудаче, как в GemItem.finishUsingItem.
        """
        for rank in range(6):
            key = (gem_type, rank)
            while self.gem_inventory[key] >= 3:
                self.gem_inventory[key] -= 3
                self.gem_upgrade_attempts[rank] += 1
                if self.rng.random() * 100 < GEM_UPGRADE_CHANCES[rank]:
                    self.gem_inventory[(gem_type, rank + 1)] += 1

    # ------------------------------------------------------------------
    def craft(self):
        made = 0
        while sum(self.ingredients.values()) >= CRAFT_N:
            # В модели игрок подбирает четыре самых высоких tier-инструмента/материала.
            remaining = CRAFT_N
            combo = []
            for tier in sorted(self.ingredients, reverse=True):
                take = min(remaining, self.ingredients[tier])
                combo.extend([tier] * take)
                remaining -= take
                if remaining == 0:
                    break
            if len(combo) < CRAFT_N:
                break

            for tier, count in Counter(combo).items():
                self.ingredients[tier] -= count
            avg_tier = sum(combo) // CRAFT_N
            item_level = tier_to_level(avg_tier)
            success_chance = CRAFT_SUCCESS
            if self.prof_level > item_level + 9:
                success_chance += 10
            elif self.prof_level > item_level + 4:
                success_chance += 5
            elif self.prof_level < item_level:
                success_chance -= min(30, (item_level - self.prof_level) * 2)

            self.crafts += 1
            if self.rng.random() * 100 >= max(0, min(100, success_chance)):
                continue
            self.crafts_ok += 1
            made += 1
            rar = weighted_choice(self.rng, CRAFT_RARITY_WEIGHTS)
            self.crafted_stats += min(CRAFT_STAT_CAP[rar], CRAFT_N)
            self.add_prof_exp(CRAFT_EXP_PER_INGREDIENT * CRAFT_N)
        return made

    def add_prof_exp(self, amount):
        max_skill_level = min(999, self.level + 5)
        if self.prof_level >= max_skill_level:
            return
        self.prof_exp += amount
        while self.prof_level < max_skill_level:
            need = profession_exp_required(self.prof_level + 1)
            if self.prof_exp < need:
                break
            self.prof_exp -= need
            self.prof_level += 1

    # ------------------------------------------------------------------
    def try_runewords(self):
        for word, need in RUNEWORDS.items():
            if word in self.runewords_done:
                continue
            if all(self.runes[r] > 0 for r in need):
                for r in need:
                    self.runes[r] -= 1
                self.runewords_done.append(word)
                self.mark("runeword")

    def play_minutes(self, minutes, goal_kind, goal_state, goal_target):
        """Отыгрывает минуты сессии; target фиксируется при назначении цели на входе."""
        step = 5.0
        while minutes > 0:
            self.minutes += step
            minutes -= step

            kills_per_min = kills_for_level(self.level) / auto_target_minutes(self.level)
            step_kills = max(1, int(round(kills_per_min * step)))
            for _ in range(step_kills):
                self.loot_event()
            goal_state["kills"] += step_kills
            goal_state["chests"] += 0.5    # допущение: один сундук примерно за 10 мин исследования

            # опыт и уровень
            self.exp += bm.mob_exp(self.level) * step_kills
            while self.level < bm.MAX_LEVEL and self.exp >= bm.exp_required(self.level + 1):
                self.exp -= bm.exp_required(self.level + 1)
                self.level += 1
                self.mark("level_%d" % self.level)
                if self.level in (10, 20, 30, 40, 50):
                    self.first.setdefault("lvl%d" % self.level, self.minutes)

            # Цель ремесла учитывает только успешные крафты в этой сессии,
            # а не накопленное за сезон число попыток.
            goal_state["crafts"] += self.craft()
            self.try_runewords()
            if self.level >= bm.MAX_LEVEL and self.prof_level_at_player50 is None:
                self.prof_level_at_player50 = self.prof_level
                self.crafts_at_player50 = self.crafts
                self.crafts_ok_at_player50 = self.crafts_ok

            progress = (goal_state["kills"] if goal_kind == 0 else
                        goal_state["chests"] if goal_kind == 1 else
                        goal_state["crafts"])
            if progress >= goal_target:
                goal_state["done"] = True


def run_trial(seed):
    players = [Player(random.Random(seed * 31 + i)) for i in range(PLAYERS)]

    goals_done = Counter()
    goals_total = Counter()
    weekly = defaultdict(list)

    for week in range(WEEKS):
        # Три вечера в неделю: понедельник, среда, пятница; следующий понедельник
        # наступает через три дня после пятницы. Цикл цели привязан к UTC-дню, как в Java.
        session_days = (week * 7, week * 7 + 2, week * 7 + 4)
        for day_index in session_days:
            for p in players:
                goal_level = p.level
                kind = (day_index + goal_level // 10) % 3
                target = daily_target(kind, goal_level)  # фиксируется при выдаче цели
                state = {"kills": 0, "chests": 0.0, "crafts": 0, "done": False}
                p.play_minutes(HOURS_PER_SESSION * 60, kind, state, target)
                goals_total[kind] += 1
                if state["done"]:
                    goals_done[kind] += 1
                    p.favor += int(round(120 * (1 + goal_level / 50.0)))

        weekly[week + 1] = [p.level for p in players]

    return players, goals_done, goals_total, weekly


def fmt_table(rows, headers):
    out = ["| " + " | ".join(headers) + " |",
           "|" + "|".join(["---"] * len(headers)) + "|"]
    for r in rows:
        out.append("| " + " | ".join(str(x) for x in r) + " |")
    return "\n".join(out)


def main():
    all_players, goals_done, goals_total, weekly = [], Counter(), Counter(), {}

    week_levels = defaultdict(list)
    company_words_per_trial = []
    company_word_trials = Counter()
    company_glorious_trials = 0
    company_divine_trials = 0
    player_glorious_count = 0
    player_divine_count = 0
    for i in range(TRIALS):
        players, done, total, weekly_levels = run_trial(RNG_SEED + i)
        all_players.extend(players)
        goals_done.update(done)
        goals_total.update(total)
        words_this_company = {word for player in players for word in player.runewords_done}
        company_words_per_trial.append(len(words_this_company))
        company_word_trials.update(words_this_company)
        has_glorious = [sum(player.gem_inventory[(t, 5)] for t in range(GEM_TYPES)) > 0
                        for player in players]
        has_divine = [sum(player.gem_inventory[(t, 6)] for t in range(GEM_TYPES)) > 0
                      for player in players]
        company_glorious_trials += any(has_glorious)
        company_divine_trials += any(has_divine)
        player_glorious_count += sum(has_glorious)
        player_divine_count += sum(has_divine)
        for w, levels in weekly_levels.items():
            week_levels[w].append(statistics.mean(levels))

    n = len(all_players)
    print("# Сезон: %d игроков, %d сессий по %.0f ч, %d недель (симуляция %d сезонов)\n"
          % (PLAYERS, SESSIONS_PER_WEEK * WEEKS, HOURS_PER_SESSION, WEEKS, TRIALS))

    print("## Рост по неделям (средний уровень в компании)\n")
    rows = [[w, "%.1f" % statistics.mean(v), "%.1f" % (statistics.mean(v) / bm.MAX_LEVEL * 100)]
            for w, v in sorted(week_levels.items())]
    print(fmt_table(rows, ["Неделя", "Средний уровень", "% от 50"]))
    print()

    def stat(fn):
        vals = [fn(p) for p in all_players]
        return statistics.mean(vals), statistics.median(vals)

    print("## Итоги сезона на одного игрока\n")
    rows = [
        ["Часов в игре", "%.0f" % statistics.mean([p.minutes / 60 for p in all_players])],
        ["Убийств", "%.0f" % statistics.mean([p.kills for p in all_players])],
        ["Уровень к концу сезона", "%.1f" % statistics.mean([p.level for p in all_players])],
        ["Душ гира", "%.0f" % statistics.mean([p.gear for p in all_players])],
        ["Уникатов", "%.1f" % statistics.mean([p.uniques for p in all_players])],
        ["Уникатов в кодексе (из 18)", "%.1f" % statistics.mean([len(p.codex) for p in all_players])],
        ["Рун выпало всего", "%.1f" % statistics.mean([p.runes_found for p in all_players])],
        ["Рун осталось после рунных слов", "%.1f" % statistics.mean([sum(p.runes.values()) for p in all_players])],
        ["Рунных слов собрано (из 7)", "%.2f" % statistics.mean([len(p.runewords_done) for p in all_players])],
        ["Самоцветов", "%.0f" % statistics.mean([sum(p.gems.values()) for p in all_players])],
        ["Ингредиентов осталось в сумке", "%.0f" % statistics.mean([sum(p.ingredients.values()) for p in all_players])],
        ["Крафтов / успешных", "%.0f / %.0f" % (statistics.mean([p.crafts for p in all_players]),
                                               statistics.mean([p.crafts_ok for p in all_players]))],
        ["Статов от крафта (сумма)", "%.0f" % statistics.mean([p.crafted_stats for p in all_players])],
        ["Уровень профессии", "%.1f" % statistics.mean([p.prof_level for p in all_players])],
        ["Благосклонность в конце", "%.0f" % statistics.mean([p.favor for p in all_players])],
    ]
    print(fmt_table(rows, ["Показатель", "За сезон"]))
    print()

    reached_max = [p for p in all_players if p.prof_level_at_player50 is not None]
    if reached_max:
        print("## Профессия в момент достижения 50 уровня\n")
        print("| Показатель | Среднее | Медиана |")
        print("|---|---:|---:|")
        print("| Уровень профессии | %.1f | %.1f |" % (
            statistics.mean(p.prof_level_at_player50 for p in reached_max),
            statistics.median(p.prof_level_at_player50 for p in reached_max)))
        print("| Попытки крафта / успешные | %.1f / %.1f | %.0f / %.0f |" % (
            statistics.mean(p.crafts_at_player50 for p in reached_max),
            statistics.mean(p.crafts_ok_at_player50 for p in reached_max),
            statistics.median(p.crafts_at_player50 for p in reached_max),
            statistics.median(p.crafts_ok_at_player50 for p in reached_max)))
        print()

    print("## Когда случается впервые (медиана по всем игрокам/сезонам)\n")
    keys = [("gem", "Первый самоцвет"), ("rune", "Первая руна"), ("unique", "Первый уникат"),
            ("runeword", "Первое рунное слово"), ("lvl10", "10 уровень"), ("lvl20", "20 уровень"),
            ("lvl30", "30 уровень"), ("lvl40", "40 уровень"), ("lvl50", "50 уровень")]
    rows = []
    for key, label in keys:
        vals = [p.first[key] for p in all_players if key in p.first]
        if not vals:
            rows.append([label, "—", "—"])
            continue
        med = statistics.median(vals)
        rows.append([label, "%.0f мин" % med, "сессия %.1f" % (med / 120.0)])
    print(fmt_table(rows, ["Событие", "Медиана", "Когда"]))
    print()

    print("## Ежедневная цель: доля выполненных\n")
    names = {0: "убийства", 1: "сундуки", 2: "ремесло"}
    rows = []
    for kind in (0, 1, 2):
        if goals_total[kind]:
            share = goals_done[kind] / goals_total[kind] * 100
            rows.append([names[kind], "%.0f%%" % share])
    print(fmt_table(rows, ["Тип цели", "Выполнено"]))
    print()

    print("## Рунные слова: сколько собрала компания из 6 человек\n")
    together = Counter()
    for p in all_players:
        for w in p.runewords_done:
            together[w] += 1
    rows = []
    for word in RUNEWORDS:
        per_player = together[word] / n
        company_share = company_word_trials[word] / TRIALS * 100
        rows.append([word, "%.0f%%" % (per_player * 100), "%.0f%%" % company_share])
    print(fmt_table(rows, ["Рунное слово", "Игрок собрал за сезон", "Есть у ≥1 игрока за сезон"]))
    print()

    company_kills_week = sum(p.kills for p in all_players) / n * PLAYERS / WEEKS
    print("## Компания целиком\n")
    print("| Показатель | Значение |")
    print("|---|---:|")
    print("| Убийств компании за неделю | %.0f |" % company_kills_week)
    print("| Общая цель недели (по умолчанию) | %d |" % WEEKLY_GOAL_KILLS)
    print("| Доля недельной цели, которую делает компания из 6 | %.0f%% |"
          % (company_kills_week / WEEKLY_GOAL_KILLS * 100))
    print("| Уникатов у компании за сезон | %.0f |" % (statistics.mean([p.uniques for p in all_players]) * PLAYERS))
    mean_words = statistics.mean(company_words_per_trial)
    full_set = sum(count == len(RUNEWORDS) for count in company_words_per_trial) / TRIALS * 100
    print("| Среднее число разных рунных слов у компании | %.1f из %d |" % (mean_words, len(RUNEWORDS)))
    print("| Сезонов, где компания закрыла весь набор | %.0f%% |" % full_set)
    print()
    print("### Недельная цель по размеру компании (по убийствам на игрока)\n")
    print("| Игроков в компании | Убийств/неделю | % цели | Сессий до цели (из 3) |")
    print("|---:|---:|---:|---:|")
    per_player_week = company_kills_week / PLAYERS
    for group_size in (4, 5, 6):
        group_week = per_player_week * group_size
        sessions_to_goal = WEEKLY_GOAL_KILLS / (group_week / SESSIONS_PER_WEEK)
        print("| %d | %.0f | %.0f%% | %.1f |" %
              (group_size, group_week, group_week / WEEKLY_GOAL_KILLS * 100, sessions_to_goal))
    print()

    print("## Самоцветы: прямые выпадения по рангам (на игрока за сезон)\n")
    rank_names = ["Cracked", "Chipped", "Flawed", "Regular", "Grand", "Glorious", "Divine"]
    rows = []
    for r in range(7):
        avg = statistics.mean([p.gems.get(r, 0) for p in all_players])
        rows.append([rank_names[r], "%.2f" % avg])
    print(fmt_table(rows, ["Ранг", "Прямых выпадений"]))
    print()

    print("## Самоцветы после апгрейдов (на игрока, если улучшать всё доступное)\n")
    rows = []
    for r in range(7):
        avg = statistics.mean([sum(p.gem_inventory[(t, r)] for t in range(GEM_TYPES))
                               for p in all_players])
        rows.append([rank_names[r], "%.2f" % avg])
    print(fmt_table(rows, ["Ранг", "Осталось в инвентаре"]))
    attempts = statistics.mean([sum(p.gem_upgrade_attempts.values()) for p in all_players])
    print("\nСреднее число попыток апгрейда за сезон: %.1f на игрока." % attempts)
    print("Игроки, у которых остался Glorious / Divine: %.0f%% / %.0f%%." %
          (player_glorious_count / n * 100, player_divine_count / n * 100))
    print("Компании, где хотя бы один игрок имеет Glorious / Divine: %.0f%% / %.0f%%.\n" %
          (company_glorious_trials / TRIALS * 100, company_divine_trials / TRIALS * 100))

    print("## Что это значит\n")
    print("* 48 часов игры за 8 недель — это 24 вечера по 2 часа; 50 уровень берётся")
    print("  примерно на 15-й сессии, остаток сезона — эндгейм (рунные слова, кодекс, башни).")
    print("* Компания из 6 игроков получает в среднем %.1f из %d видов рунных слов; полный набор есть в %.0f%% сезонов." %
          (statistics.mean(company_words_per_trial), len(RUNEWORDS), full_set))
    print("* При апгрейде каждого доступного набора 3→1, Glorious остаётся у %.0f%% игроков," %
          (player_glorious_count / n * 100))
    print("  Divine — у %.0f%%; хотя бы один такой камень есть в %.0f%% / %.0f%% компаний." %
          (player_divine_count / n * 100, company_glorious_trials / TRIALS * 100,
           company_divine_trials / TRIALS * 100))
    print("* Крафт даёт ~%.1f стата на предмет и уровень профессии ~%.0f —"
          % (statistics.mean([p.crafted_stats / max(1, p.crafts_ok) for p in all_players]),
             statistics.mean([p.prof_level for p in all_players])))
    print("  профессия растёт вместе с персонажем, но не обгоняет его (кап уровня +5).")


if __name__ == "__main__":
    main()
