#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Аудит покрытия описаний предметов (Alt-описания, см. ITEM_DESCRIPTIONS.md).

Скрипт разбирает исходники и повторяет ту же логику, что и в игре:
    ItemDescriptions.itemKey(stack)   -> mmorpg.item.desc.<путь>
                                         mmorpg.item.desc.<неймспейс>.<путь>
    ItemDescriptions.familyKey(stack) -> mmorpg.item.desc.family.<семейство>

Для каждого зарегистрированного предмета проверяется наличие ключа в ru_ru.json и
en_us.json. Результат пишется в ITEM_DESCRIPTION_COVERAGE.md; если чего-то не хватает,
в отчёте появляется раздел «Осталось без описания».

Запуск (из корня репозитория):
    python3 tools/audit_item_descriptions.py
"""

import io
import json
import os
import re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
JAVA = os.path.join(ROOT, 'src/main/java')
LANG = os.path.join(ROOT, 'src/main/resources/assets/mmorpg/lang')
REG = os.path.join(JAVA, 'com/robertx22/age_of_exile/mmorpg/registers/common/items')
PREFIX = 'mmorpg.item.desc.'


def load_lang(name):
    with io.open(os.path.join(LANG, name), encoding='utf-8') as f:
        return json.load(f)


RU = load_lang('ru_ru.json')
EN = load_lang('en_us.json')


# ---------------------------------------------------------------- иерархия классов
classes = {}  # имя класса -> (пакет, имя суперкласса)
for dirpath, _, files in os.walk(JAVA):
    for fn in files:
        if not fn.endswith('.java'):
            continue
        with io.open(os.path.join(dirpath, fn), encoding='utf-8') as f:
            src = f.read()
        pkg = re.search(r'^package\s+([\w.]+);', src, re.M)
        m = re.search(r'\b(?:class|enum|record)\s+(\w+)(?:<[^>]*>)?(?:\s+extends\s+([\w.]+))?', src)
        if not m:
            continue
        name, sup = m.group(1), m.group(2)
        classes[name] = (pkg.group(1) if pkg else '', sup.split('.')[-1] if sup else None)


# ---------------------------------------------------------------- семейства
with io.open(os.path.join(JAVA, 'com/robertx22/age_of_exile/uncommon/localization/ItemDescriptions.java'),
             encoding='utf-8') as f:
    desc_src = f.read()
fam_src = desc_src.split('public static String familyKey', 1)[1].split('public static String resolveKey', 1)[0]

families = []  # [(имя класса, семейство)] в порядке проверок
_pending = []
for line in fam_src.splitlines():
    _pending += re.findall(r'instanceof\s+(\w+)', line)
    m = re.search(r'FAMILY_PREFIX \+\s+"(\w+)"', line)
    if m:
        for c in _pending:
            families.append((c, m.group(1)))
        _pending = []


def chain(cls):
    out, seen = [], set()
    while cls and cls not in seen:
        seen.add(cls)
        out.append(cls)
        cls = classes.get(cls, ('', None))[1]
    return out


def family_for(cls):
    ch = chain(cls)
    for c, fam in families:
        if c in ch:
            return fam
    return None


# ---------------------------------------------------------------- регистрации
items = []  # (id, класс)


def scan_registers(path):
    with io.open(path, encoding='utf-8') as f:
        src = f.read()
    for m in re.finditer(r'Def\.item\(\s*"([^"]+)"\s*,\s*\(\)\s*->\s*new\s+(\w+)', src):
        items.append((m.group(1), m.group(2)))
    for m in re.finditer(r'Def\.item\(\s*\(\)\s*->\s*new\s+(\w+)\([^)]*\)\s*,\s*"([^"]+)"', src):
        items.append((m.group(2), m.group(1)))


for fn in sorted(os.listdir(REG)):
    scan_registers(os.path.join(REG, fn))

# ингредиенты регистрируются через хелпер of("id", "Name") -> ingredient/<id>
with io.open(os.path.join(REG, 'IngredientItems.java'), encoding='utf-8') as f:
    ing_src = f.read()
for m in re.finditer(r'=\s*of\(\s*"([^"]+)"\s*,', ing_src):
    items.append(('ingredient/' + m.group(1), 'IngredientItem'))

for path, modid in [('src/main/java/com/robertx22/infinite_dungeons/main/DungeonItems.java', 'infinite_dungeons'),
                    ('src/main/java/com/robertx22/divine_missions/main/ModItems.java', 'divine_missions')]:
    with io.open(os.path.join(ROOT, path), encoding='utf-8') as f:
        src = f.read()
    for m in re.finditer(r'ITEMS\.register\(\s*"([^"]+)"\s*,\s*\(\)\s*->\s*new\s+(\w+)', src):
        items.append((modid + ':' + m.group(1), m.group(2)))

# регистрации в циклах по enum-ам (id собирается из полей enum-а)
dynamic = [
    ('gems/<тип>/<ранг> (GemType x GemRank)', 'GemItem'),
    ('runes/<id> (RuneType)', 'RuneItem'),
    ('mat/smelted/0..4', 'SmeltedEssenceItem'),
    ('mat/condensed_salvage/c0..4', 'CondensedEssenceItem'),
    ('mat/salvage/salvage0..4', 'SalvagedDustItem'),
    ('stone/0..4', 'MiningStoneItem'),
    ('craft_tools/<профессия> (6 шт.)', 'CraftToolItem'),
    ('craft_result/<профессия>/<n> (13 шт.)', 'CookingConsumableItem'),
    ('alchemy/potion/<тип>/<тир> (10 шт.)', 'AlchemyPotionItem'),
    ('backpack/upgrade/<вид><тир> (14 шт.)', 'BackpackUpgradeItem'),
    ('weapon/staff/<материал> (4 шт.)', 'StaffWeapon'),
    ('jewelry/ring/<материал> (3 шт.)', 'ItemRing'),
    ('jewelry/necklace/<материал> (3 шт.)', 'ItemNecklace'),
    ('currency/<вид> (17 шт.)', 'CurrencyItem'),
]


def source(item_id, cls):
    ns = 'mmorpg' if ':' not in item_id else item_id.split(':')[0]
    path = item_id.split(':', 1)[1] if ':' in item_id else item_id
    dotted = path.replace('/', '.')
    key = PREFIX + dotted if ns == 'mmorpg' else PREFIX + ns + '.' + dotted
    if key in RU and key in EN:
        return 'свой текст', key
    fam = family_for(cls)
    if fam:
        fk = PREFIX + 'family.' + fam
        if fk in RU and fk in EN:
            return 'семейство', fk
        return 'НЕТ (нет ключа семейства)', fk
    return 'НЕТ', '-'


rows, seen = [], set()
for item_id, cls in items:
    if item_id in seen:
        continue
    seen.add(item_id)
    kind, key = source(item_id, cls)
    rows.append((item_id, cls, kind, key))

dyn_rows = []
for label, cls in dynamic:
    fam = family_for(cls)
    fk = (PREFIX + 'family.' + fam) if fam else None
    ok = fk and fk in RU and fk in EN
    dyn_rows.append((label, cls, 'семейство' if ok else 'НЕТ', fk or '-'))

missing = [r for r in rows if r[2].startswith('НЕТ')] + [r for r in dyn_rows if r[2] == 'НЕТ']

# ---------------------------------------------------------------- отчёт
lines = ['# Покрытие описаний предметов', '',
         'Сгенерировано `tools/audit_item_descriptions.py`: каждый зарегистрированный предмет',
         'прогоняется через ту же логику, что и в игре (`ItemDescriptions.itemKey` → `familyKey`).', '',
         '## Сводка', '',
         '| | шт. |', '|---|---|',
         '| статических id | %d |' % len(rows),
         '| динамических групп регистрации | %d |' % len(dyn_rows),
         '| без описания | **%d** |' % len(missing), '',
         '## Предметы', '',
         '| id | класс | источник | ключ |', '|---|---|---|---|']
for r in sorted(rows):
    src_txt = r[2] if r[2] != 'НЕТ' else '**НЕТ**'
    lines.append('| `%s` | %s | %s | `%s` |' % (r[0], r[1], src_txt, r[3]))

lines += ['', '## Динамические регистрации (циклы по enum-ам)', '',
          '| группа | класс | источник | ключ |', '|---|---|---|---|']
for r in dyn_rows:
    lines.append('| %s | %s | %s | `%s` |' % (r[0], r[1], r[2], r[3]))

lines += ['', '## Что получает описание кроме индивидуальных ключей', '',
          'Снаряжение с данными (`GearItemData`) тоже получает описание по Alt',
          '(`mmorpg.item.desc.family.gear` добавляется после статов), но подсказку',
          '`[Alt] — описание` для него не рисуем: тултип и так длинный.', '']

if missing:
    lines += ['## Осталось без описания', '']
    for m in missing:
        lines.append('- `%s` (%s)' % (m[0], m[1]))
    lines.append('')

with io.open(os.path.join(ROOT, 'ITEM_DESCRIPTION_COVERAGE.md'), 'w', encoding='utf-8') as f:
    f.write('\n'.join(lines))

print('статических id: %d, динамических групп: %d, без описания: %d'
      % (len(rows), len(dyn_rows), len(missing)))
for m in missing:
    print('  !', m)
print('отчёт: ITEM_DESCRIPTION_COVERAGE.md')
raise SystemExit(1 if missing else 0)
