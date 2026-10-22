# Age of Epoch — Full 2025 Mine and Slash Suite Port (MC `26.3` / Java `25`)

## Executive Summary

The legacy 1.16.5 codebase of **Age of Epoch** has been completely replaced and unified with the entire 6-repository 2025 **Mine and Slash Rework** ecosystem on **Minecraft `26.3` (Fabric `0.163.0+26.3`, Trinkets `4.2.1+26.3`, OpenJDK `25`)**:

1. **`library_of_exile`** (`RobertSkalko/Library-of-Exile-Rework`) — Datapack registries, `EfficientRegistryPacket` networking, NBT `ICap` capabilities, relic system, map dimension framework, and dungeon room structure placement (`server.getStructureManager().get(...)`).
2. **`orbs_of_crafting`** (`RobertSkalko/orbs_of_crafting`) — All `43` crafting currencies/orbs, `64` item modifications, and `61` item requirements.
3. **`mmorpg`** (`RobertSkalko/Mine-And-Slash-Rework`) — Full 2025 ARPG core: `741` stats, `645` perks across `2` talent/ascendancy trees, `99` spells across `6` spell schools, Stat Souls, Unique Gears (`51`), Gems (`63`), Support Gems (`59`), Auras (`24`), Runes (`22`), Runewords (`9`), Omens (`8`), Prophecies (`36` modifiers + `5` starts), Chaos Stats (`6`), Loot Chests (`6`), and all `9` Professions (`798` profession station recipes).
4. **`dungeon_realm`** (`RobertSkalko/dungeon_realm`) — Map Device block & block entity, `17` dungeons, Uber Boss fragments & arenas, Dungeon Relics, and Home Pearls.
5. **`ancient_obelisks`** (`RobertSkalko/ancient_obelisks`) — Obelisk blocks, tiered wave encounters, and Obelisk dimension structures.
6. **`the_harvest`** (`RobertSkalko/the_harvest`) — Harvest portals, arenas, and crafting material rewards.
7. **Preserved Custom `Age-of-Epoch` Systems** — `Towers` (`TowerStructureProcessor`, `TowerCollapse`, `TowerDestroyer`, `TowerBoss`), `Delves` (`DelveGenerator`, `DelveMarkerBlockEntity`, `DelveStructureProcessor`), `GameMaster` adaptive difficulty & XP scaling, `QuestBook` (`19` quests), `Guides` (`3` guides), `MobPlates` overhead HUD plates, and `Simulation` (`mmorpg:simulation`).

---

## Verification Summary (OpenJDK `25.0.2+10-LTS` + Calibrated MC `26.3` API Oracle)

| Verification Gate | Result |
| --- | --- |
| **Real MC `26.3` Baseline Proof (`1,565` `bb7437b` MC `26.3` files)** | **`EXIT CODE: 0` (`0` errors, `2,011` `.class` files)** |
| **Strict MC `26.3` Project Compile (`2,209` `.java` files, `-25`)** | **`EXIT CODE: 0` (`0` errors, `2,983` `.class` files)** |
| **JVM Constant-Pool Bytecode Audit (`2,983` `.class` files)** | **`0` stale 1.20.1 method / constructor / field descriptors** |
| **Live Headless `ModInitializer` & Datapack Reload (`PostBundleReloadVerifier`)** | **`EXIT CODE: 0` (`56` reload listeners, `0` parse/load errors)** |
| **Live `ExileRegistry` Population (`Database.getRegistry(...)`)** | **`60 / 60` registries populated (`3,658` entries, `0` empty registries)** |
| **Live `BuiltInRegistries` & Worldgen Processors** | **`2,091` items (`423` mod), `1,325` blocks (`31` mod), `tower_processor=true`, `delve_processor=true`** |
| **MC `26.3` Datapack Schemas (`structure/`, `loot_table/`, `recipe/`, `dimension/`, `dimension_type/`, `tags/`)** | **`640` `.nbt` structures, `137` workbench recipes, `16` loot tables, `17` tags, `5` dimensions (`2,448` strict RFC-8259 JSONs)** |
| **MC `26.3` Client Item Definitions (`assets/<ns>/items/**/*.json`) & Textures** | **`423` item definitions, `1,948` PNG textures, `8` OGG sounds** |
| **Bilingual Localization (`en_us.json` + `ru_ru.json` across all 6 namespaces)** | **`3,548` EN keys / `3,548` RU keys (`100.00%` parity, `0` empty files)** |

---

## Key MC `26.3` Compatibility & Runtime Integration Fixes

- **MC `26.3` GUI, Input & Container Screens**:
  - Migrated custom screens (`GuidesScreen`, `QuestBookScreen`, `SimulationScreen`) to `extractRenderState(GuiGraphicsExtractor, int, int, float)` and `CraftingStationScreen` to `tick()` (`super.tick()`), `extractTooltip(...)`, and Fabric `Screens.getWidgets(this)`.
  - Restored visible rendering, click handling (`MouseButtonEvent`), and hover tooltips across all `15` `ImageButton` subclasses (`BackpackButton`, `SpellButton`, `SuppGemOverlayButton`, `CraftButton`, `ItemButton`, `LockButton`, `PrimaryMatInfoButton`, `RecipeButton`, `CharacterStatsButtons`, `FavorButton`, `ProfessionLevelsButton`, `CardPickButton`, `InvGuiButton`, `LearnClassPointButton`, `MainProphecyButton`), plus `AllocateStatButton` (`super.extractContents`), `MainHubScreen` custom action buttons (`Квесты`, `Гайды`, `Симуляция`), `PerkButton`, `SchoolButton`, `BigSchoolButton`, `ToonEntry`, `WikiEntry`, `BufferInfo`, and `BackpackQuickLootButton`.
  - Wired `GuiOverlays.registerOverlay(new RegisterGuiOverlaysEvent())` and `ScreenEvents.AFTER_INIT` in `AgeOfEpochClientMod` so all `4` Mine-and-Slash HUD overlays (`SpellHotbarOverlay`, `SpellCastBarOverlay`, `RPGGuiOverlay`, `EffectsOverlay`) and chest quick-loot buttons render automatically.
  - Eliminated duplicate default GLFW key bindings in `SpellKeybind` / `KeybindsRegister` (`mmorpg:keys`) so `R/V/C/G` cast Spells `1–4`, `Shift+R/V/C/G` (or `F7` swap / custom binds) cast Spells `5–8`, `H` opens Hub, `K` opens QuestBook, `J` cycles Mob Plate style, and `P` drinks health potions.
- **MC `26.3` Combat, Loot, Entities, Mixins & Worldgen**:
  - Updated `DamageEvent` (3-arg `DamageSource` + `LivingEntityAccesor` `damageCooldownTime` / `lastHurt` handling), `HealthUtils` (`at.hasModifier(mod.id())`), `IsDayCondition` (`getOverworldClockTime()`), `TeleportTargetToSourceAction` (`noCollision(AABB)`), `SimpleProjectileEntity`, `SummonEntity`, `SlashEntities`, `UnstuckMobs`, `InteractionResultHandler` (8-arg `playLocalSound`), `ParticleUtils` (`ParticleTypes.SWEEP_ATTACK`), `CommonEvents` (`Item.releaseUsing`), `DungeonRoomPlacer` (6-arg `StructureProcessor.processBlock` with `BlockPos templateRelativePos`), and `LootTableItem` (`net.minecraft.util.Prediction.SERVER_ONLY`).
  - Replaced removed `LootContext.getOptionalParameter(...)` calls with `LootContext.getOptional(...)` in `AddSpawnerExtraLootMethod`, `OnBlockDropMining`, and `OnBlockDropFarming`, and updated `LevelMixin` to configure `cir.getReturnValue()` directly without calling removed `new WorldBorder()`.
  - Wired `LivingAttackEvent` (`hurtServer` HEAD), `SimulationManager` immortality guards, `EntityInfoComponent` player damage credit (`onDamagedBy`), `ServerPlayerMixin` death hook, `EntityAttributeCreationEvent` summon attributes, `EntityJoinLevelEvent`, `EntityItemPickupEvent`, `TowerBoss` / `DelveMarkerBlockEntity` death & tick hooks, and `world_of_exile:tower_processor` / `world_of_exile:delve_processor` codecs in `BuiltInRegistries.STRUCTURE_PROCESSOR`.
  - Removed obsolete Forge `MixinConnector.java` stubs across all packages.
