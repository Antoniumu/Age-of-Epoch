#!/usr/bin/env bash
#
# Публикует результат аудита умений (отчёт + видео по классам) в отдельную
# ветку репозитория.
#
# Зачем ветка: артефакты Actions не открываются по прямой ссылке, а mp4 в ветке
# открывается в браузере и играется прямо из GitHub. Ветку перезаписывает
# force-push — хранится только последний прогон.
#
# Запуск (в CI):
#   REPO=owner/repo BRANCH=ci-spell-audit GH_TOKEN=... \
#     bash tools/e2e/publish-spell-audit.sh run/e2e
#
# Никогда не падает молча: если видео не собралось, всё равно публикует отчёт и
# журналы — по ним видно, на каком умении клиент упал.

set -u

src="${1:-run/e2e}"
repo="${REPO:?REPO не задан}"
branch="${BRANCH:-ci-spell-audit}"
token="${GH_TOKEN:?GH_TOKEN не задан}"

root="$(cd "$(dirname "$0")/../.." && pwd)"
src="$(cd "$src" 2>/dev/null && pwd || echo "$root/run/e2e")"
title="${AUDIT_TITLE:-аудит умений}"

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
cd "$work" || exit 1
git init -q -b "$branch"
git config user.name 'github-actions[bot]'
git config user.email 'github-actions[bot]@users.noreply.github.com'
git remote add origin "https://x-access-token:${token}@github.com/${repo}.git"

mkdir -p videos reports logs

# --- отчёты, которые пишет сам мод в каталоге игры (run/) -------------------
for name in spell-audit.md spell-audit.json spell-audit.log spell-audit-timeline.txt; do
    for candidate in "$root/run/$name" "$src/$name"; do
        if [ -f "$candidate" ]; then
            cp "$candidate" "reports/$name"
            echo "  + reports/$name"
            break
        fi
    done
done

# --- структурный отчёт (быстрый, без игры) ----------------------------------
for name in spell-audit-structural.md spell-audit-structural.json; do
    [ -f "$src/$name" ] && cp "$src/$name" "reports/$name" && echo "  + reports/$name"
done

# --- видео и постеры --------------------------------------------------------
if [ -d "$src/videos" ]; then
    cp "$src"/videos/*.mp4 "videos/" 2>/dev/null || true
    cp "$src"/videos/*.png "videos/" 2>/dev/null || true
    [ -f "$src/videos/README.md" ] && cp "$src/videos/README.md" "videos/README.md"
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
copy_tail "$root/run/e2e/shots/spells/steps.log" steps.log 600
for f in "$root"/run/e2e/shots/spells/jstack-*.txt; do
    [ -e "$f" ] || continue
    cp "$f" "logs/$(basename "$f")"
done

# Строки аудита — отдельным файлом: по ним строится сводка.
{
    grep -h "AOEAI " "$root/run/logs/latest.log" 2>/dev/null
    grep -h "AOEAI " "$root/run/spell-audit.log" 2>/dev/null
} | awk '!seen[$0]++' > logs/aoeai.log 2>/dev/null || true
echo "  + logs/aoeai.log ($(wc -l < logs/aoeai.log 2>/dev/null || echo 0) строк)"

# --- README -----------------------------------------------------------------
{
    echo "# Аудит умений (все школы заклинаний)"
    echo
    echo "$title"
    echo
    echo "Обновлено: $(date -u '+%Y-%m-%d %H:%M UTC') · коммит \`${GITHUB_SHA:-local}\` · [прогон](${RUN_URL:-—})"
    echo
    echo "Ветку перезаписывает workflow \`.github/workflows/spell-audit.yml\` при каждом прогоне — здесь всегда последний результат."
    echo
    if [ -s logs/aoeai.log ]; then
        summary="$(grep -h "AOEAI done" logs/aoeai.log | tail -1)"
        [ -n "$summary" ] && { echo "**Итог:** \`$summary\`"; echo; }
        failed="$(grep -h "AOEAI spell=" logs/aoeai.log | grep -c "verdict=FAIL" || true)"
        total="$(grep -h "AOEAI spell=" logs/aoeai.log | grep -c "verdict=" || true)"
        echo "Умений проверено: **$total**, провалилось: **$failed**."
        echo
    else
        echo "> **Строк AOEAI нет** — клиент не дошёл до аудита. Причина в \`logs/steps.log\`, \`logs/gradle.log\` и \`logs/latest.log\`."
        echo
    fi

    if [ "$video_count" -gt 0 ]; then
        echo "## Видео по классам"
        echo
        echo "В кадре подпись: название умения, его описание из тултипа и живые цифры (урон, попадания, лечение, эффекты, итог)."
        echo
        for f in videos/*.mp4; do
            [ -e "$f" ] || continue
            base="$(basename "$f" .mp4)"
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

    if [ -f reports/spell-audit.md ]; then
        echo "## Живой аудит (замеры в игре)"
        echo
        # Заголовок отчёта уже содержит сводку — берём его целиком.
        sed -n '1,8p' reports/spell-audit.md
        echo
        echo "Полная таблица: [\`reports/spell-audit.md\`](reports/spell-audit.md)"
        echo
    fi
    if [ -f reports/spell-audit-structural.md ]; then
        echo "## Структурный аудит (без запуска игры)"
        echo
        sed -n '1,12p' reports/spell-audit-structural.md
        echo
        echo "Полностью: [\`reports/spell-audit-structural.md\`](reports/spell-audit-structural.md)"
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
git commit -q -m "spell audit: $title" || true
git push -q --force origin "$branch"
echo "Готово: https://github.com/${repo}/tree/${branch}"
