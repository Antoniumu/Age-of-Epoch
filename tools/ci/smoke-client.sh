#!/usr/bin/env bash
#
# Smoke-тест клиента: запускает dev-клиент (runClient) под xvfb и ждёт, пока
# Minecraft дойдёт до главного меню. Проект ни разу не запускался в игре после
# порта на 26.3, поэтому этот тест — единственный способ поймать падения
# инициализации (миксины, регистрация экранов, worldgen) без живого человека.
#
# Успех: в run/logs/latest.log появился маркер готовности И нет crash-report.
# Провал: процесс упал раньше маркера, появился crash-report или истёк таймаут.
#
# Запуск локально (Linux, нужен xvfb):
#   bash tools/ci/smoke-client.sh            # таймаут 20 минут
#   SMOKE_TIMEOUT=600 bash tools/ci/smoke-client.sh
#
# Требует JDK 25 в JAVA_HOME/PATH (в CI его ставит .github/actions/setup-build-env).

set -u
cd "$(dirname "$0")/../.." || exit 1

timeout_sec="${SMOKE_TIMEOUT:-1200}"
log="run/logs/latest.log"
crash_dir="run/crash-reports"

mkdir -p run
rm -rf "$crash_dir"
: > "$log" 2>/dev/null || true

echo "Запуск клиента (таймаут ${timeout_sec}s)..."
setsid ./gradlew runClient --no-daemon --stacktrace > run/smoke-gradle.log 2>&1 &
gradle_pid=$!

# Маркеры того, что клиент дошёл до главного меню. Разные версии/логи дают
# разные строки, поэтому ждём любой из них.
markers='Sound engine started|OpenAL initialized|Created: [0-9]+x[0-9]+x[0-9]+ minecraft:textures/atlas|Backend library: LWJGL'

ready=0
elapsed=0
while [ "$elapsed" -lt "$timeout_sec" ]; do
    if [ -f "$log" ] && grep -Eq "$markers" "$log" 2>/dev/null; then
        ready=1
        break
    fi
    if [ -d "$crash_dir" ] && [ -n "$(ls -A "$crash_dir" 2>/dev/null)" ]; then
        echo "Клиент упал: появился crash-report."
        break
    fi
    if ! kill -0 "$gradle_pid" 2>/dev/null; then
        echo "Процесс Gradle завершился до готовности клиента."
        break
    fi
    sleep 5
    elapsed=$((elapsed + 5))
done

# Гасим всю группу процессов (Gradle + JVM клиента).
kill -TERM -"$gradle_pid" 2>/dev/null || kill -TERM "$gradle_pid" 2>/dev/null
sleep 3
kill -KILL -"$gradle_pid" 2>/dev/null || true

echo
echo "----- последние строки $log -----"
tail -n 60 "$log" 2>/dev/null || echo "(лога нет)"
echo "---------------------------------"

status=0
if [ -n "$(ls -A "$crash_dir" 2>/dev/null)" ]; then
    echo
    echo "CRASH-REPORT:"
    for f in "$crash_dir"/*; do
        echo "== $f"
        head -n 80 "$f"
    done
    status=1
fi

if [ "$ready" -eq 1 ]; then
    echo "OK: клиент дошёл до главного меню за ${elapsed}s."
else
    echo "ПРОВАЛ: клиент не дошёл до главного меню за ${elapsed}s."
    echo "Лог Gradle: run/smoke-gradle.log"
    status=1
fi

exit "$status"
