"""Generates every themed tower structure, template pool, structure JSON and
structure set.

Usage (from the repo root):

    python3 tools/tower_gen/generate.py

The output is deterministic: rerunning the script rewrites the same bytes, so
the generated data can be committed and diffed.
"""

from __future__ import annotations

import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from build_towers import (  # noqa: E402
    START_H,
    TOP_H,
    MID_H,
    build_middle,
    build_start,
    build_top,
)
from themes import THEMES  # noqa: E402

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
DATA = os.path.join(ROOT, "src", "main", "resources", "data", "world_of_exile")
STRUCTURE_DIR = os.path.join(DATA, "structure", "towers")
POOL_DIR = os.path.join(DATA, "worldgen", "template_pool", "towers")
STRUCTURE_JSON_DIR = os.path.join(DATA, "worldgen", "structure")
SET_DIR = os.path.join(DATA, "worldgen", "structure_set")

PROCESSOR = "world_of_exile:tower_processor"

# 1 = small keep, 2 = tall tower, 3 = colossal spire.
# `size` is the jigsaw recursion depth: start + (size-2) middle pieces + top.
TIERS = (
    dict(name="t1", size=4, weight=6),
    dict(name="t2", size=6, weight=3),
    dict(name="t3", size=8, weight=2),
)


def _write_json(path, payload):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as handle:
        json.dump(payload, handle, indent=2, ensure_ascii=False)
        handle.write("\n")


def _pool_element(location):
    return {
        "weight": 1,
        "element": {
            "location": location,
            "processors": PROCESSOR,
            "projection": "rigid",
            "element_type": "minecraft:single_pool_element",
        },
    }


def _salt(theme):
    value = 2166136261
    for ch in theme["id"]:
        value = ((value ^ ord(ch)) * 16777619) & 0xFFFFFFFF
    return 100000 + (value % 800000)


def emit_theme(theme):
    tid = theme["id"]
    piece_dir = os.path.join(STRUCTURE_DIR, tid)
    os.makedirs(piece_dir, exist_ok=True)

    pieces = {
        "start0": (build_start(theme), START_H),
        "middle0": (build_middle(theme, 0), MID_H),
        "middle1": (build_middle(theme, 1), MID_H),
        "middle2": (build_middle(theme, 2), MID_H),
        "top0": (build_top(theme, 0), TOP_H),
        "top1": (build_top(theme, 1), TOP_H),
    }
    for name, (structure, _height) in pieces.items():
        structure.save(os.path.join(piece_dir, f"{name}.nbt"))

    base = f"world_of_exile:towers/{tid}"
    pool_dir = os.path.join(POOL_DIR, tid)
    _write_json(
        os.path.join(pool_dir, "start.json"),
        {
            "fallback": f"{base}/middle",
            "elements": [_pool_element(f"{base}/start0")],
        },
    )
    middle_elements = [_pool_element(f"{base}/middle{i}") for i in range(3)]
    for element, weight in zip(middle_elements, (3, 2, 2)):
        element["weight"] = weight
    _write_json(
        os.path.join(pool_dir, "middle.json"),
        {
            "fallback": f"{base}/top",
            "elements": middle_elements,
        },
    )
    _write_json(
        os.path.join(pool_dir, "top.json"),
        {
            "fallback": "minecraft:empty",
            "elements": [
                _pool_element(f"{base}/top0"),
                _pool_element(f"{base}/top1"),
            ],
        },
    )

    nether = bool(theme.get("nether"))
    for tier, entry in enumerate(TIERS, start=1):
        structure_id = f"world_of_exile:{tid}_{entry['name']}"
        payload = {
            "type": "minecraft:jigsaw",
            "biomes": theme["biomes"],
            "step": "surface_structures",
            "spawn_overrides": {},
            "terrain_adaptation": "none" if nether else "beard_thin",
            "start_pool": f"{base}/start",
            "size": entry["size"],
            "start_height": {"absolute": 36} if nether else {"absolute": 0},
            "use_expansion_hack": False,
            "max_distance_from_center": 80,
        }
        if not nether:
            payload["project_start_to_heightmap"] = "WORLD_SURFACE_WG"
        _write_json(os.path.join(STRUCTURE_JSON_DIR, f"{tid}_{entry['name']}.json"), payload)

    if nether:
        spacing, separation = 14, 5
    elif theme["rank"] >= 3:
        spacing, separation = 26, 9
    else:
        spacing, separation = 22, 7

    _write_json(
        os.path.join(SET_DIR, f"towers_{tid}.json"),
        {
            "structures": [
                {
                    "structure": f"world_of_exile:{tid}_{entry['name']}",
                    "weight": entry["weight"],
                }
                for entry in TIERS
            ],
            "placement": {
                "type": "minecraft:random_spread",
                "spacing": spacing,
                "separation": separation,
                "salt": _salt(theme),
            },
        },
    )
    return len(pieces)


def main():
    total = 0
    for theme in THEMES:
        total += emit_theme(theme)
    print(f"generated {total} tower pieces for {len(THEMES)} themes")


if __name__ == "__main__":
    main()
