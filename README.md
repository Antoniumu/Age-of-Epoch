# Аудит умений (все школы заклинаний)

Цель: `all`, клиент под Xvfb, ветка `arena/d1c5586c-age-of-epoch`

Обновлено: 2026-10-25 20:12 UTC · коммит `45966e01b7b336182009536441210a2a8b80429a` · [прогон](https://github.com/Antoniumu/Age-of-Epoch/actions/runs/40228949436)

Ветку перезаписывает workflow `.github/workflows/spell-audit.yml` при каждом прогоне — здесь всегда последний результат.

**Итог:** `AOEAI done spells=72 passed=72 failed=0`

Умений проверено: **144**, провалилось: **0**.

## Видео по классам

В кадре подпись: название умения, его описание из тултипа и живые цифры (урон, попадания, лечение, эффекты, итог).

### divine

[▶ Смотреть divine.mp4](videos/divine.mp4) · [постер](videos/divine.png)

[![divine](videos/divine.png)](videos/divine.mp4)

### fire

[▶ Смотреть fire.mp4](videos/fire.mp4) · [постер](videos/fire.png)

[![fire](videos/fire.png)](videos/fire.mp4)

### hunting

[▶ Смотреть hunting.mp4](videos/hunting.mp4) · [постер](videos/hunting.png)

[![hunting](videos/hunting.png)](videos/hunting.mp4)

### nature

[▶ Смотреть nature.mp4](videos/nature.mp4) · [постер](videos/nature.png)

[![nature](videos/nature.png)](videos/nature.mp4)

### water

[▶ Смотреть water.mp4](videos/water.mp4) · [постер](videos/water.png)

[![water](videos/water.png)](videos/water.mp4)

## Живой аудит (замеры в игре)

# Живой аудит заклинаний

Проверено: **72**, прошло: **72**, провалилось: **0**

Колонка «чем подтверждено» — только измеренные факты: урон из событий урона самого умения, лечение и мана из сработавших действий, щит из ресурсов игрока, эффекты и перемещение — по изменению состояния. Умения, у которых снаружи нечего измерить (агро, сброс кулдаунов, меню), помечены явно.

Каждое заклинание применялось тем же путём, что и с хотбара (`setToCast` → тик каста → `Spell.cast`), по манекенам впереди игрока. Если урон не прошёл, делался второй заход с кольцом манекенов вокруг игрока.


Полная таблица: [`reports/spell-audit.md`](reports/spell-audit.md)

## Структурный аудит (без запуска игры)

# Аудит заклинаний (структурный)

Заклинаний: **73**, школ: **5**, ошибок: **0**, предупреждений: **2**

## Предупреждения

- demon: есть в реестре, но не найдено в исходниках заклинаний
- test_spell: есть в реестре, но не найдено в исходниках заклинаний

## По школам

### divine (16)

Полностью: [`reports/spell-audit-structural.md`](reports/spell-audit-structural.md)

## Журналы

* [`logs/aoeai.log`](logs/aoeai.log)
* [`logs/ffmpeg.log`](logs/ffmpeg.log)
* [`logs/gradle.log`](logs/gradle.log)
* [`logs/jstack-spells.txt`](logs/jstack-spells.txt)
* [`logs/latest.log`](logs/latest.log)
* [`logs/steps.log`](logs/steps.log)

