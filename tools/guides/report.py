#!/usr/bin/env python3
"""
Собирает отчёт GUIDES.md: арифметика игры + три готовых гайда.

Запуск: python3 tools/guides/report.py
"""
import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))

sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(HERE, "..", "talent_gen"))
import aoe_math as M          # noqa: E402
import build_guides as B      # noqa: E402
import build_divine_plans as D  # noqa: E402

OUT = os.path.join(ROOT, "GUIDES.md")

LEVELS = (1, 10, 25, 40, 50)

GUIDES = {
    "warrior": {
        "title": "Стальной страж",
        "role": "Воин ближнего боя: СИЛ + ЖИВ + ЛОВ, меч/топор и тяжёлая броня, школа Божественности.",
        "school": "Божественность (divine)",
        "kit": "0 — Воин (латная броня, меч, топор, щит)",
        "spells": [("gong_strike", 16), ("whirlwind", 16), ("undying_will", 16),
                   ("charge", 10), ("taunt", 10), ("banish", 10), ("pull", 8), ("shooting_star", 8)],
        "why": [
            "СИЛ — самый эффективный стат воина: 1 очко = 10.8 брони (защита) + 1.08 физ. урона (атака).",
            "ЖИВ — единственная защита от стихийного урона: броня и уклонение против заклинаний не работают.",
            "ЛОВ добавляет уклонение (полное обнуление удара) и тот же физ. урон, что и СИЛ.",
        ],
    },
    "mage": {
        "title": "Ледяной маг",
        "role": "Маг льда: ИНТ + МУД + ЖИВ, магический щит, контроль и урон по площади, школа Воды.",
        "school": "Вода (water)",
        "kit": "2 — Маг (магический щит, посох, скипетр, щит)",
        "spells": [("frostball", 16), ("frost_nova", 16), ("ice_comet", 16),
                   ("chilling_field", 12), ("chill_eruption", 12), ("frostball_chill", 8),
                   ("mage_circle", 8), ("frost_armor", 8)],
        "why": [
            "ИНТ = сила заклинаний И сопротивления огню/воде/природе: 55 ИНТ дают 594 сопротивления (≈52% снижения).",
            "МУД = мана, а магический щит тратит ману вместо HP — это «броня» мага.",
            "ЖИВ страхует от ядов/кровотечения и добивания, когда щит и мана кончились.",
        ],
    },
    "hunter": {
        "title": "Охотник-отравитель",
        "role": "Стрелок: ЛОВ + ЖИВ + СИЛ, лук, скорость атаки, уклонение и яды, школа Охоты.",
        "school": "Охота (hunting)",
        "kit": "1 — Охотник (кожаная броня, лук, арбалет, щит)",
        "spells": [("arrow_storm", 16), ("poison_arrow", 16), ("charged_bolt", 16),
                   ("explosive_arrow", 16), ("dash", 8), ("backflip", 8),
                   ("poison_trap", 8), ("fire_trap", 8)],
        "why": [
            "ЛОВ = уклонение + физ. урон: 55 ЛОВ дают 594 уклонения (≈57%) и 59 урона.",
            "Крит: при 30% шанса и 180% урона крита средний ДПС растёт на 24% — дешевле, чем ещё один стат.",
            "ЖИВ выравнивает провал по HP: охотник получает много урона от того, что не уклонился.",
        ],
    },
}

# id -> локализованное имя перка (из Java-регистрации)
NAME_RE = re.compile(r'(?:register|bigStat|gameChanger|stat|perk|level|beast|branch)\(\s*"([a-z0-9_]+)"\s*,\s*"([^"]+)"')


def perk_names():
    names = {}
    for name in sorted(os.listdir(B.PERK_DIR)):
        if not name.endswith(".java"):
            continue
        text = open(os.path.join(B.PERK_DIR, name), encoding="utf-8").read()
        for m in NAME_RE.finditer(text):
            names.setdefault(m.group(1), m.group(2))
    return names


def apportion(total, weights):
    raw = {k: total * w for k, w in weights.items()}
    out = {k: int(v) for k, v in raw.items()}
    left = total - sum(out.values())
    order = sorted(raw, key=lambda k: raw[k] - int(raw[k]), reverse=True)
    for k in order[:left]:
        out[k] += 1
    return out


def core_totals(points, level):
    acc = {}
    for stat_id, num in points.items():
        for sid, val in M.core_stat_gain(stat_id, level):
            acc[sid] = acc.get(sid, 0.0) + val * num
    return acc


def snapshot(profile, level):
    return snapshot_weights({k: v for k, v in B.GUIDES[profile]["split"]}, level)


def snapshot_weights(weights, level):
    pts = apportion(M.stat_points(level), weights)
    t = core_totals(pts, level)
    hp = M.base_stat("health", level) + t.get("health", 0)
    mana = M.base_stat("mana", level) + t.get("mana", 0)
    energy = M.base_stat("energy", level) + t.get("energy", 0)
    sp = M.base_stat("spell_power", level) + t.get("spell_power", 0)
    phys = M.base_stat("physical_weapon_damage", level) + t.get("physical_weapon_damage", 0)
    armor = t.get("armor", 0)
    dodge = t.get("dodge", 0)
    res = t.get("fire_resist", 0)
    return {
        "level": level, "points": pts,
        "hp": hp, "mana": mana, "energy": energy, "sp": sp, "phys": phys,
        "armor": armor, "armor_pct": M.armor_reduction(armor, level) * 100,
        "dodge": dodge, "dodge_pct": M.dodge_chance(dodge, level) * 100,
        "res": res, "res_pct": M.resist_reduction(res, level) * 100,
    }


def fmt(x, digits=0):
    return f"{x:.{digits}f}"


def level_table(profile):
    return level_table_weights({k: v for k, v in B.GUIDES[profile]["split"]})


def level_table_weights(weights):
    rows = []
    for lvl in LEVELS:
        s = snapshot_weights(weights, lvl)
        rows.append("| {} | {} | {} | {} | {} | {} | {} | {} |".format(
            lvl,
            " / ".join(f"{k[:3].upper()} {v}" for k, v in s["points"].items() if v),
            fmt(s["hp"]), fmt(s["mana"]),
            f'{fmt(s["armor"])} ({fmt(s["armor_pct"], 1)}%)',
            f'{fmt(s["dodge"])} ({fmt(s["dodge_pct"], 1)}%)',
            f'{fmt(s["res"])} ({fmt(s["res_pct"], 1)}%)',
            f'{fmt(s["sp"], 1)} / {fmt(s["phys"], 1)}',
        ))
    return rows


# Гайды класса «Божественный»: план лежит в древе divine, а не в основном,
# поэтому шаги ограничены не бюджетом очков, а lvl_req узлов.
DIVINE_GUIDES = {
    "divine_necromancer": {
        "title": "Некромант",
        "role": "Власть над мёртвыми: поднятые враги, взрывы трупов, древо «Божественный».",
        "weights": [("intelligence", 0.45), ("vitality", 0.40), ("wisdom", 0.15)],
        "why": [
            "ИНТ — сила заклинаний и сопротивления: урон «Взрыва трупа» идёт от вас, поэтому работают крит и пробивание.",
            "ЖИВ — свита танкует, но вы стоите рядом: 2 HP и 0.1 регена за очко.",
            "МУД — мана на ядовитый шар и тотемы: подъём мёртвых и взрывы тратят заклинания природы.",
        ],
    },
    "divine_summoner": {
        "title": "Призыватель",
        "role": "Стая зверей: волки, медведь или боевой мамонт, древо «Божественный».",
        "weights": [("vitality", 0.45), ("dexterity", 0.30), ("intelligence", 0.25)],
        "why": [
            "ЖИВ — звери держат удар первыми, но достают и вас: HP и реген важнее собственного урона.",
            "ЛОВ — уклонение: стая физически не успевает перехватить каждый удар.",
            "ИНТ — сила зверей растёт от узлов древа, а заклинания природы дают контроль и лечение.",
        ],
    },
}

DIVINE_KIT = "2 — Маг (магический щит, посох, скипетр)"
DIVINE_SCHOOL = "Природа (nature)"
DIVINE_TREE = "«Божественный» (divine) — вторая вкладка древа талантов"


def divine_plan(guide_id):
    """План и заклинания гайда ровно так, как они записаны в GuideData.java."""
    entry = D.guide_entry(open(D.GUIDE_JAVA, encoding="utf-8").read(), guide_id)
    if entry is None:
        raise SystemExit("в GuideData.java нет гайда %s" % guide_id)
    plan = [step.split(":")[-1]
            for step in re.search(r'\.talentPlan\("([^"]*)"\)', entry).group(1).split(";")]
    spells = [(m.group(1), int(m.group(2)))
              for m in re.finditer(r'\.spell\("([a-z_]+)", (\d+)\)', entry)]
    return plan, spells


def plan_phases(plan):
    """Разбивает план на 4 фазы по уровню (очков талантов на уровне L = 1 + 2L)."""
    steps = plan.split(";")
    phases = [(1, 10), (11, 25), (26, 40), (41, 50)]
    out = []
    for lo, hi in phases:
        first = M.talent_points(lo - 1) if lo > 1 else 0
        last = M.talent_points(hi)
        chunk = steps[first:last]
        out.append((lo, hi, first + 1, min(last, len(steps)), chunk))
    return out


def main():
    plans = json.load(open(os.path.join(HERE, "plans.json"), encoding="utf-8"))
    names = perk_names()
    L = []
    A = L.append

    A("# Гайды Age of Epoch")
    A("")
    A("Готовые сборки «под ключ»: характеристики, таланты и заклинания. Вне Полигона гайд работает")
    A("как система подсказок (подсветка кнопок), на Полигоне — собирается в готовый билд одной кнопкой.")
    A("")
    A("Ниже — арифметика, на которой построены гайды, и сами сборки.")
    A("")
    A("---")
    A("")
    A("## 1. Арифметика игры")
    A("")
    A("### 1.1. Скейлинг уровня")
    A("")
    A("Все флэт-статы умножаются на скейлинг (`LevelScalingConfig`):")
    A("")
    A("| Скейлинг | Формула | 1 ур. | 10 ур. | 25 ур. | 40 ур. | 50 ур. |")
    A("|---|---|---|---|---|---|---|")
    for name, fn in (("NORMAL (HP, броня, урон)", M.normal),
                     ("SLOW (регены)", M.slow),
                     ("CORE_STAT (очки характеристик)", M.core_stat),
                     ("STAT_REQ (требования)", M.stat_req)):
        A("| {} | {} | {} | {} | {} | {} | {} |".format(
            name, {M.normal: "1 + 0.2·(L−1)", M.slow: "1 + 0.01·(L−1)",
                   M.core_stat: "1 + 0.05·(L−1)", M.stat_req: "2 + 2·(L−1)"}[fn],
            *[f"{fn(l):.2f}" for l in LEVELS]))
    A("")
    A("Очки на уровне: **характеристики** 2·L, **таланты** 1 + 100·L/50, **заклинания** 2·L.")
    A("Максимальный уровень заклинания — 16 (синергии — 8).")
    A("")
    A("### 1.2. Что даёт одно очко характеристики")
    A("")
    A("| Стат | За очко | 1 ур. | 25 ур. | 50 ур. |")
    A("|---|---|---|---|---|")
    rows = [
        ("СИЛ", "0.1 физ. урона + 1 брони", "strength", "physical_weapon_damage", "armor"),
        ("ЛОВ", "0.1 физ. урона + 1 уклонения", "dexterity", "physical_weapon_damage", "dodge"),
        ("ИНТ", "0.1 силы заклинаний + 1 сопр. огню/воде/природе", "intelligence", "spell_power", "fire_resist"),
        ("ЖИВ", "2 HP + 0.1 регена", "vitality", "health", "health_regen"),
        ("МУД", "2 маны + 0.1 регена", "wisdom", "mana", "mana_regen"),
        ("ЛВК", "2 энергии + 0.1 регена", "agility", "energy", "energy_regen"),
    ]
    for title, text, sid, k1, k2 in rows:
        vals = []
        for lvl in (1, 25, 50):
            g = dict(M.core_stat_gain(sid, lvl))
            vals.append(f"{g.get(k1, 0):.1f} / {g.get(k2, 0):.1f}")
        A(f"| {title} | {text} | {vals[0]} | {vals[1]} | {vals[2]} |")
    A("")
    A("Очки характеристик масштабируются `CORE_STAT` (3.45 на 50 ур.), внутренние статы — `NORMAL`")
    A("(10.8 на 50 ур.). Поэтому на 50 ур. 1 СИЛ = 10.8 брони, 1 ЖИВ = 21.6 HP.")
    A("")
    A("### 1.3. Защита: гипербола с потолком 80%")
    A("")
    A("`IUsableStat.getUsableValue`: `% = 0.8 · (v/need) / (v/need + 1.5)`, где `need` — значение,")
    A("необходимое для максимума на 1 ур., умноженное на `NORMAL(ур.)`.")
    A("")
    A("| Стат | need на 1 ур. | 40% | 60% | 70% |")
    A("|---|---|---|---|---|")
    for title, fn, need in (("Броня", M.armor_needed_for, 20), ("Уклонение", M.dodge_needed_for, 15)):
        A(f"| {title} | {need} | {fn(0.4, 50):.0f} | {fn(0.6, 50):.0f} | {fn(0.7, 50):.0f} |")
    A("")
    A("(значения — для 50 уровня; сопротивления считаются по той же формуле с need = 20.)")
    A("")
    A("| Броня | 10 ур. | 25 ур. | 40 ур. | 50 ур. |")
    A("|---|---|---|---|---|")
    for armor in (200, 400, 800, 1600):
        A(f"| {armor} | " + " | ".join(f"{M.armor_reduction(armor, l):.1%}" for l in (10, 25, 40, 50)) + " |")
    A("")
    A("### 1.4. Урон")
    A("")
    A("- Оружие: `AttackDamage(Physical)` = база 3 + 1·NORMAL(ур.) + вклад СИЛ/ЛОВ + статы оружия.")
    A("- Заклинание: `база × коэффициент(ур. заклинания)`, коэффициент растёт от мин. к макс. к 16 ур.")
    A("- Крит: `средний урон = урон × (1 + шанс·урон_крита)`, база 1% / 50%.")
    A("- Порядок множителей: `(база + FLAT) × (1 + PERCENT) × (1 + MULTI)`.")
    A("- Стихийные сопротивления снижают урон заклинаний; броня — только физический урон.")
    A("")
    A("---")
    A("")

    for idx, (profile, g) in enumerate(GUIDES.items(), start=1):
        plan = plans[profile]
        A(f"## 2.{idx}. {g['title']}")
        A("")
        A(g["role"])
        A("")
        A(f"- **Школа:** {g['school']}")
        A(f"- **Набор экипировки на Полигоне:** {g['kit']}")
        A(f"- **Веса характеристик:** " + ", ".join(f"{k} {v:.0%}" for k, v in B.GUIDES[profile]["split"]))
        A("")
        A("### Почему именно так")
        A("")
        for line in g["why"]:
            A(f"- {line}")
        A("")
        A("### Характеристики по уровням (без экипировки)")
        A("")
        A("| Ур. | Разбивка очков | HP | Мана | Броня (%) | Уклонение (%) | Сопр. (%) | СП / физ. урон |")
        A("|---|---|---|---|---|---|---|---|")
        for row in level_table(profile):
            A(row)
        A("")
        A("### Таланты по фазам")
        A("")
        A("Очков талантов на уровне L: `1 + 2·L` — 3 / 21 / 51 / 81 / 101 на 1 / 10 / 25 / 40 / 50 ур.")
        nodes = len(plan["plan"].split(";"))
        word = "узел" if nodes % 10 == 1 and nodes % 100 != 11 else (
            "узла" if nodes % 10 in (2, 3, 4) and nodes % 100 not in (12, 13, 14) else "узлов")
        A(f"План содержит **{nodes} {word}** — это ровно бюджет 50 уровня (1 + 2·50 = 101)."
          if nodes == 101 else
          f"План содержит **{nodes} {word}** — хватает до 50 уровня.")
        A("")
        for lo, hi, first, last, chunk in plan_phases(plan["plan"]):
            names_list = []
            for step in chunk:
                pid = step.split(":")[2]
                names_list.append(names.get(pid, pid))
            A(f"**Ур. {lo}–{hi} (очки {first}–{last}).** " + ", ".join(names_list) + ".")
            A("")
        A("### Заклинания")
        A("")
        A("| Заклинание | Цель | Треб. ур. |")
        A("|---|---|---|")
        school = {
            "warrior": {"gong_strike": 1, "whirlwind": 5, "charge": 5, "undying_will": 10,
                        "pull": 20, "taunt": 40, "banish": 40, "shooting_star": 10},
            "mage": {"frostball": 1, "frostball_chill": 5, "frost_nova": 5, "chilling_field": 10,
                     "ice_comet": 20, "chill_eruption": 20, "mage_circle": 30, "frost_armor": 5},
            "hunter": {"arrow_storm": 1, "dash": 5, "poison_arrow": 10, "charged_bolt": 20,
                       "backflip": 20, "poison_trap": 30, "fire_trap": 30, "explosive_arrow": 40},
        }[profile]
        total = 0
        for spell, target in g["spells"]:
            total += target
            A(f"| {spell} | {target} | {school.get(spell, '?')} |")
        A(f"| **Итого** | **{total}** из 100 | |")
        A("")
        A("### Как собрать")
        A("")
        A("1. Вкладка **Гайды** → выбрать сборку → «Сделать активным» (подсказки в обычном мире).")
        A("2. Полигон (`/sim`) → «Собрать» — гайд сбросит билд, вложит статы/таланты/заклинания и выдаст экипировку.")
        A("3. Отклонения: советник покажет узлы вне плана и предложит откатить лишние ПКМ.")
        A("")
        A("---")
        A("")

    req = D.lvl_reqs()
    cap = D.GUIDE_MAX_LEVEL
    for idx, (profile, g) in enumerate(DIVINE_GUIDES.items(), start=len(GUIDES) + 1):
        plan, spells = divine_plan(profile)
        needs = [max(i // 2, req.get(pid, 1)) for i, pid in enumerate(plan)]
        in_scope = sum(1 for n in needs if n <= cap)

        A(f"## 2.{idx}. {g['title']}")
        A("")
        A(g["role"])
        A("")
        A(f"- **Древо талантов:** {DIVINE_TREE}")
        A(f"- **Школа:** {DIVINE_SCHOOL}")
        A(f"- **Набор экипировки на Полигоне:** {DIVINE_KIT}")
        A("- **Веса характеристик:** "
          + ", ".join(f"{k} {v:.0%}" for k, v in g["weights"]))
        A("")
        A("### Почему именно так")
        A("")
        for line in g["why"]:
            A(f"- {line}")
        A("")
        A("### Характеристики по уровням (без экипировки)")
        A("")
        A("| Ур. | Разбивка очков | HP | Мана | Броня (%) | Уклонение (%) | Сопр. (%) | СП / физ. урон |")
        A("|---|---|---|---|---|---|---|---|")
        for row in level_table_weights(dict(g["weights"])):
            A(row)
        A("")
        A("### План талантов")
        A("")
        A("Узел открывается, когда хватает и очков талантов (`1 + 2·ур.`), и уровня перка —")
        A("в таблице указано большее из двух. Предел гайда — 20 уровней, поэтому узлы")
        A("25–30 уровня остаются за пределами плана: советник их не предлагает,")
        A("а прогресс «N из M» считается только по доступным.")
        A("")
        A("| № | Узел | Открывается |")
        A("|---|---|---|")
        for i, pid in enumerate(plan):
            mark = "" if needs[i] <= cap else " — за пределом гайда"
            A(f"| {i + 1} | {names.get(pid, pid)} | {needs[i]} ур.{mark} |")
        A(f"| **Итого** | **{len(plan)}** узлов | в пределах гайда: **{in_scope}** |")
        A("")
        if any(needs[i] < needs[i - 1] for i in range(1, len(needs))):
            A("Уровень в таблице не всегда растёт: порядок шагов диктует граф древа —")
            A("«Густая шерсть» связана только со зверями, поэтому стоит после «Стаи волков».")
            A("")
        A("### Заклинания")
        A("")
        A("| Заклинание | Цель |")
        A("|---|---|")
        total = 0
        for spell, target in spells:
            total += target
            A(f"| {spell} | {target} |")
        A(f"| **Итого** | **{total}** из 100 |")
        A("")
        A("У заклинаний в этом порте нет требования по уровню (`Spell` хранит только `max_lvl = 16`),")
        A("поэтому сборку ограничивает бюджет очков заклинаний — 2 за уровень.")
        A("")
        A("### Как собрать")
        A("")
        A("1. Вкладка **Гайды** → выбрать сборку → «Включить» (подсказки в обычном мире).")
        A("2. Полигон (`/sim`) → «Собрать» — гайд сбросит билд, вложит статы/таланты/заклинания и выдаст экипировку.")
        A("3. Ветку и зверя можно поменять: сброс талантов (ПКМ по узлу) возвращает выбор, советник поведёт дальше.")
        A("")
        A("---")
        A("")

    A("## 3. Как работает адаптация")
    A("")
    A("- **Характеристики.** Цель считается от ВСЕГО бюджета уровня (вложено + свободно) по весам гайда;")
    A("  подсказывается стат с максимальным дефицитом. Если игрок вложил очки «не туда»,")
    A("  гайд просто уменьшает долю остальных статов — математика сходится на любом уровне.")
    A("- **Таланты.** Подсвечивается первый не взятый узел плана, к которому есть путь; если он далеко,")
    A("  подсвечивается первый шаг по кратчайшему пути в реальном графе древа (BFS по связям).")
    A("  Узлы вне плана делятся на две группы: откатываемые (советник предложит ПКМ-сброс)")
    A("  и необходимые для прохода (гайд продолжается от них).")
    A("- **«Откатите этот талант».** Для каждого узла вне плана советник проверяет два условия:")
    A("  игру (`canRemoveIgnoringResetPoints` — сбрасывать можно только крайние узлы) и граф")
    A("  (после удаления все изученные узлы остаются связаны со стартом). Узлы, прошедшие обе")
    A("  проверки, попадают в список «стоит откатить» с именами; если очков сброса нет —")
    A("  советник прямо говорит, что их надо получить на Полигоне.")
    A("- **Другой стартовый путь.** Взять можно только один стартовый узел (`one_of_a_kind = start`),")
    A("  поэтому при несовместимом старте гайд честно сообщает, что план недостижим, и предлагает")
    A("  пересобрать билд на Полигоне.")
    A("- **Заклинания.** Подсвечивается следующее заклинание плана, которое персонаж уже может изучить")
    A("  по уровню; если рано — показывается требуемый уровень.")
    A("")

    A("## 4. Как проверялись планы")
    A("")
    A("`tools/guides/build_guides.py` проверяет каждый план перед записью и падает с ошибкой, если:")
    A("")
    A("- план начинается не со стартового перка гайда;")
    A("- в плане есть второй стартовый перк (`one_of_a_kind = start` — игра блокирует остальные);")
    A("- какой-то узел не связан ни с одним из предыдущих (нельзя взять по `canAllocate`);")
    A("- есть дубликаты узлов;")
    A("- длина плана не совпадает с бюджетом очков талантов.")
    A("")
    A("Планы класса «Божественный» живут в отдельном древе, их проверяет")
    A("`tools/talent_gen/build_divine_plans.py --check`:")
    A("")
    A("- каждый узел соседствует с уже взятым (иначе `canAllocate` его не даст);")
    A("- в плане нет узлов чужой ветки и двух перков одной группы `one_of_a_kind`;")
    A("- план в `GuideData.java` совпадает с сеткой `divine.json`, и у гайда указано `.tree(\"divine\")`;")
    A("- входной узел доступен с 1 уровня, а узлы вне охвата гайда идут только в конце плана.")
    A("")
    A("`tools/talent_gen/simulate_divine_guide.py` прогоняет логику советника по уровням 1–40")
    A("и сверяет её с правилами `TalentsData.canAllocate`: рекомендация не должна вести на узел,")
    A("который игра отвергнет. Отдельно проверяется выбор другого зверя (медведь или мамонт")
    A("вместо стаи волков) — план при этом доводится до конца.")
    A("")
    A("Дополнительно прогоняются: комментарийно-безопасный разбор скобок и синтаксический")
    A("разбор всех изменённых Java-файлов (tree-sitter, грамматика Java).")
    A("")
    A("## 5. Где это в коде")
    A("")
    A("| Файл | Что делает |")
    A("|---|---|")
    A("| `guides/GuideDefinition.java` | модель гайда: веса статов, путь талантов, план заклинаний |")
    A("| `guides/GuideData.java` | пять готовых гайдов (планы сгенерированы скриптами) |")
    A("| `guides/GuideDefinition.java` → `.tree(id)` | у какого древа лежит план гайда (по умолчанию `talents`) |")
    A("| `aoe_data/database/perks/DivinePerks.java` | перки древа «Божественный» и их `lvl_req` |")
    A("| `resources/data/mmorpg/talent_tree/divine.json` | сетка древа «Божественный» (24 узла) |")
    A("| `tools/talent_gen/build_divine_plans.py` | генератор и проверка планов «Некромант»/«Призыватель» |")
    A("| `tools/talent_gen/simulate_divine_guide.py` | прогон советника по уровням |")
    A("| `guides/GuideMath.java` | арифметика: бюджеты, разбивка статов, проценты защиты, проекция сборки |")
    A("| `guides/GuideAdvisor.java` | подсказки, адаптация и полная сборка на Полигоне |")
    A("| `guides/GuidesScreen.java` | вкладка «Гайды» |")
    A("| `guides/GuidesPacket.java` | пакет выбора/сборки гайда |")
    A("| `gui/screens/character_screen/CharacterScreen.java` | вкладка в хабе + пульсирующая рамка у нужного стата |")
    A("| `gui/screens/skill_tree/buttons/PerkButton.java` | подсветка следующего узла древа |")
    A("| `gui/screens/spell/{LearnSpellButton,LearnSynergyButton}.java` | подсветка следующего заклинания/синергии |")
    A("| `simulation/SimulationScreen.java` | ряд «Собрать гайд / Гайды / Сброс+гайд» |")
    A("| `simulation/SimulationManager.java` | `buildActiveGuide`, `resetBuildAndApplyGuide` |")
    A("| `capability/player/RPGPlayerData.java` | хранение `active_guide` в NBT |")
    A("")
    A("Пересборка планов талантов: `python3 tools/guides/build_guides.py` (основное древо) и")
    A("`python3 tools/talent_gen/build_divine_plans.py` (древо «Божественный»),")
    A("отчёт: `python3 tools/guides/report.py`.")
    A("")

    open(OUT, "w", encoding="utf-8").write("\n".join(L) + "\n")
    print("написано", OUT, len(L), "строк")


if __name__ == "__main__":
    main()
