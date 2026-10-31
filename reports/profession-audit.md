# Живой аудит профессий

Проверено: **6**, прошло: **6**, провалилось: **0**

Каждая профессия проверялась двумя путями: программно (те же `canCraft`/`mayPlace`/`craftResult`, что и у экрана крафта) и живым прогоном — настоящий экран 3×2, ингредиенты раскладывались мышкой по слотам, результат забирался и финализировался в инвентаре.

| профессия | итог | проверок | сложено в сетку | создано | статов | опыт | проблемы |
|---|---|---|---|---|---|---|---|
| Alchemy | PASS | 12 | 3 | mmorpg:craft_result/alchemy/1 | 3 | да | — |
| Inscribing | PASS | 12 | 3 | mmorpg:craft_result/inscribing/2 | 2 | да | — |
| Cooking | PASS | 12 | 3 | mmorpg:craft_result/cooking/4 | 3 | да | — |
| Armor Crafting | PASS | 18 | 4 | minecraft:iron_chestplate | 3 | да | — |
| Weapon Crafting | PASS | 18 | 4 | minecraft:iron_sword | 3 | да | — |
| Jewel Crafting | PASS | 18 | 4 | mmorpg:jewelry/ring/iron | 3 | да | — |

## Проверки по профессиям

### Alchemy — PASS

| проверка | итог | детали |
|---|---|---|
| `tool_registered` | OK | mmorpg:craft_tools/alchemy |
| `ingredients_available` | OK | доступно ингредиентов: 26 |
| `has_regular_ingredients` | OK | soul_matrix=true, обычных=22 |
| `grid_empty_fails` | OK | No Ingredients. |
| `junk_rejected` | OK | палка в слот не должна проходить |
| `ingredient_accepted` | OK | red_coral |
| `wrong_prof_rejected` | OK | fire_stone |
| `full_recipe_works` | OK |  |
| `result_process_saved` | OK | ингредиентов в процессе: 3 |
| `success_chance_100` | OK | шанс успеха: 100% |
| `stat_multi_sane` | OK | множитель: 100% |
| `crafted_output_valid` | OK | статов в расходуемом: 3 |

### Inscribing — PASS

| проверка | итог | детали |
|---|---|---|
| `tool_registered` | OK | mmorpg:craft_tools/inscribing |
| `ingredients_available` | OK | доступно ингредиентов: 13 |
| `has_regular_ingredients` | OK | soul_matrix=true, обычных=9 |
| `grid_empty_fails` | OK | No Ingredients. |
| `junk_rejected` | OK | палка в слот не должна проходить |
| `ingredient_accepted` | OK | redstone_crystal |
| `wrong_prof_rejected` | OK | red_coral |
| `full_recipe_works` | OK |  |
| `result_process_saved` | OK | ингредиентов в процессе: 3 |
| `success_chance_100` | OK | шанс успеха: 100% |
| `stat_multi_sane` | OK | множитель: 100% |
| `crafted_output_valid` | OK | статов в расходуемом: 2 |

### Cooking — PASS

| проверка | итог | детали |
|---|---|---|
| `tool_registered` | OK | mmorpg:craft_tools/cooking |
| `ingredients_available` | OK | доступно ингредиентов: 18 |
| `has_regular_ingredients` | OK | soul_matrix=true, обычных=14 |
| `grid_empty_fails` | OK | No Ingredients. |
| `junk_rejected` | OK | палка в слот не должна проходить |
| `ingredient_accepted` | OK | red_coral |
| `wrong_prof_rejected` | OK | enigma_stone |
| `full_recipe_works` | OK |  |
| `result_process_saved` | OK | ингредиентов в процессе: 3 |
| `success_chance_100` | OK | шанс успеха: 100% |
| `stat_multi_sane` | OK | множитель: 100% |
| `crafted_output_valid` | OK | статов в расходуемом: 3 |

### Armor Crafting — PASS

| проверка | итог | детали |
|---|---|---|
| `tool_registered` | OK | mmorpg:craft_tools/armor_craft |
| `ingredients_available` | OK | доступно ингредиентов: 12 |
| `has_regular_ingredients` | OK | soul_matrix=true, обычных=8 |
| `gear_base_found` | OK | minecraft:iron_chestplate |
| `gear_slot_matches` | OK | chest |
| `grid_empty_fails` | OK | This recipe requires gear as ingredient |
| `junk_rejected` | OK | палка в слот не должна проходить |
| `ingredient_accepted` | OK | fire_stone |
| `gear_accepted` | OK | minecraft:iron_chestplate |
| `soulless_gear_accepted` | OK | бесдушная основа проходит |
| `souled_gear_rejected` | OK | The used gear has stats already |
| `crafted_stats_rejected` | OK | The used gear has stats already |
| `wrong_prof_rejected` | OK | red_coral |
| `full_recipe_works` | OK |  |
| `result_process_saved` | OK | ингредиентов в процессе: 3 |
| `success_chance_100` | OK | шанс успеха: 100% |
| `stat_multi_sane` | OK | множитель: 100% |
| `crafted_output_valid` | OK | бесдушная основа со статами: 3 |

### Weapon Crafting — PASS

| проверка | итог | детали |
|---|---|---|
| `tool_registered` | OK | mmorpg:craft_tools/wep_craft |
| `ingredients_available` | OK | доступно ингредиентов: 12 |
| `has_regular_ingredients` | OK | soul_matrix=true, обычных=9 |
| `gear_base_found` | OK | minecraft:iron_sword |
| `gear_slot_matches` | OK | sword |
| `grid_empty_fails` | OK | This recipe requires gear as ingredient |
| `junk_rejected` | OK | палка в слот не должна проходить |
| `ingredient_accepted` | OK | enigma_stone |
| `gear_accepted` | OK | minecraft:iron_sword |
| `soulless_gear_accepted` | OK | бесдушная основа проходит |
| `souled_gear_rejected` | OK | The used gear has stats already |
| `crafted_stats_rejected` | OK | The used gear has stats already |
| `wrong_prof_rejected` | OK | red_coral |
| `full_recipe_works` | OK |  |
| `result_process_saved` | OK | ингредиентов в процессе: 3 |
| `success_chance_100` | OK | шанс успеха: 100% |
| `stat_multi_sane` | OK | множитель: 100% |
| `crafted_output_valid` | OK | бесдушная основа со статами: 3 |

### Jewel Crafting — PASS

| проверка | итог | детали |
|---|---|---|
| `tool_registered` | OK | mmorpg:craft_tools/jewel_craft |
| `ingredients_available` | OK | доступно ингредиентов: 15 |
| `has_regular_ingredients` | OK | soul_matrix=true, обычных=11 |
| `gear_base_found` | OK | mmorpg:jewelry/ring/iron |
| `gear_slot_matches` | OK | ring |
| `grid_empty_fails` | OK | This recipe requires gear as ingredient |
| `junk_rejected` | OK | палка в слот не должна проходить |
| `ingredient_accepted` | OK | enigma_stone |
| `gear_accepted` | OK | mmorpg:jewelry/ring/iron |
| `soulless_gear_accepted` | OK | бесдушная основа проходит |
| `souled_gear_rejected` | OK | The used gear has stats already |
| `crafted_stats_rejected` | OK | The used gear has stats already |
| `wrong_prof_rejected` | OK | red_coral |
| `full_recipe_works` | OK |  |
| `result_process_saved` | OK | ингредиентов в процессе: 3 |
| `success_chance_100` | OK | шанс успеха: 100% |
| `stat_multi_sane` | OK | множитель: 100% |
| `crafted_output_valid` | OK | бесдушная основа со статами: 3 |

