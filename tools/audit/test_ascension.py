#!/usr/bin/env python3
"""Regression coverage for the ascension release's missing status effect slots."""

import importlib.util
import re
import unittest

from audit_lib import JAVA, TOOLS, AuditCase

BASE = JAVA / "com/robertx22/age_of_exile"


class TestAscension(AuditCase, unittest.TestCase):
    def test_every_effect_context_has_a_registered_vanilla_slot(self):
        potions = (BASE / "mmorpg/registers/common/SlashPotions.java").read_text()
        registered = set()
        for limit, kind in re.findall(
                r"for \(int i = 0; i < (\d+); i\+\+\)\s*\{\s*"
                r"registerExileEffect\(EffectType\.(\w+), i\);", potions):
            registered.update((kind, n) for n in range(int(limit)))
        registered.update((kind, int(number)) for kind, number in re.findall(
            r"registerExileEffect\(EffectType\.(\w+), (\d+)\)", potions))
        self.assertGreater(len(registered), 50, "Could not read legacy potion slot registration")

        declared = []
        for path in BASE.rglob("*.java"):
            for number, kind in re.findall(
                    r'new EffectCtx\("[^"\n]+",\s*"[^"\n]+",\s*(\d+),\s*'
                    r'Elements\.\w+,\s*EffectType\.(beneficial|negative)\)', path.read_text()):
                declared.append((kind, int(number)))
        self.assertGreater(len(declared), 35, "Effect context corpus looks incomplete")
        self.assertEqual(len(declared), len(set(declared)), "Duplicate effect context slots")
        self.assertFalse(set(declared) - registered,
                         f"Effects missing vanilla registry slots: {set(declared) - registered}")

    def test_structural_audit_includes_all_six_ascension_skills(self):
        spec = importlib.util.spec_from_file_location("check_spells", TOOLS / "verify/check_spells.py")
        scanner = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(scanner)
        expected = {"tidewhip_lash", "slipstream_surge", "jubilant_veil",
                    "unbound_joy", "undertow_grasp", "maelstrom_waltz"}
        self.assertEqual(scanner.collect_ascension_skills(), expected)
        result = scanner.audit()
        rows = {row["id"]: row for row in result["spells"]}
        for skill in expected:
            with self.subTest(skill=skill):
                self.assertIn(skill, rows)
                self.assertTrue(rows[skill]["ascension"])
                self.assertFalse(rows[skill]["problems"])
                self.assertGreater(rows[skill]["channels"].get("particles", 0), 0)
        self.assertEqual(result["errors"], [])

    def test_live_probe_recognizes_custom_particle_action(self):
        probe = (BASE / "uncommon/testing/SpellAuditProbe.java").read_text()
        self.assertRegex(probe, r"matches\(type, SpellAction\.TIDE_SHAPE\)\)\s*\{\s*"
                               r"result\.particles = true;")


if __name__ == "__main__":
    unittest.main()
