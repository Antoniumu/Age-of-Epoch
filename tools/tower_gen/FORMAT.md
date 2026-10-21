# Формат шаблонов башен: Minecraft 26.3

Целевая версия — **DataVersion 5023**. Палитра структуры использует ключи:

```snbt
{ id: "minecraft:stone_bricks" }
{ id: "minecraft:jigsaw", properties: { orientation: "up_east" } }
```

`Name` и `Properties` — **старый формат**, несовместимый с заявленной версией 5023.
Нельзя просто поставить текущий `DataVersion` на старую схему: это отключает
необходимую миграцию. Старые оригинальные шаблоны с DataVersion 2567 не переписываем:
для них игра может применить штатный data fixer.

## Подтверждение из независимых источников

- Ванильный шаблон 26.3 `village/plains/town_centers/plains_fountain_01.nbt`:
  [данные Minecraft, зафиксированный снимок mcmeta](https://github.com/misode/mcmeta/blob/538b2b167248c648b2198f2c0d56eced10dfc0cf/data/minecraft/structure/village/plains/town_centers/plains_fountain_01.nbt).
  В нём DataVersion 5023, `palette[*].id` и `palette[*].properties`.
- [NbtUtils.java из декомпилированного Minecraft 26.3](https://github.com/0khs/minecraft_source_26.3/blob/74720c2890f22942b72d7d84958eff1b3f72b95b/net/minecraft/nbt/NbtUtils.java):
  `readBlockState` читает `id`; при отсутствии/неизвестном ID возвращает AIR.
  Свойства читает из `properties`. `writeBlockState` пишет эти же ключи.
- [StructureTemplate.java той же версии](https://github.com/0khs/minecraft_source_26.3/blob/74720c2890f22942b72d7d84958eff1b3f72b95b/net/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate.java):
  `loadPalette` вызывает `NbtUtils.readBlockState` для каждой записи палитры.

Источники проверены 2026-10-21. Полный исходный код игры и ванильные NBT в репозиторий
мода не копируются; ссылки фиксируют версию, по которой проверен контракт.

## Воспроизведение ошибки «выемка вместо башни»

Прежние 96 тематических шаблонов имели DataVersion 5023, но записывали `Name` /
`Properties`. При чтении по правилу загрузчика 26.3 **все записи палитры становились
воздухом**. Терялись также jigsaw-блоки, поэтому этажи не соединялись. При этом запись
структуры и её стартовый bounding box существовали, и `/locate` мог находить её.
Это согласуется с выемкой на месте стартовой секции и отсутствием башни.

После исправления ключей, без изменения геометрии или частоты генерации:

| Проверка 96 шаблонов по ключу `id` | До | После |
|---|---:|---:|
| Блоков, отличных от воздуха | 0 | 100877 |
| Jigsaw-соединителей | 0 | 160 |

Это проверка схемы и содержимого NBT, **не запуск игрового движка**.

## Защита от повторения

- `validate.py` отклоняет старые ключи, неверную версию, неправильный тип свойств,
  отсутствие `id` и ориентации соединителей.
- `test_regressions.py` содержит эталон палитры с буквальными ключами 26.3 (не
  константами из генератора), регрессию старой схемы, проверки всех 96 файлов.
- Геометрические проверки и проверки биомов дополняют, но не заменяют проверку схемы.

```bash
python3 tools/tower_gen/generate.py
python3 tools/tower_gen/validate.py
python3 -m unittest discover -s tools/tower_gen -p 'test_*.py' -v
```

После сборки нового jar и перезапуска игры естественную генерацию проверять в новых
чанках. Существующие пустые старты не восстанавливаются сами. Для быстрой отдельной
проверки в **тестовом мире**: `/place structure world_of_exile:plains_t1 ~ ~ ~`.
Команда изменяет блоки вокруг точки размещения и не проверяет естественный выбор биома.
