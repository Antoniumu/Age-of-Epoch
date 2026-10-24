#!/usr/bin/env python3
"""
Проверка инвариантов: константы в коде должны совпадать с теми, на которых
построен LAUNCH-READINESS.md (и на которых считает tools/balance/*).

Зачем: отчёт обещает конкретные числа (дроп, pity, потолки апгрейда, веса
редкостей, очки талантов). Если кто-то поменяет константу в Java, отчёт
молча станет врать. Этот скрипт падает, если число разошлось.

Запуск:  python3 tools/verify/check_invariants.py
Выход:   0 — всё совпало, 1 — есть расхождения (таблица покажет, что именно).
"""

import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
SRC = ROOT / "src/main/java/com/robertx22/age_of_exile"

SERVER = SRC / "config/forge/ServerContainer.java"
BALANCE = SRC / "database/data/game_balance_config/GameBalanceConfig.java"
BALANCE_MODEL = ROOT / "tools/balance/balance_model.py"
RARITY = SRC / "aoe_data/database/gear_rarities/GearRaritiesAdder.java"
GEAR_DATA = SRC / "saveclasses/item_classes/GearItemData.java"
RARITY_STONE = SRC / "vanilla_mc/items/RarityUpgradeStone.java"
GM_CONFIG = SRC / "gamemaster/GameMasterConfig.java"
RETENTION = SRC / "gamemaster/Retention.java"
PLAYER_SKILL_DATA = SRC / "saveclasses/player_skills/PlayerSkillData.java"
CRAFT_PROCESS = SRC / "player_skills/ingredient/data/CraftingProcessData.java"
GEAR_SOUL_TICK = SRC / "uncommon/utilityclasses/GearSoulOnInvTick.java"
SPELL = SRC / "database/data/spells/components/Spell.java"
GEM = SRC / "vanilla_mc/items/gemrunes/GemItem.java"
GEM_DATA = SRC / "database/data/gems/Gem.java"
GEMS_REGISTRY = SRC / "aoe_data/database/gems/Gems.java"
RUNES = SRC / "vanilla_mc/items/gemrunes/RuneItem.java"
UNIQUES = SRC / "aoe_data/database/unique_gears"
PERKS = SRC / "aoe_data/database/perks"
SCHOOLS = SRC / "aoe_data/database/spell_schools/SpellSchoolsAdder.java"
SIM = ROOT / "tools/balance/season_sim.py"

# (описание, файл, regexp, ожидаемое значение)
# Ожидаемое значение — либо float (сравнивается с допуском 1e-9),
# либо строка (точное совпадение с захваченной группой).
NUM_CHECKS = [
    ("дроп гира, % на событие лута", SERVER,
     r'GEAR_DROPRATE = b\.defineInRange\("gear_drop_rate", ([\d.]+)D', 7.0),
    ("дроп самоцветов, %", SERVER,
     r'GEM_DROPRATE = b\.defineInRange\("gem_drop_rate", ([\d.]+)D', 1.5),
    ("дроп ингредиентов, %", SERVER,
     r'INGREDIENT_DROPRATE = b\.defineInRange\("ingredient_drop_rate", ([\d.]+)D', 5.0),
    ("дроп рун, %", SERVER,
     r'RUNE_DROPRATE = b\.defineInRange\("rune_drop_rate", ([\d.]+)D', 0.15),
    ("дроп валюты, %", SERVER,
     r'CURRENCY_DROPRATE = b\.defineInRange\("currency_drop_rate", ([\d.]+)D', 0.2),
    ("мобы: доп. статы за уровень", SERVER,
     r'EXTRA_MOB_STATS_PER_LEVEL = b\.defineInRange\("extra_mob_stats_per_lvl", ([\d.]+)D', 0.01),
    ("макс. надетых уникатов", SERVER,
     r'MAX_UNIQUE_GEARS_WORN = b\.defineInRange\("max_uniques_worn", (\d+)', 3),
    ("штраф за разницу уровней / уровень", SERVER,
     r'LEVEL_DISTANCE_PENALTY_PER_LVL = b\.defineInRange\("lvl_distance_penalty_per_level", ([\d.]+)D', 0.1),
    ("макс. уровень", BALANCE, r"public int MAX_LEVEL = (\d+);", 50),
    ("очков талантов к макс. уровню", BALANCE,
     r"TALENT_POINTS_AT_MAX_LEVEL = ([\d.]+);", 100),
    ("стат-очков за уровень", BALANCE, r"STAT_POINTS_PER_LEVEL = ([\d.]+);", 2),
    ("очков заклинаний за уровень", BALANCE, r"SPELL_POINTS_PER_LEVEL = ([\d.]+);", 2),
    ("уровней в тире", BALANCE, r"levels_per_tier = (\d+);", 10),
    ("макс. уровень заклинания", SPELL, r"public int max_lvl = (\d+);", 16),
    ("проф. XP на ингредиент, в игре", GEAR_SOUL_TICK,
     r"int exp = \(int\) \((\d+) \* pdata\.getIngredientsCount\(\)\);", 40),
    ("проф. XP на ингредиент, симуляция", SIM,
     r"^CRAFT_EXP_PER_INGREDIENT = (\d+)", 40),
    ("вес common", RARITY, r"common\.weight = (\d+);", 5000),
    ("вес uncommon", RARITY, r"uncommon\.weight = (\d+);", 2000),
    ("вес rare", RARITY, r"rare\.weight = (\d+);", 300),
    ("вес epic", RARITY, r"epic\.weight = (\d+);", 100),
    ("вес unique", RARITY, r"unique\.weight = (\d+);", 100),
    ("вес runeword (только крафт)", RARITY, r"runeword\.weight = (\d+);", 0),
    ("слотов апгрейда у uncommon", RARITY, r"uncommon\.upgrades = (\d+);", 2),
    ("слотов апгрейда у rare", RARITY, r"rare\.upgrades = (\d+);", 3),
    ("слотов апгрейда у epic", RARITY, r"epic\.upgrades = (\d+);", 5),
    ("слотов апгрейда у unique", RARITY, r"unique\.upgrades = (\d+);", 5),
    ("слотов апгрейда у runeword", RARITY, r"runeword\.upgrades = (\d+);", 5),
    ("порог повышения редкости uncommon", RARITY,
     r"uncommon\.upgrade_lvl_to_increase_rar = (\d+);", 4),
    ("порог повышения редкости rare", RARITY,
     r"rare\.upgrade_lvl_to_increase_rar = (\d+);", 8),
    ("pity уникатов: старт", GM_CONFIG, r"retention_pity_start_kills = (\d+);", 250),
    ("pity уникатов: полный", GM_CONFIG, r"retention_pity_full_kills = (\d+);", 900),
    ("pity рун: старт", GM_CONFIG, r"retention_pity_rune_start_kills = (\d+);", 200),
    ("pity рун: полный", GM_CONFIG, r"retention_pity_rune_full_kills = (\d+);", 700),
    ("pity гемов: старт", GM_CONFIG, r"retention_pity_gem_start_kills = (\d+);", 120),
    ("pity гемов: полный", GM_CONFIG, r"retention_pity_gem_full_kills = (\d+);", 400),
    ("шаг награды кодекса", GM_CONFIG, r"codex_reward_step = (\d+);", 4),
    ("цель недели, убийств", GM_CONFIG, r"social_goal_kills = (\d+);", 6000),
    ("льгота серии, дней", GM_CONFIG, r"retention_streak_grace_days = (\d+);", 3),
    ("абсолютный cap уровня профессии", PLAYER_SKILL_DATA, r"MAX_LEVEL = (\d+);", 999),
    ("вес руны YUN", RUNES, r'YUN\((\d+), "yun", "Yun", 4\)', 800),
    ("вес руны VEN", RUNES, r'VEN\((\d+), "ven", "Ven", 4\)', 800),
    ("вес руны ORU", RUNES, r'ORU\((\d+), "oru", "Oru", 4\)', 800),
    ("вес руны WIR", RUNES, r'WIR\((\d+), "wir", "Wir", 4\)', 500),
    ("шанс улучшить Regular", GEM, r'REGULAR\("Regular", 3, 0\.4F, (\d+), 1000, 0\.5F\)', 30),
    ("вес ранга Grand", GEM, r'GRAND\("Grand", 4, 0\.6F, 15, (\d+), 0\.75F\)', 400),
    ("шанс улучшить Grand", GEM, r'GRAND\("Grand", 4, 0\.6F, (\d+), 400, 0\.75F\)', 15),
    ("вес ранга Glorious", GEM, r'GLORIOUS\("Glorious", 5, 0\.8F, 10, (\d+), 0\.9F\)', 250),
    ("шанс улучшить Glorious", GEM, r'GLORIOUS\("Glorious", 5, 0\.8F, (\d+), 250, 0\.9F\)', 10),
    ("вес ранга Divine", GEM, r'DIVINE\("Divine", 6, 1F, 0, (\d+), 0\.95F\)', 200),
    # Симуляция должна считать на тех же ставках, что и игра.
    ("sim: дроп гира", SIM, r"^GEAR_DROP = ([\d.]+)", 7.0),
    ("sim: дроп самоцветов", SIM, r"^GEM_DROP = ([\d.]+)", 1.5),
    ("sim: дроп рун", SIM, r"^RUNE_DROP = ([\d.]+)", 0.15),
    ("sim: дроп ингредиентов", SIM, r"^INGREDIENT_DROP = ([\d.]+)", 5.0),
    ("sim: дроп валюты", SIM, r"^CURRENCY_DROP = ([\d.]+)", 0.2),
    ("sim: пул уникатов", SIM, r"^UNIQUE_POOL = (\d+)", 18),
    ("sim: число игроков", SIM, r"^PLAYERS = (\d+)", 6),
    ("sim: цель недели", SIM, r"^WEEKLY_GOAL_KILLS = (\d+)", 6000),
    ("model: цель недели", BALANCE_MODEL, r"weekly_goal = (\d+)", 6000),
]

STR_CHECKS = [
    ("камень редкости требует редкость предмета", RARITY_STONE,
     r"(gear\.getRarity\(\)[^;]*?GUID\(\)\s*\.equals\(rar\.GUID\(\)\))",
     None),
    ("Gems копирует порог GemRank в registry", GEMS_REGISTRY,
     r"(gem\.level_to_drop = x\.levelToStartDrop;)",
     "gem.level_to_drop = x.levelToStartDrop;"),
    ("Gem использует дробный порог выпадения", GEM_DATA,
     r"(Math\.ceil\(maxLevel \* level_to_drop\))",
     "Math.ceil(maxLevel * level_to_drop)"),
    ("симулятор масштабирует порог по MAX_LEVEL", SIM,
     r"(math\.ceil\(fraction \* bm\.MAX_LEVEL\))",
     "math.ceil(fraction * bm.MAX_LEVEL)"),
    ("профессия ограничена уровнем игрока +5", PLAYER_SKILL_DATA,
     r"(Math\.min\(MAX_LEVEL, Math\.max\(1, Load\.Unit\(player\)\.getLevel\(\) \+ 5\)\))",
     "Math.min(MAX_LEVEL, Math.max(1, Load.Unit(player).getLevel() + 5))"),
    ("soft-штраф ремесла в коде", CRAFT_PROCESS,
     r"(Math\.min\(30, \(itemLvl - skillLvl\) \* 2\))",
     "Math.min(30, (itemLvl - skillLvl) * 2)"),
    ("soft-штраф ремесла в симуляторе", SIM,
     r"(min\(30, \(item_level - self\.prof_level\) \* 2\))",
     "min(30, (item_level - self.prof_level) * 2)"),
    ("дневная цель ремесла в симуляторе", SIM,
     r"(min\(6, max\(2, 2 \+ level // 10\)\))",
     "min(6, max(2, 2 + level // 10))"),
    ("cap профессии в симуляторе совпадает с кодом", SIM,
     r"(min\(999, self\.level \+ 5\))",
     "min(999, self.level + 5)"),
]

# Паттерны, которых в коде быть НЕ должно.
ABSENT = [
    ("бросок кубика в камне редкости", RARITY_STONE, r"RandomUtils\.roll"),
    ("сброс апгрейдов при повышении редкости", GEAR_DATA, r"this\.up\.regenerate\(rar\)"),
    ("равенство weekly kill count не теряет цель при смене config", SRC / "gamemaster/GameMaster.java",
     r"data\.week_kills == config\.social_goal_kills"),
    ("старое отключение cap профессии при MAX_LEVEL", PLAYER_SKILL_DATA,
     r"GameBalanceConfig\.get\(\)\.MAX_LEVEL > lvl"),
]

# Подсчёты сущностей: (описание, файл(ы), regexp, ожидаемое количество)
COUNT_CHECKS = [
    ("видов рунных слов", [UNIQUES / "runewords"], r"UniqueGearBuilder\.of\(", 7),
    ("уникатов (включая fabled)", [UNIQUES / "uniques", UNIQUES / "fabled"],
     r"UniqueGearBuilder\.of\(", 18),
    ("записей в основном древе талантов", [PERKS / "TalentTreePerks.java"],
     r"^\s+register\(", 158),
    ("школ заклинаний", [SCHOOLS], r"SchoolBuilder\.of\(", 5),
    ("типов самоцветов (6 обычных + 3 стихийных)", [GEM],
     r"new (GemStatPerTypes|EleGem)\(", 9),
    ("достижимых ступеней бонусных сокетов", [GEAR_DATA],
     r"if \(upgradeLevel >= (4|8|12)\)", 3),
    ("дневные цели сундуков/ремесла с мягким target", [RETENTION],
     r"target = \(int\) clamp\(2 \+ level / 10, 2, Math\.min\(6, cap\)\)", 2),
]


def read(path):
    try:
        return path.read_text(encoding="utf-8")
    except FileNotFoundError:
        return None


def compare_rune_and_gem_data():
    """Проверяет, что симулятор использует ровно те веса/пороги, что и Java."""
    java_runes = read(RUNES) or ""
    sim = read(SIM) or ""
    rune_java = {
        rune_id: (int(weight), int(tier))
        for _, weight, rune_id, tier in re.findall(
            r'^\s*(YUN|VEN|NOS|MOS|ITA|CEN|FEY|DOS|ANO|TOQ|ORU|WIR|ENO|HAR|XER)'
            r'\((\d+), "([a-z]+)", "[^"]+", (\d+)\)',
            java_runes, flags=re.M)
    }
    rune_block = re.search(r"^RUNES\s*=\s*\[(.*?)^\s*\]", sim, flags=re.M | re.S)
    rune_sim = {}
    if rune_block:
        rune_sim = {rune_id: (int(weight), int(tier)) for rune_id, weight, tier in
                    re.findall(r'\("([a-z]+)",\s*(\d+),\s*(\d+)\)', rune_block.group(1))}

    java_gem_block = re.search(r"public enum GemRank\s*\{(.*?)\n\s*public String locName",
                               read(GEM) or "", flags=re.S)
    gem_pattern = (r'^\s*(CRACKED|CHIPPED|FLAWED|REGULAR|GRAND|GLORIOUS|DIVINE)'
                   r'\("[^"]+",\s*(\d+),\s*[\d.]+F,\s*(\d+),\s*(\d+),\s*([\d.]+)F\)')
    java_gems = {}
    if java_gem_block:
        for _, tier, chance, weight, fraction in re.findall(
                gem_pattern, java_gem_block.group(1), flags=re.M):
            java_gems[int(tier)] = (int(weight), int(chance), float(fraction))

    sim_weight_block = re.search(r"^GEM_RANKS\s*=\s*\[(.*?)\]", sim, flags=re.M | re.S)
    sim_weights = {}
    if sim_weight_block:
        sim_weights = {int(rank): int(weight) for rank, weight in
                       re.findall(r"\((\d+),\s*(\d+)\)", sim_weight_block.group(1))}
    chance_block = re.search(r"^GEM_UPGRADE_CHANCES\s*=\s*\{(.*?)\}", sim, flags=re.M | re.S)
    sim_chances = {}
    if chance_block:
        sim_chances = {int(rank): int(chance) for rank, chance in
                       re.findall(r"(\d+)\s*:\s*(\d+)", chance_block.group(1))}
    fraction_block = re.search(r"^GEM_DROP_LEVEL_FRACTIONS\s*=\s*\[(.*?)\]", sim,
                               flags=re.M | re.S)
    sim_fractions = {}
    if fraction_block:
        sim_fractions = {rank: float(value) for rank, value in enumerate(
            re.findall(r"[\d.]+", fraction_block.group(1)))}

    rune_ok = bool(rune_java) and rune_java == rune_sim
    gem_ok = bool(java_gems) and all(
        rank in sim_weights and rank in sim_fractions
        and (weight, chance, fraction) ==
            (sim_weights[rank], sim_chances.get(rank, 0), sim_fractions[rank])
        for rank, (weight, chance, fraction) in java_gems.items())
    return [
        ("веса/тиры рун: Java совпадает с симулятором",
         f"{len(rune_java)} / {len(rune_sim)} типов", "15 типов, полное совпадение",
         "OK" if rune_ok else "FAIL"),
        ("вес/шанс/уровень гемов: Java совпадает с симулятором",
         f"{len(java_gems)} / {len(sim_weights)} рангов", "7 рангов, полное совпадение",
         "OK" if gem_ok else "FAIL"),
    ]


def main():
    rows = []
    failed = 0

    for name, path, pattern, expected in NUM_CHECKS:
        text = read(path)
        if text is None:
            rows.append((name, "файл не найден", "-", "FAIL"))
            failed += 1
            continue
        m = re.search(pattern, text, flags=re.M)
        if not m:
            rows.append((name, "не найдено в коде", str(expected), "FAIL"))
            failed += 1
            continue
        raw = m.group(1)
        try:
            actual = float(raw)
        except ValueError:
            rows.append((name, raw, str(expected), "FAIL"))
            failed += 1
            continue
        ok = abs(actual - float(expected)) < 1e-9
        rows.append((name, raw, str(expected), "OK" if ok else "FAIL"))
        failed += 0 if ok else 1

    for name, path, pattern, expected in STR_CHECKS:
        text = read(path) or ""
        m = re.search(pattern, text, flags=re.S)
        if expected is None:
            actual = "найдено" if m else "не найдено в коде"
            ok = m is not None
            expected = "найдено"
        else:
            actual = m.group(1) if m else "не найдено в коде"
            ok = actual == expected
        rows.append((name, actual, expected, "OK" if ok else "FAIL"))
        failed += 0 if ok else 1

    for name, path, pattern in ABSENT:
        text = read(path) or ""
        found = re.search(pattern, text)
        rows.append((name, "найдено: " + found.group(0) if found else "отсутствует",
                     "отсутствует", "FAIL" if found else "OK"))
        failed += 1 if found else 0

    for name, paths, pattern, expected in COUNT_CHECKS:
        total = 0
        for p in paths:
            files = [p] if p.is_file() else sorted(p.rglob("*.java"))
            for f in files:
                total += len(re.findall(pattern, read(f) or "", flags=re.M))
        ok = total == expected
        rows.append((name, str(total), str(expected), "OK" if ok else "FAIL"))
        failed += 0 if ok else 1

    consistency_rows = compare_rune_and_gem_data()
    rows.extend(consistency_rows)
    failed += sum(row[3] != "OK" for row in consistency_rows)

    name_width = max(len(r[0]) for r in rows) + 2
    actual_width = 30
    expected_width = 30
    print(f"{'проверка':<{name_width}}{'в коде':>{actual_width}}"
          f"{'ожидалось':>{expected_width}}{'итог':>8}")
    print("-" * (name_width + actual_width + expected_width + 8))
    for name, actual, expected, status in rows:
        shown = str(actual)
        if len(shown) > actual_width:
            shown = "…" + shown[-(actual_width - 1):]
        print(f"{name:<{name_width}}{shown:>{actual_width}}"
              f"{str(expected):>{expected_width}}{status:>8}")

    print()
    if failed:
        print(f"РАСХОЖДЕНИЙ: {failed}. Отчёт LAUNCH-READINESS.md больше не соответствует коду.")
        return 1

    print(f"Все {len(rows)} инвариантов совпадают с LAUNCH-READINESS.md.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
