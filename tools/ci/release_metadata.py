#!/usr/bin/env python3
"""Verify the exact installable release JAR before publishing it elsewhere.

No credentials or network access. SHA256SUMS entries are treated as data, never
as paths to open: only the selected JAR's basename may be verified.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
from pathlib import Path
from zipfile import ZipFile

VERSION = re.compile(r"[0-9]+\.[0-9]+\.[0-9]+(?:-[0-9A-Za-z.-]+)?")


def verify_release(jar: Path, properties: Path, checksums: Path) -> dict[str, str]:
    props = {}
    for line in properties.read_text(encoding="utf-8").splitlines():
        key, sep, value = line.partition("=")
        if sep and not key.lstrip().startswith("#"):
            props[key.strip()] = value.strip()

    version = props["mod_version"]
    minecraft = props["minecraft_version"]
    if not VERSION.fullmatch(version):
        raise ValueError("Invalid mod_version")
    if not re.fullmatch(r"[0-9A-Za-z._-]+", minecraft):
        raise ValueError("Invalid minecraft_version")
    expected_name = f'{props["archives_base_name"]}-{version}.jar'
    if jar.name != expected_name:
        raise ValueError(f"Expected the installable JAR {expected_name}, not {jar.name}")

    hashes = []
    for line in checksums.read_text(encoding="utf-8").splitlines():
        match = re.fullmatch(r"([a-fA-F0-9]{64}) [ *](.+)", line)
        if match and match.group(2) == jar.name:
            hashes.append(match.group(1).lower())
    if len(hashes) != 1:
        raise ValueError("SHA256SUMS must contain exactly one entry for the selected JAR")
    with jar.open("rb") as stream:
        digest = hashlib.file_digest(stream, "sha256").hexdigest()
    if digest != hashes[0]:
        raise ValueError("Release JAR SHA-256 mismatch")

    with ZipFile(jar) as archive:
        manifest = json.loads(archive.read("fabric.mod.json"))
    if manifest.get("id") != props["mod_id"] or manifest.get("version") != version:
        raise ValueError("Release JAR mod ID/version does not match the source tag")
    depends = manifest.get("depends", {})
    if depends.get("minecraft") != f"~{minecraft}":
        raise ValueError("Release JAR targets a different Minecraft version")
    if depends.get("java") != ">=25" or "fabricloader" not in depends:
        raise ValueError("Release JAR must target Fabric and Java 25")
    if "fabric-api" not in depends or "trinkets_updated" not in depends:
        raise ValueError("Release JAR is missing its required dependency declarations")

    return {"version": version, "minecraft": minecraft, "jar": jar.name, "sha256": digest}


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("jar", type=Path)
    parser.add_argument("--properties", type=Path, default=Path("gradle.properties"))
    parser.add_argument("--checksums", type=Path, required=True)
    args = parser.parse_args()
    metadata = verify_release(args.jar, args.properties, args.checksums)
    print(json.dumps(metadata, ensure_ascii=False))
    if output := os.environ.get("GITHUB_OUTPUT"):
        with open(output, "a", encoding="utf-8") as stream:
            for key, value in metadata.items():
                stream.write(f"{key}={value}\n")


if __name__ == "__main__":
    main()
