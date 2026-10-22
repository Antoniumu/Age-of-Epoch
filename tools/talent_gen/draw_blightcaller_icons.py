#!/usr/bin/env python3
"""Пиксель-арт иконок древа «Скверноводец» (16×16, как остальные талантов).

Каждая иконка нарисована вручную: строка — пиксели, символ — цвет палитры,
«.» — прозрачность. Скрипт проверяет ширину строк и пишет PNG в
src/main/resources/assets/mmorpg/textures/gui/talent_icons/.

  python3 tools/talent_gen/draw_blightcaller_icons.py            # записать PNG в репозиторий
  python3 tools/talent_gen/draw_blightcaller_icons.py --preview F # превью 10x в файл F
"""
import os
import sys

from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUT = os.path.join(ROOT, "src/main/resources/assets/mmorpg/textures/gui/talent_icons")

PALETTE = {
    "K": (23, 17, 28),      # контур
    "O": (51, 36, 63),      # обсидиан
    "B": (236, 228, 204),   # кость
    "b": (181, 171, 143),   # кость, тень
    "W": (255, 255, 255),   # блик
    "G": (141, 255, 92),    # зелёная скверна
    "g": (46, 125, 50),     # зелёная, тень
    "P": (181, 107, 255),   # фиолетовое свечение
    "p": (91, 44, 145),     # фиолетовое, тень
    "D": (138, 90, 46),     # дерево, кожа, шерсть
    "d": (78, 50, 26),      # тёмное дерево
    "T": (182, 149, 99),    # шерсть светлая
    "t": (122, 90, 54),     # шерсть тёмная
    "R": (212, 58, 58),     # красный
    "r": (125, 27, 27),     # тёмно-красный
    "S": (169, 178, 187),   # сталь
    "s": (86, 96, 106),     # сталь, тень
    "Y": (242, 194, 48),    # жёлтые глаза
    "C": (223, 245, 238),   # стекло
    "c": (136, 184, 168),   # стекло, тень
}

SPRITES = {
    # Класс: череп с зелёными глазами и фиолетовым ореолом (вкладка древа)
    "big_blightcaller": [
        "................",
        ".P...........P..",
        ".....KKKKKK.....",
        "...KKBBBBBBKK...",
        "..KBBBBBBBBBBK..",
        "..KBBBBBBBBBBK..",
        "..KBKGGBBGGKBK..",
        "..KBKKGBBGKKBK..",
        "..KBBBKBBKBBBK..",
        "...KBKBKBBKBK...",
        "..P....KKKK...P.",
        "................",
        "................",
        "................",
        "................",
        "................",
    ],
    # Путь осквернителя: склянка с зелёной скверной
    "blight_vial": [
        ".....dDDDd......",
        ".....dDDDd......",
        "......KKKK......",
        "......KCCK......",
        ".....KCCCCK.....",
        "....KCCCCCCK....",
        "...KCcGGGGcCK...",
        "...KcGGGWGGcK...",
        "..KCGGGGGGGGcK..",
        "..KcGGgGGgGGcK..",
        "..KcGgGGgGGGcK..",
        "..KcGGGgGGgGcK..",
        "..KKcGGGGGGcKK..",
        "...KKKKKKKKKK...",
        "................",
        "................",
    ],
    # Владыка нежити: железная корона с фиолетовыми самоцветами
    "death_crown": [
        "................",
        ".SS....SS....SS.",
        ".SSS..SSSS..SSS.",
        ".SSSSSSSSSSSSSS.",
        ".sYYSSPPPPSSYYs.",
        "..sSSSSSSSSSSs..",
        "...ssssssssss...",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
    ],
    # Взрыв останков: череп, разлетающийся в зелёные брызги
    "corpse_burst": [
        ".G...G...G...G..",
        "..G..KKKKKK..G..",
        "...KKBBBBBBKK...",
        "..KBBBBBBBBBBK..",
        "..KBKKBBBBKKBK..",
        "..KBBGGBBGGBBK..",
        "..KBBBKBBKBBBK..",
        "...KBKBKBBKBK...",
        "G..KKKKKKKKKK..G",
        "..G..G.G.G..G...",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
    ],
    # Цепной мор: три звена цепи с зелёным свечением внутри
    "plague_chain": [
        ".SS.............",
        "S..S............",
        "S..S............",
        ".SS.SS..........",
        "...S..S.........",
        "...S..S.........",
        "....SS.SS.......",
        "......SGGS......",
        "......SGGS......",
        ".......SS.......",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
    ],
    # Чёрная воля: обсидиановое сердце с фиолетовым ядром
    "dark_will": [
        "................",
        "..KKK...KKK.....",
        ".KOOOK.KOOOK....",
        ".KOPPOKKOPPOK...",
        "KOOPPPOOOPPPOOK.",
        "KOPPPPPOOOPPPPOK",
        "KOPPPPPPPPPPPPOK",
        ".KOPPPPPPPPPPOK.",
        "..KOPPPPPPPPOK..",
        "...KOPPPPPPOK...",
        "....KOPPPPOK....",
        ".....KOPPOK.....",
        "......KOOK......",
        ".......KK.......",
        "................",
        "................",
    ],
    # Закалённые кости: рёберная броня
    "bone_armor": [
        "................",
        "................",
        "...KKKKKKKKKK...",
        "..KBBBBbbBBBBK..",
        "..KBBKKbbKKBBK..",
        "..KBBBBbbBBBBK..",
        "..KBBKKbbKKBBK..",
        "..KBBBBbbBBBBK..",
        "...KBBKbbKBBK...",
        "....KKbbbbKK....",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
    ],
    # Владыка скверны: скипетр с черепом и фиолетовым кристаллом
    "blight_lord": [
        ".......PP.......",
        "......PWPP......",
        ".....KBBBBK.....",
        "....KBBBBBBK....",
        "....KBKKBBKB....",
        "....KBBKKBBK....",
        ".....KKBBKK.....",
        "......DD........",
        ".....DD.........",
        "....DD..........",
        "...DD...........",
        "..DD............",
        ".DD.............",
        "DD..............",
        "d...............",
        "................",
    ],
    # Трофей-коготь: три когтя на шнурке
    "beast_claw": [
        "................",
        "..DDDDDDDDDDDD..",
        "...KBK.KBK.KBK..",
        "...KBK.KBK.KBK..",
        "...KBK.KBK.KBK..",
        "....KB..KB..KB..",
        "....K...K...K...",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
    ],
    # Серая стая: голова волка анфас
    "grey_wolf": [
        ".KKK........KKK.",
        ".KSSK......KSSK.",
        ".KSSSKKKKKKSSSK.",
        ".KSSSSSSSSSSSSK.",
        "KSSKYSSSSSSYKSSK",
        "KSSSSSSSSSSSSSSK",
        "KSSSSSSSSSSSSSSK",
        "KSSSSSWWWWSSSSSK",
        "KSSSSSSWWWWSSSSK",
        ".KSSSSSSKKSSSSK.",
        ".KSKKKKKKKKKKSK.",
        "..KSSSSSSSSSSK..",
        "...KKKKKKKKKK...",
        "................",
        "................",
        "................",
    ],
    # Бурый страж: голова медведя анфас
    "brown_bear": [
        "..KKK......KKK..",
        ".KDDDK....KDDDK.",
        ".KDDDDKKKKDDDDK.",
        "KDDDDDDDDDDDDDDK",
        "KDDDKDDDDDDKDDDK",
        "KDDDDDDDDDDDDDDK",
        "KDDDDDDDDDDDDDDK",
        "KDDDDDTTTTDDDDDK",
        "KDDDDDTTKKTTDDDK",
        "KDDDDDDTTTTDDDDK",
        ".KDDDDDDDDDDDDK.",
        "..KDDDDDDDDDDK..",
        "...KKKKKKKKKK...",
        "................",
        "................",
        "................",
    ],
    # Мамонт разбойников: голова мамонта с красной бандапой
    "bandit_mammoth": [
        "................",
        "....KKKKKKKK....",
        "...KTTTTTTTTK...",
        "..KTTTTTTTTTTK..",
        "..KTTRRRRRRTTK..",
        ".KTTTKTTTTKTTTK.",
        ".KTTTTTTTTTTTTK.",
        ".KTTTTTTTTTTTTK.",
        "KTTTTTTTTTTTTTTK",
        "KTTTTTKKKKTTTTTK",
        "KBTTTTKKKKTTTTBK",
        "KBBTTTTKKTTTTBBK",
        ".KBBTTTTTTTTBBK.",
        "..KKTTTTTTTTKK..",
        "....KKKKKKKK....",
        "................",
    ],
    # Альфа-хищник: огромный клык на цепи с красным свечением
    "alpha_fang": [
        "......S..S......",
        "......S..S......",
        "....KKKKKKKK....",
        "...KBBBBBBBBK...",
        "...KBBBBBBBBK...",
        "...KBWBBBBBBK...",
        "....KBBBBBBK....",
        "....KBBBBBBK....",
        ".....KBBBBK.....",
        ".....KBBBBK.....",
        "......KBBK......",
        "......KBBK......",
        ".......KK.......",
        "................",
        "................",
        "................",
    ],
}

# Соответствие: имя спрайта → файл в talent_icons (имена совпадают с путями в BlightcallerPerks.java)
FILES = {name: name + ".png" for name in SPRITES}


def render(rows):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch == ".":
                continue
            rgb = PALETTE[ch]  # KeyError — неизвестный символ, это ошибка рисования
            img.putpixel((x, y), rgb + (255,))
    return img


def check(name, rows):
    if len(rows) != 16:
        raise SystemExit("%s: %d строк вместо 16" % (name, len(rows)))
    for i, row in enumerate(rows):
        if len(row) != 16:
            raise SystemExit("%s: строка %d длиной %d вместо 16: %r" % (name, i, len(row), row))


def main(argv):
    for name, rows in SPRITES.items():
        check(name, rows)
    if "--preview" in argv:
        target = argv[argv.index("--preview") + 1]
        cell = 10
        names = list(SPRITES)
        cols = 5
        rows_n = (len(names) + cols - 1) // cols
        sheet = Image.new("RGBA", (cols * (16 * cell + 12), rows_n * (16 * cell + 12)), (40, 36, 46, 255))
        for i, name in enumerate(names):
            big = render(SPRITES[name]).resize((16 * cell, 16 * cell), Image.NEAREST)
            x = (i % cols) * (16 * cell + 12) + 6
            y = (i // cols) * (16 * cell + 12) + 6
            sheet.alpha_composite(big, (x, y))
        sheet.save(target)
        print("превью:", target)
        return 0
    os.makedirs(OUT, exist_ok=True)
    for name, rows in SPRITES.items():
        render(rows).save(os.path.join(OUT, FILES[name]))
    print("записано иконок:", len(SPRITES), "→", os.path.relpath(OUT, ROOT))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
