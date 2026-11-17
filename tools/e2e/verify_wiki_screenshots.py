#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Check that the wiki screenshot artifact contains one real image per page."""
import csv
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DEFAULT_SCREENSHOT_DIR = ROOT / "run/e2e/shots/wiki"
WIKI_ROOT = ROOT / "src/main/resources/assets/mmorpg/wiki_pages"


def main():
    screenshot_dir = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else DEFAULT_SCREENSHOT_DIR
    manifest = screenshot_dir / "wiki-pages.tsv"
    expected = {
        path.relative_to(WIKI_ROOT).with_suffix("").as_posix()
        for path in WIKI_ROOT.rglob("*.md")
    } if WIKI_ROOT.is_dir() else set()
    errors = []

    if not expected:
        errors.append(f"каталог вики пуст: {WIKI_ROOT}")
    if not manifest.is_file():
        errors.append(f"нет манифеста скриншотов: {manifest}")
        rows = []
    else:
        with manifest.open("r", encoding="utf-8", newline="") as handle:
            rows = list(csv.DictReader(handle, delimiter="\t"))

    seen_pages = set()
    seen_images = set()
    for row in rows:
        page_id = (row.get("page_id") or "").strip()
        image_name = (row.get("screenshot") or "").strip()
        status = (row.get("status") or "").strip()
        if not page_id:
            errors.append("манифест содержит пустой page_id")
            continue
        if page_id in seen_pages:
            errors.append(f"повторная запись страницы: {page_id}")
        seen_pages.add(page_id)
        if status != "OK":
            errors.append(f"страница {page_id}: статус {status or '(пусто)'}")
        if not image_name or Path(image_name).name != image_name or not image_name.endswith(".png"):
            errors.append(f"страница {page_id}: небезопасное имя скриншота {image_name!r}")
            continue
        if image_name in seen_images:
            errors.append(f"скриншот используется более одного раза: {image_name}")
        seen_images.add(image_name)
        image = screenshot_dir / image_name
        if not image.is_file():
            errors.append(f"страница {page_id}: файл отсутствует: {image_name}")
        elif image.stat().st_size <= 1000:
            errors.append(f"страница {page_id}: скриншот пустой или обрезан: {image_name}")

    for page_id in sorted(expected - seen_pages):
        errors.append(f"нет скриншота для страницы: {page_id}")
    for page_id in sorted(seen_pages - expected):
        errors.append(f"неизвестная страница в манифесте: {page_id}")

    print(f"страниц каталога: {len(expected)}; записей в манифесте: {len(rows)}")
    if errors:
        for error in errors[:100]:
            print("FAIL:", error)
        if len(errors) > 100:
            print(f"... и ещё {len(errors) - 100} ошибок")
        print(f"FAIL: {len(errors)} ошибок проверки скриншотов")
        return 1
    print(f"OK: для всех {len(expected)} страниц есть отдельный PNG в {screenshot_dir}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
