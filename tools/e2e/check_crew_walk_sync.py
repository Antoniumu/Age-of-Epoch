#!/usr/bin/env python3
"""Verify that the client observed the tested actor move and settle at the server endpoint.

Only validates the first dev-only loop in one integrated-client run. It is not a
multiplayer, animation, collision, persistence, or production behaviour test.
"""
import math
import re
import sys
from pathlib import Path

START = re.compile(r'AIRSHIPWALK START npc=veya uuid=([\w-]+).*?position=\(([^)]+)\)')
COMPLETE = re.compile(r'AIRSHIPWALK COMPLETE npc=veya .*?end=\(([^)]+)\)')
CLIENT = re.compile(r'AIRSHIPWALK CLIENT uuid=([\w-]+).*?pos=\(([^)]+)\)')


def coords(text):
    values = tuple(float(v.strip()) for v in text.split(','))
    if len(values) != 3 or not all(math.isfinite(v) for v in values):
        raise ValueError('invalid position')
    return values


def validate(lines):
    actor = None
    start = None
    endpoint = None
    moving = []
    settled = []
    for line in lines:
        match = START.search(line)
        if match:
            if actor is not None:
                break  # independent blocker attempt
            actor, start = match.group(1), coords(match.group(2))
            continue
        match = COMPLETE.search(line)
        if match and actor is not None:
            endpoint = coords(match.group(1))
            continue
        match = CLIENT.search(line)
        if match and match.group(1) == actor:
            (settled if endpoint else moving).append(coords(match.group(2)))
    if not actor or endpoint is None or not moving or not settled:
        raise ValueError('missing first-loop server or client samples')
    if max(math.dist(start, p) for p in moving) < 1.0:
        raise ValueError('client did not observe at least one block of movement')
    if math.dist(endpoint, settled[-1]) > 0.15:
        raise ValueError('client did not settle near the server endpoint')
    return actor, len(moving), len(settled)


def main():
    if len(sys.argv) != 2:
        raise SystemExit('usage: check_crew_walk_sync.py path/to/latest.log')
    try:
        actor, moving, settled = validate(Path(sys.argv[1]).read_text(encoding='utf-8').splitlines())
    except (OSError, ValueError) as exc:
        raise SystemExit('crew client sync FAIL: ' + str(exc))
    print(f'crew client sync PASS: actor={actor}, movement_samples={moving}, settled_samples={settled}')


if __name__ == '__main__':
    main()
