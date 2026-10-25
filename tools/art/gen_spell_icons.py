#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Генератор иконок умений 16×16 (пиксель-арт, как у остальных иконок мода).

Зачем скрипт, а не просто PNG «нарисованные в редакторе»: иконки — часть
контента, который проверяет CI (`tools/verify/check_spells.py` падает, если у
заклинания нет файла иконки). Скрипт делает их воспроизводимо: сетка 16×16
описана символами, палитра — рядом, поэтому поправить цвет или форму можно
одним коммитом, не открывая графический редактор.

Фон у иконок мода — плоский цвет школы (у огня #664745, у воды #4E798D и т.д.),
поэтому здесь так же: фон школы + мотив в 3-4 оттенках.

Запуск:
    python3 tools/art/gen_spell_icons.py            # записать PNG в ресурсы
    python3 tools/art/gen_spell_icons.py --preview  # ещё и /tmp-монтаж для глаз
"""

import os
import struct
import sys
import zlib

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ICONS = os.path.join(ROOT, "src/main/resources/assets/mmorpg/textures/gui/spells/icons")

# Фоновые цвета школ — сняты с существующих иконок (fireball, frostball, ...).
BG = {
    "fire": "#664745",
    "water": "#4E798D",
    "divine": "#8A7562",
    "hunting": "#736740",
    "nature": "#5F7355",
    # Служебные/отладочные умения: серый фон, чтобы в дереве не путались с боевыми.
    "dev": "#4A4A55",
}

# Каждая иконка: школа (фон) + палитра символов + 16 строк по 16 символов.
# '.' — прозрачный/фон, остальные символы берутся из палитры.
ICONS_SPEC = {
    # --- огонь -------------------------------------------------------------
    "black_hole": {
        "school": "fire",
        "palette": {"v": "#7A3FA8", "V": "#B478E8", "K": "#140B1A", "w": "#E8D8FF"},
        "rows": [
            "................",
            ".....vvvvvv.....",
            "...vVKKKKKKv....",
            "..vVKKKKKKKKv...",
            "..vKKKKKKKKKK...",
            ".vKKKKKKKKKKKv..",
            ".vKKKKKwwwKKKK..",
            ".vKKKKwwwwwKKK..",
            ".vKKKKKwwwKKKK..",
            ".vKKKKKKKKKKKv..",
            "..vKKKKKKKKKK...",
            "..vVKKKKKKKKv...",
            "...vVKKKKKKv....",
            ".....vvvvvv.....",
            "................",
            "................",
        ],
    },
    # --- вода --------------------------------------------------------------
    "teleport": {
        "school": "water",
        "palette": {"s": "#2E9FB1", "S": "#91F0FF", "c": "#B9F5FF", "w": "#FFFFFF"},
        "rows": [
            "................",
            "......ssss......",
            "....sSSSSSSs....",
            "...sSSccccSSs...",
            "..sSSccccccSSs..",
            "..sSScc..ccSSs..",
            ".sSScc....ccSSs.",
            ".sSSc..ww..cSSs.",
            ".sSScc....ccSSs.",
            "..sSScc..ccSSs..",
            "..sSSccccccSSs..",
            "...sSSccccSSs...",
            "....sSSSSSSs....",
            "......ssss......",
            "................",
            "................",
        ],
    },
    # --- свет --------------------------------------------------------------
    "execute": {
        "school": "divine",
        "palette": {"s": "#BAA886", "W": "#FFFEFD", "r": "#C82222", "R": "#FF4C4C"},
        "rows": [
            "................",
            "............sss.",
            "...........sWWs.",
            "..........sWWs..",
            ".........sWWs...",
            "........sWWs....",
            ".......sWWs.....",
            "......sWWs......",
            ".....sWWs.......",
            "....sWWs........",
            "...sWWs.........",
            "..rrWr..........",
            ".rRRr...........",
            "..rr............",
            "................",
            "................",
        ],
    },
    "shred": {
        "school": "divine",
        "palette": {"r": "#C82222", "R": "#FF9E2F", "w": "#FFE49A"},
        "rows": [
            "................",
            "..r.............",
            "..rr..........w.",
            "...rr.........w.",
            "....rr.......rr.",
            ".....rr.....rr..",
            "......rr...rr...",
            ".......rr.rr....",
            "........rrr.....",
            ".......rrrr.....",
            "......rr..rr....",
            "..R..rr....rr...",
            "..RRrr......r...",
            "...RR...........",
            "................",
            "................",
        ],
    },
    "power_chord": {
        "school": "divine",
        "palette": {"g": "#D38317", "G": "#FFB655", "w": "#FFE49A"},
        "rows": [
            "................",
            "..........gggg..",
            ".........gGGGGg.",
            ".........gGGGGg.",
            ".........gGGGGg.",
            ".........gG..Gg.",
            ".........gG..Gg.",
            ".........gG..Gg.",
            ".........gG..Gg.",
            "....ggggggG..Gg.",
            "...gGGGGGg...g..",
            "...gGGGGGg......",
            "...gGGGGGg......",
            "....gggggg......",
            "................",
            "................",
        ],
    },
    # --- служебные ---------------------------------------------------------
    "test_command": {
        "school": "dev",
        "palette": {"g": "#4B4B4B", "G": "#7A7A7A", "W": "#D8D8D8",
                    "e": "#6BE06B"},
        "rows": [
            "................",
            "..gggggggggggg..",
            "..gGGGGGGGGGGg..",
            "..gGeeeeeeeeGg..",
            "..gGeGGGGGGeGg..",
            "..gGeGWWWWGeGg..",
            "..gGeGWGGWGeGg..",
            "..gGeGWGGWGeGg..",
            "..gGeGWWWWGeGg..",
            "..gGeGGGGGGeGg..",
            "..gGeeeeeeeeGg..",
            "..gGGGGGGGGGGg..",
            "..gggggggggggg..",
            "................",
            "................",
            "................",
        ],
    },
    # --- охота -------------------------------------------------------------
    "mark_for_death": {
        "school": "hunting",
        "palette": {"c": "#FFA14C", "w": "#BAA886", "W": "#F0E9DC",
                    "k": "#2A2418"},
        "rows": [
            "................",
            "....ww....ww....",
            ".....ww..ww.....",
            "..wwwwwwwwwwww..",
            "...wWWWWWWWWw...",
            "..wWWWWWWWWWWw..",
            "..wWWkkWWkkWWw..",
            "..wWWkkWWkkWWw..",
            "..wWWWWWWWWWWw..",
            "..wWWWWkkWWWWw..",
            "...wWWWkkkWWw...",
            "....wWWkkWWw....",
            ".....wwwwww.....",
            "...c.c.c.c.c....",
            "................",
            "................",
        ],
    },
    "jump_field": {
        "school": "hunting",
        "palette": {"c": "#FFA14C", "C": "#FFDB69", "d": "#96886C", "D": "#C2A581"},
        "rows": [
            "................",
            ".......cc.......",
            "......cCCc......",
            ".....cCCCCc.....",
            "......cCCc......",
            "......cCCc......",
            "....cc.cc.cc....",
            "...cCCccCCcC....",
            "....cc.cc.cc....",
            "......cCCc......",
            "......cCCc......",
            ".dddddddddddddd.",
            ".dDDDDDDDDDDDDd.",
            ".dddddddddddddd.",
            "................",
            "................",
        ],
    },
    "levitation": {
        "school": "hunting",
        "palette": {"c": "#FFA14C", "C": "#FFDB69", "d": "#96886C",
                    "D": "#C2A581", "w": "#E4DBD2"},
        "rows": [
            "................",
            "................",
            ".....cccccc.....",
            "....cCCCCCCc....",
            "....cCCCCCCc....",
            ".....cccccc.....",
            "................",
            "..w..........w..",
            "...w........w...",
            "....w......w....",
            "................",
            ".....dddddd.....",
            "....dDDDDDDd....",
            ".....dddddd.....",
            "................",
            "................",
        ],
    },
    "recoil_shot": {
        "school": "hunting",
        "palette": {"a": "#C2A581", "A": "#E4DBD2", "r": "#C82222", "c": "#FFA14C"},
        "rows": [
            "................",
            "..........aaaa..",
            ".........aAAAAa.",
            "..........aaaa..",
            "......c.aA......",
            ".....cc..aA.....",
            "....cc....aA....",
            "...cc......aA...",
            "..cc.......aA...",
            "...cc.....aA....",
            "....cc...aA.....",
            ".....cc.aA......",
            "......c.a.......",
            "......rr........",
            "................",
            "................",
        ],
    },
    # --- природа -----------------------------------------------------------
    "curse_of_agony": {
        "school": "nature",
        "palette": {"h": "#8F1B2B", "H": "#E24B5A", "p": "#62A71E", "P": "#A8EE55", "w": "#FFF0F0"},
        "rows": [
            "................",
            "................",
            "..hhhh....hhhh..",
            ".hHHHHh..hHHHHh.",
            ".hHHHHHhhHHHHHh.",
            ".hHHHHHHHHHHHHh.",
            ".hHHHHHHHHHHHHh.",
            "..hHHHHHHHHHHh..",
            "...hHHHHHHHHh...",
            "....hHHHHHHh....",
            ".....hHHHHh.....",
            "......hHHh......",
            ".......hh.......",
            ".....p..P.......",
            "...pPp..........",
            "..pP............",
        ],
    },
    "curse_of_weak": {
        "school": "nature",
        "palette": {"s": "#7D7D7D", "S": "#C6C6C6", "W": "#EFEFEF",
                    "r": "#8F1B2B", "p": "#62A71E", "P": "#A8EE55"},
        "rows": [
            "................",
            ".......WW.......",
            "......WSSW......",
            "......WSSW......",
            "......WSSW......",
            "......WSSW......",
            "....WWWSSWWW....",
            "......WSSW......",
            "..p...WSSW...p..",
            "...Pr.WSSW.rP...",
            "......WrrW......",
            ".....ppSSpp.....",
            "......WSSW......",
            ".......WW.......",
            "................",
            "................",
        ],
    },
    "curse_of_despair": {
        "school": "nature",
        "palette": {"d": "#2F3B52", "D": "#54648A", "L": "#8FA2CC",
                    "r": "#7D9BD6", "p": "#62A71E", "P": "#A8EE55"},
        "rows": [
            "................",
            "......dddd......",
            "....ddDDDDdd....",
            "...dDDDDDDDDd...",
            "..dDDDLLLLDDDd..",
            "..dDDDDDDDDDDd..",
            "..dDDDDDDDDDDd..",
            "...dDDDDDDDDd...",
            "....dddddddd....",
            "................",
            "....r..r..r.....",
            "....r..r..r.....",
            ".....r..r..r....",
            "................",
            "...p..P..p......",
            "..pP....pP......",
        ],
    },
}


def hex_rgb(value):
    value = value.lstrip("#")
    return tuple(int(value[i:i + 2], 16) for i in (0, 2, 4))


def write_png(path, rows, background, palette):
    """Пишет PNG 16×16 вручную: Pillow не нужен, скрипт должен работать где угодно."""
    rgba = []
    for row in rows:
        line = []
        for ch in row:
            if ch == ".":
                line.append(hex_rgb(background) + (255,))
            else:
                line.append(hex_rgb(palette[ch]) + (255,))
        rgba.append(line)

    raw = b""
    for row in rgba:
        raw += b"\x00" + b"".join(bytes(pixel) for pixel in row)

    def chunk(tag, data):
        return (struct.pack(">I", len(data)) + tag + data
                + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF))

    header = struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0)
    blob = (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", header)
            + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))
    with open(path, "wb") as handle:
        handle.write(blob)


def validate(name, spec):
    rows = spec["rows"]
    if len(rows) != 16:
        raise SystemExit("%s: строк %d, нужно 16" % (name, len(rows)))
    for index, row in enumerate(rows):
        if len(row) != 16:
            raise SystemExit("%s: строка %d длиной %d, нужно 16" % (name, index, len(row)))
        for ch in row:
            if ch != "." and ch not in spec["palette"]:
                raise SystemExit("%s: символ %r отсутствует в палитре" % (name, ch))


def main():
    os.makedirs(ICONS, exist_ok=True)
    written = []
    for name, spec in sorted(ICONS_SPEC.items()):
        validate(name, spec)
        path = os.path.join(ICONS, name + ".png")
        write_png(path, spec["rows"], BG[spec["school"]], spec["palette"])
        written.append(name)
        print("иконка: %s (%s)" % (name, spec["school"]))

    if "--preview" in sys.argv:
        # Монтаж 8× рядом с существующими иконками: видно, выбивается ли стиль.
        try:
            from PIL import Image
        except ImportError:
            print("Pillow нет — превью пропущено")
            return 0
        names = written
        sheet = Image.new("RGBA", (16 * 8 * len(names), 16 * 8), (24, 24, 30, 255))
        for i, name in enumerate(names):
            icon = Image.open(os.path.join(ICONS, name + ".png")).convert("RGBA")
            sheet.alpha_composite(icon.resize((128, 128), Image.NEAREST), (i * 128, 0))
        out = "/tmp/spell_icons_preview.png"
        sheet.save(out)
        print("превью: " + out)
    return 0


if __name__ == "__main__":
    sys.exit(main())
