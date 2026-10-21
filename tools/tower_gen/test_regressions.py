"""Regression tests for tower resources and source-level safety contracts.

These do not emulate Minecraft, validate Java types or replace an in-game test.
Run: python3 -m unittest discover -s tools/tower_gen -p 'test_*.py' -v
Requires nbtlib (the same dependency as validate.py).
"""
from pathlib import Path
import json
import shutil
import tempfile
import unittest
from unittest.mock import patch

import nbtlib
import validate

ROOT = Path(__file__).resolve().parents[2]
DATA = ROOT / "src/main/resources/data/world_of_exile"
JAVA = ROOT / "src/main/java/com/robertx22/world_of_exile"


class ResourceRegressionTests(unittest.TestCase):
    def setUp(self):
        validate.errors.clear()
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.data = Path(self.temp.name)
        shutil.copytree(DATA / "worldgen", self.data / "worldgen")
        # Pool validation only reads the worldgen JSON and checks NBT existence.
        (self.data / "structure").symlink_to(DATA / "structure", target_is_directory=True)
        self.patch = patch.object(validate, "DATA", str(self.data))
        self.patch.start()
        self.addCleanup(self.patch.stop)

    def write_json(self, relative, payload):
        (self.data / relative).write_text(json.dumps(payload), encoding="utf-8")

    def check(self):
        validate.check_json()
        return "\n".join(validate.errors)

    def test_all_48_themed_structures_are_reachable(self):
        self.assertEqual(self.check(), "")

    def test_legacy_nether_set_cannot_reintroduce_fortress(self):
        self.write_json("worldgen/structure_set/nether_towers.json", {
            "structures": [{"structure": "world_of_exile:shortfatfortress", "weight": 1}]
        })
        self.assertIn("legacy/non-themed", self.check())

    def test_nether_theme_cannot_use_overworld_biome(self):
        path = "worldgen/structure/nether_wastes_t1.json"
        data = json.loads((self.data / path).read_text())
        data["biomes"] = ["minecraft:plains"]
        self.write_json(path, data)
        self.assertIn("dimension mismatch", self.check())

    def test_ice_theme_cannot_use_nether_template(self):
        path = "worldgen/structure/ice_t1.json"
        data = json.loads((self.data / path).read_text())
        data["start_pool"] = "world_of_exile:towers/nether_wastes/start"
        self.write_json(path, data)
        self.assertIn("wrong themed start pool", self.check())

    def test_missing_theme_set_is_detected(self):
        (self.data / "worldgen/structure_set/towers_ice.json").unlink()
        self.assertIn("all 48", self.check())

    def test_legacy_fortress_is_nether_only_even_without_set(self):
        path = "worldgen/structure/shortfatfortress.json"
        data = json.loads((self.data / path).read_text())
        data["biomes"] = "#minecraft:is_overworld"
        self.write_json(path, data)
        self.assertIn("shortfatfortress must not", self.check())


class TemplateRegressionTests(unittest.TestCase):
    def setUp(self):
        validate.errors.clear()
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.path = Path(self.temp.name) / "top0.nbt"
        self.nbt = nbtlib.load(DATA / "structure/towers/ice/top0.nbt")

    def check(self):
        self.nbt.save(self.path, gzipped=True)
        validate.check_nbt(str(self.path))
        return "\n".join(validate.errors)

    def test_complete_template_passes(self):
        self.assertEqual(self.check(), "")

    def test_current_version_with_legacy_palette_is_rejected(self):
        # Reproduce the shipped bug: valid NBT, current version, all blocks
        # silently decoded as AIR by the game's id-only palette reader.
        for state in self.nbt["palette"]:
            state["Name"] = state.pop("id")
            if "properties" in state:
                state["Properties"] = state.pop("properties")
        decoded = [str(state.get("id", "minecraft:air")) for state in self.nbt["palette"]]
        self.assertEqual(set(decoded), {"minecraft:air"})
        self.assertIn("would load as air", self.check())

    def test_legacy_property_key_is_rejected_even_with_valid_id(self):
        state = next(s for s in self.nbt["palette"] if str(s["id"]) == "minecraft:jigsaw")
        state["Properties"] = state.pop("properties")
        self.assertIn("legacy Name/Properties", self.check())

    def test_missing_connector_orientation_is_rejected(self):
        state = next(s for s in self.nbt["palette"] if str(s["id"]) == "minecraft:jigsaw")
        state.pop("properties")
        self.assertIn("wrong orientation", self.check())

    def test_wrong_data_version_is_rejected(self):
        self.nbt["DataVersion"] = nbtlib.Int(2567)
        self.assertIn("expected DataVersion 5023", self.check())

    def test_omitted_air_would_preserve_terrain(self):
        palette = self.nbt["palette"]
        air = next(b for b in self.nbt["blocks"] if str(palette[int(b["state"])]["id"]) == "minecraft:air")
        self.nbt["blocks"].remove(air)
        self.assertIn("explicit air", self.check())

    def test_floating_boss_platform_is_rejected(self):
        air = next(i for i, state in enumerate(self.nbt["palette"]) if str(state["id"]) == "minecraft:air")
        floor = next(b for b in self.nbt["blocks"] if tuple(b["pos"]) == (8, 1, 12))
        floor["state"] = nbtlib.Int(air)
        self.assertIn("unsafe boss platform", self.check())

    def test_decoration_in_boss_headroom_is_rejected(self):
        floor = next(b for b in self.nbt["blocks"] if tuple(b["pos"]) == (8, 1, 12))
        head = next(b for b in self.nbt["blocks"] if tuple(b["pos"]) == (8, 3, 12))
        head["state"] = floor["state"]
        self.assertIn("obstructed boss headroom", self.check())


class PaletteWireFormatTests(unittest.TestCase):
    def test_writer_matches_26_3_vanilla_palette_keys(self):
        # Expectations are literal 26.3 wire fields, not imported writer keys.
        from nbt_writer import Structure
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / "probe.nbt"
            structure = Structure(3, 1, 1)
            structure.put(0, 0, 0, "minecraft:stone_bricks")
            structure.put(1, 0, 0, "minecraft:jigsaw", {"orientation": "up_east"})
            structure.save(path)
            nbt = nbtlib.load(path)
        self.assertEqual(int(nbt["DataVersion"]), 5023)
        self.assertEqual(nbt["palette"].unpack(), [
            {"id": "minecraft:stone_bricks"},
            {"id": "minecraft:jigsaw", "properties": {"orientation": "up_east"}},
            {"id": "minecraft:air"},
        ])

    def test_every_shipped_piece_loads_solid_blocks_and_connectors_by_id(self):
        paths = list((DATA / "structure/towers").rglob("*.nbt"))
        self.assertEqual(len(paths), 96)
        for path in paths:
            with self.subTest(piece=path.relative_to(DATA)):
                nbt = nbtlib.load(path)
                palette = [str(state.get("id", "minecraft:air")) for state in nbt["palette"]]
                decoded = [palette[int(block["state"])] for block in nbt["blocks"]]
                self.assertGreater(sum(name != "minecraft:air" for name in decoded), 100)
                self.assertEqual(decoded.count("minecraft:jigsaw"), 1 if path.stem.startswith("top") else 2)


class SourceSafetyContracts(unittest.TestCase):
    """Guard against reintroducing the specific bugs; not Java execution tests."""
    def test_containment_never_places_support_blocks(self):
        source = (JAVA / "world_gen/TowerBoss.java").read_text()
        method = source.split("public static void contain(", 1)[1].split("public static boolean denyBreak", 1)[0]
        self.assertNotIn("setBlock(", method)
        self.assertNotIn("supportBlock(", source)
        self.assertNotIn("findAir(", source)
        self.assertIn("if (stand != null)", method)

    def test_abilities_use_roster_and_safe_blink(self):
        source = (JAVA / "tower/TowerAbility.java").read_text()
        self.assertIn("TowerBoss.abilitiesOf(boss)", source)
        self.assertIn("for (TowerAbility ability : abilities)", source)
        self.assertNotIn("for (TowerAbility ability : values())", source)
        self.assertIn("TowerBoss.blink(level, boss, pos)", source)
        self.assertNotIn("boss.teleportTo(", source)

    def test_no_ambient_campfire_or_spawner_conversion(self):
        source = (JAVA / "world_gen/TowerBoss.java").read_text()
        self.assertNotIn("placeOneFallbackMarker", source)
        self.assertNotIn("pumpTowerSpawners", source)
        self.assertIn("private static void protectFromSun(Mob mob)", source)
        self.assertIn("entity.addTag(DEF_PREFIX + def.id())", source)


if __name__ == "__main__":
    unittest.main()
