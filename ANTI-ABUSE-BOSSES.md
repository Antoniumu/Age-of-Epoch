# Анти-абьюз боссов и новые мувсеты

## Проблема

Игроки расстреливали боссов из укрытия:
1. **Крыша** — пробивали дырку в потолке и били сверху, вне зоны досягаемости.
2. **Пиллар** — ставили столб из блоков под собой и расстреливали босса с высоты.
3. **Стена/туннель** — прокапывали стену или ставили блоки между собой и боссом.
4. **Выход за арену** — отходили за границу комнаты.
5. **Запирание босса** — обносили босса блоками и били из-за стенки.

Особенно ярко это проявлялось в башнях (TOWERS.md §9) и оставалось не закрыто
в делвах (DELVES.md «Нет защиты от прокапывания»).

## Решение — система «Boss Anti-Abuse»

Реализована в `com.robertx22.world_of_exile.world_gen.TowerBoss` и
`com.robertx22.world_of_exile.tower.TowerAbility` и автоматически
подхватывается как башенными боссами, так и боссами делвов.

### 1. Единый тег и арена босса

- Новый тег `aoe_boss` обозначает любого босса, вокруг которого жива активная арена.
- Башни по-прежнему вычисляют форму арены BFS по блокам постройки; делвы
  регистрируют фиксированную арену 11×8×11 через `TowerBoss.registerFixedArena`.
- Тэг `aoe_delve_boss` отмечает делвовых стражей отдельно, чтобы их смерть не
  запускала `TowerCollapse`.

### 2. Запрет на ломку и установку блоков (`denyBreak`/`denyPlace`)

- Пока босс жив, в радиусе арены (+6 блоков по горизонтали, до maxY+4 по
  вертикали) нельзя ломать:
  - пол под боссом (3 блока),
  - **крышу** до 4 блоков над боссом в радиусе 6 (дырка в потолке больше не
    выкапывается),
  - стены в радиусе 5 блоков от босса,
  - любые блоки под ногами живых игроков в арене (чтобы не ставили
    пиллар, а потом и сам столб под собой выкапывать было нельзя).
- Нельзя ставить блоки внутри арены и буфера над ней (нельзя поднять пиллар,
  построить стенку, замуровать босса).
- Творческий режим и спектатор не ограничены.
- Сундуки, бочки и спаунеры остаются доступными для ломания (лут и спавнеры).
- Игроку раз в 3 секунды показывается сообщение «Нельзя ломать/ставить
  блоки здесь — босс поблизости.»

Интеграция через `PlayerBlockBreakEvents.BEFORE` и `UseBlockCallback.EVENT`
в `FabricBootstrap`.

### 3. Целеполагание игнорирует укрытие

- `nearestFighter` теперь рассматривает цели не только внутри арены, но и в
  буфере: до +6 блоков по XZ и до +12 блоков по верху.
- Контейн-тик сбрасывает цель, если текущая цель вышла за буфер.
- Прямая видимость больше не нужна, чтобы цель стала приоритетной — анти-абьюз
  способности сами достают игрока за стеной.

### 4. Новые анти-абьюз способности (мувсет)

В `TowerAbility` добавлены 7 новых способностей, и все rank-3/rank-4 боссы
получили их по своему стилю боя:

| Способность | Что делает | Кому достаётся |
|---|---|---|
| `ROOF_SMASH` | Босс бьёт в потолок: пробивает кольцо 5×5 блоков крыши над собой и наносит AOE урон в цилиндре над собой. Звук взрыва, частицы лавы. | Все rank-4 (легендарные) боссы и тяжёлые типы. |
| `LEAP` | Босс прыгает по параболе к цели и при приземлении делает GROUND_POUND: AOE урон 5 блоков, отброс, выбивает кратер 5×5×1 в слабом полу. Звук взрыва, облако. | Тяжёлые/звериные боссы (ravager, zoglin, hoglin), военачальники. |
| `PILLAR_BREAK` | Выбивает до 4 блоков под ногами цели, которая стоит выше босса, и наносит урон. | Лёгкие/средние боссы 3-го ранга (пиллар-читers). |
| `GRAPPLE_PULL` | Притягивает цель к боссу по цепочке частиц душ и даёт небольшой начальный урон, если цель была выше. | «Капитаны», жрецы, маги, левиафан, палачи. |
| `LINE_CHARGE` | Рывок по прямой до 8 блоков, ломает тонкие преграды (все хрупкие блоки до obsidian/bedrock), урон по линии, отброс. | Рыцари, стражи, кабаноподобные, скелеты-воины, чёрнокаменный владыка. |
| `CEILING_BARRAGE` | 3–7 метеоров по позиции цели и вокруг; под целью ломается блок, на котором она стоит (сбивает пиллар). | Все маги (witch, evoker, blaze), древний иссушитель. |
| `PHASE_ENRAGE` | Разовая фаза на 35% HP: сбрасывает все кулдауны, даёт скорость, силу и короткое сопротивление, Growl дракона, частицы. | Все rank-4 боссы. |

Боссы НЕ телепортируются к читеру (как было только у BLINK). Они честно
проламывают себе путь к цели: бьют крышу, ломают пиллар, притягивают
цепями, летят прыжком, несутся в рывке.

Классы «небьющихся» блоков, которые босс НЕ может разбить: bedrock,
end_portal/_frame/_gateway, nether_portal, barrier, structure_void, jigsaw,
respawn_anchor, enchanting_table, ender_chest, chest/trapped_chest/barrel,
spawner, command blocks, anvils. Это сохраняет сокровищницы, алтари и
системные блоки целыми.

### 5. Кто получил какие абилки

- **Тяжеловесы (ravager-боссы: conqueror, ancient_beast, snow_titan, dune_lord,
  ancient_yaksha, peat_giant, clay_titan, beast_king, mountain_colossus,
  spore_colossus, blackstone_lord, crimson_lord, delta_titan, savanna_storm):**
  LEAP + ROOF_SMASH + THUNDER_STOMP/KNOCKBACK_SLAM + RAGE + SUMMON_MINIONS
  + PHASE_ENRAGE.
- **Маги/жрецы (herbalist, winter_priestess, bog_witch, shadow_caster,
  renegade_witch, abyss_priest, myconid_seer, flame_mage, bone_priest,
  soul_collector, crimson_priest, incinerator, sanctum_lord):**
  CEILING_BARRAGE + свои стихийные абилки + BLINK или HEAL_SELF.
  sanctum_lord дополнительно уходит в PHASE_ENRAGE.
- **Рыцари/бойцы ближнего боя (grave_knight, glacier_warden,
  temple_guardian, fortress_master, pass_warden, order_executioner,
  bone_baron, basalt_sentinel, pirate_captain):** LINE_CHARGE + SHIELD/RAGE
  + GRAPPLE_PULL или PILLAR_BREAK.
- **Ловкие (shadow_matriarch, cliff_spirit, gloom_herald, rot_matriarch,
  bone_cowboy, canyon_spirit, scorpion_matriarch, ridge_archer, broodmother):**
  BLINK или LINE_CHARGE + PILLAR_BREAK/GRAPPLE_PULL.
- **Особые:**
  - `ancient_leviathan` (elder_guardian) — FROST_NOVA + LEECH + GRAPPLE_PULL +
    ROOF_SMASH + SHIELD + SUMMON_ELITE + PHASE_ENRAGE.
  - `ancient_witherer` — WITHER_CURSE + FIRE_NOVA + LINE_CHARGE + GRAPPLE_PULL +
    SUMMON_MINIONS + SHIELD + PHASE_ENRAGE.
- **Боссы делвов** (delve wardens) получают стандартный anti-cheese набор:
  KNOCKBACK_SLAM, THUNDER_STOMP, ROOF_SMASH, LEAP, CEILING_BARRAGE,
  PILLAR_BREAK, GRAPPLE_PULL, LINE_CHARGE, RAGE, SHIELD, HEAL_SELF,
  PHASE_ENRAGE (без SUMMON_MINIONS/ELITE — волны управляются делвом).

### 6. LEAP-landing (GROUND_POUND)

Реализован как кумулятивный эффект: `LEAP` задаёт скорость отскока и
записывает время посадки в `LEAP_LAND_TIME`. Общий тик `TowerAbility.tick()`
отслеживает приземление и вызывает `groundPound()` в момент касания земли —
такой подход не нуждается в отдельной enum-константе и не срабатывает, если
босс ещё летит.

### 7. Безопасность

- Босс НЕ строит платформ под ногами и НЕ возится с миром, кроме ситуаций,
  где игрок явно спровоцировал разрушение блоков (крушит крышу, пиллар,
  преграду рывком).
- Только блоки с прочностью ≤50 и не в списке «запретных» могут быть разбиты.
- Воронка GROUND_POUND не трогает сундуки/бочки/спаунеры.
- Contain-телепорт срабатывает раз в секунду, только если босс реально вышел
  за пределы арены (предохранитель от выпадения через край), и только на
  существующий пол. Обычные прыжки, лестницы, отброс от атак не триггерят
  возврат.
- После смерти босса теги снимаются, арена исчезает из кеша по таймауту (10 сек),
  и игрок снова может строить/ломать.

### 8. Дельвы

- `DelveMarkerBlockEntity.spawn()` теперь помечает босса тегами
  `aoe_boss` + `aoe_delve_boss` с домашней точкой в центре комнаты и
  регистрирует фиксированную арену 11×8×11.
- Уровень ранга = номер этажа (1..4).
- Тема приводится к ближайшей башенной (frost→ice, sands→desert,
  roots→forest, crypt→plains).
- `fight()` больше не навязывает таргетинг боссу — `TowerBoss.contain`
  делает это сам.

## Как проверить

### CI / сборка
```bash
./gradlew build
python3 tools/tower_gen/generate.py
python3 -m unittest discover -s tools/tower_gen -p 'test_*.py' -v
python3 -m unittest discover -s tools/delve_gen -v
```
(В песочнице на данный момент нет JDK; код компиляционно-совместим с
Fabric-Loom 1.18/Minecraft 26.3 по структуре импортов.)

### В игре

1. **Крыша.** Пробить дырку над боссом в survival — блок не ломается. Если
   всё же оказаться над боссом (например, элитра/эндер-перл), босс применяет
   ROOF_SMASH, пробивает крышу 5×5 и раскидывает игрока уроном.
2. **Пиллар.** Поставить столп под собой в комнате босса не выходит (denyPlace).
   Установить столп ДО пула босса — при появлении босса при попытке встать
   на столб срабатывает CEILING_BARRAGE (разбивает блок под ногами, падаешь)
   или PILLAR_BREAK (выбивает столб), а LEAP/GRAPPLE_PULL подтягивают босса
   вплотную.
3. **Стена.** Построить стену из булыжника между собой и боссом не выходит.
   Прорыть стену боком не даёт denyBreak. В стене, построенной заранее,
   LINE_CHARGE ломает тонкий слой и влетает в упор.
4. **За пределом.** Выйти за арену и бить — босс использует ROOF_SMASH/
   CEILING_BARRAGE по позиции игрока за краем; LEAP долетает.
5. **Фаза.** Когда босс опускается до 35% HP — один раз срабатывает
   PHASE_ENRAGE (сброс кулдаунов, баффы, рык дракона).
6. **Делвы.** Та же самая проверка, босс этажа должен сломать столб/крышу
   комнаты и добраться до игрока.

## Структура кода

- `src/main/java/com/robertx22/world_of_exile/world_gen/TowerBoss.java`
  - Новые теги BOSS_TAG, DELVE_BOSS_TAG, DELVE_HOME_PREFIX.
  - markDelveBoss / registerFixedArena / defaultBossAbilities.
  - Переписан denyBreak (крыша/стены/пиллар/пол), добавлен denyPlace.
  - contain действует для любого BOSS_TAG-моба.
  - nearestFighter расширен до буфера над ареной.
  - onDeath различает башенных и делвовых боссов.
  - sendCantBuild — персональное троттлирование сообщений на игрока.
- `src/main/java/com/robertx22/world_of_exile/tower/TowerAbility.java`
  - Добавлены ROOF_SMASH, LEAP (с ground pound по приземлении),
    PILLAR_BREAK, GRAPPLE_PULL, LINE_CHARGE, CEILING_BARRAGE,
    PHASE_ENRAGE (одноразовая активация через PHASE_TRIGGERED).
  - LEAP_LAND_TIME и PHASE_TRIGGERED как состояния.
  - isBreakableForCharge() общий для всех способностей, ломающих блоки.
- `src/main/java/com/robertx22/world_of_exile/tower/TowerRoster.java`
  - Обновлены наборы способностей всех rank-3 и rank-4 боссов. Каждый
    rank-4 имеет хотя бы один двигатель к цели (LEAP/LINE_CHARGE/BLINK)
    и PHASE_ENRAGE. Каждый rank-3 имеет анти-абьюз абилку под свой стиль.
- `src/main/java/com/robertx22/world_of_exile/delve/DelveMarkerBlockEntity.java`
  - Спавн делвовых боссов теперь вешает BOSS_TAG/DELVE_BOSS_TAG,
    регистрирует арену, передаёт ранг и тему.
  - fight() больше не тикает абилки сам — это делает TowerAbility.tick().
- `src/main/java/com/robertx22/age_of_exile/mmorpg/FabricBootstrap.java`
  - Добавлен UseBlockCallback-хук на denyPlace; импорт BlockPos.

## Известные ограничения / TODO

- В sandbox нет JDK 21 и полной среды MC, поэтому **./gradlew build** не был
  запущен локально — требуется запуск CI/сборка на машине с JDK и выходом в
  maven для загрузки зависимостей.
- Эффект LEAP визуально использует серверные частицы CLOUD/EXPLOSION и звук
  взрыва; если захочется красивую анимацию разбега/приземления, это можно
  добавить на клиенте в дальнейшем.
- Видео/скриншоты геймплея будут сняты при следующем прогоне клиента в
  ручном тесте (сейчас в песочнице нет клиентского дисплея).
