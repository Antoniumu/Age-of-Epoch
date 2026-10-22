# PORTING.md — технический отчёт о порте Age of Exile → Minecraft 26.3 (Fabric)

Документ описывает архитектуру порта, методику миграции и результаты доводки кодовой базы
до компиляции под реальный **Minecraft 26.3** (задачи ANT-47, ANT-48, ANT-49, ANT-50).

## 1. Целевой тулчейн

| Компонент | Версия |
|---|---|
| Minecraft | 26.3 «Wilderness Bound» (Java Edition, data version 5023) |
| Java | 25 (`JDK 25.0.4.1`, обязательное требование MC 26.x) |
| Fabric Loader | 0.19.5 |
| Fabric API | 0.163.0+26.3 |
| Loom | 1.18.4 (`1.18-SNAPSHOT`, `net.fabricmc.fabric-loom`, необфусцированный режим — Mojmap в комплекте) |
| Gradle | 9.7.1 (wrapper) |
| pack.mcmeta | resource format 97 / data format 121 |

В `build.gradle` добавлены опции `-PdshMaxErrs=<N>` (для полного дампа ошибок компилятора без стандартного лимита 100) и `-PdshClasspath=true` (для печати resolved compileClasspath при диагностике).

### NBT структур в 26.3

Для `DataVersion=5023` палитра использует `id` и `properties`, а не старые
`Name`/`Properties`. Неверная схема с текущей версией загружает блоки как воздух.
Генератор башен пишет новую схему; оригинальные старые NBT сохраняют свою старую
версию для штатной миграции. Источники и регрессия: [`tools/tower_gen/FORMAT.md`](tools/tower_gen/FORMAT.md).

## 1a. Добивка: хвосты после лимита в 100 ошибок javac (второй проход)

Первая компиляция после ANT-50 упиралась в стандартный лимит `-Xmaxerrs 100`,
поэтому часть хвостов не была видна. Прогон против реального дерева исходников
MC 26.3 закрыл следующие группы:

1. **Удалённые/переехавшие библиотеки**: `javax.annotation.*` (jsr305) больше не
   на classpath — в 26.3 Mojang перешли на `org.jetbrains.annotations`
   (28 файлов); `com.google.errorprone.annotations` убран (`IgnoreNullList`);
   **LWJGL-glfw удалён** — MC 26.3 переехал с GLFW на SDL
   (`InputConstants.KEY_*` = SDL-сканкоды, `InputConstants.Type.KEYBOARD`
   вместо `KEYSYM`, `org.lwjgl.sdl.*` в ваниле).
2. **KeyMapping**: `KeyMapping.Category` — реестровый объект
   (`Category.register(Identifier)`), а не строка; `matches(KeyEvent)` /
   `matches(InputConstants.Key)` вместо `matches(int, int)`.
3. **Лут**: `UniformGenerator` разъехался на `providers.number.floats/ints`,
   для роллов используется `ContextIntProviders.between(min, max)`
   (`LootPool.Builder.setRolls(Holder<ContextIntProvider>)`).
4. **Прочее**: `Criterion` → `net.minecraft.advancements.triggers.Criterion`
   (+ в `RecipeBuilder` больше нет `getResult`, зато есть `group`/`defaultId`);
   `ContainerListener` → `net.minecraft.world.inventory.ContainerListener`,
   у `SimpleContainer` больше нет `addListener` (хук через `setChanged()`);
   `DirectionProperty` удалён — `HORIZONTAL_FACING` теперь
   `EnumProperty<Direction>`; `AbstractVillager` → `...npc.villager.AbstractVillager`;
   `RandomizableContainerBlockEntity.setLootTable(key, seed)` разделён на
   `setLootTable(key)` + `setLootTableSeed(seed)`; `Entity/Player.getServer()`
   удалён — только `levelAccessor/level.getServer()`; `level.random` →
   `level.getRandom()` (хвосты); `displayClientMessage(msg, false)` →
   `sendSystemMessage(msg)` (37 мест).
5. **Клиентские события**: `KeyEvent` не имеет `scancode()` (это `keycode()`),
   `CharacterEvent` не имеет `modifiers()` — исправлены мосты
   `forge_compat.client.Screen` / `AbstractContainerScreen`;
   `PoseStack.mulPose(Quaternionf)` удалён → `rotate(Quaternionfc)` /
   `rotate(Axis, angle)`; `MultiBufferSource`, `Font.drawInBatch` и
   `Minecraft.renderBuffers()` удалены из пайплайна — мёртвые пути
   dmg-частиц/хелсбаров выпилены (живой рендер плашек — `MobPlateRenderer`
   через `OrderedSubmitNodeCollector.submitNameTag/submitText`).

## 1b. Третий проход (следующие 100 ошибок)

6. **Инкапсуляция сущностей**: `Entity.invulnerableTime` приватный →
   `getInvulnerableTime()`/`setInvulnerableTime(int)`; поле `hurtMarked`
   переименовано в публичное `needsSync`; `hasImpulse` удалён (knockback
   синхронизирует скорость сам); `Entity/Player.getServer()` — только через
   `level()/levelAccessor.getServer()` (хвосты, включая 4 cap-класса
   `*MapCapability`/`MapConnectionsCap`, где `get(Level entity)`).
7. **Рендер-GUI**: `Minecraft.setScreen()` удалён →
   `Minecraft.getInstance().gui.setScreen(...)` (34 места);
   `Screen.hasShiftDown()/hasAltDown()/hasControlDown()` удалены →
   `InputConstants.isKeyDown(InputConstants.KEY_L*)` (SDL-сканкоды, 19 файлов);
   `GuiGraphicsExtractor` без no-arg конструктора —
   `(Minecraft, GuiRenderState, int, int)`.
8. **ChunkPos — record**: поля `.x`/`.z` → `.x()`/`.z()`,
   `new ChunkPos(BlockPos)` → `ChunkPos.containing(pos)`.
9. **Типы/реестры**: статические константы (`ZOMBIE`, `PLAYER`, …) переехали из
   `EntityType` в `EntityTypes` (`EntityType.Builder` остался); `byString(id)` →
   `BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(id))`;
   `MobEffects.MOVEMENT_SLOWDOWN/MOVEMENT_SPEED` → `SLOWNESS/SPEED`;
   `ChatFormatting.getName()/getByName()` удалены →
   `CompatChatFormatting.getName/getByName` (23 файла).
10. **Бои и спавн**: публичный конструктор `DamageSource` без позиции —
    `(Holder, direct, causing)`; `knockback(power,x,z,DamageSource,damage)`;
    `Level.getCurrentDifficultyAt` есть только у `ServerLevel` (каст);
    `BlockPos.getCenter()` → `Vec3.atCenterOf(pos)`; `Player.sweepAttack()`
    удалён → частица `SWEEP_ATTACK` + `swingAndResetAttackStrength(...)`.
11. **Блоки и датаген**: `Block.codec()`/`simpleCodec()` удалены целиком
    (7 obelisk/dungeon/harvest/profession-блоков) — сериализация блоков в 26.3
    другая; `DataGenerator.addProvider(run, provider)` →
    `addProvider((PackOutput) -> provider)`; `PreparableReloadListener.reload`
    теперь принимает `SharedState(manager)` первым аргументом.
12. `-Xmaxerrs` поднят до 5000 по умолчанию (`-PdshMaxErrs=N` для переопределения),
    чтобы хвосты ошибок больше не прятались за лимитом в 100.

## 1c. Четвёртый проход — полный список: все 258 ошибок (258 → 0 ставится этой правкой)

Полный (нелимитированный) лог впервые показал весь масштаб: 258 ошибок ~40 шаблонов.
Правки 127 файлов:

1. **EntityTypes**: импорты добавлены в 7 файлов (MapDimensionConfig, SimulationManager,
   DungeonUberBosses, LibMobLists, EntityConfigs, SpellUtils, SlashEntities) — константы
   были переименованы в 3-м проходе, но без импортов.
2. **ChunkPos-хвосты**: строковые конкатенации и присваивания полей
   (`cp.x()`, `c.x()`, `cpos.x()`, `start.x()`, `count.x()`, `be.x()`) в
   MapPlayerDataSaver, MapDeviceBlock, AllMapConnectionData, ProcessMapChunks,
   MapGenerationUTIL, HarvestBlock (включая вызов `getStartFromCounter`).
3. **Кнопки**: финальный `AbstractButton.extractWidgetRenderState` → абстрактный
   `extractContents` — переименовано в ~40 классах кнопок (+ compat Button,
   ObjectSelectionList, EditBox); ToonActionButton/FilterGroupButton получили
   реализацию `extractContents` (рисует `getMessage()` по центру).
4. **Экраны-контейнеры**: `AbstractContainerScreen.tick()` финален (ваниль сама зовёт
   `containerTick()`) — переопределение удалено; `imageWidth/imageHeight` финальны —
   в compat добавлены `guiWidth/guiHeight`, `setGuiSize`, `getGuiLeft/getGuiTop/
   getXSize/getYSize` и перецентровка в `init()`; `Screen.renderables` приватен —
   хелперы `removeWidgetsMatching/removeWidgetsExcept` поверх `children()`/
   `removeWidget` (compat Screen + AbstractContainerScreen); SkillTreeScreen
   итерирует снапшот `children()`.
5. **Команды**: `CommandSourceStack.hasPermission(int)` удалён →
   `permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)` (уровень 2; 0 → true),
   CommandBuilder.hasPermLevel(int) маппит уровни 1–4 на MODERATORS..OWNERS через
   `Permission.HasCommandLevel(PermissionLevel)`; GiveStat шлёт сообщение только Player.
6. **Лут**: `LootContext.params()` → `hasParameter`/`getOptional(ContextKey)`;
   `getOptionalParameter` → `getOptional` (5 файлов с блоками: Obelisk, MapDevice,
   Profession, Harvest, спавнер-экстра).
7. **MapChunkGenerator**: `fillFromNoise` → `buildTerrain(ChunkAccess, Blender,
   RandomState, StructureManager, BiomeManager, @Nullable WorldGenRegion,
   Set<Holder<Biome>>)`; хуки `applyCarvers`/`buildSurface` больше не существуют;
   `addDebugScreenInfo(..., SamplerContext)` (4 аргумента); `createStructures`
   6-аргументный с `ChunkGeneratorStructureState` и `ResourceKey<Level>`.
8. **Игровые правила**: `RULE_NATURAL_REGENERATION`/`getRule().get()/.set()`
   → `NATURAL_HEALTH_REGENERATION` + `GameRules.get(rule)`/`set(rule, v, server)`;
   MapGameRules больше не наследует GameRules (у него нет конструктора без аргументов).
9. **Однострочники**: `Enchantments.INFINITY` (ResourceKey); датаген через
   `DataGenerator.getVanillaPack(true).addProvider(factory)`; сид через
   `ServerLevel.getSeed()`; шаблоны данжей через
   `getStructureTemplateManager().get(id)` + `filterBlocks` без 4-го аргумента;
   `world.getRandom()`; `DustParticleOptions(int ARGB, scale)` (RGB.getIntR/G/B);
   `Camera.position()`; `!isDarkOutside()` вместо `isDay()`; `Entity.level()`;
   `BlockState.typeHolder()`; `noCollision()`; `Blocks.GLAZED_TERRACOTTA.purple()`;
   рудные теги через `BlockItemTags.X_ORES.block()`; `ParticleTypes.WITCH` вместо
   EFFECT (6 файлов спеллов); `CharacterEvent(codePoint)`; `Slot.index` вместо
   `getSlotIndex()`; `RenderTypes.entityCutout(tex, true)` (4 файла); пакеты через
   `SimpleChannel.sendToPlayer(player, msg)` (поле `connection` у листенера приватно).
10. **Сущности**: `Entity.hurt` финален → AutoAimingProj переопределяет `hurtServer`;
    `Animal.isFood` абстрактен → SummonEntity возвращает false (наследуют все
    саммоны, включая GolemSummon); `AgeableMob.isBaby` финален → переопределение в
    GolemSummon удалено; SimpleProjectileEntity реализует абстрактный
    `getDefaultPickupItem()` (стрела); `EntityDataSerializers.COMPOUND_TAG` удалён →
    `EntityDataSerializer.forValueType(ByteBufCodecs.COMPOUND_TAG)` в
    SimpleProjectileEntity и StationaryFallingBlockEntity.

## 1d. Пятый проход — хвост из 25 ошибок после круглой правки 4

1. Иерархия виджетов: у `AbstractWidget` абстрактный хук —
   `extractWidgetRenderState(GuiGraphicsExtractor,int,int,float)` (MC EditBox
   переопределяет его в :398, не финально); он финален только у `AbstractButton`,
   у которого абстрактный — `extractContents`. Поэтому переименование в 4-м проходе
   вернули для compat EditBox, ObjectSelectionList и TipsWidget (ToonList/
   WikiEntryList/FilterEntryList наследуют реализацию от ObjectSelectionList).
2. `Screen.init()` защищён — `public void init()` в compat
   AbstractContainerScreen сделан protected (иначе подклассы с protected init()
   «сужали» доступ).
3. Лут: в 4 блоках (Obelisk/MapDevice/Profession/Harvest) получатель —
   `LootParams.Builder`, у которого метод сохранился как `getOptionalParameter`
   (ошибочно переименован в 4-м проходе); в ChestGenLootMixin переменная —
   `LootParams` → `contextMap().has()/get()`.
4. `@javax.annotation.Nullable` → `@org.jspecify.annotations.Nullable` (пакет
   javax.annotation отсутствует в 26.3); `drop(ItemStack,boolean,Prediction)`
   (`net.minecraft.util.Prediction.SERVER_ONLY`); getColor в compat возвращает int
   (тернарник с `== null` удалён); `be.x`/`be.z` в HarvestBlock — поля BlockEntity,
   а не ChunkPos; импорт ByteBufCodecs в StationaryFallingBlockEntity.

## 6. Рантайм-фаза (после зелёной сборки)

Сборка и запуск достигнуты; дальше правятся падения при старте/игре.

1. **Дубликат кодека генератора чанков** (краш на `flushAll`): три map-мода
   (obelisk/dungeon/harvest) регистрировали ОДИН статический
   `MapChunkGenerator.CODEC` под разными id; в 26.3 `MappedRegistry` хранит значения
   в `IdentityHashMap` и бросает «Adding duplicate value». Решение:
   `MapChunkGenerator.buildCodec()` создаёт свежий кодек на каждую регистрацию
   (все три id сохранены — на них ссылаются JSON измерений), а `DeferredRegister`
   при повторной регистрации того же значения под другим ключом пропускает её
   с записью в лог вместо краша.
2. **Ловушка `DefaultedRegistry.getKey`**: блоки/предметы (`minecraft:air`) и
   типы сущностей (`minecraft:pig`) лежат в дефолтных реестрах, чей `getKey`
   возвращает ключ по умолчанию для НЕзарегистрированных значений — проверка
   «уже зарегистрирован» оказалась всегда истинной, весь контент мода пропускался
   (сотни skip-логов), а рецепты падали с «minecraft:air has more than max stack
   size». Правильная проверка — `Registry.getResourceKey(value).isPresent()`
   (настоящий Optional). Урок: `getKey != null` — не признак регистрации.
3. **Компоненты предмета не привязаны во время бутстрапа**: в 26.3 максимальный
   стек — компонент `MAX_STACK_SIZE` (`ItemInstance.getMaxStackSize()` →
   `getOrDefault(..., 1)`), карты компонентов предметов строятся из
   `DATA_COMPONENT_INITIALIZERS` уже ПОСЛЕ всех модовых entrypoint'ов. Валидация
   рецептов профессий в `commonSetupEvent` видела max stack = 1 у всех предметов и
   падала на `mmorpg:material/farming/0`. Проверка заменена на ванильный
   `Item.ABSOLUTE_MAX_STACK_SIZE` (99), не зависящий от стадии. Общее правило:
   во время бутстрапа нельзя читать компонентозависимые свойства предметов.
4. **Форматы worldgen-JSON датапака** (краш при создании мира — загрузка реестров):
   - `dimension_type` ×4: новое обязательное поле `has_ender_dragon_fight`;
     `monster_spawn_light_level` — IntProvider теперь инлайном
     (`{"type":"minecraft:uniform","min_inclusive":..,"max_inclusive":..}`, без
     обёртки `value`); поля ultrawarm/natural/bed_works/respawn_anchor_works/
     piglin_safe/has_raids/fixed_time/effects переехали в environment-атрибуты
     (незнакомые ключи игнорируются — убраны для чистоты).
   - `worldgen/biome` ×4: `carvers` — это HolderSet-список (строка или массив id),
     а не карта «стадия → carver» (`{}` → `[]`); `spawners`/`spawn_costs` вообще
     покинули кодек биома (убраны).
   - `worldgen/processor_list` ×2 (world_of_exile): кастомные типы процессоров
     delve/tower не портировались (классов нет) — содержимое заменено на
     `{"processors": []}`, чтобы структуры грузились.
   - Косметика: для блока portal добавлена директория-форма blockstate
     `blockstates/portal/mmorpg.json` (26.3 пробует её); удалены файлы-мусор
     « - Copy» (один ломал путь в ресурс-паке).

## 2. Состав порта

Монорепозиторий одного Fabric-мода `mmorpg` («Age of Epoch»), объединяющий исходники:

- `Age-of-Exile` (RobertSkalko/Age-of-Exile, master) — 1076 файлов / ~73k строк;
- `Library-of-Exile` — 132 файла / ~7.3k строк (перенесена исходниками);
- `Divine-Missions` — 82 файла / ~3.7k строк; `Infinite-Dungeons` — 106 файлов / ~5.5k строк
  (используются аддон-склеевкой `com.robertx22.addon.*` внутри Age of Exile);
- **World of Exile** — минимальные совместимые классы `com.robertx22.world_of_exile.*` (4 класса)
  для `DungeonDimensionJigsaw` и `SignUtils`.

## 3. Миграция кода и закрытие всех 2 583 ошибок компиляции MC 26.3 (ANT-50)

По результатам реального прогона `compileJava` на JDK 25.0.4.1 / Gradle 9.7.1 / Fabric Loom 1.18.4 / Minecraft 26.3 (`BUILD-REPORT.md` и `javac-err.txt`, 2 583 ошибки в 524 файлах) выполнены следующие системные исправления:

1. **Идентификаторы и реестры MC 26.3 (`Identifier` & `BuiltInRegistries`)**:
   - `net.minecraft.resources.ResourceLocation` → `net.minecraft.resources.Identifier` (`Identifier.fromNamespaceAndPath`, `Identifier.parse`);
   - `ResourceKey.location()` / `TagKey.location()` → `.identifier()`;
   - `BuiltInRegistries.*.get(id)` → `.getValue(id)`;
   - `EntityType.byString(id)` → `BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(id))`;
   - `LootTables.*` → `BuiltInLootTables.*.identifier()`.
2. **Переехавшие пакеты и переименованные классы MC 26.3**:
   - `Villager` / `VillagerProfession` / `VillagerData` → `net.minecraft.world.entity.npc.villager.*`;
   - `Slime` → `net.minecraft.world.entity.monster.cubemob.Slime`;
   - `AbstractArrow` / `ThrownTrident` → `net.minecraft.world.entity.projectile.arrow.*`;
   - `TamableAnimal` → `net.minecraft.world.entity.TamableAnimal`;
   - `UseAnim` → `net.minecraft.world.item.ItemUseAnimation`;
   - `ClickType` → `net.minecraft.world.inventory.ContainerInput`;
   - `MenuScreens` → `net.minecraft.client.gui.screens.MenuScreens`;
   - `MobSpawnType` → `net.minecraft.world.entity.EntitySpawnReason`;
   - `ClipContext` → `net.minecraft.world.level.ClipContext`;
   - `LootContextParams` / `LootContextParamSets` → `net.minecraft.world.level.storage.loot.parameters.*`;
   - `SimpleReloadableResourceManager` → `net.minecraft.server.packs.resources.ReloadableResourceManager`;
   - `GameRules` → `net.minecraft.world.level.gamerules.GameRules`;
   - `ItemTransform` → `net.minecraft.client.resources.model.cuboid.ItemTransform`;
   - `ItemPredicate` → `net.minecraft.advancements.criterion.ItemPredicate`;
   - `EnderChestInventory` → `net.minecraft.world.inventory.PlayerEnderChestContainer`;
   - `IAngerable` → `net.minecraft.world.entity.NeutralMob`, `INPC` → `net.minecraft.world.entity.npc.Npc`.
3. **Инкапсуляция полей в MC 26.3 (переход на геттеры/сеттеры)**:
   - `entity.level` → `entity.level()`;
   - `level.isClientSide` → `level.isClientSide()`;
   - `level.random` / `entity.random` → `.getRandom()`;
   - `entity.xRot` / `entity.yRot` → `getXRot()` / `setXRot(...)` / `getYRot()` / `setYRot(...)`;
   - `entity.onGround` → `entity.onGround()`;
   - `chunkPos.x` / `chunkPos.z` → `chunkPos.x()` / `chunkPos.z()`, `new ChunkPos(blockPos)` → `ChunkPos.containing(blockPos)`;
   - `widget.x` / `widget.y` → `getX()` / `getY()` / `setX(...)` / `setY(...)`;
   - `player.getInventory().items` → обход через `getContainerSize()` / `getItem(i)`;
   - `player.getServer()` → `player.level().getServer()`.
4. **Изменившиеся сигнатуры API в 1.21.2+ / 26.3**:
   - **CompoundTag**: переход на Optional-безопасные методы `getIntOr(k, 0)`, `getStringOr(k, "")`, `getBooleanOr(k, false)`, `getFloatOr(k, 0F)`, `getDoubleOr(k, 0D)`, `getLongOr(k, 0L)`, `getByteOr(k, (byte) 0)`, `getShortOr(k, (short) 0)`, `getCompoundOrEmpty(k)`, `getListOrEmpty(k, type)`, `keySet()`, а также `StackNBT` поверх `DataComponents.CUSTOM_DATA`;
   - **BlockEntity & Entity save/load**: переход на `ValueInput` / `ValueOutput` (`loadAdditional`/`saveAdditional` и `readAdditionalSaveData`/`addAdditionalSaveData`), конструкторы `BlockEntity(type, pos, state)`, `EntityBlock.newBlockEntity(pos, state)` и `FabricBlockEntityTypeBuilder`;
   - **Сущности и рендереры**: `EntityTypes.<T>Builder.of(...)`, `.build(ResourceKey)`, `EntityType.create(Level, EntitySpawnReason)`, `Entity.hurtServer(ServerLevel, DamageSource, float)`, `Entity.kill(ServerLevel)`, `Entity.remove(RemovalReason)`, `Entity.spawnAtLocation(ServerLevel, ItemStack, float)`, `SynchedEntityData.Builder`, `EntityRenderer<T, EntityRenderState>` (`extractRenderState` / `submit`);
   - **Эффекты, атрибуты и звуки**: `Holder<Attribute>`, `Holder<MobEffect>`, `Holder<SoundEvent>` (`SoundEvents.*.value()`), `AttributeModifier(Identifier, double, Operation)` (`ADD_VALUE`, `ADD_MULTIPLIED_BASE`, `ADD_MULTIPLIED_TOTAL`), `MobEffect` (`shouldApplyEffectTickThisTick`, `applyEffectTick(ServerLevel, LivingEntity, int)`), `MobEffects.{SLOWNESS,SPEED,RESISTANCE,STRENGTH}`;
   - **Компоненты текста и чат**: `Component` неизменяем (`comp.copy().withStyle(...)`), `CompatChatFormatting` для `getName`/`getByName`/`getColor`, `player.sendSystemMessage(msg)` и `player.sendOverlayMessage(msg)`;
   - **Взаимодействия и предметы**: `InteractionResult` без generic-параметра (`InteractionResult.{SUCCESS,PASS,FAIL,CONSUME}`), удаление `.tab(...)` из `Item.Properties`, `BlockBehaviour.Properties.of()`;
   - **Выпивание/поедание**: в 26.x звук и анимацию питья берёт **компонент** `minecraft:consumable` (`Consumable`: `consumeSeconds`, `animation`, `sound`, `hasConsumeParticles`), а не сам предмет: `Item#use`/`#finishUsingItem`/`#getUseDuration`/`#getUseAnimation` смотрят в `DataComponents.CONSUMABLE`, а звук во время питья проигрывается из `ItemStack#onUseTick`. Модовые зелья без этого компонента пьются молча — поэтому всем выпиваемым предмета мода (`AlchemyPotionItem`, `AlchemyConsumableItem`, `CookingConsumableItem`, зелья сброса, зелье смерти) задан свой `Consumable` со стандартным звуком `entity.generic.drink` / `entity.generic.eat` (хелперы `ItemUtils.drinkConsumable` / `ItemUtils.eatConsumable`), а финальный «глоток» проигрывается в `finishUsingItem` через `ItemUtils.playConsumeSound`;
   - **Клиент, GUI и клавиши**: `Minecraft.getInstance().gui.setScreen(...)` и `.gui.screen()`, `GuiGraphicsExtractor` в `HudRenderCallback` и `Screen.extractRenderState`, `KeyMapping(name, keyCode, KeyMapping.Category)` + `KeyMappingHelper.registerKeyMapping`, `SingleQuadParticle`.

## 4. Слой совместимости `com.robertx22.forge_compat` (~95 классов)

Сохраняет архитектуру мода и переводит вызовы на Fabric API 0.163.0+26.3:

| Подсистема | Реализация |
|---|---|
| **GUI и рендеринг** (`forge_compat.client`: `Screen`, `AbstractContainerScreen`, `Button`, `ImageButton`, `AbstractGui`, `RenderSystem`, `Tessellator`, `DefaultVertexFormats`, `MultiBufferSource`, `ItemBlockRenderTypes`, `RenderType`, `LightTexture`) | Мост между классическим `PoseStack`/`Screen`/`Button` API мода и новым `GuiGraphicsExtractor` / `RenderPipelines` в MC 26.3 |
| **Предметы и материалы** (`forge_compat.common`: `ArmorItem`, `ArmorMaterial`, `ArmorMaterials`, `SwordItem`, `AxeItem`, `ItemNameBlockItem`, `Tier`, `Tiers`, `CreativeModeTab`, `CompatChatFormatting`) | Совместимые базовые классы взамен удалённых в 1.21.2+/26.3 иерархий инструментов/брони/вкладок |
| **Датаген** (`forge_compat.data`: `ShapedRecipeBuilder`, `ShapelessRecipeBuilder`, `CookingRecipeBuilder`, `IFinishedRecipe`, `CriterionTriggerInstance`, `EnchantedItemTrigger`, `HashCache`, `DataProvider`) | Совместимые генераторы JSON-рецептов и датапаков мода |
| **EventBus и события** (`MinecraftForge.EVENT_BUS`, `LivingHurtEvent`, `LivingDamageEvent`, `LivingDeathEvent`, `LivingEvent`, `PlayerEvent.*`, `PlayerInteractEvent.*`, `AttackEntityEvent`, `TickEvent.*`, `EntityJoinWorldEvent`) | Fabric callbacks + mixin-мосты (`LivingTickBridgeMixin`, `LivingDamageBridgeMixin`) |
| **Капабилити** (`Capability`, `CapabilityManager`, `@CapabilityInject`, `ICapabilitySerializable`, `LazyOptional`, `AttachCapabilitiesEvent`) | Ленивое привязывание к объектам при первом обращении, NBT-сериализация через `EntityNbtCapBridgeMixin` (`ValueInput`/`ValueOutput`), перенос при смерти через `ServerPlayerEvents.COPY_FROM` |
| **Сеть и реестры** (`SimpleChannel`, `NetworkRegistry`, `PacketDistributor`, `NetworkHooks`, `DeferredRegister`, `RegistryObject`, `ForgeRegistries`) | Fabric Networking API v1 (`PayloadTypeRegistry.clientboundPlay()`/`serverboundPlay()`, `ServerPlayNetworking`/`ClientPlayNetworking`), `ExtendedScreenHandlerType`, `BuiltInRegistries` |

## 5. Локализация (ANT-49) — выполнена полностью

- `assets/mmorpg/lang/ru_ru.json` — **1622 ключа** (100% покрытие `en_us.json`);
- `assets/divine_missions/lang/ru_ru.json` — **51 ключ**;
- `assets/infinite_dungeons/lang/ru_ru.json` — **39 ключей**;
- **0 пропусков и 0 лишних ключей**, все плейсхолдеры (`%1$s`, `%2$s`, `[VAL1]`, `[calc:…]`)
  и цветовые коды `§` сохранены.
