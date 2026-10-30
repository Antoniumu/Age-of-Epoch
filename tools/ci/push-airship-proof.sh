#!/usr/bin/env bash
# CI proof jobs publish distinct files but may finish concurrently. Never force-push:
# replay the proof commit on the latest branch; a real conflict fails visibly.
set -euo pipefail
branch='arena/7bde44a9-age-of-epoch'
for attempt in 1 2 3 4; do
    if git push origin "HEAD:$branch"; then
        exit 0
    fi
    if [ "$attempt" -eq 4 ]; then
        echo 'Proof publication failed after four attempts' >&2
        exit 1
    fi
    echo "Proof branch advanced; rebasing before retry $((attempt + 1))" >&2
    git pull --rebase --autostash origin "$branch"
done
