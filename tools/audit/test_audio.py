#!/usr/bin/env python3
"""Sounds: ogg containers, sounds.json events, airship voice banks."""

import unittest
from collections import defaultdict

from audit_lib import ASSETS, AuditCase, REPO, load_json, rel, split_id, walk

VALID_CATEGORIES = {"master", "music", "record", "weather", "block", "hostile",
                    "neutral", "player", "ambient", "voice", "ui"}


class TestAudio(AuditCase, unittest.TestCase):

    def test_every_ogg_has_a_valid_container_header(self):
        bad = []
        files = walk(ASSETS, (".ogg",))
        self.assertGreater(len(files), 100, "sound corpus looks incomplete")
        for path in files:
            with open(path, "rb") as handle:
                head = handle.read(4)
            if head != b"OggS":
                bad.append(f"{rel(path)}: not an Ogg container ({head!r})")
            if path.stat().st_size < 64:
                bad.append(f"{rel(path)}: truncated file ({path.stat().st_size} bytes)")
        self.assertNoFindings(bad, "Corrupt audio files")

    def test_sounds_json_events_resolve(self):
        index = REPO.asset_index()
        bad = []
        path = ASSETS / "mmorpg" / "sounds.json"
        value, _d, err = load_json(path)
        self.assertIsNone(err, f"sounds.json: {err}")
        self.assertIsInstance(value, dict)
        for event, spec in value.items():
            if not isinstance(spec, dict):
                bad.append(f"{event}: sound event is not an object")
                continue
            category = spec.get("category", "master")
            if category not in VALID_CATEGORIES:
                bad.append(f"{event}: unknown sound category {category!r}")
            entries = spec.get("sounds")
            if not isinstance(entries, list) or not entries:
                bad.append(f"{event}: empty sounds list")
                continue
            for entry in entries:
                name = entry.get("name") if isinstance(entry, dict) else entry
                if not isinstance(name, str):
                    bad.append(f"{event}: invalid sound entry {entry!r}")
                    continue
                ns, snd = split_id(name, "mmorpg")
                rid = f"{ns}:sounds/{snd}.ogg"
                if ns == "mmorpg" and rid not in index:
                    bad.append(f"{event}: missing ogg {rid}")
        self.assertNoFindings(bad, "Broken sound events")

    def test_sound_event_names_are_unique(self):
        value, _d, err = load_json(ASSETS / "mmorpg" / "sounds.json")
        self.assertIsNone(err)
        by_key = defaultdict(list)
        for event in value:
            by_key[event.lower()].append(event)
        bad = [f"case-colliding sound events: {v}" for v in by_key.values()
               if len(v) > 1]
        self.assertNoFindings(bad, "Duplicate sound events")

    def test_airship_voice_banks_resolve(self):
        index = REPO.asset_index()
        bad = []
        total = 0
        for name in ("adventure-voice.json", "crew-voice.json"):
            path = ASSETS / "mmorpg" / "airship" / name
            value, _d, err = load_json(path)
            if err or not isinstance(value, dict):
                bad.append(f"{rel(path)}: {err or 'not an object'}")
                continue
            for key, entry in value.items():
                if not isinstance(entry, dict):
                    bad.append(f"{rel(path)}: {key} is not an object")
                    continue
                for field in ("sound", "text", "durationMillis"):
                    if field not in entry:
                        bad.append(f"{rel(path)}: {key} missing {field}")
                duration = entry.get("durationMillis")
                if not isinstance(duration, int) or duration <= 0:
                    bad.append(f"{rel(path)}: {key} invalid durationMillis")
                if not str(entry.get("text", "")).strip():
                    bad.append(f"{rel(path)}: {key} empty subtitle")
                sound = entry.get("sound")
                if isinstance(sound, str):
                    total += 1
                    rid = f"mmorpg:sounds/airship/{sound}.ogg"
                    if rid not in index:
                        bad.append(f"{rel(path)}: {key} missing ogg {rid}")
        self.assertGreater(total, 80, "voice bank looks incomplete")
        self.assertNoFindings(bad, "Broken airship voice bank")


if __name__ == "__main__":
    unittest.main()
