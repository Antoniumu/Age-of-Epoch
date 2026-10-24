@title Эссенции и камни уровней
@en Essences
@category materials
@tags эссенции, пыль, камни, разбор, materials
@item mmorpg:mat/smelted/0
@item mmorpg:mat/salvage/salvage0
@item mmorpg:mat/condensed_salvage/c0
@item mmorpg:stone/0
@item mmorpg:mat/smelted/1
@item mmorpg:mat/salvage/salvage1
@item mmorpg:mat/condensed_salvage/c1
@item mmorpg:stone/1
@item mmorpg:mat/smelted/2
@item mmorpg:mat/salvage/salvage2
@item mmorpg:mat/condensed_salvage/c2
@item mmorpg:stone/2
@item mmorpg:mat/smelted/3
@item mmorpg:mat/salvage/salvage3
@item mmorpg:mat/condensed_salvage/c3
@item mmorpg:stone/3
@item mmorpg:mat/smelted/4
@item mmorpg:mat/salvage/salvage4
@item mmorpg:mat/condensed_salvage/c4
@item mmorpg:stone/4

Эссенции — «расходная кровь» ремесла. Их получают разбором снаряжения и переплавкой,
и они же нужны почти в каждом рецепте. Пять уровней эссенций соответствуют пяти полосам
уровней персонажа: духовная для старта, божественная для эндгейма.

## Пять уровней

| Уровень | EN | Уровни мира | Предметы |
|---|---|---|---|
| Духовный (Tier 0) | Spiritual | 1–10 | `mat/smelted/0`, `mat/salvage/salvage0`, `mat/condensed_salvage/c0`, `stone/0` |
| Небесный (Tier 1) | Celestial | 10–20 | `mat/smelted/1`, `mat/salvage/salvage1`, `mat/condensed_salvage/c1`, `stone/1` |
| Эмпиреанский (Tier 2) | Empyrean | 20–30 | `mat/smelted/2`, `mat/salvage/salvage2`, `mat/condensed_salvage/c2`, `stone/2` |
| Ангельский (Tier 3) | Angelic | 30–40 | `mat/smelted/3`, `mat/salvage/salvage3`, `mat/condensed_salvage/c3`, `stone/3` |
| Божественный (Tier 4) | Divine | 40–50 | `mat/smelted/4`, `mat/salvage/salvage4`, `mat/condensed_salvage/c4`, `stone/4` |

У каждого уровня есть четыре формы:

- **Переплавленная эссенция** (`mat/smelted/<tier>`) — основной материал крафта.
- **Очищенная пыль** (`mat/salvage/salvage<tier>`) — результат разбора, чинится в предметы.
- **Сжатая эссенция** (`mat/condensed_salvage/c<tier>`) — концентрат из нескольких пылинок.
- **Камень уровня** (`stone/<tier>`) — редкий материал для брони и инструментов.

## Как получить

1. Разбери ненужное снаряжение — получишь пыль соответствующего уровня.
2. Переплавь пыль (или сжми её) — получишь эссенцию.
3. Камни уровней выпадают из жил и сундуков, а также продаются за сезонные награды.

Разбор — самый стабильный источник: см. [[craft/salvage|разбор снаряжения]].

## Зачем это нужно

- Крафт оружия и брони требует эссенцию **своего** уровня: божественный рецепт нельзя
  собрать на духовной эссенции.
- Инструменты профессий чинятся эссенциями.
- Автоматический разбор в рюкзаке превращает мусор в пыль без ручной работы —
  см. [[systems/backpack|рюкзак и улучшения]].

## Смотрите также

[[materials/ingredients|Ингредиенты]] · [[craft/salvage|Разбор]] ·
[[craft/professions|Профессии]] · [[materials/potions|Зелья и еда]] ·
[[systems/backpack|Рюкзак]]
