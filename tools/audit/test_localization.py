#!/usr/bin/env python3
"""Localisation: filenames, key parity, placeholders, garbage values.

This is where the two headline bugs live: unique-gear names that fall back to
raw translation keys, and talent names that are missing entirely.
"""

import re
import unittest
from collections import Counter

from audit_lib import (ASSETS, COMMENT_KEY, PRIMARY_LOCALES, AuditCase,
                       REPO, load_json, rel)

LOCALE_NAME = re.compile(r"^[a-z]{2,3}_[a-z]{2,3}\.json$")
CYRILLIC = re.compile(r"[\u0400-\u04ff]")
PLACEHOLDER = re.compile(r"%(?:\d+\$)?[sdif]|\{\d?\}|§[0-9a-fk-or]")
STUBS = {"todo", "tbd", "fixme", "wip", "???", "null", "n/a", "none yet"}
SHORT_VALUE_WHITELIST = {
    "mmorpg.word.emptystring", "mmorpg.word.by", "mmorpg.word.to",
    "mmorpg.word.from", "mmorpg.word.none",
    # "%" is the complete translation of "Percent" in ko_kr, not a stub.
    "mmorpg.word.percent",
}
SECTION_CODE = re.compile(r"§([0-9a-fk-orA-FK-OR])")


def placeholder_counts(value):
    return Counter(m.group(0) for m in PLACEHOLDER.finditer(value)
                   if not m.group(0).startswith("§"))


class TestLocalization(AuditCase, unittest.TestCase):

    def _locales(self):
        """(namespace, locale, path, dict) for real locale files only."""
        out = []
        for ns, path in REPO.lang_files():
            if path.name == "manual.json":
                continue  # generator fragment, validated in test_json.py
            value, dups, err = load_json(path)
            out.append((ns, path.stem, path, value if isinstance(value, dict) else {},
                        dups, err))
        return out

    def test_locale_filenames(self):
        bad = []
        seen = set()
        for ns, path in REPO.lang_files():
            if path.name == "manual.json":
                continue
            if not LOCALE_NAME.fullmatch(path.name):
                bad.append(f"{rel(path)}: not a valid <lang>_<region>.json locale")
            key = (ns, path.stem)
            if key in seen:
                bad.append(f"{rel(path)}: duplicate locale entry")
            seen.add(key)
        self.assertNoFindings(bad, "Broken locale filenames")

    def test_all_locales_parse(self):
        bad = []
        for _ns, locale, path, _data, dups, err in self._locales():
            if err:
                bad.append(f"{rel(path)}: {err}")
            for key in dups:
                if key != COMMENT_KEY:
                    bad.append(f"{rel(path)}: duplicate key {key!r}")
        self.assertNoFindings(bad, "Locales that Minecraft cannot load")

    def test_primary_locales_have_identical_key_sets(self):
        """en_us is the source of truth; ru_ru must not miss or invent keys."""
        bad = []
        # One comparison per language directory, not once per file in it.
        langs = {path.parent for _ns, path in REPO.lang_files()}
        for parent in sorted(langs, key=str):
            en_path = parent / "en_us.json"
            if not en_path.exists():
                continue
            en, _d, err = load_json(en_path)
            if err:
                continue
            for locale in PRIMARY_LOCALES:
                p = parent / f"{locale}.json"
                if not p.exists():
                    bad.append(f"{p.parent.name}: missing {locale}.json")
                    continue
                data, _d, err = load_json(p)
                if err:
                    continue
                missing = sorted(set(en) - set(data))
                orphan = sorted(set(data) - set(en))
                for key in missing:
                    bad.append(f"{p.parent.name}/{locale}: missing key {key}")
                for key in orphan:
                    if key != COMMENT_KEY:
                        bad.append(f"{p.parent.name}/{locale}: orphan key {key}")
        self.assertNoFindings(bad, "en_us / ru_ru key parity")

    def test_every_locale_covers_all_english_keys(self):
        """A missing key in any shipped locale renders as a raw key in game.

        Extended locales (fr/ko/zh) carry English fallbacks, generated from
        en_us.json, so completeness is a hard invariant for every language.
        """
        bad = []
        for ns, path in REPO.lang_files():
            if path.name == "manual.json":
                continue
            en_path = path.parent / "en_us.json"
            if not en_path.exists():
                continue
            en, _d, err = load_json(en_path)
            if err:
                continue
            data, _d, err = load_json(path)
            if err:
                continue
            for key in sorted(set(en) - set(data)):
                if key == COMMENT_KEY:
                    continue
                bad.append(f"{rel(path)}: missing {key}")
        self.assertNoFindings(bad, "Incomplete locales (raw keys in game)")

    def test_placeholder_parity(self):
        bad = []
        for ns, path in REPO.lang_files():
            if path.name == "manual.json":
                continue
            en_path = path.parent / "en_us.json"
            if not en_path.exists() or path == en_path:
                continue
            en, _d, err = load_json(en_path)
            data, _d, err2 = load_json(path)
            if err or err2:
                continue
            for key, value in data.items():
                if key not in en or not isinstance(value, str):
                    continue
                if not isinstance(en[key], str):
                    continue
                a, b = placeholder_counts(en[key]), placeholder_counts(value)
                if a != b:
                    bad.append(f"{rel(path)}: {key}: en={dict(a)} local={dict(b)}")
        self.assertNoFindings(bad, "Placeholder mismatch (crashed formatting)")

    def test_no_raw_keys_or_stub_values(self):
        bad = []
        for ns, path in REPO.lang_files():
            if path.name == "manual.json":
                continue
            data, _d, err = load_json(path)
            if err:
                continue
            for key, value in data.items():
                if key == COMMENT_KEY or not isinstance(value, str):
                    continue
                stripped = value.strip()
                if stripped.lower() in STUBS:
                    bad.append(f"{rel(path)}: {key} = stub {value!r}")
                # A single CJK glyph is a complete word (秒 = "seconds",
                # 剑 = "sword"), not a truncated Latin string. Only flag
                # one-character ASCII values, which really are generator stubs.
                if (stripped and len(stripped) <= 1 and stripped.isascii()
                        and key not in SHORT_VALUE_WHITELIST):
                    bad.append(f"{rel(path)}: {key} = truncated value {value!r}")
                if re.fullmatch(r"[a-z][a-z0-9_.]*\.[a-z0-9_.]+", stripped) \
                        and stripped.startswith(("mmorpg.", "item.mmorpg", "block.mmorpg")):
                    bad.append(f"{rel(path)}: {key} = raw key {value!r}")
                if key == value:
                    bad.append(f"{rel(path)}: {key} = key itself")
        self.assertNoFindings(bad, "Stub / truncated / raw-key translations")

    def test_english_locale_contains_no_cyrillic(self):
        bad = []
        for ns, path in REPO.lang_files():
            if path.name != "en_us.json":
                continue
            data, _d, err = load_json(path)
            if err:
                continue
            for key, value in data.items():
                if isinstance(value, str) and CYRILLIC.search(value):
                    bad.append(f"{rel(path)}: {key} = {value!r}")
        self.assertNoFindings(bad, "Russian text leaking into the English locale")

    def test_section_codes_are_well_formed(self):
        bad = []
        for ns, path in REPO.lang_files():
            if path.name == "manual.json":
                continue
            data, _d, err = load_json(path)
            if err:
                continue
            for key, value in data.items():
                if not isinstance(value, str):
                    continue
                for i, char in enumerate(value):
                    if char == "§":
                        if i + 1 >= len(value) or not SECTION_CODE.match(value, i):
                            bad.append(f"{rel(path)}: {key}: invalid § code at {i}")
        self.assertNoFindings(bad, "Malformed colour/formatting codes")

    def test_unique_gear_names_exist_in_every_locale(self):
        """Regression guard for the reported 'unique item names' bug."""
        bad = []
        gears = REPO.unique_gears()
        self.assertGreaterEqual(len(gears), 20, "unique gear registry not parsed")
        for ns, path in REPO.lang_files():
            if ns != "mmorpg" or path.name == "manual.json":
                continue
            data, _d, err = load_json(path)
            if err:
                continue
            for gid, _name, _src in gears:
                key = f"mmorpg.unique_gear.{gid}.name"
                if key not in data or not str(data[key]).strip():
                    bad.append(f"{rel(path)}: missing unique name {key}")
        self.assertNoFindings(bad, "Unique gear names missing from locales")


if __name__ == "__main__":
    unittest.main()
