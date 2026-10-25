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

# Minecraft 26.3 рисует через SDL3 + renderpearl. Под Xvfb его OpenGL-бэкенд
# не находит sRGB-capable GLX visual, поэтому клиент обязан уйти в Vulkan.
# Lavapipe (mesa-vulkan-drivers) даёт программный Vulkan с VK_KHR_surface прямо
# в Xvfb — без него оба бэкенда падают и окно не появляется вовсе.
export LIBGL_ALWAYS_SOFTWARE=1
export GALLIUM_DRIVER=llvmpipe
if [ -f /usr/share/vulkan/icd.d/lvp_icd.x86_64.json ]; then
    export VK_ICD_FILENAMES=/usr/share/vulkan/icd.d/lvp_icd.x86_64.json
fi
export SDL_VIDEODRIVER="${SDL_VIDEODRIVER:-x11}"
# На раннере нет звуковой карты: null-драйвер OpenAL даёт клиенту «включённый»
# звук, и он доходит до меню без ветки «Error starting SoundSystem».
export ALSOFT_DRIVERS=null

echo "=== Xvfb ${width}x${height} на :${display_num} ==="
Xvfb ":${display_num}" -screen 0 "${width}x${height}x24" -nolisten tcp > run/e2e/xvfb.log 2>&1 &
xvfb_pid=$!
export DISPLAY=":${display_num}"
for _ in $(seq 1 20); do
    if xdpyinfo > run/e2e/xdpyinfo.txt 2>/dev/null; then break; fi
    sleep 1
done
head -8 run/e2e/xdpyinfo.txt 2>/dev/null || true

# Оконный менеджер нужен не для красоты: без WM окно SDL не маппится
# нормально, его не находит xdotool, и снимок корня остаётся пустым.
# Loom запускает клиент через собственный xvfb-run, поэтому оконный менеджер
# нужен не только на нашем дисплее: поднимаем openbox на каждом X-дисплее,
# который появится в системе (включая созданные позже xvfb-run).
start_wm() {
    command -v openbox >/dev/null 2>&1 || return 0
    local display="$1"
    case " $wm_displays " in *" $display "*) return 0 ;; esac
    DISPLAY="$display" openbox > "run/e2e/openbox-${display#:}.log" 2>&1 &
    wm_displays="$wm_displays $display"
    echo "openbox на $display"
}
wm_displays=""
for _ in $(seq 1 30); do
    for sock in /tmp/.X11-unix/X*; do
        [ -e "$sock" ] || continue
        num="${sock##*/X}"
        case "$num" in ''|*[!0-9]*) continue ;; esac
        start_wm ":$num"
    done
    sleep 2
done &
wm_watcher=$!

# Диагностика графики: без неё непонятно, какой бэкенд доступен клиенту.
{
    echo "--- vulkaninfo --summary"
    vulkaninfo --summary 2>&1 | head -40 || echo "vulkaninfo нет"
    echo "--- glxinfo -B"
    glxinfo -B 2>&1 | head -20 || echo "glxinfo нет"
} > run/e2e/graphics.txt

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
    [ -n "${wm_watcher:-}" ] && kill -TERM "$wm_watcher" 2>/dev/null || true
    pkill -KILL openbox 2>/dev/null || true
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

# Проверки зонда (AOETEST). Боевые замеры dps/heal/team — как договорено в
# workflow, только предупреждение (тайминги боя нестабильны). А регрессия gear
# (макс. HP от экипировки) валит job: она детерминированная и означает, что
# фикс обновления здоровья сломан. Снимки к этому моменту уже сняты.
if grep -h "AOETEST gear" run/logs/latest.log run/aoe-devtest.txt 2>/dev/null | awk '!seen[$0]++' | grep -qE "=fail|fatal="; then
    echo "=== AOETEST: провалились проверки экипировки/макс. HP ==="
    grep -h "AOETEST gear" run/logs/latest.log run/aoe-devtest.txt 2>/dev/null | awk '!seen[$0]++'
    mkdir -p run/e2e
    touch run/e2e/aoetest-gear-failed
    scenario_status=1
fi

echo "=== окна на дисплее (xwininfo) ==="
xwininfo -root -tree 2>&1 | head -30 || true

echo "=== хвост лога клиента ==="
tail -n 40 run/logs/latest.log 2>/dev/null || echo "(лога нет)"

cleanup
exit "$scenario_status"
