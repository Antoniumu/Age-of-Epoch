#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Планы талантов для гайдов класса «Божественный» (GuideData.java).

Гайд — это упорядоченный список узлов "x:y:perkId;...", который игрок берёт по правилам
TalentsData.canAllocate:

  * узел с is_entry берётся без связей (это «Путь некроманта» / «Путь призывателя»);
  * любой другой узел требует, чтобы хотя бы один СОСЕД из TalentGrid.connections был изучен;
  * узлы с одинаковым one_of_a_kind (волки / медведь / мамонт) взаимоисключающие — в план
    попадает только один из них.

Скрипт строит порядок так, чтобы каждое следующее звено было соседом уже взятого, и печатает
таблицу «индекс → требуемый уровень → lvl_req узла» (очки талантов на уровне L = 1 + 2 * L,
узел с индексом i открывается на уровне i / 2 — см. GuideDefinition.levelNeededForTalentStep).

Запуск:
    python3 tools/talent_gen/build_divine_plans.py            # напечатать планы и отчёт
    python3 tools/talent_gen/build_divine_plans.py --check    # только проверки (exit 1 при ошибке)
"""

import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

import build_divine_tree as B  # noqa: E402  (POSITIONS, build_grid, connections)

REPO = os.path.dirname(os.path.dirname(HERE))
TREE_JSON = os.path.join(REPO, "src", "main", "resources", "data", "mmorpg", "talent_tree",
                         "divine.json")

NECRO_ENTRY = "divine_necromancer"
SUMM_ENTRY = "divine_summoner"

# Какого зверя берёт гайд призывателя: остальные два закрыты one_of_a_kind = "divine_beast".
SUMMONER_BEAST = "div_summ_wolves"


# ------------------------------------------------------------------ метаданные перков
def statements():
    """Объявления перков из DivinePerks.registerAll() — по одному на блок строк."""
    src = open(B.PERKS_JAVA, encoding="utf-8").read()
    body = src[src.index("public void registerAll()"):]
    body = body[:body.index("\n    private static Perk perk(")]

    out = []
    cur = None
    for line in body.split("\n"):
        if re.match(r"^        (Perk \w+ = )?(perk|level|branch|beast)\(", line):
            if cur is not None:
                out.append("\n".join(cur))
            cur = [line]
        elif cur is not None:
            cur.append(line)
    if cur is not None:
        out.append("\n".join(cur))
    return out


def split_top_level(text):
    """Аргументы вызова, разрезанные по запятым верхнего уровня."""
    args, depth, cur = [], 0, []
    for ch in text:
        if ch in "([":
            depth += 1
        elif ch in ")]":
            depth -= 1
        if ch == "," and depth == 0:
            args.append("".join(cur))
            cur = []
        else:
            cur.append(ch)
    args.append("".join(cur))
    return args


def perk_meta():
    """id перка -> (lvl_req, ветка, is_entry, one_of_a_kind) — по DivinePerks.java."""
    meta = {}
    for st in statements():
        found = re.search(r'perk\("([a-z0-9_]+)"', st)
        if not found:
            continue
        pid = found.group(1)

        lvl = 0
        for wrapper in ("level(", "beast("):
            at = st.find(wrapper)
            if at < 0:
                continue
            start = st.index("(", at)
            depth, end = 0, -1
            for k in range(start, len(st)):
                if st[k] == "(":
                    depth += 1
                elif st[k] == ")":
                    depth -= 1
                    if depth == 0:
                        end = k
                        break
            if end < 0:
                continue
            last = split_top_level(st[start + 1:end])[-1].strip()
            if re.fullmatch(r"\d+", last):
                lvl = int(last)
            break

        # ветка и one_of_a_kind: у «зверей» их проставляет хелпер beast()
        if re.search(r"\bbeast\(", st):
            branch, group = "summoner", "divine_beast"
        else:
            group = None
            if "BRANCH_NECROMANCER" in st:
                branch = "necromancer"
            elif "BRANCH_SUMMONER" in st:
                branch = "summoner"
            else:
                branch = None

        meta[pid] = (lvl, branch, "is_entry = true" in st, group)
    return meta


def adjacency():
    """Соседи перка в сетке — ровно то, что считает TalentGrid.loadIntoTree().

    connections() возвращает пары КООРДИНАТ (GridPoint.point), поэтому переводим их в id перков.
    """
    by_pos = {pos: pid for pid, pos in B.POSITIONS.items()}
    adj = {}
    for one, two in B.connections(B.build_grid()):
        a, b = by_pos.get(one), by_pos.get(two)
        if a is None or b is None:
            continue
        adj.setdefault(a, set()).add(b)
        adj.setdefault(b, set()).add(a)
    return adj


# ------------------------------------------------------------------ план
def make_plan(entry, branch, meta, adj):
    """Все узлы ветки + все общие, в порядке «сначала то, что доступно по уровню»."""
    other_entry = SUMM_ENTRY if entry == NECRO_ENTRY else NECRO_ENTRY

    wanted = {entry}
    for pid, (lvl, br, is_entry, group) in meta.items():
        if pid == other_entry:
            continue  # второй стартовый узел — это уже другой класс
        if group == "divine_beast" and pid != SUMMONER_BEAST:
            continue  # зверь один на выбор
        if br == branch or br is None:
            wanted.add(pid)

    order = [entry]
    taken = {entry}

    while len(order) < len(wanted):
        free = wanted - taken
        candidates = [p for p in free if adj.get(p, set()) & taken]
        if not candidates:
            raise SystemExit("план недостижим: нет связи к %s" % sorted(free))
        last = B.POSITIONS[order[-1]]
        # порядок детерминированный: уровень, близость к предыдущему узлу, координаты, id
        candidates.sort(key=lambda p: (meta[p][0],
                                       abs(B.POSITIONS[p][0] - last[0])
                                       + abs(B.POSITIONS[p][1] - last[1]),
                                       B.POSITIONS[p][0], B.POSITIONS[p][1], p))
        pick = candidates[0]
        order.append(pick)
        taken.add(pick)

    return order


def compact(order):
    return ";".join("%d:%d:%s" % (B.POSITIONS[p][0], B.POSITIONS[p][1], p) for p in order)


# ------------------------------------------------------------------ проверки
def check_plans(plans, meta, adj):
    errors = []

    # координаты в древе должны совпадать с тем, что лежит в divine.json
    data = json.load(open(TREE_JSON, encoding="utf-8"))
    in_json = {}
    for row_i, row in enumerate(data["perks"].split("\n")):
        for col_i, cell in enumerate(row.split(",")):
            token = cell.strip()
            if len(token) > 2:
                in_json[token] = (col_i, row_i)

    for name, order in plans.items():
        groups = {}
        for i, pid in enumerate(order):
            if pid not in meta:
                errors.append("%s: шаг %d — нет такого перка в DivinePerks.java (%s)" % (name, i, pid))
                continue
            if in_json.get(pid) != B.POSITIONS[pid]:
                errors.append("%s: координаты %s (%s) не совпадают с divine.json (%s)"
                              % (name, pid, B.POSITIONS[pid], in_json.get(pid)))
            if i > 0 and not (adj.get(pid, set()) & set(order[:i])):
                errors.append("%s: шаг %d (%s) не сосед ни с одним предыдущим узлом"
                              % (name, i, pid))
            lvl, branch, is_entry, group = meta[pid]
            if i == 0 and not is_entry:
                errors.append("%s: первый шаг %s не является стартовым узлом (is_entry)" % (name, pid))
            if i > 0 and is_entry:
                errors.append("%s: шаг %d (%s) — второй стартовый узел, так нельзя" % (name, i, pid))
            if branch is not None and branch != name:
                errors.append("%s: шаг %d (%s) из чужой ветки %s" % (name, i, pid, branch))
            if group:
                if group in groups:
                    errors.append("%s: %s и %s — оба one_of_a_kind=%s, вместе не берутся"
                                  % (name, groups[group], pid, group))
                groups[group] = pid

        # охват: все узлы ветки (кроме чужих зверей) и все общие должны быть в плане
        missing = [p for p, (lvl, br, e, g) in meta.items()
                   if (br == name or br is None)
                   and not (g == "divine_beast" and p != SUMMONER_BEAST)
                   and p not in order]
        if missing:
            errors.append("%s: в план не вошли %s" % (name, sorted(missing)))

    return errors


GUIDE_JAVA = os.path.join(REPO, "src", "main", "java", "com", "robertx22", "age_of_exile",
                          "guides", "GuideData.java")


def guide_entry(src, guide_id):
    """Кусок GuideData.java с объявлением одного гайда (до следующего new GuideDefinition)."""
    at = src.find('"%s"' % guide_id)
    if at < 0:
        return None
    nxt = src.find("new GuideDefinition(", at)
    return src[at:nxt if nxt > 0 else len(src)]


PERKS_JAVA = os.path.join(REPO, "src", "main", "java", "com", "robertx22", "age_of_exile",
                          "aoe_data", "database", "perks", "DivinePerks.java")

# GameBalanceConfig.DEFAULT_GUIDE_MAX_LEVEL — охват гайда по умолчанию
GUIDE_MAX_LEVEL = 20


def lvl_reqs():
    """lvl_req перков из DivinePerks.java (без обёртки level() — 1, как в Perk.lvl_req)."""
    src = open(PERKS_JAVA, encoding="utf-8").read()
    out = {}
    for m in re.finditer(r'perk\("([a-z_]+)"', src):
        start = src.index("(", m.start())
        depth = 0
        end = start
        for j in range(start, len(src)):
            if src[j] == "(":
                depth += 1
            elif src[j] == ")":
                depth -= 1
                if depth == 0:
                    end = j
                    break
        # после perk(...) остаются аргументы обёрток: ", 25)" или ", 25), BRANCH_...)"
        nums = re.findall(r"\b(\d+)\b", src[end + 1:src.index(";", end)])
        out[m.group(1)] = int(nums[0]) if nums else 1
    return out


def check_levels(plans, req):
    """План должен работать с 1 уровня и целиком влезать в охват гайда."""
    errors = []
    for build, plan in plans.items():
        for pid in plan:
            if pid not in req:
                errors.append("%s: у перка %s не найден lvl_req в DivinePerks.java" % (build, pid))
        first = req.get(plan[0], 1)
        if first > 1:
            errors.append("%s: входной узел %s требует %d ур. — до этого гайд ничего не советует"
                          % (build, plan[0], first))
        # talentStepsInScope обрывается на первом недостижимом шаге, поэтому за таким
        # шагом не должно остаться доступных узлов — иначе они молча выпадут из плана
        blocked = [i for i, pid in enumerate(plan)
                   if max(i // 2, req.get(pid, 1)) > GUIDE_MAX_LEVEL]
        if blocked:
            first = blocked[0]
            after = [pid for i, pid in enumerate(plan) if i > first and i not in blocked]
            if after:
                errors.append("%s: после недостижимого %s идут доступные узлы %s — "
                              "они выпадут из охвата гайда"
                              % (build, plan[first], ", ".join(after)))
            if len(plan) - len(blocked) < 8:
                errors.append("%s: в пределах %d ур. доступно только %d узл. плана"
                              % (build, GUIDE_MAX_LEVEL, len(plan) - len(blocked)))
    return errors


def check_java(plans):
    """Планы в GuideData.java должны совпадать со сгенерированными, а древо быть «divine»."""
    errors = []
    src = open(GUIDE_JAVA, encoding="utf-8").read()

    for build, guide_id in (("necromancer", "divine_necromancer"), ("summoner", "divine_summoner")):
        entry = guide_entry(src, guide_id)
        if entry is None:
            errors.append("в GuideData.java нет гайда %s — класс не попал в список" % guide_id)
            continue

        found = re.search(r'\.talentPlan\("([^"]*)"\)', entry)
        if not found:
            errors.append("%s: нет talentPlan(...)" % guide_id)
        elif found.group(1) != compact(plans[build]):
            errors.append("%s: план в GuideData.java не совпадает с сеткой divine.json\n"
                          "      в Java: %s\n"
                          "      должно: %s" % (guide_id, found.group(1), compact(plans[build])))

        tree = re.search(r'\.tree\("([^"]*)"\)', entry)
        if not tree:
            errors.append("%s: не указано .tree(\"divine\") — план будет искаться в основном древе"
                          % guide_id)
        elif tree.group(1) != "divine":
            errors.append("%s: .tree(\"%s\") вместо divine" % (guide_id, tree.group(1)))

    return errors


def report(plans, meta):
    for name, order in plans.items():
        print("\n%s — %d узлов" % (name, len(order)))
        print("  шаг  ур.  lvl_req  узел")
        for i, pid in enumerate(order):
            print("  %4d %4d %8d  %s" % (i, i // 2, meta[pid][0], pid))
        print()
        print(compact(order))


def main():
    check_only = "--check" in sys.argv

    meta = perk_meta()
    adj = adjacency()

    plans = {
        "necromancer": make_plan(NECRO_ENTRY, "necromancer", meta, adj),
        "summoner": make_plan(SUMM_ENTRY, "summoner", meta, adj),
    }

    req = lvl_reqs()
    errors = check_plans(plans, meta, adj) + check_java(plans) + check_levels(plans, req)

    if not check_only:
        report(plans, meta)

    if errors:
        print("\nОшибки плана:")
        for e in errors:
            print("  - " + e)
        return 1

    for build, plan in plans.items():
        print("  %s: завершается на ур. %d (очков талантов нужно: %d)"
              % (build, max(max(i // 2, req.get(pid, 1)) for i, pid in enumerate(plan)), len(plan)))
    print("\nПланы в порядке: каждый узел соседствует с уже взятым, чужих веток и")
    print("повторяющихся one_of_a_kind нет.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
