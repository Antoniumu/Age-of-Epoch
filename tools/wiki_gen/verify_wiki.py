# -*- coding: utf-8 -*-
"""Validate the generated in-game Wiki catalog and its navigation graph."""
import re
import sys
from collections import defaultdict, deque
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "src/main/resources"
WIKI = RES / "assets/mmorpg/wiki_pages"

# Do not enforce a prose-length target: concise, accurate records are valid.

LINK_RE = re.compile(r"\[\[([^\]|#]+)(?:#([^\]|]+))?(?:\|([^\]]+))?\]\]")
MD_LINK_RE = re.compile(r"\[([^\]]+)\]\(([^)]+)\)")
MALFORMED_TEXT_RE = re.compile(r"(?:\b\d+\.\-|\.\s*%)")
OBSOLETE_BOILERPLATE = (
    "Страница сгенерирована из данных самого мода",
    "в девяти случаях из десяти",
    "текст выше не заменяет эту подсказку",
)


def frontmatter(text):
    if not text.startswith("---"):
        return {}, text
    end = text.find("\n---", 3)
    if end == -1:
        return {}, text
    fields = {}
    for line in text[3:end].splitlines():
        if ":" in line:
            key, value = line.split(":", 1)
            fields[key.strip()] = value.strip()
    return fields, text[end + 4:]


def visible_text(body):
    text = LINK_RE.sub(lambda match: match.group(3) or match.group(1), body)
    text = MD_LINK_RE.sub(lambda match: match.group(1), text)
    text = re.sub(r"^#{1,6}\s*", "", text, flags=re.M)
    text = re.sub(r"^>\s?", "", text, flags=re.M)
    text = re.sub(r"^\s*[-*]\s+", "", text, flags=re.M)
    text = re.sub(r"^\s*\d+\.\s+", "", text, flags=re.M)
    text = text.replace("**", "").replace("`", "")
    text = re.sub(r"(?<!\w)\*(?!\s)([^*]+)\*(?!\w)", r"\1", text)
    text = text.replace("|", " ")
    text = re.sub(r"---+", " ", text)
    return re.sub(r"\s+", " ", text).strip()


def heading_slugs(body):
    slugs = set()
    for line in body.splitlines():
        match = re.match(r"^#{1,6}\s+(.*)$", line)
        if match:
            slugs.add(re.sub(r"\s+", "-", visible_text(match.group(1)).lower()).strip("-"))
    return slugs


def link_targets(body):
    targets = [
        (match.group(1).strip(), (match.group(2) or "").strip())
        for match in LINK_RE.finditer(body)
    ]
    for match in MD_LINK_RE.finditer(body):
        target = match.group(2).strip()
        if "://" in target or target.startswith("mailto:"):
            continue
        page_id, separator, anchor = target.partition("#")
        targets.append((page_id.strip(), anchor.strip() if separator else ""))
    return targets


def expected_subject_pages():
    expected = {}
    namespaces = ("mmorpg", "divine_missions", "infinite_dungeons")
    for namespace in namespaces:
        directory = RES / f"assets/{namespace}/items"
        if not directory.exists():
            continue
        for item_file in directory.rglob("*.json"):
            path = item_file.relative_to(directory).with_suffix("").as_posix()
            slug = path.replace("/", "_") if namespace == "mmorpg" else f"{namespace}_{path.replace('/', '_')}"
            expected[f"items/{slug}"] = f"{namespace}:{path}"

    item_ids = set(expected.values())
    for namespace in namespaces:
        directory = RES / f"assets/{namespace}/blockstates"
        if not directory.exists():
            continue
        for blockstate in directory.rglob("*.json"):
            path = blockstate.relative_to(directory).with_suffix("").as_posix()
            item_id = f"{namespace}:{path}"
            item_ids.add(item_id)
            if item_id not in expected.values():
                slug = path.replace("/", "_") if namespace == "mmorpg" else f"{namespace}_{path.replace('/', '_')}"
                expected.setdefault(f"blocks/{slug}", item_id)
    return expected, item_ids


def main():
    errors, warnings = [], []
    pages = {}
    if WIKI.exists():
        for page_file in sorted(WIKI.rglob("*.md")):
            page_id = page_file.relative_to(WIKI).with_suffix("").as_posix()
            pages[page_id] = page_file.read_text(encoding="utf-8")
    if not pages:
        print("FAIL: нет страниц в", WIKI)
        return 1

    expected, item_ids = expected_subject_pages()
    for page_id, item_id in expected.items():
        if page_id not in pages:
            errors.append(f"нет страницы предмета {item_id} -> {page_id}")

    metadata = {}
    bodies = {}
    anchors = {}
    stats = []
    for page_id, text in pages.items():
        fields, body = frontmatter(text)
        metadata[page_id] = fields
        bodies[page_id] = body
        anchors[page_id] = heading_slugs(body)

        if not fields.get("title"):
            errors.append(f"{page_id}: нет title в frontmatter")
        if not fields.get("category"):
            errors.append(f"{page_id}: нет category в frontmatter")
        if fields.get("item") and fields["item"] not in item_ids:
            errors.append(f"{page_id}: item={fields['item']} не существует в моде")

        visible = visible_text(body)
        stats.append((len(visible), page_id))
        if not visible:
            errors.append(f"{page_id}: нет видимого текста")

        for phrase in OBSOLETE_BOILERPLATE:
            if phrase.casefold() in body.casefold():
                errors.append(f"{page_id}: остался шаблонный текст «{phrase}»")
        for line_number, line in enumerate(body.splitlines(), 1):
            if MALFORMED_TEXT_RE.search(line):
                errors.append(f"{page_id}:{line_number}: подозрительная опечатка: {line.strip()[:120]}")

        paragraphs = set()
        for raw_paragraph in re.split(r"\n\s*\n", body):
            paragraph = visible_text(raw_paragraph).casefold()
            if len(paragraph) >= 120:
                if paragraph in paragraphs:
                    errors.append(f"{page_id}: повторяется абзац длиной {len(paragraph)} символов")
                paragraphs.add(paragraph)

        if page_id.startswith(("items/", "blocks/")):
            required = ("описание", "получение", "связанные-страницы", "справка")
            headings = {anchor for anchor in anchors[page_id]}
            for heading in required:
                if heading not in headings:
                    errors.append(f"{page_id}: отсутствует раздел «{heading}»")

    for page_id, item_id in expected.items():
        if page_id in pages and page_id.startswith("items/"):
            actual_id = metadata[page_id].get("item")
            if actual_id != item_id:
                errors.append(f"{page_id}: item={actual_id!r}, ожидался {item_id!r}")

    # Resolve all links only after collecting anchors for every page.
    graph = defaultdict(set)
    for page_id, body in bodies.items():
        targets = link_targets(body)
        if not targets and page_id != "index":
            warnings.append(f"{page_id}: нет исходящих ссылок")
        for target, anchor in targets:
            if not target:
                target = page_id
            if target not in pages:
                errors.append(f"{page_id}: битая ссылка на [[{target}]]")
                continue
            graph[page_id].add(target)
            if anchor:
                slug = re.sub(r"\s+", "-", anchor.lower()).strip("-")
                if slug not in anchors[target]:
                    errors.append(f"{page_id}: нет якоря #{anchor} на [[{target}]]")

    # Every guide and catalogue entry must be reachable from the home page.
    reachable = set()
    pending = deque(["index"] if "index" in pages else [])
    while pending:
        page_id = pending.popleft()
        if page_id in reachable:
            continue
        reachable.add(page_id)
        pending.extend(graph[page_id] - reachable)
    for page_id in sorted(set(pages) - reachable):
        errors.append(f"{page_id}: страница недостижима с главной через ссылки")

    stats.sort()
    print(f"страниц: {len(pages)}; предметов и блоков ожидается: {len(expected)}")
    print("самая короткая страница:", stats[0][1], stats[0][0], "символов")
    print("медиана длины:", stats[len(stats) // 2][0], "символов")
    for warning in warnings:
        print("WARN:", warning)
    for error in errors[:100]:
        print("FAIL:", error)
    if len(errors) > 100:
        print(f"... и ещё {len(errors) - 100} ошибок")
    print("ИТОГ:", "ОШИБКИ" if errors else "ВСЁ ЧИСТО",
          f"({len(errors)} ошибок, {len(warnings)} предупреждений)")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
