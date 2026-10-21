"""Static validation for the generated tower data.

Checks:
  * every generated NBT parses back and every block is inside the bounds;
  * the ladder column is continuous in every piece (towers stay climbable);
  * every jigsaw pool/target points at something that exists;
  * every structure JSON has a matching template pool + pieces;
  * biome ids referenced by structures are from a known vanilla list;
  * every tag sign uses a marker the structure processor understands.

Run: python3 tools/tower_gen/validate.py
"""

from __future__ import annotations

import glob
import json
import os
import re
import sys

import nbtlib

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
RES = os.path.join(ROOT, "src", "main", "resources")
DATA = os.path.join(RES, "data", "world_of_exile")

KNOWN_BIOMES = {
    "minecraft:plains",
    "minecraft:sunflower_plains",
    "minecraft:meadow",
    "minecraft:forest",
    "minecraft:birch_forest",
    "minecraft:old_growth_birch_forest",
    "minecraft:flower_forest",
    "minecraft:windswept_forest",
    "minecraft:snowy_plains",
    "minecraft:ice_spikes",
    "minecraft:snowy_taiga",
    "minecraft:snowy_beach",
    "minecraft:grove",
    "minecraft:snowy_slopes",
    "minecraft:frozen_peaks",
    "minecraft:jagged_peaks",
    "minecraft:desert",
    "minecraft:jungle",
    "minecraft:sparse_jungle",
    "minecraft:bamboo_jungle",
    "minecraft:swamp",
    "minecraft:mangrove_swamp",
    "minecraft:badlands",
    "minecraft:eroded_badlands",
    "minecraft:wooded_badlands",
    "minecraft:savanna",
    "minecraft:savanna_plateau",
    "minecraft:windswept_savanna",
    "minecraft:windswept_hills",
    "minecraft:windswept_gravelly_hills",
    "minecraft:stony_peaks",
    "minecraft:beach",
    "minecraft:stony_shore",
    "minecraft:mushroom_fields",
    "minecraft:dark_forest",
    "minecraft:pale_garden",
    "minecraft:nether_wastes",
    "minecraft:soul_sand_valley",
    "minecraft:crimson_forest",
    "minecraft:warped_forest",
    "minecraft:basalt_deltas",
}

MARKERS = {"[boss]", "[spawner]", "[treasure]", "[final_treasure]", "[lock_treasure]"}
MARKER_RE = re.compile(r"\[(boss|spawner|treasure|final_treasure|lock_treasure)[^\]]*\]")

errors = []


def fail(message):
    errors.append(message)


def check_nbt(path):
    try:
        root = nbtlib.load(path)
    except Exception as exc:  # noqa: BLE001
        fail(f"{path}: cannot parse nbt ({exc})")
        return
    size = [int(v) for v in root["size"]]
    palette = root["palette"]
    blocks = root["blocks"]
    positions = set()
    for block in blocks:
        x, y, z = [int(v) for v in block["pos"]]
        if not (0 <= x < size[0] and 0 <= y < size[1] and 0 <= z < size[2]):
            fail(f"{path}: block {(x, y, z)} outside {size}")
        positions.add((x, y, z))
        state = int(block["state"])
        if state < 0 or state >= len(palette):
            fail(f"{path}: palette index {state} out of range")
        name = str(palette[state]["Name"])
        if name in ("minecraft:air", "minecraft:cave_air"):
            fail(f"{path}: air is written explicitly at {(x, y, z)} (wastes space)")

    names = {}
    for block in blocks:
        name = str(palette[int(block["state"])]["Name"])
        names.setdefault(name, []).append(block)

    # four rotation-invariant ladder columns
    ladders = names.get("minecraft:ladder", [])
    for (lx, lz) in ((1, 8), (15, 8), (8, 1), (8, 15)):
        ys = sorted(int(b["pos"][1]) for b in ladders
                    if int(b["pos"][0]) == lx and int(b["pos"][2]) == lz)
        if not ys:
            fail(f"{path}: no ladder column at {(lx, lz)}")
            continue
        expected = list(range(ys[0], ys[-1] + 1))
        if ys != expected:
            fail(f"{path}: ladder column at {(lx, lz)} has gaps: {ys}")
        supports = {(1, 8): (0, 8), (15, 8): (16, 8), (8, 1): (8, 0), (8, 15): (8, 16)}
        sx, sz = supports[(lx, lz)]
        for y in ys:
            if (sx, y, sz) not in positions:
                fail(f"{path}: ladder at {(lx, y, lz)} has no support at {(sx, y, sz)}")
                break

    jigsaws = names.get("minecraft:jigsaw", [])
    for block in jigsaws:
        tag = block.get("nbt")
        if tag is None:
            fail(f"{path}: jigsaw without nbt")
            continue
        if str(tag.get("target")) != "world_of_exile:connect":
            fail(f"{path}: jigsaw target {tag.get('target')}")
        pool = str(tag.get("pool"))
        if pool != "minecraft:empty" and not os.path.exists(
            os.path.join(DATA, "worldgen", "template_pool", f"{pool.split(':')[1]}.json")
        ):
            fail(f"{path}: jigsaw points at missing pool {pool}")

    markers = []
    for block in names.get("minecraft:oak_sign", []):
        raw = str(block.get("nbt", ""))
        match = MARKER_RE.search(raw)
        if not match:
            fail(f"{path}: sign at {[int(v) for v in block['pos']]} carries no marker tag")
            continue
        marker = match.group(0).split("]")[0] + "]"
        markers.append(marker)
        if marker == "[spawner]" and "[tower:" not in raw:
            fail(f"{path}: spawner sign has no theme tag")
        if marker in ("[treasure]", "[final_treasure]") and "[loot#" not in raw:
            fail(f"{path}: treasure sign has no loot table")
    if "[boss]" not in markers and "top" in path:
        fail(f"{path}: top piece without a [boss] marker")
    if "[spawner]" not in markers:
        fail(f"{path}: piece without a [spawner] marker")


def check_json():
    pool_dir = os.path.join(DATA, "worldgen", "template_pool")
    structure_dir = os.path.join(DATA, "worldgen", "structure")
    set_dir = os.path.join(DATA, "worldgen", "structure_set")

    def template_exists(location):
        name = location.split(":", 1)[1]
        return os.path.exists(os.path.join(DATA, "structure", f"{name}.nbt"))

    for path in glob.glob(os.path.join(pool_dir, "towers", "*", "*.json")):
        with open(path, encoding="utf-8") as handle:
            pool = json.load(handle)
        for element in pool["elements"]:
            location = element["element"]["location"]
            if not template_exists(location):
                fail(f"{path}: missing template {location}")
            if element["element"].get("processors") != "world_of_exile:tower_processor":
                fail(f"{path}: element without the tower processor")
        fallback = pool["fallback"]
        if fallback != "minecraft:empty":
            fallback_path = os.path.join(pool_dir, f"{fallback.split(':')[1]}.json")
            if not os.path.exists(fallback_path):
                fail(f"{path}: missing fallback {fallback}")

    known_structures = set()
    for path in sorted(glob.glob(os.path.join(structure_dir, "towers_*.json")) + glob.glob(os.path.join(structure_dir, "*_t[123].json"))):
        with open(path, encoding="utf-8") as handle:
            structure = json.load(handle)
        name = os.path.basename(path)[:-5]
        known_structures.add(name)
        for biome in structure["biomes"]:
            if biome not in KNOWN_BIOMES:
                fail(f"{path}: unknown biome {biome}")
        pool = structure["start_pool"]
        pool_path = os.path.join(pool_dir, f"{pool.split(':')[1]}.json")
        if not os.path.exists(pool_path):
            fail(f"{path}: missing start pool {pool}")
        if structure["type"] != "minecraft:jigsaw":
            fail(f"{path}: not a jigsaw structure")

    for path in glob.glob(os.path.join(set_dir, "*.json")):
        with open(path, encoding="utf-8") as handle:
            structure_set = json.load(handle)
        for entry in structure_set["structures"]:
            structure_name = entry["structure"].split(":")[1]
            if not os.path.exists(os.path.join(structure_dir, f"{structure_name}.json")):
                fail(f"{path}: unknown structure {entry['structure']}")


def main():
    pieces = glob.glob(os.path.join(DATA, "structure", "towers", "*", "*.nbt"))
    for path in sorted(pieces):
        check_nbt(path)
    check_json()
    if errors:
        print(f"FAILED with {len(errors)} problem(s):")
        for error in errors:
            print("  -", error)
        sys.exit(1)
    print(f"OK: {len(pieces)} tower pieces validated")


if __name__ == "__main__":
    main()
