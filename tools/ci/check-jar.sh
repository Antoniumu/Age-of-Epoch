#!/usr/bin/env bash
#
# Проверка собранного jar: он должен быть пригоден для установки, а не просто
# «скомпилировался». Ловит типовые поломки упаковки, которые `gradlew build`
# не считает ошибкой:
#   * нет fabric.mod.json или в нём остался плейсхолдер ${version};
#   * нет mixin-конфига, на который ссылается fabric.mod.json;
#   * не попали assets/ и data/ (мод запустится, но без контента);
#   * вместо мода в libs лежит пустой dev-jar.
#
# Запуск:  bash tools/ci/check-jar.sh [каталог_с_jars]     (по умолчанию build/libs)
# Выход:   0 — jar годный; 1 — нет.

set -u
cd "$(dirname "$0")/../.." || exit 1

dir="${1:-build/libs}"
status=0
fail() { echo "  FAIL: $*"; status=1; }

# Основной jar: без -sources, без -dev.
jar="$(ls -1 "$dir"/*.jar 2>/dev/null | grep -v -- '-sources' | grep -v -- '-dev' | head -1)"
if [ -z "$jar" ]; then
    echo "Нет собранного jar в $dir"
    ls -la "$dir" 2>/dev/null
    exit 1
fi

size=$(wc -c < "$jar")
echo "Проверяю $jar ($((size / 1024 / 1024)) MB)"

if [ "$size" -lt 100000 ]; then
    fail "jar меньше 100 КБ — это пустая сборка, а не мод"
fi

entries="$(unzip -Z1 "$jar" 2>/dev/null)"
if [ -z "$entries" ]; then
    fail "архив не читается (unzip -Z1 вернул пусто)"
    echo; exit 1
fi

# --- fabric.mod.json ---------------------------------------------------------
if ! printf '%s\n' "$entries" | grep -qx 'fabric.mod.json'; then
    fail "в jar нет fabric.mod.json — мод не загрузится"
else
    fmj="$(unzip -p "$jar" fabric.mod.json)"
    mod_id="$(printf '%s' "$fmj" | grep -o '"id"[[:space:]]*:[[:space:]]*"[^"]*"' | head -1 | sed 's/.*"\([^"]*\)"$/\1/')"
    echo "  mod id: ${mod_id:-<не найден>}"
    [ -n "$mod_id" ] || fail "в fabric.mod.json нет поля id"

    if printf '%s' "$fmj" | grep -q '\${'; then
        fail "в fabric.mod.json остался неподставленный плейсхолдер (processResources не отработал)"
    fi

    # mixin-конфиги, объявленные в fabric.mod.json, должны лежать в jar
    mixins="$(printf '%s' "$fmj" | grep -o '"[a-z0-9_./-]*\.mixins\.json"' | tr -d '"' | sort -u)"
    for m in $mixins; do
        if printf '%s\n' "$entries" | grep -qx "$m"; then
            echo "  mixin: $m — на месте"
        else
            fail "fabric.mod.json ссылается на $m, но в jar этого файла нет"
        fi
    done
fi

# --- контент -----------------------------------------------------------------
count() { printf '%s\n' "$entries" | grep -c "$1" || true; }

assets=$(count '^assets/')
data=$(count '^data/')
classes=$(count '\.class$')
echo "  classes: $classes, assets: $assets, data: $data"

[ "$classes" -gt 100 ] || fail "меньше 100 классов — похоже, код не попал в jar"
[ "$assets" -gt 100 ]  || fail "меньше 100 файлов в assets/ — ресурсы не попали в jar"
[ "$data" -gt 10 ]     || fail "меньше 10 файлов в data/ — датапак мода не попал в jar"

# --- sources jar (не обязателен для игры, но им пользуются в IDE) -----------
src="$(ls -1 "$dir"/*-sources.jar 2>/dev/null | head -1)"
if [ -n "$src" ]; then
    echo "  sources: $(basename "$src") ($(($(wc -c < "$src") / 1024 / 1024)) MB)"
fi

echo
if [ "$status" -eq 0 ]; then
    echo "OK: $jar готов к установке в mods/"
else
    echo "JAR НЕ ГОДИТСЯ: см. FAIL выше."
fi
exit "$status"
