#!/usr/bin/env bash
#
# Поднимает Xvfb, запускает dev-клиент и прогоняет сценарий со скриншотами.
# Скриншоты нужны даже при падении клиента, поэтому сценарий не прерывает
# работу: что успели снять — то и уедет в артефакты и в ветку ci-screenshots.
#
# Запуск:  bash tools/e2e/run-in-xvfb.sh [сценарий] [каталог-снимков]
# Переменные: XVFB_W, XVFB_H, CLIENT_TIMEOUT (сек), DISPLAY_NUM

set -u
cd "$(dirname "$0")/../.." || exit 1

scenario="${1:-tools/e2e/scenarios/boot.txt}"
outdir="${2:-run/e2e/shots}"
display_num="${DISPLAY_NUM:-99}"
width="${XVFB_W:-1280}"
height="${XVFB_H:-720}"
client_timeout="${CLIENT_TIMEOUT:-1500}"

mkdir -p "$outdir" run/e2e run/logs
: > run/e2e/gradle.log

echo "=== Xvfb ${width}x${height} на :${display_num} ==="
Xvfb ":${display_num}" -screen 0 "${width}x${height}x24" -nolisten tcp > run/e2e/xvfb.log 2>&1 &
xvfb_pid=$!
export DISPLAY=":${display_num}"
for _ in $(seq 1 20); do
    if xdpyinfo > run/e2e/xdpyinfo.txt 2>/dev/null; then break; fi
    sleep 1
done
head -8 run/e2e/xdpyinfo.txt 2>/dev/null || true

# Настройки клиента: фиксированный размер окна и размер GUI, чтобы координаты
# кликов и содержимое кадров были одинаковыми от прогона к прогону.
mkdir -p run
cat > run/options.txt <<EOF
version:4620
renderDistance:2
simulationDistance:5
guiScale:2
maxFps:60
overrideWidth:${width}
overrideHeight:${height}
fullscreen:false
pauseOnLostFocus:false
narrator:0
skipMultiplayerWarning:true
tutorialStep:none
onboardAccessibility:false
EOF

cleanup() {
    echo "=== остановка клиента ==="
    kill -TERM "$gradle_pid" 2>/dev/null || true
    pkill -TERM -f 'net.fabricmc.devlaunchinjector' 2>/dev/null || true
    pkill -TERM -f 'net.minecraft.client.main.Main' 2>/dev/null || true
    sleep 3
    pkill -KILL -f 'net.fabricmc.devlaunchinjector' 2>/dev/null || true
    kill -KILL "$xvfb_pid" 2>/dev/null || true
}
gradle_pid=""

echo "=== запуск клиента ==="
./gradlew runClient --stacktrace --no-problems-report >> run/e2e/gradle.log 2>&1 &
gradle_pid=$!

# Клиент качается/компилируется при первом прогоне: ждём появления окна в сценарии.
started=$(date +%s)
python3 tools/e2e/run_scenario.py "$scenario" "$outdir"
scenario_status=$?
finished=$(date +%s)
echo "=== сценарий занял $((finished - started))s, статус ${scenario_status} ==="

# Клиент мог упасть: фиксируем это отдельно, чтобы job был красным, но снимки остались.
if [ -d run/crash-reports ] && [ -n "$(ls -A run/crash-reports 2>/dev/null)" ]; then
    echo "=== CRASH-REPORT ==="
    for f in run/crash-reports/*; do
        echo "--- $f"
        head -c 4000 "$f"
    done
    scenario_status=1
fi

echo "=== хвост лога клиента ==="
tail -n 40 run/logs/latest.log 2>/dev/null || echo "(лога нет)"

cleanup
exit "$scenario_status"
