#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Генератор древа талантов «Божественный» (класс призыва).

Страница поделена на три части, как и задумано в дизайне:
  * слева   — ветка НЕКРОМАНТА (призыв убитых врагов),
  * в центре — ОБЩИЕ перки для обоих билдов,
  * справа  — ветка ПРИЗЫВАТЕЛЯ (стая волков / медведь / боевой мамонт).

Скрипт:
  1. берёт список id перков прямо из DivinePerks.java (чтобы древо не могло разойтись с кодом);
  2. раскладывает перки по сетке и прокладывает между связанными узлы-коннекторы ("O");
  3. повторяет алгоритм TalentGrid.java (коннекторы + BFS) и печатает получившиеся связи —
     видно, не связалось ли лишнего;
  4. пишет src/main/resources/data/mmorpg/talent_tree/divine.json.

Запуск:  python3 tools/talent_gen/build_divine_tree.py [--check]
  --check — только проверка, файл не перезаписывать.
"""

import json
import os
import re
import sys
from collections import deque

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUT = os.path.join(REPO, "src", "main", "resources", "data", "mmorpg", "talent_tree", "divine.json")
PERKS_JAVA = os.path.join(REPO, "src", "main", "java", "com", "robertx22", "age_of_exile",
                          "aoe_data", "database", "perks", "DivinePerks.java")

TREE_ID = "divine"
TREE_ICON = "mmorpg:textures/gui/talent_icons/big_divine_light.png"

WIDTH = 57
HEIGHT = 27
# Центр — точка, на которую центрируется камера при открытии древа. Ставим его над осевой линией
# (ряд 13), чтобы по ряду 13 можно было проложить коннекторы между общими перками.
CENTER = (28, 12)

CONNECTOR = "O"
CENTER_TOKEN = "[CENTER]"
EMPTY = ""

# ------------------------------------------------------------------ раскладка

# id перка -> (x, y). Центр страницы — CENTER, слева некромант, справа призыватель.
POSITIONS = {
    # --- общие (центр) ---
    "div_shared_damage": (21, 13),
    "div_shared_health": (25, 13),
    "div_shared_duration": (31, 13),
    "div_shared_armor": (35, 13),
    "div_shared_damage_big": (25, 9),
    "div_shared_speed": (31, 9),
    "divine_command": (28, 5),
    "div_shared_health_big": (28, 17),

    # --- некромант (слева) ---
    "divine_necromancer": (14, 13),
    "div_necro_raise": (10, 9),
    "div_necro_legion": (10, 17),
    "div_necro_fury": (6, 7),
    "div_necro_flesh": (2, 10),
    "div_necro_lord": (6, 13),
    "div_necro_corpse": (6, 19),
    "div_necro_chain_death": (2, 16),

    # --- призыватель (справа) ---
    "divine_summoner": (42, 13),
    "div_summ_fangs": (46, 9),
    "div_summ_paws": (46, 17),
    "div_summ_wolves": (51, 8),
    "div_summ_bear": (51, 13),
    "div_summ_mammoth": (51, 18),
    "div_summ_hide": (55, 10),
    "div_summ_alpha": (55, 16),
}

# какие узлы должны быть связаны (коннекторы прокладываются автоматически)
LINKS = [
    # общие
    ("div_shared_damage", "div_shared_health"),
    ("div_shared_health", "div_shared_duration"),
    ("div_shared_duration", "div_shared_armor"),
    ("div_shared_health", "div_shared_damage_big"),
    ("div_shared_duration", "div_shared_speed"),
    ("div_shared_damage_big", "divine_command"),
    ("div_shared_speed", "divine_command"),
    ("div_shared_duration", "div_shared_health_big"),

    # вход в общие из обеих веток
    ("divine_necromancer", "div_shared_damage"),
    ("divine_summoner", "div_shared_armor"),

    # некромант
    ("divine_necromancer", "div_necro_raise"),
    ("divine_necromancer", "div_necro_legion"),
    ("div_necro_raise", "div_necro_fury"),
    ("div_necro_raise", "div_necro_flesh"),
    ("div_necro_legion", "div_necro_corpse"),
    ("div_necro_legion", "div_necro_chain_death"),
    ("div_necro_fury", "div_necro_lord"),
    ("div_necro_corpse", "div_necro_lord"),

    # призыватель
    ("divine_summoner", "div_summ_fangs"),
    ("divine_summoner", "div_summ_paws"),
    ("div_summ_fangs", "div_summ_wolves"),
    ("div_summ_fangs", "div_summ_bear"),
    ("div_summ_paws", "div_summ_bear"),
    ("div_summ_paws", "div_summ_mammoth"),
    # «Густая шерсть» и «Вожак стаи» должны быть доступны при любом выборе зверя
    # (звери — one_of_a_kind), поэтому оба узла связаны со всеми тремя
    ("div_summ_wolves", "div_summ_hide"),
    ("div_summ_bear", "div_summ_hide"),
    ("div_summ_mammoth", "div_summ_hide"),
    ("div_summ_wolves", "div_summ_alpha"),
    ("div_summ_bear", "div_summ_alpha"),
    ("div_summ_mammoth", "div_summ_alpha"),
]

ENTRY_PERKS = ["divine_necromancer", "divine_summoner"]

MAX_DISTANCE = 10  # GridPoint.MAX_DISTANCE


# ------------------------------------------------------------------ утилиты

def registered_perk_ids():
    """id всех перков, объявленных в DivinePerks.java."""
    src = open(PERKS_JAVA, encoding="utf-8").read()
    return re.findall(r'perk\("([a-z0-9_]+)"', src)


def build_grid():
    grid = [[EMPTY for _ in range(HEIGHT)] for _ in range(WIDTH)]

    for perk_id, (x, y) in POSITIONS.items():
        if grid[x][y]:
            raise SystemExit("В клетке (%d,%d) уже лежит %s" % (x, y, grid[x][y]))
        grid[x][y] = perk_id

    grid[CENTER[0]][CENTER[1]] = CENTER_TOKEN

    for a, b in LINKS:
        route(a, b, grid)

    return grid


def _l_cells(a, b):
    """Клетки Г-образного пути a -> b (клетка самого b не нужна)."""
    ax, ay = POSITIONS[a]
    bx, by = POSITIONS[b]

    cells = []
    x = ax
    while x != bx:
        x += 1 if bx > ax else -1
        cells.append((x, ay))
    y = ay
    while y != by:
        y += 1 if by > ay else -1
        cells.append((bx, y))

    return [c for c in cells if c != (bx, by)]


def _free(cell, grid):
    x, y = cell
    if not (0 <= x < WIDTH and 0 <= y < HEIGHT):
        return False
    if cell in POSITIONS.values() or cell == CENTER:
        return False
    return grid[x][y] in (EMPTY, CONNECTOR)


def _bfs_cells(a, b, grid):
    """Короткий путь в обход узлов и уже занятых клеток; None, если пути нет."""
    start, goal = POSITIONS[a], POSITIONS[b]
    queue = deque([start])
    prev = {start: None}

    while queue:
        cur = queue.popleft()
        if cur == goal:
            break
        x, y = cur
        for nxt in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
            if nxt in prev:
                continue
            if nxt != goal and not _free(nxt, grid):
                continue
            prev[nxt] = cur
            queue.append(nxt)

    if goal not in prev:
        return None

    path = []
    cur = goal
    while prev[cur] is not None:
        path.append(cur)
        cur = prev[cur]
    path.reverse()
    return path[:-1]  # клетка самого перка не нужна


def _usable(cells):
    for (x, y) in cells:
        if not (0 <= x < WIDTH and 0 <= y < HEIGHT):
            return False
        if (x, y) in POSITIONS.values():
            return False
        if (x, y) == CENTER:
            return False
    return True


def route(a, b, grid):
    """Путь коннекторов между перками: Г-образный, а если он упирается в узел — в обход."""
    for cells in (_l_cells(a, b), _l_cells(b, a), _bfs_cells(a, b, grid)):
        if cells is None or not _usable(cells):
            continue
        for (x, y) in cells:
            if grid[x][y] == EMPTY:
                grid[x][y] = CONNECTOR
        return

    raise SystemExit("Не удалось проложить путь %s -> %s" % (a, b))


# --------------------------------------------- повтор TalentGrid.java (проверка)

class GridPoint:
    def __init__(self, x, y, token):
        self.x, self.y = x, y
        self.id = token.lower()
        self.is_connector = len(self.id) == 1
        self.is_center = self.id == CENTER_TOKEN.lower()
        self.is_talent = (not self.is_connector and not self.is_center and len(self.id) > 2)

    @property
    def point(self):
        return (self.x, self.y)

    def in_distance_of(self, other):
        return abs(self.x - other.x) < MAX_DISTANCE and abs(self.y - other.y) < MAX_DISTANCE


def connections(grid):
    """Полный повтор TalentGrid.loadIntoTree(): какие перки с какими связаны."""
    points = {}
    for x in range(WIDTH):
        for y in range(HEIGHT):
            points[(x, y)] = GridPoint(x, y, grid[x][y])

    perks = [p for p in points.values() if p.is_talent]
    center = [p for p in points.values() if p.is_center]
    if len(center) != 1:
        raise SystemExit("В древе должен быть ровно один [CENTER], найдено %d" % len(center))

    def get(x, y):
        if 0 <= x < WIDTH and 0 <= y < HEIGHT:
            return points[(x, y)]
        return GridPoint(x, y, EMPTY)  # пустая клетка за краем

    def surrounding(p, connector):
        res = set()
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                if dx == 0 and dy == 0:
                    continue
                point = get(p.x + dx, p.y + dy)
                if abs(dx) == 1 and abs(dy) == 1:
                    if get(p.x + dx, p.y).is_talent or get(p.x, p.y + dy).is_talent:
                        continue
                if point.is_talent:
                    res.add(point.point)
                elif point.is_connector and point.id == connector:
                    res.add(point.point)
        return res

    def connector_types(p):
        types = set()
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                c = get(p.x + dx, p.y + dy)
                if c.is_connector:
                    types.add(c.id)
        return types

    def has_path(start, end, types):
        for connector in types:
            open_set = deque([start.point])
            closed = set()
            while open_set:
                current = open_set.popleft()
                if current in closed:
                    continue
                closed.add(current)
                if current == end.point:
                    return True
                cp = points[current]
                if cp.is_talent and current != start.point:
                    continue
                open_set.extend(surrounding(cp, connector))
        return False

    links = set()
    for one in perks:
        types = connector_types(one)
        for two in perks:
            if one.point == two.point:
                continue
            if not one.in_distance_of(two):
                continue
            if has_path(one, two, types):
                key = tuple(sorted([one.point, two.point]))
                links.add(key)
    return links


def check(grid):
    ids = registered_perk_ids()
    placed = list(POSITIONS.keys())

    problems = []

    for perk_id in placed:
        if perk_id not in ids:
            problems.append("Перк %s из древа не объявлен в DivinePerks.java" % perk_id)
    for perk_id in ids:
        if perk_id not in POSITIONS:
            problems.append("Перк %s из DivinePerks.java не размещён в древе" % perk_id)

    for a, b in LINKS:
        for perk_id in (a, b):
            if perk_id not in POSITIONS:
                problems.append("Связь ссылается на неизвестный перк %s" % perk_id)

    if problems:
        for p in problems:
            print("  ОШИБКА: " + p)
        return False

    links = connections(grid)
    by_id = {v: k for k, v in POSITIONS.items()}

    wanted = {tuple(sorted([POSITIONS[a], POSITIONS[b]])) for a, b in LINKS}
    missing = wanted - links
    extra = links - wanted

    print("Перков в древе: %d, связей: %d" % (len(POSITIONS), len(links)))

    ok = True
    if missing:
        ok = False
        for a, b in sorted(missing):
            print("  ОШИБКА: нет связи %s <-> %s" % (by_id[a], by_id[b]))
    if extra:
        print("  Лишние связи (не критично, но проверьте):")
        for a, b in sorted(extra):
            print("    %s <-> %s" % (by_id[a], by_id[b]))

    # достижимость от стартовых узлов
    adj = {}
    for a, b in links:
        adj.setdefault(a, set()).add(b)
        adj.setdefault(b, set()).add(a)

    for entry in ENTRY_PERKS:
        start = POSITIONS[entry]
        seen = {start}
        queue = deque([start])
        while queue:
            cur = queue.popleft()
            for nxt in adj.get(cur, ()):  # noqa
                if nxt not in seen:
                    seen.add(nxt)
                    queue.append(nxt)
        unreachable = set(POSITIONS.values()) - seen
        if unreachable:
            ok = False
            print("  ОШИБКА: от %s недоступны: %s" % (entry, sorted(by_id[p] for p in unreachable)))
        else:
            print("  От «%s» достижимы все %d узлов" % (entry, len(POSITIONS)))

    return ok


def to_json(grid):
    lines = []
    for y in range(HEIGHT):
        lines.append(",".join(grid[x][y] for x in range(WIDTH)))
    return {
        "text_format": "LIGHT_PURPLE",
        "identifier": TREE_ID,
        "school_type": "TALENTS",
        "icon": TREE_ICON,
        "order": 1,
        "perks": "\n".join(lines),
    }


def main():
    check_only = "--check" in sys.argv

    grid = build_grid()

    print("Древо «Божественный» (%s):" % TREE_ID)
    if not check(grid):
        raise SystemExit("Проверка не пройдена")

    data = to_json(grid)

    if check_only:
        print("--check: файл не записываем")
        return

    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")

    print("Записано: %s (%d байт)" % (os.path.relpath(OUT, REPO), os.path.getsize(OUT)))


if __name__ == "__main__":
    main()
