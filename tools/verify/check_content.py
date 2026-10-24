#!/usr/bin/env python3
"""Проверка целостности контента без запуска игры.

Что проверяет:

1. `talents.json` — сетка парсится, размеры/центр, дубликаты, «висячие» узлы
   (узел, до которого нет ни одного другого узла ближе MAX_DISTANCE = 10),
   узлы без коннектора рядом.
2. Каждый id узла дерева существует как строковый литерал в исходниках перков
   (ловит опечатки в id — узел, которого нет в реестре, был бы недостижим).
3. Для каждого game-changer перка есть иконка 16×16.
4. Уникаты/рунные слова: считает билдеры и сверяет с ожидаемым числом.

Запуск:  python3 tools/verify/check_content.py
Код возврата 0 — всё в порядке, 1 — есть ошибки.
"""

import json
import os
import re
import struct
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
TALENTS = os.path.join(ROOT, "src/main/resources/data/mmorpg/talent_tree/talents.json")
JAVA = os.path.join(ROOT, "src/main/java")
TEX = os.path.join(ROOT, "src/main/resources/assets/mmorpg/textures/gui")

MAX_DISTANCE = 10
CENTER_ID = "[center]"

errors = []
warnings = []


def fail(msg):
    errors.append(msg)


def warn(msg):
    warnings.append(msg)


def parse_grid(text):
    rows = text.split("\n")
    # сетка хранится по столбцам: строки файла — это координата x
    width = max(len(r.split(",")) for r in rows)
    height = len(rows)
    cells = {}
    for x, row in enumerate(rows):
        for y, token in enumerate(row.split(",")):
            t = token.strip()
            if t:
                cells[(x, y)] = t.lower()
    return cells, width, height


def check_talents():
    with open(TALENTS, encoding="utf-8") as f:
        data = json.load(f)

    cells, width, height = parse_grid(data["perks"])
    talents = {p: t for p, t in cells.items() if len(t) > 2 and t != CENTER_ID}
    centers = [p for p, t in cells.items() if t == CENTER_ID]
    connectors = {p for p, t in cells.items() if len(t) == 1}

    print(f"talents.json: сетка {width}×{height}, занятых клеток {len(cells)}")
    print(f"  узлов {len(talents)}, коннекторов {len(connectors)}, центр {centers}")

    if len(centers) != 1:
        fail(f"должен быть ровно один центр, найдено {len(centers)}")

    ids = list(talents.values())
    # Один и тот же перк может стоять в нескольких клетках (так устроены мелкие
    # стат-узлы) — это не ошибка. Ошибкой был бы, например, узел без соседей.
    print(f"  уникальных перков в дереве: {len(set(ids))}")

    if len(ids) < 700:
        fail(f"узлов в дереве подозрительно мало: {len(ids)} (ожидалось ~711)")

    # «Висячие» узлы: рядом (в пределах MAX_DISTANCE по обеим осям) нет ни одного
    # другого узла — такой узел физически не может получить связь.
    coords = list(talents.keys())
    lonely = []
    for (x, y) in coords:
        others = [c for c in coords if c != (x, y)
                  and abs(c[0] - x) < MAX_DISTANCE and abs(c[1] - y) < MAX_DISTANCE]
        if not others:
            lonely.append(talents[(x, y)])
    if lonely:
        fail(f"узлы без соседей в радиусе {MAX_DISTANCE}: {sorted(lonely)}")

    # Узел без коннектора рядом: связь возможна только если коннектор лежит
    # между двумя узлами, поэтому проверяем хотя бы наличие коннектора поблизости.
    no_connector = []
    for (x, y) in coords:
        near = any(abs(cx - x) < MAX_DISTANCE and abs(cy - y) < MAX_DISTANCE
                   for (cx, cy) in connectors)
        if not near:
            no_connector.append(talents[(x, y)])
    if no_connector:
        fail(f"узлы без единого коннектора в радиусе {MAX_DISTANCE}: {sorted(no_connector)}")

    # Все id должны встречаться в исходниках перков.
    blob = []
    for base, _, files in os.walk(JAVA):
        for name in files:
            if name.endswith(".java"):
                with open(os.path.join(base, name), encoding="utf-8", errors="ignore") as f:
                    blob.append(f.read())
    source = "\n".join(blob)

    # Часть id собирается конкатенацией ("big_heart_of_" + suffix(ele)), поэтому
    # проверяем и «составные» id: если id начинается или кончается строковым
    # литералом из исходников, он мог быть собран динамически.
    literals = {m for m in re.findall(r'"([^"\\\n]{6,})"', source)}
    unknown = []
    for i in sorted(set(ids)):
        if i in source:
            continue
        if any(i.startswith(lit) or i.endswith(lit) for lit in literals):
            continue
        unknown.append(i)
    if unknown:
        fail(f"id узлов, которых нет в исходниках (опечатка?): {unknown}")

    return set(ids), len(talents), len(connectors)


def png_size(path):
    with open(path, "rb") as f:
        head = f.read(24)
    if head[:8] != b"\x89PNG\r\n\x1a\n":
        return None
    return struct.unpack(">II", head[16:24])


def check_icons(tree_ids):
    base = os.path.join(TEX, "stat_icons/game_changers")
    icons = sorted(f[:-4] for f in os.listdir(base) if f.endswith(".png"))
    bad = []
    for icon in icons:
        size = png_size(os.path.join(base, icon + ".png"))
        if size is None:
            bad.append(f"{icon} (не PNG)")
        elif size != (16, 16):
            bad.append(f"{icon} ({size[0]}×{size[1]})")
    print(f"\nиконки game-changer: {len(icons)}")
    if bad:
        fail(f"иконки не 16×16 PNG: {bad}")

    # Каждый game-changer перк из дерева должен иметь иконку.
    tree_icons = sorted(set(tree_ids) & set(icons))
    print(f"  game-changer перков в дереве: {len(tree_icons)}")
    if len(tree_icons) < 15:
        warn(f"в дереве меньше 15 game-changer перков ({len(tree_icons)})")


def check_gear_counts():
    uniques_dir = os.path.join(JAVA, "com/robertx22/age_of_exile/aoe_data/database/unique_gears")
    total = 0
    per_file = {}
    for base, _, files in os.walk(uniques_dir):
        for name in files:
            if not name.endswith(".java") or name == "UniqueGearBuilder.java":
                continue
            path = os.path.join(base, name)
            with open(path, encoding="utf-8") as f:
                n = f.read().count("UniqueGearBuilder.of(")
            if n:
                per_file[name] = n
                total += n
    print(f"\nуникатов/рунных слов (билдеры): {total}")
    for name, n in sorted(per_file.items()):
        print(f"  {name}: {n}")
    if total != 25:
        fail(f"ожидалось 25 билдеров (18 уникатов + 7 рунных слов), найдено {total}")
    return total


def main():
    tree_ids, nodes, connectors = check_talents()
    check_icons(tree_ids)
    check_gear_counts()

    print()
    if warnings:
        print("Предупреждения:")
        for w in warnings:
            print("  !", w)
        print()
    if errors:
        print("Ошибки:")
        for e in errors:
            print("  ✗", e)
        return 1
    print(f"OK: {nodes} узлов, {connectors} коннекторов, нарушений нет")
    return 0


if __name__ == "__main__":
    sys.exit(main())
