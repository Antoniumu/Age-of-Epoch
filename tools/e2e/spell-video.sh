#!/usr/bin/env bash
#
# Запись видео прогона аудита умений.
#
# Почему x11grab, а не встроенный захват клиента: клиент под Xvfb рисует через
# Vulkan/lavapipe, и снимать кадры изнутри игры пришлось бы отдельным кодом
# рендера. X11-захват не трогает игру вообще — он просто пишет то окно, которое
# видит X-сервер, поэтому не может сломать прогон.
#
# Использование (из сценария e2e, где DISPLAY уже указывает на дисплей клиента):
#   bash tools/e2e/spell-video.sh start   # начать запись
#   bash tools/e2e/spell-video.sh stop    # остановить и проверить файл
#
# Результат: run/e2e/spell-video.mp4 (или .mkv), метки времени в
# run/e2e/video-start.txt / video-end.txt — по ним tools/e2e/split_videos.py
# режет запись на видео по классам.

set -u
cd "$(dirname "$0")/../.." || exit 1

action="${1:-start}"
# Куда писать видео: по умолчанию — файл аудита умений; аудит профессий
# передаёт свой путь через VIDEO_OUT (см. scenarios/professions.txt).
out="${VIDEO_OUT:-run/e2e/spell-video.mp4}"
pidfile="run/e2e/ffmpeg.pid"
log="run/e2e/ffmpeg.log"
startfile="run/e2e/video-start.txt"
endfile="run/e2e/video-end.txt"

now_ms() {
    date +%s%3N
}

window_geometry() {
    # Геометрия окна клиента: снимать весь экран нельзя — на раннере он больше
    # окна, и половина кадра была бы чёрной.
    local win
    win="$(xdotool search --name Minecraft 2>/dev/null | tail -1)"
    if [ -z "$win" ]; then
        win="$(xdotool search --class Minecraft 2>/dev/null | tail -1)"
    fi
    if [ -z "$win" ]; then
        echo "1280 720 0 0"
        return
    fi
    local w h x y
    w="$(xdotool getwindowgeometry --shell "$win" | awk -F= '/^WIDTH=/{print $2}')"
    h="$(xdotool getwindowgeometry --shell "$win" | awk -F= '/^HEIGHT=/{print $2}')"
    x="$(xdotool getwindowgeometry --shell "$win" | awk -F= '/^X=/{print $2}')"
    y="$(xdotool getwindowgeometry --shell "$win" | awk -F= '/^Y=/{print $2}')"
    # чётные размеры: libx264 требует кратные 2 (иначе scale делает это сам)
    w="${w:-1280}"
    h="${h:-720}"
    x="${x:-0}"
    y="${y:-0}"
    echo "$w $h $x $y"
}

case "$action" in
    start)
        mkdir -p run/e2e
        rm -f "$out" "$pidfile" "$endfile"
        command -v ffmpeg >/dev/null 2>&1 || {
            echo "ffmpeg не установлен — видео не будет" >&2
            exit 1
        }
        read -r w h x y <<< "$(window_geometry)"
        echo "окно клиента: ${w}x${h}+${x}+${y} на ${DISPLAY:-?}"
        now_ms > "$startfile"
        # -draw_mouse 0: курсор в кадре не нужен, он отвлекает.
        nohup ffmpeg -y -nostdin -loglevel warning \
            -f x11grab -draw_mouse 0 -framerate 15 \
            -video_size "${w}x${h}" -i "${DISPLAY}+${x},${y}" \
            -vf "scale=trunc(iw/2)*2:trunc(ih/2)*2" \
            -c:v libx264 -preset ultrafast -crf 27 -pix_fmt yuv420p \
            -t 3600 "$out" > "$log" 2>&1 &
        echo $! > "$pidfile"
        # Ждём, пока ffmpeg реально начнёт писать: иначе первые секунды аудита
        # (а это первое заклинание) не попадут в кадр.
        for _ in $(seq 1 20); do
            if [ -s "$out" ] && kill -0 "$(cat "$pidfile")" 2>/dev/null; then
                echo "запись пошла: $out ($(stat -c%s "$out") байт)"
                exit 0
            fi
            sleep 0.5
        done
        echo "ffmpeg не начал писать, смотри $log:" >&2
        tail -n 20 "$log" >&2 || true
        exit 1
        ;;
    stop)
        if [ ! -f "$pidfile" ]; then
            echo "нет $pidfile — запись не запускалась" >&2
            exit 0
        fi
        pid="$(cat "$pidfile")"
        # SIGINT — штатное завершение ffmpeg: он дописывает индекс mp4.
        kill -INT "$pid" 2>/dev/null || true
        for _ in $(seq 1 60); do
            kill -0 "$pid" 2>/dev/null || break
            sleep 0.5
        done
        kill -TERM "$pid" 2>/dev/null || true
        now_ms > "$endfile"
        rm -f "$pidfile"
        if [ -s "$out" ]; then
            echo "видео готово: $out ($(du -h "$out" | cut -f1))"
            # probe/ffprobe может отсутствовать — это не ошибка
            ffprobe -v error -show_entries format=duration -of default=nw=1:nk=1 "$out" 2>/dev/null \
                | awk '{printf "длительность: %.1f c\n", $1}' || true
            exit 0
        fi
        echo "видео пустое, смотри $log:" >&2
        tail -n 30 "$log" >&2 || true
        exit 1
        ;;
    *)
        echo "использование: $0 start|stop" >&2
        exit 2
        ;;
esac
