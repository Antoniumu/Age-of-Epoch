# Рекомендуемая сборка Age of Epoch — 73 мода

**Направление:** ARPG-прогрессия Age of Epoch + исследование + переделанные данжи + живой мир +
удобная база + атмосфера и кооператив. Версия — **Minecraft 26.3 / Fabric**.

Это **одна целостная комбинация**, а не каталог взаимозаменяемых кандидатов. Лимит в 100 модов
не заполняется ради числа: **47 🟢 нейтральных и 26 🟡 требующих настройки**, 🔴 модов с явно
конкурирующей боевой системой в составе нет. Age of Epoch, Fabric API, Trinkets Updated и
библиотеки-зависимости не включены в число 73.

> **Проверка состава — 24.10.2026.** Для всех позиций найдены Fabric 26.3-файлы; для добавленных
> DnT-модулей, инструментов инвентаря, оптимизации, Naturalist и Macaw's проверены прямые ответы
> Modrinth Version API. Это список для сборки, **не протестированный совместно готовый `.mrpack`**.
> 🟡 нельзя считать настроенными только потому, что их jar запускается.

## Почему именно такая комбинация

| Слой | Что выбрано | Почему это работает вместе |
|---|---|---|
| **Главная прогрессия** | Только Age of Epoch | Уровни, таланты, заклинания, аффиксы, редкость, Благосклонность, башни и Delves не конкурируют со второй RPG-системой. |
| **Мир и маршруты** | Terralith + Tectonic, Xaero, Nature's Compass, Waystones | Красивый мир становится пространством для походов. Компас помогает с биомами, а телепорты работают только к открытым точкам и за плату. |
| **Данжи** | DnT + 10 его overhauls, Repurposed Structures, Towns and Towers | Один визуальный язык вместо смеси несовместимых серий. Большинство overhauls **заменяет ванильные структуры**, поэтому добавляет разнообразие без пропорционального роста числа точек на карте. |
| **Работа с добычей** | JEI, Adaptive Tooltips, Inventory Profiles Next, Tom's Storage, ShulkerBoxTooltip, Pick Up Notifier | Длинные аффиксы остаются читаемыми, добычу удобно сортировать и разбирать после экспедиции. Это поддерживает основной ARPG-цикл, а не добавляет новую прогрессию. |
| **Противники и жизнь мира** | Friends&Foes, Variants&Ventures, More Mobs, Naturalist | Разнообразие преимущественно обычных существ и окружения. Нет Alex's Mobs/Mutant Monsters с отдельными боссами, питомцами и экипировкой, способными переписать баланс. |
| **База между вылазками** | Farmer's Delight, Cooking for Blockheads, Tom's Storage, 8 модулей Macaw's | Есть понятный домашний цикл: привезти добычу → разобрать → приготовить еду → улучшить базу → снова отправиться в башню или данж. |
| **Атмосфера и кооператив** | Sodium/Iris, звук, анимации, Distant Horizons, Simple Voice Chat | Улучшается подача уже выбранного контента. Голосовой чат полезен в кооперативных башнях и Delves, но не меняет их правила. |
| **Производительность** | Sodium-стек, Lithium, FerriteCore, Krypton, Chunky, spark и дополнительные оптимизации | Это обязательная основа для тяжёлого worldgen, больших структур и большого числа сущностей, а не необязательное дополнение. |

### Логика игрового цикла

1. **База:** еда, сортировка добычи, проверка аффиксов, подготовка расходников.
2. **Исследование:** поиск подходящего биома и маршрута по Terralith/Tectonic.
3. **Поход:** структуры DnT и обычные противники дополняют собственные башни Age of Epoch.
4. **Возвращение:** разбор экипировки, хранение, крафт и строительство базы.
5. **Конец прогрессии:** Delves и ранговые башни остаются главным измерителем силы билда.

Так дополнения занимают промежутки между системами Age of Epoch, а не пытаются заменить их.

## База установки

- Minecraft **26.3**, Fabric Loader **0.19.5+**, Java **25+**.
- [Fabric API](https://modrinth.com/mod/fabric-api/versions?g=26.3&l=fabric).
- **Trinkets Updated** и сам `Age-of-Epoch-*.jar`.
- Обязательные библиотеки выбранных версий — через **Required content** Modrinth или автоматически
  в Modrinth App. Не подменяйте их версиями для другого Minecraft.
- Для каждой ссылки ниже выбирается файл **26.3 / Fabric**, предпочтительно Release.

---

## 1. Производительность и стабильность — 14

| № | Мод | Роль в этой комбинации |
|---:|---|---|
| 1 | 🟢 [Sodium](https://modrinth.com/mod/sodium/versions?g=26.3&l=fabric) | Основа клиентского рендера; нужна для больших структур, Iris и Distant Horizons. |
| 2 | 🟢 [Lithium](https://modrinth.com/mod/lithium/versions?g=26.3&l=fabric) | Оптимизация игровой логики без изменения RPG-статов. |
| 3 | 🟢 [FerriteCore](https://modrinth.com/mod/ferrite-core/versions?g=26.3&l=fabric) | Снижает потребление памяти полной сборкой. |
| 4 | 🟢 [EntityCulling](https://modrinth.com/mod/entityculling/versions?g=26.3&l=fabric) | Не рисует скрытые сущности в плотных данжах. |
| 5 | 🟢 [ImmediatelyFast](https://modrinth.com/mod/immediatelyfast/versions?g=26.3&l=fabric) | Ускоряет текст, интерфейс и сущности; важен для длинных RPG-подсказок. |
| 6 | 🟢 [Krypton](https://modrinth.com/mod/krypton/versions?g=26.3&l=fabric) | Оптимизирует сетевой стек кооператива и сервера. |
| 7 | 🟢 [Chunky](https://modrinth.com/mod/chunky/versions?g=26.3&l=fabric) | Предгенерация после окончательного выбора worldgen; уменьшает фризы в походах. |
| 8 | 🟢 [spark](https://modrinth.com/mod/spark/versions?g=26.3&l=fabric) | Профилирование TPS/FPS, генерации структур и нагрузки от существ. |
| 9 | 🟢 [Sodium Extra](https://modrinth.com/mod/sodium-extra/versions?g=26.3&l=fabric) | Дополнительные графические настройки и снижение перегрузки от частиц. |
| 10 | 🟢 [Structure Layout Optimizer](https://modrinth.com/mod/structure-layout-optimizer/versions?g=26.3&l=fabric) | Оптимизация Jigsaw/NBT-структур — основного типа контента этой сборки. |
| 11 | 🟢 [Dynamic FPS](https://modrinth.com/mod/dynamic-fps/versions?g=26.3&l=fabric) | Снижает нагрузку неактивного окна. |
| 12 | 🟢 [BadOptimizations](https://modrinth.com/mod/badoptimizations/versions?g=26.3&l=fabric) | Дополнительные клиентские оптимизации поверх Sodium-стека. |
| 13 | 🟢 [Alternate Current](https://modrinth.com/mod/alternate-current/versions?g=26.3&l=fabric) | Снижает стоимость редстоуна на сервере при сложных базах и механизмах. |
| 14 | 🟢 [Debugify](https://modrinth.com/mod/debugify/versions?g=26.3&l=fabric) | Исправляет отдельные ошибки игры; включённые исправления проверяются при обновлениях. |

## 2. Интерфейс, инвентарь и работа с добычей — 17

| № | Мод | Роль в этой комбинации |
|---:|---|---|
| 15 | 🟢 [Mod Menu](https://modrinth.com/mod/modmenu/versions?g=26.3&l=fabric) | Единая точка настройки клиентских модов. |
| 16 | 🟢 [JEI](https://modrinth.com/mod/jei/versions?g=26.3&l=fabric) | Рецепты Age of Epoch, Farmer's Delight, Tom's Storage и Macaw's; единственный recipe viewer. |
| 17 | 🟢 [Jade](https://modrinth.com/mod/jade/versions?g=26.3&l=fabric) | Информация о блоках и контейнерах; дублирующие плашки существ отключить. |
| 18 | 🟡 [Xaero's Minimap](https://modrinth.com/mod/xaeros-minimap/versions?g=26.3&l=fabric) | Миникарта и точки маршрута. Отключить радар существ, пещерный режим и cheat/teleport-функции. |
| 19 | 🟢 [Xaero's World Map](https://modrinth.com/mod/xaeros-world-map/versions?g=26.3&l=fabric) | Карта уже исследованного мира; дополняет миникарту. |
| 20 | 🟢 [AppleSkin](https://modrinth.com/mod/appleskin/versions?g=26.3&l=fabric) | Показывает питательность еды Farmer's Delight. |
| 21 | 🟢 [Controlling](https://modrinth.com/mod/controlling/versions?g=26.3&l=fabric) | Поиск клавиш и устранение конфликтов с H/J/K Age of Epoch. |
| 22 | 🟢 [Advancements Reloaded](https://modrinth.com/mod/advancements-reloaded/versions?g=26.3&l=fabric) | Удобный экран достижений модов; не заменяет встроенный квестбук. |
| 23 | 🟢 [Overflowing Bars](https://modrinth.com/mod/overflowing-bars/versions?g=26.3&l=fabric) | Компактное отображение больших значений здоровья/брони; проверить компоновку с HUD Age of Epoch. |
| 24 | 🟢 [Pick Up Notifier](https://modrinth.com/mod/pick-up-notifier/versions?g=26.3&l=fabric) | Быстрые уведомления о потоке аффиксной добычи. |
| 25 | 🟢 [ShulkerBoxTooltip](https://modrinth.com/mod/shulkerboxtooltip/versions?g=26.3&l=fabric) | Предпросмотр шалкеров при разборе экспедиционного лута. |
| 26 | 🟢 [Crafting Tweaks](https://modrinth.com/mod/crafting-tweaks/versions?g=26.3&l=fabric) | Очистка, вращение и балансировка сетки крафта. |
| 27 | 🟢 [Adaptive Tooltips](https://modrinth.com/mod/adaptive-tooltips/versions?g=26.3&l=fabric) | Перенос/прокрутка и масштабирование длинных подсказок; особенно важно для аффиксов и требований экипировки. |
| 28 | 🟢 [BetterF3](https://modrinth.com/mod/betterf3/versions?g=26.3&l=fabric) | Читаемые координаты, biome/entity ID и диагностика; полезно при настройке datapack-правил. |
| 29 | 🟡 [Tom's Simple Storage Mod](https://modrinth.com/mod/toms-storage/versions?g=26.3&l=fabric) | Общий терминал для добычи на базе. Не использовать беспроводной доступ для сброса лута посреди данжа и обхода инвентарного давления. |
| 30 | 🟢 [Inventory Profiles Next](https://modrinth.com/mod/inventory-profiles-next/versions?g=26.3&l=fabric) | Сортировка инвентаря и контейнеров Tom's Storage. Заменяет Mouse Tweaks, чтобы не держать два пересекающихся инвентарных мода. |
| 31 | 🟡 [Target Dummy](https://modrinth.com/mod/target-dummy/versions?g=26.3&l=fabric) | Проверка урона билда на базе. Манекен не должен давать опыт, RPG-дроп или Благосклонность; это проверяется отдельно. |

## 3. Мир и путешествия — 4

| № | Мод | Роль в этой комбинации |
|---:|---|---|
| 32 | 🟡 [Terralith](https://modrinth.com/mod/terralith/versions?g=26.3&l=fabric) | Биомы без собственной экипировки. Добавить их в datapack-списки башен/Delves или осознанно настроить изменённую встречаемость. |
| 33 | 🟡 [Tectonic](https://modrinth.com/mod/tectonic/versions?g=26.3&l=fabric) | Рельеф для длинных маршрутов. Использовать совместимые версии Terralith/Tectonic и проверить доступность входов в структуры. |
| 34 | 🟡 [Waystones](https://modrinth.com/mod/waystones/versions?g=26.3&l=fabric) | Возвращение к уже открытым базам и точкам. Плата обязательна; нельзя обходить правила боссовых комнат и Delves. |
| 35 | 🟡 [Nature's Compass](https://modrinth.com/mod/natures-compass/versions?g=26.3&l=fabric) | Поиск биомов Terralith для постройки и datapack-условий. Рецепт средней игры; не заменяет исследование структур. |

## 4. Данжи и поселения — 13

DnT-overhauls ниже — самостоятельные модули; основной Dungeons and Taverns **не включает их
функции**, поэтому это не дубли одних и тех же файлов. Вся серия 🟡: сундуки влияют на
Благосклонность и RPG-добычу, а спавнеры/особые противники — на боевую нагрузку.

| № | Мод | Роль в этой комбинации |
|---:|---|---|
| 36 | 🟡 [Dungeons and Taverns](https://modrinth.com/mod/dungeons-and-taverns/versions?g=26.3&l=fabric) | Основной exploration-контент и карты к структурам. Уникальные чары, мини-боссы и особые награды свежей версии проверить/убрать до отдельной интеграции. |
| 37 | 🟡 [Repurposed Structures](https://modrinth.com/mod/repurposed-structures-fabric/versions?g=26.3&l=fabric) | Биомные варианты для Terralith. Снизить плотность и проверить двойную RPG-добычу/Благосклонность от контейнеров. |
| 38 | 🟡 [Towns and Towers](https://modrinth.com/mod/towns-and-towers/versions?g=26.3&l=fabric) | Поселения и точки отдыха между данжами. Торговля и зачарованные товары не должны обгонять RPG-экипировку. |
| 39 | 🟡 [DnT: Ancient City Overhaul](https://modrinth.com/mod/dungeons-and-taverns-ancient-city-overhaul/versions?g=26.3&l=fabric) | Поздняя экспедиция с археологией. Проверить Warden, одноразовость наград и число лутовых контейнеров. |
| 40 | 🟡 [DnT: Stronghold Overhaul](https://modrinth.com/mod/dungeons-and-taverns-stronghold-overhaul/versions?g=26.3&l=fabric) | Большой финальный данж перед Эндом. Предгенерировать с запасом времени: автор предупреждает о паузе первой генерации. |
| 41 | 🟡 [DnT: Pillager Outpost Overhaul](https://modrinth.com/mod/dungeons-and-taverns-pillager-outpost-overhaul/versions?g=26.3&l=fabric) | Ванильная точка набега становится полноценной вылазкой; проверить уровни illager-существ и награды. |
| 42 | 🟡 [DnT: Swamp Hut Overhaul](https://modrinth.com/mod/dungeons-and-taverns-swamp-hut-overhaul/versions?g=26.3&l=fabric) | Небольшая атмосферная цель в болотах; единственный сундук не должен давать лишнюю цепочку наград. |
| 43 | 🟡 [DnT: Woodland Mansion Overhaul](https://modrinth.com/mod/dungeons-and-taverns-woodland-mansion-overhaul/versions?g=26.3&l=fabric) | Большой рейд с подвалом и вариативными комнатами; сверить сложность и лут с башней соответствующего ранга. |
| 44 | 🟡 [DnT: Desert Temple Overhaul](https://modrinth.com/mod/dungeons-and-taverns-desert-temple-overhaul/versions?g=26.3&l=fabric) | Ловушки, скрытые комнаты и археология; сочетается с Better Archeology, но требует пересмотра наград. |
| 45 | 🟡 [DnT: Jungle Temple Overhaul](https://modrinth.com/mod/dungeons-and-taverns-jungle-temple-overhaul/versions?g=26.3&l=fabric) | Головоломки и варианты храма вместо короткого ванильного осмотра; проверить лут и ловушки на разных уровнях. |
| 46 | 🟡 [DnT: Nether Fortress Overhaul](https://modrinth.com/mod/dungeons-and-taverns-nether-fortress-overhaul/versions?g=26.3&l=fabric) | Полноценная цель в Незере без BetterNether. Учесть гарантированный/дополнительные спавнеры и штатный уровень измерения. |
| 47 | 🟡 [DnT: Ocean Monument Overhaul](https://modrinth.com/mod/dungeons-and-taverns-ocean-monument-overhaul/versions?g=26.3&l=fabric) | Вариативный подводный данж; проверить Guardians, дыхание/подводную подготовку и награды. |
| 48 | 🟡 [DnT: Mineshaft Overhaul](https://modrinth.com/mod/dungeons-and-taverns-mineshaft-overhaul/versions?g=26.3&l=fabric) | Заменяет ванильные и меза-заброшенные шахты, добавляя редкие варианты; контролировать рельсы/ресурсы и сундуки в час. |

## 5. Существа, археология и еда — 7

| № | Мод | Роль в этой комбинации |
|---:|---|---|
| 49 | 🟡 [Better Archeology](https://modrinth.com/mod/better-archeology/versions?g=26.3&l=fabric) | Небоевой exploration-цикл, объединённый с DnT-храмами и Ancient City. Награды должны оставаться ресурсами/историей, а не сильной экипировкой. |
| 50 | 🟢 [Cooking for Blockheads](https://modrinth.com/mod/cooking-for-blockheads/versions?g=26.3&l=fabric) | Удобная кухня и доступ к запасам еды на базе. |
| 51 | 🟡 [Farmer's Delight Refabricated](https://modrinth.com/mod/farmers-delight-refabricated/versions?g=26.3&l=fabric) | Приготовление еды перед вылазкой. Nourishment/Comfort и другие эффекты не должны заменять регенерацию и сопротивления Age of Epoch. |
| 52 | 🟡 [Friends&Foes](https://modrinth.com/mod/friends-and-foes/versions?g=26.3&l=fabric) | Vanilla-like разнообразие противников. Нужны `specific_mobs` и проверка HP/урона/способностей Wildfire, Illusioner и остальных. |
| 53 | 🟡 [Variants&Ventures](https://modrinth.com/mod/variants-and-ventures/versions?g=26.3&l=fabric) | Новые варианты знакомых врагов без отдельной RPG-элитности; проверить каждую атаку, опыт и дроп. |
| 54 | 🟡 [More Mobs](https://modrinth.com/mod/more-mobs/versions?g=26.3&l=fabric) | Разнообразие ванильных существ. Не считать автоматически косметикой: проверить entity ID, AI, уровни и лут каждого варианта. |
| 55 | 🟡 [Naturalist](https://modrinth.com/mod/naturalist/versions?g=26.3&l=fabric) | Животные и поведение окружения делают мир живым без добавления ещё одной линейки боссов. Настроить spawn-rate, дроп и EntityConfig. |

## 6. Строительство базы — 8

Выбрана одна серия Macaw's, а не несколько конкурирующих декоративных наборов: единый стиль,
общие зависимости, никакой боевой экипировки или второй экономики.

| № | Мод | Роль в этой комбинации |
|---:|---|---|
| 56 | 🟢 [Macaw's Furniture](https://modrinth.com/mod/macaws-furniture/versions?g=26.3&l=fabric) | Мебель для домашнего хаба, кухни и комнаты хранения. |
| 57 | 🟢 [Macaw's Doors](https://modrinth.com/mod/macaws-doors/versions?g=26.3&l=fabric) | Двери и ворота для баз и небольших поселений. |
| 58 | 🟢 [Macaw's Windows](https://modrinth.com/mod/macaws-windows/versions?g=26.3&l=fabric) | Окна и завершение фасадов в едином стиле серии. |
| 59 | 🟢 [Macaw's Roofs](https://modrinth.com/mod/macaws-roofs/versions?g=26.3&l=fabric) | Нормальные скатные крыши для средневековых и фэнтезийных построек. |
| 60 | 🟢 [Macaw's Lights and Lamps](https://modrinth.com/mod/macaws-lights-and-lamps/versions?g=26.3&l=fabric) | Освещение базы и дорог; использует обычные правила света, без боевых бонусов. |
| 61 | 🟢 [Macaw's Fences and Walls](https://modrinth.com/mod/macaws-fences-and-walls/versions?g=26.3&l=fabric) | Ограды и периметр базы. |
| 62 | 🟢 [Macaw's Paths and Pavings](https://modrinth.com/mod/macaws-paths-and-pavings/versions?g=26.3&l=fabric) | Дороги между домом, фермой, хранилищем и Waystone. |
| 63 | 🟢 [Macaw's Bridges](https://modrinth.com/mod/macaws-bridges/versions?g=26.3&l=fabric) | Мосты через реки и ущелья Terralith/Tectonic; хорошо завершает инфраструктуру базы. |

## 7. Графика, звук и кооператив — 10

| № | Мод | Роль в этой комбинации |
|---:|---|---|
| 64 | 🟢 [LambDynamicLights](https://modrinth.com/mod/lambdynamiclights/versions?g=26.3&l=fabric) | Свет факела в руке при исследовании пещер и шахт. |
| 65 | 🟢 [Iris Shaders](https://modrinth.com/mod/iris/versions?g=26.3&l=fabric) | Шейдеры по желанию; начинать со стабильной совместимой пары Sodium/Iris, не с новейшей Beta. |
| 66 | 🟢 [Raised](https://modrinth.com/project/nCQRBEiR/versions?g=26.3&l=fabric) | Исправляет вертикальное положение хотбара; проверить вместе с HUD Age of Epoch. |
| 67 | 🟢 [Distant Horizons](https://modrinth.com/mod/distanthorizons/versions?g=26.3&l=fabric) | Дальние ориентиры и виды Terralith. Первый кандидат на отключение при нехватке FPS/памяти; мир от этого не ломается. |
| 68 | 🟢 [Sound Physics Remastered](https://modrinth.com/mod/sound-physics-remastered/versions?g=26.3&l=fabric) | Эхо и затухание звука в больших DnT-залах и пещерах. |
| 69 | 🟢 [AmbientSounds](https://modrinth.com/mod/ambientsounds/versions?g=26.3&l=fabric) | Фоновая звуковая картина биомов и окружения. |
| 70 | 🟢 [Presence Footsteps](https://modrinth.com/mod/presence-footsteps/versions?g=26.3&l=fabric) | Шаги по разным поверхностям для походов и построек. |
| 71 | 🟢 [Not Enough Animations](https://modrinth.com/mod/not-enough-animations/versions?g=26.3&l=fabric) | Дополнительные анимации третьего лица без Better Combat. |
| 72 | 🟢 [Better Animations Collection](https://modrinth.com/mod/better-animations-collection/versions?g=26.3&l=fabric) | Более живые движения ванильных существ без изменения их характеристик. |
| 73 | 🟢 [Simple Voice Chat](https://modrinth.com/mod/simple-voice-chat/versions?g=26.3&l=fabric) | Голосовая координация в кооперативных башнях, DnT-рейдах и Delves; нужен отдельный UDP-порт на сервере. |

---

## Обязательная настройка перед постоянным миром

### 1. Сначала зафиксировать правила Age of Epoch

Собственные уровни, дроп, Благосклонность, смерть, башни и Delves являются базовой линией.
Нельзя глобально урезать урон или награды Age of Epoch, чтобы «втянуть» неудачно настроенный
сторонний контент. Проблемный модуль отключается или исправляется локально.

### 2. DnT — самый важный balance-pass

Свежий DnT содержит не только архитектуру: у него есть особые чары, мини-боссы, торговля картами
и собственные награды. Поэтому перед постоянным миром нужно:

- проверить каждую уникальную чару на стак с аффиксами, заклинаниями и перемещением Age of Epoch;
- не давать особым мини-боссам одновременно собственные множители и непропорциональную RPG-элитность;
- проверить торговлю картами: она заменяет Explorer's Compass, но не должна открывать весь late-game сразу;
- сравнить сундуки, Благосклонность и экипировку в час с маршрутом без DnT;
- оставить только один набор overhauls — не добавлять YUNG's поверх тех же структур.

Если конкретную механику нельзя локально ограничить datapack-правилами, соответствующий DnT-модуль
исключается. Наличие Fabric 26.3-файла не решает эту задачу автоматически.

### 3. Сторонние сущности

Для Friends&Foes, Variants&Ventures, More Mobs, Naturalist и при необходимости Target Dummy
используются отдельные записи `data/mmorpg/entity_config/specific_mobs/`: `hp_multi`, `dmg_multi`,
`stat_multi`, `exp_multi`, `loot_multi`, `min_lvl`, `max_lvl`.

Манекен не должен быть источником опыта/дропа. Пассивные животные Naturalist не должны забивать
spawn-cap боевых существ или создавать непропорциональную ресурсную ферму.

### 4. Плотность и награды

- Не добавлять Structory: после DnT 6 и его overhauls дополнительный поток мелких находок не нужен.
- Repurposed Structures использовать с меньшей плотностью; overhauls в основном заменяют ванильные
  структуры, а не дают право одновременно увеличивать все остальные источники лута.
- Не обнулять глобально `favor_per_chest`: это затронет и собственные награды Age of Epoch.
- Проверить одинаковый 30-минутный маршрут до/после контентных модов: сундуки, Благосклонность,
  экипировка, руны, самоцветы, валюта, смерти и время убийств.

### 5. Темп и удобства

- Waystones — только открытые точки и плата; Nature's Compass — средняя игра; радар миникарты выключен.
- Tom's Storage остаётся базовой инфраструктурой, а не беспроводным безразмерным рюкзаком в данже.
- Explorer's Compass не нужен: у DnT уже есть собственная логика карт и исследования.
- GameMaster остаётся включённым, но его показания оцениваются после окончательного выбора контента.
  Он не исправляет сторонний лут, чары, предметы или профессии.
- Смерть остаётся штатной; могилы и рюкзаки в эту комбинацию не входят.

## Что осознанно не вошло и почему

| Что исключено | Причина именно в этой комбинации |
|---|---|
| Better Combat, Alex's Mobs, Mutant Monsters, Additional Additions | Собственные атаки, питомцы, боссы, экипировка или особые предметы требуют полноценной RPG-интеграции, а не ещё одного глобального множителя. |
| BetterEnd, BetterNether, Stellarity и MES/MNS | Отдельная прогрессия измерений и предметов. DnT Nether Fortress и ванильные уровни измерений Age of Epoch уже закрывают выбранную роль без этого конфликта. |
| Lootr, Universal Graves, Traveler's Backpack, Carry On, Tree Harvester | Меняют персональную добычу, смерть, инвентарное давление, правила спавнеров или профессиональную экономику. |
| Explorer's Compass, Structory, Mouse Tweaks | Их функции уже лучше покрыты DnT-картами, плотным набором структур и Inventory Profiles Next соответственно. |
| YUNG's Better-структуры | Для проверенных Mineshafts, Desert/Jungle Temples, Ocean Monuments и Nether Fortresses не найдено Fabric 26.3-файлов; вместо них выбрана единая DnT-серия. |
| Вторые классы, уровни, заклинания, аффиксы, редкость мобов, Curios, Neat | Дублируют собственные системы Age of Epoch или используемый Trinkets Updated. |

## Порядок сборки и приёмки

1. **Ядро:** Age of Epoch + обязательные зависимости + №1–14. Проверить запуск, H/J/K, башню и Delve.
2. **Интерфейс:** №15–31. Проверить длинные аффиксы, сортировку, хранение и отсутствие дюпа.
3. **Новый мир:** до создания мира поставить №32–55, затем провести datapack/loot/entity balance-pass.
4. **База и клиент:** добавить №56–73. Distant Horizons и шейдеры включать после стабильного запуска.
5. **Генерация:** только после окончательного worldgen-состава сделать резервную копию и запустить Chunky.
6. **Бой:** уровни 1/25/50, одинаковая экипировка, ванильные и новые существа; отдельно DnT-мини-боссы.
7. **Контент:** по одной башне, Delve, DnT Stronghold, Mansion, Nether Fortress и Ocean Monument.
8. **Кооператив:** spark под нагрузкой, Simple Voice Chat UDP, отсутствие двойного лута и неверных уровней.

Если нужен быстрый облегчённый вариант, сначала убираются **шейдеры и Distant Horizons**, а не
системные моды Age of Epoch и не оптимизация сервера.

## Итог

Эти 73 мода выбраны не потому, что каждый «популярный», а потому что закрывают разные части одного
цикла: **подготовка → исследование → данж → добыча → база → более сложный контент Age of Epoch**.
Главная последовательность сборки — единая DnT-серия вместо случайного набора данжей, vanilla-like
существа вместо второй линейки боссов, инструменты разбора RPG-добычи и отсутствие модов, которые
переписывают урон, смерть или экипировку поверх Age of Epoch.
