# Прокачка: полный цикл

Целевой уровень: **50**, фактический: **50**

Проверок пройдено: **1**, провалено: **0**

## Этапы

- `AOEPROG stage=intro «Старт персонажа»`
- `AOEPROG intro level=1 exp=0 stat_free=2`
- `AOEPROG stage=level_10 «Прокачка 1 → 10»`
- `AOEPROG level_up 1 -> 2`
- `AOEPROG level_up 2 -> 3`
- `AOEPROG level_up 3 -> 4`
- `AOEPROG level_up 4 -> 5`
- `AOEPROG level_up 5 -> 6`
- `AOEPROG level_up 6 -> 7`
- `AOEPROG level_up 7 -> 8`
- `AOEPROG level_up 8 -> 9`
- `AOEPROG level_up 9 -> 10`
- `AOEPROG stage=alloc_stats «Очки характеристик»`
- `AOEPROG skip alloc=none — очки оставлены для GUI-сценария`
- `AOEPROG stage=level_25 «Прокачка 10 → 25»`
- `AOEPROG level_up 10 -> 11`
- `AOEPROG level_up 11 -> 12`
- `AOEPROG level_up 12 -> 13`
- `AOEPROG level_up 13 -> 14`
- `AOEPROG level_up 14 -> 15`
- `AOEPROG level_up 15 -> 16`
- `AOEPROG level_up 16 -> 17`
- `AOEPROG level_up 17 -> 18`
- `AOEPROG level_up 18 -> 19`
- `AOEPROG level_up 19 -> 20`
- `AOEPROG level_up 20 -> 21`
- `AOEPROG level_up 21 -> 22`
- `AOEPROG level_up 22 -> 23`
- `AOEPROG level_up 23 -> 24`
- `AOEPROG level_up 24 -> 25`
- `AOEPROG stage=alloc_spell «Заклинание школы»`
- `AOEPROG skip alloc=none — очки оставлены для GUI-сценария`
- `AOEPROG stage=level_50 «Прокачка 25 → 50»`
- `AOEPROG level_up 25 -> 26`
- `AOEPROG level_up 26 -> 27`
- `AOEPROG level_up 27 -> 28`
- `AOEPROG level_up 28 -> 29`
- `AOEPROG level_up 29 -> 30`
- `AOEPROG level_up 30 -> 31`
- `AOEPROG level_up 31 -> 32`
- `AOEPROG level_up 32 -> 33`
- `AOEPROG level_up 33 -> 34`
- `AOEPROG level_up 34 -> 35`
- `AOEPROG level_up 35 -> 36`
- `AOEPROG level_up 36 -> 37`
- `AOEPROG level_up 37 -> 38`
- `AOEPROG level_up 38 -> 39`
- `AOEPROG level_up 39 -> 40`
- `AOEPROG level_up 40 -> 41`
- `AOEPROG level_up 41 -> 42`
- `AOEPROG level_up 42 -> 43`
- `AOEPROG level_up 43 -> 44`
- `AOEPROG level_up 44 -> 45`
- `AOEPROG level_up 45 -> 46`
- `AOEPROG level_up 46 -> 47`
- `AOEPROG level_up 47 -> 48`
- `AOEPROG level_up 48 -> 49`
- `AOEPROG level_up 49 -> 50`
- `AOEPROG stage=ascend_pick «Вознесение: выбор пути»`
- `AOEPROG skip alloc=none — выбор пути оставлен для GUI-сценария`
- `AOEPROG stage=verify «Проверка итогов»`
- `AOEPROG ok=true verify level=50`
