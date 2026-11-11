#!/usr/bin/env python3
"""Repair / synchronise every shipped language file.

Why this exists
---------------
``CreateLangFile`` regenerates ``en_us.json`` from the Java registries at dev
time. Every other locale is hand-maintained, so over the years they collected:

* invalid filenames, a UTF-8 BOM, shell-style ``#`` comments and duplicate
  keys (Minecraft silently refuses those files);
* hundreds of ``item.mmorpg.uniques.*`` keys for items that no longer exist;
* thousands of *missing* keys, which show up as raw ``mmorpg.xxx.yyy``
  strings in tooltips;
* no names at all for the 7 newest unique items, the 9 elemental talents and
  the 5 spell schools.

This tool makes ``en_us.json`` the single source of truth:

* canonical (EN/RU) values below are enforced in the two primary locales;
* every other locale drops dead keys and gains English fallbacks for all
  missing keys, while keeping all of its real translations;
* all files are rewritten as clean UTF-8 JSON with no BOM/comments/duplicates.

``python tools/audit/sync_lang.py --check`` reports drift without writing and
is used by CI. Running without ``--check`` is idempotent.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path

LANG_DIR = (Path(__file__).resolve().parents[2] / "src" / "main" /
            "resources" / "assets" / "mmorpg" / "lang")
MANUAL = LANG_DIR / "manual.json"

# Keys that must exist with these exact values in the two primary locales.
# Everything else is synced from en_us with the English text as fallback.
CANONICAL = {
    # --- unique gear display names -------------------------------------
    "mmorpg.unique_gear.iron_bulwark.name": ("Iron Bulwark", "Железный оплот"),
    "mmorpg.unique_gear.swift_stride.name": ("Swift Stride", "Стремительный шаг"),
    "mmorpg.unique_gear.pathfinder.name": ("Pathfinder", "Следопыт"),
    "mmorpg.unique_gear.bloodletter.name": ("Bloodletter", "Кровопускатель"),
    "mmorpg.unique_gear.worldsplitter.name": ("Worldsplitter", "Рассекатель миров"),
    "mmorpg.unique_gear.whisperwind.name": ("Whisperwind", "Шепчущий ветер"),
    "mmorpg.unique_gear.starfall.name": ("Starfall", "Звездопад"),
    # --- named elemental talents ---------------------------------------
    "mmorpg.talent.big_breath_of_fire": ("Breath of Flame", "Дыхание пламени"),
    "mmorpg.talent.big_breath_of_water": ("Breath of the Tide", "Дыхание прилива"),
    "mmorpg.talent.big_breath_of_nature": ("Breath of Nature", "Дыхание природы"),
    "mmorpg.talent.big_heart_of_fire": ("Heart of Flame", "Сердце пламени"),
    "mmorpg.talent.big_heart_of_water": ("Heart of the Tide", "Сердце прилива"),
    "mmorpg.talent.big_heart_of_nature": ("Heart of Nature", "Сердце природы"),
    "mmorpg.talent.fire_dom": ("Lord of Fire", "Повелитель огня"),
    "mmorpg.talent.water_dom": ("Lord of Water", "Повелитель воды"),
    "mmorpg.talent.nature_dom": ("Lord of Earth", "Повелитель земли"),
    # --- spell school names ---------------------------------------------
    "mmorpg.spell_school.divine": ("Divine", "Божественность"),
    "mmorpg.spell_school.hunting": ("Hunting", "Охота"),
    "mmorpg.spell_school.nature": ("Nature", "Природа"),
    "mmorpg.spell_school.fire": ("Fire", "Огонь"),
    "mmorpg.spell_school.water": ("Water", "Вода"),
    # --- broken generated strings ---------------------------------------
    "mmorpg.word.simulation": ("Simulation Grounds", "Полигон"),
    "mmorpg.stat_desc.craft_suc_chance": (
        "Chance that a craft succeeds. Below 100% the craft can fail and consume the ingredients.",
        "Шанс успешного крафта. Ниже 100% крафт может провалиться и потратить ингредиенты."),
}

# Keys from retired versions of the mod. They have no consumer in Java and
# only bloat (and in some cases corrupt) the locale files.
DEAD_PREFIXES = ("mmorpgs.", "item.mmorpg.unique.", "item.mmorpg.uniques.")
DEAD_EXACT = {"mmorpg.word.isotunique", "mmorpg.rarity.unique"}
COMMENT_KEY = "_comment"

# Keys whose *English source string* used to be corrupt (Russian text or a
# one-letter generator stub). Translators copied that corruption verbatim into
# every other locale. Such a value is not a translation, so replace it with the
# repaired English text; genuine translations of these keys are left alone.
SOURCE_REPAIRS = {
    "mmorpg.word.simulation": {"Полигон"},
    "mmorpg.stat_desc.craft_suc_chance": {"r", "ш"},
}

LINE = re.compile(
    r'^(?P<indent>\s*)"(?P<key>(?:[^"\\]|\\.)*)"\s*:\s*'
    r'(?P<value>"(?:[^"\\]|\\.)*")\s*,?\s*$')


def is_dead(key: str) -> bool:
    return key in DEAD_EXACT or key.startswith(DEAD_PREFIXES)


def read_pairs(path: Path):
    """Parse a locale as an ordered list of (key, value) pairs.

    Duplicate keys are preserved so that intentional ``_comment`` section
    markers survive a rewrite; real duplicates keep only their last value.
    """
    raw = path.read_bytes()
    text = raw.decode("utf-8-sig")
    text = "\n".join(line for line in text.splitlines()
                     if not line.lstrip().startswith("#"))
    pairs = []

    def hook(items):
        pairs.extend(items)
        return dict(items)

    value = json.loads(text, object_pairs_hook=hook)
    if not isinstance(value, dict):
        raise ValueError("root is not an object")
    return pairs


def dump(path: Path, pairs) -> str:
    lines = ["{"]
    body = [f'  {json.dumps(k, ensure_ascii=False)}: {json.dumps(v, ensure_ascii=False)}'
            for k, v in pairs]
    for i, line in enumerate(body):
        lines.append(line + ("," if i + 1 < len(body) else ""))
    lines.append("}")
    return "\n".join(lines) + "\n"


def canonical_for(locale: str):
    """(key -> value) that this locale must have, regardless of en_us."""
    if locale == "en_us":
        return {k: v[0] for k, v in CANONICAL.items()}
    if locale == "ru_ru":
        return {k: v[1] for k, v in CANONICAL.items()}
    return {}


def sync_file(path: Path, en_keys: dict, *, check: bool):
    locale = path.stem
    pairs = read_pairs(path)
    fixed = canonical_for(locale)

    seen = set()
    cleaned = []
    for key, value in pairs:
        if is_dead(key):
            continue
        if key != COMMENT_KEY and key in seen:
            continue  # real duplicate: last value already removed with the first
        if key != COMMENT_KEY:
            seen.add(key)
        if key in fixed:
            value = fixed[key]
        elif locale not in ("en_us", "ru_ru") and key not in en_keys:
            continue  # orphan key with no English counterpart
        cleaned.append((key, value))

    # Last-wins semantics for real duplicates: re-apply the final value.
    # Canonical values must win over *both* the first and the last occurrence,
    # otherwise a generator stub such as "r" silently survives the rewrite.
    last = {k: v for k, v in pairs if k != COMMENT_KEY}
    cleaned = [(k, (fixed[k] if k in fixed else last.get(k, v))
                if k != COMMENT_KEY else v)
               for k, v in cleaned]

    # Repair translated locales that copied a broken English source string.
    if locale not in ("en_us", "ru_ru"):
        cleaned = [(k, en_keys[k]
                    if (k in SOURCE_REPAIRS and v in SOURCE_REPAIRS[k]
                        and k in en_keys) else v)
                   for k, v in cleaned]

    present = {k for k, _ in cleaned}
    for key, value in fixed.items():
        if key not in present:
            cleaned.append((key, value))
            present.add(key)
    if locale not in ("en_us", "ru_ru"):
        for key, value in en_keys.items():
            if key not in present and key != COMMENT_KEY:
                cleaned.append((key, value))
                present.add(key)

    content = dump(path, cleaned)
    # Normalise CRLF: on Windows a checkout with core.autocrlf=true turns
    # our LF file into CRLF, which would otherwise look like permanent drift.
    current = path.read_bytes().replace(b"\r\n", b"\n").decode("utf-8-sig")
    changed = content != current
    return content, changed, cleaned


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true",
                        help="report drift, do not modify files")
    args = parser.parse_args()

    # Drift found before the per-locale loop (filename / manual fragment).
    pre_drift = False

    # 1. Fix the invalid fr_fr..json filename before anything else.
    broken = LANG_DIR / "fr_fr..json"
    target = LANG_DIR / "fr_fr.json"
    if broken.exists():
        if args.check:
            print(f"DRIFT  {broken.name} must be renamed to {target.name}")
            pre_drift = True
        else:
            if target.exists():
                print(f"ERROR  both {broken.name} and {target.name} exist", file=sys.stderr)
                return 1
            broken.rename(target)
            print(f"RENAMED {broken.name} -> {target.name}")

    # 2. manual.json is a raw fragment; keep its two drifted values in sync.
    # Compare like with like: a checkout with core.autocrlf=true stores CRLF,
    # so both sides must be normalised before deciding there is drift.
    original_manual = (MANUAL.read_bytes().replace(b"\r\n", b"\n")
                       .decode("utf-8-sig"))
    manual_text = original_manual
    for key, (english, _russian) in CANONICAL.items():
        pattern = re.compile(r'("' + re.escape(key) + r'"\s*:\s*)"(?:[^"\\]|\\.)*"')
        if pattern.search(manual_text):
            manual_text = pattern.sub(
                lambda m: m.group(1) + json.dumps(english, ensure_ascii=False),
                manual_text)
    # The two known value drifts are plain literals in the fragment.
    manual_text = manual_text.replace(
        '"itemGroup.mmorpg_currency": "Crafting Items"',
        '"itemGroup.mmorpg_currency": "Age of Exile: Currency"')
    manual_text = manual_text.replace(
        '"mmorpg.one_of_a_kind.start": "Start"',
        '"mmorpg.one_of_a_kind.start": "Starting Path"')
    if manual_text != original_manual:
        if args.check:
            print("DRIFT  manual.json")
            pre_drift = True
        else:
            MANUAL.write_bytes(manual_text.encode("utf-8"))
            print("FIXED  manual.json")

    # 3. en_us is the source of truth: repair it first.
    en_path = LANG_DIR / "en_us.json"
    en_content, en_changed, en_pairs = sync_file(en_path, {}, check=args.check)
    if en_changed and not args.check:
        en_path.write_bytes(en_content.encode("utf-8"))
        print(f"FIXED  en_us.json ({len(en_pairs)} keys)")
    elif en_changed:
        print("DRIFT  en_us.json")
    en_keys = dict(en_pairs)

    # 4. Every other locale: drop dead keys, add English fallbacks.
    drift = en_changed or pre_drift
    for path in sorted(LANG_DIR.glob("*.json")):
        if path.name in ("manual.json", "en_us.json"):
            continue
        content, changed, pairs = sync_file(path, en_keys, check=args.check)
        if changed:
            drift = True
            if args.check:
                print(f"DRIFT  {path.name}")
            else:
                path.write_bytes(content.encode("utf-8"))
                print(f"FIXED  {path.name} ({len(pairs)} keys)")

    if args.check:
        if drift:
            print("\nLocales are out of sync. Run: python tools/audit/sync_lang.py")
            return 1
        print("All locales are in sync.")
    else:
        print("Done.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
