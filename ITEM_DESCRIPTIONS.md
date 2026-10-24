# Описания предметов (Alt)

У всех предметов мода, у которых не было внятного описания, теперь есть текст,
который раскрывается **по Alt** в тултипе. Обычный тултип при этом почти не растёт —
в нём висит только одна серая строка-подсказка.

## Как это работает

Единственная точка входа — `TooltipMethod` (его вызывает миксин `ItemStackMixin`
на `ItemStack#getTooltipLines`):

1. Собирается обычный тултип предмета (ванильный + модовый, включая эссенции,
   души, валюту и т.д.).
2. Затем `ItemDescriptions.appendToTooltip(...)` добавляет описание:
   * **Alt** (без Shift) — блок описания: пустая строка + строки текста;
   * без Alt — короткая подсказка `[Alt] — описание предмета`, и только у тех
     предметов, у которых описание вообще есть и не видно иначе.
3. Снаряжение с данными (`GearItemData`) описание получает, но подсказку `[Alt]`
   ему не рисуем: тултип и так длинный, а Alt там уже занят описаниями характеристик
   (`TooltipInfo.shouldShowDescriptions()`).

Клавиши не конфликтуют: **Alt** — описания, **Shift** — подробные статы/требования,
**Ctrl** — компактный вид снаряжения.

## Где лежат тексты

Тексты — обычные ключи локализации, русский (`ru_ru.json`) — основной,
английский (`en_us.json`) — фолбэк. Многострочность — через `\n`.

| Что | Ключ | Пример |
|---|---|---|
| Конкретный предмет | `mmorpg.item.desc.<путь предмета>` | `mmorpg.item.desc.currency.orb_of_transmutation` |
| Семейство предметов | `mmorpg.item.desc.family.<семейство>` | `mmorpg.item.desc.family.gem` |
| Подсказка в тултипе | `mmorpg.word.press_alt_for_item_description` | `[Alt] — описание предмета` |

Путь предмета — это его registry id без неймспейса, где `/` заменён на `.`:
`mat/golden_orb` → `mmorpg.item.desc.mat.golden_orb`.
Для предметов встроенных сабмодов неймспейс входит в ключ, чтобы пути не пересекались:
`infinite_dungeons:skeletal_coin` → `mmorpg.item.desc.infinite_dungeons.skeletal_coin`.

Приоритет: сначала ищется описание конкретного предмета, затем семейства.
Если нет ни того, ни другого — ничего не показывается (и подсказки тоже нет).

## Семейства

Семейное описание пишется один раз и работает для всех предметов этого класса —
это спасает от сотни почти одинаковых текстов (камни, руны, ингредиенты, эссенции).

| Семейство | Что покрывает |
|---|---|
| `family.gem` | все самоцветы (`gems/<тип>/<ранг>`) |
| `family.rune` | руны (`runes/*`) |
| `family.ingredient` | ингредиенты профессий (`ingredient/*` и стеки с `IngredientData`) |
| `family.smelted_essence` | `mat/smelted/*` |
| `family.condensed_essence` | `mat/condensed_salvage/*` |
| `family.mining_stone` | мистические камни (`stone/*`) |
| `family.salvage_dust` | очищенная эссенция (`mat/salvage/*`) |
| `family.craft_tool` | инструменты профессий |
| `family.backpack` / `family.material_pouch` | сумка и мешок материалов |
| `family.backpack_upgrade` | улучшения сумки |
| `family.stat_soul` | души снаряжения |
| `family.crafted_consumable` | результаты крафта профессий (`craft_result/*`) |
| `family.alchemy_potion` | зелья алхимии (`alchemy/potion/*`) |
| `family.currency` | валюта и камни улучшения редкости |
| `family.loot_crate` | сундуки с наградами |
| `family.tiered` | прочие ранговые материалы |
| `family.block` | блоки мода |
| `family.gear` | снаряжение без данных (базовые предметы) |
| `family.dungeon_key` | ключи Бесконечных подземелий |
| `family.dungeon_coin` | монеты Бесконечных подземелий |

## Предметы с индивидуальным описанием

Валюта, самоцветы, руны, ингредиенты и т.п. описываются семействами, а вот
уникальные предметы получили свой текст:

* `common_soul_produce`, `identify_tome`, `stat_soul/stat_soul`;
* `loot_crate/default`, `custom_crate`, `loot_table_chest`, `newbie_gear_bag`;
* `source_of_strength`, `rarity/essence/base`, `rarity/upgrade_stone/base`;
* `projectile/fireball`, `projectile/snowball`, `projectile/slimeball`;
* `scroll/paper`, `misc/teleport_back`;
* `full_favor`, `empty_favor`;
* `salvage_hammer`, `socket_extractor`;
* `mat/infused_iron`, `mat/crystallized_essence`, `mat/golden_orb`, `mat/mythic_essence`;
* `potions/reset_all_perks`, `potions/add_reset_perk_points`, `potions/reset_spells`,
  `potions/reset_stats`, `potions/death_potion`;
* `backpack/normal`, `backpack/material_pouch`;
* `teleporter`, `runeword_station`, `portal`;
* `infinite_dungeons:reward_crate` (сундук наград подземелий);
* `divine_missions:mission` (задание божества), `divine_missions:shrine` (святилище).

## Как добавить описание новому предмету

1. Возьмите его registry id, например `mmorpg:mat/new_thing`.
2. Добавьте в `ru_ru.json` ключ `mmorpg.item.desc.mat.new_thing` и такой же в `en_us.json`.
   Строки разделяйте `\n`.
3. Если предмет относится к существующему семейству — достаточно семейного текста,
   отдельный ключ не нужен.
4. Если у предмета описание уже видно в тултипе (валюта, инструменты, зелья алхимии,
   сундуки с наградой по таблице, молот разбора), его нужно добавить в
   `ItemDescriptions.alreadyExplained(...)` — тогда подсказка про Alt не будет висеть зря.

## Про генерацию языков

`en_us.json` может пересобираться генератором (`CreateLangFile`), который подмешивает
`manual.json` в начало файла. Поэтому все английские тексты описаний (и ключи JEI)
продублированы в `src/main/resources/assets/mmorpg/lang/manual.json` — при пересборке
они не потеряются. `ru_ru.json` генератор не трогает.

## Позиционные и артефактные ингредиенты

Ингредиенты в сетке крафта (3×2) влияют друг на друга: соседство по стороне может
как усиливать, так и ослаблять эффективность. У таких «позиционных» ингредиентов есть
свой текст, а не только семейный:

| Предмет | Что делает |
|---|---|
| `ingredient/bone_figurine` | +10% ингредиентам, которые её не касаются; −25% касающимся |
| `ingredient/blackpearl` | крит. урон, −20% касающимся |
| `ingredient/enigma_stone` | крит заклинаний, −25% касающимся |
| `ingredient/rock_jade` | +1 случайный стат, +мин. редкость, −10% успех, +20% касающимся (артефакт) |
| `ingredient/endergem` | +2 случайных стата, +мин. редкость, −25% успех, +20% касающимся (артефакт) |
| `ingredient/wishing_bowl` | +1 к минимальной редкости (артефакт) |
| `ingredient/pot_of_greed` | +2 к максимуму применений расходника (артефакт) |
| `ingredient/soul_matrix` | +20% к шансу успеха крафта (артефакт) |

Остальные ингредиенты описываются семейным текстом `family.ingredient`.

## Полное покрытие

Аудит по исходникам показывает **0 предметов без описания** (94 статических id + 14 групп
динамических регистраций). Таблица со всеми id и источником описания —
[`ITEM_DESCRIPTION_COVERAGE.md`](ITEM_DESCRIPTION_COVERAGE.md).

Перегенерация после добавления предметов:

```bash
python3 tools/audit_item_descriptions.py
```

Если забыть текст, скрипт завершится с кодом 1 и заполнит раздел
«Осталось без описания» — удобно вешать в CI.

## Ограничения

* Описания — клиентская часть (используют `I18n` и `Screen.hasAltDown()`);
  на сервере они не нужны и не вызываются.
* Компиляция проверена статически; в игре не тестировалось (в песочнице нет JDK 25
  и кэша Loom), поэтому возможны правки формулировок после первого запуска.
