#!/usr/bin/env python3
"""Every JSON/MCMETA resource: bytes, encoding, syntax, keys, filenames."""

import json
import re
import unittest
from pathlib import Path

from audit_lib import (ASSETS, COMMENT_KEY, RES, ROOT, AuditCase, load_json,
                       rel, walk)

CONTROL = re.compile(r"[\x00-\x1f\x7f]")
BAD_FILENAME = re.compile(r"[^a-z0-9_.-]")


def resource_json_files():
    files = [p for p in walk(RES) if p.suffix in (".json", ".mcmeta")]
    return files


def _keys_deep(value, path=""):
    """Yield (json_path, key) for every object key at any depth."""
    if isinstance(value, dict):
        for key, sub in value.items():
            here = f"{path}.{key}" if path else key
            yield here, key
            yield from _keys_deep(sub, here)
    elif isinstance(value, list):
        for i, sub in enumerate(value):
            yield from _keys_deep(sub, f"{path}[{i}]")


class TestJsonIntegrity(AuditCase, unittest.TestCase):

    def test_every_json_file_is_valid_utf8_json(self):
        bad = []
        for path in resource_json_files():
            # manual.json is a raw fragment consumed by CreateLangFile, not a
            # standalone language file; it is validated in test_manual_fragment.
            if path.name == "manual.json":
                continue
            value, dups, err = load_json(path, allow_bom=False)
            if err:
                bad.append(f"{rel(path)}: {err}")
        self.assertNoFindings(bad, "Unparsable / non-UTF8 JSON resources")

    def test_no_duplicate_keys_inside_any_object(self):
        bad = []
        for path in resource_json_files():
            if path.name == "manual.json":
                continue
            value, dups, err = load_json(path)
            if err:
                continue
            for key in dups:
                if key != COMMENT_KEY:
                    bad.append(f"{rel(path)}: duplicate key {key!r}")
        self.assertNoFindings(bad, "Duplicate JSON keys (last-wins data loss)")

    def test_no_empty_files_and_no_empty_json_containers(self):
        bad = []
        for path in resource_json_files():
            if path.name == "manual.json":
                continue
            if path.stat().st_size == 0:
                bad.append(f"{rel(path)}: empty file")
                continue
            value, _dups, err = load_json(path)
            if err:
                continue
            if value in ({}, [], None):
                bad.append(f"{rel(path)}: empty JSON container")
        self.assertNoFindings(bad, "Empty resources")

    def test_keys_are_clean(self):
        bad = []
        for path in resource_json_files():
            if path.name == "manual.json":
                continue
            # In a blockstate file, variants."" is the legal key for a block
            # with no state properties.
            is_blockstate = "/blockstates/" in rel(path)
            value, _dups, err = load_json(path)
            if err:
                continue
            for where, key in _keys_deep(value):
                if not isinstance(key, str) or not key:
                    if is_blockstate and where.startswith("variants."):
                        continue
                    bad.append(f"{rel(path)}: empty key at {where}")
                elif CONTROL.search(key):
                    bad.append(f"{rel(path)}: control character in key {key!r}")
                elif key != key.strip():
                    bad.append(f"{rel(path)}: padded key {key!r} at {where}")
        self.assertNoFindings(bad, "Malformed JSON keys")

    def test_resource_filenames_are_sane(self):
        bad = []
        for path in resource_json_files():
            name = path.name
            if ".." in name:
                bad.append(f"{rel(path)}: double dot in filename")
            if BAD_FILENAME.search(name):
                bad.append(f"{rel(path)}: unexpected characters in filename")
            if name.startswith("."):
                bad.append(f"{rel(path)}: hidden file inside resources")
        self.assertNoFindings(bad, "Invalid resource filenames")

    def test_no_unescaped_literals_in_translation_values(self):
        """A stray newline/tab byte inside a JSON string is a parse error."""
        bad = []
        for path in resource_json_files():
            if path.name == "manual.json":
                continue
            raw = path.read_bytes()
            if b"\x00" in raw:
                bad.append(f"{rel(path)}: NUL byte")
        self.assertNoFindings(bad, "Binary garbage in JSON resources")

    def test_manual_fragment_matches_en_us(self):
        """manual.json is prepended verbatim by CreateLangFile.create().

        If any of its keys or English values drift from en_us.json, the next
        regeneration silently rewrites shipped English strings.
        """
        manual = ASSETS / "mmorpg" / "lang" / "manual.json"
        text = manual.read_text(encoding="utf-8-sig").strip()
        if not text.startswith('"'):
            self.fail("manual.json must start with a quoted key")
        body = text.rstrip().rstrip(",")
        try:
            fragment = json.loads("{" + body + "\n}")
        except ValueError as exc:
            self.fail(f"manual.json is not a valid JSON fragment: {exc}")
        en, _d, err = load_json(ASSETS / "mmorpg" / "lang" / "en_us.json")
        self.assertIsNone(err, "en_us.json must parse")
        missing = [k for k in fragment if k not in en and k != COMMENT_KEY]
        drift = [k for k, v in fragment.items()
                 if k in en and k != COMMENT_KEY and en[k] != v]
        self.assertNoFindings(
            [f"manual.json: key absent from en_us.json: {k}" for k in missing]
            + [f"manual.json: {k} = {fragment[k]!r}, en_us = {en[k]!r}"
               for k in drift],
            "manual.json / en_us.json drift")

    def test_no_bom_anywhere(self):
        bad = []
        for path in resource_json_files():
            if path.read_bytes().startswith(b"\xef\xbb\xbf"):
                bad.append(f"{rel(path)}: UTF-8 BOM")
        self.assertNoFindings(bad, "BOM-prefixed resources")

    def test_no_hash_comments_in_json(self):
        bad = []
        for path in resource_json_files():
            first = path.read_bytes().decode("utf-8-sig", errors="ignore").lstrip()
            if first.startswith("#"):
                bad.append(f"{rel(path)}: shell-style comment before JSON body")
        self.assertNoFindings(bad, "Commented JSON resources")


if __name__ == "__main__":
    unittest.main()
