#!/usr/bin/env python3
"""Every crafting / smelting recipe: schema, patterns, item references."""

import re
import unittest
from collections import defaultdict

from audit_lib import (DATA, OWN_NAMESPACES, AuditCase, REPO, load_json, rel,
                       split_id, walk)

VALID_TYPES = {"minecraft:crafting_shaped", "minecraft:crafting_shapeless",
               "minecraft:smelting", "minecraft:blasting", "minecraft:smoking",
               "minecraft:campfire_cooking", "minecraft:stonecutting",
               "minecraft:smithing_transform"}
ITEM_ID = re.compile(r"^[a-z0-9_.-]+:[a-z0-9_./-]+$")
TAG_ID = re.compile(r"^#[a-z0-9_.-]+:[a-z0-9_./-]+$")


def valid_ingredient(item):
    return isinstance(item, str) and (ITEM_ID.match(item) or TAG_ID.match(item))


def recipes():
    for path in walk(DATA, (".json",)):
        if "/recipe/" in rel(path):
            yield path


class TestRecipes(AuditCase, unittest.TestCase):

    def test_all_recipes_parse_and_have_a_type(self):
        bad = []
        count = 0
        for path in recipes():
            value, _d, err = load_json(path)
            if err:
                bad.append(f"{rel(path)}: {err}")
                continue
            count += 1
            if not isinstance(value, dict):
                bad.append(f"{rel(path)}: root is not an object")
                continue
            if value.get("type") not in VALID_TYPES:
                bad.append(f"{rel(path)}: unknown recipe type {value.get('type')!r}")
        self.assertGreater(count, 80, "recipe corpus looks incomplete")
        self.assertNoFindings(bad, "Invalid recipes")

    def test_shaped_recipe_patterns(self):
        bad = []
        for path in recipes():
            value, _d, err = load_json(path)
            if err or not isinstance(value, dict):
                continue
            if value.get("type") != "minecraft:crafting_shaped":
                continue
            pattern, key = value.get("pattern"), value.get("key")
            if not isinstance(pattern, list) or not pattern:
                bad.append(f"{rel(path)}: missing pattern")
                continue
            if not isinstance(key, dict) or not key:
                bad.append(f"{rel(path)}: missing key map")
                continue
            if any(not isinstance(row, str) for row in pattern):
                bad.append(f"{rel(path)}: non-string pattern row")
                continue
            if len({len(row) for row in pattern}) != 1:
                bad.append(f"{rel(path)}: ragged pattern rows")
            if not 1 <= len(pattern) <= 3 or not 1 <= len(pattern[0]) <= 3:
                bad.append(f"{rel(path)}: pattern must be at most 3x3")
            used = {ch for row in pattern for ch in row if ch != " "}
            unknown = used - set(key)
            unused = set(key) - used
            if unknown:
                bad.append(f"{rel(path)}: pattern symbols without key: {sorted(unknown)}")
            if unused:
                bad.append(f"{rel(path)}: unused key entries: {sorted(unused)}")
        self.assertNoFindings(bad, "Malformed shaped recipes")

    def test_ingredients_and_results_are_well_formed(self):
        bad = []
        for path in recipes():
            value, _d, err = load_json(path)
            if err or not isinstance(value, dict):
                continue
            rtype = value.get("type")
            raw_ingredients = []
            if rtype == "minecraft:crafting_shapeless":
                raw_ingredients = list(value.get("ingredients", []))
            elif rtype == "minecraft:crafting_shaped":
                raw_ingredients = list((value.get("key") or {}).values())
            elif "ingredient" in value:
                raw_ingredients = [value["ingredient"]]
            if not raw_ingredients:
                bad.append(f"{rel(path)}: no ingredients")
            for item in raw_ingredients:
                if isinstance(item, dict):
                    item = item.get("item") or item.get("id") or item.get("tag")
                if not valid_ingredient(item):
                    bad.append(f"{rel(path)}: bad ingredient {item!r}")
            result = value.get("result")
            if isinstance(result, str):
                result = {"id": result}
            if not isinstance(result, dict) or not isinstance(result.get("id"), str):
                bad.append(f"{rel(path)}: missing result id")
            else:
                if not ITEM_ID.match(result["id"]):
                    bad.append(f"{rel(path)}: bad result id {result['id']!r}")
                count = result.get("count", 1)
                if not isinstance(count, int) or not 1 <= count <= 64:
                    bad.append(f"{rel(path)}: result count out of range: {count}")
            if "cookingtime" in value and (not isinstance(value["cookingtime"], int)
                                          or value["cookingtime"] <= 0):
                bad.append(f"{rel(path)}: invalid cookingtime")
            if "experience" in value and (not isinstance(value["experience"],
                                                        (int, float))
                                         or value["experience"] < 0):
                bad.append(f"{rel(path)}: negative experience")
        self.assertNoFindings(bad, "Malformed recipe ingredients/results")

    def test_own_item_ids_have_item_definitions(self):
        index = REPO.asset_index()
        bad = []
        checked = 0
        for path in recipes():
            value, _d, err = load_json(path)
            if err or not isinstance(value, dict):
                continue
            ids = []
            for item in value.get("ingredients", []):
                if isinstance(item, str):
                    ids.append(item)
            for item in (value.get("key") or {}).values():
                if isinstance(item, str):
                    ids.append(item)
            result = value.get("result")
            if isinstance(result, dict) and isinstance(result.get("id"), str):
                ids.append(result["id"])
            for item in ids:
                ns, item_path = split_id(item)
                if ns not in OWN_NAMESPACES:
                    continue
                checked += 1
                rid = f"{ns}:items/{item_path}.json"
                if rid not in index:
                    bad.append(f"{rel(path)}: no item definition for {item} ({rid})")
        self.assertGreater(checked, 80, "recipe item corpus looks incomplete")
        self.assertNoFindings(bad, "Recipes referencing unregistered items")

    def test_no_recipe_crafts_itself(self):
        bad = []
        for path in recipes():
            value, _d, err = load_json(path)
            if err or not isinstance(value, dict):
                continue
            result = value.get("result")
            result_id = result.get("id") if isinstance(result, dict) else result
            ingredients = set()
            for item in value.get("ingredients", []):
                if isinstance(item, str):
                    ingredients.add(item)
            for item in (value.get("key") or {}).values():
                if isinstance(item, str):
                    ingredients.add(item)
            if result_id in ingredients:
                bad.append(f"{rel(path)}: result {result_id} is also an ingredient")
        self.assertNoFindings(bad, "Self-referential recipes")

    def test_recipe_ids_are_unique_per_folder(self):
        seen = defaultdict(list)
        for path in recipes():
            seen[rel(path).lower()].append(rel(path))
        bad = [f"duplicate recipe path: {paths}" for paths in seen.values()
               if len(paths) > 1]
        self.assertNoFindings(bad, "Duplicate recipes")


if __name__ == "__main__":
    unittest.main()
