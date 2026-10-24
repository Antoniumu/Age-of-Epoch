#!/usr/bin/env python3
"""Проверка внутриигровой вики без запуска игры.

Что проверяется:

1. Манифест `assets/mmorpg/wiki/index.txt` разбирается, страницы на месте,
   лишних .md-файлов нет, `# home` существует.
2. У каждой страницы есть `@title`, `@en`, `@category`, а категория объявлена в манифесте.
3. Текст страницы без разметки — не короче 1000 символов (критерий заказчика).
4. Все ссылки `[[id]]` и `[текст](id)` ведут на существующие страницы.
5. Нет «сирот»: каждая страница либо ссылается, либо на неё ссылаются.
6. Каждый зарегистрированный предмет мода имеет страницу: самоцветы (9 типов × 7 рангов),
   руны (15), валюта (17), ингредиенты (35), инструменты (6), улучшения рюкзака (14),
   зелья (10), крафтовые расходники (13), эссенции и камни (20).
7. Страница не ссылается на несуществующий id предмета (ловим опечатки в `@item`).

Запуск:  python3 tools/wiki/check_wiki.py
Код возврата 0 — всё в порядке, 1 — есть ошибки.
"""

import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
WIKI = os.path.join(ROOT, "src", "main", "resources", "assets", "mmorpg", "wiki")
PAGES_DIR = os.path.join(WIKI, "ru")
JAVA = os.path.join(ROOT, "src", "main", "java")
MOD = "mmorpg"

errors = []
warnings = []
MIN_CHARS = 1000


def fail(message):
    errors.append(message)


def warn(message):
    warnings.append(message)


def read(path):
    with open(path, encoding="utf-8") as handle:
        return handle.read()


def java(*parts):
    return read(os.path.join(JAVA, *parts))


def plain_text(body):
    text = re.sub(r"!\[[^\]]*\]\([^)]*\)", "", body)
    text = re.sub(r"\[\[([^\]|]+)\|([^\]]+)\]\]", r"\2", text)
    text = re.sub(r"\[\[([^\]]+)\]\]", r"\1", text)
    text = re.sub(r"\[([^\]]+)\]\([^)]*\)", r"\1", text)
    text = text.replace("`", "").replace("**", "").replace("|", " ").replace("#", "")
    text = text.replace("---", " ").replace("*", "")
    return re.sub(r"\s+", " ", text).strip()


def parse_page(path):
    raw = read(path).replace("\r", "")
    meta = {"item": []}
    lines = raw.split("\n")
    index = 0
    while index < len(lines):
        line = lines[index].strip()
        if not line:
            index += 1
            break
        if not line.startswith("@"):
            break
        body = line[1:].strip()
        key, _, value = body.partition(" ")
        key = key.strip().lower()
        value = value.strip()
        if key == "item":
            meta["item"].append(value.lower())
        else:
            meta[key] = value
        index += 1
    body = "\n".join(lines[index:])
    return meta, body


# ── список реально зарегистрированных предметов ──────────────────────────────

def registered_items():
    """Возвращает (обязательные для каталога, допустимые) id предметов."""
    required = {}
    optional = {}

    gem_item = java("com", "robertx22", "age_of_exile", "vanilla_mc", "items", "gemrunes", "GemItem.java")
    gem_ids = re.findall(r'^\s+([A-Z_]+)\("([a-z]+)",\s*"[A-Za-z ]+",', gem_item, re.M)
    rank_names = re.findall(r'^\s+[A-Z]+\(\"([A-Z][a-z]+)\"', gem_item, re.M)
    ranks = sum(1 for name in rank_names if name in (
        "Cracked", "Chipped", "Flawed", "Regular", "Grand", "Glorious", "Divine"))
    for _, gem_id in gem_ids:
        for tier in range(max(ranks, 7)):
            required[f"{MOD}:gems/{gem_id}/{tier}"] = "самоцвет"

    rune_item = java("com", "robertx22", "age_of_exile", "vanilla_mc", "items", "gemrunes", "RuneItem.java")
    for rune_id in re.findall(r'^\s+[A-Z]+\(\d+,\s*"([a-z]+)"', rune_item, re.M):
        required[f"{MOD}:runes/{rune_id}"] = "руна"

    currency = java("com", "robertx22", "age_of_exile", "mmorpg", "registers", "common", "items",
                    "CurrencyItems.java")
    for path in re.findall(r'Def\.item\("(currency/[a-z_0-9]+)"', currency):
        required[f"{MOD}:{path}"] = "валюта"

    ingredients = java("com", "robertx22", "age_of_exile", "mmorpg", "registers", "common", "items",
                       "IngredientItems.java")
    for ing_id in re.findall(r'=\s*of\("([a-z_0-9]+)"', ingredients):
        required[f"{MOD}:ingredient/{ing_id}"] = "ингредиент"

    skills = read(os.path.join(JAVA, "com", "robertx22", "age_of_exile", "saveclasses", "player_skills",
                               "PlayerSkillEnum.java"))
    enum_ids = dict(re.findall(r'^\s+([A-Z_]+)\("([a-z_]+)"', skills, re.M))
    tools = java("com", "robertx22", "age_of_exile", "mmorpg", "registers", "common", "items",
                 "CraftToolItems.java")
    for const in re.findall(r'Def\.item\("craft_tools/" \+ PlayerSkillEnum\.([A-Z_]+)\.id', tools):
        item_id = enum_ids.get(const)
        if item_id:
            required[f"{MOD}:craft_tools/{item_id}"] = "инструмент"

    backpack = java("com", "robertx22", "age_of_exile", "mmorpg", "registers", "common", "items",
                    "BackpackItems.java")
    required[f"{MOD}:backpack/normal"] = "рюкзак"
    for match in re.finditer(r'upgrade\(BackpackUpgrade\.[A-Za-z_0-9]+,\s*SkillItemTier\.TIER(\d),\s*"([a-z_]+)"',
                             backpack):
        required[f"{MOD}:backpack/upgrade/{match.group(2)}{match.group(1)}"] = "улучшение рюкзака"
    for tier in range(5):
        required[f"{MOD}:backpack/upgrade/size{tier}"] = "улучшение рюкзака"

    potion_types = java("com", "robertx22", "age_of_exile", "player_skills", "items", "alchemy",
                        "PotionType.java")
    tiers = len(re.findall(r'^\s+[A-Z_]+\("', read(os.path.join(JAVA, "com", "robertx22", "age_of_exile",
                                                               "player_skills", "items", "foods",
                                                               "SkillItemTier.java")), re.M))
    for potion_id in re.findall(r'\([A-Za-z ]*"[A-Za-z ]+",\s*"([a-z]+)",\s*ResourceType', potion_types):
        for tier in range(max(tiers, 5)):
            required[f"{MOD}:alchemy/potion/{potion_id}/{tier}"] = "зелье"

    consumables = java("com", "robertx22", "age_of_exile", "mmorpg", "registers", "common", "items",
                       "CraftedConsumableItems.java")
    counts = {name: int(value) for name, value in
              re.findall(r"int ([A-Z_]+)_COUNT = (\d+)", consumables)}
    for name, skill in (("FOOD", "cooking"), ("POTION", "alchemy"), ("SCROLL", "inscribing")):
        for number in range(1, counts.get(name, 0) + 1):
            required[f"{MOD}:craft_result/{skill}/{number}"] = "расходник"

    for tier in range(5):
        for template in ("mat/smelted/{}", "mat/salvage/salvage{}", "mat/condensed_salvage/c{}",
                         "stone/{}"):
            required[f"{MOD}:{template.format(tier)}"] = "эссенция или камень"

    # Предметы-носители снаряжения и станции не входят в каталог как отдельные страницы,
    # но страница может на них ссылаться (например, для витрины униката).
    for material in ("diamond", "gold", "iron", "wood"):
        optional[f"{MOD}:weapon/staff/{material}"] = "посох"
    for material in ("diamond", "gold", "iron"):
        optional[f"{MOD}:jewelry/ring/{material}"] = "кольцо"
        optional[f"{MOD}:jewelry/necklace/{material}"] = "ожерелье"
    optional[f"{MOD}:runeword_station"] = "станция рунных слов"

    return required, optional


# ── разбор манифеста ─────────────────────────────────────────────────────────

def parse_manifest():
    path = os.path.join(WIKI, "index.txt")
    if not os.path.exists(path):
        fail("нет файла index.txt")
        return [], {}, ""
    categories = []
    current = None
    home = ""
    for raw in read(path).replace("\r", "").split("\n"):
        line = raw.strip()
        if not line or line.startswith(";;"):
            continue
        if line.startswith("# category"):
            parts = line[len("# category"):].strip().split(" ", 1)
            current = {"id": parts[0], "title": parts[1] if len(parts) > 1 else parts[0], "pages": []}
            categories.append(current)
            continue
        if line.startswith("# home"):
            home = line[len("# home"):].strip()
            continue
        if line.startswith("#"):
            continue
        if current is None:
            fail("страница до объявления категории: " + line)
            continue
        if any(page["id"] == line for category in categories for page in category["pages"]):
            fail("дубликат страницы в манифесте: " + line)
        current["pages"].append({"id": line})
    return categories, {}, home


def main():
    required, optional = registered_items()
    registered = {**optional, **required}
    categories, _, home = parse_manifest()

    listed = {}
    for category in categories:
        if len(category["pages"]) < 2:
            fail(f"категория {category['id']}: меньше двух страниц")
        for page in category["pages"]:
            page["category"] = category["id"]
            listed[page["id"]] = page

    if not listed:
        fail("в манифесте нет ни одной страницы")

    if home and home not in listed:
        fail(f"# home указывает на несуществующую страницу: {home}")

    # файлы на диске
    on_disk = set()
    for root, _dirs, files in os.walk(PAGES_DIR):
        for name in files:
            if name.endswith(".md"):
                relative = os.path.relpath(os.path.join(root, name), PAGES_DIR)
                on_disk.add(relative[:-3].replace(os.sep, "/"))

    for page_id in sorted(listed):
        if page_id not in on_disk:
            fail(f"нет файла ru/{page_id}.md")
    for page_id in sorted(on_disk - set(listed)):
        fail(f"файл ru/{page_id}.md не указан в index.txt")

    # содержимое страниц
    links = {}
    declared = {}
    sizes = {}
    for page_id in sorted(listed):
        path = os.path.join(PAGES_DIR, *page_id.split("/")) + ".md"
        if not os.path.exists(path):
            continue
        meta, body = parse_page(path)
        for key in ("title", "en", "category"):
            if not meta.get(key):
                fail(f"{page_id}: нет @{key}")
        if meta.get("category") and meta["category"] != listed[page_id]["category"]:
            fail(f"{page_id}: @category={meta['category']} не совпадает с категорией в манифесте "
                 f"({listed[page_id]['category']})")
        chars = len(plain_text(body))
        sizes[page_id] = chars
        if chars < MIN_CHARS:
            fail(f"{page_id}: текста {chars} символов, нужно минимум {MIN_CHARS}")
        page_links = set()
        for target in re.findall(r"\[\[([^\]|]+)", body):
            page_links.add(Wiki_normalize(target))
        for target in re.findall(r"\[[^\]]+\]\(([^)]+)\)", body):
            page_links.add(Wiki_normalize(target))
        links[page_id] = page_links
        declared[page_id] = meta["item"]

    # ссылки
    for page_id, targets in links.items():
        for target in targets:
            if target not in listed:
                fail(f"{page_id}: ссылка на несуществующую страницу '{target}'")
        if len(targets) < 2:
            fail(f"{page_id}: меньше двух исходящих ссылок ({len(targets)})")
        tags = [t.strip().lower() for t in meta.get("tags", "").split(",") if t.strip()]
        if len(tags) != len(set(tags)):
            warn(f"{page_id}: повторяющиеся @tags")

    incoming = {page_id: 0 for page_id in listed}
    for page_id, targets in links.items():
        for target in targets:
            if target in incoming and target != page_id:
                incoming[target] += 1
    for page_id, count in incoming.items():
        if count == 0 and page_id != home:
            fail(f"{page_id}: на страницу нигде нет ссылок (сирота)")

    # покрытие предметов
    covered = set()
    for page_id, items in declared.items():
        for item in items:
            if not re.match(r"^[a-z_]+:[a-z_0-9/]+$", item):
                fail(f"{page_id}: кривой id предмета '{item}'")
                continue
            if item not in registered:
                fail(f"{page_id}: предмета '{item}' нет в реестре мода")
            covered.add(item)

    owners = {}
    for page_id, items in declared.items():
        for item in items:
            if item in required:
                owners.setdefault(item, []).append(page_id)
    for item, pages in owners.items():
        if len(pages) > 2:
            warn(f"предмет {item} указан на {len(pages)} страницах: " + ", ".join(sorted(pages)))
    for page_id, items in declared.items():
        if len(items) != len(set(items)):
            fail(f"{page_id}: один и тот же @item указан дважды")

    missing = sorted(set(required) - covered)
    if missing:
        for item in missing[:40]:
            fail(f"нет страницы для предмета {item} ({required[item]})")
        if len(missing) > 40:
            fail(f"... и ещё {len(missing) - 40} предметов без страниц")

    # отчёт
    print(f"страниц в манифесте: {len(listed)}")
    print(f"файлов .md на диске: {len(on_disk)}")
    print(f"предметов в каталоге: {len(required)}")
    if sizes:
        shortest = min(sizes.items(), key=lambda kv: kv[1])
        longest = max(sizes.items(), key=lambda kv: kv[1])
        print(f"символов текста: {sum(sizes.values())} (в среднем {sum(sizes.values()) // len(sizes)})")
        print(f"минимум: {shortest[1]} — {shortest[0]}; максимум: {longest[1]} — {longest[0]}")
    print(f"предметов покрыто страницами: {len(covered)}")
    for message in warnings:
        print("ПРЕДУПРЕЖДЕНИЕ: " + message)
    if errors:
        print(f"\nОШИБОК: {len(errors)}")
        for message in errors[:80]:
            print("  " + message)
        if len(errors) > 80:
            print(f"  ... и ещё {len(errors) - 80}")
        return 1
    print("\nOK: вики целая — все ссылки живые, страницы длинные, предметы покрыты.")
    return 0


def Wiki_normalize(target):
    value = target.strip().lower()
    if value.endswith(".md"):
        value = value[:-3]
    while value.startswith("./"):
        value = value[2:]
    if value.startswith("/"):
        value = value[1:]
    return value.replace(" ", "-")


if __name__ == "__main__":
    sys.exit(main())
