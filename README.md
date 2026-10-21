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

На Windows удобнее `build.bat` (сам находит JDK 25, кэши держит в `.gradle-home`):

```bat
build.bat                :: собрать мод
build.bat repair         :: починить сборку: сброс кэшей Loom/Minecraft + --refresh-dependencies
build.bat diagnose       :: build-diagnose.txt: видит ли javac Minecraft на classpath
```

Если `compileJava` сыплет `package net.minecraft.* does not exist` и падает с
`NullPointerException` в `TypeElement.getNestingKind()` — сначала проверьте Loom-артефакт
Minecraft: в описанном ниже случае причиной был пустой jar, а не исходники:

```bat
build.bat diagnose                :: build-diagnose.txt: на каком шаге jar стал пустым
build.bat probe                   :: повторяет шаг слияния Loom и печатает скрытое исключение
build.bat jcheck                  :: сравнить JDK: не врёт ли Files.isWritable()
build.bat repair                  :: сброс кэшей + --refresh-dependencies
build.bat singlejar client        :: собрать без слияния client+server (если zipfs в порядке)
```

`minecraft-merged-<hash>-26.3.jar (22 bytes)` — пустой zip: скачанные client/server целы,
а слияние Loom пишет пустоту и молча теряет исключение (`MinecraftJarMerger` не сохраняет
`Future` своих задач). На JDK 25 zipfs объявляет jar read-only, как только
`Files.isWritable()` вернёт `false`, и первая же запись падает с `ReadOnlyFileSystemException`.
Если `build.bat probe` показывает `Files.isWritable = false` там, где запись реально проходит —
проверка доступа расходится с фактической записью. `build.bat jcheck` сравнит найденные JDK;
рабочий нужно поставить
в `.jdks\` или указать в `JAVA_HOME`. Подробности — [`BUILD-REPORT.md`](BUILD-REPORT.md) §6.

### Выбор рабочего JDK 25

`build.bat` пропускает JDK, не прошедшие проверку записи в проект и Gradle-кэш.
`build.bat jcheck` выдаёт сводку без дублей: JDK 21 можно сравнить, но собирать нужно 25+.
Ошибки отдельных путей больше не скрываются успешной проверкой другого пути.

```bat
build.bat getjdk microsoft
build.bat jcheck
build.bat repair
```

Для установки Microsoft требуется winget с пакетом `Microsoft.OpenJDK.25`.
`getjdk` без аргумента по-прежнему скачивает Temurin; переменная `AOE_JDK_URL`
позволяет указать доверенный HTTPS ZIP другого JDK. После установки выполняется проверка.
Подробности и ограничения диагностики — в дополнении к §6 `BUILD-REPORT.md`.

### Разбор нетипичного сбоя сборки (2026-10-21)

**Статус: сборка успешно завершена на машине пользователя.** Windows 10,
Microsoft JDK 25.0.4.1: `BUILD SUCCESSFUL in 1m 1s`, артефакт
`build/libs/Age-of-Epoch-5.0.0.jar`, **6 966 555 байт**.

Сбой состоял из нескольких независимых проблем, скрывавших друг друга:

1. **Противоречивая проверка доступа.** Temurin 25.0.4.1 возвращал
   `Files.isWritable() = false` для проекта и Gradle-кэша, хотя `File.canWrite()`
   возвращал `true`, а создание и удаление файлов проходило. В `%TEMP%` реальная
   операция тоже падала с `AccessDeniedException`. Microsoft JDK 25.0.4.1 на тех же
   путях успешно проходил все проверки; Temurin 21 тоже проходил, но для сборки
   Minecraft 26.3 он слишком старый.
2. **Read-only ZIP-файловая система.** В JDK 25 `zipfs` учитывает
   `Files.isWritable()` при выборе режима архива. Отрицательный ответ делал jar
   доступным только для чтения, и запись Loom падала с `ReadOnlyFileSystemException`.
3. **Потерянное исключение Loom.** В исследованном коде `MinecraftJarMerger`
   результаты фоновых задач не проверялись через `Future`, поэтому ошибка записи
   терялась. Оставался валидный по формату, но пустой ZIP — **22 байта, 0 записей**.
   Отсюда вторичные ошибки компилятора об отсутствующих пакетах Minecraft.
   `singlejar` не помогал: последующие jar-процессоры тоже использовали zipfs.
4. **Настоящие ошибки исходников.** После замены JDK и пересоздания кэша Minecraft
   jar вырос до **37,7 МБ**. Компиляция выявила три отсутствующих импорта:
   `ITrinketType` в `MyTrinketUtils` и `TrinketEvents`, `Gear` в `TrinketEvents`.
   После их добавления сборка завершилась успешно.

**Необычная особенность:** две сборки одной версии Java от разных поставщиков
вели себя по-разному на одной машине. Это установленный результат сравнения,
но не окончательный диагноз внутренней причины: дефект конкретной сборки JDK
и взаимодействие с Windows, политиками доступа или защитным ПО не разграничены.
Раннее категоричное исключение антивируса из причин было необоснованным.

Для предотвращения повторения добавлены `probe`, `jcheck`, предварительная проверка
и пропуск неподходящих JDK, а также установка альтернативного JDK. Переименование
плохого JDK внутри `.jdks` не исключает его из поиска: каталог нужно переместить
за пределы сканируемых путей либо явно выбрать рабочий `JAVA_HOME`.

Оставшиеся сообщения о deprecated API, unchecked operations и несовместимости
устаревших Gradle-возможностей с будущим Gradle 10 — **предупреждения, не ошибки
текущей успешной сборки**. Их следует разбирать отдельно перед обновлением.
Успешная сборка подтверждает создание jar, но не заменяет проверку загрузки мода
и его работы в игре.

**Вывод:** валидный архив не обязательно содержит нужные классы, последняя ошибка
в логе может быть лишь следствием, а возможность обычной записи не гарантирует
работу другого файлового API. Подробная диагностика — в [`BUILD-REPORT.md`](BUILD-REPORT.md) §6.

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
