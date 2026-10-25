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

shots="$(find "$src" -maxdepth 1 -name '*.png' 2>/dev/null | sort)"
count="$(printf '%s' "$shots" | grep -c . || true)"
echo "Снимков: $count"

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
    fi
}
copy_tail run/logs/latest.log latest.log 300
copy_tail run/e2e/gradle.log gradle.log 400
copy_tail run/e2e/xvfb.log xvfb.log 50
copy_tail run/e2e/xdpyinfo.txt xdpyinfo.txt 40
copy_tail "$src/steps.log" steps.log 400
[ -f "$src/index.txt" ] && cp "$src/index.txt" index.txt

if [ -d run/crash-reports ] && [ -n "$(ls -A run/crash-reports 2>/dev/null)" ]; then
    mkdir -p crash-reports
    cp run/crash-reports/* crash-reports/ 2>/dev/null || true
fi

grep -E "Sound engine started|Backend library|OpenAL|Setting user|LWJGL|Exception|ERROR|FATAL" \
    run/logs/latest.log 2>/dev/null | tail -n 40 > logs/key-lines.log || true

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
