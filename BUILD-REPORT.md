# Отчёт о сборке Age of Epoch (ANT-50)

Дата: 2026-10-20. Рабочая папка `C:\Code\Age-of-Epoch`.
Связка: JDK 25.0.4.1, Gradle 9.7.1, Fabric Loom 1.18.4, Minecraft 26.3, Fabric API 0.163.0+26.3.

> **Срез не относится к текущему HEAD.** Указанный `BUILD SUCCESSFUL` — сборка состояния
> от 2026-10-20. Последующие правки ветки, включая текущую ревизию, отдельно не компилировались;
> см. актуальный статус и тесты в [`LAUNCH-READINESS.md`](LAUNCH-READINESS.md).

> **Обновление 2026-10-21 — плашки мобов.** Добавлены три стиля плашек вместо одной
> жёстко заданной (`a_libraries/mob_plates/`: `MobPlateRenderer`, `PlateDraw`,
> `MobPlateSettings`; стиль — `config/forge/MobPlateStyle`). Опции клиента
> (`MOB_PLATE_*` в `ClientConfigs`) читаются из `config/age_of_epoch/mob_plates.properties`,
> стиль переключается клавишей **J**. Изменения проверены только статически: `gradlew build`
> и запуск клиента в песочнице недоступны (нет JDK 25/собранных jar-ов Minecraft).
> Перед релизом нужно: `./gradlew build`, затем `runClient` и проверка трёх стилей на
> дистанции, у боссов и при полном здоровье. Точки риска перечислены в §6a.

> **Обновление 2026-10-25 — плашки больше не спорят с ванильным ником.** `MODERN`/`RUNE`
> прижимались к голове и перекрывали ник, который Minecraft сам рисует игрокам и
> переименованным биркой мобам (`bbHeight + 0.5 … + 0.74`). Теперь `MobPlateRenderer`
> определяет такие существа (`Info.vanillaName`: игрок не в приседе и не в наблюдателе,
> либо `hasCustomName() && isCustomNameVisible()`), поднимает плашку над ником на
> `named_offset` (по умолчанию 0.06 блока поверх 0.74) и в режиме `COMPACT` (новый
> дефолт, `config/forge/MobPlateNameMode`) рисует её без повторного имени — остаются
> бейдж уровня, полоса и числа HP. Животные без бирки не затронуты: у них ванильного
> ника нет, плашка остаётся полной и висит у головы. `CLASSIC` тоже поднимает обе строки,
> если ник есть. `FULL`/`IGNORE` возвращают полную плашку или старое поведение.
> Проверено статически (tree-sitter, `tools/check_java_syntax.py --all`); сборка и
> клиент по-прежнему недоступны в песочнице — сценарий проверки в §6a, пункты 6–9.

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

### Причина найдена: этот JDK врёт в `Files.isWritable()`

Проба на машине пользователя (JDK 25.0.4.1, Windows 10) показала:

```
=== 5. What the file system says about writability ===
  probe dir: C:\Code\Age-of-Epoch\build\probe
    Files.isWritable   : false        <-- врёт
    create+delete file : OK           <-- но запись работает
  loom dir: ...\caches\fabric-loom\26.3
    Files.isWritable   : false
    create+delete file : OK
  client jar: ...\minecraft-client.jar
    Files.isWritable   : false
    dos:readonly       : false
    open for WRITE     : OK
  merged jar: ...\minecraft-merged.jar   (22 bytes)
    Files.isWritable   : false
    dos:readonly       : false
    open for WRITE     : OK

=== 6. Re-opening a copy read-write (JDK 25 accessMode) ===
    accessMode=readWrite : REJECTED
    java.io.IOException: the ZIP file is not writable
```

`Files.isWritable()` возвращает **false для всех** путей — включая только что созданный
`build\probe` и джарники, которые тут же успешно открываются на запись. Атрибут
`dos:readonly` при этом `false`, обычная запись (`java.util.zip`, `create+delete`) проходит.
То есть **это не антивирус и не права на папку — врёт сама JVM**.

На JDK 25 этого достаточно, чтобы сломать всю сборку. В `jdk.nio.zipfs.ZipFileSystem`
(после JDK-8350880/8350881):

```java
this.readOnly = forceReadOnly || multiReleaseVersion.isPresent() || !Files.isWritable(zfpath);
if (readOnly && accessMode == ZipAccessMode.READ_WRITE) {
    throw new IOException("the ZIP file is not writable");
}
```

Дальше цепочка замыкается:

1. `MinecraftJarMerger` открывает `minecraft-merged.jar` через zipfs → ФС read-only;
2. первый же `Files.createDirectories` в `add()` → `ReadOnlyFileSystemException`;
3. `merge()` отправляет задачи в executor **без сохранения `Future`** → исключение исчезает;
4. конструктор уже записал END-заголовок → в файле **22 байта, 0 записей**, в логе тишина;
5. ремап «успешно» перемапывает пустоту → `package net.minecraft.* does not exist` +
   `NullPointerException ... getNestingKind()`.

Важно: zipfs пишут **все** шаги, которые создают джарник Minecraft, поэтому на такой JVM не
работает и обход `singlejar`:

```
> Failed to setup Minecraft, java.lang.RuntimeException: Failed to process CLIENT only jar
```

Это падает jar-процессор (`JsrAnnotationRemapperProcessor` → tiny-remapper `OutputConsumerPath`,
он тоже пишет через zipfs). `build.bat singlejar` остаётся полезен только когда zipfs здоров.

### Что делать

**1. Найти JDK, который не врёт.**

```bat
build.bat jcheck
```

Команда прогоняет `tools\WritableCheck.java` каждым найденным JDK (`JAVA_HOME`, все `.jdks\*`,
`java` из PATH) по путям проекта, `%TEMP%` и `.gradle-home` и печатает вердикт:

| Вердикт | Значение |
|---|---|
| `OK` | `Files.isWritable` = true → этим JDK можно собирать |
| `MISREPORT` | isWritable = false, а запись проходит → JDK врёт, zipfs будет read-only |
| `DENIED` | запись реально не проходит → права/антивирус (см. пункт 3) |

`build.bat` сам предупреждает об этом перед каждой сборкой (`:jdk_sanity` вызывает
`WritableCheck` и молчит, кроме случая «MISREPORT»), так что заведомо пустой jar больше не
собирается «вслепую».

**2. Поставить другой JDK 25.** Microsoft OpenJDK 25, Amazon Corretto 25, Azul Zulu 25 или
Oracle JDK 25 — любой, кроме текущего 25.0.4.1 (Temurin). Дальше либо в `.jdks\` — `build.bat`
сам найдёт (`:find_jdk` сканирует `.jdks\*`, `%USERPROFILE%\.jdks\*` и `Program Files`),
либо `set JAVA_HOME=<новый JDK>` перед `build.bat`. Затем `build.bat repair`.

**3. Если `jcheck` покажет `DENIED`** (запись реально не проходит) — тогда уже права или
антивирус: исключение для `C:\Code\Age-of-Epoch` и `.gradle-home`, разрешить `java.exe` в
Controlled folder access, либо
`icacls "C:\Code\Age-of-Epoch" /grant "%USERNAME%:(OI)(CI)F" /T`.

**4. Сообщить наверх** (в отчёте есть всё нужное):

- **OpenJDK**, к JDK-8350880: `Files.isWritable()` возвращает `false` для путей, в которые
  запись реально проходит (Windows 10, JDK 25.0.4.1); из-за этого `newFileSystem(..., create=true)`
  отдаёт read-only ФС, а `accessMode=readWrite` — `IOException: the ZIP file is not writable`;
- **Fabric Loom**: (1) `MinecraftJarMerger.merge()` не проверяет `Future` своих задач — ошибка
  слияния полностью теряется, на выходе валидный пустой jar; (2)
  `FileSystemUtil.getJarFileSystem(..., create = true)` стоит передавать `accessMode=readWrite`,
  тогда JDK 25 падает с внятным исключением вместо тихого пустого jar.

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
5. показывает, что файловая система думает о путях (`Files.isWritable`, `dos:readonly`,
   владелец, real path, реальное открытие на запись) и ловит главный случай — isWritable
   врёт, а запись проходит;
6. открывает **копию** `minecraft-merged.jar` с `accessMode=readWrite` (кэш не меняется) и
   сообщает, что отвечает JDK 25;
7. печатает итог и код возврата: `0` — все примитивы работают, `1` — что-то упало.

Запуск в режиме одного файла (`java tools\MergeProbe.java`), компилятор не нужен, пишет
только в `build\probe\`. Файл можно запустить и руками:

```powershell
& "$env:JAVA_HOME\bin\java" -Dprobe.projectDir=. tools\MergeProbe.java
```

Дальше по результату:

- упало в п.2 (открытие client.jar) → битый **скачанный** jar: удалить `minecraft-client.jar`,
  `minecraft-server.jar`, `minecraft-merged.jar` и `build.bat repair`;
- упало только в п.3, а п.4 прошёл → zipfs объявил файл read-only (JDK 25): смотреть п.5–6;
  если там `Files.isWritable = false` при успешной записи — виноват JDK, `build.bat jcheck`
  и другой JDK 25; если запись реально не проходит — права/антивирус;
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

### Дополнение: сравнение JDK на машине пользователя

`jcheck` на тех же каталогах дал:

| JDK | `Files.isWritable` | `File.canWrite` | Реальная запись |
|---|---|---|---|
| Temurin 25.0.4.1 | false | true | проект и `.gradle-home`: OK; `%TEMP%`: AccessDeniedException |
| Temurin 21.0.10 | true | true | все три каталога: OK |

Это подтверждает зависимость от запущенного JDK, но **не доказывает**, что защитное ПО
или политика процесса не участвуют: разные `java.exe` могут получать разные ограничения.
Ранее категоричные утверждения об исключении антивируса следует читать с этой оговоркой.
JDK 21 годится для сравнения, **не для сборки Minecraft 26.3**.

Теперь `build.bat jcheck` показывает каждый путь JDK один раз и сводку с версией,
вердиктом и пригодностью для сборки (25+ и все проверки успешны). Проверяются также
стандартные каталоги установки, а не только PATH. Ошибка хотя бы одного пути больше
не маскируется успешным результатом другого.

`:find_jdk` проверяет проект и `GRADLE_USER_HOME` до запуска Gradle и пропускает JDK
с ненулевым результатом, включая `MISREPORT`, `DENIED` и ошибку запуска проверки.
Первый рабочий кандидат сохраняет приоритет. `%TEMP%` остаётся в полной диагностике,
но не участвует в выборе JDK для каталогов сборки. Если рабочего JDK нет, сборка не
стартует, а `repair` не удаляет кэши.

```bat
build.bat getjdk microsoft
build.bat jcheck
build.bat repair
```

`getjdk microsoft` использует `winget install --id Microsoft.OpenJDK.25 -e`.
Доступность пакета зависит от каталога winget. По умолчанию `getjdk` скачивает Temurin.
Для другого доверенного **ZIP с JDK для Windows**:

```bat
set "AOE_JDK_URL=https://<доверенный-сервер>/<jdk25-windows-x64.zip>"
build.bat getjdk
set "AOE_JDK_URL="
```

После установки запускаются диагностика и выбор пригодного JDK; при отсутствии
такого JDK команда завершается с ошибкой. Не используйте случайные ссылки на JDK.
Переименование сломанного каталога внутри `.jdks` **не исключает его из поиска**:
переместите его за пределы сканируемых каталогов либо задайте рабочий `JAVA_HOME`.
Проверка успешной записи не гарантирует успешную сборку: это только предварительный тест.

## 6a. Плашки мобов: точки риска и что проверять

Новый код: `a_libraries/mob_plates/{MobPlateRenderer,PlateDraw,MobPlateSettings,MobPlateStyleHud}.java`,
`config/forge/{MobPlateStyle,MobPlateNameMode}.java`; точка входа — `RenderMobInfo.submitWorldMobLabels`
(делегат в `MobPlateRenderer`), регистрация — `ClientInit` → `LevelRenderEvents.COLLECT_SUBMITS`.

Что нельзя проверить без сборки и почему это важно:

1. **`submitText` (10 аргументов)** — порядок `…, DisplayMode, lightCoords, color, backgroundColor, outlineColor`
   сверен с рабочим модом на 26.3 (VoX/lod-server-support), но не с самим jar-ом.
   Если порядок другой, текст плашек будет чёрным/без фона, а не упадёт: проверять визуально.
2. **`poseStack.rotate(camera.orientation)`** — в порте уже используется `mulPose(Matrix4f)`;
   перегрузка с кватернионом подтверждена в чужом 26.3-коде, но не локально.
3. **Порядок отрисовки.** Геометрия плашки уходит в `order(-1)` с `RenderTypes.debugQuads()`
   (запись глубины включена), текст — в `order(0)` и сдвинут на 0.5 пикселя к камере.
   Если текст окажется позади своей плашки, он пропадёт: лечится сменой знака в
   `poseStack.translate(0, 0, -0.5F)` в `MobPlateRenderer.submitPanel`.
4. **`mc.level.entitiesForRendering()` + `entity.xOld/yOld/zOld`** — интерполяция позиции;
   при `getDeltaTracker() == null` (пауза/скриншот) могут быть рывки.
5. **Производительность.** На каждого моба в радиусе идёт один `submitCustomGeometry`
   (до ~250 вершин) и 2–4 `submitText`. При 40+ мобах в кадре стоит замерить FPS и,
   при просадке, уменьшить `max_distance` или радиус скругления в `PlateDraw.rounded`.
6. **Геометрия ванильного ника.** Константы `VANILLA_NAME_TAG_Y = 0.5` и
   `VANILLA_NAME_TAG_HEIGHT = 0.24` в `MobPlateRenderer` взяты из ванильной отрисовки
   (9 пикселей шрифта при масштабе 0.025 плюс пиксель фона). Если в 26.3 ник рисуется
   от другой точки, плашка окажется выше или ниже, чем нужно, — правится `named_offset`
   без пересборки кода. Проверять на игроке и на переименованной биркой корове.
7. **`hasCustomName() && isCustomNameVisible()`** — та же пара, которой пользуется ванильный
   рендер. Служебные мобы мода (`DelveMarkerBlockEntity`, `TowerBoss`) ставят
   `setCustomNameVisible(true)`, прислужники (`MinionUtils`) — `false`: у прислужников
   ванильного ника нет, поэтому они остаются с полной плашкой. Проверить оба случая.
8. **`isCrouching()` / `isSpectator()`** для игроков: присевший игрок для других выглядит
   без ника, поэтому ему возвращается полная плашка у головы. Если метода не окажется в
   26.3, компиляция укажет на `hasVanillaNameTag` — замена на `isShiftKeyDown()`.

Сценарий проверки в игре:

1. `./gradlew build`, затем `build.bat runClient`.
2. Стиль по умолчанию (MODERN): плашка над мобом, бейдж уровня, цветная рамка по редкости.
3. Нажать **J** — стиль меняется на RUNE, затем на CLASSIC, затем обратно; внизу экрана
   появляется подтверждение, выбор сохраняется в `config/age_of_epoch/mob_plates.properties`.
4. Отойти на 24+ блока — плашки исчезают; подойти к боссу (`boss`) — плашка видна и на
   максимальной дистанции, полоса здоровья не «съедает» имя.
5. Зайти в настройки файла: `hide_full_health=true`, перезапуск — плашки полных мобов скрыты.
6. Встать рядом с другим игроком (или зайти вторым клиентом): его ник читается целиком,
   а над ним висит узкая плашка с уровнем, полосой и HP. Присесть — ник исчезает, плашка
   возвращается к голове.
7. Переименовать биркой корову: то же поведение. Обычная корова без бирки — полная плашка
   с именем у головы, как раньше.
8. **Shift+J** — внизу экрана «Плашки мобов: … · имена: Полная/Компактная/Не учитывать»;
   в `FULL` над ником видна полная плашка, в `IGNORE` — старое перекрытие.
9. Стиль `CLASSIC`: у игрока и у именованной коровы обе строки уходят выше ника, у
   безымянной коровы остаются на прежних высотах.

## Подземные экспедиции — 2026-10-21

Добавлены четыре тематических конечных данжа (12 планировок по 4 этажа), серверные
контроллеры комнат и ресурсы. Проверены топология и NBT, регрессии башен и синтаксис
Java. **Эта новая Java-реализация ещё не прошла `gradlew build` и игровой запуск**:
в песочнице нет Java 25 и нужных Maven-зависимостей. Более ранний BUILD SUCCESSFUL
не относится к этим изменениям. Сценарий приёмки и баланс около часа — в `DELVES.md`.
