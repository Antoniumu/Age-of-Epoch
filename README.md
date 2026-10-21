# Age of Epoch — порт Age of Exile на Minecraft 26.3 (Fabric)

Полный порт мода **[Age of Exile](https://github.com/RobertSkalko/Age-of-Exile)** (MMORPG-хак-н-слэш для Minecraft 1.16.5, Forge)
на **Minecraft 26.3 «Wilderness Bound» + Fabric**, с **полной русской локализацией**.

В состав порта включены (как единый мод):
- **Age of Exile** — ядро мода (заклинания, характеристики, аффиксы, снаряжение, данжи, карты приключений, профессии…);
- **Library of Exile** — общая библиотека (реестры JSON-базы данных, пакеты, капабилити, события);
- **Divine Missions** и **Infinite Dungeons** — аддоны, интегрированные в исходниках Age of Exile.
- **Тематические башни** — 16 тем по биомам, 3 уровня сложности и 80 боссов; см. [`TOWERS.md`](TOWERS.md).

## Статус

| Часть | Статус |
|---|---|
| Структура проекта Fabric 26.3, сборка (Loom 1.17, Gradle 9.6.1, Java 25, Fabric Loader 0.19.5, Fabric API 0.163.0+26.3) | ✅ |
| Миграция 1.16.5 → 26.3 Mojmap (1396+ java-файлов, автоматический скрипт `tools/port_to_fabric.py`) | ✅ |
| Ядро: entrypoints, реестры, сеть, капабилити, события (слой `com.robertx22.forge_compat`) | ✅ |
| Полная русская локализация `ru_ru.json` — 1622 + 51 + 39 ключей, 0 пропусков, плейсхолдеры сохранены | ✅ |
| Финальная компиляция/запуск (ANT-50) — см. `PORTING.md` | ⚠️ требуется локальная сборка |

> Сборка Gradle **не выполнялась** в среде разработки (нет доступа к Maven-репозиториям Mojang/Fabric).
> Оставшиеся известные пункты доводки компиляции перечислены в [`PORTING.md`](PORTING.md).

## Сборка

Требования: **JDK 25**, доступ в интернет к `maven.fabricmc.net` и Mojang-репозиториям.

```bash
./gradlew build          # результат: build/libs/Age-of-Epoch-*.jar
./gradlew runClient      # запуск клиента для проверки
```

## Управление

Всё управление прежнее (кнопка H — хаб, панель заклинаний и т.д.), перевод выполнен
с сохранением цветовых кодов (§) и плейсхолдеров (`%1$s`, `[VAL1]`, `[calc:...]`).

## Структура репозитория

```
src/main/java/com/robertx22/age_of_exile/     — ядро мода (перенесено и мигрировано)
src/main/java/com/robertx22/library_of_exile/ — библиотека
src/main/java/com/robertx22/divine_missions/   — аддон «Божественные миссии»
src/main/java/com/robertx22/infinite_dungeons/ — аддон «Бесконечные подземелья»
src/main/java/com/robertx22/forge_compat/      — адаптер Forge API → Fabric (см. PORTING.md)
src/main/java/com/robertx22/world_of_exile/    — заглушки (World of Exile не входит в порт)
src/main/resources/                            — ресурсы, данные, fabric.mod.json, локализация
tools/port_to_fabric.py                        — воспроизводимый скрипт миграции
tools/ru_work/                                 — исходники перевода (генерация ru_ru.json)
tools/tower_gen/                               — генератор тематических башен (NBT + JSON), см. TOWERS.md
TOWERS.md                                      — описание тем, сложности и боссов башен
```

## Авторство

Оригинальный мод: **mcrobertx22 (Robert Skalko)** — [RobertSkalko/Age-of-Exile](https://github.com/RobertSkalko/Age-of-Exile).
Порт на Minecraft 26.3 и русская локализация: **Antoniumu**, 2026.
