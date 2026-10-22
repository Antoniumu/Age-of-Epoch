# PORTING.md — технический отчёт о полном порте 6-модового пакета Mine and Slash 2025 → Minecraft 26.3 (Fabric, Java 25)

Документ описывает архитектуру единого Fabric-мода **Age of Epoch** (`mmorpg`) для **Minecraft 26.3** (Data Version `5023`, Fabric API `0.163.0+26.3`, Trinkets `4.2.1+26.3`, Java `25`), в котором устаревшие подсистемы `1.16.5` полностью заменены актуальным 6-репозиторным стеком **Mine and Slash Rework (2025)** с сохранением уникальных механик **Age of Epoch**.

## 1. Целевой тулчейн

| Компонент | Версия |
|---|---|
| Minecraft | `26.3` (Java Edition, Mojmap без обфускации, `DataVersion = 5023`) |
| Java | `25` (`OpenJDK 25` / language level `25`) |
| Fabric Loader | `0.19.5` |
| Fabric API | `0.163.0+26.3` |
| Trinkets | `4.2.1+26.3` (`eu.pb4:trinkets`) |
| Loom / Gradle | `1.18.4` / `9.7.1` |

## 2. Состав объединённого мода (`2 209` `.java` файлов → `2 983` `.class`)

1. **`library_of_exile`** (`Library-of-Exile-Rework`) — реестры датапаков `ExileRegistry`, сжатая сетевая синхронизация `EfficientRegistryPacket`, `ICap` капабилити на `DataComponents.CUSTOM_DATA` / `ValueInput` / `ValueOutput`, реликвии и каркас измерений карт.
2. **`orbs_of_crafting`** — `43` валютных сферы крафта, `64` модификации предметов и `61` требование.
3. **`mmorpg`** (`Mine-And-Slash-Rework`) — `741` стат, `645` перков дерева талантов и классов Восхождения, `99` заклинаний в `6` школах, души статов (`Stat Souls`), `51` уникальный предмет, `63` камня умений, `59` камней поддержки, `24` ауры, `22` руны, `9` рунных слов, `8` знамений (`Omens`), пророчества, статы Хаоса, сундуки добычи и все `9` профессий (`798` рецептов станций + `137` рецептов верстака).
4. **`dungeon_realm`** — устройство карт (`Map Device`), `17` подземелий, фрагменты и арены Убер-боссов.
5. **`ancient_obelisks`** — обелиски волн, тиры наград и структуры измерения обелисков.
6. **`the_harvest`** — порталы, арены и валютные награды Жатвы.
7. **Сохранённые механики `Age-of-Epoch`** — Башни (`Towers`: `TowerStructureProcessor`, `TowerBoss`, `TowerCollapse`, `TowerDestroyer`), Спуски (`Delves`: `DelveStructureProcessor`, `DelveMarkerBlockEntity`), адаптивный `GameMaster`, `QuestBook` (`19` квестов), `Guides` (`3` гайда), `MobPlates` и камера `Simulation` (`mmorpg:simulation`).

## 3. Ключевые адаптации под API Minecraft 26.3

1. **GUI, экраны, виджеты и оверлеи (`GuiGraphicsExtractor`)**:
   - Экраны `GuidesScreen`, `QuestBookScreen`, `SimulationScreen` переведены на `extractRenderState(GuiGraphicsExtractor, int, int, float)`; в `MainHubScreen` кнопки `Квесты`, `Гайды`, `Симуляция` регистрируются после `clearWidgets()`, а `AllocateStatButton` вызывает `super.extractContents(...)` без рекурсивного `StackOverflowError`;
   - `CraftingStationScreen` переведён на `tick()` / `super.tick()`, `extractTooltip(...)` и `Screens.getWidgets(this)`;
   - Восстановлена отрисовка иконок, клики (`MouseButtonEvent`) и тултипы во всех `15` классах кнопок (`BackpackButton`, `SpellButton`, `SuppGemOverlayButton`, `CraftButton`, `ItemButton`, `LockButton`, `PrimaryMatInfoButton`, `RecipeButton`, `CharacterStatsButtons`, `FavorButton`, `ProfessionLevelsButton`, `CardPickButton`, `InvGuiButton`, `LearnClassPointButton`, `MainProphecyButton`);
   - В `AgeOfEpochClientMod` подключены `GuiOverlays.registerOverlay(new RegisterGuiOverlaysEvent())` (все `4` HUD-оверлея MNS) и `ScreenEvents.AFTER_INIT` (кнопка быстрого сбора рюкзака в сундуках);
   - Исправлены привязки клавиш в `KeybindsRegister` / `SpellKeybind` (`R/V/C/G` — слоты `1–4`, `Shift + R/V/C/G` или `F7` — слоты `5–8`, `H` — Хаб, `K` — Квесты, `J` — стиль табличек мобов, `P` — быстрое зелье).
2. **Боевая система, добыча, миксины, сущности и генерация мира**:
   - `DamageEvent` переведён на 3-аргументный `DamageSource` и `LivingEntityAccesor` (`damageCooldownTime` + сброс `lastHurt`); в `LivingDamageBridgeMixin` подключён `LivingAttackEvent` (`hurtServer`), защита бессмертия в `SimulationManager` и `LivingDeathEvent`; в `library_of_exile/mixins/LivingEntityMixin` восстановлен учёт урона игрока (`onDamagedBy`) для генерации лута и опыта;
   - В `AddSpawnerExtraLootMethod`, `OnBlockDropMining` и `OnBlockDropFarming` вызовы удалённого `LootContext.getOptionalParameter(...)` заменены на `LootContext.getOptional(...)`;
   - `DungeonRoomPlacer` и процессоры `world_of_exile` используют точную 6-аргументную сигнатуру `StructureProcessor.processBlock(..., BlockPos templateRelativePos, ...)`, а `world_of_exile:tower_processor` и `world_of_exile:delve_processor` зарегистрированы в `BuiltInRegistries.STRUCTURE_PROCESSOR`;
   - `LevelMixin` настраивает `WorldBorder` из `cir.getReturnValue()` без вызова удалённого конструктора `new WorldBorder()`; удалены устаревшие заглушки `MixinConnector.java`.

## 4. Итоги верификации

- **Компиляция Java 25 (`ECJ 3.45.0` / `OpenJDK 25.0.2`)**: `2 209` `.java` → `2 983` `.class`, **`EXIT CODE: 0`, `0` ошибок** (`bb7437b` baseline `1 565` `.java` → `2 011` `.class`, **`EXIT CODE: 0`**).
- **Аудит байткода (`Constant Pool`)**: `0` устаревших сигнатур `1.20.1` во всех `2 983` `.class` файлах.
- **Живой запуск `AgeOfEpochMod.onInitialize()` и перезагрузка датапаков (`OpenJDK 25.0.2`)**: **`EXIT CODE: 0`**, выполнены `56` загрузчиков, заполнены **`60 / 60`** реестров `ExileRegistry` (**`3 658` записей**, `0` пустых реестров), `2 091` предмет (`423` модовых), `1 325` блоков (`31` модовый).
- **Ресурсы и локализация**: `2 448` валидных JSON-файлов (`RFC 8259`), `423` client item definitions, `1 948` PNG текстур, `640` `.nbt` структур, **`3 548` EN / `3 548` RU ключей (`100.00%` паритет)**, `0` пустых файлов.
