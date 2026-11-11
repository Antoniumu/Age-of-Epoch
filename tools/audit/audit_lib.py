#!/usr/bin/env python3
"""Shared infrastructure for the Age of Epoch content audit.

Pure standard library on purpose: the same code must run locally, in the
sandbox and on a bare GitHub runner without installing anything.

The repository is indexed lazily once per process (see :class:`Repo`) and every
test module works against that single snapshot, so a full run touches each
file only once.
"""

from __future__ import annotations

import json
import os
import re
import struct
from functools import lru_cache
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "src" / "main" / "resources"
ASSETS = RES / "assets"
DATA = RES / "data"
JAVA = ROOT / "src" / "main" / "java"
TEST_JAVA = ROOT / "src" / "test" / "java"
TOOLS = ROOT / "tools"
WORKFLOWS = ROOT / ".github" / "workflows"

OWN_NAMESPACES = {"mmorpg", "divine_missions", "infinite_dungeons", "library_of_exile"}
PRIMARY_LOCALES = ("en_us", "ru_ru")
VANILLA_LIKE = {"minecraft", "fabric", "c"}

# Section markers emitted by CreateLangFileUtils.comment(). They are duplicated
# on purpose (one per generated category), so duplicate detection ignores them.
COMMENT_KEY = "_comment"


def rel(path) -> str:
    """Repository-relative POSIX path, used in every diagnostic message."""
    p = Path(path)
    try:
        return p.resolve().relative_to(ROOT).as_posix()
    except ValueError:
        return p.as_posix().replace(os.sep, "/")


def walk(root: Path, suffixes=None):
    """All files below *root* (recursive), as absolute Path objects."""
    if not root.exists():
        return []
    out = []
    for base, _dirs, files in os.walk(root):
        for name in files:
            if suffixes is None or name.endswith(suffixes):
                out.append(Path(base) / name)
    return sorted(out)


def json_files(root: Path = RES):
    return walk(root, (".json",))


def _duplicate_hook(collector):
    def hook(pairs):
        seen = set()
        obj = {}
        for key, value in pairs:
            if key in seen:
                collector.append(key)
            seen.add(key)
            obj[key] = value
        return obj

    return hook


def load_json(path, *, allow_bom=False, allow_comments=False):
    """Parse a JSON file strictly.

    Returns ``(object, duplicate_keys, error_string)``. BOM and leading ``#``
    comment lines are reported as errors unless explicitly allowed: a resource
    pack shipped to Minecraft must be plain UTF-8 JSON.
    """
    path = Path(path)
    try:
        raw = path.read_bytes()
    except OSError as exc:
        return None, [], f"read error: {exc}"

    bom = raw.startswith(b"\xef\xbb\xbf")
    if bom and not allow_bom:
        return None, [], "UTF-8 BOM before the JSON body"
    try:
        text = raw.decode("utf-8-sig" if bom else "utf-8")
    except UnicodeDecodeError as exc:
        return None, [], f"invalid UTF-8: {exc}"

    if allow_comments:
        text = "\n".join(line for line in text.splitlines()
                         if not line.lstrip().startswith("#"))

    dups = []
    try:
        value = json.loads(text, object_pairs_hook=_duplicate_hook(dups),
                           parse_constant=lambda c: (_ for _ in ()).throw(
                               ValueError(f"non-finite constant {c}")))
    except (ValueError, json.JSONDecodeError) as exc:
        return None, dups, f"invalid JSON: {exc}"

    # _comment markers are duplicated by the generator on purpose.
    dups = [d for d in dups if d != COMMENT_KEY]
    return value, dups, None


def png_size(path: Path):
    try:
        with open(path, "rb") as handle:
            head = handle.read(24)
    except OSError:
        return None
    if len(head) < 24 or head[:8] != b"\x89PNG\r\n\x1a\n":
        return None
    return struct.unpack(">II", head[16:24])


def split_id(value: str, default_ns: str = "minecraft"):
    """Split a resource location into (namespace, path)."""
    if ":" in value:
        ns, path = value.split(":", 1)
        return ns, path
    return default_ns, value


def texture_id(value: str, default_ns: str):
    ns, path = split_id(value, default_ns)
    if not path.startswith("textures/"):
        path = "textures/" + path
    if not path.endswith(".png"):
        path += ".png"
    return ns + ":" + path


def model_id(value: str, default_ns: str):
    ns, path = split_id(value, default_ns)
    if not path.startswith("models/"):
        path = "models/" + path
    if not path.endswith(".json"):
        path += ".json"
    return ns + ":" + path


class Repo:
    """Lazy, cached view over the whole repository."""

    def __init__(self):
        self._asset_index = None
        self._data_index = None
        self._nbt_index = None
        self._java_text = None
        self._java_literals = None
        self._lang = {}

    # ---------------------------------------------------------------- indexes
    def asset_index(self):
        """Set of ``ns:relative/path`` for every file under assets/."""
        if self._asset_index is None:
            index = set()
            for p in walk(ASSETS):
                r = rel(p)
                marker = "/assets/"
                if marker not in r:
                    continue
                tail = r.split(marker, 1)[1]
                ns, _, tail = tail.partition("/")
                index.add(ns + ":" + tail)
            self._asset_index = index
        return self._asset_index

    def data_index(self):
        if self._data_index is None:
            index = set()
            for p in walk(DATA):
                r = rel(p)
                marker = "/data/"
                if marker not in r:
                    continue
                tail = r.split(marker, 1)[1]
                ns, _, tail = tail.partition("/")
                index.add(ns + ":" + tail)
            self._data_index = index
        return self._data_index

    def nbt_ids(self):
        """Structure ids resolvable from template_pool ``location`` fields."""
        if self._nbt_index is None:
            ids = set()
            for p in walk(DATA, (".nbt",)):
                r = rel(p)
                tail = r.split("/data/", 1)[1][:-4]
                ns, _, tail = tail.partition("/")
                for prefix in ("structure/", "structures/"):
                    if tail.startswith(prefix):
                        ids.add(ns + ":" + tail[len(prefix):])
                        break
                else:
                    ids.add(ns + ":" + tail)
            self._nbt_index = ids
        return self._nbt_index

    def asset_path(self, rid: str):
        ns, path = split_id(rid)
        return ASSETS / ns / path

    # ------------------------------------------------------------------ java
    def java_text(self):
        if self._java_text is None:
            chunks = []
            for p in walk(JAVA, (".java",)):
                chunks.append(p.read_text(encoding="utf-8", errors="ignore"))
            self._java_text = "\n".join(chunks)
        return self._java_text

    def java_literals(self):
        if self._java_literals is None:
            self._java_literals = set(re.findall(
                r'"([^"\\\n]{3,})"', self.java_text()))
        return self._java_literals

    def unique_gears(self):
        """All ``UniqueGearBuilder.of("id", "name", ...)`` registrations."""
        out = []
        pattern = re.compile(
            r'UniqueGearBuilder\.of\(\s*"([^"]+)"\s*,\s*"([^"]*)"')
        for p in walk(JAVA / "com" / "robertx22" / "age_of_exile" /
                      "aoe_data" / "database" / "unique_gears", (".java",)):
            for match in pattern.finditer(p.read_text(encoding="utf-8", errors="ignore")):
                out.append((match.group(1), match.group(2), rel(p)))
        return out

    def talent_tree_ids(self):
        ids = set()
        for name in ("talents.json", "divine.json"):
            path = DATA / "mmorpg" / "talent_tree" / name
            value, _dups, err = load_json(path)
            if err or not isinstance(value, dict):
                continue
            grid = value.get("perks", "")
            if not isinstance(grid, str):
                continue
            for row in grid.split("\n"):
                for token in row.split(","):
                    token = token.strip().lower()
                    if len(token) > 2 and token != "[center]":
                        ids.add(token)
        return ids

    def named_perk_ids(self):
        """Perk ids that render ``locName()`` and therefore need a translation.

        STAT perks deliberately have no name (their tooltip is just the stat
        list), while bigStat/gameChanger/register/perk create named perks.
        Nine of them are built by concatenating an element suffix and have no
        literal id in the sources, so they are expanded explicitly.
        """
        text = self.java_text()
        ids = set()
        for pattern in (
                r'PerkBuilder\.bigStat\(\s*"([a-z0-9_]+)"',
                r'PerkBuilder\.gameChanger\(\s*"([a-z0-9_]+)"',
                r'\bregister\(\s*"([a-z0-9_]+)"',
                r'\bperk\(\s*"([a-z0-9_]+)"'):
            ids.update(re.findall(pattern, text))
        if 'big_breath_of_' in text and 'big_heart_of_' in text:
            for element in ("fire", "water", "nature"):
                ids.update((f"big_breath_of_{element}",
                            f"big_heart_of_{element}", f"{element}_dom"))
        return ids

    def java_code(self):
        """Java sources with line/block comments stripped (string-safe enough
        for resource paths, which never contain '//' or '/*')."""
        text = self.java_text()
        text = re.sub(r"/\*.*?\*/", " ", text, flags=re.S)
        text = re.sub(r"//[^\n]*", " ", text)
        return text

    def java_texture_refs(self):
        """Texture ids referenced from Java, with their namespace resolved.

        Returns ``ns:textures/...png`` strings for our own namespaces only.
        Identifier.withDefaultNamespace(...) targets vanilla and is skipped.
        """
        text = self.java_code()
        refs = set()
        consumed = set()
        ns_prefixes = (
            (r'SlashRef\.MODID\s*,\s*"(textures/[^"]+\.png)"', "mmorpg"),
            (r'SlashRef\.id\(\s*"(textures/[^"]+\.png)"', "mmorpg"),
            (r'DivineMissions\.id\(\s*"(textures/[^"]+\.png)"', "divine_missions"),
            (r'MainID\.id\(\s*"(textures/[^"]+\.png)"', "infinite_dungeons"),
            # The lookbehind is essential: without it this also matches
            # "SlashRef.MODID" and silently rewrites 50 mmorpg textures into
            # the library_of_exile namespace.
            (r'(?<![A-Za-z0-9_])Ref\.MODID\s*,\s*"(textures/[^"]+\.png)"',
             "library_of_exile"),
        )
        for pattern, ns in ns_prefixes:
            for match in re.findall(pattern, text):
                consumed.add(match)
                refs.add(ns + ":" + match)
        for match in re.findall(
                r'Identifier\.withDefaultNamespace\(\s*"(textures/[^"]+\.png)"', text):
            consumed.add(match)  # vanilla texture, not shipped by this mod
        # Bare texture literals in data builders live in the mmorpg namespace.
        for match in re.findall(r'"(textures/[^"\s]+\.png)"', text):
            if match not in consumed:
                refs.add("mmorpg:" + match)
        return refs

    # ------------------------------------------------------------------- lang
    def lang_dirs(self):
        return sorted(p for p in (ASSETS).iterdir()
                      if p.is_dir() and (p / "lang").is_dir())

    def lang(self, namespace: str, locale: str):
        key = (namespace, locale)
        if key not in self._lang:
            path = ASSETS / namespace / "lang" / (locale + ".json")
            value, dups, err = load_json(path)
            self._lang[key] = (value if isinstance(value, dict) else {},
                              dups, err, path)
        return self._lang[key]

    def lang_files(self):
        out = []
        for d in self.lang_dirs():
            for p in sorted((d / "lang").glob("*.json")):
                out.append((d.name, p))
        return out


REPO = Repo()


def format_findings(findings, limit=25):
    """Human-readable, bounded diagnostic block for assertion messages."""
    findings = list(findings)
    lines = [f"  - {item}" for item in findings[:limit]]
    if len(findings) > limit:
        lines.append(f"  ... and {len(findings) - limit} more")
    return "\n".join(lines)


class AuditCase:
    """Mixin for unittest.TestCase: bounded assertion over many findings."""

    def assertNoFindings(self, findings, title, limit=25):
        findings = list(findings)
        if findings:
            self.fail(f"{title}: {len(findings)} violation(s)\n"
                      + format_findings(findings, limit))
