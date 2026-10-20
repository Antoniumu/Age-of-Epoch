# Отчёт о сборке Age of Epoch (ANT-50)

Дата: 2026-10-20. Рабочая папка `C:\Code\Age-of-Epoch`.
Связка: JDK 25.0.4.1, Gradle 9.7.1, Fabric Loom 1.18.4, Minecraft 26.3, Fabric API 0.163.0+26.3.

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

```powershell
$env:JAVA_HOME = "<JDK 25>"
$env:GRADLE_USER_HOME = "C:\Code\Age-of-Epoch\.gradle-home"
.\gradlew.bat build
```

Быстрый цикл проверки компиляции без Gradle (полный список ошибок, ~40 c):

```powershell
.\gradlew.bat dshClasspath          # обновляет build/dsh-cp.txt и build/dsh-sources.txt
javac -J-Xmx3G -Xmaxerrs 20000 -encoding UTF-8 --release 25 -proc:none `
      -cp @build/dsh-cp.txt -d build/dsh-classes @build/dsh-sources.txt
```

Задача `dshClasspath` и флаг `-PdshMaxErrs` — вспомогательные, добавлены в `build.gradle`.
