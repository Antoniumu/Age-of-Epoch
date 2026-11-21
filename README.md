# Аудит умений (все школы заклинаний)

Цель: `all`, клиент под Xvfb, ветка `arena/62df65d6-age-of-epoch`

Обновлено: 2026-11-21 00:41 UTC · коммит `a236435453e0fc7d1e71c982784a0ea674271ac2` · [прогон](https://github.com/Antoniumu/Age-of-Epoch/actions/runs/44840443883)

Ветку перезаписывает workflow `.github/workflows/spell-audit.yml` при каждом прогоне — здесь всегда последний результат.

> **Строк AOEAI нет** — клиент не дошёл до аудита. Причина в `logs/steps.log`, `logs/gradle.log` и `logs/latest.log`.

## Видео по классам

В кадре подпись: название умения, его описание из тултипа и живые цифры (урон, попадания, лечение, эффекты, итог).

### audit-all

[▶ Смотреть audit-all.mp4](videos/audit-all.mp4) · [постер](videos/audit-all.png)

[![audit-all](videos/audit-all.png)](videos/audit-all.mp4)

## Структурный аудит (без запуска игры)

# Аудит заклинаний (структурный)

Заклинаний: **79**, школ: **5**, ошибок: **0**, предупреждений: **8**

## Предупреждения

- jubilant_veil: есть в коде, но нет в реестре (modpack_dev_helper/spells.txt) — список мог устареть
- maelstrom_waltz: есть в коде, но нет в реестре (modpack_dev_helper/spells.txt) — список мог устареть
- slipstream_surge: есть в коде, но нет в реестре (modpack_dev_helper/spells.txt) — список мог устареть
- tidewhip_lash: есть в коде, но нет в реестре (modpack_dev_helper/spells.txt) — список мог устареть
- unbound_joy: есть в коде, но нет в реестре (modpack_dev_helper/spells.txt) — список мог устареть
- undertow_grasp: есть в коде, но нет в реестре (modpack_dev_helper/spells.txt) — список мог устареть

Полностью: [`reports/spell-audit-structural.md`](reports/spell-audit-structural.md)

## Журналы

* [`logs/aoeai.log`](logs/aoeai.log)
* [`logs/ffmpeg.log`](logs/ffmpeg.log)
* [`logs/gradle.log`](logs/gradle.log)
* [`logs/steps.log`](logs/steps.log)

