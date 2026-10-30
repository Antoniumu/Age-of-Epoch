#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Нарезает запись аудита профессий на ролики по профессиям.

Откуда берутся границы: ProfessionAuditProbe пишет маркеры
`AOEPR_MARK prof_start alchemy <epoch_ms>` / `prof_end` в
`run/profession-audit-timeline.txt`, а `tools/e2e/spell-video.sh` — момент начала
записи в `run/e2e/video-start.txt`. Разница эпох даёт секунды внутри видео —
поэтому запись одна, а на выходе отдельный ролик на каждую профессию.

Дополнительно к каждому ролику делается постер (кадр) и README со списком
профессий и их итогом: так видео можно смотреть и разбирать без запуска.

Запуск:
  python3 tools/e2e/split_profession_videos.py \\
      --video run/e2e/profession-video.mp4 \\
      --timeline run/profession-audit-timeline.txt \\
      --start run/e2e/video-start.txt \\
      --out run/e2e/prof-videos \\
      --report run/profession-audit.json
"""

import argparse
import json
import os
import shutil
import subprocess
import sys

TITLES = {
    "alchemy": ("Alchemy", "зелья: ингредиенты → расходник с эффектом"),
    "inscribing": ("Inscribing", "свитки: ингредиенты → групповой бафф"),
    "cooking": ("Cooking", "еда: ингредиенты → длительный бафф"),
    "armor_craft": ("Armor Crafting", "броня: основа + ингредиенты → зачарованная основа"),
    "wep_craft": ("Weapon Crafting", "оружие: основа + ингредиенты → зачарованная основа"),
    "jewel_craft": ("Jewel Crafting", "украшения: кольцо/ожерелье + ингредиенты → зачарованная основа"),
    "custom": ("Выбранные", "точечный прогон"),
}


def read_text(path):
    try:
        with open(path, encoding="utf-8") as handle:
            return handle.read()
    except OSError:
        return ""


def parse_timeline(path):
    """[{'kind':..,'name':..,'ms':int}] в порядке записи."""
    events = []
    for line in read_text(path).splitlines():
        parts = line.strip().split()
        if len(parts) < 4 or parts[0] != "AOEPR_MARK":
            continue
        try:
            ms = int(parts[-1])
        except ValueError:
            continue
        events.append({"kind": parts[1], "name": " ".join(parts[2:-1]), "ms": ms})
    return events


def prof_windows(events):
    """{prof: (start_ms, end_ms)} по маркерам prof_start/prof_end."""
    windows = {}
    open_prof = None
    for event in events:
        if event["kind"] == "prof_start":
            open_prof = (event["name"], event["ms"])
        elif event["kind"] == "prof_end" and open_prof:
            name, start = open_prof
            windows[name] = (start, event["ms"])
            open_prof = None
    if open_prof:
        # Прогон оборвался: закрываем окно последним известным временем.
        name, start = open_prof
        last = events[-1]["ms"] if events else start
        windows[name] = (start, max(last, start + 1000))
    return windows


def ffmpeg(args, timeout=900):
    result = subprocess.run(["ffmpeg", "-hide_banner", "-loglevel", "error"] + args,
                            capture_output=True, text=True, timeout=timeout)
    if result.returncode != 0:
        print("ffmpeg: " + (result.stderr or "").strip()[-800:], file=sys.stderr)
    return result.returncode == 0


def cut(video, start_s, end_s, out_path):
    """Точная нарезка: перекодирование (с -c copy резы уезжают на keyframe)."""
    if end_s <= start_s:
        end_s = start_s + 1.0
    return ffmpeg([
        "-y", "-ss", "%.3f" % start_s, "-to", "%.3f" % end_s, "-i", video,
        "-c:v", "libx264", "-preset", "veryfast", "-crf", "28",
        "-pix_fmt", "yuv420p", "-movflags", "+faststart", out_path,
    ])


def poster(video, at_s, out_path):
    return ffmpeg(["-y", "-ss", "%.3f" % max(0.0, at_s), "-i", video,
                   "-frames:v", "1", "-vf", "scale=640:-2", out_path], timeout=180)


def load_report(path):
    try:
        with open(path, encoding="utf-8") as handle:
            return json.load(handle)
    except (OSError, ValueError):
        return {"professions": []}


def write_readme(outdir, produced, by_prof, report):
    lines = ["# Видео: проверка всех профессий ремесла", ""]
    total = len(report.get("professions", []))
    failed = [p for p in report.get("professions", []) if p.get("verdict") != "PASS"]
    lines.append("Проверено профессий: **%d**, провалилось: **%d**." % (total, len(failed)))
    lines.append("")
    lines.append("Каждый ролик — одна профессия. В кадре видно, как предметы "
                 "мышкой кладутся в сетку крафта 3×2 (каждое перемещение — "
                 "отдельный скриншот в ветке), сколько статов дал крафт и чем "
                 "подтверждён результат.")
    lines.append("")
    lines.append("| профессия | ролик | итог |")
    lines.append("|---|---|---|")
    verdicts = {p.get("id", ""): p.get("verdict", "?") for p in report.get("professions", [])}
    for prof, (title, _about) in produced.items():
        lines.append("| %s | [%s.mp4](%s.mp4) | %s |" % (
            title, prof, prof, verdicts.get(prof, "?")))
    lines.append("")
    for prof, (title, about) in produced.items():
        lines.append("## %s — %s" % (title, about))
        lines.append("")
        if os.path.exists(os.path.join(outdir, "%s.mp4" % prof)):
            lines.append("[%s.mp4](%s.mp4) · [постер](%s.png)" % (prof, prof, prof))
            lines.append("")
            lines.append("[![%s](%s.png)](%s.mp4)" % (prof, prof, prof))
        else:
            lines.append("_(ролик не получился)_")
        lines.append("")
        rows = [p for p in report.get("professions", []) if p.get("id") == prof]
        for p in rows:
            lines.append("| шаг | итог |")
            lines.append("|---|---|")
            lines.append("| сложено в сетку | %s |" % p.get("placed", 0))
            lines.append("| результат появился | %s |" % ("да" if p.get("result") else "нет"))
            lines.append("| результат забран | %s |" % ("да" if p.get("taken") else "нет"))
            lines.append("| предмет создан | %s (%s) |" % (p.get("item", "—"), p.get("stats", 0)))
            lines.append("| опыт профессии | %s |" % ("да" if p.get("exp") else "нет"))
            lines.append("| итог | **%s** |" % p.get("verdict", "?"))
            problems = p.get("problems") or []
            if problems:
                lines.append("")
                lines.append("Проблемы: " + "; ".join(problems))
            lines.append("")
    with open(os.path.join(outdir, "README.md"), "w", encoding="utf-8") as handle:
        handle.write("\n".join(lines) + "\n")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--video", default="run/e2e/profession-video.mp4")
    parser.add_argument("--timeline", default="run/profession-audit-timeline.txt")
    parser.add_argument("--start", default="run/e2e/video-start.txt")
    parser.add_argument("--out", default="run/e2e/prof-videos")
    parser.add_argument("--report", default="run/profession-audit.json")
    args = parser.parse_args()

    if not os.path.exists(args.video) or os.path.getsize(args.video) < 10_000:
        print("видео нет или оно пустое: %s" % args.video, file=sys.stderr)
        return 1

    os.makedirs(args.out, exist_ok=True)
    video_start = read_text(args.start).strip()
    try:
        video_start_ms = int(video_start)
    except ValueError:
        print("нет метки начала записи (%s), режу весь файл" % args.start, file=sys.stderr)
        video_start_ms = None

    events = parse_timeline(args.timeline)
    if not events or video_start_ms is None:
        # Без маркеров делаем один общий ролик: он всё равно полезен.
        out_path = os.path.join(args.out, "audit-all.mp4")
        shutil.copyfile(args.video, out_path)
        poster(args.video, 1.0, os.path.join(args.out, "audit-all.png"))
        write_readme(args.out, {"audit-all": ("Весь прогон", "все профессии подряд")},
                     {}, load_report(args.report))
        print("маркеров нет — опубликован общий ролик")
        return 0

    windows = prof_windows(events)
    report = load_report(args.report)
    by_prof = {}
    for prof in report.get("professions", []):
        by_prof.setdefault(prof.get("id", ""), []).append(prof)

    produced = {}
    for prof, (start_ms, end_ms) in windows.items():
        start_s = max(0.0, (start_ms - video_start_ms) / 1000.0 - 1.0)
        end_s = max(start_s + 1.0, (end_ms - video_start_ms) / 1000.0 + 1.0)
        out_path = os.path.join(args.out, "%s.mp4" % prof)
        print("%-12s %7.1f .. %7.1f c" % (prof, start_s, end_s))
        if cut(args.video, start_s, end_s, out_path):
            produced[prof] = TITLES.get(prof, (prof, ""))
            poster(args.video, start_s + 2.0, os.path.join(args.out, "%s.png" % prof))
        else:
            print("не удалось нарезать %s" % prof, file=sys.stderr)

    if not produced:
        return 1
    write_readme(args.out, produced, by_prof, report)
    return 0


if __name__ == "__main__":
    sys.exit(main())
