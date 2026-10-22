#!/usr/bin/env python3
"""Иконки классов и школ: персонажи генерируются ИИ отдельно, а фон и рамка рисуются здесь.

Так рамка одинакова у всех иконок: меняется только цвет палитры и персонаж.

Команды:
  cut <raw_dir>   из сырых генераций (персонаж на белом фоне) сделать прозрачные фигуры
                  в tools/talent_gen/class_icon_subjects/ (256 px)
  build           собрать иконки 128×128: процедурный фон, общая рамка, фигура с тенью
  check           проверить, что все выходные файлы есть и имеют размер 128×128

Пример:
  python3 tools/talent_gen/make_class_icons.py cut /path/to/raw
  python3 tools/talent_gen/make_class_icons.py build
  python3 tools/talent_gen/make_class_icons.py check
"""
import os
import struct
import sys
from collections import deque

from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.abspath(os.path.join(HERE, '..', '..'))
SUBJ_DIR = os.path.join(HERE, 'class_icon_subjects')
TEX_DIR = os.path.join(REPO, 'src', 'main', 'resources', 'assets', 'mmorpg', 'textures', 'gui')

SIZE = 128
SUBJECT_BOX = 100   # фигура вписывается в квадрат 100×100
SUBJECT_PX = 256    # размер сохранённых фигур

# Палитры: top/bottom — градиент фона, accent — внутренняя линия рамки, outer — внешняя рамка и тень.
GOLD = dict(top=(247, 234, 184), bottom=(168, 118, 40), accent=(255, 217, 106), outer=(74, 50, 16))
GREEN = dict(top=(62, 96, 56), bottom=(12, 26, 14), accent=(139, 227, 106), outer=(8, 14, 8))
RED = dict(top=(214, 70, 36), bottom=(70, 14, 8), accent=(255, 154, 60), outer=(40, 8, 4))
OLIVE = dict(top=(222, 214, 166), bottom=(110, 104, 62), accent=(232, 217, 160), outer=(58, 53, 32))
LIME = dict(top=(168, 220, 80), bottom=(60, 107, 28), accent=(212, 255, 122), outer=(30, 58, 16))
VIOLET = dict(top=(140, 92, 216), bottom=(42, 20, 80), accent=(201, 168, 255), outer=(26, 11, 48))
CRIMSON = dict(top=(122, 22, 34), bottom=(20, 16, 30), accent=(224, 64, 79), outer=(14, 6, 9))
BLUE = dict(top=(191, 232, 247), bottom=(62, 143, 196), accent=(232, 249, 255), outer=(27, 78, 116))

# (id, имя исходного файла без .png, путь вывода относительно textures/gui, палитра)
ICONS = [
    ('divine', 'subj_divine', 'spells/schools/divine.png', GOLD),
    ('fire', 'subj_fire', 'spells/schools/fire.png', RED),
    ('hunting', 'subj_hunting', 'spells/schools/hunting.png', OLIVE),
    ('nature', 'subj_nature', 'spells/schools/nature.png', LIME),
    ('storm', 'subj_storm', 'spells/schools/storm.png', VIOLET),
    ('unholy', 'subj_unholy', 'spells/schools/unholy.png', CRIMSON),
    ('water', 'subj_water', 'spells/schools/water.png', BLUE),
    ('class_divine', 'subj_class_divine', 'talent_icons/class_divine.png', GOLD),
    ('class_blightcaller', 'subj_class_blightcaller', 'talent_icons/class_blightcaller.png', GREEN),
]


def white_bg_to_alpha(im, thr=236):
    """Заливка от краёв только по почти белым пикселям: внутренние светлые места фигуры остаются."""
    im = im.convert('RGBA')
    w, h = im.size
    px = im.load()
    seen = bytearray(w * h)
    q = deque()
    for x in range(w):
        q.append((x, 0))
        q.append((x, h - 1))
    for y in range(h):
        q.append((0, y))
        q.append((w - 1, y))
    while q:
        x, y = q.popleft()
        if x < 0 or y < 0 or x >= w or y >= h:
            continue
        i = y * w + x
        if seen[i]:
            continue
        r, g, b, a = px[x, y]
        if min(r, g, b) < thr:
            continue
        seen[i] = 1
        px[x, y] = (r, g, b, 0)
        q.extend(((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)))
    return im


def cut(raw_dir):
    os.makedirs(SUBJ_DIR, exist_ok=True)
    for name, stem, _out, _pal in ICONS:
        src = os.path.join(raw_dir, stem + '.png')
        im = white_bg_to_alpha(Image.open(src))
        bbox = im.getchannel('A').getbbox()
        im = im.crop(bbox)
        k = SUBJECT_PX / max(im.size)
        im = im.resize((max(1, round(im.width * k)), max(1, round(im.height * k))), Image.LANCZOS)
        im.save(os.path.join(SUBJ_DIR, name + '.png'))
        print('cut', name, im.size)


def background(pal):
    img = Image.new('RGBA', (SIZE, SIZE), pal['top'] + (255,))
    d = ImageDraw.Draw(img)
    top, bot = pal['top'], pal['bottom']
    for y in range(SIZE):
        t = y / (SIZE - 1)
        c = tuple(round(top[i] + (bot[i] - top[i]) * t) for i in range(3))
        d.line([(0, y), (SIZE - 1, y)], fill=c + (255,))
    return img


def frame(img, pal):
    """Одинаковая для всех рамка: внешняя тёмная, внутренняя цветная линия, тонкая тёмная, уголки."""
    d = ImageDraw.Draw(img)
    outer = pal['outer'] + (255,)
    accent = pal['accent'] + (255,)
    d.rectangle([0, 0, SIZE - 1, SIZE - 1], outline=outer, width=4)
    d.rectangle([5, 5, SIZE - 6, SIZE - 6], outline=accent, width=2)
    d.rectangle([8, 8, SIZE - 9, SIZE - 9], outline=outer, width=1)
    for cx, cy in ((11, 11), (SIZE - 12, 11), (11, SIZE - 12), (SIZE - 12, SIZE - 12)):
        r = 2
        d.polygon([(cx, cy - r), (cx + r, cy), (cx, cy + r), (cx - r, cy)], fill=accent)


def subject_layer(subj, pal):
    k = SUBJECT_BOX / max(subj.size)
    fig = subj.resize((max(1, round(subj.width * k)), max(1, round(subj.height * k))), Image.LANCZOS)
    x = (SIZE - fig.width) // 2
    y = (SIZE - fig.height) // 2 + 2
    shadow = Image.new('RGBA', fig.size, pal['outer'] + (0,))
    shadow.putalpha(fig.getchannel('A').filter(ImageFilter.GaussianBlur(2)).point(lambda a: a * 3 // 4))
    layer = Image.new('RGBA', (SIZE, SIZE), (0, 0, 0, 0))
    layer.alpha_composite(shadow, (x, y + 2))
    layer.alpha_composite(fig, (x, y))
    return layer


def build():
    for name, _stem, out, pal in ICONS:
        subj = Image.open(os.path.join(SUBJ_DIR, name + '.png')).convert('RGBA')
        img = background(pal)
        frame(img, pal)
        img.alpha_composite(subject_layer(subj, pal))
        path = os.path.join(TEX_DIR, out)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        img.save(path)
        print('build', out)


def check():
    bad = 0
    for _name, _stem, out, _pal in ICONS:
        path = os.path.join(TEX_DIR, out)
        if not os.path.exists(path):
            print('нет файла:', out)
            bad += 1
            continue
        with open(path, 'rb') as f:
            head = f.read(24)
        w, h = struct.unpack('>II', head[16:24])
        if (w, h) != (SIZE, SIZE):
            print('размер %dx%d: %s' % (w, h, out))
            bad += 1
    print('check: ok' if bad == 0 else 'check: ошибок %d' % bad)
    return bad


def main(argv):
    if len(argv) < 2:
        print(__doc__)
        return 2
    cmd = argv[1]
    if cmd == 'cut':
        if len(argv) < 3:
            print('нужен путь к каталогу с сырыми генерациями')
            return 2
        cut(argv[2])
        return 0
    if cmd == 'build':
        build()
        return 0
    if cmd == 'check':
        return 1 if check() else 0
    print(__doc__)
    return 2


if __name__ == '__main__':
    sys.exit(main(sys.argv))
