#!/usr/bin/env bash
#
# Публикует скриншоты в отдельную ветку репозитория, чтобы их было видно прямо
# в GitHub: артефакты Actions лежат в blob-хранилище за пределами github.com
# (и, например, из песочницы разработки недоступны), а ветка доступна всем.
#
# В ветку кладётся только последний прогон (force-push), поэтому она не растёт.
#
# Запуск (в CI, где есть GH_TOKEN и репозиторий):
#   REPO=owner/repo BRANCH=ci-screenshots GH_TOKEN=... \
#     bash tools/e2e/publish-shots.sh run/e2e/shots "сценарий: boot"
#
# В README.md ветки получается галерея, которую GitHub рендерит сам.

set -eu

src="${1:-run/e2e/shots}"
title="${2:-прогон без описания}"
repo="${REPO:?REPO не задан}"
branch="${BRANCH:-ci-screenshots}"
token="${GH_TOKEN:?GH_TOKEN не задан}"

if [ ! -d "$src" ]; then
    echo "Нет каталога со снимками: $src"
    exit 1
fi

shots="$(find "$src" -maxdepth 1 -name '*.png' | sort)"
count="$(printf '%s\n' "$shots" | grep -c . || true)"
if [ "$count" -eq 0 ]; then
    echo "Снимков нет — публиковать нечего."
    exit 1
fi
echo "Публикую $count снимков в ветку $branch"

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
cd "$work"
git init -q -b "$branch"
git config user.name 'github-actions[bot]'
git config user.email 'github-actions[bot]@users.noreply.github.com'
git remote add origin "https://x-access-token:${token}@github.com/${repo}.git"

mkdir -p shots
cp $shots shots/

{
    echo "# Скриншоты dev-клиента"
    echo
    echo "$title"
    echo
    echo "Обновлено: $(date -u '+%Y-%m-%d %H:%M UTC') · коммит \`${GITHUB_SHA:-local}\` · [прогон](${RUN_URL:-—})"
    echo
    echo "Ветку перезаписывает workflow \`.github/workflows/screenshots.yml\` при каждом прогоне — здесь всегда последний результат."
    echo
    for f in shots/*.png; do
        base="$(basename "$f" .png)"
        echo "## ${base}"
        echo
        echo "![${base}](${f})"
        echo
    done
} > README.md

if [ -f "$src/steps.log" ]; then
    cp "$src/steps.log" steps.log
fi
if [ -f "$src/index.txt" ]; then
    cp "$src/index.txt" index.txt
fi

git add -A
git commit -q -m "screenshots: ${title}"
git push -q --force origin "$branch"
echo "Готово: https://github.com/${repo}/tree/${branch}"
