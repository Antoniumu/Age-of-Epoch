"""Procedural tower geometry.

Every theme gets six pieces:

    start0            – ground floor with four entrances and the way up
    middle0..middle2  – walkable floors (hall / cells / themed special room)
    top0, top1        – boss hall with the [boss] marker and treasure

All pieces are 17x17 in footprint so they line up through the vertical
jigsaw connector in the centre. Four ladder columns, one against each wall,
remain aligned even when jigsaw rotates individual pieces.
"""

from __future__ import annotations

import random

from nbt_writer import Structure, sign_text

W = 17
D = 17
MID_H = 6
TOP_H = 13
START_H = 7

CX = 8
CZ = 8
LADDER_X = 1
LADDER_Z = 8

CONNECT = "world_of_exile:connect"


def _h(*parts):
    """Stable hash from strings, so regenerating gives identical output."""
    text = "|".join(str(p) for p in parts)
    value = 2166136261
    for ch in text:
        value = ((value ^ ord(ch)) * 16777619) & 0xFFFFFFFF
    return value


def _rng(theme, piece):
    return random.Random(_h(theme["id"], piece))


def _chamfer(x, z):
    return (x in (0, W - 1)) and (z in (0, D - 1))


def _wall_material(theme, rng):
    roll = rng.random()
    if roll < 0.82:
        return theme["wall"]
    if roll < 0.95:
        return theme["wall_alt"]
    return theme["wall_moss"]


def _jigsaw(s, x, y, z, orientation, pool, final_state):
    s.put(
        x,
        y,
        z,
        "minecraft:jigsaw",
        {"orientation": orientation},
        {
            "joint": "rollable",
            "name": CONNECT,
            "target": CONNECT,
            "pool": pool,
            "final_state": final_state,
            "id": "minecraft:jigsaw",
        },
    )


def _marker(s, x, y, z, text, tag=None, loot=None):
    """Place a tag sign.  The structure processor turns it into a chest,
    spawner or boss cage and strips the sign from the final structure.

    `tag` carries the theme (`[tower:<id>]`), `loot` the chest loot table
    (`[loot#<namespace:path>]`)."""
    s.put(
        x,
        y,
        z,
        "minecraft:oak_sign",
        {"rotation": "0", "waterlogged": "false"},
        sign_text(text, tag, loot),
    )


# Ladders on all four walls.  Jigsaw pieces may be rotated by 90 degrees when
# they are connected, so a single ladder column would break between floors.
# The four-way pattern is rotation invariant, which keeps every tower climbable.
LADDERS = (
    (LADDER_X, LADDER_Z, "east"),     # against the west wall
    (W - 1 - LADDER_X, LADDER_Z, "west"),
    (CX, LADDER_X, "south"),          # against the north wall
    (CX, D - 1 - LADDER_X, "north"),
)

# the solid block each ladder is attached to
LADDER_SUPPORTS = {
    (0, LADDER_Z), (W - 1, LADDER_Z), (CX, 0), (CX, D - 1),
}


def _ladder_column(s, bottom=0, top=None):
    """Continuous climbable ladders in all four quadrants of the shaft."""
    if top is None:
        top = s.size[1] - 1
    for y in range(bottom, top + 1):
        for (x, z, facing) in LADDERS:
            s.put(x, y, z, "minecraft:ladder", {"facing": facing, "waterlogged": "false"})


def _light(s, x, y, z, theme):
    s.put(x, y, z, theme["light"])


def _floor(s, height, theme, rng):
    for x in range(W):
        for z in range(D):
            name = theme["floor"] if (x + z) % 2 else theme["floor_alt"]
            if max(abs(x - CX), abs(z - CZ)) in (3, 6):
                name = theme["accent"]
            if rng.random() < 0.03:
                name = theme["wall_moss"]
            s.put(x, 0, z, name)
    _jigsaw(s, CX, 0, CZ, "down_north", "minecraft:empty", "minecraft:air")


def _ceiling(s, height, theme, rng, pool):
    y = height - 1
    for x in range(W):
        for z in range(D):
            name = theme["floor"] if (x + z) % 4 else theme["floor_alt"]
            s.put(x, y, z, name)
    _jigsaw(s, CX, y, CZ, "up_east", pool, "minecraft:air")


def _window_spot(x, z, y):
    """Sparse, regular window grid with solid pillars between the panes."""
    if z == 0 or z == D - 1:
        return x % 4 == 2 and 3 <= x <= W - 4 and y % 3 != 0
    if x == 0 or x == W - 1:
        return z % 4 == 2 and 3 <= z <= D - 4 and y % 3 != 0
    return False


def _walls(s, height, theme, rng, window_rows, door_rows=None, door_sides=None):
    door_rows = door_rows or ()
    door_sides = door_sides or set()
    for y in range(1, height - 1):
        for x in range(W):
            for z in range(D):
                if not (x in (0, W - 1) or z in (0, D - 1)):
                    continue
                if _chamfer(x, z):
                    continue
                name = _wall_material(theme, rng)

                # keep the wall solid behind every ladder column
                if (x, z) in LADDER_SUPPORTS:
                    s.put(x, y, z, name)
                    continue

                # entrances (the ladder column keeps its wall pier)
                if y in door_rows:
                    if z in (0, D - 1) and 6 <= x <= 10 and x != CX:
                        if ("north" if z == 0 else "south") in door_sides:
                            s.remove(x, y, z)
                            continue
                    if x in (0, W - 1) and 6 <= z <= 10 and z != CZ:
                        if ("west" if x == 0 else "east") in door_sides:
                            s.remove(x, y, z)
                            continue

                # windows
                if y in window_rows and _window_spot(x, z, y):
                    s.put(x, y, z, theme["glass"])
                    continue

                s.put(x, y, z, name)


def _pillars(s, height, theme):
    for (x, z) in ((3, 3), (3, 13), (13, 3), (13, 13)):
        for y in range(1, height - 1):
            s.put(x, y, z, theme["accent"])
        _light(s, x, height - 2, z, theme)
        _light(s, x, 2, z, theme)


def _room(s, x0, z0, size, theme, door_side, height):
    """A small side room built against the outer wall."""
    x1 = x0 + size - 1
    z1 = z0 + size - 1
    for y in range(1, height - 1):
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if x in (x0, x1) or z in (z0, z1):
                    s.put(x, y, z, theme["wall"])
    y = 1
    mid = size // 2
    if door_side == "west":
        s.remove(x0, y, z0 + mid)
        s.remove(x0, y + 1, z0 + mid)
    elif door_side == "east":
        s.remove(x1, y, z0 + mid)
        s.remove(x1, y + 1, z0 + mid)
    elif door_side == "north":
        s.remove(x0 + mid, y, z0)
        s.remove(x0 + mid, y + 1, z0)
    else:
        s.remove(x0 + mid, y, z1)
        s.remove(x0 + mid, y + 1, z1)
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            s.put(x, height - 2, z, theme["accent"])
    _light(s, x0 + 1, height - 3, z0 + 1, theme)


def _plants(s, theme, rng, y=1, count=6):
    spots = [
        (4, 4),
        (12, 4),
        (4, 12),
        (12, 12),
        (5, 8),
        (11, 8),
        (8, 4),
        (8, 12),
    ]
    rng.shuffle(spots)
    for (x, z) in spots[:count]:
        s.put(x, y, z, theme["deco"])
        if rng.random() < 0.4:
            _light(s, x, y + 1, z, theme)


def _pool(s, theme, rng, y=1):
    liquid = theme["liquid"] or "minecraft:water"
    cx, cz = 8, 4
    for x in range(cx - 2, cx + 3):
        for z in range(cz - 2, cz + 3):
            edge = x in (cx - 2, cx + 2) or z in (cz - 2, cz + 2)
            if edge:
                s.put(x, y, z, theme["accent"])
            else:
                s.put(x, y, z, liquid, {"level": "0"})
    for (x, z) in ((cx - 1, cz - 1), (cx + 1, cz + 1)):
        _light(s, x, y + 1, z, theme)


def _forge(s, theme, rng):
    for x, z, name in (
        (3, 3, "minecraft:blast_furnace"),
        (4, 3, "minecraft:furnace"),
        (3, 4, "minecraft:smithing_table"),
        (12, 12, "minecraft:anvil"),
        (13, 12, "minecraft:grindstone"),
        (12, 13, "minecraft:cauldron"),
    ):
        s.put(x, 1, z, name)
    for z in range(6, 11):
        s.put(8, 1, z, theme["accent"])
    _light(s, 8, 2, 13, theme)


def _crypt(s, theme, rng):
    for x in (6, 10):
        for z in (4, 12):
            s.put(x, 1, z, theme["accent"])
            s.put(x, 2, z, "minecraft:skeleton_skull", {"rotation": "0"})
            _light(s, x - 1, 2, z, theme)
    for z in (6, 8, 10):
        s.put(3, 1, z, theme["accent"])
        s.put(13, 1, z, theme["accent"])


def _library(s, theme, rng):
    for x in range(3, 14, 2):
        for z in (2, 14):
            s.put(x, 1, z, "minecraft:bookshelf")
            s.put(x, 2, z, "minecraft:bookshelf")
    for z in range(4, 13, 2):
        s.put(2, 1, z, "minecraft:bookshelf")
        s.put(14, 1, z, "minecraft:bookshelf")
    s.put(8, 1, 8, "minecraft:enchanting_table")
    _light(s, 8, 2, 8, theme)


def _garden(s, theme, rng):
    for x in range(3, 14, 3):
        for z in range(3, 14, 3):
            if (x, z) == (9, 9):
                continue
            s.put(x, 1, z, theme["deco"])
            if (x + z) % 2 == 0:
                _light(s, x, 2, z, theme)
    for (x, z) in ((4, 8), (12, 8)):
        for y in range(1, 4):
            s.put(x, y, z, theme["accent"])
        s.outline(x - 1, 4, z - 1, x + 1, 4, z + 1, theme["deco"])


def _decorate_special(s, theme, rng, height):
    kind = theme["special"]
    if kind == "pool":
        _pool(s, theme, rng)
    elif kind == "forge":
        _forge(s, theme, rng)
    elif kind == "crypt":
        _crypt(s, theme, rng)
    elif kind == "library":
        _library(s, theme, rng)
    elif kind == "garden":
        _garden(s, theme, rng)
    else:
        _plants(s, theme, rng, count=4)


def _middle_pool(theme):
    return f"world_of_exile:towers/{theme['id']}/middle"


def _top_pool(theme):
    return f"world_of_exile:towers/{theme['id']}/top"


# ---------------------------------------------------------------------------
# Pieces
# ---------------------------------------------------------------------------


def build_start(theme):
    rng = _rng(theme, "start")
    s = Structure(W, START_H, D)
    _floor(s, START_H, theme, rng)
    _walls(
        s,
        START_H,
        theme,
        rng,
        window_rows=(2,),
        door_rows=(1, 2, 3),
        door_sides={"north", "south", "east", "west"},
    )
    # decorative arch over each entrance
    for x in range(6, 11):
        s.put(x, 4, 0, theme["accent"])
        s.put(x, 4, D - 1, theme["accent"])
    for z in range(6, 11):
        s.put(0, 4, z, theme["accent"])
        s.put(W - 1, 4, z, theme["accent"])
    _ceiling(s, START_H, theme, rng, _middle_pool(theme))
    _pillars(s, START_H, theme)
    _ladder_column(s, bottom=1)
    _plants(s, theme, rng, count=2)
    for x in range(W):
        for z in range(D):
            if (x in (0, W - 1) or z in (0, D - 1)) and (x + z) % 2 == 0:
                s.put(x, START_H - 1, z, theme["wall_alt"])

    _marker(s, 4, 1, 4, "[spawner]", f"[tower:{theme['id']}]")
    _marker(s, 12, 1, 12, "[spawner]", f"[tower:{theme['id']}]")
    _marker(s, 4, 1, 12, "[treasure]", "[chance:60]", loot=theme["loot"])
    return s


def build_middle(theme, variant):
    rng = _rng(theme, f"middle{variant}")
    s = Structure(W, MID_H, D)
    _floor(s, MID_H, theme, rng)
    _walls(s, MID_H, theme, rng, window_rows=(2, 3))
    _ceiling(s, MID_H, theme, rng, _middle_pool(theme))
    _pillars(s, MID_H, theme)
    _ladder_column(s, bottom=0)

    if variant == 0:
        # open hall
        _plants(s, theme, rng, count=3)
        _marker(s, 4, 1, 4, "[spawner]", f"[tower:{theme['id']}]")
        _marker(s, 12, 1, 12, "[treasure]", "[chance:45]", loot=theme["loot"])
        _marker(s, 13, 1, 8, "[treasure]", "[chance:25]", loot=theme["loot"])
    elif variant == 1:
        # prison cells / barracks
        _room(s, 2, 2, 5, theme, "east", MID_H)
        _room(s, 10, 10, 5, theme, "west", MID_H)
        _marker(s, 13, 1, 4, "[treasure]", "[chance:30]", loot=theme["loot"])
        _marker(s, 4, 1, 12, "[spawner]", f"[tower:{theme['id']}]")
        _marker(s, 12, 1, 4, "[spawner]", f"[tower:{theme['id']}]")
    else:
        # themed special room
        _decorate_special(s, theme, rng, MID_H)
        _marker(s, 4, 1, 12, "[spawner]", f"[tower:{theme['id']}]")
        _marker(s, 13, 1, 4, "[treasure]", "[chance:35]", loot=theme["loot"])

    return s


def build_top(theme, variant):
    rng = _rng(theme, f"top{variant}")
    s = Structure(W, TOP_H, D)
    _floor(s, TOP_H, theme, rng)
    _walls(s, TOP_H, theme, rng, window_rows=(4, 5, 6, 7))
    # roof with crenellations
    for x in range(W):
        for z in range(D):
            s.put(x, TOP_H - 1, z, theme["floor_alt"])
    for x in range(W):
        for z in range(D):
            if x in (0, W - 1) or z in (0, D - 1):
                if not _chamfer(x, z) and (x + z) % 2 == 0:
                    s.put(x, TOP_H - 1, z, theme["wall_alt"])
    _jigsaw(s, CX, 0, CZ, "down_north", "minecraft:empty", "minecraft:air")
    _ladder_column(s, bottom=0, top=2)
    _pillars(s, TOP_H, theme)

    # dais for the boss cage
    dais_z = 12 if variant == 0 else 4
    for x in range(CX - 3, CX + 4):
        for z in range(dais_z - 1, dais_z + 2):
            if 1 <= x <= W - 2 and 1 <= z <= D - 2:
                s.put(x, 0, z, theme["accent"])
                s.put(x, 1, z, theme["wall_alt"])
    _light(s, CX - 2, 2, dais_z, theme)
    _light(s, CX + 2, 2, dais_z, theme)

    _decorate_special(s, theme, rng, TOP_H)
    _plants(s, theme, rng, count=4)

    # Decorations must not bury the boss inside plants, lava or a pillar.
    # A 5x5 platform and five blocks of headroom also fit the larger bosses.
    for x in range(CX - 2, CX + 3):
        for z in range(dais_z - 2, dais_z + 3):
            s.put(x, 1, z, theme["wall_alt"])
            for y in range(2, 7):
                s.remove(x, y, z)

    _marker(s, CX, 2, dais_z, "[boss]", f"[tower:{theme['id']}]")

    # treasure hoard at the opposite end
    hoard_z = 4 if variant == 0 else 12
    for x in (CX - 1, CX + 1):
        _marker(s, x, 1, hoard_z, "[treasure]", "[chance:100]", loot=theme["loot"])
    _marker(s, CX, 1, hoard_z, "[final_treasure]", "[chance:100]", loot=theme["loot"])

    # defenders
    for (x, z) in ((4, 4), (12, 4), (4, 12), (12, 12)):
        _marker(s, x, 1, z, "[spawner]", f"[tower:{theme['id']}]")

    return s
