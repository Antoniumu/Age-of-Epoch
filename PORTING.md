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
