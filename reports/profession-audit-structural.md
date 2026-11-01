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
| `inscribing` | `ingredients_allowed` | OK | разрешено ингредиентов: 13 (обычных 9) |
| `inscribing` | `regular_ingredients` | OK | обычных ингредиентов: 9 (нужно >= 2 для рецепта аудита) |
| `inscribing` | `result_items` | OK | InscribingConsumableItem |
| `inscribing` | `words_InscribingDesc` | OK | Words.InscribingDesc |
| `cooking` | `tool_registered` | OK | CraftToolItems: есть |
| `cooking` | `ingredients_allowed` | OK | разрешено ингредиентов: 18 (обычных 14) |
| `cooking` | `regular_ingredients` | OK | обычных ингредиентов: 14 (нужно >= 2 для рецепта аудита) |
| `cooking` | `result_items` | OK | CookingConsumableItem |
| `cooking` | `words_CookingDesc` | OK | Words.CookingDesc |
| `jewel_craft` | `tool_registered` | OK | CraftToolItems: есть |
| `jewel_craft` | `ingredients_allowed` | OK | разрешено ингредиентов: 15 (обычных 11) |
| `jewel_craft` | `regular_ingredients` | OK | обычных ингредиентов: 11 (нужно >= 2 для рецепта аудита) |
| `jewel_craft` | `gear_mapping` | OK | gearMatchesProfession в PlayerSkillEnum |
| `jewel_craft` | `words_JewelCraftingDesc` | OK | Words.JewelCraftingDesc |
| `armor_craft` | `tool_registered` | OK | CraftToolItems: есть |
| `armor_craft` | `ingredients_allowed` | OK | разрешено ингредиентов: 12 (обычных 8) |
| `armor_craft` | `regular_ingredients` | OK | обычных ингредиентов: 8 (нужно >= 2 для рецепта аудита) |
| `armor_craft` | `gear_mapping` | OK | gearMatchesProfession в PlayerSkillEnum |
| `armor_craft` | `words_ArmorCraftingDesc` | OK | Words.ArmorCraftingDesc |
| `wep_craft` | `tool_registered` | OK | CraftToolItems: есть |
| `wep_craft` | `ingredients_allowed` | OK | разрешено ингредиентов: 12 (обычных 9) |
| `wep_craft` | `regular_ingredients` | OK | обычных ингредиентов: 9 (нужно >= 2 для рецепта аудита) |
| `wep_craft` | `gear_mapping` | OK | gearMatchesProfession в PlayerSkillEnum |
| `wep_craft` | `words_WeaponCraftingDesc` | OK | Words.WeaponCraftingDesc |

