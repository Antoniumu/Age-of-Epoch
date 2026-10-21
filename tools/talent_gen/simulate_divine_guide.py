#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Прогон логики советника гайдов по древу «Божественный».

Зачем: советник рекомендует узел плана, а взять его можно только если
TalentsData.canAllocate согласен (lvl_req, блокировка one_of_a_kind, свободные очки,
соседство с уже взятым). Если рекомендации и правила взятия расходятся, игрок видит
подсветку, которая не работает. Здесь тот же цикл считается в лоб для уровней 1..40,
поэтому расхождение видно сразу.

Повторяет шаги GuideAdvisor: talentStepsInScope (обрыв на первом шаге вне охвата),
nextActionableStep (пропуск узлов, закрытых уровнем или выбором ветки) и правило
«мост не ведёт через недоступный узел». Это модель, а не сам Java-код: она проверяет
данные (план, lvl_req, соседство) и формулу, но не компиляцию.

Запуск:
    python3 tools/talent_gen/simulate_divine_guide.py
    python3 tools/talent_gen/simulate_divine_guide.py --verbose
"""

import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

import build_divine_plans as P             # noqa: E402

MAX_BRIDGE_STEPS = 4                      # GuideAdvisor.MAX_BRIDGE_STEPS

# GameBalanceConfig: STARTING_TALENT_POINTS=1, TALENT_POINTS_AT_MAX_LEVEL=100, MAX_LEVEL=50
CAP = P.GUIDE_MAX_LEVEL
BEAST_GROUP = "divine_beast"


def talent_budget(level):
    return int(1 + 100 * float(level) / 50)


def level_needed(plan, req, i):
    """GuideAdvisor.levelNeededForTalentStep: очки талантов + lvl_req перка."""
    return max(i // 2, req.get(plan[i], 1))


def steps_in_scope(plan, req, use_lvl_req=True):
    """GuideMath.talentStepsInScope: обрыв на первом недоступном шаге.

    use_lvl_req=False — правило до правки: считались только очки талантов.
    """
    points = talent_budget(CAP)
    n = 0
    for i in range(len(plan)):
        need = level_needed(plan, req, i) if use_lvl_req else i // 2
        if need > CAP or i + 1 > points:
            break
        n = i + 1
    return n


def takeable(pid, req, locked, level):
    """Perk.isLockedToPlayer: lvl_req и блокировка one_of_a_kind."""
    return req.get(pid, 1) <= level and pid not in locked


def can_allocate(pid, taken_ids, adj, req, locked, level, free):
    """Правила TalentsData.canAllocate, которые важны для плана."""
    if free <= 0:
        return False, "нет свободных очков"
    if not takeable(pid, req, locked, level):
        if pid in locked:
            return False, "закрыт выбором в группе one_of_a_kind"
        return False, "нужен уровень %d" % req.get(pid, 1)
    if P.perk_meta()[pid][2]:           # is_entry — сосед не нужен
        return True, ""
    if not (adj.get(pid, set()) & taken_ids):
        return False, "не соседствует с взятым"
    return True, ""


def next_actionable(plan, req, locked, taken, level):
    """GuideAdvisor.nextActionableStep."""
    for i in range(steps_in_scope(plan, req)):
        if i in taken:
            continue
        if i // 2 > level:
            return -1                   # дальше по плану очков талантов не хватит
        if takeable(plan[i], req, locked, level):
            return i
    return -1


def bridge(adj, taken_ids, target, limit=MAX_BRIDGE_STEPS):
    """GuideAdvisor.pathTo: BFS от взятых узлов к цели, список ещё не взятых."""
    if target in taken_ids:
        return []
    prev = {t: None for t in taken_ids}
    depth = {t: 0 for t in taken_ids}
    queue = list(taken_ids)
    while queue:
        cur = queue.pop(0)
        d = depth.get(cur, 0)
        if d >= limit:
            continue
        for nxt in sorted(adj.get(cur, ())):
            if nxt in prev:
                continue
            prev[nxt] = cur
            depth[nxt] = d + 1
            if nxt == target:
                path = []
                node = nxt
                while node is not None and node not in taken_ids:
                    path.insert(0, node)
                    node = prev[node]
                return path
            queue.append(nxt)
    return None


def recommend(plan, req, adj, locked, taken_ids, level, free, use_lvl_req=True):
    """GuideAdvisor.computeRecommendedTalent: id узла под подсветку или None."""
    scope = steps_in_scope(plan, req, use_lvl_req)
    if scope <= 0 or free <= 0:
        return None
    if not taken_ids:
        if use_lvl_req and not takeable(plan[0], req, locked, level):
            return None                 # стартовый узел ещё закрыт уровнем
        return plan[0]
    for i in range(scope):
        pid = plan[i]
        if pid in taken_ids:
            continue
        if i // 2 > level:
            return None                 # ещё нет нужного количества очков талантов
        if use_lvl_req and not takeable(pid, req, locked, level):
            continue                    # узел закрыт уровнем или веткой — план идёт дальше
        if adj.get(pid, set()) & taken_ids:
            return pid
        path = bridge(adj, taken_ids, pid)
        if not path:
            return None
        # мост не должен вести через узел, который сейчас взять нельзя
        return path[0] if takeable(path[0], req, locked, level) else None
    return None


def simulate(plan, req, adj, locked=frozenset(), verbose=False, use_lvl_req=True,
             preallocate=()):
    """Узлы вне плана (мосты) тоже берутся — как в игре, советник их не откатывает.

    preallocate — что игрок взял сам до советника (например, другого зверя).
    """
    taken_ids = set(preallocate)        # всё взятое в древе, включая мосты вне плана
    steps = {plan.index(pid) for pid in taken_ids if pid in plan}
    problems = []
    done_at = None
    for level in range(1, 41):
        while True:
            free = talent_budget(level) - len(taken_ids)
            pid = recommend(plan, req, adj, locked, taken_ids, level, free, use_lvl_req)
            if pid is None:
                break
            ok, why = can_allocate(pid, taken_ids, adj, req, locked, level, free)
            if not ok:
                problems.append("ур.%d: советник рекомендует %s, а взять нельзя (%s)"
                                % (level, pid, why))
                break
            taken_ids.add(pid)
            if pid in plan:
                steps.add(plan.index(pid))
            if verbose:
                print("    ур.%2d  %s%s" % (level, pid, "" if pid in plan else "  (мост вне плана)"))
        goal = steps_in_scope(plan, req, use_lvl_req)
        reachable = goal - sum(1 for i in range(goal) if plan[i] in locked)
        if done_at is None and len(steps) == reachable:
            done_at = level
    return steps, problems, done_at


def main():
    verbose = "--verbose" in sys.argv
    src = open(P.GUIDE_JAVA, encoding="utf-8").read()
    plans_java = {}
    for build, guide_id in (("necromancer", "divine_necromancer"), ("summoner", "divine_summoner")):
        entry = P.guide_entry(src, guide_id)
        assert entry, "в GuideData.java нет гайда %s" % guide_id
        plans_java[build] = [s.split(":")[-1]
                             for s in re.search(r'\.talentPlan\("([^"]*)"\)', entry).group(1).split(";")]

    req = P.lvl_reqs()
    adj = P.adjacency()
    failures = 0

    for build, plan in plans_java.items():
        scope = steps_in_scope(plan, req)
        print("%s: шагов в плане %d, в охвате гайда (до %d ур.) %d"
              % (build, len(plan), CAP, scope))
        taken, problems, done_at = simulate(plan, req, adj, verbose=verbose)
        print("    взято в пределах охвата: %d из %d, охват закрыт на ур. %s"
              % (len(taken), scope, done_at if done_at is not None else "нет"))

        # то же самое правилом «до правки» — видно, что учёт lvl_req не декоративный
        _, old_problems, _ = simulate(plan, req, adj, use_lvl_req=False)
        dead = sorted({line.split("рекомендует ")[1].split(",")[0] for line in old_problems})
        print("    без учёта lvl_req советник %d раз отправлял бы на недоступные узлы: %s"
              % (len(old_problems), ", ".join(dead) if dead else "—"))

        # игрок взял зверя сам, в том числе не того, что в плане: советник не должен
        # вести на закрытый узел и должен довести план до конца
        meta = P.perk_meta()
        branch = meta[plan[0]][1]
        beasts = sorted(pid for pid in meta
                        if meta[pid][3] == BEAST_GROUP and meta[pid][1] == branch)
        for chosen in beasts:
            locked = frozenset(b for b in beasts if b != chosen)
            t, pr, d = simulate(plan, req, adj, locked, preallocate=[chosen])
            expect = scope - sum(1 for i in range(scope) if plan[i] in locked)
            in_plan = "в плане" if chosen in plan else "вне плана"
            print("    зверь %-16s (%s): взято %d из %d достижимых, закрыт на ур. %s%s"
                  % (chosen, in_plan, len(t), expect, d if d is not None else "нет",
                     ", ошибок нет" if not pr else ""))
            for line in pr:
                print("      ОШИБКА: %s" % line)
            if pr:
                failures += 1
            if len(t) != expect or d is None:
                print("      ОШИБКА: план не доведён до конца при выборе %s" % chosen)
                failures += 1

        for line in problems:
            print("    ОШИБКА: %s" % line)
        if problems:
            failures += 1
        if len(taken) != scope or done_at is None:
            print("    ОШИБКА: советник не довёл план до конца охвата")
            failures += 1

    print("смежность крупных узлов: hide -> %s, alpha -> %s"
          % (sorted(adj["div_summ_hide"]), sorted(adj["div_summ_alpha"])))

    if failures:
        print("\nЕсть расхождения: %d" % failures)
        return 1
    print("\nСоветник и правила взятия узлов согласованы на уровнях 1..40.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
