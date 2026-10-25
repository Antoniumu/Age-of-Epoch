#!/usr/bin/env bash
#
# Проверки, которым не нужен JDK 25: синтаксис Java (tree-sitter) и инварианты
# из LAUNCH-READINESS.md. Точно та же команда запускается в GitHub Actions
# (.github/workflows/ci.yml, job «static»), поэтому локальный прогон и CI
# не расходятся.
#
# Запуск:
#   bash tools/ci/static-checks.sh
#
# Зависимости (один раз):
#   python3 -m pip install -r tools/ci/requirements.txt
#
# Выход: 0 — всё зелёное; 1 — есть ошибки (что именно — в выводе выше).

set -u
cd "$(dirname "$0")/../.." || exit 1

status=0
line() { printf '\n=== %s ===\n' "$1"; }

line "1/5. Синтаксис всех Java-файлов (tree-sitter)"
python3 tools/check_java_syntax.py --all || status=1

line "2/5. Инварианты: код против LAUNCH-READINESS.md"
python3 tools/verify/check_invariants.py || status=1

line "3/5. Контент: дерево талантов, перки, иконки, вики"
python3 tools/verify/check_content.py || status=1
python3 tools/wiki_gen/verify_wiki.py || status=1

line "4/5. Модель баланса"
python3 tools/balance/balance_model.py || status=1

line "5/5. Симуляция сезона (6 игроков, 8 недель)"
python3 tools/balance/season_sim.py || status=1

line "Итог"
if [ "$status" -eq 0 ]; then
    echo "OK: статические проверки пройдены."
else
    echo "ЕСТЬ ПРОБЛЕМЫ: смотри FAIL/ошибки выше."
fi
exit "$status"
