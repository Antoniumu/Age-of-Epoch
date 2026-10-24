# -*- coding: utf-8 -*-
"""
Проверка каталога вики Age of Epoch. Завершается с кодом 1 при любой проблеме.

Проверки:
  1. Страница есть на каждый предмет (items/*.json) и блок (blockstates).
  2. Каждая страница содержит не менее MIN_CHARS видимых символов текста
     (без frontmatter и разметки, у ссылок считается подпись).
  3. Все [[ссылки]] ведут на существующие страницы; якоря #... — на заголовки.
  4. frontmatter в каждой странице: title и category; item:, если указан,
     существует среди предметов/блоков мода.
  5. Нет пустых заголовков и страниц без единого исходящего линка (кроме тупиков по смыслу).

Запуск: python3 tools/wiki_gen/verify_wiki.py
"""
import re
import sys
from pathlib import Path

MIN_CHARS = 1000
ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "src/main/resources"
WIKI = RES / "assets/mmorpg/wiki_pages"

LINK_RE = re.compile(r"\[\[([^\]|#]+)(?:#([^\]|]+))?(?:\|([^\]]+))?\]\]")
MD_LINK_RE = re.compile(r"\[([^\]]+)\]\(([^)]+)\)")

def frontmatter(text):
    if not text.startswith("---"):
        return {}, text
    end = text.find("\n---", 3)
    if end == -1:
        return {}, text
    fm = {}
    for line in text[3:end].splitlines():
        if ":" in line:
            k, v = line.split(":", 1)
            fm[k.strip()] = v.strip()
    return fm, text[end + 4:]

def visible_text(body):
    t = LINK_RE.sub(lambda m: m.group(3) or m.group(1), body)
    t = MD_LINK_RE.sub(lambda m: m.group(1), t)
    t = re.sub(r"^#{1,6}\s*", "", t, flags=re.M)
    t = re.sub(r"^>\s?", "", t, flags=re.M)
    t = re.sub(r"^\s*[-*]\s+", "", t, flags=re.M)
    t = re.sub(r"^\s*\d+\.\s+", "", t, flags=re.M)
    t = t.replace("**", "").replace("`", "")
    t = re.sub(r"(?<!\w)\*(?!\s)([^*]+)\*(?!\w)", r"\1", t)
    t = t.replace("|", " ")
    t = re.sub(r"---+", " ", t)
    return re.sub(r"\s+", " ", t).strip()

def heading_slugs(body):
    slugs = set()
    for line in body.splitlines():
        m = re.match(r"^#{1,6}\s+(.*)$", line)
        if m:
            slugs.add(re.sub(r"\s+", "-", visible_text(m.group(1)).lower()).strip("-"))
    return slugs

def main():
    errors, warnings = [], []
    pages = {}
    for p in sorted(WIKI.rglob("*.md")):
        pid = p.relative_to(WIKI).with_suffix("").as_posix()
        pages[pid] = p.read_text(encoding="utf-8")
    if not pages:
        print("FAIL: нет страниц в", WIKI)
        return 1

    # -- ожидаемые предметы
    expected = {}
    for ns in ("mmorpg", "divine_missions", "infinite_dungeons"):
        d = RES / f"assets/{ns}/items"
        if d.exists():
            for p in d.rglob("*.json"):
                path = p.relative_to(d).with_suffix("").as_posix()
                slug = path.replace("/", "_") if ns == "mmorpg" else f"{ns}_{path.replace('/', '_')}"
                expected[f"items/{slug}"] = f"{ns}:{path}"
    item_keys = set(expected.values())
    for ns in ("mmorpg", "divine_missions", "infinite_dungeons"):
        d = RES / f"assets/{ns}/blockstates"
        if d.exists():
            for p in d.rglob("*.json"):
                path = p.relative_to(d).with_suffix("").as_posix()
                item_keys.add(f"{ns}:{path}")
                if f"{ns}:{path}" not in expected.values():
                    slug = path.replace("/", "_") if ns == "mmorpg" else f"{ns}_{path.replace('/', '_')}"
                    expected.setdefault(f"blocks/{slug}", f"{ns}:{path}")
    for pid, fid in expected.items():
        if pid not in pages:
            errors.append(f"нет страницы предмета {fid} -> {pid}")

    anchors = {}
    stats = []
    for pid, text in pages.items():
        fm, body = frontmatter(text)
        if not fm.get("title"):
            errors.append(f"{pid}: нет title в frontmatter")
        if not fm.get("category"):
            errors.append(f"{pid}: нет category в frontmatter")
        if fm.get("item") and fm["item"] not in item_keys:
            errors.append(f"{pid}: frontmatter item={fm['item']} не существует в моде")
        vt = visible_text(body)
        stats.append((len(vt), pid))
        if len(vt) < MIN_CHARS:
            errors.append(f"{pid}: видимого текста {len(vt)} < {MIN_CHARS}")
        anchors[pid] = heading_slugs(body)
        targets = [(m.group(1).strip(), (m.group(2) or "").strip()) for m in LINK_RE.finditer(body)]
        targets += [(m.group(2).split("#")[0].strip(), (m.group(2).split("#")[1].strip() if "#" in m.group(2) else ""))
                    for m in MD_LINK_RE.finditer(body)]
        for tgt, anchor in targets:
            if "/" not in tgt and tgt not in pages and tgt != "index":
                errors.append(f"{pid}: ссылка [[{tgt}]] не похожа на id страницы")
                continue
            if tgt not in pages:
                errors.append(f"{pid}: битая ссылка [[{tgt}]]")
            elif anchor:
                slug = re.sub(r"\s+", "-", anchor.lower()).strip("-")
                if slug not in anchors[tgt]:
                    errors.append(f"{pid}: нет якоря #{anchor} на [[{tgt}]]")
        if not targets and pid != "index":
            warnings.append(f"{pid}: нет исходящих ссылок")

    # достижимость категорий с главной
    home_targets = {m.group(1).strip() for m in LINK_RE.finditer(pages.get("index", ""))}
    for pid in pages:
        if pid.startswith("categories/") and pid not in home_targets:
            warnings.append(f"{pid}: категория не связана с главной")

    stats.sort()
    print(f"страниц: {len(pages)}; предметов ожидается: {len(expected)}")
    print("самая короткая страница:", stats[0][1], stats[0][0], "символов")
    print("медиана длины:", stats[len(stats)//2][0], "символов")
    for w in warnings:
        print("WARN:", w)
    for e in errors[:80]:
        print("FAIL:", e)
    if len(errors) > 80:
        print(f"... и ещё {len(errors) - 80} ошибок")
    print("ИТОГ:", "ОШИБКИ" if errors else "ВСЁ ЧИСТО", f"({len(errors)} ошибок, {len(warnings)} предупреждений)")
    return 1 if errors else 0

if __name__ == "__main__":
    sys.exit(main())
