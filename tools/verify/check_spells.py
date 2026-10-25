#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Аудит заклинаний БЕЗ запуска игры.

Зачем: сборка Gradle в этой среде недоступна, а живые замеры делает
`SpellAuditProbe` в CI. Здесь — быстрая структурная проверка, которую можно
прогнать за секунду после правки контента.

Что проверяет для каждого заклинания:

1. Описание (`.manualDesc(...)`) — без него в тултипе пусто.
2. Иконку `textures/gui/spells/icons/<id>.png` — иначе в дереве школы и на
   хотбаре пустой квадрат.
3. Партиклы — заклинание без визуального отклика не понять, сработало ли оно.
4. Звук — второй канал обратной связи.
5. Функциональный эффект — урон / лечение / наложение эффекта / движение /
   щит / предмет / смена кулдаунов. Заклинание без ничего из этого «не работает».
6. Принадлежность школе (или явное исключение для служебных заклинаний) —
   заклинание вне всех школ игрок не может выучить.
7. Сетку школ: дубликаты координат, выход за границы, точки вне lvl_reqs.

Запуск:
    python3 tools/verify/check_spells.py                # отчёт в консоль
    python3 tools/verify/check_spells.py --markdown r.md --json r.json
    python3 tools/verify/check_spells.py --strict       # падать и на warnings
"""

import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SPELLS_JAVA = os.path.join(ROOT, "src/main/java/com/robertx22/age_of_exile/aoe_data/database/spells")
SCHOOLS_JAVA = os.path.join(
    ROOT, "src/main/java/com/robertx22/age_of_exile/aoe_data/database/spell_schools/SpellSchoolsAdder.java")
TEXTURES = os.path.join(ROOT, "src/main/resources/assets/mmorpg/textures/gui/spells")
REGISTRY_LIST = os.path.join(
    ROOT, "src/main/resources/assets/mmorpg/modpack_dev_helper/spells.txt")

# Служебные/тестовые заклинания: они не обязаны быть в школе, и им не нужна
# иконка для игрока — они выдаются только командами разработчика.
SERVICE_SPELLS = {"test_command", "test_spell", "demon"}

# Маркеры действий в цепочке билдера. Сравнение по тексту цепочки — грубо, но
# для структурного аудита этого достаточно: цепочка всегда лежит в одном файле
# и не содержит вложенных объявлений.
MARKERS = {
    "particles": [
        r"\bParticle\b", r"ParticleTypes\.", r"particleOnTick", r"aoeParticles",
        r"groundParticles", r"groundEdgeParticles", r"swordSweepParticles",
        r"nova\(", r"\.particle\(", r"ParticlesInRadius", r"PARTICLES_IN_RADIUS",
    ],
    "sound": [r"playSound", r"PLAY_SOUND", r"SlashSounds\."],
    "damage": [
        r"damageInAoe", r"damageInFront", r"\bdamage\(", r"dotDamage",
        r"onTickRaycast", r"DEAL_DAMAGE", r"damageInRadius",
    ],
    "heal": [
        r"healInAoe", r"healCaster", r"onTickHeal", r"RESTORE_HEALTH",
        r"SHOOTING_STAR", r"healInRadius",
    ],
    "mana": [r"restoreMana", r"RESTORE_MANA"],
    "effect": [
        r"giveSelfExileEffect", r"giveExileEffectToAlliesInRadius",
        r"addExileEffectToEnemies", r"EXILE_EFFECT", r"POTION\.create",
        r"giveEffectToAlliesInRadius", r"giveSelfEffect", r"giveShield",
        r"GIVE_SHIELD",
    ],
    "movement": [
        r"SET_ADD_MOTION", r"TP_TARGET_TO_SELF", r"TP_CASTER_IN_DIRECTION",
        r"pushCaster", r"teleportForward", r"SUMMON_PROJECTILE",
    ],
    "item": [r"GIVE_ITEM"],
    "aggro": [r"AGGRO"],
    "refresh": [r"REFRESH_COOLDOWNS"],
    "container": [r"OPEN_ENDER_CHEST"],
    "summon": [r"SUMMON_BLOCK", r"SUMMON_EVOKER_FANG", r"SUMMON_LIGHTNING"],
}

FUNCTIONAL = ["damage", "heal", "mana", "effect", "movement", "item", "aggro", "refresh", "container", "summon"]


def read(path):
    with open(path, encoding="utf-8") as handle:
        return handle.read()


def java_files():
    files = []
    for root, _, names in os.walk(SPELLS_JAVA):
        for name in sorted(names):
            if name.endswith(".java"):
                files.append(os.path.join(root, name))
    return files


def strip_comments(text):
    """Убираем // и /* */ комментарии, чтобы закомментированные билдеры не
    попадали в аудит (в StrSpells такой пример есть)."""
    out = []
    i = 0
    n = len(text)
    while i < n:
        if text.startswith("//", i):
            j = text.find("\n", i)
            i = n if j < 0 else j
        elif text.startswith("/*", i):
            j = text.find("*/", i + 2)
            if j < 0:
                i = n
            else:
                # сохраняем переводы строк, чтобы номера строк не съезжали
                out.append("\n" * text.count("\n", i, j))
                i = j + 2
        elif text.startswith('"', i):
            j = i + 1
            while j < n and text[j] != '"':
                if text[j] == "\\":
                    j += 1
                j += 1
            out.append(text[i:j + 1])
            i = j + 1
        else:
            out.append(text[i])
            i += 1
    return "".join(out)


def match_paren(text, start):
    """index закрывающей скобки для открывающей в позиции start."""
    depth = 0
    i = start
    in_str = False
    while i < len(text):
        ch = text[i]
        if in_str:
            if ch == "\\":
                i += 2
                continue
            if ch == '"':
                in_str = False
        else:
            if ch == '"':
                in_str = True
            elif ch == "(":
                depth += 1
            elif ch == ")":
                depth -= 1
                if depth == 0:
                    return i
        i += 1
    return -1


def split_args(text):
    """Аргументы вызова (text — содержимое между скобками)."""
    args = []
    depth = 0
    in_str = False
    current = []
    i = 0
    while i < len(text):
        ch = text[i]
        if in_str:
            current.append(ch)
            if ch == "\\":
                current.append(text[i + 1])
                i += 2
                continue
            if ch == '"':
                in_str = False
        elif ch == '"':
            in_str = True
            current.append(ch)
        elif ch in "([{":
            depth += 1
            current.append(ch)
        elif ch in ")]}":
            depth -= 1
            current.append(ch)
        elif ch == "," and depth == 0:
            args.append("".join(current).strip())
            current = []
        else:
            current.append(ch)
        i += 1
    tail = "".join(current).strip()
    if tail:
        args.append(tail)
    return args


def constants(files):
    """Все статические строковые константы: {class: {name: value}} + глобальный
    индекс {name: [(file, class, value)]}."""
    by_file = {}
    index = {}
    for path in files:
        text = strip_comments(read(path))
        cls = os.path.basename(path)[:-5]
        table = {}
        for m in re.finditer(r"(?:public\s+|private\s+|protected\s+)?static\s+final\s+String\s+(\w+)\s*=\s*\"([^\"]*)\"", text):
            table[m.group(1)] = m.group(2)
        # В этих файлах константы объявлены без final — учитываем оба варианта.
        for m in re.finditer(r"(?:public\s+|private\s+|protected\s+)?static\s+String\s+(\w+)\s*=\s*\"([^\"]*)\"", text):
            table.setdefault(m.group(1), m.group(2))
        by_file[path] = (cls, table)
        for name, value in table.items():
            index.setdefault(name, []).append((path, cls, value))
    return by_file, index


def enclosing_method(text, position):
    """Имя метода и его параметры, внутри которого находится position."""
    head = text[:position]
    # ищем последнее объявление метода до позиции
    pattern = re.compile(r"([\w<>\[\],\.\s]+?)\s+(\w+)\s*\(([^();{}]*)\)\s*\{")
    last = None
    for m in pattern.finditer(head):
        last = m
    if not last:
        return None, []
    params = []
    for raw in last.group(3).split(","):
        raw = raw.strip()
        if not raw:
            continue
        bits = raw.split()
        params.append(bits[-1])
    return last.group(2), params


def builder_chains(text):
    """Все цепочки SpellBuilder.of(...)...build() в файле."""
    chains = []
    for m in re.finditer(r"SpellBuilder\.of\s*\(", text):
        open_paren = text.index("(", m.start())
        close_paren = match_paren(text, open_paren)
        if close_paren < 0:
            continue
        args = split_args(text[open_paren + 1:close_paren])
        # Конец цепочки: либо .build()/.buildForEffect(), либо конец оператора
        # (; на нулевой глубине). Второй случай нужен хелперам вида
        # `static SpellBuilder trap(...) { return SpellBuilder.of(...)...; }`:
        # у них внутри нет .build(), и раньше цепочка обрывалась на заголовке.
        rest = text[close_paren + 1:]
        end_rel = len(rest)
        depth = 0
        in_str = False
        i = 0
        while i < len(rest):
            ch = rest[i]
            if in_str:
                if ch == "\\":
                    i += 2
                    continue
                if ch == '"':
                    in_str = False
            elif ch == '"':
                in_str = True
            elif ch in "([{":
                depth += 1
            elif ch in ")]}":
                if depth == 0:
                    end_rel = i
                    break
                depth -= 1
            elif ch == ";" and depth == 0:
                end_rel = i
                break
            i += 1
        build_match = re.search(r"\.build(ForEffect)?\s*\(\s*\)", rest[:end_rel])
        if build_match:
            end_rel = build_match.end()
        end = close_paren + 1 + end_rel
        chains.append({
            "start": m.start(),
            "id_expr": args[0] if args else "",
            "text": text[m.start():end],
        })
    return chains


def literal(value):
    value = value.strip()
    if value.startswith('"') and value.endswith('"'):
        return value[1:-1]
    return None


def resolve_id(id_expr, path, consts_by_file, const_index):
    value = literal(id_expr)
    if value is not None:
        return value, "literal"
    name = id_expr.strip()
    # Class.CONST
    if "." in name:
        cls, field = name.split(".")[-2], name.split(".")[-1]
        for other_path, (other_cls, table) in consts_by_file.items():
            if other_cls == cls and field in table:
                return table[field], "const"
        return None, "unresolved"
    cls, table = consts_by_file.get(path, ("", {}))
    if name in table:
        return table[name], "const"
    if name in const_index:
        return const_index[name][0][2], "const-global"
    return None, "param"


def statement_end(text, position):
    """Индекс ; (или }), завершающего оператор, начинающийся в position."""
    depth = 0
    in_str = False
    i = position
    while i < len(text):
        ch = text[i]
        if in_str:
            if ch == "\\":
                i += 2
                continue
            if ch == '"':
                in_str = False
        elif ch == '"':
            in_str = True
        elif ch in "([{":
            depth += 1
        elif ch in ")]}":
            if depth == 0:
                return i
            depth -= 1
        elif ch == ";" and depth == 0:
            return i
        i += 1
    return len(text)


def resolve_helper_call(path, text, method_name, param_name, consts_by_file, const_index):
    """Для заклинаний, собранных хелпером (trap/curse/song/of): ищем вызовы
    хелпера и берём литерал, который попадает в позицию param_name.

    Возвращает (id, как_разрешён, текст_вызова): текст вызова нужен, потому что
    часть цепочки (например .manualDesc) дописывают уже на месте вызова."""
    results = []
    params = []
    # находим объявление хелпера, чтобы знать индекс параметра
    for m in re.finditer(r"([\w<>\[\],\.\s]+?)\s+" + re.escape(method_name) + r"\s*\(([^();{}]*)\)\s*\{", text):
        params = [p.strip().split()[-1] for p in m.group(2).split(",") if p.strip()]
        break
    if param_name not in params:
        return results
    index = params.index(param_name)
    for m in re.finditer(r"(?<![\w.])" + re.escape(method_name) + r"\s*\(", text):
        open_paren = m.end() - 1
        close_paren = match_paren(text, open_paren)
        if close_paren < 0:
            continue
        if close_paren > len(text):
            continue
        args = split_args(text[open_paren + 1:close_paren])
        if len(args) <= index:
            continue
        value, how = resolve_id(args[index], path, consts_by_file, const_index)
        if value:
            call = text[m.start():statement_end(text, m.start())]
            results.append((value, how, call))
    return results


def classify(chain_text):
    found = {}
    for kind, patterns in MARKERS.items():
        found[kind] = 0
        for pattern in patterns:
            found[kind] += len(re.findall(pattern, chain_text))
    return found


def icon_exists(spell_id):
    for folder in ("", "unused"):
        path = os.path.join(TEXTURES, "icons", folder, spell_id + ".png") if folder else \
            os.path.join(TEXTURES, "icons", spell_id + ".png")
        if os.path.exists(path):
            return True
    return False


def collect_spells():
    files = java_files()
    consts_by_file, const_index = constants(files)
    spells = {}
    unresolved = []

    for path in files:
        text = strip_comments(read(path))
        for chain in builder_chains(text):
            spell_id, how = resolve_id(chain["id_expr"], path, consts_by_file, const_index)
            if spell_id is None and how == "param":
                method, params = enclosing_method(text, chain["start"])
                if method and params:
                    # какой из параметров улетел в id
                    param = chain["id_expr"].strip()
                    for value, _, call_text in resolve_helper_call(path, text, method, param,
                                                                   consts_by_file, const_index):
                        add(spells, value, chain["text"] + "\n" + call_text, path, "helper:" + method)
                    continue
                unresolved.append((os.path.basename(path), chain["id_expr"]))
                continue
            if spell_id is None:
                unresolved.append((os.path.basename(path), chain["id_expr"]))
                continue
            add(spells, spell_id, chain["text"], path, how)

    return spells, unresolved


def add(spells, spell_id, chain_text, path, how):
    entry = spells.setdefault(spell_id, {
        "id": spell_id,
        "file": os.path.relpath(path, ROOT),
        "how": how,
        "chains": [],
        "channels": {k: 0 for k in MARKERS},
        "chains_text": "",
    })
    entry["chains"].append(how)
    entry["chains_text"] += "\n" + chain_text
    found = classify(chain_text)
    for kind, count in found.items():
        entry["channels"][kind] += count


def collect_schools():
    """Школы: {school: [(spell/synergy, x, y, kind)]}."""
    path = SCHOOLS_JAVA
    text = strip_comments(read(path))
    consts_by_file, const_index = constants(java_files())

    school_consts = {}
    for m in re.finditer(r"public\s+static\s+String\s+(\w+)\s*=\s*\"([^\"]*)\"", text):
        school_consts[m.group(1)] = m.group(2)

    schools = {}
    current = None
    for line in text.split("\n"):
        m = re.search(r"SchoolBuilder\.of\(\s*(\w+|\"[^\"]*\")", line)
        if m:
            raw = m.group(1)
            current = school_consts.get(raw) or literal(raw)
            schools.setdefault(current, [])
            continue
        m = re.search(r"\.add(Spell|Synergy)\(\s*([\w\.]+|\"[^\"]*\")\s*,\s*new\s+PointData\(\s*(-?\d+)\s*,\s*(-?\d+)\s*\)", line)
        if m and current:
            kind = m.group(1).lower()
            ref = m.group(2)
            if ref.startswith('"'):
                # В дереве школы допустим и строковый литерал: часть умений из impl/*
                # не имеет константы с id.
                spell_id = ref.strip('"')
            else:
                spell_id, _ = resolve_id(ref, path, consts_by_file, const_index)
            schools[current].append({
                "kind": kind,
                "ref": ref,
                "id": spell_id,
                "x": int(m.group(3)),
                "y": int(m.group(4)),
            })
    return schools


def registry_ids():
    if not os.path.exists(REGISTRY_LIST):
        return None
    lines = [l.strip() for l in read(REGISTRY_LIST).split("\n")]
    skip = {"These are all default registry entries of this type.",
            "This registry is modify-able through datapacks."}
    return {l for l in lines if l and l not in skip}


def audit():
    spells, unresolved = collect_spells()
    schools = collect_schools()
    registry = registry_ids()

    in_school = {}
    for school, nodes in schools.items():
        for node in nodes:
            if node["kind"] != "spell" or not node["id"]:
                continue
            in_school.setdefault(node["id"], []).append(school)

    errors, warnings = [], []
    rows = []

    for spell_id in sorted(spells):
        entry = spells[spell_id]
        channels = entry["channels"]
        service = spell_id in SERVICE_SPELLS
        row = {
            "id": spell_id,
            "file": entry["file"],
            "schools": sorted(in_school.get(spell_id, [])),
            "channels": {k: channels[k] for k in channels if channels[k]},
            "problems": [],
            "warnings": [],
        }
        functional = [k for k in FUNCTIONAL if channels[k] > 0]
        if not functional:
            row["problems"].append("ничего не делает (нет урона/лечения/эффекта/движения)")
        if channels["particles"] == 0:
            row["problems"].append("нет партиклов")
        if channels["sound"] == 0:
            row["warnings"].append("нет звука")
        if "manualDesc" not in entry["chains_text"]:
            row["problems"].append("нет описания (manualDesc)")
        if not icon_exists(spell_id):
            row["problems"].append("нет иконки " + spell_id + ".png")
        if not service and spell_id not in in_school:
            row["problems"].append("не входит ни в одну школу — игрок не может выучить")
        if len(row["schools"]) > 1:
            row["warnings"].append("в нескольких школах: " + ", ".join(row["schools"]))

        for problem in row["problems"]:
            errors.append("%s: %s" % (spell_id, problem))
        for warning in row["warnings"]:
            warnings.append("%s: %s" % (spell_id, warning))
        rows.append(row)

    # школа ссылается на заклинание, которого нет в аудите
    for school, nodes in schools.items():
        seen = {}
        for node in nodes:
            key = (node["x"], node["y"])
            if key in seen:
                errors.append("%s: координата (%d,%d) занята дважды: %s и %s"
                              % (school, node["x"], node["y"], seen[key], node["id"] or node["ref"]))
            seen[key] = node["id"] or node["ref"]
            if node["kind"] != "spell" or not node["id"]:
                continue
            if node["id"] not in spells:
                errors.append("%s: в сетке неизвестное заклинание %s" % (school, node["id"]))
            if node["x"] < 0 or node["y"] < 0:
                errors.append("%s: отрицательная координата у %s" % (school, node["id"]))
            if node["y"] > 6:
                errors.append("%s: y=%d вне lvl_reqs (0..6) у %s" % (school, node["y"], node["id"]))
            if node["x"] > 10:
                errors.append("%s: x=%d вне сетки (0..10) у %s" % (school, node["x"], node["id"]))

    for file_name, expr in unresolved:
        warnings.append("%s: не удалось разобрать id билдера %s" % (file_name, expr))

    if registry is not None:
        for spell_id in sorted(set(spells) - registry):
            warnings.append("%s: есть в коде, но нет в реестре (modpack_dev_helper/spells.txt) — "
                            "список мог устареть" % spell_id)
        for spell_id in sorted(registry - set(spells)):
            warnings.append("%s: есть в реестре, но не найдено в исходниках заклинаний" % spell_id)

    return {
        "spells": rows,
        "schools": schools,
        "errors": errors,
        "warnings": warnings,
    }


def to_markdown(result):
    lines = []
    rows = result["spells"]
    lines.append("# Аудит заклинаний (структурный)")
    lines.append("")
    lines.append("Заклинаний: **%d**, школ: **%d**, ошибок: **%d**, предупреждений: **%d**"
                 % (len(rows), len(result["schools"]), len(result["errors"]), len(result["warnings"])))
    lines.append("")
    if result["errors"]:
        lines.append("## Ошибки")
        lines.append("")
        for error in result["errors"]:
            lines.append("- " + error)
        lines.append("")
    if result["warnings"]:
        lines.append("## Предупреждения")
        lines.append("")
        for warning in result["warnings"]:
            lines.append("- " + warning)
        lines.append("")

    lines.append("## По школам")
    lines.append("")
    for school in sorted(result["schools"]):
        nodes = [n for n in result["schools"][school] if n["kind"] == "spell"]
        lines.append("### %s (%d)" % (school, len(nodes)))
        lines.append("")
        lines.append("| заклинание | урон | лечение | эффект | движение | партиклы | звук |")
        lines.append("|---|---|---|---|---|---|---|")
        by_id = {r["id"]: r for r in rows}
        for node in sorted(nodes, key=lambda n: (n["y"], n["x"])):
            row = by_id.get(node["id"] or "", {})
            channels = row.get("channels", {})

            def mark(kind):
                return "да" if channels.get(kind, 0) else "—"

            lines.append("| `%s` | %s | %s | %s | %s | %s | %s |" % (
                node["id"] or node["ref"], mark("damage"), mark("heal") + "/" + mark("mana"),
                mark("effect"), mark("movement"), mark("particles"), mark("sound")))
        lines.append("")

    lines.append("## Вне школ")
    lines.append("")
    # Служебные умения (test_command и т.п.) намеренно не в дереве школ.
    outside = [r["id"] for r in rows if not r["schools"] and r["id"] not in SERVICE_SPELLS]
    lines.append(", ".join("`%s`" % x for x in outside) if outside else "нет")
    lines.append("")
    return "\n".join(lines)


def main():
    result = audit()
    strict = "--strict" in sys.argv
    markdown_path = None
    json_path = None
    for i, arg in enumerate(sys.argv):
        if arg == "--markdown" and i + 1 < len(sys.argv):
            markdown_path = sys.argv[i + 1]
        if arg == "--json" and i + 1 < len(sys.argv):
            json_path = sys.argv[i + 1]

    markdown = to_markdown(result)
    if markdown_path:
        with open(markdown_path, "w", encoding="utf-8") as handle:
            handle.write(markdown + "\n")
        print("markdown: " + markdown_path)
    else:
        print(markdown)

    if json_path:
        with open(json_path, "w", encoding="utf-8") as handle:
            json.dump(result, handle, ensure_ascii=False, indent=2, default=str)
        print("json: " + json_path)

    for error in result["errors"]:
        print("ERROR: " + error)
    for warning in result["warnings"]:
        print("WARN: " + warning)

    print("\nзаклинаний=%d ошибок=%d предупреждений=%d"
          % (len(result["spells"]), len(result["errors"]), len(result["warnings"])))

    if result["errors"] or (strict and result["warnings"]):
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
