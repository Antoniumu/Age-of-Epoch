#!/usr/bin/env bash
#
# Публикует результат прогона (скриншоты + журнал + хвосты логов) в отдельную
# ветку репозитория, чтобы это было видно прямо в GitHub.
#
# Почему ветка, а не только артефакт: артефакты Actions лежат в blob-хранилище
# за пределами github.com — их не открыть из браузера по прямой ссылке и не
# прочитать инструментам. Ветка доступна всем и рендерится в README.md.
#
# Ветку перезаписывает force-push: хранится только последний прогон.
#
# Запуск (в CI, где есть GH_TOKEN):
#   REPO=owner/repo BRANCH=ci-screenshots GH_TOKEN=... \
#     bash tools/e2e/publish-results.sh run/e2e/shots "описание"
#
# Никогда не падает молча: если снимков нет, всё равно публикует журнал и логи —
# именно по ним видно, на каком шаге клиент не поднялся.

set -u

src="${1:-run/e2e/shots}"
title="${2:-прогон без описания}"
repo="${REPO:?REPO не задан}"
branch="${BRANCH:-ci-screenshots}"
token="${GH_TOKEN:?GH_TOKEN не задан}"

mkdir -p "$src"

# Все пути делаем абсолютными ДО перехода в каталог публикации — иначе копирование
# молча не находит файлы (первый прогон именно так и потерял снимки).
root="$(cd "$(dirname "$0")/../.." && pwd)"
src="$(cd "$src" && pwd)"
shots="$(find "$src" -maxdepth 1 -name '*.png' 2>/dev/null | sort)"
count="$(printf '%s' "$shots" | grep -c . || true)"
echo "Снимков: $count (из $src)"

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
cd "$work" || exit 1
git init -q -b "$branch"
git config user.name 'github-actions[bot]'
git config user.email 'github-actions[bot]@users.noreply.github.com'
git remote add origin "https://x-access-token:${token}@github.com/${repo}.git"

mkdir -p shots logs
if [ "$count" -gt 0 ]; then
    # shellcheck disable=SC2086
    cp $shots shots/
fi

# --- диагностика: то, что обычно нужно, когда кадров нет -------------------
copy_tail() {
    local from="$1" to="$2" lines="${3:-250}"
    if [ -f "$from" ]; then
        tail -n "$lines" "$from" > "logs/$to"
        echo "  + logs/$to ($(wc -l < "logs/$to") строк)"
    else
        echo "  - нет файла $from" >> logs/missing.log
    fi
}
copy_tail "$root/run/logs/latest.log" latest.log 400
copy_tail "$root/run/e2e/gradle.log" gradle.log 500
copy_tail "$root/run/e2e/xvfb.log" xvfb.log 50
copy_tail "$root/run/e2e/xdpyinfo.txt" xdpyinfo.txt 40
copy_tail "$root/run/e2e/graphics.txt" graphics.txt 80
for f in "$src"/steps*.log; do
    [ -e "$f" ] || continue
    tail -n 400 "$f" > "logs/$(basename "$f")"
    echo "  + logs/$(basename "$f")"
done
# стеки JVM (если снимались) — по ним видно, где стоит render thread
for f in "$src"/jstack-*.txt; do
    [ -e "$f" ] || continue
    cp "$f" "logs/$(basename "$f")"
done
[ -f "$src/index.txt" ] && cp "$src/index.txt" index.txt

if [ -d "$root/run/crash-reports" ] && [ -n "$(ls -A "$root/run/crash-reports" 2>/dev/null)" ]; then
    mkdir -p crash-reports
    cp "$root"/run/crash-reports/* crash-reports/ 2>/dev/null || true
fi

grep -E "Sound engine started|Backend library|OpenAL|Setting user|LWJGL|Exception|ERROR|FATAL|Stopping" \
    "$root/run/logs/latest.log" 2>/dev/null | tail -n 60 > logs/key-lines.log || true

{
    echo "# Скриншоты dev-клиента"
    echo
    echo "$title"
    echo
    echo "Обновлено: $(date -u '+%Y-%m-%d %H:%M UTC') · коммит \`${GITHUB_SHA:-local}\` · [прогон](${RUN_URL:-—})"
    echo
    if [ "$count" -gt 0 ]; then
        echo "Снимков: **$count**."
    else
        echo "> **Снимков нет.** Клиент не дошёл до состояния, в котором делается кадр."
        echo "> Причина — в \`logs/steps.log\` (журнал шагов) и \`logs/gradle.log\`."
    fi
    echo
    echo "Ветку перезаписывает workflow \`.github/workflows/screenshots.yml\` при каждом прогоне — здесь всегда последний результат."
    echo
    for f in shots/*.png; do
        [ -e "$f" ] || continue
        base="$(basename "$f" .png)"
        echo "## ${base}"
        echo
        echo "![${base}](${f})"
        echo
    done
    echo "## Диагностика"
    echo
    for f in logs/*; do
        [ -e "$f" ] || continue
        echo "* [\`$f\`]($f)"
    done
} > README.md

git add -A
git commit -q -m "screenshots: $title" || true
git push -q --force origin "$branch"
echo "Готово: https://github.com/${repo}/tree/${branch}"
