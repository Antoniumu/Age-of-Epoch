#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Рендер макетов («тайлов») категорий JEI для Age of Epoch.

Это НЕ скриншоты игры, а схемы страниц JEI для согласования дизайна:
панели имеют JEI-размер 170 логических пикселей по ширине, слоты стоят
на тех же координатах, что в существующих категориях/проекте новых,
а иконки по возможности являются настоящими текстурами мода.

Запуск:  python3 tools/jei_tiles/render_tiles.py
Результат: docs/jei-tiles/*.png (+ общий лист 00-all-tiles.png)
"""
import os
from PIL import Image, ImageDraw, ImageFont

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
ASSETS = os.path.join(REPO, "src/main/resources/assets/mmorpg")
OUT = os.path.join(REPO, "docs/jei-tiles")
S = 4  # масштаб: 1 логический пиксель JEI = 4 px макета

FONT_DIR = "/usr/share/fonts/truetype/dejavu"
F_TITLE = ImageFont.truetype(f"{FONT_DIR}/DejaVuSans-Bold.ttf", 29)
F_ID = ImageFont.truetype(f"{FONT_DIR}/DejaVuSans.ttf", 18)
F_TEXT = ImageFont.truetype(f"{FONT_DIR}/DejaVuSans.ttf", 21)
F_SMALL = ImageFont.truetype(f"{FONT_DIR}/DejaVuSans.ttf", 18)
F_BADGE = ImageFont.truetype(f"{FONT_DIR}/DejaVuSans-Bold.ttf", 18)
F_NOTE = ImageFont.truetype(f"{FONT_DIR}/DejaVuSans-Oblique.ttf", 20) \
    if os.path.exists(f"{FONT_DIR}/DejaVuSans-Oblique.ttf") else F_TEXT

PANEL = (198, 198, 198)
PANEL_DARK = (85, 85, 85)
INK = (38, 38, 38)
MUTED = (82, 82, 82)
NOTE_BG = (38, 34, 28)
NOTE_FG = (255, 210, 130)
_MEASURE = ImageDraw.Draw(Image.new("RGB", (8, 8)))


def wrap_text(text, font, max_width):
    """Перенос текста по реальной ширине шрифта; пустая строка сохраняется."""
    if text == "":
        return [""]
    out = []
    for paragraph in str(text).split("\n"):
        words = paragraph.split()
        if not words:
            out.append("")
            continue
        line = ""
        for word in words:
            candidate = word if not line else line + " " + word
            if _MEASURE.textlength(candidate, font=font) <= max_width:
                line = candidate
                continue
            if line:
                out.append(line)
                line = ""
            # Очень длинное слово всё равно нельзя выпускать за панель.
            while word and _MEASURE.textlength(word, font=font) > max_width:
                cut = max(1, len(word) - 1)
                while cut > 1 and _MEASURE.textlength(word[:cut] + "…", font=font) > max_width:
                    cut -= 1
                out.append(word[:cut] + "…")
                word = word[cut:]
            line = word
        if line:
            out.append(line)
    return out


def asset(rel):
    path = os.path.join(ASSETS, rel)
    img = Image.open(path).convert("RGBA")
    # У некоторых текстур атлас выше 16 px (анимация) — берём первый кадр.
    if img.height > img.width:
        img = img.crop((0, 0, img.width, img.width))
    return img.resize((16 * S, 16 * S), Image.NEAREST)


def tex(rel):
    return asset("textures/items/" + rel)


def iron_chest():
    """Ванильного нагрудника в репозитории нет — рисуем схематичную иконку."""
    img = Image.new("RGBA", (16 * S, 16 * S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    c, edge = (216, 216, 216, 255), (90, 90, 90, 255)
    d.polygon([(3*S,2*S),(6*S,2*S),(6*S,4*S),(10*S,4*S),(10*S,2*S),(13*S,2*S),
               (14*S,6*S),(12*S,7*S),(12*S,14*S),(4*S,14*S),(4*S,7*S),(2*S,6*S)],
              fill=c, outline=edge, width=2)
    d.line([(8*S,4*S),(8*S,14*S)], fill=edge, width=2)
    return img


def iron_sword():
    img = Image.new("RGBA", (16 * S, 16 * S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Толстый диагональный клинок, гарда и рукоять — схема ванильного меча.
    d.polygon([(10*S,1*S),(15*S,1*S),(15*S,4*S),(7*S,12*S),(5*S,10*S)],
              fill=(224,224,224,255), outline=(88,88,88,255), width=2)
    d.line([(13*S,3*S),(7*S,9*S)], fill=(170,170,170,255), width=2)
    d.line([(3*S,9*S),(7*S,13*S)], fill=(170,120,35,255), width=4)
    d.line([(2*S,13*S),(5*S,10*S)], fill=(105,65,25,255), width=4)
    return img


def zombie_face():
    """Схематичный маркер источника «мобы» для тайла источников."""
    img = Image.new("RGBA", (16 * S, 16 * S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([2*S, 2*S, 14*S, 14*S], fill=(72, 128, 62, 255), outline=(35, 75, 32, 255), width=2)
    d.rectangle([4*S, 6*S, 6*S, 8*S], fill=(20, 25, 20, 255))
    d.rectangle([10*S, 6*S, 12*S, 8*S], fill=(20, 25, 20, 255))
    d.rectangle([7*S, 9*S, 9*S, 11*S], fill=(50, 95, 45, 255))
    d.line([5*S, 13*S, 11*S, 13*S], fill=(25, 45, 25, 255), width=2)
    return img


class Tile:
    def __init__(self, w, h, title, type_id, note_lines, header_icon=None):
        self.w, self.h = w, h
        self.note_lines = []
        for line in note_lines:
            self.note_lines.extend(wrap_text(line, F_NOTE, (w - 5) * S))
        head_h = 34
        note_h = 9 * len(self.note_lines) + 8
        self.img = Image.new("RGB", ((w + 8) * S, (h + head_h + note_h + 8) * S), (30, 30, 34))
        self.d = ImageDraw.Draw(self.img, "RGBA")
        self.ox, self.oy = 4 * S, (head_h + 4) * S

        # Заголовок-вкладка категории: две строки, ID никогда не налезает на название.
        self.d.rectangle([4*S, 4*S, (w+4)*S, head_h*S], fill=(60, 60, 66))
        if header_icon is not None:
            self.img.paste(header_icon, (6*S, 7*S), header_icon)
        self.d.text((26*S, 5*S), title, font=F_TITLE, fill=(255, 255, 255))
        self.d.text((26*S, 21*S), type_id, font=F_ID, fill=(172, 172, 182))

        # Панель рецепта.
        self.d.rectangle([self.ox, self.oy, self.ox + w*S, self.oy + h*S], fill=PANEL)
        self.d.rectangle([self.ox, self.oy, self.ox + w*S, self.oy + h*S], outline=PANEL_DARK, width=2)

        # Подпись-пояснение макета (не является частью самого JEI).
        ny = self.oy + h*S + 4*S
        self.d.rectangle([4*S, ny, (w+4)*S, ny + note_h*S - 4*S], fill=NOTE_BG)
        for i, line in enumerate(self.note_lines):
            self.d.text((7*S, ny + (3 + 9*i)*S), line, font=F_NOTE, fill=NOTE_FG)

    def slot(self, x, y, item=None, badge=None, cycle=False, count=None, question=False, dim=False):
        px, py = self.ox + x*S, self.oy + y*S
        sz = 18 * S
        self.d.rectangle([px, py, px+sz, py+sz], fill=(139, 139, 139))
        self.d.line([px, py, px+sz, py], fill=(55, 55, 55), width=3)
        self.d.line([px, py, px, py+sz], fill=(55, 55, 55), width=3)
        self.d.line([px, py+sz, px+sz, py+sz], fill=(255, 255, 255), width=3)
        self.d.line([px+sz, py, px+sz, py+sz], fill=(255, 255, 255), width=3)
        if item is not None:
            it = item.copy()
            if dim:
                it.putalpha(it.getchannel("A").point(lambda a: int(a * 0.48)))
            self.img.paste(it, (px + S, py + S), it)
        if question:
            self.d.text((px + 6*S, py + 0), "?", font=F_TITLE, fill=(255, 255, 120), stroke_width=1, stroke_fill=(70, 70, 20))
        if count:
            t = str(count)
            tw = self.d.textlength(t, font=F_BADGE)
            self.d.text((px + sz - tw - 3, py + sz - 25), t, font=F_BADGE,
                        fill=(255, 255, 255), stroke_width=1, stroke_fill=(45, 45, 45))
        if cycle:
            # Компактный ↻ внутри слота: список вариантов JEI будет листать сам.
            bx, by = px + 1*S, py + sz - 7*S
            self.d.rounded_rectangle([bx, by, bx + 7*S, by + 6*S], 4, fill=(35, 92, 190))
            self.d.text((bx + 4, by - 1), "↻", font=F_BADGE, fill=(255, 255, 255))
        if badge:
            bx, by = px + sz - 8*S, py - 4*S
            col = (40, 110, 220) if badge == "U" else (30, 150, 80)
            self.d.rounded_rectangle([bx, by, bx + 8*S, by + 7*S], 5, fill=col)
            tw = self.d.textlength(badge, font=F_BADGE)
            self.d.text((bx + (8*S-tw)//2, by), badge, font=F_BADGE, fill=(255, 255, 255))

    def empty_slots_grid(self, xs, ys, filled):
        for i, (x, y) in enumerate(zip(xs, ys)):
            if i not in filled:
                self.slot(x, y)

    def plus(self, x, y):
        px, py = self.ox + x*S, self.oy + y*S
        self.d.line([px, py+6*S, px+12*S, py+6*S], fill=INK, width=4)
        self.d.line([px+6*S, py, px+6*S, py+12*S], fill=INK, width=4)

    def arrow(self, x, y):
        px, py = self.ox + x*S, self.oy + y*S
        self.d.rectangle([px, py+4*S, px+16*S, py+8*S], fill=(110, 110, 110))
        self.d.polygon([(px+15*S, py+1*S), (px+23*S, py+6*S), (px+15*S, py+11*S)],
                       fill=(110, 110, 110))

    def _draw_wrapped(self, texts, x, y, max_x, color, font, step):
        yy = y
        max_width = max(20*S, (max_x - x) * S)
        for text in texts:
            for line in wrap_text(text, font, max_width):
                self.d.text((self.ox + x*S, self.oy + yy*S), line, font=font, fill=color)
                yy += step
        return yy

    def lines(self, texts, x=1, y=26, color=INK, step=9, max_x=None):
        return self._draw_wrapped(texts, x, y, self.w - 1 if max_x is None else max_x,
                                   color, F_TEXT, step)

    def label(self, x, y, t, color=MUTED, max_x=None, step=8):
        return self._draw_wrapped([t], x, y, self.w - 1 if max_x is None else max_x,
                                   color, F_SMALL, step)

    def save(self, name):
        os.makedirs(OUT, exist_ok=True)
        path = os.path.join(OUT, name)
        self.img.save(path)
        print("->", path)
        return path


CHEST = iron_chest()
SWORD = iron_sword()
ZOMBIE = zombie_face()
SOUL_CHEST = tex("stat_soul/chest.png")
SOUL_ANY = tex("stat_soul/stat_soul.png")
SOUL_Q = tex("stat_soul/question.png")
SOUL_RING = tex("stat_soul/ring.png")
RING = tex("jewelry/ring/diamond.png")


def t01_profession():
    t = Tile(170, 128, "Ремесло профессий", "mmorpg:profession_crafting · уже есть, улучшаем",
             ["U по ингредиенту/инструменту, R по результату. Новое: видимый слот катализатора и листaющиеся базы/результаты."],
             tex("craft_tools/armor_craft.png"))
    xs, ys = [1, 19, 37, 1, 19, 37], [1, 1, 1, 19, 19, 19]
    t.empty_slots_grid(xs, ys, filled={0, 2})
    t.slot(1, 1, CHEST, badge="U", cycle=True)
    t.slot(37, 1, tex("ingredient/fresh_heart.png"), badge="U", cycle=True)
    t.arrow(66, 3)
    t.slot(92, 1, CHEST, badge="R", cycle=True)
    t.slot(92, 24, tex("craft_tools/armor_craft.png"), badge="U")
    t.label(112, 25, "катализатор; не расходуется")
    t.lines([
        "Сетка 3×2: один слот — снаряжение без статов, остальные — ингредиенты.",
        "Шанс: 100% −10% за каждый ингредиент после первого; уровень профессии может дать +5/+10%.",
        "↻: базы профессии, ранги ингредиента и все варианты расходника листаются в слотах.",
    ], y=50)
    return t.save("01-profession-crafting.png")


def t02_runeword():
    t = Tile(170, 112, "Рунные слова", "mmorpg:runewords · уже есть, улучшаем",
             ["U по руне и базе, R по уникальному результату. Новое: база листает все подходящие физические предметы."],
             tex("runeword.png"))
    xs, ys = [1, 19, 37, 1, 19, 37], [1, 1, 1, 19, 19, 19]
    for i, rune in enumerate(["ano", "toq", "mos", "har"]):
        t.slot(xs[i], ys[i], tex(f"runes/{rune}.png"), badge="U" if i == 0 else None)
    t.empty_slots_grid(xs, ys, filled={0, 1, 2, 3})
    t.slot(1, 40, SWORD, badge="U", cycle=True)
    t.arrow(66, 42)
    t.slot(92, 40, SWORD, badge="R")
    t.label(112, 42, "уникальный меч")
    t.lines([
        "«Черепоруб»: ano + toq + mos + har.",
        "База: меч (↻ все совместимые мечи).",
        "Положите базу и руны в станцию рунных слов.",
    ], y=64)
    return t.save("02-runewords.png")


def t03_salvage():
    t = Tile(170, 116, "Разбор снаряжения", "mmorpg:salvage · уже есть, расширяем",
             ["U по снаряжению, душе и эссенции редкости; R по пыли и Источнику силы."],
             tex("salvage_hammer.png"))
    t.slot(1, 1, CHEST, badge="U", cycle=True)
    t.arrow(26, 3)
    t.slot(52, 1, tex("mat/salvage/salvage2.png"), badge="R", count=2)
    t.slot(70, 1, tex("rarity/essence/rare.png"), count=1)
    t.lines([
        "Перетащите молот разбора на предмет. Редкость задаёт диапазон пыли и эссенцию редкости.",
    ], y=24)
    t.slot(1, 48, SOUL_CHEST, cycle=True)
    t.arrow(26, 50)
    t.slot(52, 48, tex("mat/salvage/salvage2.png"), count=2)
    t.label(74, 50, "душа: та же формула")
    t.slot(1, 72, tex("rarity/essence/rare.png"))
    t.arrow(26, 74)
    t.slot(52, 72, tex("source_of_strength.png"), count=2)
    t.label(74, 74, "эссенция → Источник силы")
    t.lines(["В JEI будут отдельные страницы: снаряжение, души и эссенции каждой редкости."], y=96)
    return t.save("03-salvage.png")


def t04_currency():
    t = Tile(170, 132, "Валюта снаряжения", "mmorpg:currency_on_gear · уже есть, улучшаем",
             ["U по валюте, U/R по снаряжению. Новое: настоящие образцы под требования; самоцветы вынесены в тайл 07."],
             tex("currency/orb_of_transmutation.png"))
    t.slot(1, 1, CHEST, badge="U", cycle=True)
    t.plus(26, 3)
    t.slot(52, 1, tex("currency/orb_of_transmutation.png"), badge="U")
    t.arrow(76, 3)
    t.slot(104, 1, CHEST, badge="R", cycle=True)
    t.label(1, 21, "обычное", max_x=50)
    t.label(52, 21, "шар трансмутации", max_x=101)
    t.label(104, 21, "необычное")
    t.lines([
        "Превращает обычный предмет в более редкий.",
        "Перетащите валюту на снаряжение в инвентаре.",
        "Требования читаются из эффекта: снаряжение, обычная редкость, есть следующая редкость.",
        "Образцы генерируются под требования валюты; точные аффиксы результата случайны.",
    ], y=42)
    return t.save("04-currency.png")


def t05_soul():
    t = Tile(170, 132, "Души снаряжения", "mmorpg:stat_soul · НОВАЯ",
             ["Ключевой сценарий: U по душе — совместимые базы. U по пустой базе — подходящие души. R по готовому предмету — эта страница."],
             SOUL_CHEST)
    t.slot(1, 1, CHEST, badge="U", cycle=True)
    t.plus(26, 3)
    t.slot(52, 1, SOUL_CHEST, badge="U", cycle=True)
    t.arrow(76, 3)
    t.slot(104, 1, CHEST, badge="R", cycle=True)
    t.label(1, 21, "пустое снаряжение", max_x=50)
    t.label(52, 21, "душа слота", max_x=101)
    t.label(104, 21, "готовое снаряжение")
    t.lines([
        "Возьмите душу курсором и нажмите по пустому снаряжению.",
        "Слот души должен совпадать со слотом предмета; душа без слота подходит любому снаряжению.",
        "Редкость, ранг и уникальный шаблон берутся из души; характеристики генерируются при наложении.",
        "Компактно: одна страница на слот, а базы и души всех рангов/ред­костей листаются (↻).",
    ], y=42)
    return t.save("05-stat-soul.png")


def t06_identify():
    t = Tile(170, 118, "Опознание и создание душ", "mmorpg:soul_identify · НОВАЯ",
             ["U по тому, душе и производителю. Том сканирует весь инвентарь и тратит один экземпляр на душу."],
             tex("identify_tome.png"))
    t.slot(1, 1, SOUL_Q, badge="U")
    t.plus(26, 3)
    t.slot(52, 1, tex("identify_tome.png"), badge="U")
    t.arrow(76, 3)
    t.slot(104, 1, SOUL_CHEST, badge="R")
    t.lines([
        "ПКМ томом: душа без встроенного снаряжения получает готовые статы; её тултип показывает предмет до наложения.",
    ], y=25)
    t.slot(1, 66, tex("common_soul_produce.png"), badge="U")
    t.arrow(26, 68)
    t.slot(52, 66, SOUL_ANY, badge="R")
    t.label(74, 67, "Вторая страница: ПКМ создаёт обычную душу любого слота ранга игрока; её нельзя разобрать.")
    return t.save("06-soul-identify.png")


def t07_gems():
    t = Tile(170, 190, "Самоцветы и гнёзда", "mmorpg:gem_socketing · НОВАЯ",
             ["Три вида страниц: вставка, улучшение 3→1 и извлечение. U по камню показывает все три."],
             tex("gems/ruby/3.png"))
    t.label(1, 1, "1) Вставка в гнездо")
    t.slot(1, 11, CHEST, badge="U", cycle=True)
    t.plus(26, 13)
    t.slot(52, 11, tex("gems/ruby/3.png"), badge="U")
    t.arrow(76, 13)
    t.slot(104, 11, CHEST, badge="R", cycle=True)
    t.lines([
        "Нужно свободное гнездо; второй камень того же типа в предмете запрещён.",
        "Рубин: броня — сопротивление огню, оружие — урон огнём, украшения — урон заклинаний огня.",
    ], y=34)

    t.label(1, 70, "2) Улучшение ранга: держать ПКМ")
    t.slot(1, 80, tex("gems/ruby/2.png"), badge="U", count=3)
    t.arrow(26, 82)
    t.slot(52, 80, tex("gems/ruby/3.png"), badge="R")
    t.lines([
        "3 одинаковых камня ранга 2: шанс 50% получить ранг 3; при провале все 3 уничтожаются.",
        "Цепочка шансов: 100 / 75 / 50 / 25 / 10 / 5%. Ранг 6 — максимум.",
    ], y=103)

    t.label(1, 137, "3) Извлечение")
    t.slot(1, 147, CHEST, badge="U")
    t.plus(26, 149)
    t.slot(52, 147, tex("socket_extractor.png"), badge="U")
    t.arrow(76, 149)
    t.slot(104, 147, tex("gems/ruby/3.png"), badge="R")
    t.slot(126, 147, CHEST)
    t.lines(["Извлекатель возвращает первый вставленный камень; снаряжение остаётся у игрока."], y=170)
    return t.save("07-gem-socketing.png")


def t08_rarity():
    t = Tile(170, 126, "Улучшение редкости", "mmorpg:rarity_upgrade · НОВАЯ",
             ["Вся цепочка в одном месте: разбор → эссенция редкости → камень → предмет следующей редкости."],
             tex("rarity/upgrade_stone/rare.png"))
    t.slot(1, 1, tex("rarity/essence/rare.png"), badge="U", count=9)
    t.arrow(26, 3)
    t.slot(52, 1, tex("rarity/upgrade_stone/rare.png"), badge="R")
    t.label(74, 3, "ПКМ по стеку: 9 эссенций одной редкости → камень")
    t.slot(1, 38, CHEST, badge="U", cycle=True)
    t.plus(26, 40)
    t.slot(52, 38, tex("rarity/upgrade_stone/rare.png"), badge="U")
    t.arrow(76, 40)
    t.slot(104, 38, CHEST, badge="R", cycle=True)
    t.lines([
        "Камень работает только на предмете своей редкости.",
        "Порог уровня улучшения: необычная редкость — +4, редкая — +8.",
        "Старые статы сохраняются; добавляются аффиксы новой редкости и слоты улучшения.",
    ], y=62)
    return t.save("08-rarity-upgrade.png")


def t09_repair():
    t = Tile(170, 116, "Ремонт эссенцией", "mmorpg:repair · НОВАЯ",
             ["Сейчас эта механика в JEI не видна. Будет одна страница на каждый из пяти рангов пыли."],
             tex("mat/salvage/salvage2.png"))
    t.slot(1, 1, CHEST, badge="U", cycle=True, dim=True)
    t.plus(26, 3)
    t.slot(52, 1, tex("mat/salvage/salvage2.png"), badge="U")
    t.arrow(76, 3)
    t.slot(104, 1, CHEST, badge="R", cycle=True)
    t.label(1, 21, "повреждённое", max_x=50)
    t.label(52, 21, "ранг 2: 200", max_x=101)
    t.label(104, 21, "отремонтированное")
    t.lines([
        "Возьмите очищенную эссенцию курсором и нажмите по повреждённому снаряжению с данными.",
        "Ремонт по рангам 0–4: 50 / 100 / 200 / 300 / 500 прочности.",
        "Если ранг снаряжения выше ранга эссенции, ремонт делится на 5.",
    ], y=42)
    return t.save("09-repair.png")


def t10_backpack():
    t = Tile(170, 116, "Улучшения сумки", "mmorpg:backpack_upgrade · НОВАЯ",
             ["U по улучшению и сумке. Страница на каждое из 14 улучшений; размеры 0–4 показаны как цепочка."],
             tex("backpack/normal.png"))
    t.slot(1, 1, tex("backpack/normal.png"), badge="U")
    t.plus(26, 3)
    t.slot(52, 1, tex("backpack/upgrade/gem1.png"), badge="U")
    t.arrow(76, 3)
    t.slot(104, 1, tex("backpack/normal.png"), badge="R")
    t.lines([
        "Перетащите улучшение на сумку искателя приключений.",
        "Пример: автоподбор самоцветов отправляет поднятые камни сразу внутрь сумки.",
        "Дубликаты запрещены; улучшения размера ставятся последовательно и заменяют меньший размер.",
    ], y=27)
    return t.save("10-backpack-upgrade.png")


def t11_crate():
    t = Tile(170, 126, "Содержимое сундуков", "mmorpg:crate_opening · НОВАЯ",
             ["U по сундуку — пул наград, R по награде — сундуки-источники. Для данж-сундуков видны веса и джекпот."],
             tex("loot_crate/gem.png"))
    t.slot(1, 20, tex("loot_crate/gem.png"), badge="U", cycle=True)
    t.arrow(26, 22)
    outs = [tex("gems/ruby/2.png"), tex("gems/sapphire/2.png"), tex("gems/emerald/2.png"),
            tex("gems/topaz/2.png"), tex("gems/amethyst/2.png"), tex("gems/azurite/2.png"),
            tex("gems/garnet/2.png"), tex("gems/opal/2.png"), tex("gems/tourmaline/2.png")]
    for i, icon in enumerate(outs):
        t.slot(56 + (i % 3) * 19, 2 + (i // 3) * 19, icon, badge="R" if i == 0 else None)
    t.label(116, 17, "1 случайный результат")
    t.lines([
        "ПКМ — открыть. У сундука камней ранг награды равен рангу сундука; у рун и валюты — не выше.",
    ], y=64)
    t.slot(1, 90, tex("custom_crate.png"), badge="U")
    t.arrow(26, 92)
    t.slot(56, 90, SOUL_ANY, badge="R", cycle=True)
    t.label(78, 91, "Вторая страница: сундук редкости → душа этой редкости")
    return t.save("11-crate-opening.png")


def t12_unique():
    t = Tile(170, 132, "Уникальное снаряжение", "mmorpg:unique_gear · НОВАЯ",
             ["Каталог целей и способ получения: конкретная уникальная душа + база. Рунные слова ссылаются на тайл 02."],
             RING)
    t.slot(1, 1, SOUL_RING, badge="U")
    t.plus(26, 3)
    t.slot(52, 1, RING, badge="U", cycle=True)
    t.arrow(76, 3)
    t.slot(104, 1, RING, badge="R")
    t.label(1, 21, "уникальная душа", max_x=50)
    t.label(52, 21, "пустое кольцо", max_x=101)
    t.label(104, 21, "уникат")
    t.lines([
        "«Ведьмин отвар» — уникальное кольцо.",
        "+15–25% сопротивления земле; +25–50 к силе пищевых баффов;",
        "+10–15 к урону заклинаний; +5–10% к регенерации маны.",
        "Источник: душа с шаблоном witch_brew из дропа снаряжения. Значения в диапазонах перебрасываются.",
    ], y=42)
    return t.save("12-unique-gear.png")


def t13_info():
    t = Tile(170, 142, "Справка по предмету", "jei:information · расширить на все предметы",
             ["Гарантия «все предметы»: текст не дублируется вручную, а берётся из ItemDescriptions — тех же Alt-описаний."],
             tex("identify_tome.png"))
    t.slot(76, 2, tex("identify_tome.png"))
    t.lines([
        "Том определения",
        "",
        "ПКМ — определить души снаряжения в инвентаре.",
        "Каждая душа без характеристик получает готовые данные снаряжения.",
        "На каждую душу тратится одна книга.",
        "Души, у которых уже есть характеристики, книга не трогает.",
        "",
        "Такая U-страница строится для всех 245 базовых Item ID и ключевых вариантов с данными.",
    ], y=26)
    return t.save("13-info-pages.png")


def t14_actions():
    t = Tile(170, 126, "Действия и расположение", "mmorpg:item_actions · НОВАЯ",
             ["Преобразования без верстака тоже должны находиться через U/R, а не только текстом справки."],
             tex("full_favor.png"))
    t.slot(1, 1, tex("empty_favor.png"), badge="U")
    t.arrow(26, 3)
    t.slot(52, 1, tex("full_favor.png"), badge="R")
    t.label(74, 3, "ПКМ при уровне 20+: списывает 1000 расположения")
    t.slot(1, 48, tex("full_favor.png"), badge="U")
    t.arrow(26, 50)
    t.label(54, 52, "+1000 расположения игроку", color=(20, 110, 65))
    t.lines([
        "Отдельные страницы получат преобразования с предметным входом/выходом и ресурсными условиями.",
        "Предметы только с эффектом на игрока дополнительно покрываются справкой тайла 13.",
    ], y=78)
    return t.save("14-item-actions.png")


def t15_sources():
    t = Tile(170, 132, "Источники в мире", "mmorpg:world_sources · НОВАЯ",
             ["R по душе, ингредиенту или лут-сундуку ведёт не в пустоту, а на страницу источника. Это не рецепт крафта."],
             tex("loot_crate/gem.png"))
    t.slot(1, 20, ZOMBIE, dim=False)
    t.label(1, 41, "мобы / сундуки", max_x=50)
    t.arrow(27, 22)
    outs = [
        (SOUL_CHEST, "R"),
        (tex("ingredient/fresh_heart.png"), None),
        (tex("loot_crate/gem.png"), None),
        (tex("loot_crate/rune.png"), None),
        (asset("textures/blocks/loot_crate/currency.png"), None),
    ]
    positions = [(57, 2), (76, 2), (95, 2), (114, 2), (133, 2),
                 (57, 24), (76, 24), (95, 24)]
    # В макете пять семейств наград; в реализации это отдельные страницы по семействам,
    # чтобы R не смешивал разные генераторы и их условия.
    for (icon, badge), (x, y) in zip(outs, [(57, 11), (76, 11), (95, 11), (114, 11), (133, 11)]):
        t.slot(x, y, icon, badge=badge, cycle=True)
    t.lines([
        "Отдельная страница на семейство: души, ингредиенты, сундуки камней/рун/валюты.",
        "Шанс берётся из серверного конфига и множителей моба, сложности, измерения и расположения.",
        "Сундуки мира используют тот же генератор с множителем 1.5 и максимумом 7 предметов.",
    ], y=62)
    return t.save("15-world-sources.png")


def main():
    paths = [t01_profession(), t02_runeword(), t03_salvage(), t04_currency(),
             t05_soul(), t06_identify(), t07_gems(), t08_rarity(), t09_repair(),
             t10_backpack(), t11_crate(), t12_unique(), t13_info(), t14_actions(),
             t15_sources()]
    imgs = [Image.open(p) for p in paths]
    col_w = max(i.width for i in imgs) + 24
    rows = (len(imgs) + 1) // 2
    heights = [max(imgs[i].height, imgs[i+1].height if i+1 < len(imgs) else 0) + 24
               for i in range(0, len(imgs), 2)]
    sheet = Image.new("RGB", (col_w * 2 + 24, sum(heights) + 24), (24, 24, 28))
    y = 12
    for r in range(rows):
        x = 12
        for c in range(2):
            idx = r * 2 + c
            if idx < len(imgs):
                sheet.paste(imgs[idx], (x, y))
            x += col_w
        y += heights[r]
    sheet.save(os.path.join(OUT, "00-all-tiles.png"))
    print("->", os.path.join(OUT, "00-all-tiles.png"), sheet.size)


if __name__ == "__main__":
    main()
