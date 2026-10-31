#!/usr/bin/env bash
#
# Публикует результат аудита профессий (отчёт + скриншоты + видео по профессиям)
# в отдельную ветку репозитория.
#
# Зачем ветка: артефакты Actions не открываются по прямой ссылке, а mp4 и PNG в
# ветке открываются в браузере прямо из GitHub. Ветку перезаписывает force-push —
# хранится только последний прогон.
#
# Запуск (в CI):
#   REPO=owner/repo BRANCH=ci-profession-audit GH_TOKEN=... \
#     bash tools/e2e/publish-profession-audit.sh run/e2e
#
# Никогда не падает молча: если видео не собралось, всё равно публикует отчёт,
# скриншоты и журналы — по ним видно, на какой профессии клиент упал.

set -u

src="${1:-run/e2e}"
repo="${REPO:?REPO не задан}"
branch="${BRANCH:-ci-profession-audit}"
token="${GH_TOKEN:?GH_TOKEN не задан}"

root="$(cd "$(dirname "$0")/../.." && pwd)"
src="$(cd "$src" 2>/dev/null && pwd || echo "$root/run/e2e")"
title="${AUDIT_TITLE:-аудит профессий}"

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
cd "$work" || exit 1
git init -q -b "$branch"
git config user.name 'github-actions[bot]'
git config user.email 'github-actions[bot]@users.noreply.github.com'
git remote add origin "https://x-access-token:${token}@github.com/${repo}.git"

mkdir -p videos reports logs shots

# --- отчёты, которые пишет сам мод в каталоге игры (run/) -------------------
for name in profession-audit.md profession-audit.json profession-audit.log profession-audit-timeline.txt; do
    for candidate in "$root/run/$name" "$src/$name"; do
        if [ -f "$candidate" ]; then
            cp "$candidate" "reports/$name"
            echo "  + reports/$name"
            break
        fi
    done
done

# --- структурный отчёт (быстрый, без игры) ----------------------------------
for name in profession-audit-structural.md profession-audit-structural.json; do
    [ -f "$src/$name" ] && cp "$src/$name" "reports/$name" && echo "  + reports/$name"
done

# --- скриншоты: каждое перемещение предмета в сетку крафта ------------------
if [ -d "$root/run/e2e/shots/professions" ]; then
    cp "$root"/run/e2e/shots/professions/*.png shots/ 2>/dev/null || true
    [ -f "$root/run/e2e/shots/professions/steps.log" ] && cp "$root/run/e2e/shots/professions/steps.log" logs/steps.log
fi
shot_count="$(ls shots/*.png 2>/dev/null | wc -l | tr -d ' ')"
echo "  скриншотов: $shot_count"

# --- видео и постеры --------------------------------------------------------
if [ -d "$src/prof-videos" ]; then
    cp "$src"/prof-videos/*.mp4 "videos/" 2>/dev/null || true
    cp "$src"/prof-videos/*.png "videos/" 2>/dev/null || true
    [ -f "$src/prof-videos/README.md" ] && cp "$src/prof-videos/README.md" "videos/README.md"
fi
# Полное видео прогона — «видео с полной отладкой».
if [ -f "$src/profession-video.mp4" ]; then
    cp "$src/profession-video.mp4" videos/full-debug.mp4
    echo "  + videos/full-debug.mp4"
fi
video_count="$(ls videos/*.mp4 2>/dev/null | wc -l | tr -d ' ')"
echo "  видео: $video_count"

# --- журналы ----------------------------------------------------------------
copy_tail() {
    local from="$1" to="$2" lines="${3:-300}"
    if [ -f "$from" ]; then
        tail -n "$lines" "$from" > "logs/$to"
        echo "  + logs/$to"
    fi
}
copy_tail "$root/run/logs/latest.log" latest.log 600
copy_tail "$root/run/e2e/gradle.log" gradle.log 400
copy_tail "$root/run/e2e/ffmpeg.log" ffmpeg.log 120
copy_tail "$root/run/e2e/shots/professions/steps.log" steps.log 600
for f in "$root"/run/e2e/shots/professions/jstack-*.txt; do
    [ -e "$f" ] || continue
    cp "$f" "logs/$(basename "$f")"
done
# Краш-репорты клиента: если клиент умер посреди прогона, причина должна
# остаться в ветке, а не только в недоступном артефакте Actions.
if [ -d "$root/run/crash-reports" ]; then
    for f in "$root"/run/crash-reports/*; do
        [ -e "$f" ] || continue
        cp "$f" "logs/crash-$(basename "$f")"
        echo "  + logs/crash-$(basename "$f")"
    done
fi

# Строки аудита — отдельным файлом: по ним строится сводка.
{
    grep -h "AOEPR " "$root/run/logs/latest.log" 2>/dev/null
    grep -h "AOEPR " "$root/run/profession-audit.log" 2>/dev/null
} | awk '!seen[$0]++' > logs/aoepr.log 2>/dev/null || true
echo "  + logs/aoepr.log ($(wc -l < logs/aoepr.log 2>/dev/null || echo 0) строк)"

# --- README -----------------------------------------------------------------
{
    echo "# Аудит профессий (все ремёсла + видео)"
    echo
    echo "$title"
    echo
    echo "Обновлено: $(date -u '+%Y-%m-%d %H:%M UTC') · коммит \`${GITHUB_SHA:-local}\` · [прогон](${RUN_URL:-—})"
    echo
    echo "Ветку перезаписывает workflow \`.github/workflows/profession-audit.yml\` при каждом прогоне — здесь всегда последний результат."
    echo
    if [ -s logs/aoepr.log ]; then
        summary="$(grep -h "AOEPR done" logs/aoepr.log | tail -1)"
        [ -n "$summary" ] && { echo "**Итог:** \`$summary\`"; echo; }
        failed="$(grep -h "AOEPR prof=" logs/aoepr.log | grep -c "verdict=FAIL" || true)"
        total="$(grep -h "AOEPR prof=" logs/aoepr.log | grep -c "verdict=" || true)"
        echo "Профессий проверено: **$total**, провалилось: **$failed**."
        echo
    else
        echo "> **Строк AOEPR нет** — клиент не дошёл до аудита. Причина в \`logs/steps.log\`, \`logs/gradle.log\` и \`logs/latest.log\`."
        echo
    fi

    if [ -s logs/aoepr.log ]; then
        echo "## Живые шаги (что происходило в сетке крафта)"
        echo
        echo '```'
        grep -hE "AOEPR (given|gui_open|placed|result_ready|taken|crafted|prof=)" logs/aoepr.log | tail -60
        echo '```'
        echo
    fi

    if [ "$shot_count" -gt 0 ]; then
        echo "## Скриншоты: каждое перемещение в сетку 3×2"
        echo
        for f in shots/*.png; do
            [ -e "$f" ] || continue
            base="$(basename "$f" .png)"
            echo "**${base}**"
            echo
            echo "[![${base}](shots/${base}.png)](shots/${base}.png)"
            echo
        done
    else
        echo "## Скриншоты"
        echo
        echo "_Снимков нет — сценарий не дошёл до сетки крафта._"
        echo
    fi

    if [ "$video_count" -gt 0 ]; then
        echo "## Видео с полной отладкой"
        echo
        if [ -f videos/full-debug.mp4 ]; then
            echo "* [▶ Полное видео прогона (full-debug.mp4)](videos/full-debug.mp4)"
        fi
        echo
        echo "### Ролики по профессиям"
        echo
        for f in videos/*.mp4; do
            [ -e "$f" ] || continue
            base="$(basename "$f" .mp4)"
            [ "$base" = "full-debug" ] && continue
            echo "### $base"
            echo
            echo "[▶ Смотреть $base.mp4]($f) · [постер](${f%.mp4}.png)"
            echo
            if [ -f "videos/${base}.png" ]; then
                echo "[![${base}](videos/${base}.png)]($f)"
                echo
            fi
        done
    else
        echo "## Видео"
        echo
        echo "_Видео не собралось. Запись и нарезка — в \`logs/ffmpeg.log\` и \`logs/steps.log\`._"
        echo
    fi

    if [ -f reports/profession-audit.md ]; then
        echo "## Живой аудит (замеры в игре)"
        echo
        sed -n '1,12p' reports/profession-audit.md
        echo
        echo "Полная таблица: [\`reports/profession-audit.md\`](reports/profession-audit.md)"
        echo
    fi
    if [ -f reports/profession-audit-structural.md ]; then
        echo "## Структурный аудит (без запуска игры)"
        echo
        sed -n '1,12p' reports/profession-audit-structural.md
        echo
        echo "Полностью: [\`reports/profession-audit-structural.md\`](reports/profession-audit-structural.md)"
        echo
    fi

    echo "## Журналы"
    echo
    for f in logs/*; do
        [ -e "$f" ] || continue
        echo "* [\`$f\`]($f)"
    done
    echo
} > README.md

git add -A
git commit -q -m "profession audit: $title" || true
# Без этой проверки скрипт всегда выходил с кодом echo (0), и неудачный push
# выглядел как успех: ветка молча оставалась с результатами прошлого прогона.
if ! git push --force origin "$branch"; then
    echo "ОШИБКА: git push в ветку $branch не удался" >&2
    exit 1
fi
echo "Готово: https://github.com/${repo}/tree/${branch}"
