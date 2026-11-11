#!/usr/bin/env python3
"""Talent trees, perk ids, talent names, icons and spell-school labels."""

import json
import re
import unittest
from collections import defaultdict
from pathlib import Path

from audit_lib import (ASSETS, DATA, JAVA, AuditCase, REPO, load_json, rel)

TREE_DIR = DATA / "mmorpg" / "talent_tree"
TREES = ("talents.json", "divine.json")
CENTER = "[center]"


def grid_rows(text):
    """Rows of the CSV-like perk grid.

    Java's ``String.split("\\n")`` discards trailing empty strings, so a
    trailing newline in the JSON value is harmless and must not be counted as
    a ragged extra row here either.
    """
    lines = text.split("\n")
    while lines and not lines[-1].strip():
        lines.pop()
    return [line.split(",") for line in lines]


PERK_JAVA = (JAVA / "com" / "robertx22" / "age_of_exile" /
             "aoe_data" / "database" / "perks")


def java_perk_ids():
    """Perk ids declared in Java but not necessarily placed in a JSON grid.

    Starting classes such as ``scion`` are registered through a
    ``public static String`` constant and referenced by the design CSV, even
    though the shipped talent tree JSON never contains them as a node.
    """
    ids = set()
    for path in PERK_JAVA.rglob("*.java"):
        text = path.read_text(encoding="utf-8", errors="ignore")
        ids.update(re.findall(
            r'public\s+static\s+String\s+\w+\s*=\s*"([a-z0-9_]+)"', text))
        ids.update(re.findall(r'\bof\(\s*"([a-z0-9_]+)"', text))
        ids.update(re.findall(
            r'PerkBuilder\.\w+\(\s*"([a-z0-9_]+)"', text))
    return ids


def spell_school_ids():
    """Ids of the five schools declared in SpellSchoolsAdder."""
    text = (JAVA / "com" / "robertx22" / "age_of_exile" /
            "aoe_data" / "database" / "spell_schools" /
            "SpellSchoolsAdder.java").read_text(encoding="utf-8", errors="ignore")
    return sorted(set(re.findall(
        r'public\s+static\s+String\s+\w+\s*=\s*"([a-z0-9_]+)"', text)))


class TestTalents(AuditCase, unittest.TestCase):

    def _tree(self, name):
        value, dups, err = load_json(TREE_DIR / name)
        self.assertIsNone(err, f"{name}: {err}")
        self.assertIsInstance(value, dict)
        return value

    def test_tree_schema_and_icons(self):
        bad = []
        index = REPO.asset_index()
        for name in TREES:
            data = self._tree(name)
            for field in ("text_format", "identifier", "school_type", "icon",
                          "order", "perks"):
                if field not in data:
                    bad.append(f"{name}: missing field {field}")
            icon = data.get("icon", "")
            if isinstance(icon, str) and icon and icon not in index:
                bad.append(f"{name}: missing school icon {icon}")
            if not isinstance(data.get("perks"), str) or not data["perks"].strip():
                bad.append(f"{name}: empty perk grid")
            if not isinstance(data.get("order"), int):
                bad.append(f"{name}: order is not an integer")
        self.assertNoFindings(bad, "Talent tree schema")

    def test_grid_shape_and_center(self):
        bad = []
        for name in TREES:
            rows = grid_rows(self._tree(name)["perks"])
            widths = {len(r) for r in rows}
            if len(widths) != 1:
                bad.append(f"{name}: ragged grid, row widths {sorted(widths)}")
            centers = sum(1 for row in rows for cell in row
                          if cell.strip().lower() == CENTER)
            if name == "talents.json" and centers != 1:
                bad.append(f"{name}: expected exactly 1 center, got {centers}")
            for y, row in enumerate(rows):
                for x, cell in enumerate(row):
                    token = cell.strip()
                    if token and not re.fullmatch(r"[a-z0-9_\[\]]{1,48}", token.lower()):
                        bad.append(f"{name}: cell ({x},{y}) = invalid token {token!r}")
        self.assertNoFindings(bad, "Talent grid integrity")

    def test_tree_ids_are_registered_in_java(self):
        source = REPO.java_code()
        literals = REPO.java_literals()
        bad = []
        for tid in sorted(REPO.talent_tree_ids()):
            if tid in source:
                continue
            if any(tid.startswith(lit) or tid.endswith(lit) for lit in literals):
                continue
            bad.append(f"{tid}: node has no matching perk id in Java sources")
        self.assertNoFindings(bad, "Unreachable talent nodes")

    def test_named_perks_have_translated_names(self):
        """Regression guard for the reported 'talent name' bug.

        STAT perks render only their stat list and need no name; every other
        perk calls Perk.locName() -> mmorpg.talent.<id>.
        """
        needed = sorted(REPO.named_perk_ids() & REPO.talent_tree_ids())
        self.assertGreater(len(needed), 150, "perk registry was not parsed")
        en, _d, _e = load_json(ASSETS / "mmorpg" / "lang" / "en_us.json")
        ru, _d, _e = load_json(ASSETS / "mmorpg" / "lang" / "ru_ru.json")
        bad = []
        for tid in needed:
            key = f"mmorpg.talent.{tid}"
            for locale, data in (("en_us", en), ("ru_ru", ru)):
                value = data.get(key)
                if not isinstance(value, str) or not value.strip():
                    bad.append(f"{locale}: missing talent name {key}")
        self.assertNoFindings(bad, "Missing talent names")

    def test_no_orphan_talent_keys(self):
        literals = REPO.java_literals()
        bad = []
        for ns, path in REPO.lang_files():
            if ns != "mmorpg" or path.name == "manual.json":
                continue
            data, _d, err = load_json(path)
            if err:
                continue
            for key in data:
                if not key.startswith("mmorpg.talent."):
                    continue
                tid = key[len("mmorpg.talent."):]
                if tid not in literals and tid not in REPO.talent_tree_ids():
                    bad.append(f"{rel(path)}: orphan talent key {key}")
        self.assertNoFindings(bad, "Orphan talent translations")

    def test_talent_names_are_distinct_and_readable(self):
        en, _d, _e = load_json(ASSETS / "mmorpg" / "lang" / "en_us.json")
        ru, _d, _e = load_json(ASSETS / "mmorpg" / "lang" / "ru_ru.json")
        bad = []
        for locale, data in (("en_us", en), ("ru_ru", ru)):
            by_name = defaultdict(list)
            for key, value in data.items():
                if not key.startswith("mmorpg.talent."):
                    continue
                if not isinstance(value, str) or not value.strip():
                    bad.append(f"{locale}: {key} empty")
                    continue
                name = value.strip()
                if re.fullmatch(r"[a-z0-9_]+", name):
                    bad.append(f"{locale}: {key} looks like an id: {name!r}")
                if len(name) > 80:
                    bad.append(f"{locale}: {key} too long ({len(name)})")
                by_name[name].append(key)
            for name, keys in by_name.items():
                if len(keys) > 1:
                    bad.append(f"{locale}: duplicate talent name {name!r}: {keys}")
        self.assertNoFindings(bad, "Talent name quality")

    def test_spell_school_names_exist(self):
        ids = spell_school_ids()
        self.assertEqual(5, len(ids), "expected 5 spell schools")
        bad = []
        for ns, path in REPO.lang_files():
            if ns != "mmorpg" or path.name == "manual.json":
                continue
            data, _d, err = load_json(path)
            if err:
                continue
            for school in ids:
                key = f"mmorpg.spell_school.{school}"
                if key not in data or not str(data[key]).strip():
                    bad.append(f"{rel(path)}: missing school name {key}")
        self.assertNoFindings(bad, "Missing spell-school names")

    def test_talent_icons_exist(self):
        index = REPO.asset_index()
        bad = []
        for rid in sorted(REPO.java_texture_refs()):
            if "/talent_icons/" in rid and rid not in index:
                bad.append(f"missing talent icon {rid}")
        self.assertNoFindings(bad, "Broken talent icon references")

    def test_talents_csv_is_consistent(self):
        """talents.csv is the level-design sketch, not a mirror of the JSON grid.

        It is a larger annotated canvas (``x`` = occupied cell, ``[CENTER]`` =
        the root) used to plan the tree. What must hold is that it is a proper
        rectangular grid and that every real perk token in it is a node that
        also exists in the shipped trees.
        """
        csv = TREE_DIR / "talents.csv"
        self.assertTrue(csv.exists(), "talents.csv missing")
        rows = [line.split(",") for line in
                csv.read_text(encoding="utf-8").splitlines()]
        widths = {len(r) for r in rows}
        self.assertEqual(1, len(widths),
                         f"talents.csv has ragged rows: {sorted(widths)}")
        tokens = {cell.strip().lower() for row in rows for cell in row
                  if cell.strip()}
        known = (REPO.talent_tree_ids() | java_perk_ids() |
                 {"x", "o", "int", "[center]"})
        bad = [f"talents.csv: unknown token {t!r}" for t in sorted(tokens - known)]
        self.assertNoFindings(bad, "Talent design grid references unknown nodes")


if __name__ == "__main__":
    unittest.main()
