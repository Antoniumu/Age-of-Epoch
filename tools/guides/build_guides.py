#!/usr/bin/env python3
"""
Планировщик талантов для гайдов Age of Epoch.

Точное зеркало серверной логики:
  * TalentGrid.loadIntoTree()  — как клетки talents.json превращаются в узлы и связи
  * TalentsData.canAllocate()  — новый узел обязан быть соединён с уже взятым
                                 (исключение: стартовые перки с is_entry = true)
  * GameBalanceConfig          — сколько очков талантов даёт уровень

Что делает скрипт:
  1. строит граф древа и проверяет его;
  2. парсит статы перков из aoe_data/database/perks/*.java;
  3. считает ценность перка для архетипа в «очках характеристик на 50 уровне»;
  4. строит валидный порядок прокачки (жадный поиск лучшего узла в радиусе + путь к нему);
  5. печатает готовые строки для GuideData.java.

Запуск: python3 tools/guides/build_guides.py
"""
import json
import os
import re
import sys
from collections import defaultdict, deque

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
PERK_DIR = os.path.join(ROOT, "src/main/java/com/robertx22/age_of_exile/aoe_data/database/perks")
TREE = os.path.join(ROOT, "src/main/resources/data/mmorpg/talent_tree/talents.json")

sys.path.insert(0, HERE)
import aoe_math as M  # noqa: E402

CENTER = "[CENTER]"
START_PERK_FILE = os.path.join(ROOT, "src/main/java/com/robertx22/age_of_exile/aoe_data/database/perks/StartPerks.java")


def entry_perk_ids():
    """id стартовых перков (Perk.is_entry = true, one_of_a_kind = "start").

    Взять можно ровно один стартовый путь: как только вложено очко в любой из них,
    остальные блокируются через isLockedToPlayer. Поэтому в план может входить
    только стартовый перк самого гайда.
    """
    ids = set()
    try:
        text = open(START_PERK_FILE, encoding="utf-8").read()
        ids.update(re.findall(r'of\("([a-z0-9_]+)"', text))
    except OSError:
        pass
    ids.update({"start_int", "start_str", "start_dex",
                "start_strint", "start_strdex", "start_intdex"})
    return ids


ENTRY_PERKS = entry_perk_ids()
SEARCH_RADIUS = 3
PATH_PENALTY = 0.8
PLAN_POINTS = 101


# --------------------------------------------------------------------------- граф
class Grid:
    def __init__(self, text):
        self.rows = [line.split(",") for line in text.split("\n")]
        self.height = len(self.rows)
        self.width = max(len(r) for r in self.rows)

    def get(self, x, y):
        if x < 0 or y < 0 or y >= self.height or x >= len(self.rows[y]):
            return None
        return self.rows[y][x].strip().lower()


def is_connector(cell):
    return cell is not None and len(cell) == 1


def is_talent(cell):
    # GridPoint: len == 1 -> connector, len == 2 -> мусор, len > 2 -> талант
    return cell is not None and len(cell) > 2 and cell != CENTER


def build_connections(grid):
    talents = []
    for y in range(grid.height):
        for x in range(grid.width):
            c = grid.get(x, y)
            if is_talent(c):
                talents.append((x, y, c))

    def surrounding(p):
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                if dx == 0 and dy == 0:
                    continue
                cell = grid.get(p[0] + dx, p[1] + dy)
                if cell is not None:
                    yield cell, (p[0] + dx, p[1] + dy)

    def connector_types(p):
        return {c for c, _ in surrounding(p) if is_connector(c)}

    def eligible(p, connector):
        out = []
        x, y = p
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                if dx == 0 and dy == 0:
                    continue
                nx, ny = x + dx, y + dy
                cell = grid.get(nx, ny)
                if cell is None:
                    continue
                if abs(dx) == 1 and abs(dy) == 1:
                    if is_talent(grid.get(x + dx, y)) or is_talent(grid.get(x, y + dy)):
                        continue
                if is_talent(cell) or (is_connector(cell) and cell == connector):
                    out.append((nx, ny))
        return out

    def has_path(start, end, connectors):
        for connector in connectors:
            open_set = deque([start])
            closed = set()
            while open_set:
                cur = open_set.popleft()
                if cur in closed:
                    continue
                closed.add(cur)
                if cur == end:
                    return True
                if is_talent(grid.get(*cur)) and cur != start:
                    continue
                open_set.extend(eligible(cur, connector))
        return False

    conn = defaultdict(set)
    for (x1, y1, _) in talents:
        ctypes = connector_types((x1, y1))
        if not ctypes:
            continue
        for (x2, y2, _) in talents:
            if (x1, y1) == (x2, y2) or (x2, y2) in conn[(x1, y1)]:
                continue
            if abs(x1 - x2) >= 10 or abs(y1 - y2) >= 10:
                continue
            if has_path((x1, y1), (x2, y2), ctypes):
                conn[(x1, y1)].add((x2, y2))
                conn[(x2, y2)].add((x1, y1))
    return talents, conn


# --------------------------------------------------------------------------- перки
STAT_RE = re.compile(
    r"new OptScaleExactStat\(\s*(-?[\d.]+)F?\s*,\s*(.+?)\s*,\s*ModType\.(\w+)\s*\)(\.scale\(\))?")


def classify(expr):
    """Java-выражение стата -> категория, по которой гайд считает ценность."""
    e = expr.replace(" ", "")
    simple = {
        "DatapackStats.INT": "core_int", "DatapackStats.STR": "core_str",
        "DatapackStats.DEX": "core_dex", "DatapackStats.VIT": "core_vit",
        "DatapackStats.WIS": "core_wis", "DatapackStats.AGI": "core_agi",
        "Health.getInstance()": "health", "HealthRegen.getInstance()": "health_regen",
        "Mana.getInstance()": "mana", "ManaRegen.getInstance()": "mana_regen",
        "Energy.getInstance()": "energy", "EnergyRegen.getInstance()": "energy_regen",
        "Armor.getInstance()": "armor", "DodgeRating.getInstance()": "dodge",
        "SpellPower.getInstance()": "spell_power", "SpellDamage.getInstance()": "spell_dmg_all",
        "DamageShield.getInstance()": "magic_shield",
        "DamageAbsorbedByMana.getInstance()": "mana_shield",
        "ArmorPenetration.getInstance()": "pen_physical",
        "SpellDodge.getInstance()": "spell_dodge",
        "AllAttributes.getInstance()": "all_attrs",
        "Blood.getInstance()": "blood",
    }
    if e in simple:
        return simple[e]

    m = re.match(r"new ElementalResist\(Elements\.(\w+)\)", e)
    if m:
        return "resist_" + m.group(1).lower()
    m = re.match(r"new ElementalPenetration\(Elements\.(\w+)\)", e)
    if m:
        return "pen_" + m.group(1).lower()
    m = re.match(r"new AttackDamage\(Elements\.(\w+)\)", e)
    if m:
        return "phys_dmg"

    m = re.match(r"Stats\.(\w+)\.get\((.*)\)$", e)
    if m:
        name, arg = m.group(1), m.group(2).lower()
        for pref in ("Elements.", "WeaponTypes.", "NegativeEffects.", "SpellTag.",
                     "PlayStyle.", "ResourceType."):
            arg = arg.replace(pref.lower(), "")
        arg = arg.replace('"', "")
        table = {
            "ELEMENTAL_ANY_WEAPON_DAMAGE": lambda a: "atk_pct_" + a,
            "ELEMENTAL_WEAPON_DAMAGE": lambda a: "wep_pct_" + a,
            "ELEMENTAL_SPELL_DAMAGE": lambda a: "spell_dmg_" + a,
            "WEAPON_DAMAGE": lambda a: "wep_pct_" + a,
            "ELEMENTAL_DAMAGE": lambda a: "ele_pct_" + a,
            "ELE_DOT_DAMAGE": lambda a: "dot_ele_" + a,
            "CHANCE_OF_APPLYING_EFFECT": lambda a: "chance_" + a,
            "CRIT_CHANCE": lambda a: "crit",
            "CRIT_DAMAGE": lambda a: "crit_dmg",
            "SPELL_CRIT_CHANCE": lambda a: "spell_crit",
            "SPELL_CRIT_DAMAGE": lambda a: "spell_crit_dmg",
            "ATTACK_SPEED": lambda a: "attack_speed",
            "CAST_SPEED": lambda a: "cast_speed",
            "COOLDOWN_REDUCTION": lambda a: "cdr",
            "AREA_DAMAGE": lambda a: "area",
            "PROJECTILE_DAMAGE": lambda a: "projectile",
            "DOT_DAMAGE": lambda a: "dot",
            "LIFESTEAL": lambda a: "lifesteal",
            "SPELL_LIFESTEAL": lambda a: "spell_lifesteal",
            "HEAL_STRENGTH": lambda a: "heal",
            "ACCURACY": lambda a: "accuracy",
            "DAMAGE_RECEIVED": lambda a: "dmg_received",
            "MANA_COST": lambda a: "mana_cost",
            "ALL_ATTRIBUTES": lambda a: "all_attrs",
        }
        if name in table:
            return table[name](arg)
        return "misc"
    return "misc"


def parse_perks():
    perks = {}
    for name in sorted(os.listdir(PERK_DIR)):
        if not name.endswith(".java"):
            continue
        text = open(os.path.join(PERK_DIR, name), encoding="utf-8").read()
        for line in text.split("\n"):
            m = re.search(r"(?:register|bigStat|stat|gameChanger|of)\(\s*\"([a-z0-9_]+)\"", line)
            if not m or m.group(1) in perks:
                continue
            stats = []
            for sm in STAT_RE.finditer(line):
                stats.append({
                    "v": float(sm.group(1)),
                    "cat": classify(sm.group(2)),
                    "mod": sm.group(3),
                    "scaled": bool(sm.group(4)),
                })
            if stats:
                perks[m.group(1)] = stats
    return perks


# --------------------------------------------------------------------------- веса
# Вес = ценность одной «эффективной» единицы стата в очках характеристик на 50 ур.
# FLAT-статы с .scale() в игре умножаются на NORMAL(50) = 10.8 — здесь это уже учтено
# в константах: напр. 1 флат-урон = 10.8 урона = 10 очков СИЛ.
NORM = M.normal(50)  # 10.8

# Общая шкала (одинаковая для всех гайдов)
VALUE = {
    "health": 0.5, "health_regen": 0.9,
    "mana": 0.5, "mana_regen": 0.9,
    "energy": 0.4, "energy_regen": 0.6,
    "armor": 1.0, "dodge": 0.9, "spell_dodge": 0.4,
    "resist_fire": 0.33, "resist_water": 0.33, "resist_earth": 0.33,
    "resist_physical": 0.33, "resist_elemental": 0.33,
    "magic_shield": 0.8, "mana_shield": 0.5,
    "spell_power": 1.0, "phys_dmg": 10.0,
    "pen_physical": 0.8, "pen_fire": 0.5, "pen_water": 0.5,
    "pen_earth": 0.5, "pen_elemental": 0.5,
    "crit": 0.2, "crit_dmg": 0.15,
    "spell_crit": 0.25, "spell_crit_dmg": 0.18,
    "attack_speed": 0.3, "cast_speed": 0.35, "cdr": 0.35,
    "area": 0.35, "projectile": 0.35, "dot": 0.2,
    "lifesteal": 0.2, "spell_lifesteal": 0.25, "heal": 0.1,
    "accuracy": 0.2, "dmg_received": -1.0, "mana_cost": 0.3,
    "all_attrs": 0.85, "blood": 0.2,
    "chance_burn": 0.1, "chance_poison": 0.1, "chance_chill": 0.1,
    "chance_bleed": 0.1, "chance_torment": 0.1,
    "dot_ele_fire": 0.2, "dot_ele_water": 0.2, "dot_ele_earth": 0.25,
    "atk_pct_physical": 0.7, "atk_pct_elemental": 0.55, "atk_pct_fire": 0.4,
    "atk_pct_water": 0.4, "atk_pct_earth": 0.45,
    "ele_pct_physical": 0.65, "ele_pct_elemental": 0.5, "ele_pct_fire": 0.35,
    "ele_pct_water": 0.35, "ele_pct_earth": 0.45,
    "wep_pct_sword": 0.3, "wep_pct_axe": 0.3, "wep_pct_bow": 0.3,
    "wep_pct_crossbow": 0.3, "wep_pct_staff": 0.2, "wep_pct_scepter": 0.2,
    "wep_pct_none": 0.1,
    "spell_dmg_all": 0.67, "spell_dmg_water": 0.8, "spell_dmg_fire": 0.6,
    "spell_dmg_earth": 0.6, "spell_dmg_elemental": 0.6,
    "health_pct": 0.2, "mana_pct": 0.2, "armor_pct": 0.45,
    "dodge_pct": 0.2, "resist_pct": 0.55, "magic_shield_pct": 0.5,
    "misc": 0.0,
}

# Всё, что не перечислено явно, считается мусором для архетипа.
# Иначе «нейтральные» категории (крит, скорость атаки, элементальный урон)
# проходят с весом 1.0 и план разбавляется чужими кластерами.
DEFAULT_AFFINITY = 0.05

# Множитель «нужен ли этот стат именно этой сборке»
AFFINITY = {
    "warrior": {
        "core_str": 1.0, "core_vit": 0.9, "core_dex": 0.6,
        "core_int": 0.05, "core_wis": 0.05, "core_agi": 0.15, "all_attrs": 0.7,
        "wep_pct_sword": 2.2, "wep_pct_axe": 2.2, "wep_pct_bow": 0.4,
        "wep_pct_crossbow": 0.4, "wep_pct_staff": 0.3, "wep_pct_scepter": 0.4,
        "spell_dmg_all": 0.0, "spell_dmg_water": 0.0, "spell_dmg_fire": 0.0,
        "spell_dmg_earth": 0.0, "spell_dmg_elemental": 0.0, "spell_power": 0.0,
        "spell_crit": 0.0, "spell_crit_dmg": 0.0, "spell_lifesteal": 0.1,
        "cast_speed": 0.1, "mana": 0.1, "mana_regen": 0.15,
        "magic_shield": 0.25, "mana_shield": 0.1, "pen_water": 0.3,
        "pen_fire": 0.3, "pen_earth": 0.3, "chance_burn": 0.0,
        "chance_poison": 0.0, "chance_chill": 0.0, "dot_ele_fire": 0.0,
        "dot_ele_water": 0.0, "dot_ele_earth": 0.1, "spell_dodge": 0.4,
        "dot": 0.3, "projectile": 0.1, "lifesteal": 1.5,
        "atk_pct_physical": 1.25, "ele_pct_physical": 1.2,
        "phys_dmg": 1.0, "armor": 1.25, "dodge": 0.5, "health": 1.15,
        # меле-архетип: свои категории задаём явно, всё прочее — мусор
        "pen_physical": 1.0, "crit": 0.8, "crit_dmg": 0.8, "attack_speed": 1.1,
        "area": 0.9, "heal": 1.0, "health_regen": 1.0, "cdr": 0.4,
        "chance_bleed": 0.8, "chance_torment": 0.0,
        "atk_pct_elemental": 0.15, "atk_pct_fire": 0.0, "atk_pct_water": 0.0,
        "atk_pct_earth": 0.15, "ele_pct_fire": 0.0, "ele_pct_water": 0.0,
        "ele_pct_earth": 0.1, "ele_pct_elemental": 0.1,
        "dot_ele_fire": 0.0, "dot_ele_water": 0.0, "dot_ele_earth": 0.2,
        "mana_shield": 0.05, "accuracy": 0.2,
    },
    "mage": {
        "core_int": 1.0, "core_wis": 0.6, "core_vit": 0.7,
        "core_str": 0.0, "core_dex": 0.1, "core_agi": 0.1, "all_attrs": 0.6,
        "spell_dmg_water": 1.2, "spell_dmg_elemental": 0.9,
        "spell_dmg_all": 1.0, "spell_dmg_fire": 0.3, "spell_dmg_earth": 0.3,
        "pen_water": 1.3, "pen_elemental": 0.8, "pen_physical": 0.0,
        "phys_dmg": 0.0, "atk_pct_physical": 0.0, "ele_pct_physical": 0.0,
        "wep_pct_sword": 0.0, "wep_pct_axe": 0.0, "wep_pct_bow": 0.0,
        "wep_pct_crossbow": 0.0, "wep_pct_staff": 1.2, "wep_pct_scepter": 1.0,
        "crit": 0.05, "crit_dmg": 0.05, "attack_speed": 0.0,
        "cast_speed": 1.2, "cdr": 1.2, "spell_crit": 1.3, "spell_crit_dmg": 1.0,
        "magic_shield": 1.4, "mana_shield": 1.2, "mana": 1.0, "mana_regen": 1.6,
        "health": 0.7, "health_regen": 0.6, "armor": 0.3, "dodge": 0.3,
        "resist_fire": 1.2, "resist_water": 1.0, "resist_earth": 1.0,
        "resist_elemental": 1.0, "area": 1.2, "projectile": 0.8,
        "dot": 0.6, "dot_ele_water": 1.0, "chance_chill": 1.2,
        "heal": 0.4, "spell_lifesteal": 1.2, "lifesteal": 0.1,
        "mana_cost": 0.8, "blood": 0.5,
        # маг не бьёт оружием и не стакает физ-урон
        "atk_pct_physical": 0.0, "atk_pct_elemental": 0.0, "atk_pct_fire": 0.0,
        "atk_pct_water": 0.0, "atk_pct_earth": 0.0,
        "ele_pct_physical": 0.0, "ele_pct_fire": 0.1, "ele_pct_water": 0.6,
        "ele_pct_earth": 0.25, "ele_pct_elemental": 0.4,
        "attack_speed": 0.0, "accuracy": 0.0, "crit": 0.05, "crit_dmg": 0.05,
        "health_regen": 0.6, "area": 1.2, "cdr": 1.2,
        "chance_burn": 0.0, "chance_poison": 0.05, "chance_bleed": 0.05,
        "chance_torment": 0.8, "chance_chill": 1.2,
        "dot_ele_fire": 0.1, "dot_ele_water": 1.0, "dot_ele_earth": 0.4,
        "mana_shield": 1.2, "resist_physical": 0.4,
    },
    "hunter": {
        "core_dex": 1.0, "core_vit": 0.8, "core_str": 0.35,
        "core_agi": 0.2, "core_int": 0.05, "core_wis": 0.1, "all_attrs": 0.6,
        "wep_pct_bow": 2.2, "wep_pct_crossbow": 1.9, "wep_pct_sword": 0.2,
        "wep_pct_axe": 0.2, "wep_pct_staff": 0.0, "wep_pct_scepter": 0.0,
        "spell_power": 0.05, "spell_dmg_all": 0.05,
        "spell_dmg_earth": 0.4, "spell_dmg_water": 0.1, "spell_dmg_fire": 0.1,
        "spell_dmg_elemental": 0.15, "spell_crit": 0.2, "spell_crit_dmg": 0.2,
        "crit": 1.4, "crit_dmg": 1.3, "attack_speed": 1.3, "cdr": 0.7,
        "projectile": 1.3, "area": 0.7, "dot": 0.9, "dot_ele_earth": 1.3,
        "chance_poison": 1.1, "chance_bleed": 1.0, "dot_ele_fire": 0.2,
        "dot_ele_water": 0.2, "dodge": 1.4, "dodge_pct": 1.2,
        "health": 0.9, "health_regen": 0.8, "armor": 0.5, "armor_pct": 0.5,
        "resist_elemental": 1.0, "phys_dmg": 1.0, "atk_pct_physical": 1.2,
        "ele_pct_physical": 1.0, "ele_pct_earth": 1.3, "ele_pct_elemental": 0.6,
        "pen_physical": 1.2, "pen_earth": 1.1, "pen_elemental": 0.4,
        "cast_speed": 0.15, "mana": 0.2, "mana_regen": 0.3,
        "magic_shield": 0.1, "lifesteal": 1.2, "heal": 0.2,
        "attack_speed": 1.3, "accuracy": 1.0, "energy": 0.5, "energy_regen": 0.5,
        # лучник: элементальные стрелы и доты — профиль, магия и щиты — нет
        "atk_pct_elemental": 1.1, "atk_pct_fire": 0.2, "atk_pct_water": 0.2,
        "atk_pct_earth": 1.0, "ele_pct_fire": 0.2, "ele_pct_water": 0.2,
        "chance_burn": 0.2, "chance_chill": 0.2, "chance_torment": 0.4,
        "crit": 1.4, "crit_dmg": 1.3, "attack_speed": 1.3, "cdr": 0.7,
        "area": 0.7, "projectile": 1.3, "dot": 0.9,
        "magic_shield": 0.1, "mana_shield": 0.05, "cast_speed": 0.15,
    },
}

GUIDES = {
    "warrior": {
        "name": "Стальной страж",
        "start": "start_str",
        "split": [("strength", 0.45), ("vitality", 0.35), ("dexterity", 0.20)],
        "school": "divine",
    },
    "mage": {
        "name": "Ледяной маг",
        "start": "start_int",
        "split": [("intelligence", 0.55), ("wisdom", 0.25), ("vitality", 0.20)],
        "school": "water",
    },
    "hunter": {
        "name": "Охотник-отравитель",
        "start": "start_dex",
        "split": [("dexterity", 0.55), ("vitality", 0.30), ("strength", 0.15)],
        "school": "hunting",
    },
}


def stat_value(stat, profile):
    base = VALUE.get(stat["cat"], 0.0)
    if base <= 0:
        return 0.0
    aff = AFFINITY[profile].get(stat["cat"], DEFAULT_AFFINITY)
    if aff <= 0:
        return 0.0
    v = stat["v"]
    if stat["mod"] == "FLAT" and stat["scaled"]:
        v *= NORM
    return v * base * aff


def perk_score(stats, profile):
    return sum(stat_value(s, profile) for s in stats)


# --------------------------------------------------------------------------- план
TALENTS = {}


def node_id(p):
    return TALENTS.get(p, "?")


def make_plan(start, profile, perks, conn, points=PLAN_POINTS):
    plan = [start]
    taken = {start}
    while len(plan) < points:
        prev = {p: None for p in taken}
        depth = {p: 0 for p in taken}
        q = deque(taken)
        candidates = []
        while q:
            cur = q.popleft()
            if depth[cur] >= SEARCH_RADIUS:
                continue
            for nxt in conn.get(cur, ()):
                if nxt in depth:
                    continue
                depth[nxt] = depth[cur] + 1
                prev[nxt] = cur
                q.append(nxt)
                if nxt not in taken:
                    candidates.append(nxt)
        if not candidates:
            break

        best, best_val = None, float("-inf")
        for cand in candidates:
            if node_id(cand) in ENTRY_PERKS:
                continue  # второй стартовый путь взять нельзя — заблокирован игрой
            score = perk_score(perks.get(node_id(cand), []), profile)
            d = depth[cand]
            val = (score - PATH_PENALTY * (d - 1)) / d
            if val > best_val:
                best, best_val = cand, val
        if best is None:
            break

        path = []
        node = best
        while node is not None and node not in taken:
            path.append(node)
            node = prev[node]
        path.reverse()
        if not path:
            break
        for p in path:
            if p in taken:
                continue
            taken.add(p)
            plan.append(p)
            if len(plan) >= points:
                break
    return plan


def validate_plan(plan, conn, start_id):
    """Проверяет, что план реально можно взять по правилам TalentsData.canAllocate."""
    if node_id(plan[0]) != start_id:
        raise SystemExit(f"план начинается не со стартового перка: {node_id(plan[0])}")
    taken = {plan[0]}
    for i, point in enumerate(plan[1:], start=1):
        pid = node_id(point)
        if pid in ENTRY_PERKS:
            raise SystemExit(f"в плане второй стартовый перк {pid} (индекс {i})")
        neighbours = conn.get(point, set())
        if not (neighbours & taken):
            raise SystemExit(f"узел {pid} ({point[0]}:{point[1]}) не связан с предыдущими")
        taken.add(point)
    if len(set(plan)) != len(plan):
        raise SystemExit("в плане есть дубликаты узлов")


def main():
    grid = Grid(json.load(open(TREE, encoding="utf-8"))["perks"])
    talents, conn = build_connections(grid)
    for x, y, pid in talents:
        TALENTS[(x, y)] = pid
    perks = parse_perks()

    print(f"Узлов: {len(talents)}, связей: {sum(len(v) for v in conn.values()) // 2}, "
          f"перков со статами: {len(perks)}")

    result = {}
    for profile, guide in GUIDES.items():
        coords = [(x, y) for x, y, pid in talents if pid == guide["start"]]
        if not coords:
            raise SystemExit("нет стартового перка " + guide["start"])
        start = coords[0]
        plan = make_plan(start, profile, perks, conn)
        total = sum(perk_score(perks.get(node_id(p), []), profile) for p in plan)
        dead = sum(1 for p in plan if perk_score(perks.get(node_id(p), []), profile) <= 0.01)
        print(f"\n{guide['name']}: {len(plan)} узлов, ценность {total:.1f}, "
              f"узлов-связок {dead}, {total / max(1, len(plan)):.2f} очка/узел")
        print("  " + ";".join(f"{x}:{y}:{node_id((x, y))}" for x, y in plan))
        validate_plan(plan, conn, guide["start"])
        result[profile] = {
            "name": guide["name"],
            "school": guide["school"],
            "split": guide["split"],
            "plan": ";".join(f"{x}:{y}:{node_id((x, y))}" for x, y in plan),
            "steps": [{"x": x, "y": y, "perk": node_id((x, y))} for x, y in plan],
            "score": round(total, 1),
        }

    with open(os.path.join(HERE, "plans.json"), "w", encoding="utf-8") as f:
        json.dump(result, f, ensure_ascii=False, indent=1)
    print("\nсохранено tools/guides/plans.json")


if __name__ == "__main__":
    main()
