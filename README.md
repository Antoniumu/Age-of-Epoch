# Аудит профессий (все ремёсла + видео)

Цель: `all`, клиент под Xvfb, ветка `main`

Обновлено: 2026-10-31 11:37 UTC · коммит `d1c0a2ba256f0044eaed35b87218a54fca9a1164` · [прогон](https://github.com/Antoniumu/Age-of-Epoch/actions/runs/41197353547)

Ветку перезаписывает workflow `.github/workflows/profession-audit.yml` при каждом прогоне — здесь всегда последний результат.

**Итог:** `AOEPR done profs=6 passed=6 failed=0`

Профессий проверено: **12**, провалилось: **0**.

## Живые шаги (что происходило в сетке крафта)

```
AOEPR result_ready prof=alchemy item=mmorpg:craft_result/alchemy/3 chance=100
AOEPR placed prof=alchemy filled=2 item=mmorpg:ingredient/red_coral
AOEPR placed prof=alchemy filled=3 item=mmorpg:ingredient/enigma_stone
AOEPR placed prof=alchemy filled=0 item=-
AOEPR taken prof=alchemy slot=-2
AOEPR crafted prof=alchemy ok=true item=mmorpg:craft_result/alchemy/1 stats=3 exp_gain=true
AOEPR prof=alchemy verdict=PASS checks=12 gui=true placed=3 result=true taken=true crafted=true item=mmorpg:craft_result/alchemy/1 stats=3 exp=true
AOEPR given prof=inscribing tool=mmorpg:craft_tools/inscribing gear=no ings=3
AOEPR gui_open prof=inscribing
AOEPR placed prof=inscribing filled=1 item=mmorpg:ingredient/soul_matrix
AOEPR result_ready prof=inscribing item=mmorpg:craft_result/inscribing/3 chance=100
AOEPR placed prof=inscribing filled=2 item=mmorpg:ingredient/redstone_crystal
AOEPR placed prof=inscribing filled=3 item=mmorpg:ingredient/magic_log
AOEPR placed prof=inscribing filled=0 item=-
AOEPR taken prof=inscribing slot=-2
AOEPR crafted prof=inscribing ok=true item=mmorpg:craft_result/inscribing/2 stats=2 exp_gain=true
AOEPR prof=inscribing verdict=PASS checks=12 gui=true placed=3 result=true taken=true crafted=true item=mmorpg:craft_result/inscribing/2 stats=2 exp=true
AOEPR given prof=cooking tool=mmorpg:craft_tools/cooking gear=no ings=3
AOEPR gui_open prof=cooking
AOEPR placed prof=cooking filled=1 item=mmorpg:ingredient/soul_matrix
AOEPR result_ready prof=cooking item=mmorpg:craft_result/cooking/2 chance=100
AOEPR placed prof=cooking filled=2 item=mmorpg:ingredient/red_coral
AOEPR placed prof=cooking filled=3 item=mmorpg:ingredient/yarrow_flower
AOEPR placed prof=cooking filled=0 item=-
AOEPR taken prof=cooking slot=-2
AOEPR crafted prof=cooking ok=true item=mmorpg:craft_result/cooking/4 stats=3 exp_gain=true
AOEPR prof=cooking verdict=PASS checks=12 gui=true placed=3 result=true taken=true crafted=true item=mmorpg:craft_result/cooking/4 stats=3 exp=true
AOEPR given prof=armor_craft tool=mmorpg:craft_tools/armor_craft gear=yes ings=3
AOEPR gui_open prof=armor_craft
AOEPR placed prof=armor_craft filled=1 item=minecraft:iron_chestplate
AOEPR placed prof=armor_craft filled=2 item=mmorpg:ingredient/soul_matrix
AOEPR result_ready prof=armor_craft item=minecraft:iron_chestplate chance=100
AOEPR placed prof=armor_craft filled=3 item=mmorpg:ingredient/fire_stone
AOEPR placed prof=armor_craft filled=4 item=mmorpg:ingredient/fire_stone
AOEPR placed prof=armor_craft filled=0 item=-
AOEPR taken prof=armor_craft slot=-2
AOEPR crafted prof=armor_craft ok=true item=minecraft:iron_chestplate stats=3 exp_gain=true
AOEPR prof=armor_craft verdict=PASS checks=18 gui=true placed=4 result=true taken=true crafted=true item=minecraft:iron_chestplate stats=3 exp=true
AOEPR given prof=wep_craft tool=mmorpg:craft_tools/wep_craft gear=yes ings=3
AOEPR gui_open prof=wep_craft
AOEPR placed prof=wep_craft filled=1 item=minecraft:iron_sword
AOEPR placed prof=wep_craft filled=2 item=mmorpg:ingredient/soul_matrix
AOEPR result_ready prof=wep_craft item=minecraft:iron_sword chance=100
AOEPR placed prof=wep_craft filled=3 item=mmorpg:ingredient/enigma_stone
AOEPR placed prof=wep_craft filled=4 item=mmorpg:ingredient/enigma_stone
AOEPR placed prof=wep_craft filled=0 item=-
AOEPR taken prof=wep_craft slot=-2
AOEPR crafted prof=wep_craft ok=true item=minecraft:iron_sword stats=3 exp_gain=true
AOEPR prof=wep_craft verdict=PASS checks=18 gui=true placed=4 result=true taken=true crafted=true item=minecraft:iron_sword stats=3 exp=true
AOEPR given prof=jewel_craft tool=mmorpg:craft_tools/jewel_craft gear=yes ings=3
AOEPR gui_open prof=jewel_craft
AOEPR placed prof=jewel_craft filled=1 item=mmorpg:jewelry/ring/iron
AOEPR placed prof=jewel_craft filled=2 item=mmorpg:ingredient/soul_matrix
AOEPR result_ready prof=jewel_craft item=mmorpg:jewelry/ring/iron chance=100
AOEPR placed prof=jewel_craft filled=3 item=mmorpg:ingredient/enigma_stone
AOEPR placed prof=jewel_craft filled=4 item=mmorpg:ingredient/enigma_stone
AOEPR placed prof=jewel_craft filled=0 item=-
AOEPR taken prof=jewel_craft slot=-2
AOEPR crafted prof=jewel_craft ok=true item=mmorpg:jewelry/ring/iron stats=3 exp_gain=true
AOEPR prof=jewel_craft verdict=PASS checks=18 gui=true placed=4 result=true taken=true crafted=true item=mmorpg:jewelry/ring/iron stats=3 exp=true
```

## Скриншоты: каждое перемещение в сетку 3×2

**00-main-menu**

[![00-main-menu](shots/00-main-menu.png)](shots/00-main-menu.png)

**01-world-list**

[![01-world-list](shots/01-world-list.png)](shots/01-world-list.png)

**02-create-world**

[![02-create-world](shots/02-create-world.png)](shots/02-create-world.png)

**03-in-world**

[![03-in-world](shots/03-in-world.png)](shots/03-in-world.png)

**90-audit-done**

[![90-audit-done](shots/90-audit-done.png)](shots/90-audit-done.png)

**91-character**

[![91-character](shots/91-character.png)](shots/91-character.png)

**alchemy-01-grid-empty**

[![alchemy-01-grid-empty](shots/alchemy-01-grid-empty.png)](shots/alchemy-01-grid-empty.png)

**alchemy-02-slot-1**

[![alchemy-02-slot-1](shots/alchemy-02-slot-1.png)](shots/alchemy-02-slot-1.png)

**alchemy-03-slot-2**

[![alchemy-03-slot-2](shots/alchemy-03-slot-2.png)](shots/alchemy-03-slot-2.png)

**alchemy-04-slot-3**

[![alchemy-04-slot-3](shots/alchemy-04-slot-3.png)](shots/alchemy-04-slot-3.png)

**alchemy-05-result-ready**

[![alchemy-05-result-ready](shots/alchemy-05-result-ready.png)](shots/alchemy-05-result-ready.png)

**alchemy-06-result-taken**

[![alchemy-06-result-taken](shots/alchemy-06-result-taken.png)](shots/alchemy-06-result-taken.png)

**alchemy-07-stored**

[![alchemy-07-stored](shots/alchemy-07-stored.png)](shots/alchemy-07-stored.png)

**alchemy-08-tooltip**

[![alchemy-08-tooltip](shots/alchemy-08-tooltip.png)](shots/alchemy-08-tooltip.png)

**alchemy-09-closed**

[![alchemy-09-closed](shots/alchemy-09-closed.png)](shots/alchemy-09-closed.png)

**armor_craft-01-grid-empty**

[![armor_craft-01-grid-empty](shots/armor_craft-01-grid-empty.png)](shots/armor_craft-01-grid-empty.png)

**armor_craft-02-slot-1**

[![armor_craft-02-slot-1](shots/armor_craft-02-slot-1.png)](shots/armor_craft-02-slot-1.png)

**armor_craft-03-slot-2**

[![armor_craft-03-slot-2](shots/armor_craft-03-slot-2.png)](shots/armor_craft-03-slot-2.png)

**armor_craft-04-slot-3**

[![armor_craft-04-slot-3](shots/armor_craft-04-slot-3.png)](shots/armor_craft-04-slot-3.png)

**armor_craft-05-slot-4**

[![armor_craft-05-slot-4](shots/armor_craft-05-slot-4.png)](shots/armor_craft-05-slot-4.png)

**armor_craft-06-result-ready**

[![armor_craft-06-result-ready](shots/armor_craft-06-result-ready.png)](shots/armor_craft-06-result-ready.png)

**armor_craft-07-result-taken**

[![armor_craft-07-result-taken](shots/armor_craft-07-result-taken.png)](shots/armor_craft-07-result-taken.png)

**armor_craft-08-stored**

[![armor_craft-08-stored](shots/armor_craft-08-stored.png)](shots/armor_craft-08-stored.png)

**armor_craft-09-tooltip**

[![armor_craft-09-tooltip](shots/armor_craft-09-tooltip.png)](shots/armor_craft-09-tooltip.png)

**armor_craft-10-closed**

[![armor_craft-10-closed](shots/armor_craft-10-closed.png)](shots/armor_craft-10-closed.png)

**cooking-01-grid-empty**

[![cooking-01-grid-empty](shots/cooking-01-grid-empty.png)](shots/cooking-01-grid-empty.png)

**cooking-02-slot-1**

[![cooking-02-slot-1](shots/cooking-02-slot-1.png)](shots/cooking-02-slot-1.png)

**cooking-03-slot-2**

[![cooking-03-slot-2](shots/cooking-03-slot-2.png)](shots/cooking-03-slot-2.png)

**cooking-04-slot-3**

[![cooking-04-slot-3](shots/cooking-04-slot-3.png)](shots/cooking-04-slot-3.png)

**cooking-05-result-ready**

[![cooking-05-result-ready](shots/cooking-05-result-ready.png)](shots/cooking-05-result-ready.png)

**cooking-06-result-taken**

[![cooking-06-result-taken](shots/cooking-06-result-taken.png)](shots/cooking-06-result-taken.png)

**cooking-07-stored**

[![cooking-07-stored](shots/cooking-07-stored.png)](shots/cooking-07-stored.png)

**cooking-08-tooltip**

[![cooking-08-tooltip](shots/cooking-08-tooltip.png)](shots/cooking-08-tooltip.png)

**cooking-09-closed**

[![cooking-09-closed](shots/cooking-09-closed.png)](shots/cooking-09-closed.png)

**inscribing-01-grid-empty**

[![inscribing-01-grid-empty](shots/inscribing-01-grid-empty.png)](shots/inscribing-01-grid-empty.png)

**inscribing-02-slot-1**

[![inscribing-02-slot-1](shots/inscribing-02-slot-1.png)](shots/inscribing-02-slot-1.png)

**inscribing-03-slot-2**

[![inscribing-03-slot-2](shots/inscribing-03-slot-2.png)](shots/inscribing-03-slot-2.png)

**inscribing-04-slot-3**

[![inscribing-04-slot-3](shots/inscribing-04-slot-3.png)](shots/inscribing-04-slot-3.png)

**inscribing-05-result-ready**

[![inscribing-05-result-ready](shots/inscribing-05-result-ready.png)](shots/inscribing-05-result-ready.png)

**inscribing-06-result-taken**

[![inscribing-06-result-taken](shots/inscribing-06-result-taken.png)](shots/inscribing-06-result-taken.png)

**inscribing-07-stored**

[![inscribing-07-stored](shots/inscribing-07-stored.png)](shots/inscribing-07-stored.png)

**inscribing-08-tooltip**

[![inscribing-08-tooltip](shots/inscribing-08-tooltip.png)](shots/inscribing-08-tooltip.png)

**inscribing-09-closed**

[![inscribing-09-closed](shots/inscribing-09-closed.png)](shots/inscribing-09-closed.png)

**jewel_craft-01-grid-empty**

[![jewel_craft-01-grid-empty](shots/jewel_craft-01-grid-empty.png)](shots/jewel_craft-01-grid-empty.png)

**jewel_craft-02-slot-1**

[![jewel_craft-02-slot-1](shots/jewel_craft-02-slot-1.png)](shots/jewel_craft-02-slot-1.png)

**jewel_craft-03-slot-2**

[![jewel_craft-03-slot-2](shots/jewel_craft-03-slot-2.png)](shots/jewel_craft-03-slot-2.png)

**jewel_craft-04-slot-3**

[![jewel_craft-04-slot-3](shots/jewel_craft-04-slot-3.png)](shots/jewel_craft-04-slot-3.png)

**jewel_craft-05-slot-4**

[![jewel_craft-05-slot-4](shots/jewel_craft-05-slot-4.png)](shots/jewel_craft-05-slot-4.png)

**jewel_craft-06-result-ready**

[![jewel_craft-06-result-ready](shots/jewel_craft-06-result-ready.png)](shots/jewel_craft-06-result-ready.png)

**jewel_craft-07-result-taken**

[![jewel_craft-07-result-taken](shots/jewel_craft-07-result-taken.png)](shots/jewel_craft-07-result-taken.png)

**jewel_craft-08-stored**

[![jewel_craft-08-stored](shots/jewel_craft-08-stored.png)](shots/jewel_craft-08-stored.png)

**jewel_craft-09-tooltip**

[![jewel_craft-09-tooltip](shots/jewel_craft-09-tooltip.png)](shots/jewel_craft-09-tooltip.png)

**jewel_craft-10-closed**

[![jewel_craft-10-closed](shots/jewel_craft-10-closed.png)](shots/jewel_craft-10-closed.png)

**wep_craft-01-grid-empty**

[![wep_craft-01-grid-empty](shots/wep_craft-01-grid-empty.png)](shots/wep_craft-01-grid-empty.png)

**wep_craft-02-slot-1**

[![wep_craft-02-slot-1](shots/wep_craft-02-slot-1.png)](shots/wep_craft-02-slot-1.png)

**wep_craft-03-slot-2**

[![wep_craft-03-slot-2](shots/wep_craft-03-slot-2.png)](shots/wep_craft-03-slot-2.png)

**wep_craft-04-slot-3**

[![wep_craft-04-slot-3](shots/wep_craft-04-slot-3.png)](shots/wep_craft-04-slot-3.png)

**wep_craft-05-slot-4**

[![wep_craft-05-slot-4](shots/wep_craft-05-slot-4.png)](shots/wep_craft-05-slot-4.png)

**wep_craft-06-result-ready**

[![wep_craft-06-result-ready](shots/wep_craft-06-result-ready.png)](shots/wep_craft-06-result-ready.png)

**wep_craft-07-result-taken**

[![wep_craft-07-result-taken](shots/wep_craft-07-result-taken.png)](shots/wep_craft-07-result-taken.png)

**wep_craft-08-stored**

[![wep_craft-08-stored](shots/wep_craft-08-stored.png)](shots/wep_craft-08-stored.png)

**wep_craft-09-tooltip**

[![wep_craft-09-tooltip](shots/wep_craft-09-tooltip.png)](shots/wep_craft-09-tooltip.png)

**wep_craft-10-closed**

[![wep_craft-10-closed](shots/wep_craft-10-closed.png)](shots/wep_craft-10-closed.png)

## Видео с полной отладкой

* [▶ Полное видео прогона (full-debug.mp4)](videos/full-debug.mp4)

### Ролики по профессиям

### alchemy

[▶ Смотреть alchemy.mp4](videos/alchemy.mp4) · [постер](videos/alchemy.png)

[![alchemy](videos/alchemy.png)](videos/alchemy.mp4)

### armor_craft

[▶ Смотреть armor_craft.mp4](videos/armor_craft.mp4) · [постер](videos/armor_craft.png)

[![armor_craft](videos/armor_craft.png)](videos/armor_craft.mp4)

### cooking

[▶ Смотреть cooking.mp4](videos/cooking.mp4) · [постер](videos/cooking.png)

[![cooking](videos/cooking.png)](videos/cooking.mp4)

### inscribing

[▶ Смотреть inscribing.mp4](videos/inscribing.mp4) · [постер](videos/inscribing.png)

[![inscribing](videos/inscribing.png)](videos/inscribing.mp4)

### jewel_craft

[▶ Смотреть jewel_craft.mp4](videos/jewel_craft.mp4) · [постер](videos/jewel_craft.png)

[![jewel_craft](videos/jewel_craft.png)](videos/jewel_craft.mp4)

### wep_craft

[▶ Смотреть wep_craft.mp4](videos/wep_craft.mp4) · [постер](videos/wep_craft.png)

[![wep_craft](videos/wep_craft.png)](videos/wep_craft.mp4)

## Живой аудит (замеры в игре)

# Живой аудит профессий

Проверено: **6**, прошло: **6**, провалилось: **0**

Каждая профессия проверялась двумя путями: программно (те же `canCraft`/`mayPlace`/`craftResult`, что и у экрана крафта) и живым прогоном — настоящий экран 3×2, ингредиенты раскладывались мышкой по слотам, результат забирался и финализировался в инвентаре.

| профессия | итог | проверок | сложено в сетку | создано | статов | опыт | проблемы |
|---|---|---|---|---|---|---|---|
| Alchemy | PASS | 12 | 3 | mmorpg:craft_result/alchemy/1 | 3 | да | — |
| Inscribing | PASS | 12 | 3 | mmorpg:craft_result/inscribing/2 | 2 | да | — |
| Cooking | PASS | 12 | 3 | mmorpg:craft_result/cooking/4 | 3 | да | — |
| Armor Crafting | PASS | 18 | 4 | minecraft:iron_chestplate | 3 | да | — |

Полная таблица: [`reports/profession-audit.md`](reports/profession-audit.md)

## Структурный аудит (без запуска игры)

# Структурный аудит профессий (без игры)

Проверено профессий: **6**, ошибок: **0**, предупреждений: **0**.

| профессия | проверка | итог | детали |
|---|---|---|---|
| `alchemy` | `tool_registered` | OK | CraftToolItems: есть |
| `alchemy` | `ingredients_allowed` | OK | разрешено ингредиентов: 26 (обычных 22) |
| `alchemy` | `regular_ingredients` | OK | обычных ингредиентов: 22 (нужно >= 2 для рецепта аудита) |
| `alchemy` | `result_items` | OK | AlchemyConsumableItem |
| `alchemy` | `words_AlchemyDesc` | OK | Words.AlchemyDesc |
| `inscribing` | `tool_registered` | OK | CraftToolItems: есть |

Полностью: [`reports/profession-audit-structural.md`](reports/profession-audit-structural.md)

## Журналы

* [`logs/aoepr.log`](logs/aoepr.log)
* [`logs/ffmpeg.log`](logs/ffmpeg.log)
* [`logs/gradle.log`](logs/gradle.log)
* [`logs/jstack-professions.txt`](logs/jstack-professions.txt)
* [`logs/latest.log`](logs/latest.log)
* [`logs/steps.log`](logs/steps.log)

