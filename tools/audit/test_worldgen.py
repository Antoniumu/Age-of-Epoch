#!/usr/bin/env python3
"""Worldgen: jigsaw structures, structure sets, template pools, NBT, dimensions.

The dungeon dimensions are the backbone of the whole progression loop, so a
dangling ``start_pool`` or a corrupt .nbt silently removes content with no
crash and no log line. These checks resolve every reference in both
directions.
"""

import gzip
import re
import unittest
from collections import defaultdict

from audit_lib import (DATA, AuditCase, REPO, load_json, rel, split_id, walk)

ID = re.compile(r"^[a-z0-9_.-]+:[a-z0-9_./-]+$")
PATH = re.compile(r"^[a-z0-9_./-]+$")

VALID_STEPS = {"surface_structures", "underground_structures"}
VALID_ADAPTATION = {"none", "beard_thin", "beard_box", "bury", "encapsulate"}
VALID_ELEMENT_TYPES = {
    "minecraft:single_pool_element",
    "minecraft:list_pool_element",
    "minecraft:feature_pool_element",
    "minecraft:empty_pool_element",
    "minecraft:legacy_single_pool_element",
}

# These four legacy towers have no structure_set on purpose: they are placed
# by the tower/delve runtime (StructureWrapper / TowerRoster), not by vanilla
# random-spread placement. Anything *new* without a set is a dead structure.
RUNTIME_PLACED = {
    "world_of_exile:blackstone_tower",
    "world_of_exile:ladder_tower",
    "world_of_exile:shortfatfortress",
    "world_of_exile:stone_brick_tower",
}


def under(*parts):
    return DATA.joinpath(*parts)


def worldgen_files(kind):
    """Every data/<ns>/worldgen/<kind>/**/*.json in the pack."""
    out = []
    for p in walk(DATA, (".json",)):
        r = rel(p)
        marker = f"/worldgen/{kind}/"
        if marker in r:
            out.append(p)
    return out


def rid_of(path, kind):
    """Resource id of a worldgen json, e.g. mmorpg:towers/foo/start."""
    r = rel(path)
    ns = r.split("/data/", 1)[1].split("/", 1)[0]
    tail = r.split(f"/worldgen/{kind}/", 1)[1][:-len(".json")]
    return f"{ns}:{tail}"


def parsed(paths):
    for p in paths:
        value, _dups, err = load_json(p)
        yield p, value, err


class TestWorldgen(AuditCase, unittest.TestCase):

    def _pool_ids(self):
        return {rid_of(p, "template_pool") for p in worldgen_files("template_pool")}

    def _processor_ids(self):
        return {rid_of(p, "processor_list") for p in worldgen_files("processor_list")}

    def _structure_ids(self):
        return {rid_of(p, "structure") for p in worldgen_files("structure")}

    # ------------------------------------------------------------- structures
    def test_jigsaw_structure_schema(self):
        files = worldgen_files("structure")
        self.assertGreaterEqual(len(files), 50, "structure corpus incomplete")
        bad = []
        for path, value, err in parsed(files):
            where = rel(path)
            if err or not isinstance(value, dict):
                bad.append(f"{where}: {err or 'root is not an object'}")
                continue
            if value.get("type") != "minecraft:jigsaw":
                bad.append(f"{where}: unexpected type {value.get('type')!r}")
            biomes = value.get("biomes")
            if isinstance(biomes, str):
                # A biome tag such as "#minecraft:is_overworld".
                if not re.fullmatch(r"#[a-z0-9_.-]+:[a-z0-9_./-]+", biomes):
                    bad.append(f"{where}: malformed biome tag {biomes!r}")
            elif not isinstance(biomes, list) or not biomes:
                bad.append(f"{where}: missing biomes")
            elif any(not isinstance(b, str) or not ID.match(b) for b in biomes):
                bad.append(f"{where}: malformed biome id")
            if value.get("step") not in VALID_STEPS:
                bad.append(f"{where}: invalid step {value.get('step')!r}")
            if value.get("terrain_adaptation") not in VALID_ADAPTATION:
                bad.append(f"{where}: invalid terrain_adaptation")
            size = value.get("size")
            if not isinstance(size, int) or not 1 <= size <= 128:
                bad.append(f"{where}: invalid jigsaw size {size!r}")
            distance = value.get("max_distance_from_center")
            if not isinstance(distance, int) or not 1 <= distance <= 256:
                bad.append(f"{where}: invalid max_distance_from_center {distance!r}")
            height = value.get("start_height")
            if not isinstance(height, dict):
                bad.append(f"{where}: start_height must be a height provider")
            pool = value.get("start_pool")
            if not isinstance(pool, str) or not ID.match(pool):
                bad.append(f"{where}: invalid start_pool {pool!r}")
            overrides = value.get("spawn_overrides")
            if not isinstance(overrides, dict):
                bad.append(f"{where}: spawn_overrides must be an object")
        self.assertNoFindings(bad, "Malformed jigsaw structures")

    def test_structure_start_pools_resolve(self):
        known = self._pool_ids()
        bad = []
        for path, value, err in parsed(worldgen_files("structure")):
            if err or not isinstance(value, dict):
                continue
            pool = value.get("start_pool")
            if isinstance(pool, str) and pool not in known:
                bad.append(f"{rel(path)}: unknown start_pool {pool}")
        self.assertNoFindings(bad, "Structures with unresolvable start pools")

    # ---------------------------------------------------------- structure sets
    def test_structure_set_schema(self):
        files = worldgen_files("structure_set")
        self.assertGreaterEqual(len(files), 15, "structure_set corpus incomplete")
        bad = []
        for path, value, err in parsed(files):
            where = rel(path)
            if err or not isinstance(value, dict):
                bad.append(f"{where}: {err or 'root is not an object'}")
                continue
            structures = value.get("structures")
            if not isinstance(structures, list) or not structures:
                bad.append(f"{where}: empty structures list")
            else:
                for i, entry in enumerate(structures):
                    if not isinstance(entry, dict) or not ID.match(str(entry.get("structure", ""))):
                        bad.append(f"{where} structures[{i}]: bad structure id")
                    w = entry.get("weight") if isinstance(entry, dict) else None
                    if not isinstance(w, int) or w < 1:
                        bad.append(f"{where} structures[{i}]: bad weight {w!r}")
            placement = value.get("placement")
            if not isinstance(placement, dict):
                bad.append(f"{where}: missing placement")
                continue
            if placement.get("type") != "minecraft:random_spread":
                bad.append(f"{where}: unexpected placement type")
            spacing, separation = placement.get("spacing"), placement.get("separation")
            if not isinstance(spacing, int) or not 1 <= spacing <= 4096:
                bad.append(f"{where}: invalid spacing {spacing!r}")
            if not isinstance(separation, int) or not 0 <= separation < spacing:
                bad.append(f"{where}: separation must be < spacing ({separation} vs {spacing})")
            salt = placement.get("salt")
            if not isinstance(salt, int) or salt < 0:
                bad.append(f"{where}: invalid salt {salt!r}")
        self.assertNoFindings(bad, "Malformed structure sets")

    def test_structure_set_references_resolve(self):
        known = self._structure_ids()
        bad = []
        for path, value, err in parsed(worldgen_files("structure_set")):
            if err or not isinstance(value, dict):
                continue
            for entry in value.get("structures", []):
                sid = entry.get("structure") if isinstance(entry, dict) else None
                if isinstance(sid, str) and sid not in known:
                    bad.append(f"{rel(path)}: unknown structure {sid}")
        self.assertNoFindings(bad, "Structure sets pointing at missing structures")

    def test_structure_set_salts_are_unique(self):
        """Two sets sharing a salt generate the exact same chunk positions."""
        owners = defaultdict(list)
        for path, value, err in parsed(worldgen_files("structure_set")):
            if err or not isinstance(value, dict):
                continue
            salt = (value.get("placement") or {}).get("salt")
            if isinstance(salt, int):
                owners[salt].append(rel(path))
        bad = [f"salt {salt}: {', '.join(paths)}"
               for salt, paths in sorted(owners.items()) if len(paths) > 1]
        self.assertNoFindings(bad, "Colliding structure-set salts")

    def test_every_structure_is_reachable(self):
        used = set()
        for _p, value, err in parsed(worldgen_files("structure_set")):
            if err or not isinstance(value, dict):
                continue
            for entry in value.get("structures", []):
                if isinstance(entry, dict) and isinstance(entry.get("structure"), str):
                    used.add(entry["structure"])
        orphans = sorted(self._structure_ids() - used - RUNTIME_PLACED)
        bad = [f"{sid}: no structure_set places it" for sid in orphans]
        self.assertNoFindings(bad, "Unreachable structures")

    # ---------------------------------------------------------- template pools
    def test_template_pool_schema(self):
        files = worldgen_files("template_pool")
        self.assertGreaterEqual(len(files), 60, "template pool corpus incomplete")
        bad = []
        for path, value, err in parsed(files):
            where = rel(path)
            if err or not isinstance(value, dict):
                bad.append(f"{where}: {err or 'root is not an object'}")
                continue
            fallback = value.get("fallback")
            if not isinstance(fallback, str) or not ID.match(fallback):
                bad.append(f"{where}: invalid fallback {fallback!r}")
            elements = value.get("elements")
            if not isinstance(elements, list) or not elements:
                bad.append(f"{where}: empty elements")
                continue
            for i, entry in enumerate(elements):
                tag = f"{where} elements[{i}]"
                if not isinstance(entry, dict):
                    bad.append(f"{tag}: not an object")
                    continue
                weight = entry.get("weight")
                if not isinstance(weight, int) or weight < 1:
                    bad.append(f"{tag}: invalid weight {weight!r}")
                element = entry.get("element")
                if not isinstance(element, dict):
                    bad.append(f"{tag}: missing element")
                    continue
                if element.get("element_type") not in VALID_ELEMENT_TYPES:
                    bad.append(f"{tag}: unknown element_type {element.get('element_type')!r}")
                location = element.get("location")
                if not isinstance(location, str) or not ID.match(location):
                    bad.append(f"{tag}: invalid location {location!r}")
                projection = element.get("projection")
                if projection not in ("rigid", "terrain_matching"):
                    bad.append(f"{tag}: invalid projection {projection!r}")
                processors = element.get("processors")
                if processors is not None and (not isinstance(processors, str)
                                               or not ID.match(processors)):
                    bad.append(f"{tag}: invalid processors {processors!r}")
        self.assertNoFindings(bad, "Malformed template pools")

    def test_template_pool_fallbacks_resolve(self):
        known = self._pool_ids() | {"minecraft:empty"}
        bad = []
        for path, value, err in parsed(worldgen_files("template_pool")):
            if err or not isinstance(value, dict):
                continue
            fb = value.get("fallback")
            if isinstance(fb, str) and fb not in known:
                bad.append(f"{rel(path)}: unknown fallback {fb}")
        self.assertNoFindings(bad, "Template pools with unresolvable fallbacks")

    def test_template_pool_nbt_locations_exist(self):
        known = REPO.nbt_ids()
        bad = []
        checked = 0
        for path, value, err in parsed(worldgen_files("template_pool")):
            if err or not isinstance(value, dict):
                continue
            for entry in value.get("elements", []):
                element = entry.get("element") if isinstance(entry, dict) else None
                location = element.get("location") if isinstance(element, dict) else None
                if not isinstance(location, str):
                    continue
                ns, _ = split_id(location)
                if ns == "minecraft":
                    continue
                checked += 1
                if location not in known:
                    bad.append(f"{rel(path)}: missing .nbt for {location}")
        self.assertGreater(checked, 100, "template pool locations not parsed")
        self.assertNoFindings(bad, "Template pools pointing at missing structures")

    def test_template_pool_processors_resolve(self):
        known = self._processor_ids() | {"minecraft:empty"}
        bad = []
        for path, value, err in parsed(worldgen_files("template_pool")):
            if err or not isinstance(value, dict):
                continue
            for entry in value.get("elements", []):
                element = entry.get("element") if isinstance(entry, dict) else None
                processors = element.get("processors") if isinstance(element, dict) else None
                if isinstance(processors, str) and processors not in known:
                    bad.append(f"{rel(path)}: unknown processor list {processors}")
        self.assertNoFindings(bad, "Template pools with unresolvable processors")

    # ----------------------------------------------------------- processor list
    def test_processor_lists_schema(self):
        files = worldgen_files("processor_list")
        self.assertGreaterEqual(len(files), 2, "processor list corpus incomplete")
        bad = []
        for path, value, err in parsed(files):
            where = rel(path)
            if err or not isinstance(value, dict):
                bad.append(f"{where}: {err or 'root is not an object'}")
                continue
            processors = value.get("processors")
            if not isinstance(processors, list) or not processors:
                bad.append(f"{where}: empty processors")
                continue
            for i, proc in enumerate(processors):
                if not isinstance(proc, dict) or "processor_type" not in proc:
                    bad.append(f"{where} processors[{i}]: missing processor_type")
        self.assertNoFindings(bad, "Malformed processor lists")

    def test_processor_lists_are_registered_in_java(self):
        """StructureProcessor codecs must be registered under the same id."""
        java = REPO.java_code()
        bad = []
        for rid in sorted(self._processor_ids()):
            ns, name = split_id(rid)
            if f'"{ns}"' not in java or f'"{name}"' not in java:
                bad.append(f"{rid}: no Java registration found")
        self.assertNoFindings(bad, "Unregistered structure processors")

    # --------------------------------------------------------------------- nbt
    def test_nbt_files_are_valid_gzip(self):
        files = walk(DATA, (".nbt",))
        self.assertGreaterEqual(len(files), 200, "structure nbt corpus incomplete")
        bad = []
        for path in files:
            try:
                raw = path.read_bytes()
            except OSError as exc:
                bad.append(f"{rel(path)}: {exc}")
                continue
            if not raw.startswith(b"\x1f\x8b"):
                bad.append(f"{rel(path)}: not a gzip-compressed NBT")
                continue
            try:
                body = gzip.decompress(raw)
            except (OSError, EOFError) as exc:
                bad.append(f"{rel(path)}: corrupt gzip ({exc})")
                continue
            if len(body) < 10:
                bad.append(f"{rel(path)}: decompressed NBT is empty ({len(body)} bytes)")
        self.assertNoFindings(bad, "Corrupt structure NBT files")

    def test_nbt_singular_and_plural_copies_agree(self):
        """Both `structure/` and `structures/` are shipped for compatibility.

        If a pair disagrees, which version loads depends on the game version,
        so the same room silently changes shape between installs.
        """
        bad = []
        pairs = 0
        for ns in ("mmorpg", "infinite_dungeons"):
            singular = under(ns, "structure")
            plural = under(ns, "structures")
            if not singular.is_dir() or not plural.is_dir():
                continue
            left = {p.relative_to(singular).as_posix(): p
                    for p in walk(singular, (".nbt",))}
            right = {p.relative_to(plural).as_posix(): p
                     for p in walk(plural, (".nbt",))}
            for name in sorted(left.keys() & right.keys()):
                pairs += 1
                try:
                    if gzip.decompress(left[name].read_bytes()) != \
                            gzip.decompress(right[name].read_bytes()):
                        bad.append(f"{ns}/{name}: singular and plural NBT differ")
                except (OSError, EOFError) as exc:
                    bad.append(f"{ns}/{name}: {exc}")
        self.assertGreater(pairs, 50, "NBT mirror pairs not found")
        self.assertNoFindings(bad, "Divergent structure NBT copies")

    # -------------------------------------------------------------- dimensions
    def test_dimension_type_schema(self):
        files = walk(under("mmorpg", "dimension_type"), (".json",))
        self.assertGreaterEqual(len(files), 2, "dimension types missing")
        bad = []
        for path, value, err in parsed(files):
            where = rel(path)
            if err or not isinstance(value, dict):
                bad.append(f"{where}: {err or 'root is not an object'}")
                continue
            height, min_y = value.get("height"), value.get("min_y")
            if not isinstance(height, int) or height < 16:
                bad.append(f"{where}: invalid height {height!r}")
            if not isinstance(min_y, int) or min_y % 16:
                bad.append(f"{where}: min_y must be a multiple of 16 ({min_y!r})")
            logical = value.get("logical_height")
            if not isinstance(logical, int) or logical > height:
                bad.append(f"{where}: logical_height exceeds height")
            scale = value.get("coordinate_scale")
            if not isinstance(scale, (int, float)) or not 0 < scale <= 1e6:
                bad.append(f"{where}: invalid coordinate_scale {scale!r}")
            for key in ("has_skylight", "has_ceiling", "has_fixed_time",
                        "has_ender_dragon_fight", "ultrawarm", "natural",
                        "piglin_safe", "respawn_anchor_works", "bed_works",
                        "has_raids"):
                if key in value and not isinstance(value[key], bool):
                    bad.append(f"{where}: {key} must be a boolean")
        self.assertNoFindings(bad, "Malformed dimension types")

    def test_dimension_type_references_resolve(self):
        known = {f"mmorpg:{p.stem}"
                 for p in walk(under("mmorpg", "dimension_type"), (".json",))}
        bad = []
        for path, value, err in parsed(walk(under("mmorpg", "dimension"), (".json",))):
            if err or not isinstance(value, dict):
                continue
            dtype = value.get("type")
            if dtype not in known:
                bad.append(f"{rel(path)}: unknown dimension_type {dtype!r}")
            generator = value.get("generator")
            if not isinstance(generator, dict) or "type" not in generator:
                bad.append(f"{rel(path)}: missing generator type")
        self.assertNoFindings(bad, "Dimensions with unresolvable type or generator")

    def test_dimension_ids_are_registered_in_java(self):
        java = REPO.java_code()
        bad = []
        for path in walk(under("mmorpg", "dimension"), (".json",)):
            name = path.stem
            if f'"{name}"' not in java:
                bad.append(f"{rel(path)}: dimension id {name!r} not used in Java")
        self.assertNoFindings(bad, "Unregistered dimensions")


if __name__ == "__main__":
    unittest.main()
