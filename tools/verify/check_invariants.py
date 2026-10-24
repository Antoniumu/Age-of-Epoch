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
RARITY = SRC / "aoe_data/database/gear_rarities/GearRaritiesAdder.java"
GEAR_DATA = SRC / "saveclasses/item_classes/GearItemData.java"
RARITY_STONE = SRC / "vanilla_mc/items/RarityUpgradeStone.java"
GM_CONFIG = SRC / "gamemaster/GameMasterConfig.java"
RETENTION = SRC / "gamemaster/Retention.java"
SPELL = SRC / "database/data/spells/components/Spell.java"
GEM = SRC / "vanilla_mc/items/gemrunes/GemItem.java"
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
     r"uncommon\.upgrade_lvl_to_increase_rar = (\d+);", 2),
    ("порог повышения редкости rare", RARITY,
     r"rare\.upgrade_lvl_to_increase_rar = (\d+);", 3),
    ("pity уникатов: старт", GM_CONFIG, r"retention_pity_start_kills = (\d+);", 250),
    ("pity уникатов: полный", GM_CONFIG, r"retention_pity_full_kills = (\d+);", 900),
    ("pity рун: старт", GM_CONFIG, r"retention_pity_rune_start_kills = (\d+);", 200),
    ("pity рун: полный", GM_CONFIG, r"retention_pity_rune_full_kills = (\d+);", 700),
    ("pity гемов: старт", GM_CONFIG, r"retention_pity_gem_start_kills = (\d+);", 120),
    ("pity гемов: полный", GM_CONFIG, r"retention_pity_gem_full_kills = (\d+);", 400),
    ("шаг награды кодекса", GM_CONFIG, r"codex_reward_step = (\d+);", 4),
    ("цель недели, убийств", GM_CONFIG, r"social_goal_kills = (\d+);", 8000),
    ("льгота серии, дней", GM_CONFIG, r"retention_streak_grace_days = (\d+);", 3),
    # Симуляция должна считать на тех же ставках, что и игра.
    ("sim: дроп гира", SIM, r"^GEAR_DROP = ([\d.]+)", 7.0),
    ("sim: дроп самоцветов", SIM, r"^GEM_DROP = ([\d.]+)", 1.5),
    ("sim: дроп рун", SIM, r"^RUNE_DROP = ([\d.]+)", 0.15),
    ("sim: дроп ингредиентов", SIM, r"^INGREDIENT_DROP = ([\d.]+)", 5.0),
    ("sim: дроп валюты", SIM, r"^CURRENCY_DROP = ([\d.]+)", 0.2),
    ("sim: пул уникатов", SIM, r"^UNIQUE_POOL = (\d+)", 18),
    ("sim: число игроков", SIM, r"^PLAYERS = (\d+)", 6),
]

STR_CHECKS = [
    ("сокет за каждые +2 апгрейда", GEAR_DATA,
     r"(Math\.min\(2, this\.up\.getUpgradeLevel\(\) / 2\))",
     "Math.min(2, this.up.getUpgradeLevel() / 2)"),
    ("камень редкости требует редкость предмета", RARITY_STONE,
     r"(gear\.getRarity\(\)[^;]*?GUID\(\)\s*\.equals\(rar\.GUID\(\)\))",
     None),  # проверяется как «шаблон присутствует»
]

# Паттерны, которых в коде быть НЕ должно (старые недостижимые пороги).
ABSENT = [
    ("старый порог сокета +8/+12", GEAR_DATA, r"uplvl >= (8|12)"),
    ("бросок кубика в камне редкости", RARITY_STONE, r"RandomUtils\.roll"),
    ("камень редкости без сброса апгрейдов", GEAR_DATA, r"this\.up\.regenerate\(rar\)"),
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
]


def read(path):
    try:
        return path.read_text(encoding="utf-8")
    except FileNotFoundError:
        return None


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

    width = max(len(r[0]) for r in rows) + 2
    print(f"{'проверка'.ljust(width)}{'в коде':>28}{'ожидалось':>16}{'итог':>8}")
    print("-" * (width + 52))
    for name, actual, expected, status in rows:
        shown = str(actual)
        if len(shown) > 26:
            shown = "…" + shown[-25:]
        print(f"{name.ljust(width)}{shown:>28}{str(expected)[:16]:>16}{status:>8}")

    print()
    if failed:
        print(f"РАСХОЖДЕНИЙ: {failed}. Отчёт LAUNCH-READINESS.md больше не соответствует коду.")
        return 1

    print(f"Все {len(rows)} инвариантов совпадают с LAUNCH-READINESS.md.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
