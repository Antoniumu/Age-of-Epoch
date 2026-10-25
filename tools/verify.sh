#!/usr/bin/env bash
#
# Одна команда, которая проверяет всё, что можно проверить без живого сервера:
#   1) константы в коде совпадают с обещаниями LAUNCH-READINESS.md;
#   2) контент целостен: дерево талантов, id перков, иконки, число уникатов;
#   3) каждое заклинание: урон/лечение/эффект, партиклы, звук, описание, иконка
#      и место в дереве школы (check_spells.py) — без этого умение не выучить
#      или оно визуально «мёртвое»;
#   4) модель баланса сходится (balance_model.py);
#   5) симуляция сезона для компании 4-6 человек отрабатывает и печатает цифры;
#   6) если в системе есть JDK+Gradle — ещё и компиляция мода.
#
# Запуск:  ./tools/verify.sh
# Выход:   0 — всё зелёное; 1 — что-то не сошлось (смотри вывод выше).

set -u
cd "$(dirname "$0")/.." || exit 1

status=0
line() { printf '\n=== %s ===\n' "$1"; }

line "1/6. Инварианты: код против отчёта"
python3 tools/verify/check_invariants.py || status=1

line "2/6. Контент: дерево талантов, перки, иконки"
python3 tools/verify/check_content.py || status=1
# вики: страница на каждый предмет/блок, >=1000 видимых символов, целые ссылки
python3 tools/wiki_gen/verify_wiki.py || status=1

line "3/6. Заклинания: эффект, партиклы, иконка, место в школе"
python3 tools/verify/check_spells.py || status=1

line "4/6. Модель баланса"
python3 tools/balance/balance_model.py || status=1

line "5/6. Симуляция сезона (6 игроков, 8 недель)"
python3 tools/balance/season_sim.py || status=1

line "6/6. Компиляция мода"
missing=""
[ -x ./gradlew ] || missing="./gradlew не исполняемый (chmod +x gradlew)"
command -v java >/dev/null 2>&1 || missing="${missing:+$missing; }java не в PATH"

if [ -z "$missing" ]; then
    ./gradlew compileJava --offline -q || status=1
else
    echo "ПРОПУЩЕНО: $missing"
    echo "Это единственная проверка, которую нельзя сделать в песочнице."
    echo "Проекту нужен JDK 25 (build.gradle: release = 25)."
    echo "На машине с JDK 25: ./gradlew build   (или ./gradlew compileJava)"
fi

line "Итог"
if [ "$status" -eq 0 ]; then
    echo "OK: инварианты и математика сходятся."
else
    echo "ЕСТЬ ПРОБЛЕМЫ: смотри FAIL/ошибки выше."
fi
exit "$status"
