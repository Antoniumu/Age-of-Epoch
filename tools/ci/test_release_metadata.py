#!/usr/bin/env python3
"""Release checks need no Java, credentials or network."""

import copy
import hashlib
import json
import tempfile
import unittest
from pathlib import Path
from zipfile import ZipFile

from release_metadata import verify_release


class ReleaseMetadataTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.jar = self.root / "Age-of-Epoch-5.2.6-beta.1.jar"
        self.props = self.root / "gradle.properties"
        self.props.write_text(
            "mod_version=5.2.6-beta.1\nminecraft_version=26.3\nmod_id=mmorpg\n"
            "archives_base_name=Age-of-Epoch\n", encoding="utf-8")
        self.checksums = self.root / "SHA256SUMS.txt"
        self.manifest = {
            "id": "mmorpg", "version": "5.2.6-beta.1",
            "depends": {"minecraft": "~26.3", "java": ">=25", "fabricloader": ">=0.19.5",
                        "fabric-api": ">=0.163.0", "trinkets_updated": ">=4.2.1"},
        }
        self.make_jar()

    def make_jar(self, manifest=None):
        with ZipFile(self.jar, "w") as archive:
            archive.writestr("fabric.mod.json", json.dumps(manifest or self.manifest))
        digest = hashlib.sha256(self.jar.read_bytes()).hexdigest()
        self.checksums.write_text(f"{digest}  {self.jar.name}\n", encoding="utf-8")

    def verify(self):
        return verify_release(self.jar, self.props, self.checksums)

    def test_valid_beta(self):
        metadata = self.verify()
        self.assertEqual(metadata["version"], "5.2.6-beta.1")
        self.assertEqual(metadata["minecraft"], "26.3")
        self.assertEqual(metadata["sha256"], hashlib.sha256(self.jar.read_bytes()).hexdigest())

    def test_rejects_sources_or_wrong_filename(self):
        for name in ("Age-of-Epoch-5.2.6-beta.1-sources.jar", "Age-of-Epoch-5.2.5.jar"):
            with self.subTest(name=name), self.assertRaises(ValueError):
                verify_release(self.root / name, self.props, self.checksums)

    def test_rejects_tampered_binary(self):
        with self.jar.open("ab") as stream:
            stream.write(b"tampered")
        with self.assertRaisesRegex(ValueError, "SHA-256 mismatch"):
            self.verify()

    def test_rejects_missing_duplicate_and_path_checksum_entries(self):
        original = self.checksums.read_text()
        for entries in ("", original * 2, original.replace(self.jar.name, "../" + self.jar.name)):
            with self.subTest(entries=entries), self.assertRaisesRegex(ValueError, "exactly one"):
                self.checksums.write_text(entries)
                self.verify()

    def test_rejects_wrong_manifest(self):
        for key, value in (("id", "another-mod"), ("version", "${version}")):
            with self.subTest(key=key), self.assertRaises(ValueError):
                manifest = copy.deepcopy(self.manifest)
                manifest[key] = value
                self.make_jar(manifest)
                self.verify()

    def test_rejects_wrong_game_java_or_missing_dependencies(self):
        for key, value in (("minecraft", "~26.1"), ("java", ">=21"),
                           ("fabricloader", None), ("fabric-api", None), ("trinkets_updated", None)):
            with self.subTest(key=key), self.assertRaises(ValueError):
                manifest = copy.deepcopy(self.manifest)
                if value is None:
                    del manifest["depends"][key]
                else:
                    manifest["depends"][key] = value
                self.make_jar(manifest)
                self.verify()


if __name__ == "__main__":
    unittest.main()
