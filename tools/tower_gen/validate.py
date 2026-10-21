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

from themes import THEMES

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
    "minecraft:taiga",
    "minecraft:old_growth_pine_taiga",
    "minecraft:old_growth_spruce_taiga",
    "minecraft:cherry_grove",
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
    # Do not accept the old keys even though an NBT parser can read them.
    # 26.3 resolves a palette entry without `id` to AIR; DataVersion 5023
    # prevents the legacy data fixer from rescuing mislabeled templates.
    if int(root.get("DataVersion", -1)) != 5023:
        fail(f"{path}: expected DataVersion 5023 for the 26.3 palette schema")
        return
    invalid_palette = False
    for index, state in enumerate(palette):
        if not isinstance(state.get("id"), nbtlib.String) or not str(state["id"]).startswith("minecraft:"):
            fail(f"{path}: palette {index} missing valid 26.3 block id (would load as air)")
            invalid_palette = True
        if "Name" in state or "Properties" in state:
            fail(f"{path}: palette {index} uses legacy Name/Properties with current DataVersion")
            invalid_palette = True
        props = state.get("properties")
        if props is not None and (not isinstance(props, nbtlib.Compound)
                                  or any(not isinstance(v, nbtlib.String) for v in props.values())):
            fail(f"{path}: palette {index} properties must be a compound of strings")
            invalid_palette = True
    if invalid_palette:
        return
    blocks = root["blocks"]
    positions = set()
    for block in blocks:
        x, y, z = [int(v) for v in block["pos"]]
        if not (0 <= x < size[0] and 0 <= y < size[1] and 0 <= z < size[2]):
            fail(f"{path}: block {(x, y, z)} outside {size}")
        if (x, y, z) in positions:
            fail(f"{path}: duplicate block at {(x, y, z)}")
        positions.add((x, y, z))
        state = int(block["state"])
        if state < 0 or state >= len(palette):
            fail(f"{path}: palette index {state} out of range")
            return
    if len(positions) != size[0] * size[1] * size[2]:
        fail(f"{path}: missing explicit air cells: terrain can fill the tower interior")
    states = {tuple(int(v) for v in block["pos"]): str(palette[int(block["state"])]["id"])
              for block in blocks}

    names = {}
    for block in blocks:
        name = str(palette[int(block["state"])]["id"])
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
            if states.get((sx, y, sz), "minecraft:air") == "minecraft:air":
                fail(f"{path}: ladder at {(lx, y, lz)} has no support at {(sx, y, sz)}")
                break

    jigsaws = names.get("minecraft:jigsaw", [])
    for block in jigsaws:
        tag = block.get("nbt")
        if tag is None:
            fail(f"{path}: jigsaw without nbt")
            continue
        orientation = str(palette[int(block["state"])].get("properties", {}).get("orientation", ""))
        expected_orientation = "down_north" if int(block["pos"][1]) == 0 else "up_east"
        if orientation != expected_orientation:
            fail(f"{path}: connector has wrong orientation {orientation}")
        if str(tag.get("name")) != "world_of_exile:connect":
            fail(f"{path}: connector has wrong name")
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
        if marker == "[boss]":
            x, y, z = [int(v) for v in block["pos"]]
            for dx in range(-2, 3):
                for dz in range(-2, 3):
                    floor = states.get((x + dx, y - 1, z + dz), "minecraft:air")
                    if floor in {"minecraft:air", "minecraft:water", "minecraft:lava", "minecraft:magma_block"}:
                        fail(f"{path}: unsafe boss platform")
                    for dy in range(5):
                        if dx == dz == dy == 0:
                            continue  # the marker itself is removed before spawn
                        if states.get((x + dx, y + dy, z + dz)) != "minecraft:air":
                            fail(f"{path}: obstructed boss headroom at {(x+dx, y+dy, z+dz)}")
        if marker == "[spawner]" and "[tower:" not in raw:
            fail(f"{path}: spawner sign has no theme tag")
        if marker in ("[treasure]", "[final_treasure]") and "[loot#" not in raw:
            fail(f"{path}: treasure sign has no loot table")
    is_top = os.path.basename(path).startswith("top")
    if markers.count("[boss]") != (1 if is_top else 0):
        fail(f"{path}: expected exactly one boss in top, none in other pieces")
    if len(jigsaws) != (1 if is_top else 2):
        fail(f"{path}: missing vertical connector")
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
        theme_id = os.path.basename(os.path.dirname(path))
        for element in pool["elements"]:
            location = element["element"]["location"]
            if not location.startswith(f"world_of_exile:towers/{theme_id}/"):
                fail(f"{path}: template belongs to another theme")
            if not template_exists(location):
                fail(f"{path}: missing template {location}")
            if element["element"].get("processors") != "world_of_exile:tower_processor":
                fail(f"{path}: element without the tower processor")
        fallback = pool["fallback"]
        if fallback != "minecraft:empty":
            if not fallback.startswith(f"world_of_exile:towers/{theme_id}/"):
                fail(f"{path}: fallback belongs to another theme")
            fallback_path = os.path.join(pool_dir, f"{fallback.split(':')[1]}.json")
            if not os.path.exists(fallback_path):
                fail(f"{path}: missing fallback {fallback}")

    by_id = {theme["id"]: theme for theme in THEMES}
    nether_biomes = {biome for theme in THEMES if theme.get("nether") for biome in theme["biomes"]}
    expected_structures = {f"{theme['id']}_t{tier}" for theme in THEMES for tier in (1, 2, 3)}
    known_structures = set()
    for path in sorted(glob.glob(os.path.join(structure_dir, "towers_*.json")) + glob.glob(os.path.join(structure_dir, "*_t[123].json"))):
        with open(path, encoding="utf-8") as handle:
            structure = json.load(handle)
        name = os.path.basename(path)[:-5]
        known_structures.add(name)
        theme = by_id[name.rsplit("_t", 1)[0]]
        if set(structure["biomes"]) != set(theme["biomes"]):
            fail(f"{path}: wrong biome whitelist for theme {theme['id']}")
        if bool(set(structure["biomes"]) & nether_biomes) != bool(theme.get("nether")):
            fail(f"{path}: dimension mismatch")
        if structure["start_pool"] != f"world_of_exile:towers/{theme['id']}/start":
            fail(f"{path}: wrong themed start pool")
        for biome in structure["biomes"]:
            if biome not in KNOWN_BIOMES:
                fail(f"{path}: unknown biome {biome}")
        pool = structure["start_pool"]
        pool_path = os.path.join(pool_dir, f"{pool.split(':')[1]}.json")
        if not os.path.exists(pool_path):
            fail(f"{path}: missing start pool {pool}")
        if structure["type"] != "minecraft:jigsaw":
            fail(f"{path}: not a jigsaw structure")

    if known_structures != expected_structures:
        fail("Missing or extra themed structures")
    active = set()
    for path in glob.glob(os.path.join(set_dir, "*.json")):
        with open(path, encoding="utf-8") as handle:
            structure_set = json.load(handle)
        for entry in structure_set["structures"]:
            structure_name = entry["structure"].split(":")[1]
            active.add(structure_name)
            if structure_name not in expected_structures:
                fail(f"{path}: legacy/non-themed structure still in natural generation: {structure_name}")
            if not os.path.exists(os.path.join(structure_dir, f"{structure_name}.json")):
                fail(f"{path}: unknown structure {entry['structure']}")

    if active != expected_structures:
        fail("Natural generation must include all 48 themed structures and no legacy towers")
    legacy = os.path.join(structure_dir, "shortfatfortress.json")
    with open(legacy, encoding="utf-8") as handle:
        if json.load(handle)["biomes"] != "#minecraft:is_nether":
            fail("shortfatfortress must not be eligible in the overworld even if re-added to a set")


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
