# Age of Epoch — Full 2025 Mine and Slash Suite Port (MC `26.3` / Java `25`)

## Executive Summary

The legacy 1.16.5 codebase of **Age of Epoch** has been completely replaced and unified with the entire 6-repository 2025 **Mine and Slash Rework** ecosystem on **Minecraft `26.3` (Fabric `0.163.0+26.3`, Trinkets `4.2.1+26.3`, OpenJDK `25`)**:

1. **`library_of_exile`** (`RobertSkalko/Library-of-Exile-Rework`) — Datapack registries, `EfficientRegistryPacket` networking, NBT `ICap` capabilities, relic system, map dimension framework, and dungeon room structure placement (`server.getStructureManager().get(...)`).
2. **`orbs_of_crafting`** (`RobertSkalko/orbs_of_crafting`) — All `43` crafting currencies/orbs, `64` item modifications, and `61` item requirements.
3. **`mmorpg`** (`RobertSkalko/Mine-And-Slash-Rework`) — Full 2025 ARPG core: `741` stats, `645` perks across `2` talent/ascendancy trees, `99` spells across `6` spell schools, Stat Souls, Unique Gears (`51`), Gems (`63`), Support Gems (`59`), Auras (`24`), Runes (`22`), Runewords (`9`), Omens (`8`), Prophecies (`36` modifiers + `5` starts), Chaos Stats (`6`), Loot Chests (`6`), and all `9` Professions (`798` profession station recipes).
4. **`dungeon_realm`** (`RobertSkalko/dungeon_realm`) — Map Device block & block entity, `17` dungeons, Uber Boss fragments & arenas, Dungeon Relics, and Home Pearls.
5. **`ancient_obelisks`** (`RobertSkalko/ancient_obelisks`) — Obelisk blocks, tiered wave encounters, and Obelisk dimension structures.
6. **`the_harvest`** (`RobertSkalko/the_harvest`) — Harvest portals, arenas, and crafting material rewards.
7. **Preserved Custom `Age-of-Epoch` Systems** — `Towers` (`TowerStructureProcessor`, `TowerCollapse`, `TowerBoss`), `Delves` (`DelveGenerator`, `DelveMarkerBlockEntity`, `DelveStructureProcessor`), `GameMaster` adaptive difficulty & XP scaling, `QuestBook` (`19` quests), `Guides` (`3` guides), `MobPlates` overhead HUD plates, and `Simulation` (`mmorpg:simulation`).

---

## Verification Summary (OpenJDK `25.0.2` + Max-Strict MC `26.3` API Oracle)

| Verification Gate | Result |
| --- | --- |
| **Max-Strict MC `26.3` Baseline Proof (`1,565` real `bb7437b` MC `26.3` files)** | **`EXIT CODE: 0` (`0` errors)** |
| **Max-Strict MC `26.3` Project Compile (`2,217` `.java` files, `-source 25 -target 25`)** | **`EXIT CODE: 0` (`0` errors, `2,989` `.class` files)** |
| **Full JVM Bytecode Linker Audit (`FullLinkerAudit` across all `2,989` `.class` files)** | **`0` broken class / field / method descriptors** |
| **Production Bootstrap & Datapack Reload (`PostBundleReloadVerifier`, `MODID_TO_GENERATE_DATA = false`)** | **`EXIT CODE: 0` (`56` reload listeners, `3,331` JSON objects loaded, `0` parse errors)** |
| **Live `ExileRegistry` Population (`Database.getAllRegistries()`)** | **`60 / 60` registries populated (`3,658` entries, `0` empty registries)** |
| **Live Gameplay Serialization Smoke Tests (`GearBlueprint` x10 + `StatSoulData`)** | **`10 / 10` passed + `StatSoulData` round-trip `true`** |
| **MC `26.3` Datapack Schemas (`structure/`, `loot_table/`, `recipe/`, `dimension/`, `dimension_type/`, `tags/`)** | **`640` `.nbt` structures, `137` workbench recipes, `16` loot tables, `17` tags, `5` dimensions (`2,448` strict RFC-8259 JSONs)** |
| **MC `26.3` Client Item Definitions (`assets/<ns>/items/**/*.json`) & Textures** | **`423` item definitions, `1,948` PNG textures, `8` OGG sounds** |
| **Bilingual Localization (`en_us.json` + `ru_ru.json` across all 6 namespaces)** | **`3,545` EN keys / `3,545` RU keys (`100.00%` parity, `0` placeholder mismatches)** |

---

## Key MC `26.3` Compatibility & Production Bug Fixes

- **Commands Permission API**: Updated `CommandBuilder.java` and all `25` command classes from removed `CommandSourceStack.hasPermission(int)` to MC `26.3` `Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)` / `Commands.hasPermission(Commands.LEVEL_ALL)`.
- **Dimension & Chunk Generation**: Removed legacy `ChunkGenerator` subclassing and unparameterized `new GameRules()`; configured all map dimensions (`dungeon_realm:dungeon`, `ancient_obelisks:ancient_obelisks`, `the_harvest:the_harvest`, `mmorpg:simulation`) on MC `26.3` `DimensionType` (`has_fixed_time`, `has_ender_dragon_fight`, `skybox`) + `"minecraft:flat"` void generator with chunk building driven through `MapGenEvents.MAP_CHUNK_GEN`.
- **Registries, Tags & Keys**: Migrated `ResourceKey.location()` and record `TagKey<T>(registry, identifier)` calls to `.identifier()`, `HorizontalDirectionalBlock.FACING` / `BlockStateProperties` to `EnumProperty<Direction>`, `MenuScreens` registration through `MenuScreensCompat.register(...)`, 3-argument `KeyMapping` with `InputConstants.Type.KEYBOARD`, and `mc.gui.screen()`.
- **Tooltip & Item Hook Safety**: Added both 4-arg and 6-arg `ClientTooltipComponent.extractImage` implementations on `SocketTooltip` and removed self-recursive `TeleportBackItem.getUseDuration`.
- **Production `MnsConstructor` Initialization Order**: Registered `LootChests.init()`, `new ProphecyStarts().registerAll()`, and `SpecialStats.init()` prior to `GeneratedData.addAllObjectsToGenerate()` so all `60 / 60` `ExileRegistry` containers populate cleanly in normal gameplay (`MODID_TO_GENERATE_DATA == false`).
- **MC `26.3` Datapack Directories & Workbench Recipes**: Migrated all plural 1.20 datapack paths to MC `26.3` singular directories (`structure/`, `loot_table/`, `recipe/`, `tags/item/`, `tags/block/`), synced `trinkets_updated` item tags, and generated all `137` MC `26.3` crafting table recipes (`"result": {"id": ..., "count": ...}`).
