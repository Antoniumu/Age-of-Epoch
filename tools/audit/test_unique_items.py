#!/usr/bin/env python3
"""Unique gear / runewords: registry, ids, display names, descriptions."""

import re
import unittest
from collections import defaultdict

from audit_lib import (ASSETS, JAVA, AuditCase, REPO, load_json, rel, walk)

BAD_NAMESPACES = (
    "mmorpgs.",                 # typo: plural namespace
    "item.mmorpg.unique.",      # typo: singular 'unique'
    "item.mmorpg.uniques.",     # retired namespace, no Java consumer
)
BAD_EXACT_KEYS = {
    "mmorpg.word.isotunique",       # en_us uses mmorpg.word.isnotunique
    "mmorpg.rarity.unique",         # en_us uses mmorpg.rarity.gear.unique
}


class TestUniqueItems(AuditCase, unittest.TestCase):

    def test_registry_ids_are_unique_and_well_formed(self):
        gears = REPO.unique_gears()
        bad = []
        seen = defaultdict(list)
        for gid, name, src in gears:
            seen[gid].append(src)
            if not re.fullmatch(r"[a-z0-9_]+", gid):
                bad.append(f"{gid}: invalid id (src {src})")
            if not name.strip():
                bad.append(f"{gid}: empty Java display name (src {src})")
        for gid, sources in seen.items():
            if len(sources) > 1:
                bad.append(f"{gid}: registered {len(sources)} times: {sources}")
        self.assertEqual(25, len(gears),
                         "unique gear registry count drifted from 25")
        self.assertNoFindings(bad, "Unique gear registry integrity")

    def test_every_unique_has_en_and_ru_display_name(self):
        en, _d, err = load_json(ASSETS / "mmorpg" / "lang" / "en_us.json")
        ru, _d, err2 = load_json(ASSETS / "mmorpg" / "lang" / "ru_ru.json")
        self.assertIsNone(err)
        self.assertIsNone(err2)
        bad = []
        for gid, java_name, src in REPO.unique_gears():
            key = f"mmorpg.unique_gear.{gid}.name"
            for locale, data in (("en_us", en), ("ru_ru", ru)):
                value = data.get(key)
                if not isinstance(value, str) or not value.strip():
                    bad.append(f"{locale}: {key} missing/empty (registered in {src})")
                elif value.strip() == gid or value.strip() == key:
                    bad.append(f"{locale}: {key} = technical id")
        self.assertNoFindings(bad, "Missing unique gear display names")

    def test_unique_display_names_are_distinct_and_readable(self):
        en, _d, _e = load_json(ASSETS / "mmorpg" / "lang" / "en_us.json")
        ru, _d, _e = load_json(ASSETS / "mmorpg" / "lang" / "ru_ru.json")
        bad = []
        for locale, data in (("en_us", en), ("ru_ru", ru)):
            by_name = defaultdict(list)
            for key, value in data.items():
                if not key.startswith("mmorpg.unique_gear.") or not key.endswith(".name"):
                    continue
                name = value.strip() if isinstance(value, str) else ""
                if not name:
                    bad.append(f"{locale}: {key} empty")
                    continue
                if re.fullmatch(r"[a-z0-9_.]+", name):
                    bad.append(f"{locale}: {key} looks like an id: {name!r}")
                if len(name) > 80:
                    bad.append(f"{locale}: {key} suspiciously long ({len(name)})")
                by_name[name].append(key)
            for name, keys in by_name.items():
                if len(keys) > 1:
                    bad.append(f"{locale}: duplicate display name {name!r}: {keys}")
        self.assertNoFindings(bad, "Unique gear name quality")

    def test_no_retired_or_typoed_translation_namespaces(self):
        bad = []
        for ns, path in REPO.lang_files():
            if path.name == "manual.json":
                continue
            data, _d, err = load_json(path)
            if err:
                continue
            for key in data:
                if key in BAD_EXACT_KEYS or key.startswith(BAD_NAMESPACES):
                    bad.append(f"{rel(path)}: dead/misspelled key {key}")
        self.assertNoFindings(bad, "Retired unique-gear translation namespaces")

    def test_unique_builder_sources_reference_known_base_gear(self):
        """Every basegear literal must exist somewhere in the Java sources."""
        literals = REPO.java_literals()
        bad = []
        root = JAVA / "com" / "robertx22" / "age_of_exile" / "aoe_data" / \
               "database" / "unique_gears"
        for path in walk(root, (".java",)):
            text = path.read_text(encoding="utf-8", errors="ignore")
            for match in re.finditer(
                    r'UniqueGearBuilder\.of\(\s*"[^"]+"\s*,\s*"[^"]*"\s*,\s*"([^"]+)"',
                    text):
                base = match.group(1)
                if base not in literals and base.replace("/", "_") not in literals:
                    bad.append(f"{rel(path)}: unknown base gear {base!r}")
        self.assertNoFindings(bad, "Broken base-gear references")

    def test_every_builder_source_is_registered(self):
        """Every builder must be reachable from UniqueGearReg.

        The registry is a tree: UniqueGearReg -> UniqueJewelry -> UniqueRings
        etc. Checking only direct ``new X()`` calls in the root file flags
        perfectly live leaf builders as dead content.
        """
        root = (JAVA / "com" / "robertx22" / "age_of_exile" / "aoe_data" /
                "database" / "unique_gears")
        sources = {p.stem: p for p in walk(root, (".java",))}
        edges = {}
        for stem, path in sources.items():
            text = path.read_text(encoding="utf-8", errors="ignore")
            edges[stem] = set(re.findall(r'new\s+([A-Za-z0-9_]+)\s*\(\s*\)\s*\.registerAll',
                                         text)) & set(sources)
        reachable, todo = set(), ["UniqueGearReg"]
        while todo:
            stem = todo.pop()
            if stem in reachable or stem not in edges:
                continue
            reachable.add(stem)
            todo.extend(edges[stem])
        bad = []
        for stem, path in sources.items():
            if stem in ("UniqueGearReg", "UniqueGearBuilder"):
                continue
            if "UniqueGearBuilder.of(" not in path.read_text(
                    encoding="utf-8", errors="ignore"):
                continue
            if stem not in reachable:
                bad.append(f"{rel(path)}: not reachable from UniqueGearReg")
        self.assertNoFindings(bad, "Orphan unique gear sources")


if __name__ == "__main__":
    unittest.main()
