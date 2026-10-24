#!/usr/bin/env bash
#
# Одна команда, которая проверяет всё, что можно проверить без живого сервера:
#   1) константы в коде совпадают с обещаниями LAUNCH-READINESS.md;
#   2) контент целостен: дерево талантов, id перков, иконки, число уникатов;
#   3) модель баланса сходится (balance_model.py);
#   4) симуляция сезона для компании 4-6 человек отрабатывает и печатает цифры;
#   5) если в системе есть JDK+Gradle — ещё и компиляция мода.
#
# Запуск:  ./tools/verify.sh
# Выход:   0 — всё зелёное; 1 — что-то не сошлось (смотри вывод выше).

set -u
cd "$(dirname "$0")/.." || exit 1

status=0
line() { printf '\n=== %s ===\n' "$1"; }

line "1/5. Инварианты: код против отчёта"
python3 tools/verify/check_invariants.py || status=1

line "2/5. Контент: дерево талантов, перки, иконки"
python3 tools/verify/check_content.py || status=1

line "3/5. Модель баланса"
python3 tools/balance/balance_model.py || status=1

line "4/5. Симуляция сезона (6 игроков, 8 недель)"
python3 tools/balance/season_sim.py || status=1

line "5/5. Компиляция мода"
if [ -x ./gradlew ] && command -v java >/dev/null 2>&1; then
    ./gradlew compileJava --offline -q || status=1
else
    echo "ПРОПУЩЕНО: нет ./gradlew или java."
    echo "Это единственная проверка, которую нельзя сделать в песочнице."
    echo "На машине с JDK 21: ./gradlew build   (или ./gradlew compileJava)"
fi

line "Итог"
if [ "$status" -eq 0 ]; then
    echo "OK: инварианты и математика сходятся."
else
    echo "ЕСТЬ ПРОБЛЕМЫ: смотри FAIL/ошибки выше."
fi
exit "$status"
