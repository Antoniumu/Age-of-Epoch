#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Проверка древа «Божественный» целиком: сетка, перки, статы, иконки, локализация.

Что проверяется:
  1. каждый id перка из divine.json объявлен в DivinePerks.java (и наоборот);
  2. каждый стат, на который ссылается DivinePerks.java, существует (SummonStat / DivineStats);
  3. иконка каждого перка реально лежит в assets;
  4. для каждого перка и стата есть ключи mmorpg.talent.* / mmorpg.stat.* (+ stat_desc) в ru и en;
  5. эффекты raise_dead / explode_corpse зарегистрированы в StatEffect и StatEffects;
  6. ветки разведены: у узлов слева exclusive_branch = necromancer, справа = summoner,
     в центре ветки нет (иначе взаимоисключение не сработает).

Запуск: python3 tools/talent_gen/verify_divine.py
"""

import json
import os
import re
import sys

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
JAVA = os.path.join(REPO, "src", "main", "java", "com", "robertx22", "age_of_exile")
RES = os.path.join(REPO, "src", "main", "resources")

TREE_JSON = os.path.join(RES, "data", "mmorpg", "talent_tree", "divine.json")
DIVINE_PERKS = os.path.join(JAVA, "aoe_data", "database", "perks", "DivinePerks.java")
SUMMON_STAT = os.path.join(JAVA, "database", "data", "stats", "types", "summoning", "SummonStat.java")
DIVINE_STATS = os.path.join(JAVA, "aoe_data", "database", "stats", "DivineStats.java")
STAT_EFFECT = os.path.join(JAVA, "uncommon", "effectdatas", "rework", "action", "StatEffect.java")
STAT_EFFECTS = os.path.join(JAVA, "aoe_data", "database", "stat_effects", "StatEffects.java")
ALL_PERKS = os.path.join(JAVA, "aoe_data", "database", "perks", "AllPerks.java")
GENERATED = os.path.join(JAVA, "aoe_data", "GeneratedData.java")
STATS_REGISTER = os.path.join(JAVA, "database", "registrators", "StatsRegister.java")
COMMON_EVENTS = os.path.join(JAVA, "mmorpg", "event_registers", "CommonEvents.java")

errors = []
notes = []


def read(path):
    with open(path, encoding="utf-8") as f:
        return f.read()


def fail(msg):
    errors.append(msg)


# ------------------------------------------------------------------ 1. сетка и перки

grid = json.load(open(TREE_JSON, encoding="utf-8"))
rows = grid["perks"].split("\n")

perks_in_grid = {}
centers = 0
for y, row in enumerate(rows):
    cells = row.split(",")
    if len(cells) != len(rows[0].split(",")):
        fail("Ряд %d сетки имеет другую ширину" % y)
    for x, token in enumerate(cells):
        low = token.lower()
        if low == "[center]":
            centers += 1
        elif len(low) > 2:
            perks_in_grid[token] = (x, y)

if centers != 1:
    fail("В древе должен быть ровно один [CENTER], найдено %d" % centers)

perks_src = read(DIVINE_PERKS)
perks_in_code = re.findall(r'perk\("([a-z0-9_]+)"', perks_src)

for perk_id in perks_in_grid:
    if perk_id not in perks_in_code:
        fail("Перк %s есть в древе, но не объявлен в DivinePerks.java" % perk_id)
for perk_id in perks_in_code:
    if perk_id not in perks_in_grid:
        fail("Перк %s объявлен в DivinePerks.java, но отсутствует в древе" % perk_id)

notes.append("перков: %d (в коде и в древе совпадают)" % len(perks_in_grid))

# ------------------------------------------------------------------ 2. статы

summon_src = read(SUMMON_STAT)
divine_stats_src = read(DIVINE_STATS)

summon_stat_ids = re.findall(r'of\("([a-z0-9_]+)"', summon_src)
datapack_stat_ids = re.findall(r'ofSingle\("([a-z0-9_]+)"', divine_stats_src)
all_stat_ids = set(summon_stat_ids) | set(datapack_stat_ids)

# SummonStat.ALL должен перечислять все старые статы
all_list = summon_src.split("public static final List<SummonStat> ALL")[1].split("));")[0]
for stat_id in summon_stat_ids:
    name = stat_id.upper()
    if name not in all_list:
        fail("Стат %s не добавлен в SummonStat.ALL (не зарегистрируется)" % stat_id)

used_stats = set(re.findall(r'SummonStat\.([A-Z0-9_]+)', perks_src))
for name in used_stats:
    if name not in summon_src:
        fail("DivinePerks ссылается на SummonStat.%s, которого нет" % name)

used_datapack = set(re.findall(r'DivineStats\.([A-Z0-9_]+)\.get\(\)', perks_src))
for name in used_datapack:
    if name + " =" not in divine_stats_src and name + "=" not in divine_stats_src:
        fail("DivinePerks ссылается на DivineStats.%s, которого нет" % name)

notes.append("статов призыва: %d (старых) + %d (датапак)" % (len(summon_stat_ids), len(datapack_stat_ids)))

# ------------------------------------------------------------------ 3. иконки

icons = re.findall(r'perk\("[a-z0-9_]+",\s*"[^"]+",\s*Perk\.PerkType\.[A-Z_]+,\s*"([^"]+)"', perks_src)
if len(icons) != len(perks_in_code):
    fail("Не удалось разобрать иконки у всех перков (%d из %d)" % (len(icons), len(perks_in_code)))

for icon in icons:
    path = os.path.join(RES, "assets", "mmorpg", icon.replace("/", os.sep))
    if not os.path.exists(path):
        fail("Иконка не найдена: %s" % icon)

namespace, tree_icon = grid["icon"].split(":", 1)
if not os.path.exists(os.path.join(RES, "assets", namespace, tree_icon.replace("/", os.sep))):
    fail("Иконка вкладки древа не найдена: %s" % tree_icon)

notes.append("иконок проверено: %d + иконка вкладки" % len(icons))

# ------------------------------------------------------------------ 4. локализация

lang = {}
for code in ("ru_ru", "en_us"):
    lang[code] = json.load(open(os.path.join(RES, "assets", "mmorpg", "lang", code + ".json"), encoding="utf-8"))

for perk_id in perks_in_grid:
    for code in lang:
        if "mmorpg.talent." + perk_id not in lang[code]:
            fail("Нет ключа mmorpg.talent.%s в %s" % (perk_id, code))

for stat_id in sorted(all_stat_ids):
    for code in lang:
        if "mmorpg.stat." + stat_id not in lang[code]:
            fail("Нет ключа mmorpg.stat.%s в %s" % (stat_id, code))
        if "mmorpg.stat_desc." + stat_id not in lang[code]:
            fail("Нет ключа mmorpg.stat_desc.%s в %s" % (stat_id, code))

for key in ("mmorpg.branch.divine.necromancer", "mmorpg.branch.divine.summoner", "mmorpg.branch.divine", "mmorpg.one_of_a_kind.divine_beast"):
    for code in lang:
        if key not in lang[code]:
            fail("Нет ключа %s в %s" % (key, code))

notes.append("ключей локализации: %d на язык" % (len(perks_in_grid) + 2 * len(all_stat_ids) + 3))

# ------------------------------------------------------------------ 5. эффекты и регистрация

stat_effect_src = read(STAT_EFFECT)
for effect in ("RaiseDeadAction", "ExplodeCorpseAction"):
    if "addSer(new %s())" % effect not in stat_effect_src:
        fail("%s не зарегистрирован в StatEffect.SERIALIZERS" % effect)

stat_effects_src = read(STAT_EFFECTS)
for effect in ("RAISE_DEAD", "EXPLODE_CORPSE"):
    if effect + " = new " not in stat_effects_src:
        fail("%s не объявлен в StatEffects" % effect)
    if effect + ".addToSerializables()" not in stat_effects_src:
        fail("%s не добавляется в сериализацию в StatEffects.registerAll()" % effect)

if "new DivinePerks().registerAll()" not in read(ALL_PERKS):
    fail("DivinePerks не зарегистрирован в AllPerks")
if "new DivineStats().registerAll()" not in read(GENERATED):
    fail("DivineStats не зарегистрирован в GeneratedData")
if "addAll(SummonStat.ALL)" not in read(STATS_REGISTER):
    fail("SummonStat.ALL не зарегистрирован в StatsRegister")
if "new MinionTick()" not in read(COMMON_EVENTS):
    fail("MinionTick не зарегистрирован в CommonEvents")

# события у датапак-статов
if "OnMobKilledByDamageEvent.ID" not in divine_stats_src:
    fail("Датапак-статы некроманта не привязаны к событию убийства")

notes.append("эффекты: raise_dead, explode_corpse; тик прислужников: MinionTick")

# ------------------------------------------------------------------ 6. разведение веток

width = len(rows[0].split(","))
# центр страницы — общие перки; всё, что заметно левее/правее, относится к веткам
left_border = width // 2 - 10
right_border = width // 2 + 10

branch_of = {}

matches = list(re.finditer(r'perk\("([a-z0-9_]+)"', perks_src))
method_end = perks_src.index("\n    }\n", matches[0].start())  # конец registerAll()

for i, m in enumerate(matches):
    perk_id = m.group(1)

    start = m.start()
    end = matches[i + 1].start() if i + 1 < len(matches) else method_end
    end = min(end, method_end)

    before = perks_src[max(0, start - 40):start]      # обёртка: branch( / beast( / level(
    after = perks_src[start:end]                      # хвост: lvl_req, is_entry, exclusive_branch

    chunk = before + after

    if "BRANCH_NECROMANCER" in chunk:
        branch_of[perk_id] = "necromancer"
    elif "BRANCH_SUMMONER" in chunk or "beast(" in before:
        branch_of[perk_id] = "summoner"
    else:
        branch_of[perk_id] = ""

for perk_id in perks_in_grid:
    if perk_id not in branch_of:
        fail("Не удалось определить ветку перка %s" % perk_id)
        branch_of[perk_id] = "?"

for perk_id, branch in branch_of.items():
    x = perks_in_grid[perk_id][0]
    zone = "слева" if x < left_border else ("справа" if x > right_border else "центр")

    if zone == "слева" and branch != "necromancer":
        fail("%s стоит слева, но ветка «%s» (должна быть necromancer)" % (perk_id, branch or "-"))
    if zone == "справа" and branch != "summoner":
        fail("%s стоит справа, но ветка «%s» (должна быть summoner)" % (perk_id, branch or "-"))
    if zone == "центр" and branch != "":
        fail("%s в центре, но помечен веткой «%s» — общие перки должны быть доступны обоим" % (perk_id, branch))

zones = {"слева": 0, "центр": 0, "справа": 0}
for perk_id in perks_in_grid:
    x = perks_in_grid[perk_id][0]
    zones["слева" if x < left_border else ("справа" if x > right_border else "центр")] += 1

notes.append("раскладка: некромант %d / общие %d / призыватель %d"
             % (zones["слева"], zones["центр"], zones["справа"]))

# ------------------------------------------------------------------ итог

print("Проверка древа «Божественный»")
for n in notes:
    print("  • " + n)

if errors:
    print("\nОШИБКИ (%d):" % len(errors))
    for e in errors:
        print("  ✗ " + e)
    sys.exit(1)

print("\nВсё в порядке.")
