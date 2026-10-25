#!/usr/bin/env bash
#
# Публикует в PR один «липкий» комментарий с итогами прогона CI и обновляет его
# при следующих прогонах (ищет по скрытому маркеру), а не плодит новые.
#
# Зачем: job summary видна только в веб-интерфейсе Actions, а логи артефактов
# живут 30 дней. Комментарий в PR читается и человеком, и через `gh api`.
#
# Использование (из ci-ok, только для pull_request):
#   REPO=owner/repo PR_NUMBER=105 bash tools/ci/pr-report.sh report.md
#
# Требует: gh с правом pull-requests: write.

set -u

marker='<!-- aoe-ci-report -->'
repo="${REPO:?REPO не задан (owner/repo)}"
pr="${PR_NUMBER:?PR_NUMBER не задан}"
file="${1:?нужен путь к markdown-файлу с текстом отчёта}"

body_file="$(mktemp)"
trap 'rm -f "$body_file"' EXIT
{ printf '%s\n' "$marker"; cat "$file"; } > "$body_file"

# Ищем уже существующий комментарий с маркером.
existing_id="$(
    gh api "repos/$repo/issues/$pr/comments" --paginate 2>/dev/null \
    | MARKER="$marker" python3 -c '
import json, os, sys
marker = os.environ["MARKER"]
try:
    data = json.load(sys.stdin)
except Exception:
    sys.exit(0)
for c in data:
    if marker in (c.get("body") or ""):
        print(c["id"])
        break
'
)"

if [ -n "${existing_id:-}" ]; then
    gh api -X PATCH "repos/$repo/issues/comments/$existing_id" \
        -F "body=@$body_file" --jq '"обновлён комментарий: " + .html_url'
else
    gh api -X POST "repos/$repo/issues/$pr/comments" \
        -F "body=@$body_file" --jq '"создан комментарий: " + .html_url'
fi
