#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Аудит профессий ремесла БЕЗ запуска игры.

Зачем: сборка Gradle в этой среде недоступна, а живые замеры делает
`ProfessionAuditProbe` в CI. Здесь — быстрая структурная проверка, которую можно
прогнать за секунду после правки контента.

Что проверяется для каждой профессии (alchemy, inscribing, cooking,
armor_craft, wep_craft, jewel_craft):

1. Инструмент: зарегистрирован в `CraftToolItems` как `CraftToolItem` и умеет
   открывать меню крафта (рецепт крафта инструмента есть у всех — проверяем
   сам класс).
2. Ингредиенты: хотя бы 3 ингредиента разрешены в профессии
   (`allowedIn(PlayerSkillEnum.X)`), и среди них есть хотя бы 2 обычных
   (без «one of a kind») — иначе сценарий аудита не соберёт рецепт.
3. Результат: у расходных профессий есть классы предметов (зелья/свитки/еда),
   у гир-профессий — сопоставление слота (`gearMatchesProfession`).
4. Локализация: `Words.X` и `Words.XDesc` на месте (без них в тултипах пусто).
5. Сетка крафта 3×2: контейнер по-прежнему создаёт `TransientCraftingContainer(this, 3, 2)`
   и показывает причину отказа (`getFailReason`) — это то, что тестирует живой прогон.
6. Регрессия главного фикса: в `ProfCraftingRecipe` обязана остаться проверка
   `hasRealGearStats` («настоящие статы», а не просто наличие GEAR_ITEM_DATA).
   Её исчезновение вернёт баг, при котором все три гир-профессии не могли
   крафтить вообще.
7. Инфраструктура аудита: зонд, сценарий с мышкой и workflow на месте.
8. Каждый `allowedIn(PlayerSkillEnum.X)` указывает на существующую профессию.

Запуск:
    python3 tools/verify/check_professions.py                # отчёт в консоль
    python3 tools/verify/check_professions.py --markdown r.md --json r.json
    python3 tools/verify/check_professions.py --strict       # падать и на warnings
"""

import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
JAVA = os.path.join(ROOT, "src/main/java/com/robertx22/age_of_exile")
ENUM = os.path.join(JAVA, "saveclasses/player_skills/PlayerSkillEnum.java")
TOOLS = os.path.join(JAVA, "mmorpg/registers/common/items/CraftToolItems.java")
INGREDIENT_ADDERS = os.path.join(JAVA, "aoe_data/database/ingredient")
WORDS = os.path.join(JAVA, "uncommon/localization/Words.java")
CONTAINER = os.path.join(JAVA, "player_skills/crafting_inv/ProfCraftContainer.java")
RECIPE = os.path.join(JAVA, "player_skills/ingredient/ProfCraftingRecipe.java")
CONSUMABLE_ITEMS = os.path.join(JAVA, "mmorpg/registers/common/items/CraftedConsumableItems.java")
PROBE = os.path.join(JAVA, "uncommon/testing/ProfessionAuditProbe.java")
SCENARIO = os.path.join(ROOT, "tools/e2e/scenarios/professions.txt")
WORKFLOW = os.path.join(ROOT, ".github/workflows/profession-audit.yml")
STRUCTURE_OK_MARKER = "TransientCraftingContainer(this, 3, 2)"

# Расходные профессии и где лежат их результирующие предметы.
CONSUMABLES = {
    "alchemy": ("POTIONS", "AlchemyConsumableItem"),
    "inscribing": ("SCROLLS", "InscribingConsumableItem"),
    "cooking": ("FOODS", "CookingConsumableItem"),
}
GEAR_PROFS = {"armor_craft", "wep_craft", "jewel_craft"}


def read(path):
    try:
        with open(path, encoding="utf-8") as handle:
            return handle.read()
    except OSError:
        return ""


def find_professions():
    """[(id, enum_name, is_gear)] из PlayerSkillEnum.java."""
    text = read(ENUM)
    profs = []
    # ALCHEMY("alchemy", ...) { ... } — имя enum + строковый id.
    for match in re.finditer(r"\b([A-Z_]{3,})\(\"([a-z_]+)\"", text):
        name, pid = match.group(1), match.group(2)
        if pid in ("all", "none"):
            continue
        profs.append((pid, name))
    return profs


def count_allowed_in(enum_name):
    """Сколько `allowedIn(PlayerSkillEnum.NAME)` в билдерах ингредиентов."""
    total = 0
    regular = 0
    for fname in os.listdir(INGREDIENT_ADDERS) if os.path.isdir(INGREDIENT_ADDERS) else []:
        if not fname.endswith(".java"):
            continue
        text = read(os.path.join(INGREDIENT_ADDERS, fname))
        for block in re.finditer(
            r"IngredientBuilder\.of\((.*?)\)\s*(.*?)\.build\(\);", text, re.S
        ):
            body = block.group(2)
            if ("PlayerSkillEnum." + enum_name + ")") in body:
                total += 1
                if "setIsArtifact" not in body:
                    regular += 1
    return total, regular


def main():
    markdown_path = None
    json_path = None
    strict = False
    args = sys.argv[1:]
    for i, arg in enumerate(args):
        if arg == "--markdown" and i + 1 < len(args):
            markdown_path = args[i + 1]
        elif arg == "--json" and i + 1 < len(args):
            json_path = args[i + 1]
        elif arg == "--strict":
            strict = True

    problems = []      # (prof, check, detail) — жёсткие ошибки
    warnings = []      # (prof, check, detail)
    rows = []

    profs = find_professions()
    enum_text = read(ENUM)
    tools_text = read(TOOLS)
    words_text = read(WORDS)
    container_text = read(CONTAINER)
    recipe_text = read(RECIPE)
    consumable_text = read(CONSUMABLE_ITEMS)

    if len(profs) != 6:
        problems.append(("все", "professions_found",
                         "ожидалось 6 профессий, найдено %d" % len(profs)))

    # --- сквозные проверки инфраструктуры -----------------------------------
    if "hasRealGearStats" not in recipe_text:
        problems.append(("все", "gear_gate_fix_present",
                         "в ProfCraftingRecipe нет hasRealGearStats — вернулся баг, "
                         "при котором гир-профессии отклоняли любую основу "
                         "(«The used gear has stats already»)"))
    if STRUCTURE_OK_MARKER not in container_text:
        problems.append(("все", "grid_is_3x2",
                         "ProfCraftContainer больше не создаёт сетку 3×2"))
    if "getFailReason" not in container_text:
        problems.append(("все", "fail_reason_ui",
                         "контейнер не показывает причину отказа"))
    for path, name in ((PROBE, "зонд ProfessionAuditProbe.java"),
                       (SCENARIO, "сценарий professions.txt"),
                       (WORKFLOW, "workflow profession-audit.yml")):
        if not os.path.exists(path):
            problems.append(("все", "audit_infra", "нет файла: %s" % name))

    # Сценарий обязан снимать скриншот после КАЖДОГО перемещения в сетку:
    # число кликов по слотам сетки (673/691/709) должно совпадать с числом
    # имён скриншотов *-slot-*.
    scenario_text = read(SCENARIO)
    if scenario_text:
        grid_clicks = len(re.findall(
            r"^click (?:673|691|709) (?:275|293)$", scenario_text, re.M))
        slot_shots = len(re.findall(r"^shot \S+-slot-\d+$", scenario_text, re.M))
        if grid_clicks != slot_shots or slot_shots == 0:
            problems.append(("все", "shot_per_grid_move",
                             "кликов по слотам сетки: %d, скриншотов *-slot-*: %d — "
                             "каждое перемещение в сетку 3×2 обязано сниматься"
                             % (grid_clicks, slot_shots)))

    # Каждый allowedIn(...) указывает на существующую профессию.
    known = {pid for pid, _ in profs}
    for fname in os.listdir(INGREDIENT_ADDERS) if os.path.isdir(INGREDIENT_ADDERS) else []:
        if not fname.endswith(".java"):
            continue
        text = read(os.path.join(INGREDIENT_ADDERS, fname))
        for match in re.finditer(r"allowedIn\(PlayerSkillEnum\.([A-Z_]+)\)", text):
            name = match.group(1)
            ids = {p[1]: p[0] for p in profs}
            if name not in ids and name not in ("ALL", "NONE"):
                problems.append((fname, "allowed_in_unknown",
                                 "PlayerSkillEnum.%s не является профессией" % name))

    # --- по каждой профессии -----------------------------------------------
    for pid, enum_name in profs:
        row = {"id": pid, "checks": []}

        def record(check, ok, detail, soft=False):
            row["checks"].append({"name": check, "ok": ok, "detail": detail})
            if not ok:
                (warnings if soft else problems).append((pid, check, detail))

        # 1. инструмент
        tool_ok = ("PlayerSkillEnum." + enum_name) in tools_text and "CraftToolItem" in tools_text
        record("tool_registered", tool_ok,
               "CraftToolItems: " + ("есть" if tool_ok else "нет"))

        # 2. ингредиенты
        total, regular = count_allowed_in(enum_name)
        record("ingredients_allowed", total >= 3,
               "разрешено ингредиентов: %d (обычных %d)" % (total, regular))
        record("regular_ingredients", regular >= 2,
               "обычных ингредиентов: %d (нужно >= 2 для рецепта аудита)" % regular,
               soft=True)

        # 3. результат
        if pid in CONSUMABLES:
            list_name, item_class = CONSUMABLES[pid]
            ok = list_name in consumable_text and item_class in read(
                os.path.join(JAVA, "player_skills/ingredient/items/%s.java" % item_class))
            record("result_items", ok, item_class)
        else:
            ok = enum_name in enum_text and "gearMatchesProfession" in enum_text
            record("gear_mapping", ok, "gearMatchesProfession в PlayerSkillEnum")

        # 4. локализация: реальные имена берём из PlayerSkillEnum
        #    (ALCHEMY("alchemy", null, Words.Alchemy, Words.AlchemyDesc, ...)
        words_refs = re.findall(
            r"\b%s\(\"[a-z_]+\"[^)]*Words\.([A-Za-z_]+)" % enum_name, enum_text)
        for ref in words_refs:
            record("words_%s" % ref, re.search(r"\b%s\(" % ref, words_text) is not None,
                   "Words.%s" % ref)
        if not words_refs:
            record("words_name", False, "не найдены ссылки Words.* в %s" % enum_name)

        rows.append(row)

    # --- отчёт ---------------------------------------------------------------
    failed = len(problems)
    warn_count = len(warnings)
    summary = "проверено профессий: %d, ошибок: %d, предупреждений: %d" % (
        len(profs), failed, warn_count)
    print(summary)
    for prof, check, detail in problems:
        print("  FAIL %-14s %-24s %s" % (prof, check, detail))
    for prof, check, detail in warnings:
        print("  WARN %-14s %-24s %s" % (prof, check, detail))

    if markdown_path:
        lines = ["# Структурный аудит профессий (без игры)", "",
                 "Проверено профессий: **%d**, ошибок: **%d**, предупреждений: **%d**."
                 % (len(profs), failed, warn_count), ""]
        lines.append("| профессия | проверка | итог | детали |")
        lines.append("|---|---|---|---|")
        for row in rows:
            for c in row["checks"]:
                lines.append("| `%s` | `%s` | %s | %s |" % (
                    row["id"], c["name"], "OK" if c["ok"] else "**FAIL**", c["detail"]))
        for prof, check, detail in problems:
            lines.append("| `%s` | `%s` | **FAIL** | %s |" % (prof, check, detail))
        for prof, check, detail in warnings:
            lines.append("| `%s` | `%s` | warn | %s |" % (prof, check, detail))
        lines.append("")
        os.makedirs(os.path.dirname(markdown_path) or ".", exist_ok=True)
        with open(markdown_path, "w", encoding="utf-8") as handle:
            handle.write("\n".join(lines) + "\n")

    if json_path:
        os.makedirs(os.path.dirname(json_path) or ".", exist_ok=True)
        with open(json_path, "w", encoding="utf-8") as handle:
            json.dump({
                "professions": rows,
                "problems": [{"prof": p, "check": c, "detail": d} for p, c, d in problems],
                "warnings": [{"prof": p, "check": c, "detail": d} for p, c, d in warnings],
            }, handle, ensure_ascii=False, indent=2)

    if failed:
        return 1
    if strict and warn_count:
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
