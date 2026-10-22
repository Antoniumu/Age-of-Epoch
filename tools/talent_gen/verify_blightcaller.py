#!/usr/bin/env python3
"""Проверка древа «Скверноводец» (data/mmorpg/talent_tree/blightcaller.json).

Проверяет, что:
  * каждый узел сетки зарегистрирован в BlightcallerPerks.java ровно один раз;
  * у каждого узла есть иконка-файл и ключи локализации ru_ru / en_us;
  * все узлы достижимы от стартовых (is_entry) по соседству в сетке;
  * узлы некроманта и призывателя стоят в своих ветках, звери — с one_of_a_kind;
  * ни один ид не совпадает с перками «Божественного» (div_*, divine_*).

Запуск из корня репозитория:  python3 tools/talent_gen/verify_blightcaller.py
"""
import json
import os
import re
import sys
from collections import deque

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
TREE = os.path.join(ROOT, "src/main/resources/data/mmorpg/talent_tree/blightcaller.json")
JAVA = os.path.join(ROOT, "src/main/java/com/robertx22/age_of_exile/aoe_data/database/perks/BlightcallerPerks.java")
ASSETS = os.path.join(ROOT, "src/main/resources/assets/mmorpg")
LANG = {"ru_ru": os.path.join(ASSETS, "lang/ru_ru.json"), "en_us": os.path.join(ASSETS, "lang/en_us.json")}
DIVINE_PREFIXES = ("div_", "divine_")
BRANCH_OF = {"necro": "DivinePerks.BRANCH_NECROMANCER", "summ": "DivinePerks.BRANCH_SUMMONER"}

errors = []


def err(msg):
    errors.append(msg)


def grid_cells(text):
    """Сетка: ячейки через запятую, строки через перевод строки. Возвращает {(x,y): значение}."""
    cells = {}
    for y, row in enumerate(text.split("\n")):
        for x, cell in enumerate(row.split(",")):
            if cell:
                cells[(x, y)] = cell
    return cells


def java_registrations(src):
    """Все идентификаторы перков в perk("...", ...) и путь иконки для каждого."""
    found = {}
    for m in re.finditer(r'perk\("(bc_[a-z_]+)",\s*"[^"]*",\s*Perk\.PerkType\.\w+,\s*"([^"]+)"', src):
        found.setdefault(m.group(1), []).append(m.group(2))
    return found


def statement_of(src, pid):
    """Фрагмент Java от perk("pid" до конца оператора (;), чтобы увидеть ветку и beast()."""
    at = src.index('perk("%s"' % pid)
    start = src.rfind(";", 0, at) + 1   # оператор может начинаться с beast(/branch(
    end = src.index(";", at)
    return src[start:end]


def main():
    tree = json.load(open(TREE, encoding="utf-8"))
    if tree.get("identifier") != "blightcaller":
        err("identifier в blightcaller.json должен быть 'blightcaller'")
    if tree.get("school_type") != "TALENTS":
        err("school_type должен быть TALENTS")

    src = open(JAVA, encoding="utf-8").read()
    regs = java_registrations(src)
    for pid, paths in regs.items():
        if len(paths) != 1:
            err("перк %s зарегистрирован %d раз(а)" % (pid, len(paths)))

    cells = grid_cells(tree["perks"])
    perk_cells = {v: xy for xy, v in cells.items() if len(v) > 2 and v != "[CENTER]"}
    # ячейки из одного символа — соединители, [CENTER] — центр; всё остальное — ид перков
    ids = list(perk_cells)
    print("узлов в сетке: %d, зарегистрировано в Java: %d" % (len(ids), len(regs)))
    if len(ids) != 24:
        err("ожидалось 24 узла в сетке, найдено %d" % len(ids))
    for pid in ids:
        if pid not in regs:
            err("узел %s есть в сетке, но не зарегистрирован в BlightcallerPerks.java" % pid)
        if pid.startswith(DIVINE_PREFIXES):
            err("узел %s использует префикс «Божественного»" % pid)
    for pid in regs:
        if pid not in perk_cells:
            err("перк %s зарегистрирован, но не стоит в сетке древа" % pid)

    # иконки и локализация
    lang = {k: json.load(open(p, encoding="utf-8")) for k, p in LANG.items()}
    for pid, paths in regs.items():
        icon = os.path.join(ASSETS, paths[0])
        if not os.path.exists(icon):
            err("нет файла иконки для %s: %s" % (pid, paths[0]))
        for lg, d in lang.items():
            if ("mmorpg.talent." + pid) not in d:
                err("нет ключа mmorpg.talent.%s в %s" % (pid, lg))
    for lg, d in lang.items():
        if "mmorpg.word.blightcaller" not in d:
            err("нет ключа mmorpg.word.blightcaller в %s" % lg)
    tab_icon = os.path.join(ASSETS, tree["icon"].split(":", 1)[1])
    if not os.path.exists(tab_icon):
        err("нет иконки вкладки: %s" % tree["icon"])

    # связность: от каждого стартового узла должны достигаться все узлы сетки
    entries = [p for p in ids if "_entry" in p]
    if len(entries) != 2:
        err("ожидалось 2 стартовых узла (_entry), найдено %d" % len(entries))
    occupied = set(cells)
    for start in entries:
        seen = {perk_cells[start]}
        q = deque([perk_cells[start]])
        while q:
            x, y = q.popleft()
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                n = (x + dx, y + dy)
                if n in occupied and n not in seen:
                    seen.add(n)
                    q.append(n)
        unreachable = [cells[xy] for xy in occupied if xy not in seen and cells[xy] != "[CENTER]" and len(cells[xy]) <= 2]
        reached = [cells[xy] for xy in seen if len(cells[xy]) > 2 and cells[xy] != "[CENTER]"]
        if len(reached) != 24:
            err("от %s достижимо %d из 24 узлов" % (start, len(reached)))
        print("от %s достижимы все %d узлов" % (start, len(reached)) if len(reached) == 24 else "от %s достижимо %d" % (start, len(reached)))

    # ветки: некромант — necromancer, призыватель — summoner, звери — one_of_a_kind
    for pid in ids:
        st = statement_of(src, pid)
        if pid.startswith("bc_necro_") and "BRANCH_NECROMANCER" not in st and pid != "bc_necro_entry":
            err("узел %s не в ветке некроманта" % pid)
        if pid.startswith("bc_summ_") and "BRANCH_SUMMONER" not in st and "beast(" not in st and pid != "bc_summ_entry":
            err("узел %s не в ветке призывателя" % pid)
        if pid in ("bc_summ_wolves", "bc_summ_bear", "bc_summ_mammoth") and "beast(" not in st:
            err("зверь %s должен идти через beast() (one_of_a_kind)" % pid)
    for pid in ("bc_necro_entry", "bc_summ_entry"):
        if pid in regs:
            branch = "necromancer" if "necro" in pid else "summoner"
            if ("exclusive_branch = DivinePerks.BRANCH_%s" % branch.upper()) not in src:
                err("стартовый узел %s не задаёт exclusive_branch = %s" % (pid, branch))

    if errors:
        print("ОШИБКИ:")
        for e in errors:
            print("  -", e)
        return 1
    print("Всё в порядке.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
