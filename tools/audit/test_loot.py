#!/usr/bin/env python3
"""Loot tables: folder naming, pool schema, rolls, weights, item refs."""

import re
import unittest

from audit_lib import (DATA, OWN_NAMESPACES, AuditCase, REPO, load_json, rel,
                       split_id, walk)

ITEM_ID = re.compile(r"^[a-z0-9_.-]+:[a-z0-9_./-]+$")


def loot_files():
    return [p for p in walk(DATA, (".json",))
            if "loot_table" in rel(p)]


class TestLoot(AuditCase, unittest.TestCase):

    def test_loot_tables_use_modern_directory(self):
        """Minecraft resolves Registries.LOOT_TABLE at data/<ns>/loot_table/.

        Files left in the legacy ``loot_tables`` folder are unreachable, so a
        reward that points at them silently drops nothing.
        """
        bad = [f"{rel(p)}: legacy 'loot_tables' folder is not loaded"
               for p in loot_files() if "/loot_tables/" in rel(p)]
        self.assertNoFindings(bad, "Loot tables in a dead directory")

    def test_every_loot_table_schema(self):
        bad = []
        files = loot_files()
        self.assertGreaterEqual(len(files), 10, "loot table corpus incomplete")
        for path in files:
            value, _d, err = load_json(path)
            if err or not isinstance(value, dict):
                bad.append(f"{rel(path)}: {err or 'root is not an object'}")
                continue
            pools = value.get("pools")
            if not isinstance(pools, list) or not pools:
                bad.append(f"{rel(path)}: no loot pools")
                continue
            for i, pool in enumerate(pools):
                prefix = f"{rel(path)} pool[{i}]"
                if not isinstance(pool, dict):
                    bad.append(f"{prefix}: not an object")
                    continue
                rolls = pool.get("rolls", 1)
                if isinstance(rolls, dict):
                    rolls = rolls.get("min", 1)
                if not isinstance(rolls, (int, float)) or rolls < 0:
                    bad.append(f"{prefix}: invalid rolls {rolls!r}")
                entries = pool.get("entries")
                if not isinstance(entries, list) or not entries:
                    bad.append(f"{prefix}: empty entries")
                    continue
                for j, entry in enumerate(entries):
                    where = f"{prefix} entry[{j}]"
                    if not isinstance(entry, dict):
                        bad.append(f"{where}: not an object")
                        continue
                    weight = entry.get("weight", 1)
                    if not isinstance(weight, (int, float)) or weight < 0:
                        bad.append(f"{where}: invalid weight {weight!r}")
                    name = entry.get("name")
                    if entry.get("type") == "minecraft:item":
                        if not isinstance(name, str) or not ITEM_ID.match(name):
                            bad.append(f"{where}: invalid item id {name!r}")
                    for func in entry.get("functions", []) or []:
                        if not isinstance(func, dict) or "function" not in func:
                            bad.append(f"{where}: invalid loot function")
                            continue
                        if func.get("function") == "minecraft:set_count":
                            count = func.get("count", 1)
                            if isinstance(count, int) and not 1 <= count <= 64:
                                bad.append(f"{where}: set_count out of range {count}")
        self.assertNoFindings(bad, "Malformed loot tables")

    def test_loot_items_are_registered(self):
        index = REPO.asset_index()
        bad = []
        checked = 0
        for path in loot_files():
            value, _d, err = load_json(path)
            if err or not isinstance(value, dict):
                continue
            for pool in value.get("pools", []):
                for entry in pool.get("entries", []):
                    name = entry.get("name") if isinstance(entry, dict) else None
                    if not isinstance(name, str):
                        continue
                    ns, item = split_id(name)
                    if ns not in OWN_NAMESPACES:
                        continue
                    checked += 1
                    if f"{ns}:items/{item}.json" not in index:
                        bad.append(f"{rel(path)}: unregistered loot item {name}")
        self.assertNoFindings(bad, "Loot tables referencing unknown items")

    def test_loot_table_reward_ids_have_files(self):
        """RewardBuilder.lootTable(DivineMissions.id(...)) ids must resolve."""
        known = set()
        for path in loot_files():
            r = rel(path)
            ns = r.split("/data/")[1].split("/")[0]
            name = path.name[:-len(".json")]
            # include the subfolder, mirroring the resource id
            tail = r.split("/loot_table/", 1)[-1][:-len(".json")]
            known.add(f"{ns}:{tail}")
        java = REPO.java_code()
        ids = set(re.findall(
            r'RewardBuilder\.lootTable\([^;]*?DivineMissions\.id\(\s*"([a-z0-9_/]+)"',
            java, flags=re.S))
        bad = [f"divine_missions:{i}: no loot table file"
               for i in sorted(ids) if f"divine_missions:{i}" not in known]
        self.assertGreater(len(ids), 0, "loot-table reward ids not found")
        self.assertNoFindings(bad, "Mission rewards with unresolved loot tables")


if __name__ == "__main__":
    unittest.main()
