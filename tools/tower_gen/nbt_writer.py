"""Minimal NBT structure writer (gzip) for Minecraft structure templates.

Only the subset needed by the tower generator: `size`, `entities`, `blocks`,
`palette`, `DataVersion`.  Blocks may carry optional NBT (used for signs).

The files written here follow the same layout as the hand-built tower
structures that shipped with the mod, so `StructureTemplate` reads them
without any extra conversion.
"""

from __future__ import annotations

import gzip
import json
import struct

# Tag ids
TAG_END = 0
TAG_BYTE = 1
TAG_SHORT = 2
TAG_INT = 3
TAG_LONG = 4
TAG_FLOAT = 5
TAG_DOUBLE = 6
TAG_BYTE_ARRAY = 7
TAG_STRING = 8
TAG_LIST = 9
TAG_COMPOUND = 10
TAG_INT_ARRAY = 11
TAG_LONG_ARRAY = 12

# Current data version of the target platform (see BUILD-REPORT.md).  Using the
# current version keeps the newer palette blocks (mud bricks, deepslate,
# smooth basalt, ...) out of the data fixer pass.
DATA_VERSION = 5023


class Tag:
    """A typed NBT payload."""

    __slots__ = ("kind", "value")

    def __init__(self, kind, value):
        self.kind = kind
        self.value = value


def Byte(v):
    return Tag(TAG_BYTE, int(v))


def Short(v):
    return Tag(TAG_SHORT, int(v))


def Int(v):
    return Tag(TAG_INT, int(v))


def Long(v):
    return Tag(TAG_LONG, int(v))


def Float(v):
    return Tag(TAG_FLOAT, float(v))


def String(v):
    return Tag(TAG_STRING, str(v))


def _auto(value):
    """Wrap plain python values so callers can pass literal dicts."""
    if isinstance(value, Tag):
        return value
    if isinstance(value, bool):
        return Byte(1 if value else 0)
    if isinstance(value, int):
        return Int(value)
    if isinstance(value, float):
        return Float(value)
    if isinstance(value, str):
        return String(value)
    if isinstance(value, dict):
        return Compound(value)
    if isinstance(value, (list, tuple)):
        raise TypeError("use ListOf()/IntList() for lists")
    raise TypeError(f"cannot convert {type(value)!r} to an NBT tag")


def Compound(d):
    return Tag(TAG_COMPOUND, {k: _auto(v) for k, v in dict(d).items()})


def ListOf(tag_kind, items):
    return Tag(TAG_LIST, (tag_kind, list(items)))


def IntList(values):
    return Tag(TAG_LIST, (TAG_INT, [Int(v) for v in values]))


def _write_string(out, value):
    raw = value.encode("utf-8")
    out.extend(struct.pack(">H", len(raw)))
    out.extend(raw)


def _write_payload(out, tag):
    kind = tag.kind
    value = tag.value
    if kind == TAG_BYTE:
        out.extend(struct.pack(">b", value))
    elif kind == TAG_SHORT:
        out.extend(struct.pack(">h", value))
    elif kind == TAG_INT:
        out.extend(struct.pack(">i", value))
    elif kind == TAG_LONG:
        out.extend(struct.pack(">q", value))
    elif kind == TAG_FLOAT:
        out.extend(struct.pack(">f", value))
    elif kind == TAG_DOUBLE:
        out.extend(struct.pack(">d", value))
    elif kind == TAG_STRING:
        _write_string(out, value)
    elif kind == TAG_LIST:
        item_kind, items = value
        out.extend(struct.pack(">b", item_kind))
        out.extend(struct.pack(">i", len(items)))
        for item in items:
            if item_kind == TAG_COMPOUND:
                body = item.value if isinstance(item, Tag) else item
                _write_compound_body(out, body)
            else:
                _write_payload(out, item)
    elif kind == TAG_COMPOUND:
        _write_compound_body(out, value)
    else:
        raise TypeError(f"unsupported tag kind {kind}")


def _write_compound_body(out, mapping):
    for name, tag in mapping.items():
        out.extend(struct.pack(">b", tag.kind))
        _write_string(out, name)
        _write_payload(out, tag)
    out.append(TAG_END)


def save_nbt(path, root_name, root):
    out = bytearray()
    out.extend(struct.pack(">b", TAG_COMPOUND))
    _write_string(out, root_name)
    _write_compound_body(out, root)
    with open(path, "wb") as handle:
        handle.write(gzip.compress(bytes(out), 9, mtime=0))


# ---------------------------------------------------------------------------
# Structure building helpers
# ---------------------------------------------------------------------------


def sign_text(text, tag=None, loot=None):
    """A sign NBT payload.  Only tag lines are read by the structure processor."""
    return Compound(
        {
            "id": String("minecraft:sign"),
            "Text1": String(json.dumps({"text": text}, ensure_ascii=False)),
            "Text2": String(json.dumps({"text": tag or ""}, ensure_ascii=False)),
            "Text3": String(json.dumps({"text": f"[loot#{loot}]" if loot else ""}, ensure_ascii=False)),
            "Text4": String(json.dumps({"text": ""})),
        }
    )


class Structure:
    """A mutable voxel canvas that can be written out as a structure NBT."""

    def __init__(self, size_x, size_y, size_z):
        self.size = (size_x, size_y, size_z)
        self.blocks = {}

    def in_bounds(self, x, y, z):
        sx, sy, sz = self.size
        return 0 <= x < sx and 0 <= y < sy and 0 <= z < sz

    def put(self, x, y, z, name, props=None, nbt=None):
        if not self.in_bounds(x, y, z):
            return
        key = (int(x), int(y), int(z))
        if name is None:
            self.blocks.pop(key, None)
            return
        if props is None and name.endswith("_leaves"):
            # structure leaves decay otherwise
            props = {"persistent": "true"}
        frozen = tuple(sorted(props.items())) if props else ()
        if isinstance(nbt, dict):
            nbt = Compound(nbt)
        self.blocks[key] = (name, frozen, nbt)

    def get(self, x, y, z):
        return self.blocks.get((int(x), int(y), int(z)))

    def remove(self, x, y, z):
        self.blocks.pop((int(x), int(y), int(z)), None)

    def fill(self, x0, y0, z0, x1, y1, z1, name, props=None):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.put(x, y, z, name, props)

    def outline(self, x0, y0, z0, x1, y1, z1, name, props=None):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    if x in (x0, x1) or y in (y0, y1) or z in (z0, z1):
                        self.put(x, y, z, name, props)

    def save(self, path):
        palette_index = {}
        palette = []
        blocks = []
        for pos in sorted(self.blocks):
            name, props, nbt = self.blocks[pos]
            key = (name, props)
            if key not in palette_index:
                entry = {"Name": String(name)}
                if props:
                    entry["Properties"] = Compound({k: String(v) for k, v in props})
                palette_index[key] = len(palette)
                palette.append(Compound(entry))
            block = {
                "pos": IntList(pos),
                "state": Int(palette_index[key]),
            }
            if nbt is not None:
                block["nbt"] = nbt
            blocks.append(Compound(block))

        root = {
            "size": IntList(self.size),
            "entities": ListOf(TAG_COMPOUND, []),
            "blocks": ListOf(TAG_COMPOUND, blocks),
            "palette": ListOf(TAG_COMPOUND, palette),
            "DataVersion": Int(DATA_VERSION),
        }
        save_nbt(path, "", root)
        return len(blocks), len(palette)
