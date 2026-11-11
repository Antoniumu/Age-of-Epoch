#!/usr/bin/env python3
"""Java-side generators and source hygiene.

Several bugs were originally *generated* rather than written by hand: a Russian
word in ``Words.java`` and a one-letter stat description in
``CraftingSuccessChance.java`` were copied into every locale by
``CreateLangFile``. Fixing only the JSON would let the next regeneration
silently reintroduce them, so the sources are audited here too.
"""

import re
import unittest

from audit_lib import JAVA, ROOT, TOOLS, AuditCase, rel, walk

CYRILLIC = re.compile(r"[\u0400-\u04ff]")
LANG_GENERATORS = (
    JAVA / "com" / "robertx22" / "age_of_exile" / "uncommon" /
    "localization" / "Words.java",
)
LOC_METHOD = re.compile(
    r'String\s+(locDescForLangFile|locNameForLangFile)\s*\([^)]*\)\s*\{'
    r'(.{0,400}?)\}', re.S)
RETURN_LITERAL = re.compile(r'return\s+"([^"\\]*)"\s*;')


class TestJava(AuditCase, unittest.TestCase):

    def test_english_word_source_contains_no_russian(self):
        bad = []
        for path in LANG_GENERATORS:
            if not path.is_file():
                bad.append(f"missing English word source {rel(path)}")
                continue
            for number, line in enumerate(
                    path.read_text(encoding="utf-8").splitlines(), 1):
                if CYRILLIC.search(line):
                    bad.append(f"{rel(path)}:{number}: {line.strip()[:100]}")
        self.assertNoFindings(bad, "Russian text in the English word generator")

    def test_loc_file_methods_do_not_return_stubs(self):
        """A one-character literal is a truncated generator string.

        An *empty* string is fine: datapack stats deliberately return "" to
        say "no description" and build their tooltip from the stat list.
        """
        bad = []
        for path in walk(JAVA, (".java",)):
            text = path.read_text(encoding="utf-8", errors="ignore")
            for match in LOC_METHOD.finditer(text):
                literal = RETURN_LITERAL.search(match.group(2))
                if literal and len(literal.group(1)) == 1:
                    bad.append(f"{rel(path)}: {match.group(1)}() returns "
                               f"{literal.group(1)!r}")
        self.assertNoFindings(bad, "Truncated Java-generated translations")

    def test_no_java_writes_to_legacy_loot_folder(self):
        bad = []
        for path in walk(JAVA, (".java",)):
            text = path.read_text(encoding="utf-8", errors="ignore")
            if "loot_tables/" in text or '"/loot_tables"' in text:
                bad.append(f"{rel(path)}: writes to dead 'loot_tables' folder")
        self.assertNoFindings(bad, "Generators targeting a dead directory")

    def test_java_sources_are_clean_utf8(self):
        files = walk(JAVA, (".java",))
        self.assertGreater(len(files), 1000, "Java corpus looks incomplete")
        bad = []
        for path in files:
            raw = path.read_bytes()
            if raw.startswith(b"\xef\xbb\xbf"):
                bad.append(f"{rel(path)}: UTF-8 BOM")
            if b"\x00" in raw:
                bad.append(f"{rel(path)}: NUL byte")
            try:
                raw.decode("utf-8")
            except UnicodeDecodeError as exc:
                bad.append(f"{rel(path)}: not UTF-8 ({exc})")
        self.assertNoFindings(bad, "Malformed Java sources")

    def test_lang_file_generator_still_uses_manual_fragment(self):
        """manual.json is a raw fragment, not standalone JSON.

        If the generator stops prepending ``DirUtils.getManualString()`` the
        locale tests would still pass while the shipped en_us loses every
        hand-written override.
        """
        path = (JAVA / "com" / "robertx22" / "age_of_exile" / "aoe_data" /
                "datapacks" / "lang_file" / "CreateLangFile.java")
        self.assertTrue(path.is_file(), f"missing {rel(path)}")
        text = path.read_text(encoding="utf-8", errors="ignore")
        self.assertIn("getManualString()", text,
                      "CreateLangFile no longer includes manual.json")
        dirs = (JAVA / "com" / "robertx22" / "age_of_exile" /
                "uncommon" / "utilityclasses" / "DirUtils.java")
        self.assertIn("manual.json",
                      dirs.read_text(encoding="utf-8", errors="ignore"),
                      "DirUtils no longer points at manual.json")

    def test_lang_generator_covers_every_named_registry(self):
        """Every ExileDB registry with its own display name must be generated.

        ``CreateLangFile.create()`` rewrites en_us.json from scratch. A registry
        omitted from ``getMap()`` therefore loses *all* of its translations on
        the next regeneration, no matter how carefully the JSON was patched.
        SpellSchools() was exactly such an omission: the talent tree tabs showed
        raw ``mmorpg.spell_school.*`` keys.
        """
        exiledb = (JAVA / "com" / "robertx22" / "age_of_exile" / "database" /
                   "registry" / "ExileDB.java")
        generator = (JAVA / "com" / "robertx22" / "age_of_exile" /
                     "aoe_data" / "datapacks" / "lang_file" /
                     "CreateLangFile.java")
        self.assertTrue(exiledb.is_file() and generator.is_file(),
                        "ExileDB.java / CreateLangFile.java not found")
        registries = re.findall(
            r'public static ExileRegistryContainer<(\w+)>\s+(\w+)\s*\(',
            exiledb.read_text(encoding="utf-8", errors="ignore"))
        self.assertGreater(len(registries), 20, "registry corpus incomplete")

        gen_text = generator.read_text(encoding="utf-8", errors="ignore")
        marker = "public static HashMap<String, List<IAutoLocName>> getMap()"
        end = "public static HashMap<String, List<IAutoLocDesc>> getDescMap()"
        self.assertIn(marker, gen_text, "getMap() signature changed")
        body = gen_text.split(marker, 1)[1].split(end, 1)[0]
        covered = set(re.findall(r'ExileDB\.(\w+)\(\)', body))

        bad, named = [], 0
        for cls, method in registries:
            sources = [p for p in JAVA.rglob(cls + ".java")
                       if "com/robertx22" in rel(p).replace("\\", "/")]
            # Only registries that declare their own locNameForLangFile() need
            # a generated key; the rest are named by vanilla item/block keys.
            if not any("IAutoLocName" in p.read_text(encoding="utf-8",
                                                    errors="ignore") and
                       "locNameForLangFile(" in p.read_text(
                           encoding="utf-8", errors="ignore")
                       for p in sources):
                continue
            named += 1
            if method not in covered:
                bad.append(f"CreateLangFile.getMap(): ExileDB.{method}() "
                           f"({cls}) is never added; its names disappear on "
                           f"the next lang regeneration")
        self.assertGreater(named, 10, "named registry corpus incomplete")
        self.assertNoFindings(bad, "Lang generator drops whole registries")

    def test_e2e_runner_reports_failures_to_ci(self):
        path = TOOLS / "e2e" / "run_scenario.py"
        text = path.read_text(encoding="utf-8", errors="ignore")
        self.assertIn("not self.failures", text,
                      "run_scenario.py ignores recorded failures again; "
                      "a red E2E run would be reported green to CI")

    def test_junit_test_sources_are_present(self):
        tests = walk(ROOT / "src" / "test" / "java", (".java",))
        self.assertGreater(len(tests), 20,
                           "JUnit test corpus looks incomplete")


if __name__ == "__main__":
    unittest.main()
