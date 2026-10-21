# Отчёт о сборке Age of Epoch (ANT-50)

Дата: 2026-10-20. Рабочая папка `C:\Code\Age-of-Epoch`.
Связка: JDK 25.0.4.1, Gradle 9.7.1, Fabric Loom 1.18.4, Minecraft 26.3, Fabric API 0.163.0+26.3.

> **Обновление 2026-10-21 — исправления башен:** результат сборки ниже относится к
> предыдущему состоянию проекта. Текущие изменения генерации и ИИ проверены статически;
> Java 25/Gradle и игровой запуск в песочнице недоступны. Для этой правки `gradlew build`
> и проверка в новом мире ещё требуются; сценарий — в `TOWERS.md`, §9.

## 1. Результат

```
BUILD SUCCESSFUL in 1m 11s
build/libs/Age-of-Epoch-5.0.0.jar          5.4 МБ  (1916 классов, 2060 assets, 124 data)
build/libs/Age-of-Epoch-5.0.0-sources.jar  3.8 МБ
```

Мод компилируется и собирается в jar. Запуск (`runClient`) не проверялся — см. §4.

## 2. Что было сделано в этом заходе

| Шаг | Ошибок |
|---|---|
| Состояние на начало работы | 2583 |
| Перегенерация списка исходников (в старом не хватало 30 новых файлов) | 1313 «фантомных» убрано |
| 7 синтаксических ошибок от прошлых автозамен | 122 |
| Механические правки API (см. §3) | 48 |
| Точечные правки по файлам | 1 |
| **Итог** | **0** |

## 3. Основные классы правок под API Minecraft 26.3

- `ResourceLocation` → `Identifier` (195 файлов).
- Поля, ставшие приватными → методы: `entity.level()`, `level.isClientSide()`, `getYRot()/getXRot()`,
  `ChunkPos.x()/z()`, `AbstractWidget.getX()/getY()`, `Screen.children()`.
- Переехавшие/переименованные классы: `GameRules` → `net.minecraft.world.level.gamerules` (+ `get/set`
  вместо `getRule`), `UseAnim`/`Tier`/`MobSpawnType`/`LootTables`/`MultiBufferSource` — заменены на
  доступные аналоги или на локальные заглушки слоя `forge_compat`.
- Изменённые сигнатуры: `Block.use` → `useItemOn`, `Block.entityInside` (+`InsideBlockEffectApplier`),
  `Item.inventoryTick` (ServerLevel + EquipmentSlot), `LivingEntity.drop` (+Prediction),
  `Inventory.placeItemBackInInventory` (+Prediction), `Entity.spawnAtLocation` (+ServerLevel),
  `LootContext.getOptionalParameter` → `getOptional`, `PoseStack.mulPose` (Matrix4f),
  `Minecraft.setScreen` → `setScreenAndShow`, `MinecraftServer.getStructureManager` →
  `getStructureTemplateManager`, `TextureManager.bind` → удалено (рендер-пайплайн 26.3).
- `BlockBehaviour.Properties.of()` без `Material` (материалов больше нет), `noCollission` → `noCollision`.

## 4. Что осталось недоделанным (важно)

Это компилируемый, но **не протестированный в игре** порт. Осознанно оставленные заглушки:

1. **`forge_compat/client/MenuScreensCompat`** — `MenuScreens.register` в 26.3 приватный, поэтому
   регистрация экранов контейнеров (рюкзак, рунная станция, сумка, крафт) идёт через рефлексию и
   динамический прокси. Компилируется, но в рантайме не проверялось; если прокси над приватным
   интерфейсом недопустим, экраны не откроются (нужен access widener или миксин).
2. **`DungeonDimensionJigsaw.createPoolAndInitPools()`** возвращает `null` — старый `AbstractPool`
   не умеет строить `StructureTemplatePool` в 26.3 (нужен codec-based worldgen).
3. **`SkillTreeScreen.renderBackgroundDirt`** — тело метода удалено (в 26.3 нет immediate-mode
   `Tessellator/BufferBuilder`), фон дерева навыков сейчас не рисуется.
4. **GUI/рендер в целом** работает через слой `forge_compat` (`AbstractGui`, `RenderSystem`,
   `GuiGraphicsExtractor`) — визуально не проверялось.
5. **Миксины** компилируются, но их таргеты в рантайме не валидировались (`SignBlockEntityDuckMixin`,
   `LivingEntityMixin` использует `BYPASSES_EFFECTS` вместо удалённого `BYPASSES_MAGIC`).

Следующий практический шаг: `./gradlew runClient` и починить то, что упадёт при инициализации
(миксины, регистрация экранов, worldgen).

## 5. Как собирать

Самый простой способ — `build.bat` в корне проекта (сам находит JDK 25, кэши Gradle держит
в `.gradle-home`, в конце печатает путь к собранному jar):

```bat
build.bat                 :: собрать мод
build.bat clean           :: чистая сборка
build.bat runClient       :: запустить клиент для проверки
build.bat getjdk          :: скачать Temurin JDK 25 в .jdks (нужен только один раз)
```

Вручную:

```powershell
$env:JAVA_HOME = "<JDK 25>"
$env:GRADLE_USER_HOME = "C:\Code\Age-of-Epoch\.gradle-home"
.\gradlew.bat build --no-problems-report
```

`--no-problems-report` отключает HTML-отчёт Gradle о проблемах сборки: он не нужен, а именно
его запись ломается, если каталог `build/` недоступен на запись.

## 5a. Автообновление, деплой в ElyPrism и запуск

`watch.bat` опрашивает git-upstream, забирает новые коммиты и после каждой успешной сборки
кладёт jar в инстанс и запускает его:

```bat
watch.bat            :: опрос раз в 60 секунд
watch.bat 30         :: опрос раз в 30 секунд
```

Что происходит на каждом тике:

1. `git fetch`; если upstream ушёл вперёд — `git pull --ff-only`;
2. если HEAD изменился (или это первый запуск) — `build.bat build`;
3. после успешной сборки `deploy.bat launch`: старые `Age-of-Epoch-*.jar` в
   `<инстанс>\minecraft\mods` удаляются, новый jar копируется на их место;
4. инстанс запускается через CLI лаунчера:
   `ElyPrismLauncher.exe --launch "26.3" --profile "Antoniumu"`.

Всё пишется в `autobuild.log`. Локальные коммиты тоже пересобираются (сравнение идёт с
последним собранным HEAD, а не только с upstream). Если `git pull` не проходит из-за локальных
правок, наблюдатель пишет об этом и продолжает опрос.

Переопределяемые переменные: `AOE_INSTANCE` (26.3), `AOE_PROFILE` (Antoniumu),
`AOE_MODS_DIR`, `AOE_LAUNCHER`. Отключатели: `AOE_NO_DEPLOY=1` (только сборка),
`AOE_NO_LAUNCH=1` (деплой без запуска игры).

Быстрый цикл проверки компиляции без Gradle (полный список ошибок, ~40 c):

```powershell
.\gradlew.bat dshClasspath          # обновляет build/dsh-cp.txt и build/dsh-sources.txt
javac -J-Xmx3G -Xmaxerrs 20000 -encoding UTF-8 --release 25 -proc:none `
      -cp @build/dsh-cp.txt -d build/dsh-classes @build/dsh-sources.txt
```

Задача `dshClasspath` и флаг `-PdshMaxErrs` — вспомогательные, добавлены в `build.gradle`.

## 6. Если `compileJava` не видит Minecraft (обновление 2026-10-21)

Симптом ровно один и он узнаваем:

```
error: package net.minecraft.network.chat does not exist
error: package net.minecraft.world.entity.player does not exist
...
> java.lang.NullPointerException: Cannot invoke
  "javax.lang.model.element.TypeElement.getNestingKind()" because "typeElement" is null
```

Это **не ошибки исходников**. Оба признака — следствие одного: на compile classpath нет
(или он пуст/обрезан) remap-нутый jar Minecraft, который Loom кладёт в
`.gradle\loom-cache\minecraftMaven\...`. javac тогда не находит ни одного `net.minecraft.*`,
а annotation processor миксинов падает с NPE, потому что поиск целевого класса возвращает null.

Доказательство, что дело не в коде: `build.gradle` и `gradle.properties` между успешной
сборкой (§1) и этим логом не менялись (в PR #59 добавлены только строки про Trinkets),
а ни один java-файл не может «потерять» ванильные пакеты.

### Лечение

```bat
build.bat repair      :: удаляет .gradle\loom-cache, кэши fabric-loom / com.mojang /
                      :: net.minecraft / net.fabricmc и пересобирает с --refresh-dependencies
```

Вручную то же самое:

```powershell
Remove-Item -Recurse -Force .gradle\loom-cache
Remove-Item -Recurse -Force "$env:GRADLE_USER_HOME\caches\fabric-loom"
Remove-Item -Recurse -Force "$env:GRADLE_USER_HOME\caches\modules-2\files-2.1\com.mojang"
.\gradlew.bat build --refresh-dependencies --no-problems-report
```

Если после `repair` картина та же, удалите кэши целиком и соберите снова (дольше, но
гарантированно с нуля):

```powershell
Remove-Item -Recurse -Force .gradle, .gradle-home
.\gradlew.bat build --refresh-dependencies --no-problems-report
```

Сброс кэшей помогает, когда виноват **залежавшийся** (обрезанный) jar: пересборка скачивает
манифест версии, библиотеки и заново ремапит Minecraft (самый долгий шаг, 1–3 минуты).
Если же jar каждый раз создаётся заново пустым (см. следующий раздел), сброс сам по себе
не поможет — нужна причина.

### Что было в реальном прогоне (2026-10-21)

После `build.bat repair` кэши пересоздались, проверка сказала:

```
Minecraft classes are missing from the compile classpath
(the Minecraft jar(s) on the compile classpath are empty or truncated:
 ...\minecraft-merged-80b2a6d18e-26.3.jar (22 bytes))
```

А `build.bat diagnose` показал, где именно всё рассыпается:

| Шаг | Файл | Размер | Записей `net/minecraft/` |
|---|---|---|---|
| скачивание клиента | `caches\fabric-loom\26.3\minecraft-client.jar` | 39.6 МБ | **11 266** ✅ |
| скачивание сервера | `caches\fabric-loom\26.3\minecraft-server.jar` | 59.4 МБ | 6 (это бандлер, так и должно быть) ✅ |
| **слияние** | `caches\fabric-loom\26.3\minecraft-merged.jar` | **0.0 МБ** | **0** ❌ |
| ремап + jar-процессоры | `...\minecraftMaven\...\minecraft-merged-80b2a6d18e-26.3.jar` | 22 байта | 0 |

Вывод: **скачанные jar'ы целы, ломается слияние**. 22 байта — это пустой zip (только
end-of-central-directory), то есть файл создан, но ни одной записи в него не попало.
Сброс кэшей тут не поможет: он каждый раз честно создаёт файл заново и снова пустым.

Почему Loom молчит. В `MinecraftJarMerger.merge()` (loom 1.18) задачи отправляются в
executor **без сохранения `Future`**:

```java
try (ExecutorService service = Executors.newFixedThreadPool(2)) {
    service.submit(() -> readToMap(entriesClient, inputClient));   // результат никто не проверяет
    service.submit(() -> readToMap(entriesServer, inputServer));
}
entriesAll.addAll(entriesClient.keySet());
entriesAll.addAll(entriesServer.keySet());
try (ExecutorService service = Executors.newWorkStealingPool()) {
    for (String entry : entriesAll) processEntry(entry, service);  // add() тоже async
}
```

Любое исключение внутри `readToMap()` / `add()` просто теряется: конструктор уже создал
пустой выходной zip, поток закрывается — и на выходе валидный пустой jar без единой строки
в лог. Дальше ремап «успешно» перемапывает пустоту, на classpath попадает 22-байтный jar,
javac не видит `net.minecraft.*`, а annotation processor миксинов падает с тем самым
`NullPointerException ... getNestingKind()`.

### `build.bat probe` — показать то, что Loom скрывает

`tools/MergeProbe.java` повторяет те же примитивы тем же JDK 25 и печатает настоящее
исключение:

```bat
build.bat probe
```

Что делает проба:

1. перечисляет jar'ы в `caches\fabric-loom\<version>`;
2. открывает client/server через **тот же zipfs API**, что и `readToMap()`, и обходит их
   `walkFileTree` — падение здесь и есть причина пустого merge;
3. пишет zip через zipfs (`create=true`, `CREATE_NEW`, `setTimes`) — так делает
   `MinecraftJarMerger.add()`, — затем переоткрывает и считает записи;
4. контроль: те же данные через `java.util.zip` — чтобы отличить проблему zipfs от
   проблемы диска/антивируса;
5. печатает итог и код возврата: `0` — все примитивы работают, `1` — что-то упало.

Запуск в режиме одного файла (`java tools\MergeProbe.java`), компилятор не нужен, пишет
только в `build\probe\`. Файл можно запустить и руками:

```powershell
& "$env:JAVA_HOME\bin\java" -Dprobe.projectDir=. tools\MergeProbe.java
```

Дальше по результату:

- упало в п.2 (открытие client.jar) → битый **скачанный** jar: удалить `minecraft-client.jar`,
  `minecraft-server.jar`, `minecraft-merged.jar` и `build.bat repair`;
- упало только в п.3, а п.4 прошёл → виноват zipfs самого JDK: попробовать другой JDK 25
  (Corretto/Zulu/Microsoft) в `.jdks\`, `build.bat` подхватит его автоматически;
- упало и в п.3, и в п.4 → диск/антивирус: исключение для папки проекта и `.gradle-home`,
  проверить свободное место;
- всё прошло → ошибка внутри Loom; приложить вывод пробы и
  `build.bat build --info --stacktrace > build-log.txt`.

### `build.bat diagnose` — какой шаг виноват

```bat
build.bat diagnose    :: пишет build-diagnose.txt
```

Отчёт (JDK, свободное место, `gradle.properties`, jar'ы Loom, кэш `modules-2`, разобранный
compile classpath) заканчивается блоком `=== Verdict`:

- `[OK] Minecraft classes are on the compile classpath` — Minecraft на месте, значит ошибки
  действительно в исходниках; полный их список без ограничения в 100 штук:
  `build.bat compileJava -PdshMaxErrs=4000`;
- `Minecraft is NOT on the compile classpath` — ни один minecraft-jar не разрешился;
  смотреть `gradlew dependencies --configuration compileClasspath`;
- `The merge step is the broken link` — скачанные client/server целы, но слияние дало пустой
  jar (см. выше): `build.bat probe` покажет скрытое исключение;
- `The remap/jar-processing step is the broken link` — merge в порядке, пустой уже
  обработанный jar: диск / антивирус / память Gradle / сбой jar-процессора.

Полный лог сборки (приложить к issue, если причина не нашлась):

```bat
build.bat build --info --stacktrace > build-log.txt
```

С этого момента сборка сама проверяет это состояние: задача `checkMinecraftClasspath`
(перед `compileJava`) падает с одной понятной строкой вместо сотни ложных ошибок.
Обойти проверку, если она вдруг сработает на здоровой сборке: `-PskipMcCheck`.
