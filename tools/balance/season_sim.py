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
RUNES = [("yun", 400, 4), ("ven", 400, 4), ("oru", 600, 4), ("wir", 400, 4),
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

# ранги самоцветов: (тир, вес)
GEM_RANKS = [(0, 100999), (1, 25999), (2, 5000), (3, 1000), (4, 200), (5, 25), (6, 1)]

CRAFT_N = 4
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
    return tier * 10


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
        return int(min(8, max(3, 3 + level // 8)))
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
        self.runes = Counter()
        self.gems = Counter()
        self.ingredients = 0
        self.currency = 0
        self.favor = 500.0

        self.crafts = 0
        self.crafts_ok = 0
        self.crafted_stats = 0
        self.prof_level = 1
        self.prof_exp = 0

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
            self.ingredients += rng.randint(1, 3)

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
            tier = tier_of_level(self.level)
            pool = [(r, w) for r, w in GEM_RANKS if tier_to_level(r) <= self.level or r == 6]
            self.gems[weighted_choice(rng, pool)] += 1
            self.mark("gem")

        # руна
        if (rng.random() * 100 < RUNE_DROP * mod
                or rng.random() < pity_ramp(self.since_rune, *RUNE_PITY)):
            self.since_rune = 0
            tier = tier_of_level(self.level)
            pool = [(r[0], r[1]) for r in RUNES if r[2] <= tier]
            if pool:
                self.runes[weighted_choice(rng, pool)] += 1
            self.mark("rune")

        self.favor = max(0.0, self.favor - 0.1)

    def mark(self, key):
        self.first.setdefault(key, self.minutes)

    # ------------------------------------------------------------------
    def craft(self):
        made = 0
        while self.ingredients >= CRAFT_N:
            self.ingredients -= CRAFT_N
            self.crafts += 1
            if self.rng.random() * 100 >= CRAFT_SUCCESS:
                continue
            self.crafts_ok += 1
            made += 1
            rar = weighted_choice(self.rng, CRAFT_RARITY_WEIGHTS)
            self.crafted_stats += min(CRAFT_STAT_CAP[rar], CRAFT_N)
            self.add_prof_exp(25 * CRAFT_N)
        return made

    def add_prof_exp(self, amount):
        if self.prof_level >= self.level + 5 or self.prof_level >= bm.MAX_LEVEL:
            return
        self.prof_exp += amount
        while self.prof_level < bm.MAX_LEVEL and self.prof_level < self.level + 5:
            need = int((25 + 20 * self.prof_level) ** 1.07)
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

    def play_minutes(self, minutes, goal_kind, goal_state):
        """Отыгрывает минуты сессии. goal_state — счётчик цели дня."""
        step = 5.0
        while minutes > 0:
            self.minutes += step
            minutes -= step

            kills_per_min = kills_for_level(self.level) / auto_target_minutes(self.level)
            for _ in range(max(1, int(round(kills_per_min * step)))):
                self.loot_event()
                goal_state[0] += 1
                goal_state[1] += 0.5    # сундук примерно раз в 10 минут исследования

            # опыт и уровень
            self.exp += bm.mob_exp(self.level) * max(1, int(round(kills_per_min * step)))
            while self.level < bm.MAX_LEVEL and self.exp >= bm.exp_required(self.level + 1):
                self.exp -= bm.exp_required(self.level + 1)
                self.level += 1
                self.mark("level_%d" % self.level)
                if self.level in (10, 20, 30, 40, 50):
                    self.first.setdefault("lvl%d" % self.level, self.minutes)

            self.craft()
            self.try_runewords()

            if goal_kind == 0 and goal_state[0] >= daily_target(0, self.level):
                goal_state[2] = True
            elif goal_kind == 1 and goal_state[1] >= daily_target(1, self.level):
                goal_state[2] = True
            elif goal_kind == 2 and self.crafts >= daily_target(2, self.level):
                goal_state[2] = True


def run_trial(seed):
    rng = random.Random(seed)
    players = [Player(random.Random(seed * 31 + i)) for i in range(PLAYERS)]

    goals_done = Counter()
    goals_total = Counter()
    day = 0
    weekly = defaultdict(list)

    for week in range(WEEKS):
        for session in range(SESSIONS_PER_WEEK):
            day += 1
            for p in players:
                kind = (day + p.level // 10) % 3
                state = [0, 0.0, False]
                crafts_before = p.crafts_ok
                p.play_minutes(HOURS_PER_SESSION * 60, kind, state)
                goals_total[kind] += 1
                if kind == 2:
                    state[2] = (p.crafts_ok - crafts_before) >= daily_target(2, p.level) or state[2]
                if state[2]:
                    goals_done[kind] += 1
                    p.favor += int(round(120 * (1 + p.level / 50.0)))

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
    for i in range(TRIALS):
        players, done, total, weekly_levels = run_trial(RNG_SEED + i)
        all_players.extend(players)
        goals_done.update(done)
        goals_total.update(total)
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
        ["Рун", "%.1f" % statistics.mean([sum(p.runes.values()) + 0 for p in all_players])],
        ["Рунных слов собрано (из 7)", "%.2f" % statistics.mean([len(p.runewords_done) for p in all_players])],
        ["Самоцветов", "%.0f" % statistics.mean([sum(p.gems.values()) for p in all_players])],
        ["Ингредиентов осталось в сумке", "%.0f" % statistics.mean([p.ingredients for p in all_players])],
        ["Крафтов / успешных", "%.0f / %.0f" % (statistics.mean([p.crafts for p in all_players]),
                                               statistics.mean([p.crafts_ok for p in all_players]))],
        ["Статов от крафта (сумма)", "%.0f" % statistics.mean([p.crafted_stats for p in all_players])],
        ["Уровень профессии", "%.1f" % statistics.mean([p.prof_level for p in all_players])],
        ["Благосклонность в конце", "%.0f" % statistics.mean([p.favor for p in all_players])],
    ]
    print(fmt_table(rows, ["Показатель", "За сезон"]))
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
        rows.append([word, "%.2f" % per_player,
                     "%.0f%%" % (100 * (1 - (1 - per_player) ** PLAYERS))])
    print(fmt_table(rows, ["Рунное слово", "На игрока", "Хотя бы у одного из 6"]))
    print()

    company_kills_week = sum(p.kills for p in all_players) / n * PLAYERS / WEEKS
    print("## Компания целиком\n")
    print("| Показатель | Значение |")
    print("|---|---:|")
    print("| Убийств компании за неделю | %.0f |" % company_kills_week)
    print("| Общая цель недели (по умолчанию) | %d |" % 8000)
    print("| Доля недельной цели, которую делает компания | %.0f%% |"
          % (company_kills_week / 8000 * 100))
    print("| Уникатов у компании за сезон | %.0f |" % (statistics.mean([p.uniques for p in all_players]) * PLAYERS))
    distinct = len([w for w in RUNEWORDS if together[w] > 0])
    print("| Видов рунных слов собрано компанией | %d из %d |" % (distinct, len(RUNEWORDS)))
    print()

    print("## Самоцветы по рангам (на игрока за сезон)\n")
    rank_names = ["Cracked", "Chipped", "Flawed", "Regular", "Grand", "Glorious", "Divine"]
    rows = []
    for r in range(7):
        avg = statistics.mean([p.gems.get(r, 0) for p in all_players])
        rows.append([rank_names[r], "%.1f" % avg])
    print(fmt_table(rows, ["Ранг", "Штук"]))
    print()

    print("## Что это значит\n")
    print("* 48 часов игры за 8 недель — это 24 вечера по 2 часа; 50 уровень берётся")
    print("  примерно на 15-й сессии, остаток сезона — эндгейм (рунные слова, кодекс, башни).")
    print("* Руны идут ровно, но рунных слов на игрока за сезон единицы — это")
    print("  «долгая цель», а не расходник; компания из 6 собирает почти весь набор.")
    print("* Самоцветы сыпятся стабильно, но выше Flawed их приходится собирать")
    print("  по 3 штуки: Divine практически недостижим ни дропом, ни апгрейдом.")
    print("* Крафт даёт ~%.1f стата на предмет и уровень профессии ~%.0f —"
          % (statistics.mean([p.crafted_stats / max(1, p.crafts_ok) for p in all_players]),
             statistics.mean([p.prof_level for p in all_players])))
    print("  профессия растёт вместе с персонажем, но не обгоняет его (кап уровня +5).")


if __name__ == "__main__":
    main()
