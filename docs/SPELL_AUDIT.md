# Аудит умений: живой прогон, отчёт и ролики

Снимок отчёта CI-прогона (workflow `spell-audit.yml`, ветка результатов
`ci-spell-audit`). Отчёт и ролики пересобираются на каждый прогон, поэтому
источник истины — ветка, а этот файл — читаемая копия.

## Ролики по классам

| класс | ролик | умений |
|---|---|---|
| Divine — свет, щиты и ближний бой | [divine.mp4](https://github.com/Antonimu/Age-of-Epoch/blob/ci-spell-audit/videos/divine.mp4) | 16 |
| Hunting — луки, ловушки и мобильность | [hunting.mp4](https://github.com/Antonimu/Age-of-Epoch/blob/ci-spell-audit/videos/hunting.mp4) | 20 |
| Nature — яды, тотемы и поддержка | [nature.mp4](https://github.com/Antonimu/Age-of-Epoch/blob/ci-spell-audit/videos/nature.mp4) | 14 |
| Fire — огонь и урон по площади | [fire.mp4](https://github.com/Antonimu/Age-of-Epoch/blob/ci-spell-audit/videos/fire.mp4) | 8 |
| Water — лёд, контроль и лечение | [water.mp4](https://github.com/Antonimu/Age-of-Epoch/blob/ci-spell-audit/videos/water.mp4) | 14 |

Прямые ссылки на файлы (скачивание без страницы GitHub):

- https://raw.githubusercontent.com/Antonimu/Age-of-Epoch/ci-spell-audit/videos/divine.mp4
- https://raw.githubusercontent.com/Antonimu/Age-of-Epoch/ci-spell-audit/videos/hunting.mp4
- https://raw.githubusercontent.com/Antonimu/Age-of-Epoch/ci-spell-audit/videos/nature.mp4
- https://raw.githubusercontent.com/Antonimu/Age-of-Epoch/ci-spell-audit/videos/fire.mp4
- https://raw.githubusercontent.com/Antonimu/Age-of-Epoch/ci-spell-audit/videos/water.mp4

Постеры кадров: `videos/<класс>.png` в той же ветке. Индекс с таблицами по
классам — `videos/README.md`.

## Как проверяется

1. `tools/verify/check_spells.py` — структурная проверка: у умения есть
   полезный эффект, партиклы, звук, описание, иконка и место в дереве школы.
2. `SpellAuditProbe` в dev-клиенте под Xvfb: строит арену, ставит манекены и
   применяет каждое умение тем же путём, что и хотбар (`Spell.cast`), замеряя
   урон из событий урона самого умения, лечение и ману из сработавших действий,
   щит, эффекты, перемещение, выданные предметы и спавн.
3. Весь прогон пишется через `ffmpeg x11grab` и режется на ролики по маркерам,
   которые пишет сам зонд; подпись в кадре рисует клиент (название, описание из
   тултипа, живые цифры, вердикт).

## Отчёт этого прогона

Проверено: **72**, прошло: **72**, провалилось: **0**

Колонка «чем подтверждено» — только измеренные факты: урон из событий урона самого умения, лечение и мана из сработавших действий, щит из ресурсов игрока, эффекты и перемещение — по изменению состояния. Умения, у которых снаружи нечего измерить (агро, сброс кулдаунов, меню), помечены явно.

Каждое заклинание применялось тем же путём, что и с хотбара (`setToCast` → тик каста → `Spell.cast`), по манекенам впереди игрока. Если урон не прошёл, делался второй заход с кольцом манекенов вокруг игрока.

| школа | заклинание | итог | урон | попаданий | лечение | мана | чем подтверждено | партиклы | проблемы |
|---|---|---|---|---|---|---|---|---|---|
| divine | `healing_aura` | PASS | 0.0 | 0 | 8.0 | 10.8 | лечение +8.0 | да | — |
| divine | `gong_strike` | PASS | 9.7 | 1 | 0.0 | 10.8 | урон 9.7 (1 поп.) · эффекты: negative/11 | да | — |
| divine | `shout_warn` | PASS | 0.0 | 0 | 0.0 | 10.8 | щит +5.0 | да | — |
| divine | `charge` | PASS | 23.3 | 8 | 0.0 | 10.8 | урон 23.3 (8 поп.) · щит +5.0 · перемещение 0.4 бл. | да | — |
| divine | `whirlwind` | PASS | 17.5 | 9 | 0.0 | 0.0 | урон 17.5 (9 поп.) · эффекты: knockback_resist | да | — |
| divine | `shooting_star` | PASS | 0.0 | 0 | 4.9 | 21.6 | лечение +4.9 | да | — |
| divine | `undying_will` | PASS | 0.0 | 0 | 0.0 | 10.8 | эффекты: beneficial/24 | да | — |
| divine | `pull` | PASS | 9.7 | 5 | 0.0 | 0.0 | урон 9.7 (5 поп.) · эффекты: negative/11, slowness · прим.: перемещение не замечено | да | — |
| divine | `song_of_perseverance` | PASS | 0.0 | 0 | 0.0 | 0.0 | эффекты: beneficial/19 | да | — |
| divine | `song_of_valor` | PASS | 0.0 | 0 | 0.0 | 10.8 | эффекты: beneficial/18 | да | — |
| divine | `song_of_vigor` | PASS | 0.0 | 0 | 0.0 | 10.8 | эффекты: beneficial/20 | да | — |
| divine | `awaken_mana` | PASS | 0.0 | 0 | 0.0 | 186.8 | мана +186.8 | да | — |
| divine | `power_chord` | PASS | 5.0 | 1 | 4.0 | 21.6 | урон 5.0 (1 поп.) · эффекты: negative/16 | да | — |
| divine | `taunt` | PASS | 0.0 | 0 | 0.0 | 10.8 | только каст: измеримых эффектов нет (агро/кулдаун/меню снаружи не видны) | да | — |
| divine | `banish` | PASS | 0.0 | 0 | 4.0 | 21.6 | эффекты: negative/0 · прим.: перемещение не замечено | да | — |
| divine | `wish` | PASS | 0.0 | 0 | 5.6 | 0.0 | лечение +5.6 | да | — |
| hunting | `arrow_storm` | PASS | 11.6 | 4 | 4.0 | 21.6 | урон 11.6 (4 поп.) · прим.: умение накладывается на выстрел из лука — прогон стреляет | да | — |
| hunting | `make_arrows` | PASS | 0.0 | 0 | 4.0 | 21.6 | предметы +64 · прим.: умение накладывается на выстрел из лука — прогон стреляет | да | — |
| hunting | `jump_field` | PASS | 0.0 | 0 | 0.0 | 0.0 | перемещение 22.7 бл. | да | — |
| hunting | `dash` | PASS | 0.0 | 0 | 4.0 | 21.6 | эффекты: slow_falling · перемещение 2.2 бл. | да | — |
| hunting | `hunter_potion` | PASS | 0.0 | 0 | 8.0 | 10.8 | лечение +8.0 | да | — |
| hunting | `poison_arrow` | PASS | 120.3 | 8 | 4.0 | 21.6 | урон 120.3 (8 поп.) · эффекты: negative/3, slowness, beneficial/32 · прим.: умение накладывается на выстрел из лука — прогон стреляет | да | — |
| hunting | `mark_for_death` | PASS | 0.0 | 0 | 0.0 | 0.0 | эффекты: beneficial/31, knockback_resist, negative/18, negative/3, glowing, slowness · прим.: умение накладывается на выстрел из лука — прогон стреляет | да | — |
| hunting | `smoke_bomb` | PASS | 0.0 | 0 | 0.0 | 0.0 | эффекты: negative/10 | да | — |
| hunting | `the_hunt` | PASS | 0.0 | 0 | 0.0 | 0.0 | эффекты: speed, night_vision, glowing | да | — |
| hunting | `charged_bolt` | PASS | 4.9 | 1 | 6.0 | 32.4 | урон 4.9 (1 поп.) · эффекты: slowness · прим.: умение накладывается на выстрел из лука — прогон стреляет | да | — |
| hunting | `shred` | PASS | 5.8 | 2 | 4.0 | 21.6 | урон 5.8 (2 поп.) · эффекты: negative/19, slowness · прим.: умение накладывается на выстрел из лука — прогон стреляет | да | — |
| hunting | `recoil_shot` | PASS | 16.5 | 1 | 4.0 | 21.6 | урон 16.5 (1 поп.) · эффекты: negative/4 · перемещение 4.2 бл. · прим.: умение накладывается на выстрел из лука — прогон стреляет | да | — |
| hunting | `backflip` | PASS | 0.0 | 0 | 0.0 | 0.0 | эффекты: beneficial/30, negative/4 · перемещение 8.0 бл. · прим.: умение накладывается на выстрел из лука — прогон стреляет | да | — |
| hunting | `levitation` | PASS | 0.0 | 0 | 0.0 | 10.8 | перемещение 0.7 бл. | да | — |
| hunting | `execute` | PASS | 101.6 | 9 | 0.0 | 10.8 | урон 101.6 (9 поп.) | да | — |
| hunting | `fire_trap` | PASS | 4.9 | 1 | 6.0 | 32.4 | урон 4.9 (1 поп.) | да | — |
| hunting | `frost_trap` | PASS | 4.9 | 1 | 4.0 | 21.6 | урон 4.9 (1 поп.) | да | — |
| hunting | `poison_trap` | PASS | 4.9 | 1 | 4.0 | 21.6 | урон 4.9 (1 поп.) | да | — |
| hunting | `night_vision` | PASS | 0.0 | 0 | 0.0 | 0.0 | эффекты: night_vision | да | — |
| hunting | `explosive_arrow` | PASS | 4.9 | 1 | 4.0 | 21.6 | урон 4.9 (1 поп.) · эффекты: slowness · прим.: умение накладывается на выстрел из лука — прогон стреляет | да | — |
| nature | `poison_ball` | PASS | 4.9 | 1 | 4.0 | 21.6 | урон 4.9 (1 поп.) · эффекты: slowness · прим.: умение накладывается на выстрел из лука — прогон стреляет | да | — |
| nature | `poisoned_weapons` | PASS | 0.0 | 0 | 0.0 | 10.8 | эффекты: beneficial/9 | да | — |
| nature | `nature_balm` | PASS | 0.0 | 0 | 5.0 (×2) | 10.8 | лечение +5.0 ×2 · эффекты: beneficial/5 | да | — |
| nature | `thorn_armor` | PASS | 0.0 | 0 | 0.0 | 10.8 | эффекты: beneficial/6 | да | — |
| nature | `entangling_seed` | PASS | 0.0 | 0 | 4.0 | 21.6 | эффекты: negative/1 | да | — |
| nature | `rejuv_totem` | PASS | 0.0 | 0 | 8.0 (×6) | 21.6 | лечение +8.0 ×6 | да | — |
| nature | `astral_totem` | PASS | 0.0 | 0 | 4.0 | 177.6 (×6) | мана +177.6 ×6 | да | — |
| nature | `guard_totem` | PASS | 0.0 | 0 | 4.0 | 21.6 | щит +156.0 | да | — |
| nature | `poison_cloud` | PASS | 102.9 | 6 | 0.0 | 10.8 | урон 102.9 (6 поп.) · эффекты: negative/3 | да | — |
| nature | `curse_of_agony` | PASS | 2.0 | 1 | 4.0 | 21.6 | урон 2.0 (1 поп.) · щит +156.0 · эффекты: negative/13 | да | — |
| nature | `curse_of_weak` | PASS | 2.0 | 1 | 0.0 | 0.0 | урон 2.0 (1 поп.) · эффекты: negative/14 | да | — |
| nature | `curse_of_despair` | PASS | 2.0 | 1 | 4.0 | 21.6 | урон 2.0 (1 поп.) · эффекты: negative/15 | да | — |
| nature | `conjure_ender_chest` | PASS | 0.0 | 0 | 0.0 | 10.8 | только каст: измеримых эффектов нет (агро/кулдаун/меню снаружи не видны) | да | — |
| nature | `refresh` | PASS | 0.0 | 0 | 0.0 | 10.8 | только каст: измеримых эффектов нет (агро/кулдаун/меню снаружи не видны) | да | — |
| fire | `fireball` | PASS | 7.3 | 1 | 4.0 | 21.6 | урон 7.3 (1 поп.) | да | — |
| fire | `flame_strike` | PASS | 3.9 | 1 | 0.0 | 0.0 | урон 3.9 (1 поп.) | да | — |
| fire | `fire_weapon` | PASS | 0.0 | 0 | 0.0 | 10.8 | эффекты: beneficial/7 | да | — |
| fire | `meteor` | PASS | 10.7 | 1 | 4.0 | 21.6 | урон 10.7 (1 поп.) | да | — |
| fire | `fire_nova` | PASS | 21.4 | 2 | 0.0 | 0.0 | урон 21.4 (2 поп.) | да | — |
| fire | `vamp_blood` | PASS | 0.0 | 0 | 0.0 | 10.8 | эффекты: beneficial/3 | да | — |
| fire | `draconic_blood` | PASS | 0.0 | 0 | 0.0 | 10.8 | эффекты: beneficial/4 | да | — |
| fire | `overload` | PASS | 0.0 | 0 | 0.0 | 10.8 | эффекты: beneficial/17 | да | — |
| water | `frostball` | PASS | 4.9 | 1 | 4.0 | 21.6 | урон 4.9 (1 поп.) | да | — |
| water | `tidal_strike` | PASS | 3.9 | 1 | 0.0 | 10.8 | урон 3.9 (1 поп.) | да | — |
| water | `frost_nova` | PASS | 21.4 | 2 | 0.0 | 10.8 | урон 21.4 (2 поп.) | да | — |
| water | `frost_armor` | PASS | 0.0 | 0 | 0.0 | 0.0 | эффекты: beneficial/14 | да | — |
| water | `water_breath` | PASS | 0.0 | 0 | 0.0 | 0.0 | эффекты: water_breathing | да | — |
| water | `chilling_field` | PASS | 11.6 | 6 | 0.0 | 0.0 | урон 11.6 (6 поп.) · эффекты: negative/2 | да | — |
| water | `ice_weapon` | PASS | 0.0 | 0 | 0.0 | 10.8 | эффекты: beneficial/10 | да | — |
| water | `ice_comet` | PASS | 7.8 | 1 | 4.0 | 21.6 | урон 7.8 (1 поп.) | да | — |
| water | `chill_eruption` | PASS | 32.0 | 3 | 0.0 | 10.8 | урон 32.0 (3 поп.) · прим.: подготовка: на манекены наложен mmorpg:negative/2 | да | — |
| water | `nourishment` | PASS | 0.0 | 0 | 0.0 | 10.8 | эффекты: beneficial/2 | да | — |
| water | `mage_circle` | PASS | 0.0 | 0 | 4.0 | 21.6 | эффекты: beneficial/33 · перемещение 2.2 бл. | да | — |
| water | `teleport` | PASS | 0.0 | 0 | 4.0 | 21.6 | перемещение 2.2 бл. | да | — |
| water | `heart_of_ice` | PASS | 0.0 | 0 | 8.0 | 10.8 | лечение +8.0 | да | — |
| water | `black_hole` | PASS | 2.0 | 1 | 4.0 | 21.6 | урон 2.0 (1 поп.) · прим.: перемещение не замечено | да | — |
