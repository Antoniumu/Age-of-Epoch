# План переноса механики классов (`SpellSchool` + `Ascendancy`) из Mine and Slash Rework в Age of Epoch

> **Источник:** [`RobertSkalko/Mine-And-Slash-Rework`](https://github.com/RobertSkalko/Mine-And-Slash-Rework) (`v6.3.7`, Minecraft 1.20.1, Forge, пакет `com.robertx22.mine_and_slash`)  
> **Приёмник:** `Age of Epoch` (Minecraft **26.3** «Wilderness Bound», Fabric API `0.163.0+26.3`, Java 25, пакет `com.robertx22.age_of_exile` + слой `com.robertx22.forge_compat`)  
> **Статус документа:** Технический план переноса и декомпозиция по этапам

---

## 1. Сравнительный анализ двух систем (что меняется концептуально)

В обоих модах внутри кодовой базы реестр называется **`SpellSchool`** (`ExileRegistryTypes.SPELL_SCHOOL`, `SpellSchoolsAdder`, `SchoolBuilder`), но в `Mine-And-Slash-Rework` старая система 5 стихийных школ была полностью переработана в **систему классов персонажа**:

| Аспект | Сейчас в `Age of Epoch` (база AoE 1.16.5) | В `Mine-And-Slash-Rework` (целевая система) |
|---|---|---|
| **Сущность `SpellSchool`** | **5 школ магии**: `divine`, `hunting`, `nature`, `fire`, `water` | **6 базовых классов** (`Words.Classes`, `mmorpg.asc_class.<id>`): **`sorcerer`** (Чародей), **`warlock`** (Чернокнижник), **`minstrel`** (Менестрель), **`hunter`** (Охотник), **`shaman`** (Шаман), **`warrior`** (Воин) |
| **Мультикласс** | До 2 школ (хранится `Set<String> schools` в `SpellsData`) | **Строго до 2 классов одновременно** (`MAX_2_CLASSES`); список активных классов вычисляется динамически по изученным перкам (`allocated_lvls > 0`) — полный сброс ветки автоматически освобождает слот второго класса |
| **Узлы внутри класса** | `spells` + устаревшие `synergies` (всего 3 синергии на весь мод) | Единая сетка `HashMap<String, PointData> perks` (`11 × 7`, `SLOT_SPACING = 21`):<br>• **слева (`x = 1..6`, `y = 0..6`)** — **активные навыки класса** (`PerkBuilder.spell`, макс. ур. `16`, слот `spells/slots/spell.png`);<br>• **справа (`x = 8..9`, `y = 1..6`)** — **8 пассивных умений класса** (`SpellPassives`, макс. ур. `8`, арочный слот `spells/slots/passive.png`);<br>• система `Synergy` упразднена |
| **Требования по уровню рядов** | `lvl_reqs = [1, 5, 10, 20, 30, 40, 50]` | `lvl_reqs = [1, 5, 10, 15, 20, 25, 30]` (вшиты цифрами слева прямо в текстуру `asc_classes/background.png`!) + проверка уровня персонажа при повышении ранга навыка (`isLevelEnoughForSpellLevelUp`) |
| **Очки прокачки и сброс** | Единый пул `SPELL_POINTS` без селективного отката в UI | **Два независимых пула очков** (`PlayerPointsType.SPELLS` и `PlayerPointsType.PASSIVES`) + **очки сброса (`reset_points`)**: **ЛКМ** — вложить очко, **ПКМ** — откатить 1 уровень за 1 очко сброса (`AllocateClassPointPacket.ACTION.REMOVE`) + зелья малого/полного сброса |
| **Окно класса (`250×233`)** | `SpellScreen` (`spells/spell_school_background.png`), стрелки `←/→`, снизу 8 слотов хотбара | `SpellSchoolScreen` (`asc_classes/background.png`): сверху герб текущего класса (`BigSchoolButton` `36×36`), стрелки `←/→`, по бокам — **2 кнопки быстрого переключения своих выбранных классов** (`LEFT_SCHOOL` / `RIGHT_SCHOOL` `25×25`) поверх тематических баннеров `93×36`, вертикальный разделитель «Навыки \| Пассивки», снизу два счётчика (`Spell Points` / `Passive Points`) |
| **Назначение на хотбар (слоты 1–8)** | Drag-and-drop и клик по `PickHotbarButton` внизу `SpellScreen` | В M&S перенесено в `SkillGemsScreen` (`SpellButton` → `InvGuiScreen` / `PickSpellAction`), т.к. нижнюю планку `asc_classes/background.png` заняли два счётчика очков *(см. раздел 5.2 — гибридное решение для Age of Epoch!)* |
| **Подклассы / Восхождения *(опциональный контур Б)*** | Отсутствуют (есть авторская страница `divine.json` в `TALENTS`) | Отдельная вкладка хаба **`AscendancyTree`** (`ascendancy.json`, 14 подклассов по 7 крупных перков, пул `PlayerPointsType.ASCENDANCY`, 112 иконок `talent_icons/asc/*`) |

---

## 2. Полная инвентаризация контента 6 классов (`SpellSchoolsAdder`)

Сверка всех **81 уникальных активных заклинаний** и **48 классовых пассивок** из `Mine-And-Slash-Rework` с текущей базой `Age-of-Epoch`:

- **Активные заклинания:** `81` всего → **`42` уже есть в `Age-of-Epoch`**, **`39` новых** (требуют переноса описаний `SpellBuilder` + недостающих `SpellAction`/`ExileEffect`/сущностей призыва).
- **Классовые пассивки (`SpellPassives`):** `48` активных (по **8 на каждый класс**, макс. ранг `8`) + `1` резервная (`p_missile_barrage`).

### Сводная таблица по каждому из 6 классов

| ID класса | Название (EN / RU) | Роль и тематика | Уже есть в AoE (42) | Новые заклинания из M&S (39) | Пассивки класса — `SpellPassives` (по 8 шт., макс. ур. 8) |
|---|---|---|---|---|---|
| **`sorcerer`** | Sorcerer / **Чародей** | Огонь, лёд, големы стихий, телепорт, маг. щит | `fireball` (1,0), `fire_nova` (1,1), `meteor` (1,2), `frostball` (3,0), `frost_nova` (3,1), `chilling_field` (3,2), `ice_comet` (3,3), `heart_of_ice` (5,2), `mage_circle` (5,4), `refresh` (5,5) *(10)* | `summon_fire_golem` (1,5), `frozen_orb` (3,4), `summon_cold_golem` (3,5), `blizzard` (3,6), `teleport` (5,3), `frost_lich_armor` (5,0) *(6)* | `p_mana_regen` (8,1), `p_spell_dmg` (9,1), `p_burn_chance` (8,2), `p_freeze_chance` (9,2), `p_magic_shield_regen` (8,3), `p_ele_res` (8,4), `p_cast_speed_sorc` (8,5), `p_golem_chance` (8,6) |
| **`warlock`** | Warlock / **Чернокнижник** | Хаос/яд, призыв нежити и пауков, проклятия, DoT-вампиризм | `poison_ball` (1,0), `poison_cloud` (1,4), `draconic_blood` (5,4) *(3)* | `summon_spider` (1,3), `black_hole` (1,6), `summon_zombie` (3,0), `chilling_touch` (3,1), `return_summons` (3,2), `summon_skeleton_army` (3,3), `curse_of_agony` (5,1), `explode_minions` (5,2), `curse_of_weak` (5,3), `curse_of_despair` (5,5) *(10)* | `p_poison_chance` (8,1), `p_spell_lifesteal` (8,2), `p_summon_dmg` (9,2), `p_dot_dmg` (8,3), `p_poison_duration` (8,4), `p_cast_speed_wl` (9,4), `p_dmg_to_cursed` (8,5), `p_dot_lifesteal` (8,6) |
| **`minstrel`** | Minstrel / **Менестрель** | Боевые песни, гимны-ауры, исцеление группы, КДР | `song_of_vigor` (3,0), `song_of_perseverance` (3,2), `song_of_valor` (3,4), `healing_aura` (5,1), `shooting_star` (5,2), `awaken_mana` (5,4), `wish` (5,6) *(7)* | `power_chord` (1,0), `ritardando` (1,3), `resonance` (1,5) *(3)* | `p_song_duration` (8,1), `p_heal_str` (9,1), `p_health_mins` (8,2), `p_heal_to_spell` (9,2), `p_inc_aoe` (8,3), `p_low_hp_heal` (8,4), `p_mana_cost` (8,5), `p_cdr` (8,6) |
| **`hunter`** | Hunter / **Охотник** | Стрельба из лука, ловушки, призрачный волк, мобильность | `charged_bolt` (1,1), `explosive_arrow` (1,3), `recoil_shot` (1,4), `arrow_storm` (1,6), `fire_trap` (3,0), `frost_trap` (3,2), `poison_trap` (3,5), `dash` (5,1), `hunter_potion` (5,3), `smoke_bomb` (5,4) *(10)* | `arrow_barrage` (1,0), `quickdraw` (1,2), `arrow_totem` (2,3), `meteor_arrow` (1,5), `summon_spirit_wolf` (6,1), `gale_wind` (6,4) *(6)* | `p_proj_dmg` (8,1), `p_ene_regen` (9,1), `p_trap_dmg` (8,2), `p_dodge` (9,2), `p_proj_spd` (8,3), `p_trap_cdr` (8,4), `p_summon_health` (9,4), `p_crit_dmg` (8,5) |
| **`shaman`** | Shaman / **Шаман** | Молнии, стихийные и лечащие тотемы/цветы, шипы природы | `rejuv_totem` (3,1), `astral_totem` (3,3), `entangling_seed` (5,2) *(3)* | `lightning_spear` (1,0), `lightning_nova` (1,2), `chain_lightning` (1,3), `thorn_bush` (3,0), `lightning_totem` (3,2), `magma_flower` (3,4), `chaos_totem` (3,5), `rejuvenation` (5,0), `circle_of_healing` (5,1), `garden_of_thorns` (5,3), `frost_flower` (5,5) *(11)* | `p_electrify_chance` (8,1), `p_totem_dmg` (9,1), `p_health_sha` (8,2), `p_mana_sha` (9,2), `p_lightning_ele` (8,4), `p_totem_cdr` (9,4), `p_armor_per_mana` (8,5), `p_ele_pen` (8,6) |
| **`warrior`** | Warrior / **Воин** | Удары ближнего боя, кровотечение, провокация, стяжка, танк | `gong_strike` (1,0), `flame_strike` (1,3), `tidal_strike` (2,3), `whirlwind` (1,4), `charge` (2,4), `taunt` (4,1), `pull` (4,2), `vamp_blood` (4,4), `undying_will` (4,6) *(9)* | `shred` (1,2), `execute` (1,5), `boomerang` (5,1) *(3)* | `p_health_war` (8,1), `p_armor_pen` (8,2), `p_health_regen` (8,3), `p_block_chance` (9,3), `p_crit_hit` (8,4), `p_lifesteal` (9,4), `p_aura_effect` (8,5), `p_damage_received` (8,6) |

---

## 3. Полная карта переносимых текстур и иконок (`assets/mmorpg/textures/`)

Все `100%` необходимых графических ассетов проверены в `Mine-And-Slash-Rework` и готовы к прямому копированию без рисования заглушек:

1. **Фон окна классов и баннеры шапки (`7` PNG):**
   - `textures/gui/asc_classes/background.png` (`256×256`, рабочая область `250×233`) — фон экрана `SpellSchoolScreen` с гравировкой уровней `1 / 5 / 10 / 15 / 20 / 25 / 30`, разделителем «Активные / Пассивные» и двумя плашками очков внизу;
   - `textures/gui/asc_classes/background/{sorcerer,warlock,minstrel,hunter,shaman,warrior}.png` (`93×36` × 6 шт.) — атмосферные фоны верхних боковых панелей выбранного класса.
2. **Гербы / иконки классов (`6` активных + `13` резервных PNG):**
   - `textures/gui/asc_classes/class/{sorcerer,warlock,minstrel,hunter,shaman,warrior}.png` (`36×36` × 6 шт.) — рендерятся как `36×36` в центре (`BigSchoolButton`) и с масштабированием до `25×25` в слотах выбранных классов (`SchoolButton`);
   - *(опционально в `unused/`)* `divine.png`, `elementalist.png`, `fighter.png`, `paladin.png`, `ranger.png`, `storm.png`, `unholy.png` и др. — полезны для будущих кастомных классов `Age of Epoch`.
3. **Рамки слотов навыков и пассивок (`2` новых + `1` существующий PNG):**
   - `textures/gui/spells/slots/spell.png` (`18×18`) — уже есть в AoE (проверить идентичность палитры с M&S);
   - `textures/gui/spells/slots/passive.png` (`18×18`, новый) — арочная рамка пассивного перка класса;
   - `textures/gui/spells/slots/overlay.png` (`18×18`, новый) — полупрозрачный блик/окантовка поверх иконки слота;
   - `textures/gui/empty_spell.png` (`16×16`) — иконка пустого слота заклинания.
4. **Иконки всех пассивок классов (`49` PNG):**
   - `textures/gui/spells/passives/p_*.png` (`16×16` × 49 шт. — все 48 активных пассивок `SpellPassives` + `p_missile_barrage.png`).
5. **Недостающие иконки активных заклинаний классов (`40` PNG):**
   - `textures/gui/spells/icons/{arrow_barrage,arrow_totem,black_hole,blizzard,boomerang,chain_lightning,chaos_totem,chilling_touch,circle_of_healing,curse_of_agony,curse_of_despair,curse_of_weak,execute,explode_minions,frost_flower,frost_lich_armor,frozen_orb,gale_wind,garden_of_thorns,lightning_nova,lightning_spear,lightning_totem,magma_flower,meteor_arrow,power_chord,quickdraw,recoil_shot,rejuvenation,resonance,return_summons,ritardando,shred,summon_cold_golem,summon_fire_golem,summon_skeleton_army,summon_spider,summon_spirit_wolf,summon_zombie,teleport,thorn_bush}.png` (`16×16` × 40 шт.).
6. **Иконки главного хаба (`H`):**
   - `textures/gui/main_hub/icons/spells.png` (уже есть; обновить под стиль M&S при необходимости) и `textures/gui/main_hub/icons/ascendancy.png` (`16×16`, для фазы Восхождений).
7. *(При включении Контура Б — Восхождения `Ascendancy`):*
   - `textures/gui/talent_icons/asc/*.png` и `textures/gui/talent_icons/asc/<subclass>/<0..6>.png` (`112` PNG `16×16`/`32×32`) + `textures/gui/asc_classes/perk/**` (`120` PNG `16×16`).

---

## 4. Ключевые особенности адаптации под Minecraft 26.3 и архитектуру Age of Epoch

Прямое копирование Java-файлов из M&S 1.20.1 невозможно без учёта пяти архитектурных отличий `Age of Epoch`. План закладывает чистые решения для каждого из них:

### 4.1. Сериализация игрока (`@Storable` / `@Store` в `info.loenwind.autosave`)
- В M&S 1.20.1 данные игрока сериализуются через Gson, а в `Age of Epoch` (`RPGPlayerData`, `EntitySpellCap`, `SpellsData`) — через **`LoadSave` + аннотации `@Storable` / `@Store`** поверх `CompoundTag.get*Or(...)` (MC 26.3).
- **Решение:** снабдить `SpellSchoolsData` (`allocated_lvls`), `PlayerPointsData` (`bonus_points`, `cheat_points`, `reset_points` по каждому `PlayerPointsType`) и обновлённый `SpellsData` (`hotbars`, `extra_lvls`) аннотациями `@Storable` / `@Store`.
- **Безопасная миграция старых сохранений:** при загрузке `SpellsData` из старого мира, если в `schools` встречаются удалённые ID (`divine`, `hunting`, `nature`, `fire`, `water`) или старые синергии (`frostball_chill`, `fireball_burn`, `poisonball_poison`), удалять несуществующие записи из `allocated_lvls` и `hotbars` без краша — благодаря динамическому расчёту `freePoints = totalPoints - spentPoints` игроку **автоматически вернутся все потраченные очки** для выбора новых классов!

### 4.2. Проблема хотбара заклинаний (слоты 1–8) в `SpellSchoolScreen`
- В `Mine-And-Slash-Rework` нижняя панель `asc_classes/background.png` (`y + 206`) занята двумя плашками счётчиков **`Spell Points`** и **`Passive Points`**, потому что выбор заклинаний на панель быстрого доступа уехал в контейнер камней умений (`SkillGemsScreen`), которого в `Age of Epoch` нет.
- **Решение (лучшее из двух миров):**
  1. Сохранить оригинальный вид `asc_classes/background.png` (`250×233`) с обоими счётчиками (`Очки навыков` слева, `Очки пассивок` справа, тултип `Очки сброса`).
  2. Под окном (`guiTop + sizeY + 4`) отрисовать аккуратную **пристыкованную панель из 8 слотов хотбара (`PickHotbarButton`)** с сохранением уже реализованного в `Age of Epoch` **Drag-and-Drop** (перетаскивание изученного навыка ЛКМ на слот `1..8`, подсветка рамок слотов, клик по слоту для выбора навыка + автопостановка первого изученного навыка в свободный слот).
  3. При полном откате навыка ПКМ до уровня `0/16` на сервере автоматически очищать соответствующие ячейки `SpellsData.hotbars`.

### 4.3. Слой GUI под MC 26.3 (`forge_compat.client`)
- Перевести `SpellSchoolScreen`, `BigSchoolButton`, `SchoolButton`, `LearnClassPointButton`, `PointsDisplayButton` с `GuiGraphics` / `ResourceLocation` / `Tooltip.create` на API `Age of Epoch` (`Identifier.fromNamespaceAndPath`, `BaseScreen`, `com.robertx22.forge_compat.client.Button` / `ImageButton`, `PoseStack`, `AbstractGui.bindTexture`, `GuiUtils.renderTooltip`, `TextUtils`), как это уже сделано в `CharacterScreen`, `TalentsScreen` и `QuestBookScreen`.
- В `LearnClassPointButton` добавить визуальную индикацию доступности ряда (затемнение/замок при недостаточном уровне игрока или при достижении лимита 2 классов, цвет текста уровня `0/16`..`16/16` от `GRAY` → `GREEN` → `AQUA` → `LIGHT_PURPLE` → `GOLD` → `DARK_PURPLE` при бонусе `+ к уровню навыков`).

### 4.4. Призыв существ (`SummonSpells` vs `com.robertx22.age_of_exile.summoning`) и анимации
- **Сущности призыва:** 7 активных заклинаний призыва у Чародея, Чернокнижника и Охотника (`summon_fire_golem`, `summon_cold_golem`, `summon_spirit_wolf`, `summon_zombie`, `summon_spider`, `summon_skeleton_army` + `summon_lightning_golem`) используют кастомные `EntityType` (`SlashEntities`) или единый контроллер прислужников.
  - В `SlashEntities` + `RenderRegister` регистрируем сущности големов/духа волка/паука/нежити под MC 26.3 (`EntityType.Builder.of(...).build(ResourceKey)`, рендереры `EntityRenderer<T, S>` на базе ванильных `IronGolemRenderer`/`WolfRenderer`/`CaveSpiderRenderer`/`ZombieRenderer`/`SkeletonRenderer` с масштабированием и тонировкой стихии) **и интегрируем их с `MinionUtils`**, чтобы:
    - активные миньоны классов и пассивные миньоны древа `divine.json` считались союзниками игрока, не атаковали друг друга и корректно реагировали на `return_summons`, `chilling_touch` (`COMMAND_SUMMONS_ATTACK`) и `explode_minions`!
- **Анимации каста (`SpellAnimations`):** в M&S используется внешний мод `playerAnimator` (`1.20.1`). Чтобы не добавлять внешнюю runtime-зависимость под MC 26.3, сделать `SpellAnimations` лёгкой обёрткой (взмах рукой `setSwingArm()` + частицы каста).

### 4.5. Баланс уровней `Age of Epoch` (макс. уровень `50` vs `100` в M&S)
- В `Age of Epoch` максимальный уровень персонажа — **`50`** (ряды класса требуют уровни `1, 5, 10, 15, 20, 25, 30` — идеально ложатся в диапазон `1..50`!), тогда как в M&S `MAX_LEVEL = 100`.
- **Формула очков и скейлинга под макс. уровень 50:**
  - `PlayerPointsType.SPELLS`: `1 + 1 × уровень` (или текущий `2 × уровень` из `GameBalanceConfig` — вынести в конфиг `GameBalanceConfig` с дефолтами на выбор);
  - `PlayerPointsType.PASSIVES`: `1 × уровень` (50 очков к 50 уровню = 6+ полностью прокачанных пассивок `8/8` между двумя классами);
  - порог повышения ранга заклинания `isLevelEnoughForSpellLevelUp`: адаптировать шаг скейлинга так, чтобы заклинание 30-го ряда могло достичь `16/16` к `50` уровню (например, `step = (50 - baselvl) / 15.0`, т.е. у навыка 1-го ур. шаг ~3 ур., у навыка 30-го ур. — ~1.3 ур., и на 50 ур. доступны все `16/16`!).

---

## 5. Поэтапный план реализации (Декомпозиция работ)

### Этап 1. Ресурсы, иконки и модели данных (`Data & Assets Layer`)
1. Скопировать из `Mine-And-Slash-Rework` все графические ассеты:
   - `assets/mmorpg/textures/gui/asc_classes/**` (фон окна, 6 гербов классов `36×36`, 6 баннеров `93×36`);
   - `assets/mmorpg/textures/gui/spells/slots/{passive,overlay}.png` и `empty_spell.png`;
   - `assets/mmorpg/textures/gui/spells/passives/p_*.png` (49 иконок пассивок);
   - 40 недостающих иконок заклинаний в `assets/mmorpg/textures/gui/spells/icons/*.png`.
2. Обновить/добавить классы данных и пойнтов:
   - `PlayerPointsType` (`TALENTS`, `SPELLS`, `PASSIVES`, `STATS`, резерв под `ASCENDANCY`) и `@Storable` `PlayerPointsData` в `RPGPlayerData`;
   - `LearnSpellStat` (`database/data/stats/types/LearnSpellStat.java`) — связка `Perk ↔ Spell`;
   - расширить `Perk` и `PerkBuilder` методами `PerkBuilder.spell(id)`, `PerkBuilder.passive(id, maxlvl, stats...)`, `isSpell()`, `getSpell()`, `isPassive()`, `getPointType()`, `getSpellSchool()` *(сохранив `Perk.exclusive_branch` из `DIVINE.md`!)*;
   - обновить `SpellSchool` (`perks`, `lvl_reqs = [1, 5, 10, 15, 20, 25, 30]`, `getIconLoc()`, `getBackgroundLoc()`, `isLevelEnoughFor()`, `isLevelEnoughForSpellLevelUp()`) и `SchoolBuilder`;
   - реализовать `@Storable` `SpellSchoolsData` в `RPGPlayerData` (`ascClass`), подключив его `SimpleStatCtx(PASSIVES)` и уровни изученных заклинаний к расчёту `Unit` и `EntitySpellCap.getLevelOf(id)` (включая учёт бонусных уровней `extra_lvls` от экипировки!).

### Этап 2. Перенос статов, `SpellPassives` и 39 недостающих заклинаний (`Content & Mechanics Layer`)
1. Проверить и добавить в реестр `StatsRegister` все недостающие статы для 48 классовых пассивок `SpellPassives`:
   - `GolemSpellChance` (`p_golem_chance`), `DamageToCursed` (`p_dmg_to_cursed`), `SpellLifesteal` / `DotLifesteal`, `EffectDurationPerTag(song)` (`p_song_duration`), `LowHpHealing` (`p_low_hp_heal`), `HealToSkillDmg` (`p_heal_to_spell`), `DamagePerSpellTag(trap / totem)`, `CooldownReductionPerSpellTag(trap / totem)`, `ArmorPerMana` (`p_armor_per_mana`), `DamageReceived` (`p_damage_received`), `AuraEffect` (`p_aura_effect`).
2. Перенести `SpellPassives.java` и зарегистрировать все 48 пассивок в `AllPerks.registerAll()`.
3. Автоматизировать генерацию `PerkBuilder.spell(id)` для всех заклинаний, входящих в `SpellSchoolsAdder`.
4. Перенести недостающие компоненты заклинаний и **39 новых заклинаний** по пакетам:
   - новые `SpellAction` / `EffectCondition` / `ExileEffect`: `CommandSummonsAction`, `TpToCaster`, `KnockbackAction`, `AddChargeAction`, `AutoAimingProj`, проклятия (`Agony`, `Weakness`, `Despair`), баффы големов (`Fire/Ice/Lightning Golem Buff`), `Frost Lich Armor`, песни Менестреля;
   - сущности призыва (`SlashEntities` + MC 26.3 рендереры + интеграция с `MinionUtils`);
   - новые и обновлённые школы заклинаний: `BasicAttackSpells`, `LightningSpells`, `SummonSpells`, `PetSpells`, дополнения в `CurseSpells`, `SongSpells`, `FireSpells`, `WaterSpells`, `NatureSpells`, `HolySpells`, `RangerSpells`, `StrSpells`, `DexSpells`, `IntSpells`, `TotemSpells`;
   - зарегистрировать все 6 классов в `SpellSchoolsAdder.java` (`sorcerer`, `warlock`, `minstrel`, `hunter`, `shaman`, `warrior`) и удалить устаревший `SynergiesAdder` / `Synergy` (с сохранением миграции старых сейвов).
5. Обновить `PlusSkillsInSchoolStats` (`+X к уровню навыков класса Чародея / Чернокнижника / Менестреля / Охотника / Шамана / Воина`), зелья сброса в `AlchemyPotions` (малое зелье сброса навыков/пассивок + полный сброс классов) и админ-команды `/slash points give/reset`.

### Этап 3. Сетевой слой и новый UI экрана классов (`Network & UI Layer`)
1. Заменить `AllocateSpellPacket` и `AllocateSynergyPacket` в `C2SPacketRegister` на **`AllocateClassPointPacket`** (`ACTION.ALLOCATE` / `ACTION.REMOVE`), выполняющий серверную валидацию (`ExplainedResult` с понятным сообщением в чат при нехватке очков, низком уровне или попытке взять 3-й класс), пересчёт статов `Unit` и синхронизацию `RPGPlayerData` + `EntitySpellCap` на клиент.
2. Реализовать пакет UI в `com.robertx22.age_of_exile.gui.screens.spell`:
   - **`SpellSchoolScreen`** (замена `SpellScreen` или рефакторинг с сохранением совместимых точек входа):
     - рендер `asc_classes/background.png` (`250×233`) и парных баннеров `asc_classes/background/<id>.png` (`93×36`);
     - **`BigSchoolButton`** (`36×36` по центру шапки) с расширенным русским тултипом (название класса, архетип/роль, вложено очков навыков и пассивок, статус «Выбран 1/2» / «Доступен» / «Заблокирован — уже выбрано 2 класса»);
     - **`LEFT_SCHOOL` и `RIGHT_SCHOOL` (`SchoolButton`, `25×25`)** — кликабельные иконки 1-го и 2-го выбранного класса с подсветкой активной вкладки (и пустой рамкой-подсказкой «Свободный слот класса», пока выбран < 2 классов);
     - **`LeftRightButton`** (`22×22`) — перелистывание всех 6 классов с превью следующего/предыдущего класса в тултипе;
     - **`LearnClassPointButton`** (`18×18`) — рендер слота `spell.png` / `passive.png` + иконка `16×16` + `overlay.png` + счётчик `ур./макс.`, **ЛКМ** (изучить/улучшить), **ПКМ** (откатить за очко сброса), богатый тултип с предпросмотром статов и требуемого уровня;
     - **`PointsDisplayButton`** — нижние плашки `Очки навыков: X` и `Очки пассивок: Y` (при наведении показывает `Очки сброса: Z`);
     - **Пристыкованная снизу панель 8 слотов хотбара (`PickHotbarButton` + Drag-and-Drop)** и плашка подсказки `GuideAdvisor`.
3. Обновить **`CharacterScreen` (`H`-хаб)**:
   - переименовать вкладку «Навыки» → **«Классы»** (`Words.Classes`), подключить `IAlertScreen` (мигающий индикатор на кнопке вкладки в хабе, когда есть свободные очки навыков **или** пассивок класса), выводить выбранные классы игрока (например, *«Чародей / Чернокнижник»*) на главном экране персонажа.

### Этап 4. Интеграция с кастомными подсистемами `Age of Epoch`
1. **Система Билд-Гайдов (`guides/` + `tools/guides/` + `GUIDES.md`):**
   - перевести `GuideDefinition` / `GuideAdvisor` / `GuidesScreen` с одиночного поля `school` + `synergies` на поддержку **1–2 классов (`schools`) и пассивок класса (`passives`)**;
   - подсветка следующего рекомендуемого навыка или пассивки прямо на `LearnClassPointButton` в `SpellSchoolScreen`;
   - пересобрать `tools/guides/plans.json`, `build_guides.py` и `GUIDES.md` под новые 6 классов.
2. **Полигон (`SimulationScreen`):**
   - обновить кнопку открытия экрана классов и автосборку билда в 1 клик (начисление/распределение очков `SPELLS` + `PASSIVES`).
3. **Квестбук (`QuestBookData` / `docs/QUESTBOOK.md`):**
   - актуализировать квестовые шаги знакомства со школами/навыками под выбор 1-го и 2-го класса и прокачку первой пассивки класса;
   - проверить, что страница древа талантов `divine.json` («Путь некроманта / Путь призывателя») продолжает штатно работать во вкладке «Таланты» и синергирует с классом `Warlock` / `Hunter`.

### Этап 5. Локализация (`100% EN + RU`) и автоматическая верификация
1. Добавить все новые ключи одновременно в `assets/mmorpg/lang/en_us.json` и `assets/mmorpg/lang/ru_ru.json` (соблюдая стандарт ANT-49: **0 пропусков, 0 лишних ключей**, сохранение всех плейсхолдеров `%1$s`, `[VAL1]`, `[calc:...]` и цветовых кодов `§`):
   - 6 классов `mmorpg.asc_class.*`;
   - 48 пассивок `mmorpg.talent.p_*` и все новые статы `mmorpg.stat.*` / `mmorpg.stat_desc.*`;
   - 39 новых заклинаний `mmorpg.spell.*` / `spell.desc.*` и эффекты `mmorpg.effect.*`;
   - строки UI и ошибок (`mmorpg.word.classes`, `mmorpg.chat.max_2_classes`, `mmorpg.gui.passive_points`, `mmorpg.gui.respec_points`, зелья сброса).
2. Написать проверочный скрипт **`tools/verify_classes.py`** (по образцу `tools/talent_gen/verify_divine.py`), автоматически проверяющий:
   - все 6 классов в `SpellSchoolsAdder`, отсутствие коллизий `PointData(x, y)` и попадание в сетку `0..10 × 0..6`;
   - регистрацию всех `81` заклинаний, `48` пассивок и всех используемых ими статов и эффектов;
   - физическое наличие в `src/main/resources/assets/mmorpg/textures/` **каждой** PNG-текстуры фона, баннера, герба класса, слота, иконки заклинания и пассивки;
   - `100%` совпадение ключей `en_us.json` ↔ `ru_ru.json`;
   - прогон `python3 tools/check_java_syntax.py` по всем изменённым и новым `.java` файлам (0 ошибок синтаксиса Java 25).

---

## 6. Опциональный Этап 6 (Контур Б): Подклассы / Восхождения (`Ascendancy`)

Если помимо 6 базовых классов (`SpellSchool`) требуется перенести из `Mine-And-Slash-Rework` и **вторую ступень специализации — Восхождения (`Ascendancy`)**:

1. **Данные и сетка:** перенести `src/main/resources/data/mmorpg/talent_tree/ascendancy.json` (14 круговых созвездий подклассов: *Arcanist, Death Lich, Chieftain, Champion, Raider, Hunter, Trickster, Assassin, Necromancer, Elementalist, Guardian, Battlemage, Jester, Ascendant*).
2. **Перки (98 крупных узлов + 14 стартовых):** перенести `AscendancyKey.java` и `AscendancyPerks.java`, адаптировав скейлинг и заряды (`Power / Frenzy / Endurance Charges`) под `Age of Epoch`.
3. **Ассеты:** скопировать `112` иконок `textures/gui/talent_icons/asc/**` и иконку вкладки `textures/gui/main_hub/icons/ascendancy.png`.
4. **UI и очки:** добавить вкладку `AscendancyTree` (на базе `SkillTreeScreen` с `SchoolType.ASCENDANCY`) в `CharacterScreen` (`H`-хаб) и начислять очки Восхождения за ключевые рубежи прогрессии (например, закрытие башен из `TOWERS.md` / этажей экспедиций `DELVES.md` или уровни 15 / 30 / 40 / 50).

---

## 7. Открытые дизайн-решения перед стартом кода

Перед переходом к реализации стоит зафиксировать 3 параметра:
1. **Объём переноса:** переносим только **6 базовых классов (`SpellSchool` — навыки + пассивки + `SpellSchoolScreen`)** или сразу включаем **Этап 6 (`Ascendancy` — 14 подклассов восхождения)**?
2. **Судьба древа `divine.json` («Некромант / Призыватель» из `docs/DIVINE.md`):** оставляем как дополнительную страницу во вкладке «Таланты» (она отлично сочетается с новым классом *Чернокнижник* и *Охотник*) или объединяем с классом `Warlock`?
3. **Бюджет очков на 50 уровне:** рекомендуемый под `MAX_LEVEL = 50` — **`SPELLS`: 2 очка/ур. (100 к 50 ур. = ~6 навыков 16/16)** и **`PASSIVES`: 1 очко/ур. (50 к 50 ур. = ~6 пассивок 8/8)**, старт Сброса — **10 бесплатных очков сброса** новому персонажу + зелья сброса.
