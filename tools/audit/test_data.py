#!/usr/bin/env python3
"""Datapack layout: registry folder names, tags, id uniqueness.

Minecraft renamed most ``data/<ns>/<registry>/`` folders to the singular in
1.21 (``recipes`` -> ``recipe``, ``loot_tables`` -> ``loot_table``). A file in
the old folder is silently invisible: no error, just a missing reward.
"""

import re
import unittest
from collections import defaultdict

from audit_lib import ASSETS, DATA, AuditCase, REPO, load_json, rel, walk

NAMESPACE = re.compile(r"^[a-z0-9_.-]+$")
RESOURCE_ID = re.compile(r"^#[a-z0-9_.-]+:[a-z0-9_./-]+$|^[a-z0-9_.-]+:[a-z0-9_./-]+$")
LEGACY_PLURAL = ("recipes", "loot_tables", "advancements", "functions",
                 "item_modifiers", "predicates")
SINGULAR_REGISTRIES = ("recipe", "loot_table", "advancement", "function",
                       "item_modifier", "predicate")


class TestData(AuditCase, unittest.TestCase):

    def test_namespace_names_are_valid(self):
        bad = []
        for path in DATA.iterdir():
            if path.is_dir() and not NAMESPACE.match(path.name):
                bad.append(f"invalid namespace directory {rel(path)}")
        self.assertNoFindings(bad, "Invalid datapack namespaces")

    def test_no_legacy_plural_registry_folders(self):
        bad = []
        for ns in DATA.iterdir():
            if not ns.is_dir():
                continue
            for folder in ns.iterdir():
                if folder.is_dir() and folder.name in LEGACY_PLURAL:
                    bad.append(f"{rel(folder)}: legacy plural folder "
                               f"(use the singular form)")
        self.assertNoFindings(bad, "Data in unreachable legacy folders")

    def test_singular_registry_files_are_json(self):
        bad = []
        for path in walk(DATA, (".json",)):
            parts = path.relative_to(DATA).parts
            if len(parts) >= 2 and parts[1] in SINGULAR_REGISTRIES:
                _value, _dups, err = load_json(path)
                if err:
                    bad.append(f"{rel(path)}: {err}")
        self.assertNoFindings(bad, "Malformed datapack registry files")

    def test_tag_files_are_well_formed(self):
        index = REPO.asset_index()
        bad = []
        tags = [p for p in walk(DATA, (".json",))
                if "/tags/" in rel(p)]
        self.assertGreater(len(tags), 0, "no tag files found")
        for path in tags:
            value, _dups, err = load_json(path)
            if err or not isinstance(value, dict):
                bad.append(f"{rel(path)}: {err or 'not an object'}")
                continue
            if "replace" in value and not isinstance(value["replace"], bool):
                bad.append(f"{rel(path)}: replace must be a boolean")
            values = value.get("values")
            if not isinstance(values, list) or not values:
                bad.append(f"{rel(path)}: missing/empty values")
                continue
            for entry in values:
                if isinstance(entry, dict):
                    entry = entry.get("id")
                if not isinstance(entry, str) or not RESOURCE_ID.match(entry):
                    bad.append(f"{rel(path)}: invalid tag value {entry!r}")
                    continue
                rid = entry.lstrip("#")
                ns, item = rid.split(":", 1)
                if ns in ("mmorpg", "divine_missions", "infinite_dungeons",
                          "library_of_exile") and not entry.startswith("#"):
                    if f"{ns}:items/{item}.json" not in index:
                        bad.append(f"{rel(path)}: unknown item {entry}")
        self.assertNoFindings(bad, "Broken item tags")

    def test_registry_ids_are_unique(self):
        """Two files resolving to the same resource id fight at load time."""
        bad = []
        for registry in SINGULAR_REGISTRIES:
            seen = defaultdict(list)
            for path in walk(DATA, (".json",)):
                parts = path.relative_to(DATA).parts
                if len(parts) < 3 or parts[1] != registry:
                    continue
                rid = f"{parts[0]}:{'/'.join(parts[2:])[:-len('.json')]}"
                seen[rid].append(rel(path))
            for rid, paths in seen.items():
                if len(paths) > 1:
                    bad.append(f"duplicate {registry} id {rid}: "
                               f"{', '.join(paths)}")
        self.assertNoFindings(bad, "Duplicate datapack ids")

    def test_item_definitions_are_reachable(self):
        """Every assets/<ns>/items/*.json must be valid JSON (all of them)."""
        bad = []
        defs = [p for p in walk(ASSETS, (".json",))
                if "/items/" in rel(p)]
        self.assertGreater(len(defs), 100, "item definition corpus incomplete")
        for path in defs:
            _value, _dups, err = load_json(path)
            if err:
                bad.append(f"{rel(path)}: {err}")
        self.assertNoFindings(bad, "Malformed item definitions")


if __name__ == "__main__":
    unittest.main()
