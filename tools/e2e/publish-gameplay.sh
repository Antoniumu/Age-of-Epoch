#!/usr/bin/env bash
#
# Публикует результат полного цикла (прокачка + GUI-клики + видео умений) в
# отдельную ветку репозитория: ролики открываются и играются прямо в GitHub,
# снимки собираются в галерею README.
#
# Зачем отдельная ветка: артефакты Actions не открываются по прямой ссылке,
# а mp4/png в ветке видны сразу. Ветку перезаписывает force-push — хранится
# только последний прогон.
#
# Запуск (в CI):
#   REPO=owner/repo BRANCH=ci-gameplay GH_TOKEN=... \
#     bash tools/e2e/publish-gameplay.sh run/e2e
#
# Ожидаемые входы (что есть — то и уедет; скрипт не падает на отсутствующем):
#   run/e2e/videos-progression/*.mp4|.png|README.md  — ролики этапов прокачки
#   run/e2e/videos/*.mp4|.png|README.md              — ролики умений по классам
#   run/e2e/gui-video.mp4                            — видео кликов в GUI
#   run/progression-report.md/.json, run/progression-timeline.txt
#   run/spell-audit.md/.json, run/spell-audit-timeline.txt
#   run/e2e/aoeprog.log, run/e2e/aoegui.log, run/e2e/aoeai.log
#   run/e2e/shots/<сценарий>/*.png + steps.log       — снимки и журналы шагов

set -u

src="${1:-run/e2e}"
repo="${REPO:?REPO не задан}"
branch="${BRANCH:-ci-gameplay}"
token="${GH_TOKEN:?GH_TOKEN не задан}"

root="$(cd "$(dirname "$0")/../.." && pwd)"
src="$(cd "$src" 2>/dev/null && pwd || echo "$root/run/e2e")"

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
cd "$work" || exit 1
git init -q -b "$branch"
git config user.name 'github-actions[bot]'
git config user.email 'github-actions[bot]@users.noreply.github.com'
git remote add origin "https://x-access-token:${token}@github.com/${repo}.git"

mkdir -p videos/progression videos/spells videos/gui reports shots logs

# --- видео ------------------------------------------------------------------
if [ -d "$src/videos-progression" ]; then
    cp "$src"/videos-progression/*.mp4 "videos/progression/" 2>/dev/null || true
    cp "$src"/videos-progression/*.png "videos/progression/" 2>/dev/null || true
fi
if [ -d "$src/videos" ]; then
    cp "$src"/videos/*.mp4 "videos/spells/" 2>/dev/null || true
    cp "$src"/videos/*.png "videos/spells/" 2>/dev/null || true
fi
[ -f "$src/gui-video.mp4" ] && cp "$src/gui-video.mp4" videos/gui/gui-video.mp4 && echo "  + видео GUI"

prog_count="$(ls videos/progression/*.mp4 2>/dev/null | wc -l | tr -d ' ')"
spell_count="$(ls videos/spells/*.mp4 2>/dev/null | wc -l | tr -d ' ')"
gui_count="$(ls videos/gui/*.mp4 2>/dev/null | wc -l | tr -d ' ')"
echo "  видео: прокачка $prog_count, умения $spell_count, GUI $gui_count"

# --- отчёты ------------------------------------------------------------------
for name in progression-report.md progression-report.json progression-timeline.txt \
            spell-audit.md spell-audit.json spell-audit-timeline.txt; do
    [ -f "$root/run/$name" ] && cp "$root/run/$name" "reports/$name" && echo "  + reports/$name"
done

# --- снимки (плоско, с префиксом сценария) ------------------------------------
for dir in "$src"/shots/*/; do
    [ -d "$dir" ] || continue
    name="$(basename "$dir")"
    for f in "$dir"/*.png; do
        [ -e "$f" ] || continue
        cp "$f" "shots/${name}-$(basename "$f")"
    done
    [ -f "$dir/steps.log" ] && cp "$dir/steps.log" "logs/steps-${name}.log"
    # gradle.log — причина «клиент не запустился»: публикуем рядом с журналом шагов.
    [ -f "$dir/gradle.log" ] && cp "$dir/gradle.log" "logs/gradle-${name}.log"
done
shot_count="$(ls shots/*.png 2>/dev/null | wc -l | tr -d ' ')"
echo "  снимков: $shot_count"

# --- журналы ------------------------------------------------------------------
# Отладка кликов клиента ([AOE-CLICK] печатают экраны): снимается в GUI-шаге,
# потому что к моменту публикации latest.log уже от spells-сценария.
[ -f "$root/run/e2e/aoeclick.log" ] && cp "$root/run/e2e/aoeclick.log" logs/aoeclick.log

for f in aoeprog.log aoegui.log aoeai.log; do
    [ -f "$src/$f" ] && cp "$src/$f" "logs/$f" && echo "  + logs/$f"
done
[ -f "$root/run/logs/latest.log" ] && tail -n 400 "$root/run/logs/latest.log" > logs/latest.log
[ -f "$src/ffmpeg.log" ] && tail -n 80 "$src/ffmpeg.log" > logs/ffmpeg.log

# --- README -------------------------------------------------------------------
{
    echo "# Полный цикл: прокачка, GUI и умения — видео и снимки"
    echo
    echo "Обновлено: $(date -u '+%Y-%m-%d %H:%M UTC') · коммит \`${GITHUB_SHA:-local}\` · [прогон](${RUN_URL:-—})"
    echo
    echo "Ветку перезаписывает workflow \`.github/workflows/gameplay-e2e.yml\` — здесь всегда последний результат."
    echo

    echo "## 1. Прокачка (видео по этапам)"
    echo
    if [ -s logs/aoeprog.log ]; then
        summary="$(grep -h "AOEPROG done" logs/aoeprog.log | tail -1)"
        [ -n "$summary" ] && { echo "**Итог:** \`$summary\`"; echo; }
    fi
    if [ "$prog_count" -gt 0 ]; then
        echo "Каждый ролик — этап: набор опыта (в кадре «+N опыта» и level-up), вложение очков, выбор пути Вознесения, цепочка умений, хотбар."
        echo
        for f in videos/progression/*.mp4; do
            [ -e "$f" ] || continue
            base="$(basename "$f" .mp4)"
            echo "* [▶ $base]($f)$([ -f "videos/progression/${base}.png" ] && echo " · [постер](videos/progression/${base}.png)")"
        done
        echo
    else
        echo "_Ролики этапов не собрались — смотри \`logs/\`._"
        echo
    fi
    [ -f reports/progression-report.md ] && echo "Отчёт: [\`reports/progression-report.md\`](reports/progression-report.md)" && echo

    echo "## 2. Клики в GUI (видео)"
    echo
    echo "Сценарий кликает настоящей мышью по реальным координатам виджетов (дамп клиента),"
    echo "а сервер подтверждает каждый пакет строкой \`AOEGUI packet=... ok=...\`."
    echo
    if [ -s logs/aoegui.log ]; then
        echo '```'
        cat logs/aoegui.log
        echo '```'
        echo
    fi
    if [ "$gui_count" -gt 0 ]; then
        echo "[▶ Смотреть gui-video.mp4](videos/gui/gui-video.mp4)"
        echo
    fi

    echo "## 3. Все умения (видео по классам)"
    echo
    if [ -s logs/aoeai.log ]; then
        summary="$(grep -h "AOEAI done" logs/aoeai.log | tail -1)"
        [ -n "$summary" ] && { echo "**Итог:** \`$summary\`"; echo; }
    fi
    if [ "$spell_count" -gt 0 ]; then
        for f in videos/spells/*.mp4; do
            [ -e "$f" ] || continue
            base="$(basename "$f" .mp4)"
            echo "* [▶ $base]($f)$([ -f "videos/spells/${base}.png" ] && echo " · [постер](videos/spells/${base}.png)")"
        done
        echo
    else
        echo "_Ролики классов не собрались._"
        echo
    fi
    [ -f reports/spell-audit.md ] && echo "Отчёт: [\`reports/spell-audit.md\`](reports/spell-audit.md)" && echo

    if [ "$shot_count" -gt 0 ]; then
        echo "## Снимки ($shot_count)"
        echo
        for f in shots/*.png; do
            [ -e "$f" ] || continue
            base="$(basename "$f" .png)"
            echo "**$base**"
            echo
            echo "[![$base]($f)]($f)"
            echo
        done
    fi
} > README.md

git add -A
git commit -q -m "gameplay-e2e: $(date -u '+%Y-%m-%d %H:%M UTC') ${GITHUB_SHA:-local}" || {
    echo "нечего публиковать (нет изменений)"
    exit 0
}
git push -q -f origin "$branch"
echo "опубликовано: https://github.com/${repo}/tree/${branch}"
