@title Зелья, еда и свитки
@en Consumables
@category materials
@tags зелья,еда,свитки,расходники,potions
@item mmorpg:alchemy/potion/health/0
@item mmorpg:alchemy/potion/mana/0
@item mmorpg:alchemy/potion/health/1
@item mmorpg:alchemy/potion/mana/1
@item mmorpg:alchemy/potion/health/2
@item mmorpg:alchemy/potion/mana/2
@item mmorpg:alchemy/potion/health/3
@item mmorpg:alchemy/potion/mana/3
@item mmorpg:alchemy/potion/health/4
@item mmorpg:alchemy/potion/mana/4
@item mmorpg:craft_result/cooking/1
@item mmorpg:craft_result/cooking/2
@item mmorpg:craft_result/cooking/3
@item mmorpg:craft_result/cooking/4
@item mmorpg:craft_result/cooking/5
@item mmorpg:craft_result/alchemy/1
@item mmorpg:craft_result/alchemy/2
@item mmorpg:craft_result/alchemy/3
@item mmorpg:craft_result/alchemy/4
@item mmorpg:craft_result/inscribing/1
@item mmorpg:craft_result/inscribing/2
@item mmorpg:craft_result/inscribing/3
@item mmorpg:craft_result/inscribing/4

Зелья и еда — расходники, которые делают бой управляемым. Зелья восстановления
варятся в алхимии, еда готовится на кухне, свитки пишутся в письменности.
Все они делятся на пять уровней, и уровень расходника должен соответствовать уровню персонажа:
духовное зелье на 40 уровне восстанавливает сущие крохи.

## Зелья восстановления

| Уровень | Мир | Здоровье | Мана |
|---|---|---|---|
| Духовное (Tier 0) | 1–10 | `alchemy/potion/health/0` | `alchemy/potion/mana/0` |
| Небесное (Tier 1) | 10–20 | `alchemy/potion/health/1` | `alchemy/potion/mana/1` |
| Эмпиреанское (Tier 2) | 20–30 | `alchemy/potion/health/2` | `alchemy/potion/mana/2` |
| Ангельское (Tier 3) | 30–40 | `alchemy/potion/health/3` | `alchemy/potion/mana/3` |
| Божественное (Tier 4) | 40–50 | `alchemy/potion/health/4` | `alchemy/potion/mana/4` |

Зелья восстанавливают **процент** ресурса, а не фиксированное число, поэтому они не устаревают
так быстро, как кажется. Но требования к уровню и стоимость крафта растут.

## Еда и свитки

- **Еда** (`craft_result/cooking/1…5`) даёт временные баффы: здоровье, регенерацию,
  сопротивления. Сила баффа зависит от уровня еды и от стата «сила еды»
  (см. [[uniques/autumn_harvest|«Осенний урожай»]] и
  [[uniques/witch_brew|«Ведьмин отвар»]]).
- **Свитки письменности** (`craft_result/inscribing/1…4`) — расходные эффекты поддержки.
- **Алхимические смеси** (`craft_result/alchemy/1…4`) — готовые зелья из рецептов профессии.

## Как это готовить

1. Возьми инструмент профессии ([[craft/tools|посуда, ступка, ткань]]).
2. Открой рецепты профессии и выбери уровень.
3. Положи основные материалы и [[materials/ingredients|ингредиенты]].
4. Скрафти: результат зависит от уровня профессии и редкости ингредиентов.

## Правила, о которых забывают

- Зелья не срабатывают мгновенно: у них есть анимация использования.
- Еда не складывается: новый бафф заменяет старый.
- Расходники не работают в творческом режиме «просто так» — их всё равно надо держать в руке.

## Смотрите также

[[craft/alchemy|Алхимия]] · [[craft/cooking|Кулинария]] ·
[[craft/professions|Профессии]] · [[craft/tools|Инструменты]] ·
[[combat/resources|Мана и энергия]]
