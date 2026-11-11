#!/usr/bin/env python3
"""Visual resources: PNGs, models, blockstates, item definitions, particles."""

import json
import re
import unittest
from pathlib import Path

from audit_lib import (ASSETS, JAVA, OWN_NAMESPACES, AuditCase, REPO,
                       load_json, model_id, png_size, rel, split_id,
                       texture_id, walk)


def under(root: Path, segment: str, suffix=".json"):
    """Files directly inside <namespace>/<segment>/... ."""
    out = []
    for path in walk(root, (suffix,)):
        try:
            tail = path.relative_to(root).as_posix()
        except ValueError:
            continue
        parts = tail.split("/")
        if len(parts) >= 2 and parts[1] == segment:
            out.append((parts[0], path, tail))
    return out


class TestAssets(AuditCase, unittest.TestCase):

    def test_every_png_is_a_real_image(self):
        bad = []
        pngs = walk(ASSETS, (".png",))
        self.assertGreater(len(pngs), 1000, "texture corpus looks incomplete")
        for path in pngs:
            size = png_size(path)
            if size is None:
                bad.append(f"{rel(path)}: broken PNG signature")
            elif size[0] == 0 or size[1] == 0 or size[0] > 8192 or size[1] > 8192:
                bad.append(f"{rel(path)}: implausible dimensions {size}")
        self.assertNoFindings(bad, "Corrupt textures")

    def test_mcmeta_files_point_at_real_textures(self):
        bad = []
        for path in walk(ASSETS, (".mcmeta",)):
            value, _d, err = load_json(path)
            if err:
                bad.append(f"{rel(path)}: {err}")
                continue
            target = Path(str(path)[:-len(".mcmeta")])
            if not target.exists():
                bad.append(f"{rel(path)}: no matching texture {target.name}")
            anim = value.get("animation") if isinstance(value, dict) else None
            if isinstance(anim, dict):
                if not isinstance(anim.get("frametime", 1), int) or \
                        anim.get("frametime", 1) <= 0:
                    bad.append(f"{rel(path)}: invalid frametime")
                frames = anim.get("frames", [])
                if not isinstance(frames, list) or any(
                        not isinstance(f, int) or f < 0 for f in frames):
                    bad.append(f"{rel(path)}: invalid animation frame list")
        self.assertNoFindings(bad, "Broken texture metadata")

    @staticmethod
    def _model_ids(node):
        """Collect every model id in an item-definition tree.

        Since 1.21.4 an item definition can be a ``range_dispatch`` /
        ``select`` tree with models in ``fallback`` and ``entries``, not just a
        flat ``{"model": "..."}`` object.
        """
        if isinstance(node, dict):
            for key, value in node.items():
                if key == "model" and isinstance(value, str):
                    yield value
                else:
                    yield from TestAssets._model_ids(value)
        elif isinstance(node, list):
            for value in node:
                yield from TestAssets._model_ids(value)

    def test_item_definitions_reference_existing_models(self):
        index = REPO.asset_index()
        bad = []
        count = 0
        for ns, path, _tail in under(ASSETS, "items"):
            value, _d, err = load_json(path)
            if err:
                bad.append(f"{rel(path)}: {err}")
                continue
            model = value.get("model") if isinstance(value, dict) else None
            if not isinstance(model, dict):
                bad.append(f"{rel(path)}: missing model definition")
                continue
            targets = sorted(set(self._model_ids(model)))
            if not targets:
                bad.append(f"{rel(path)}: model has no resource id")
                continue
            for target in targets:
                count += 1
                if model_id(target, ns) not in index:
                    bad.append(f"{rel(path)}: missing model {target}")
        self.assertGreater(count, 250, "item definition corpus looks incomplete")
        self.assertNoFindings(bad, "Broken item model references")

    def test_blockstates_reference_existing_models(self):
        index = REPO.asset_index()
        bad = []
        count = 0
        for ns, path, _tail in under(ASSETS, "blockstates"):
            value, _d, err = load_json(path)
            if err:
                bad.append(f"{rel(path)}: {err}")
                continue
            for target in re.findall(r'"model"\s*:\s*"([^"]+)"',
                                    json.dumps(value)):
                count += 1
                if model_id(target, ns) not in index:
                    bad.append(f"{rel(path)}: missing model {target}")
        self.assertGreater(count, 90, "blockstate corpus looks incomplete")
        self.assertNoFindings(bad, "Broken blockstate model references")

    def test_models_reference_existing_own_textures(self):
        index = REPO.asset_index()
        bad = []
        count = 0
        for ns, path, _tail in under(ASSETS, "models"):
            value, _d, err = load_json(path)
            if err or not isinstance(value, dict):
                continue
            for tex in (value.get("textures") or {}).values():
                if not isinstance(tex, str) or tex.startswith("#"):
                    continue
                # A resource location without a namespace resolves against
                # "minecraft", NOT against the namespace of the model file.
                # Vanilla parents such as "block/nether_portal" are external.
                ns2, _ = split_id(tex, "minecraft")
                if ns2 not in OWN_NAMESPACES:
                    continue  # vanilla / external texture
                count += 1
                if texture_id(tex, "minecraft") not in index:
                    bad.append(f"{rel(path)}: missing texture {tex}")
        self.assertGreater(count, 300, "model texture corpus looks incomplete")
        self.assertNoFindings(bad, "Models pointing at missing textures")

    def test_model_parents_resolve(self):
        index = REPO.asset_index()
        bad = []
        for ns, path, _tail in under(ASSETS, "models"):
            value, _d, err = load_json(path)
            if err or not isinstance(value, dict):
                continue
            parent = value.get("parent")
            if not isinstance(parent, str):
                continue
            pns, _ = split_id(parent, "minecraft")
            if pns not in OWN_NAMESPACES:
                continue  # vanilla / builtin parent
            if model_id(parent, pns) not in index:
                bad.append(f"{rel(path)}: missing parent model {parent}")
        self.assertNoFindings(bad, "Broken model inheritance")

    def test_particle_textures_exist(self):
        index = REPO.asset_index()
        bad = []
        count = 0
        for ns, path, _tail in under(ASSETS, "particles"):
            value, _d, err = load_json(path)
            if err or not isinstance(value, dict):
                bad.append(f"{rel(path)}: {err or 'not an object'}")
                continue
            textures = value.get("textures")
            if not isinstance(textures, list) or not textures:
                bad.append(f"{rel(path)}: empty particle texture list")
                continue
            for tex in textures:
                if not isinstance(tex, str):
                    bad.append(f"{rel(path)}: non-string texture {tex!r}")
                    continue
                count += 1
                pns, ppath = split_id(tex, "minecraft")
                if pns not in OWN_NAMESPACES:
                    continue  # vanilla sprite (e.g. minecraft:drip_hang)
                candidate = f"{pns}:textures/particle/{ppath}.png"
                if candidate not in index:
                    bad.append(f"{rel(path)}: missing particle texture {tex} "
                               f"({candidate})")
        self.assertGreater(count, 10, "particle corpus looks incomplete")
        self.assertNoFindings(bad, "Particles with missing sprites")

    def test_bar_overlay_icons_exist(self):
        """BarGuiType.getIcon() derives ``<name>_icon.png`` from ``<name>.png``.

        MineAndSlashBars binds that derived id for every rendered bar, so a bar
        shipped without its icon shows the purple/black missing sprite in the
        HUD even though the fill texture itself is present.
        """
        path = (JAVA / "com" / "robertx22" / "age_of_exile" / "gui" /
                "overlays" / "BarGuiType.java")
        text = path.read_text(encoding="utf-8", errors="ignore")
        bars = set(re.findall(
            r'SlashRef\.id\(\s*"(textures/gui/overlay/[^"]+\.png)"', text))
        self.assertGreater(len(bars), 5, "bar overlay corpus looks incomplete")
        index = REPO.asset_index()
        bad = []
        for bar in sorted(bars):
            rid = f"mmorpg:{bar}"
            if rid not in index:
                bad.append(f"BarGuiType: missing bar texture {rid}")
                continue
            icon = f"mmorpg:{bar[:-4]}_icon.png"
            if icon not in index:
                bad.append(f"BarGuiType: {rid} has no derived icon {icon}")
        self.assertNoFindings(bad, "HUD bars with missing sprites")

    def test_java_gui_and_icon_textures_exist(self):
        index = REPO.asset_index()
        bad = []
        refs = REPO.java_texture_refs()
        self.assertGreater(len(refs), 150, "Java texture corpus looks incomplete")
        for rid in sorted(refs):
            if rid not in index:
                bad.append(f"Java references missing texture {rid}")
        self.assertNoFindings(bad, "Missing GUI / icon textures")

    def test_atlas_directories_exist(self):
        bad = []
        for ns, path, _tail in under(ASSETS, "atlases"):
            value, _d, err = load_json(path)
            if err or not isinstance(value, dict):
                continue
            for source in value.get("sources", []):
                if not isinstance(source, dict):
                    continue
                directory = source.get("source")
                prefix = source.get("prefix", "")
                if source.get("type") == "minecraft:directory" and directory:
                    # Atlas sources in assets/minecraft are overlays: the game
                    # scans textures/<dir> in *every* namespace, so a directory
                    # only has to exist in one of them.
                    if not any((candidate / "textures" / directory).is_dir()
                               for candidate in ASSETS.iterdir()
                               if candidate.is_dir()):
                        bad.append(f"{rel(path)}: atlas directory missing "
                                   f"textures/{directory} in every namespace")
                    if prefix and not prefix.endswith("/"):
                        bad.append(f"{rel(path)}: prefix must end with '/'")
        self.assertNoFindings(bad, "Broken texture atlases")


if __name__ == "__main__":
    unittest.main()
