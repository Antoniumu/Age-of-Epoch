# PORTING.md — технический отчёт о порте Age of Exile → Minecraft 26.3 (Fabric)

Документ описывает архитектуру порта, методику миграции и **честный список того, что осталось
довести до компиляции/запуска** (задача ANT-50). Всё перечисленное в разделе «Известные
остатки» — это точки, которые нельзя проверить без `./gradlew build` (в среде разработки
отсутствовали JDK и доступ к maven.fabricmc.net / Mojang-репозиториям).

## 1. Целевой тулчейн

| Компонент | Версия |
|---|---|
| Minecraft | 26.3 «Wilderness Bound» (Java Edition, data version 5023) |
| Java | 25 (обязательное требование MC 26.x) |
| Fabric Loader | 0.19.5 |
| Fabric API | 0.163.0+26.3 |
| Loom | 1.17-SNAPSHOT (`net.fabricmc.fabric-loom`, необфусцированный режим — Mojmap в комплекте) |
| Gradle | 9.6.1 (wrapper) |
| pack.mcmeta | resource format 97 / data format 121 |

## 2. Состав порта

Ветка `arena/3944e6e4-age-of-epoch`, монорепозиторий одного Fabric-мода `mmorpg`
(«Age of Epoch»), объединяющий исходники:

- `Age-of-Exile` (RobertSkalko/Age-of-Exile, master) — 1076 файлов / ~73k строк;
- `Library-of-Exile` — 132 файла / ~7.3k строк (была jar-зависимость, перенесена исходниками);
- `Divine-Missions` — 82 файла / ~3.7k строк; `Infinite-Dungeons` — 106 файлов / ~5.5k строк
  (используются аддон-склеевкой `com.robertx22.addon.*` внутри Age of Exile);
- **World of Exile** — отдельный мод без публичных исходников; написаны минимальные заглушки
  `com.robertx22.world_of_exile.*` (4 класса) для единственной использующей их части кода
  (`DungeonDimensionJigsaw`, `SignUtils`).

## 3. Миграция кода: `tools/port_to_fabric.py`

Скрипт идемпотентен и полностью воспроизводим (можно перезапускать).

1. **Импорты net.minecraft 1.16.5 (official) → Mojmap 26.3** — таблица на ~270 переименований
   пакетов/классов (`PlayerEntity→Player`, `World→Level`, `CompoundNBT→CompoundTag`,
   `PacketBuffer→FriendlyByteBuf`, `StringTextComponent→Component` и т.д.), включая
   статические и wildcard-импорты.
2. **Неточечные имена классов** в теле кода (по границам слов), с автодобавлением
   недостающих импортов (`StackNBT`, `BuiltInRegistries`, `ChatFormatting`…).
3. **Конструкторы**: `new StringTextComponent(x)` → `Component.literal(x)`,
   `new TranslationTextComponent(x)` → `Component.translatable(x)`,
   `new ResourceLocation(ns, path)` → `ResourceLocation.fromNamespaceAndPath(...)`,
   `new ResourceLocation(id)` → `ResourceLocation.parse(...)`.
4. **Методные переименования** (1.16.5 → 26.3): `getCommandSenderWorld()→level()`,
   `player.inventory.*→player.getInventory().*`, `sendPacket→send`, `getMotion()→getDeltaMovement()`
   и др.
5. **NBT предметов** (до 1.20.5 API) → хелпер `StackNBT` поверх компонента `CUSTOM_DATA`
   (`stack.getTag()→StackNBT.get(stack)` и т.п.).
6. **net.minecraftforge.\*** → `com.robertx22.forge_compat.*` — весь Forge-код
   перенаправлен на слой совместимости (см. §4).

### Проверки после миграции (выполнены)

- `python3 tools/port_to_fabric.py` → «no leftover old imports»: **0** ссылок на
  `net.minecraftforge` и устаревшие пакеты `net.minecraft.*` осталось в 1396 файлах.
- Все файлы прошли статическую перезапись без возврата при повторном запуске (идемпотентность).

## 4. Слой совместимости `com.robertx22.forge_compat` (~65 классов)

Сохраняет Forge-идиомы исходников и переводит их на Fabric API:

| Подсистема | Реализация |
|---|---|
| **EventBus** (`MinecraftForge.EVENT_BUS`, `IEventBus`, `@SubscribeEvent`, `EventPriority`) | Реестр хендлеров; `@Mod.EventBusSubscriber`-классы капабилити регистрируются автоматически |
| **Капабилити** (`Capability`, `CapabilityManager`, `@CapabilityInject`, `ICapabilitySerializable`, `LazyOptional`, `AttachCapabilitiesEvent`) | Ленивое привязывание к объектам при первом обращении (`CapabilityProvider.get`), NBT-сериализация через mixin `EntityNbtCapBridgeMixin` (`saveWithoutId`/`load`), перенос при смерти через `ServerPlayerEvents.COPY_FROM` → `PlayerEvent.Clone` |
| **Сеть** (`SimpleChannel`, `NetworkRegistry`, `NetworkEvent.Context`, `PacketDistributor`, `NetworkHooks`) | Fabric Networking API v1 (`ServerPlayNetworking`/`ClientPlayNetworking`, `PayloadTypeRegistry`); `sendTo(..., connection, dir)` через отслеживаемые соединения |
| **Реестры** (`DeferredRegister`, `RegistryObject`, `ForgeRegistries`, `IForgeRegistryEntry`) | Немедленная регистрация в `BuiltInRegistries` во время init (эквивалентно жизненному циклу Fabric) |
| **Конфиги** (`ForgeConfigSpec`, `Builder`, `*Value`) | Та же DSL-поверхность (`define`, `defineEnum`, `defineInRange`, `configure`), значения в памяти (можно нарастить файловый I/O) |
| **События игры** (`LivingHurtEvent`, `LivingDamageEvent`, `LivingDeathEvent`, `LivingEvent`, `PlayerEvent.*`, `PlayerInteractEvent.*`, `AttackEntityEvent`, `TickEvent.*`, `EntityJoinWorldEvent`, `AddReloadListenerEvent`, `FMLServer*Event`) | Fabric callbacks (`ServerTickEvents`, `ServerEntityEvents`, `Use*Callback`, `AttackEntityCallback`, `ServerPlayConnectionEvents`, `ResourceManagerHelper`) + mixin-мосты (`LivingTickBridgeMixin`, `LivingDamageBridgeMixin`) |
| **Клиент** (`RenderingRegistry`, `ClientRegistry`, `RenderGameOverlayEvent`, `RenderLivingEvent`) | `EntityRendererRegistry`, `KeyMapping.register`; HUD-событие через `HudRenderCallback` |
| **Прочее** (`@Mod`, `ModList`, `DistExecutor`, `FMLPaths`, `InterModComms`, `Material`, `Tuple`, `IForgeContainerType`…) | Аналоги/заглушки; `InterModComms` — no-op (Curios недоступен на Fabric) |

Точки входа (`fabric.mod.json`): `MMORPG` (main, `ModInitializer`) и
`ClientInit` (client, `ClientModInitializer`); связка инициализации —
`com.robertx22.age_of_exile.mmorpg.FabricBootstrap`.

## 5. Локализация (ANT-49) — выполнена полностью

- `assets/mmorpg/lang/ru_ru.json` — **1622 ключа** (все, что есть в `en_us.json`);
- `assets/divine_missions/lang/ru_ru.json` — 51 ключ;
- `assets/infinite_dungeons/lang/ru_ru.json` — 39 ключей;
- **0 пропусков и 0 лишних ключей**, плейсхолдеры (`%1$s`, `%2$s`, `[VAL1]`, `[calc:…]`)
  и цветовые коды `§` сохранены построчно (валидация скриптом при сборке `ru_ru.json`);
- перевод согласован по терминологии (редкости, стихии, самоцветы, станции, аффиксы).

Исходники перевода: `tools/ru_work/` (`ru_part_0*.txt` + скрипт сборки в истории команд).
Примечание: `ko_kr.json`, `zh_tw.json` (битые UTF-8) и `manual.json` (фрагмент для генератора
лэнгов, не JSON) были некорректны и в апстриме — оставлены как есть.

## 6. Известные остатки (компиляция/запуск, ANT-50)

Приоритетный список для первого `./gradlew build`:

1. **Мир-генерация** (`dimension/DungeonDimensionJigsaw.java` и вокруг):
   1.16-е структуры/джигсоу (`Structure.configured`, `VillageConfig.CODEC`,
   `DimensionStructuresSettings`) семантически переписаны в MC 1.18+; заглушки
   `forge_compat.worldgen.*` держат исходники компилируемыми, но методы
   `createConfiguredFeature/createFeature` требуют переписывания под
   `Structure`/`StructureType` 26.3. Также проверить `VoidChunkGenerator`
   (кодеки генераторов чанков регистрируются в `BuiltInRegistries.FEATURE_TYPE`-подобные
   реестры).
2. **GUI-рендеринг** (54 файла `gui/`): `AbstractGui.blit`, конструкторы
   `Button`/`ImageButton` (1.20.4+ `Button.builder`, `WidgetSprites`), `Screen#render`
   через `GuiGraphics`. Имена классов уже мигрированы, сигнатуры — довести по месту.
3. **Взаимодействия** `InteractionResult` (1.21.2+ объединил `ActionResult`/
   `ActionResultType`): проверить переопределения `use`/`interactLivingEntity` в предметах.
4. **Curios** — интеграция с аксессуарами сделана no-op-заглушками
   (`top.theillusivec4.curios.api.*`); при желании заменить на Trinkets.
5. **Material** — удалён из Minecraft; `forge_compat.material.Material` держит
   вызовы `AbstractBlock.Properties.of(Material.X)`; при компиляции перевести на
   `BlockBehaviour.Properties.of()`/`noCollission()` по смыслу.
6. **Мелочи**: `Tags` (`ITag`→`Tag` + `TagKey` API), `STitlePacket`-совместимый
   `ClientboundTitleCompatPacket` (в 1.17+ разбит на 3 пакета), `Entity#getAddEntityPacket(ServerEntity)`
   (сигнатура исправлена в 3 сущностях), `joptsimple`→`String.join` (исправлено),
   `ServerSidePacketRegistry` в закомментированном `EntityPacket`.
7. **Производительность**: `LazyOptional` и капабилити-хранилище — простые карты
   (`WeakHashMap`); при нагрузке заменить на attachment API.

## 7. Как продолжить

1. `./gradlew build` на машине с JDK 25 и доступом к Maven; далее по списку §6.
2. `./gradlew runClient` — проверить русский язык (язык в настройках), регистрацию
   предметов/заклинаний, выдачу снаряжения, капабилити (статы игрока), пакеты синхронизации.
3. Воспроизводимость: любые правки переименований вносить в `tools/port_to_fabric.py`.
