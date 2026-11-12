# Вознесённые классы (Ascended Classes, 50+)

Вознесённые классы — «ультимативные» пути развития, открывающиеся **после 50 уровня**.
Первый реализованный класс — **Танцор Прилива** (`tide_dancer`): водный танцор с живым
кнутом из волны, по мотивам архетипа Нилы (LoL), но в эстетике Age of Epoch.

## Основные правила

| Правило | Значение |
|---|---|
| Уровень открытия | `AscensionClasses.UNLOCK_LEVEL = 50` |
| Очки Вознесения | `max(0, уровень - 49)` — по 1 очку за каждый уровень с 50-го |
| Дерево | линейная цепочка из 6 узлов × 3 ранга |
| Гейт узла | узел N открыт, если в предыдущие узлы вложено ≥ `N` очков |
| Ранг умения | = бонус-уровень заклинания (`SpellsData.extra_lvls`), **обычные очки школ не тратятся** |
| Закрытые узлы | показываются как «?» до открытия |
| Смена класса | откатывает ранги и вычищает хотбар от умений старого класса |

Данные хранятся в `RPGPlayerData.ascension` (`AscensionData`, @Storable: `chosen_class`,
`allocated` — список, ранг узла = число вхождений его spellId). Синхронизация на клиент
идёт через штатный sync `RPGPlayerData`.

## GUI (путь игрока)

`H` → Навыки (`SpellScreen`) → кнопка **«✦ Классы»** (правый верхний угол) →
анимированный переход «разрыв воды» (`AscensionTransitionScreen`, ~0.65 c:
концентрические водяные кольца из точки клика, стена глубины, призматические искры) →
`AscendedClassesScreen`:

- **Уровень < 50** — серый силуэт пути, большой замок, красная надпись
  «У вас ещё нет 50-го уровня» + строка «Уровень: X / 50».
- **Класс не выбран** — карточки классов с эмблемой и лором, кнопка «Выбрать путь».
- **Путь класса** — цепочка из 6 узлов волной, струи между ними, пипсы рангов,
  счётчик свободных очков. ЛКМ — вложить очко, ПКМ — назначить выученное умение в
  первую свободную ячейку хотбара.

Тема экрана (цвета, фон, потоки, пузыри) берётся из `AscensionClasses` —
новый класс автоматически перекрашивает весь GUI.

## Танцор Прилива — навыки

| # | id | Название | Примечание |
|---|---|---|---|
| 1 | `tidewhip_lash` | Кнут Гребня | веерный удар водой + «Утопление» |
| 2 | `slipstream_surge` | Скользящий Бурун | рывок, 2 заряда, спираль-шлейф |
| 3 | `jubilant_veil` | Вуаль Ликования | щит + броня/уклонение, сфера-саммон |
| 4 | `unbound_joy` | Несдержанная Радость | транс: AS/MS, двойное похищение жизни |
| 5 | `undertow_grasp` | Хватка Девятого Вала | стягивание врагов к себе + утопление |
| 6 | `maelstrom_waltz` | Апофеоз: Вальс Пучины | ульт (КД 90 c): водоворот-турель 6 c, тики, затягивание в глаз, финальный разрыв |

Все навыки: `max_lvl = 3` (ранг проставляет `AscensionLogic`), уникальные кальки в
`TideDancerCalcs` (коэффициенты выше школьных — это ультимативный тир).

### Свои эффекты (ExileEffect)

| id | Название | Тип | Статы |
|---|---|---|---|
| beneficial.80 | Несдержанная Радость | buff 14 c | AS flat, lifesteal, spell lifesteal, +15% MS |
| beneficial.81 | Вуаль Ликования | buff 8 c | Dodge%, Armor%, knockback resist |
| negative.80 | Утопление | debuff 4-6 c | −водосопр., −25% скорости |

## Эффекты и частица

`TideShapeAction` (`SpellAction.TIDE_SHAPE`, GUID `tide_shape`) — параметрические
3D-фигуры из частиц. 8 форм:

`WHIP_ARC` (дуга кнута по yaw кастера), `HELIX_DOUBLE` (двойная спираль),
`TORUS`, `FUNNEL_VORTEX` (смерч-воронка), `SPHERE_SHELL` (Фибоначчи-сфера),
`WAVE_RING` (расходящееся кольцо), `COLLAPSE_RING` (схлопывающееся кольцо к центру),
`PRISM_GEYSER` (вертикальный гейзер-призма).

Параметры через MapField: `PARTICLE_SHAPE`, `PARTICLE_COUNT`, `RADIUS` (× area_multi),
`HEIGHT`, `DISTANCE` (фаза, градусы), `MOTION_MULTI` (скорость разлёта),
`PARTICLE_TYPE` (по умолчанию `mmorpg:tide_glow`).

Частица `tide_glow` (`TideGlowParticle extends SingleQuadParticle`): 3 спрайта
(16×16, `textures/particle/tide_glow/`), слой TRANSLUCENT, всплытие (gravity < 0),
fade-in/fade-out, болтанка синусом. Регистрация типа — `FabricParticleTypes.simple()`
в `SlashParticles`, фабрика — `ParticleProviderRegistry` в `ClientInit`
(обёрнуто в try/catch, чтобы сервер не падал без FAPI-клиента).

## Сеть

| Пакет | Назначение | Валидация (сервер) |
|---|---|---|
| `mmorpg:ascension_pick` | выбор/смена класса | `canPickClass` (уровень ≥ 50), откат старого класса |
| `mmorpg:ascension_alloc` | +1 ранг узлу | класс выбран, узел открыт, ранг < 3, есть очки |
| `mmorpg:ascension_hotbar` | умение → слот 0..7 | умение выучено (ранг > 0), принадлежит классу, дедуп |

## Как добавить второй Вознесённый класс

1. Новый элемент в enum `AscensionClasses` (id, имя, лор, тема-цвета, список из 6 spellId).
2. `MyClassSpells implements ExileRegistryInit` — 6 заклинаний `SpellBuilder` +
   `max_lvl = MAX_RANK`; регистрация в `Spells.registerAll()`.
3. Свои эффекты/кальки по образцу `TideEffects`/`TideDancerCalcs`.
4. Арт: `textures/gui/spells/icons/<spellId>.png` (32×32), `textures/gui/ascension/<id>_bg.png` (256)
   и `<id>_emblem.png` (64); при желании — свой цвет частиц.
5. Локализация: `mmorpg.spell.<id>`, `spell.desc.<id>`, `effect.mmorpg.*` —
   добавить в **en_us.json** (источник истины) + ru_ru, затем `python tools/audit/sync_lang.py`.

Всё остальное (очки, дерево, GUI, переходы, пакеты) подхватится автоматически.

## Файлы

```
src/main/java/com/robertx22/age_of_exile/ascension/
├── AscensionClasses.java        — реестр классов + тема + константы (UNLOCK_LEVEL, MAX_RANK)
├── AscensionData.java           — @Storable: chosen_class, allocated
├── AscensionLogic.java          — правила: очки, гейты, аллокация, выбор класса, хотбар
├── spells/TideDancerSpells.java — 6 заклинаний (билдер-цепочки)
├── spells/TideDancerCalcs.java  — калькуляторы урона/значений
├── effects/TideEffects.java     — 3 эффекта
├── fx/TideShapeAction.java      — 8 параметрических 3D-фигур
├── fx/TideGlowParticle.java     — частица tide_glow
├── fx/TideFx.java               — билдер-хелпер ComponentPart
├── gui/AscendedClassesScreen.java     — экран классов (3 режима)
├── gui/AscensionTransitionScreen.java — анимированный «разрыв воды»
└── packets/                     — AscensionPick/Alloc/HotbarPacket (C2S)
```
