#!/usr/bin/env python3
"""Build metadata: fabric.mod.json, gradle.properties, entrypoints, mixins.

A wrong entrypoint or a renamed mixin class does not fail ``javac`` — the mod
simply crashes at startup. These tests catch that class of bug before a build.
"""

import json
import unittest

from audit_lib import JAVA, RES, ROOT, AuditCase, load_json, rel

PROPS = ROOT / "gradle.properties"
MOD_JSON = RES / "fabric.mod.json"
MCMETA = RES / "pack.mcmeta"


def gradle_properties():
    out = {}
    for line in PROPS.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        out[key.strip()] = value.strip()
    return out


def class_path(fqcn):
    return JAVA / (fqcn.replace(".", "/") + ".java")


class TestBuild(AuditCase, unittest.TestCase):

    def test_mod_metadata_matches_gradle_properties(self):
        value, _dups, err = load_json(MOD_JSON)
        self.assertIsNone(err, f"fabric.mod.json: {err}")
        props = gradle_properties()
        bad = []
        if value.get("id") != props.get("mod_id"):
            bad.append(f"fabric.mod.json id {value.get('id')!r} != "
                       f"gradle.properties mod_id {props.get('mod_id')!r}")
        # Loom substitutes ${version} from mod_version at build time.
        if value.get("version") != "${version}":
            bad.append(f"version must be the '${{version}}' placeholder, "
                       f"got {value.get('version')!r}")
        icon = value.get("icon")
        if not isinstance(icon, str) or not (RES / icon).is_file():
            bad.append(f"icon asset is missing: {icon!r}")
        depends = value.get("depends") or {}
        mc = props.get("minecraft_version", "")
        loader = props.get("loader_version", "")
        api = props.get("fabric_api_version", "").split("+")[0]
        for key, expected in (("minecraft", mc), ("fabricloader", loader),
                              ("fabric-api", api)):
            if expected and expected not in str(depends.get(key, "")):
                bad.append(f"depends.{key}={depends.get(key)!r} does not match "
                           f"gradle.properties ({expected})")
        self.assertNoFindings(bad, "Mod metadata is out of sync with the build")

    def test_every_entrypoint_class_exists(self):
        value, _dups, err = load_json(MOD_JSON)
        self.assertIsNone(err)
        bad, count = [], 0
        for group, classes in (value.get("entrypoints") or {}).items():
            for fqcn in classes:
                count += 1
                if not class_path(fqcn).is_file():
                    bad.append(f"entrypoint {group}: missing class {fqcn}")
        self.assertGreater(count, 0, "no entrypoints declared")
        self.assertNoFindings(bad, "Broken Fabric entrypoints")

    def test_mixin_configs_and_classes_resolve(self):
        value, _dups, err = load_json(MOD_JSON)
        self.assertIsNone(err)
        configs = value.get("mixins") or []
        self.assertGreater(len(configs), 0, "no mixin configs declared")
        bad, count = [], 0
        for cfg in configs:
            path = RES / cfg
            if not path.is_file():
                bad.append(f"fabric.mod.json: missing mixin config {cfg}")
                continue
            conf, _d, cerr = load_json(path)
            if cerr:
                bad.append(f"{rel(path)}: {cerr}")
                continue
            package = conf.get("package", "")
            if not package:
                bad.append(f"{rel(path)}: no package")
                continue
            prefix = package.replace(".", "/")
            for section in ("mixins", "client", "server"):
                for name in conf.get(section, []) or []:
                    count += 1
                    if not (JAVA / prefix / (name + ".java")).is_file():
                        bad.append(f"{cfg} [{section}]: missing class {name}")
        self.assertGreater(count, 10, "mixin corpus looks incomplete")
        self.assertNoFindings(bad, "Broken mixin configuration")

    def test_pack_mcmeta_is_sane(self):
        value, _dups, err = load_json(MCMETA)
        self.assertIsNone(err, f"pack.mcmeta: {err}")
        pack = value.get("pack") or {}
        fmt = pack.get("pack_format")
        lo, hi = pack.get("min_format", fmt), pack.get("max_format", fmt)
        bad = []
        if not isinstance(fmt, int) or not 1 <= fmt <= 999:
            bad.append(f"implausible pack_format {fmt!r}")
        elif not (isinstance(lo, int) and isinstance(hi, int) and lo <= fmt <= hi):
            bad.append(f"pack_format {fmt} is outside [{lo}, {hi}]")
        if not str(pack.get("description", "")).strip():
            bad.append("empty pack description")
        self.assertNoFindings(bad, "Invalid pack.mcmeta")

    def test_accesswidener_is_intentionally_unwired(self):
        """The AW is a documented reference file, not an active config.

        Wiring it into fabric.mod.json without a Loom ``accessWidenerPath``
        would make the game refuse to load the mod. Guard the decision.
        """
        mod = MOD_JSON.read_text(encoding="utf-8")
        aw = (RES / "mmorpg.accesswidener").read_text(encoding="utf-8")
        self.assertIn("intentionally NOT wired up", aw,
                      "accesswidener comment changed: re-check the Loom setup")
        self.assertNotIn("accessWidener", mod,
                         "fabric.mod.json now declares an accessWidener; "
                         "build.gradle must configure accessWidenerPath too")


if __name__ == "__main__":
    unittest.main()
