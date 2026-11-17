# -*- coding: utf-8 -*-
"""
Генератор каталога внутриигровой вики Age of Epoch.

Читает настоящие данные мода:
  - определения предметов  src/main/resources/assets/<ns>/items/**/*.json
  - блокстейты             src/main/resources/assets/mmorpg/blockstates/**/*.json
  - локализацию RU/EN      src/main/resources/assets/<ns>/lang/{ru_ru,en_us}.json
  - описания по Alt         ключи mmorpg.item.desc.* в ru_ru.json
  - рецепты                src/main/resources/data/<ns>/recipe/**/*.json
  - рукописные гайды        tools/wiki_gen/wiki_guides.py

Пишет .md страницы в  src/main/resources/assets/mmorpg/wiki_pages/
(их читает внутриигровой экран ModWikiScreen через WikiCatalog).

Карточка включает описание, доступные рецепты и использования, ссылки и
справочные ID. Текст не дополняется шаблонными абзацами ради длины.

Запуск:  python3 tools/wiki_gen/generate_wiki.py
Проверка без записи: python3 tools/wiki_gen/generate_wiki.py --check
"""
import json
import re
import sys
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "src/main/resources"
OUT = RES / "assets/mmorpg/wiki_pages"

sys.path.insert(0, str(Path(__file__).resolve().parent))
from wiki_guides import GUIDES  # noqa: E402

NAMESPACES = ["mmorpg", "divine_missions", "infinite_dungeons"]

# ---------------------------------------------------------------- lang
def load_lang(ns, code):
    p = RES / f"assets/{ns}/lang/{code}.json"
    if not p.exists():
        return {}
    return json.loads(p.read_text(encoding="utf-8"))

RU = {ns: load_lang(ns, "ru_ru") for ns in NAMESPACES}
EN = {ns: load_lang(ns, "en_us") for ns in NAMESPACES}
RU_MAIN = RU["mmorpg"]

SECTION_RE = re.compile(r"§[0-9a-fk-orx]", re.IGNORECASE)

def clean(s):
    if not s:
        return ""
    return SECTION_RE.sub("", str(s)).replace("§", "").strip()

# ---------------------------------------------------------------- каталог предметов
class Subject:
    def __init__(self, ns, path, is_block_only=False):
        self.ns = ns
        self.path = path                 # registry path, напр. gems/ruby/6
        self.full_id = f"{ns}:{path}"     # mmorpg:gems/ruby/6
        self.is_block_only = is_block_only
        slug = path.replace("/", "_")
        if ns != "mmorpg":
            slug = f"{ns}_{slug}"
        self.page_id = ("blocks/" if is_block_only else "items/") + slug
        self.name_ru = None
        self.name_en = None
        self.category = None
        self.family = None
        self.desc = None        # описание конкретного предмета
        self.family_desc = None # описание семейства

def collect_items():
    out = []
    for ns in NAMESPACES:
        d = RES / f"assets/{ns}/items"
        if d.exists():
            for p in sorted(d.rglob("*.json")):
                out.append(Subject(ns, p.relative_to(d).with_suffix("").as_posix()))
    return out

def collect_block_only(item_paths):
    out = []
    for ns in NAMESPACES:
        d = RES / f"assets/{ns}/blockstates"
        if not d.exists():
            continue
        for p in sorted(d.rglob("*.json")):
            path = p.relative_to(d).with_suffix("").as_posix()
            if (ns, path) not in item_paths:
                out.append(Subject(ns, path, is_block_only=True))
    return out

# ---------------------------------------------------------------- имена
STATION_NAMES = {
    "alchemy_station": "Станция алхимии",
    "cooking_station": "Станция кулинарии",
    "inscribing_station": "Станция начертания",
    "smithing_station": "Кузнечная станция",
    "tablet_station": "Станция скрижалей",
    "socketing_station": "Станция гнёзд снаряжения",
    "socket_station": "Станция вставки самоцветов",
    "salvage_station": "Станция разбора",
    "shield_basic_base": "Щит (базовая основа)",
    "shield_basic_base_blocking": "Щит в стойке блока (модель)",
    "spells/ice_ball": "Ледяной шар (снаряд заклинания)",
}
EXTRA_NAMES_FULL = {
    "mmorpg:furnace": "Печь (блок мода)",
    "mmorpg:life": "Блок жизни",
    "mmorpg:mana": "Блок маны",
    "mmorpg:loot_crate/currency": "Ящик с добычей валюты",
    "mmorpg:summon_portal": "Портал призыва",
    "mmorpg:plant1": "Растение земледелия (ступень 1)",
    "mmorpg:plant2": "Растение земледелия (ступень 2)",
    "mmorpg:plant3": "Растение земледелия (ступень 3)",
    "mmorpg:plant4": "Растение земледелия (ступень 4)",
    "mmorpg:plant5": "Растение земледелия (ступень 5)",
    "infinite_dungeons:shrine": "Святилище подземелий",
}
SLOT_NAMES = {
    "axe": "топор", "boots": "ботинки", "bow": "лук", "chest": "нагрудник",
    "crossbow": "арбалет", "helmet": "шлем", "necklace": "ожерелье",
    "pants": "штаны", "ring": "кольцо", "scepter": "скипетр",
    "shield": "щит", "staff": "посох", "sword": "меч",
}

def rarity_name(r):
    return clean(RU_MAIN.get(f"mmorpg.rarity.gear.{r}", r))

def humanize(path):
    last = path.split("/")[-1].replace("_", " ").strip()
    return last[:1].upper() + last[1:] if last else path

def resolve_name(s):
    dots = s.path.replace("/", ".")
    ru = RU[s.ns].get(f"item.{s.ns}.{dots}") or RU[s.ns].get(f"block.{s.ns}.{dots}")
    en = EN[s.ns].get(f"item.{s.ns}.{dots}") or EN[s.ns].get(f"block.{s.ns}.{dots}")
    if not ru:
        if s.full_id in EXTRA_NAMES_FULL:
            ru = EXTRA_NAMES_FULL[s.full_id]
        elif s.path in STATION_NAMES:
            ru = STATION_NAMES[s.path]
        elif s.path.startswith("rarity/essence/"):
            ru = f"Эссенция редкости ({rarity_name(s.path.split('/')[-1])})"
        elif s.path.startswith("rarity/upgrade_stone/"):
            ru = f"Камень улучшения редкости ({rarity_name(s.path.split('/')[-1])})"
        elif s.path.startswith("stat_soul/"):
            slot = s.path.split("/")[-1]
            ru = f"Душа снаряжения: {SLOT_NAMES.get(slot, slot)}"
        elif re.fullmatch(r"weapon/scepter/scepter(\d+)", s.path):
            ru = f"Скипетр (основа, ступень {s.path[-1]})"
        elif re.fullmatch(r"weapon/staff/staff(\d+)", s.path):
            ru = f"Посох (основа, ступень {s.path[-1]})"
        elif s.path == "potions/reset_stats_potion":
            ru = clean(RU_MAIN.get("item.mmorpg.potions.reset_stats")) or "Зелье сброса характеристик"
        else:
            ru = clean(en) or humanize(s.path)
    s.name_ru = clean(ru)
    s.name_en = clean(en) or s.path.split("/")[-1].replace("_", " ").title()

# ---------------------------------------------------------------- категории и семейства
CAT_LABELS = {
    "gems": "Самоцветы", "runes": "Руны", "currency": "Валюта",
    "ingredient": "Ингредиенты профессий", "mat": "Материалы и эссенции",
    "stone": "Мистические камни", "craft_tools": "Инструменты профессий",
    "craft_result": "Результаты ремесла", "alchemy": "Зелья алхимии",
    "potions": "Особые зелья", "backpack": "Сумки и улучшения",
    "jewelry": "Украшения (основы)", "weapon": "Оружие (основы)",
    "stat_soul": "Души снаряжения", "rarity": "Эссенции и камни редкости",
    "loot_crate": "Ящики с добычей", "projectile": "Снаряды заклинаний",
    "scroll": "Свитки и бумага", "spells": "Снаряды заклинаний",
    "misc": "Особые предметы", "stations": "Станции и блоки",
    "special": "Особые предметы", "blocks": "Блоки мира",
    "divine_missions": "«Божественные миссии»",
    "infinite_dungeons": "«Бесконечные подземелья»",
}
STATION_PATHS = {
    "alchemy_station", "cooking_station", "inscribing_station", "runeword_station",
    "salvage_station", "smithing_station", "socket_station", "socketing_station",
    "tablet_station", "teleporter", "portal",
}

def resolve_category(s):
    if s.is_block_only:
        s.category = "blocks"
        return
    if s.ns != "mmorpg":
        s.category = s.ns
        return
    top = s.path.split("/")[0] if "/" in s.path else ""
    if top and top in CAT_LABELS:
        s.category = top
    elif s.path in STATION_PATHS:
        s.category = "stations"
    else:
        s.category = "special"

def resolve_family(s):
    p = s.path
    if s.ns == "infinite_dungeons":
        s.family = "dungeon_key" if "key" in p else ("dungeon_coin" if "coin" in p else None)
        return
    rules = [
        ("gems/", "gem"), ("runes/", "rune"), ("ingredient/", "ingredient"),
        ("mat/smelted/", "smelted_essence"), ("mat/condensed_salvage/", "condensed_essence"),
        ("stone/", "mining_stone"), ("mat/salvage/", "salvage_dust"),
        ("craft_tools/", "craft_tool"), ("backpack/upgrade/", "backpack_upgrade"),
        ("stat_soul/", "stat_soul"), ("craft_result/", "crafted_consumable"),
        ("alchemy/", "alchemy_potion"), ("currency/", "currency"),
        ("rarity/upgrade_stone/", "currency"), ("loot_crate", "loot_crate"),
    ]
    for pref, fam in rules:
        if p.startswith(pref):
            s.family = fam
            return
    if p == "backpack/material_pouch":
        s.family = "material_pouch"
    elif p.startswith("backpack/"):
        s.family = "backpack"
    elif s.category == "stations" or s.is_block_only:
        s.family = "block"
    elif s.category in ("weapon", "jewelry") or p.startswith("shield"):
        s.family = "gear"

def resolve_desc(s):
    dots = s.path.replace("/", ".")
    key = "mmorpg.item.desc." + (dots if s.ns == "mmorpg" else f"{s.ns}.{dots}")
    d = RU_MAIN.get(key)
    if not d:
        d = RU[s.ns].get(f"item.{s.ns}.{dots}.desc")
    s.desc = clean(d) if d else None
    if s.family:
        fd = RU_MAIN.get(f"mmorpg.item.desc.family.{s.family}")
        s.family_desc = clean(fd) if fd else None

# ---------------------------------------------------------------- рецепты
VANILLA_RU = {
    "minecraft:book": "книга", "minecraft:bowl": "миска", "minecraft:bread": "хлеб",
    "minecraft:chest": "сундук", "minecraft:coal": "уголь",
    "minecraft:cobblestone": "булыжник", "minecraft:crafting_table": "верстак",
    "minecraft:diamond": "алмаз", "minecraft:emerald": "изумруд",
    "minecraft:ender_pearl": "эндер-жемчуг", "minecraft:feather": "перо",
    "minecraft:furnace": "печь", "minecraft:glass_bottle": "стеклянная бутылка",
    "minecraft:glistering_melon_slice": "сверкающий ломтик арбуза",
    "minecraft:gold_ingot": "золотой слиток", "minecraft:gold_nugget": "золотой самородок",
    "minecraft:iron_axe": "железный топор", "minecraft:iron_chestplate": "железный нагрудник",
    "minecraft:iron_ingot": "железный слиток", "minecraft:iron_sword": "железный меч",
    "minecraft:lapis_lazuli": "лазурит", "minecraft:leather": "кожа",
    "minecraft:name_tag": "бирка", "minecraft:paper": "бумага",
    "minecraft:redstone": "красная пыль", "minecraft:rotten_flesh": "гнилая плоть",
    "minecraft:stick": "палка", "minecraft:string": "нить",
    "minecraft:torch": "факел", "minecraft:wheat_seeds": "семена пшеницы",
}
RECIPE_KIND = {
    "minecraft:crafting_shaped": "крафт на верстаке по форме (ингредиенты раскладываются рисунком)",
    "minecraft:crafting_shapeless": "крафт на верстаке без формы (положить ингредиенты в любом порядке)",
    "minecraft:smelting": "переплавка в печи",
}

def ingredient_ids(node):
    """Достать id предметов из узла ингредиента рецепта 26.x (строка/список/объект)."""
    if isinstance(node, str):
        return [node]
    if isinstance(node, list):
        out = []
        for x in node:
            out += ingredient_ids(x)
        return out
    if isinstance(node, dict):
        for k in ("item", "id"):
            if isinstance(node.get(k), str):
                return [node[k]]
        if isinstance(node.get("tag"), str):
            return ["#" + node["tag"]]
    return []

def load_recipes(subjects_by_id):
    by_result = defaultdict(list)
    used_in = defaultdict(list)
    for ns in NAMESPACES + ["divine_missions"]:
        d = RES / f"data/{ns}/recipe"
        if not d.exists():
            d = RES / f"data/{ns}/recipes"
        if not d.exists():
            continue
        for p in sorted(d.rglob("*.json")):
            try:
                j = json.loads(p.read_text(encoding="utf-8"))
            except Exception:
                continue
            res = j.get("result") or {}
            rid = res.get("id") or res.get("item") if isinstance(res, dict) else None
            if not rid:
                continue
            count = res.get("count", 1) if isinstance(res, dict) else 1
            ings = Counter()
            if j.get("key") and j.get("pattern"):
                for row in j["pattern"]:
                    for ch in row:
                        if ch != " " and ch in j["key"]:
                            for iid in ingredient_ids(j["key"][ch]):
                                ings[iid] += 1
            for node in (j.get("ingredients") or []):
                for iid in ingredient_ids(node):
                    ings[iid] += 1
            if j.get("ingredient") is not None:
                for iid in ingredient_ids(j["ingredient"]):
                    ings[iid] += 1
            rec = {"kind": RECIPE_KIND.get(j.get("type", ""), "особый рецепт"),
                   "result": rid, "count": count, "ings": dict(ings)}
            by_result[rid].append(rec)
            for iid in ings:
                if iid in subjects_by_id:
                    used_in[iid].append(rec)
    return by_result, used_in

# ---------------------------------------------------------------- тексты по семействам
GEM_TYPES = {
    "tourmaline": ("Турмалин", "живучесть", "восстановление здоровья", "вампиризм: лечение частью нанесённого урона"),
    "azurite": ("Азурит", "мудрость", "восстановление маны", "возврат маны при ударе"),
    "garnet": ("Гранат", "ловкость", "восстановление энергии", "шанс критического удара оружия"),
    "opal": ("Опал", "силу", "шанс критического удара оружия", "критический урон оружия"),
    "topaz": ("Топаз", "проворность", "запас энергии", "возврат энергии при ударе"),
    "amethyst": ("Аметист", "интеллект", "урон заклинаний", "критический урон заклинаний"),
    "ruby": ("Рубин", "защиту от стихии огня", "урон стихией огня", "урон стихией огня"),
    "emerald": ("Изумруд", "защиту от стихии земли", "урон стихией земли", "урон стихией земли"),
    "sapphire": ("Сапфир", "защиту от стихии воды", "урон стихией воды", "урон стихией воды"),
}
GEM_RANKS = ["треснувший", "сколотый", "с изъяном", "обычный", "великий", "великолепный", "божественный"]
GEM_RANK_POWER = ["примерно десятую часть", "примерно пятую часть", "примерно треть",
                  "примерно две пятых", "примерно три пятых", "примерно четыре пятых", "полную силу"]
RUNE_LEVEL = {"yun": 5, "ven": 5, "oru": 4, "wir": 4, "eno": 3, "har": 3, "xer": 3,
              "cen": 2, "fey": 2, "dos": 2, "ano": 2, "toq": 2, "nos": 1, "mos": 1, "ita": 1}
RUNEWORDS = [
    (["cen", "dos", "yun"], "лук"), (["ita", "dos", "toq"], "шлем"),
    (["toq", "dos", "yun", "wir"], "ожерелье"), (["ano", "oru", "dos"], "кольцо"),
    (["yun", "fey", "oru"], "кольцо"), (["yun", "mos", "oru", "ven"], "посох"),
    (["ano", "toq", "mos", "har"], "меч"),
]
INGREDIENT_SPECIAL = {
    "ingredient/bone_figurine": "Позиционный ингредиент: даёт +10% ингредиентам, которые её не касаются в сетке, и −25% тем, что касаются. Кладётся в угол сетки, соседей ей лучше не давать.",
    "ingredient/blackpearl": "Позиционный ингредиент критического урона: ослабляет касающиеся ингредиенты на 20%, поэтому кладётся подальше от тех, чью силу терять жалко.",
    "ingredient/enigma_stone": "Позиционный ингредиент крита заклинаний: касающиеся ингредиенты слабее на 25% — раскладывайте вокруг него воздух или дешёвую мелочь.",
    "ingredient/rock_jade": "Артефактный ингредиент: добавляет 1 случайную характеристику, поднимает минимальную редкость результата и усиливает касающиеся ингредиенты на 20%, но снижает шанс успеха крафта на 10%.",
    "ingredient/endergem": "Артефактный ингредиент: добавляет 2 случайные характеристики, поднимает минимальную редкость, усиливает соседей на 20%, но снижает шанс успеха на 25% — страхуйте матрицей душ.",
    "ingredient/wishing_bowl": "Артефактный ингредиент: поднимает минимальную редкость результата крафта на ступень.",
    "ingredient/pot_of_greed": "Артефактный ингредиент: даёт +2 к максимуму применений создаваемого расходника.",
    "ingredient/soul_matrix": "Артефактный ингредиент-страховка: +20% к шансу успеха крафта. Кладётся в дорогие раскладки.",
}

FAMILY_GUIDES = {
    "gem": ("guides/gems", "Самоцветы"), "rune": ("guides/runes", "Руны и рунные слова"),
    "ingredient": ("guides/crafting", "Ремесло профессий"), "craft_tool": ("guides/professions", "Профессии"),
    "crafted_consumable": ("guides/crafting", "Ремесло профессий"),
    "alchemy_potion": ("guides/food-potions", "Еда и зелья"), "currency": ("guides/currency", "Валюта: шары и камни"),
    "stat_soul": ("guides/souls", "Души снаряжения"), "backpack": ("guides/backpack", "Сумки и хранение"),
    "material_pouch": ("guides/backpack", "Сумки и хранение"), "backpack_upgrade": ("guides/backpack", "Сумки и хранение"),
    "smelted_essence": ("guides/salvage", "Разбор и эссенции"), "condensed_essence": ("guides/salvage", "Разбор и эссенции"),
    "salvage_dust": ("guides/salvage", "Разбор и эссенции"), "mining_stone": ("guides/professions", "Профессии"),
    "block": ("guides/stations", "Станции и блоки мода"), "gear": ("guides/gear", "Снаряжение от А до Я"),
    "loot_crate": ("guides/backpack", "Сумки и хранение"), "dungeon_key": ("guides/dungeons", "Бесконечные подземелья"),
    "dungeon_coin": ("guides/dungeons", "Бесконечные подземелья"), "tiered": ("guides/professions", "Профессии"),
}
CATEGORY_GUIDES = {
    "gems": ("guides/gems", "Самоцветы"), "runes": ("guides/runes", "Руны и рунные слова"),
    "currency": ("guides/currency", "Валюта: шары и камни"), "ingredient": ("guides/crafting", "Ремесло профессий"),
    "mat": ("guides/salvage", "Разбор и эссенции"), "stone": ("guides/professions", "Профессии"),
    "craft_tools": ("guides/professions", "Профессии"), "craft_result": ("guides/crafting", "Ремесло профессий"),
    "alchemy": ("guides/food-potions", "Еда и зелья"), "potions": ("guides/food-potions", "Еда и зелья"),
    "backpack": ("guides/backpack", "Сумки и хранение"), "jewelry": ("guides/gear", "Снаряжение от А до Я"),
    "weapon": ("guides/gear", "Снаряжение от А до Я"), "stat_soul": ("guides/souls", "Души снаряжения"),
    "rarity": ("guides/rarities", "Редкости"), "loot_crate": ("guides/backpack", "Сумки и хранение"),
    "projectile": ("guides/spells", "Заклинания"), "spells": ("guides/spells", "Заклинания"),
    "stations": ("guides/stations", "Станции и блоки мода"), "blocks": ("guides/stations", "Станции и блоки мода"),
    "divine_missions": ("guides/missions", "Миссии богов и благосклонность"),
    "infinite_dungeons": ("guides/dungeons", "Бесконечные подземелья"),
}
CAT_INTRO = {
    "gems": "Самоцветы вставляются в гнёзда снаряжения и работают по-разному в броне, украшениях и оружии.",
    "runes": "Руны — буквы рунных слов: порядок на станции важнее количества.",
    "currency": "Валюта применяется прямо к снаряжению: взяли на курсор — нажали по вещи.",
    "ingredient": "Ингредиенты кладутся в сетку ремесла 3×2 и определяют характеристики результата.",
    "mat": "Материалы и эссенции — топливо ремонта, ремесла и половины крафта мода.",
    "stone": "Мистические камни попадаются при добыче руды и уходят в ремесло.",
    "craft_tools": "Инструмент открывает ремесло своей профессии (ПКМ).",
    "craft_result": "Изделия ремесла: их характеристики рождаются из вложенных ингредиентов.",
    "alchemy": "Зелья алхимии: пять ступеней силы, от духовных до божественных.",
    "potions": "Особые зелья перестраивают героя, а не лечат.",
    "backpack": "Сумки расширяют карманы героя, улучшения ставятся прямо на сумку.",
    "jewelry": "Основы колец и ожерелий: заготовки под добычу, ремесло и рунные слова.",
    "weapon": "Основы оружия: ступени болванок под снаряжение с характеристиками.",
    "stat_soul": "Души хранят заготовки снаряжения и переносят их на пустые основы.",
    "rarity": "Эссенции и камни, работающие с редкостью снаряжения.",
    "loot_crate": "Ящики открываются и выдают добычу своей темы и редкости.",
    "projectile": "Предметы-снаряды: так выглядят в инвентаре «патроны» заклинаний.",
    "spells": "Предметы, связанные с заклинаниями напрямую.",
    "scroll": "Бумага и свитки — основа начертания и перелётов домой.",
    "misc": "Особые предметы, не влезшие в другие разделы.",
    "stations": "Устанавливаемые блоки: каждое рабочее место — одна операция.",
    "special": "Особые предметы мода: инструменты, награды и служебные вещи.",
    "blocks": "Блоки, встречающиеся в мире и структурах мода.",
    "divine_missions": "Предметы дополнения «Божественные миссии»: задания богов и благосклонность.",
    "infinite_dungeons": "Предметы дополнения «Бесконечные подземелья»: ключи, монеты и награды.",
}
OBTAIN_BY_FAMILY = {
    "gem": "Самоцветы встречаются в добыче, сундуках и тематических ящиках; высокие ранги реже.",
    "rune": "Руны выпадают с монстров и из ящиков рун; руны более высокого уровня встречаются реже.",
    "ingredient": "Ингредиенты получают через профессии и исследование мира; точный источник зависит от вида.",
    "smelted_essence": "Получается переплавкой очищенной эссенции в печи или на станции.",
    "condensed_essence": "Создаётся из трёх переплавленных эссенций одного ранга.",
    "salvage_dust": "Получается при разборе снаряжения молотом или на станции разбора.",
    "mining_stone": "Может выпасть при добыче руды как бонус шахтёрства.",
    "craft_tool": "Рецепт указан выше, если инструмент создаётся крафтом; другие способы и требования смотрите у профессии (H).",
    "currency": "Валюта встречается в добыче, сундуках и наградах игровых систем; редкие инструменты выпадают реже.",
    "stat_soul": "Души встречаются в добыче и наградах; отдельные заготовки создаются соответствующей профессией.",
    "alchemy_potion": "Зелья создаются алхимией; рецепт этого предмета указан выше, если он есть в датапаке.",
    "crafted_consumable": "Создаётся ремеслом соответствующей профессии из ингредиентов.",
    "block": "Блок можно создать по рецепту выше, если он есть; служебные блоки также встречаются в структурах и игровых системах.",
    "gear": "Основы снаряжения выпадают, создаются ремеслом или используются с душами; силу им дают характеристики и улучшения.",
    "loot_crate": "Ящики выдаются как награды за уровни, миссии, башни, подземелья и события.",
    "dungeon_key": "Ключи получают в мире и в самих бесконечных подземельях.",
    "dungeon_coin": "Монеты добываются и тратятся внутри бесконечных подземелий.",
}

CAT_MORE = {
    "craft_tools": "Инструмент открывает меню профессии правой кнопкой мыши; для ремесла по снаряжению предусмотрен отдельный слот вещи. Требования к уровню профессии видны на экране профессий (H).",
    "divine_missions": "Святилища выдают задания; за них начисляются опыт и благосклонность. Условия зачёта и траты благосклонности — в гайде [[guides/missions|Миссии богов]], коллекционный прогресс — в [[guides/questbook|Книге Эпохи]].",
    "jewelry": "Кольца и ожерелья служат основами для свойств, ремесла и рунных слов. На украшениях удобно добирать сопротивления, критический урон и восстановление ресурсов; см. [[guides/runes|гайд по рунам]].",
    "loot_crate": "Ящики открываются по подсказке предмета и дают награды своей темы или редкости. Содержимое зависит от источника награды; держать ящик до более высокого уровня обычно невыгодно.",
    "misc": "В этом разделе собраны особые предметы для редких ситуаций. Средство возврата из опасной зоны держите в доступном слоте перед походом в карту или экспедицию.",
    "projectile": "Снаряды — визуальные носители эффектов заклинаний, а не обычное оружие. Назначение и тип урона описаны в [[guides/spells|гайде по заклинаниям]].",
    "scroll": "Бумага и свитки используются в начертании и для возвращения домой. Созданные ремеслом свитки могут передавать свойства вложенных ингредиентов; см. [[guides/professions|Профессии]].",
    "spells": "Предметы этого раздела связаны с заклинаниями, но сами по себе не заменяют изучение умений. Заклинания открываются на экране умений за очки уровней; см. [[guides/spells|гайд по заклинаниям]].",
    "stone": "Мистические камни выпадают при добыче руды и используются в ремесле. Сохраняйте их ранги раздельно; связанные системы — [[guides/professions|Профессии]] и [[guides/crafting|Ремесло]].",
}

# ---------------------------------------------------------------- рендер страниц предметов
def first_sentence(text):
    if not text:
        return ""
    t = text.replace("\n", " ").strip()
    m = re.split(r"(?<=[.!])\s+", t)
    return m[0] if m else t

def name_for_id(iid, subjects_by_id):
    if iid in subjects_by_id:
        s = subjects_by_id[iid]
        return f"[[{s.page_id}|{s.name_ru}]]"
    if iid in VANILLA_RU:
        return VANILLA_RU[iid] + f" (`{iid}`)"
    return f"`{iid}`"

def recipe_lines(recs, subjects_by_id):
    out = []
    for r in recs:
        out.append(f"- **{r['kind']}**" + (f", получается ×{r['count']}" if r["count"] != 1 else "") + ":")
        for iid, n in sorted(r["ings"].items()):
            out.append(f"  - {name_for_id(iid, subjects_by_id)}" + (f" ×{n}" if n > 1 else ""))
    return out

def neighbours(s, subjects):
    """Похожие предметы: та же «лесенка» (ранг/ступень) или та же подпапка."""
    parts = s.path.split("/")
    same = []
    if len(parts) >= 2:
        prefix = "/".join(parts[:-1]) + "/"
        same = [x for x in subjects if x.ns == s.ns and x.path.startswith(prefix) and x is not s]
    if not same:
        same = [x for x in subjects if x.category == s.category and x is not s][:6]
    return same[:6]

def extra_facts(s):
    facts = []
    p = s.path.split("/")
    if s.family == "gem" and len(p) == 3 and p[1] in GEM_TYPES and p[2].isdigit():
        gname, armor, jewel, weapon = GEM_TYPES[p[1]]
        rank = int(p[2])
        facts.append(f"Это **{gname.lower()}** ступени {rank + 1} из {len(GEM_RANKS)} («{GEM_RANKS[rank]}»): "
                     f"в броне даёт {armor}, в украшениях — {jewel}, в оружии — {weapon}; "
                     f"сила — {GEM_RANK_POWER[rank]} максимального эффекта. Таблица камней — в гайде [[guides/gems|Самоцветы]].")
    if s.family == "rune" and len(p) == 2:
        rid = p[1]
        lvl = RUNE_LEVEL.get(rid)
        words = [(w, slot) for w, slot in RUNEWORDS if rid in w]
        facts.append(f"«Уровень» этой руны — {lvl} из 5: чем выше, тем реже руна выпадает. "
                     + ("Входит в слова: " + "; ".join(" · ".join(f"[[items/runes_{r}|{r.upper()}]]" for r in w) + f" — на {slot}" for w, slot in words) + "."
                        if words else "В коротких известных словах из гайда не участвует — её слова длиннее и реже; не выбрасывайте дубликаты."))
    special = INGREDIENT_SPECIAL.get(s.path)
    if special:
        facts.append(special)
    if s.category == "alchemy" and len(p) == 4 and p[-1].isdigit():
        tier = int(p[-1])
        kind = "здоровья" if "health" in s.path else "маны"
        facts.append(f"Это зелье {kind}, ступень {tier + 1} из 5: более высокая ступень восстанавливает больше ресурса. "
                     f"Смотрите точное значение в подсказке предмета; правила лечения и маны — в гайде [[guides/resources|Ресурсы]].")
    if s.family == "currency" and s.desc:
        facts.append("Как и вся валюта, применяется без верстака: предмет берут на курсор и нажимают по снаряжению в инвентаре. "
                     "Требования, шансы и цену нестабильности смотрите по Shift до применения — отменить результат нельзя. "
                     "Полная техника безопасности: [[guides/currency|Валюта: шары и камни]].")
    return facts

def render_subject(s, subjects, by_id, by_result, used_in):
    cat_label = CAT_LABELS.get(s.category, s.category)
    guide = FAMILY_GUIDES.get(s.family) or CATEGORY_GUIDES.get(s.category)
    desc_full = s.desc or s.family_desc

    lines = [
        "---",
        f"title: {s.name_ru}",
    ]
    if not s.is_block_only:
        lines.append(f"item: {s.full_id}")
    lines.extend([
        f"category: {s.category}",
        f"category_label: {cat_label}",
        f"tags: {cat_label}, {s.name_en}, {s.path}",
        "---",
        f"# {s.name_ru}",
        "",
        f"Раздел: [[categories/{s.category}|{cat_label}]].",
    ])
    if guide:
        lines[-1] += f" Подробности системы: [[{guide[0]}|{guide[1]}]]."
    lines.extend(["", "## Описание", ""])

    if desc_full:
        for paragraph in [part.strip() for part in desc_full.split("\n") if part.strip()]:
            lines.extend([paragraph, ""])
    else:
        lines.extend([CAT_INTRO.get(s.category, "Описание этого предмета пока не добавлено."), ""])

    for fact in extra_facts(s):
        lines.extend([fact, ""])

    lines.extend(["## Получение", ""])
    recs = (by_result.get(s.full_id)
            or by_result.get(f"mmorpg:{s.path}" if s.ns != "mmorpg" else "")
            or [])
    if recs:
        lines.extend(["Рецепты из датапака мода:", ""])
        lines.extend(recipe_lines(recs, by_id))
        lines.append("")
    else:
        obtain = OBTAIN_BY_FAMILY.get(
            s.family or "",
            "Рецепт в датапаке мода не найден. Проверьте источники предмета в JEI или связанном гайде.",
        )
        lines.extend([obtain, ""])

    uses = used_in.get(s.full_id, [])
    if uses:
        targets = []
        for recipe in uses:
            result = by_id.get(recipe["result"])
            label = f"[[{result.page_id}|{result.name_ru}]]" if result else f"`{recipe['result']}`"
            if label not in targets:
                targets.append(label)
        lines.extend(["### Используется в рецептах", ""])
        for target in targets[:16]:
            lines.append(f"- {target}")
        if len(targets) > 16:
            lines.append(f"- И ещё {len(targets) - 16} рецептов.")
        lines.append("")

    details = []
    if s.family == "currency":
        details.append("Перед применением проверьте требования и возможный рост нестабильности по подсказке предмета.")
    if s.family == "gem":
        details.append("Для установки нужно свободное гнездо; извлечь камень можно [[items/socket_extractor|экстрактором самоцветов]].")
    if s.family == "rune":
        details.append("Одна руна не даёт эффекта: она используется в правильной последовательности на станции рунных слов.")
    if s.family == "ingredient":
        details.append("Эффект зависит от сетки ремесла 3×2 и соседних ингредиентов; подробности — в гайде [[guides/crafting|Ремесло профессий]].")
    if s.family in ("salvage_dust", "smelted_essence", "condensed_essence"):
        details.append("Ранги эссенций различаются и не взаимозаменяемы в рецептах.")
    if s.category in ("stations", "blocks"):
        details.append("Блок открывается взаимодействием в мире. Служебные блоки структур могут не иметь рецепта.")
    if s.family == "stat_soul":
        details.append("Душа применяется к пустой основе того же типа и расходуется.")
    if s.category in ("projectile", "spells"):
        details.append("Это носитель или снаряд заклинания, а не самостоятельное оружие.")
    if details:
        lines.extend(["## Ограничения", ""])
        for detail in details:
            lines.append(f"- {detail}")
        lines.append("")

    related = []
    if guide:
        related.append(f"- [[{guide[0]}|{guide[1]}]]")
    related.append(f"- [[categories/{s.category}|{cat_label}]]")
    similar = neighbours(s, subjects)
    if similar:
        related.append("- Похожие предметы:")
        related.extend(f"  - [[{other.page_id}|{other.name_ru}]]" for other in similar)
    lines.extend(["## Связанные страницы", "", *related, "", "## Справка", "", "| Поле | Значение |", "|---|---|"])
    if not s.is_block_only:
        lines.append(f"| ID предмета | `{s.full_id}` |")
    lines.append(f"| Английское название | {s.name_en} |")
    lines.append(f"| Раздел | {cat_label} |")
    if s.family:
        lines.append(f"| Семейство | `{s.family}` |")
    lines.append(f"| ID страницы | `{s.page_id}` |")
    lines.append("")
    return "\n".join(lines) + "\n"

# ---------------------------------------------------------------- категории и индекс
def render_category(cat, subjects):
    label = CAT_LABELS.get(cat, cat)
    items = [s for s in subjects if s.category == cat]
    guide = CATEGORY_GUIDES.get(cat)
    lines = ["---", f"title: {label}", "category: categories", f"category_label: Разделы", "---",
             f"# {label}", ""]
    lines.append(CAT_INTRO.get(cat, "") + f" Всего предметов: {len(items)}. Откройте карточку по ссылке, чтобы увидеть описание и рецепты.")
    lines.append("")
    if guide:
        lines.append(f"> Как система работает целиком: [[{guide[0]}|{guide[1]}]].")
        lines.append("")
    if cat in CAT_MORE:
        lines.append(CAT_MORE[cat])
        lines.append("")
    groups = defaultdict(list)
    for s in items:
        sub = "/".join(s.path.split("/")[:-1]) or "—"
        groups[sub].append(s)
    for sub in sorted(groups):
        if len(groups) > 1:
            lines.append(f"## {sub}")
            lines.append("")
        for s in sorted(groups[sub], key=lambda x: x.name_ru):
            hook = first_sentence(s.desc or s.family_desc)
            lines.append(f"- [[{s.page_id}|{s.name_ru}]]" + (f" — {hook}" if hook else f" — `{s.full_id}`"))
        lines.append("")
    lines.append("## Связанные страницы")
    lines.append("")
    lines.append("- [[index|Главная страница вики]]")
    if guide:
        lines.append(f"- [[{guide[0]}|{guide[1]}]]")
    lines.append("")
    return "\n".join(lines) + "\n"

GUIDE_TOC = [
    ("guides/start", "Первые шаги"), ("guides/levels", "Уровни и опыт"),
    ("guides/stats", "Характеристики простыми словами"), ("guides/damage", "Типы урона"),
    ("guides/defense", "Защита: броня, уклонение, блок"), ("guides/crit", "Критические удары и точность"),
    ("guides/resources", "Здоровье, мана, энергия и кровь"), ("guides/rarities", "Редкости"),
    ("guides/gear", "Снаряжение от А до Я"), ("guides/identify", "Определение предметов"),
    ("guides/currency", "Валюта: шары и камни"), ("guides/gems", "Самоцветы"),
    ("guides/runes", "Руны и рунные слова"), ("guides/salvage", "Разбор и эссенции"),
    ("guides/crafting", "Ремесло профессий"), ("guides/professions", "Профессии"),
    ("guides/food-potions", "Еда и зелья"), ("guides/spells", "Заклинания"),
    ("guides/talents", "Таланты"), ("guides/divine", "Класс «Божественный»: призыв"),
    ("guides/effects", "Эффекты"), ("guides/mobs", "Монстры: уровни и редкости"),
    ("guides/maps", "Земли, измерения и карты приключений"), ("guides/dungeons", "Бесконечные подземелья"),
    ("guides/delves", "Подземные экспедиции"), ("guides/towers", "Тематические башни"),
    ("guides/missions", "Миссии богов и благосклонность"), ("guides/questbook", "Книга Эпохи"),
    ("guides/backpack", "Сумки и хранение"), ("guides/souls", "Души снаряжения"),
    ("guides/stations", "Станции и блоки мода"), ("guides/uniques", "Уникальные предметы и комплекты"),
    ("guides/faq", "Частые вопросы"), ("guides/glossary", "Словарь простыми словами"),
]

def render_index(subjects):
    cats = sorted({s.category for s in subjects})
    lines = ["---", "title: Вики Эпохи — главная", "category: home", "category_label: Главная", "---",
             "# Вики Эпохи", ""]
    lines.append("**Добро пожаловать в полную энциклопедию Age of Epoch.** Здесь есть страница на **каждый предмет мода** — с вращающейся моделью справа, подробным объяснением обычными словами, рецептами и ссылками на связанные системы, — а также исчерпывающие гайды по всем механикам: от типов урона и уклонения до порчи, рунных слов и класса «Божественный».")
    lines.append("")
    lines.append("## Как пользоваться вики")
    lines.append("")
    lines.append("- **Поиск сверху** ищет по русским и английским названиям, ID предметов и тексту страниц: начните печатать — список слева отфильтруется.")
    lines.append("- **Ссылки в тексте** ведут на другие страницы и разделы страниц; кнопки назад/вперёд в шапке возвращают по истории переходов, домик — сюда.")
    lines.append("- **Модель справа** можно крутить мышью (зажмите и тяните); наведите курсор — увидите обычное игровое описание предмета.")
    lines.append("- **Колесо мыши** прокручивает статью или список слева — смотря где курсор.")
    lines.append("- **Быстрый вход из инвентаря**: наведите курсор на любой предмет и удерживайте клавишу **B** — прямо в тултипе заполнится полоса, и вики сама откроется на статье этого предмета. Снаряжение с характеристиками откроет [[guides/gear|гайд по снаряжению]], а ванильный ингредиент — поиск по рецептам с ним. Клавиша переназначается в настройках управления («Удерживать на предмете: открыть в Вики Эпохи»).")
    lines.append("")
    lines.append("## С чего начать")
    lines.append("")
    lines.append("1. [[guides/start|Первые шаги]] — маршрут новичка за пять минут.")
    lines.append("2. [[guides/stats|Характеристики простыми словами]] и [[guides/damage|Типы урона]] — база, без которой не читаются вещи.")
    lines.append("3. [[guides/gear|Снаряжение от А до Я]] — как устроена любая находка и чем её улучшать.")
    lines.append("4. [[guides/questbook|Книга Эпохи]] — ваш личный маршрут по игре в самой игре (клавиша K).")
    lines.append("")
    lines.append("## Гайды по системам")
    lines.append("")
    for pid, title in GUIDE_TOC:
        lines.append(f"- [[{pid}|{title}]]")
    lines.append("")
    lines.append("## Каталог предметов по разделам")
    lines.append("")
    for cat in cats:
        n = sum(1 for s in subjects if s.category == cat)
        lines.append(f"- [[categories/{cat}|{CAT_LABELS.get(cat, cat)}]] — {n} предм.")
    lines.append("")
    lines.append("## Об этой вики")
    lines.append("")
    lines.append(f"Страницы предметов генерируются из данных мода: названий и описаний из языковых файлов и рецептов из датапака, а гайды написаны вручную простым языком. Всего предметов в каталоге: {len(subjects)}. Если нашли неточность — сравните с подсказкой по Alt в инвентаре: она берётся из тех же данных, и сообщите на сервере или в репозитории (WIKI.md).")
    lines.append("")
    return "\n".join(lines) + "\n"

# ---------------------------------------------------------------- main
def main():
    check_only = "--check" in sys.argv[1:]
    unknown = [arg for arg in sys.argv[1:] if arg != "--check"]
    if unknown:
        print("Неизвестные аргументы:", " ".join(unknown))
        return 2

    subjects = collect_items()
    item_paths = {(s.ns, s.path) for s in subjects}
    subjects += collect_block_only(item_paths)
    for s in subjects:
        resolve_name(s)
        resolve_category(s)
        resolve_family(s)
        resolve_desc(s)
    by_id = {s.full_id: s for s in subjects}
    by_result, used_in = load_recipes(by_id)

    if not check_only:
        if OUT.exists():
            for old in OUT.rglob("*.md"):
                old.unlink()
        OUT.mkdir(parents=True, exist_ok=True)

    written = 0
    generated = {}

    def write(page_id, text):
        nonlocal written
        if check_only:
            generated[page_id] = text
        else:
            p = OUT / (page_id + ".md")
            p.parent.mkdir(parents=True, exist_ok=True)
            p.write_text(text, encoding="utf-8")
        written += 1

    write("index", render_index(subjects))
    for cat in sorted({s.category for s in subjects}):
        write(f"categories/{cat}", render_category(cat, subjects))
    for gid, g in GUIDES.items():
        fm = ["---", f"title: {g['title']}"]
        if g.get("item"):
            fm.append(f"item: {g['item']}")
        fm.append("category: guides")
        fm.append("category_label: Гайды")
        fm.append(f"tags: гайд, {g['title']}")
        fm.append("---")
        write(f"guides/{gid}", "\n".join(fm) + f"\n# {g['title']}\n\n" + g["body"].strip() + "\n")
    for s in subjects:
        write(s.page_id, render_subject(s, subjects, by_id, by_result, used_in))

    print(f"subjects: {len(subjects)} (block-only: {sum(1 for s in subjects if s.is_block_only)})")
    if check_only:
        actual = {}
        if OUT.exists():
            for page in OUT.rglob("*.md"):
                page_id = page.relative_to(OUT).with_suffix("").as_posix()
                actual[page_id] = page.read_text(encoding="utf-8")
        missing = sorted(set(generated) - set(actual))
        stale = sorted(set(actual) - set(generated))
        changed = sorted(page_id for page_id in generated
                         if page_id in actual and generated[page_id] != actual[page_id])
        if missing or stale or changed:
            for page_id in missing:
                print(f"MISSING: {page_id}.md")
            for page_id in stale:
                print(f"STALE: {page_id}.md")
            for page_id in changed:
                print(f"OUTDATED: {page_id}.md")
            print(f"FAIL: generated catalog differs ({len(missing)} missing, {len(stale)} stale, {len(changed)} changed)")
            return 1
        print(f"OK: {written} generated pages match {OUT}")
        return 0

    print(f"pages written: {written} -> {OUT}")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
