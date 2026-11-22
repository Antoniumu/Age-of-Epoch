#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Нарезает запись аудита умений на видео по классам (школам).

Откуда берутся границы: SpellAuditProbe пишет маркеры
`AOEAI_MARK school_start fire <epoch_ms>` / `school_end` в
`run/spell-audit-timeline.txt`, а `tools/e2e/spell-video.sh` — момент начала
записи в `run/e2e/video-start.txt`. Разница эпох даёт секунды внутри видео —
поэтому запись одна, а на выходе отдельный ролик на каждый класс.

Дополнительно к каждому ролику делается постер (первый кадр) и README со
списком умений и их итогом: так видео можно смотреть и разбирать без запуска.

Запуск:
  python3 tools/e2e/split_videos.py \
      --video run/e2e/spell-video.mp4 \
      --timeline run/spell-audit-timeline.txt \
      --start run/e2e/video-start.txt \
      --out run/e2e/videos \
      --report run/spell-audit.json
"""

import argparse
import json
import os
import shutil
import subprocess
import sys

TITLES = {
    "divine": ("Divine", "свет, щиты и ближний бой"),
    "hunting": ("Hunting", "луки, ловушки и мобильность"),
    "nature": ("Nature", "яды, тотемы и поддержка"),
    "fire": ("Fire", "огонь и урон по площади"),
    "water": ("Water", "лёд, контроль и лечение"),
    "no_school": ("Вне школ", "заклинания, которые не выдают ни в одной школе"),
    "custom": ("Выбранные", "точечный прогон"),
}


def read_text(path):
    try:
        with open(path, encoding="utf-8") as handle:
            return handle.read()
    except OSError:
        return ""


def parse_timeline(path, marker="AOEAI_MARK"):
    """[{'kind':..,'name':..,'ms':int}] в порядке записи."""
    events = []
    for line in read_text(path).splitlines():
        parts = line.strip().split()
        if len(parts) < 4 or parts[0] != marker:
            continue
        try:
            ms = int(parts[-1])
        except ValueError:
            continue
        events.append({"kind": parts[1], "name": " ".join(parts[2:-1]), "ms": ms})
    return events


def school_windows(events, start_kind="school_start", end_kind="school_end"):
    """{school: (start_ms, end_ms)} по маркерам начала/конца группы."""
    windows = {}
    open_school = None
    for event in events:
        if event["kind"] == start_kind:
            open_school = (event["name"], event["ms"])
        elif event["kind"] == end_kind and open_school:
            name, start = open_school
            windows[name] = (start, event["ms"])
            open_school = None
    if open_school:
        # Прогон оборвался: закрываем окно последним известным временем.
        name, start = open_school
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
        return {"spells": []}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--video", default="run/e2e/spell-video.mp4")
    parser.add_argument("--timeline", default="run/spell-audit-timeline.txt")
    parser.add_argument("--start", default="run/e2e/video-start.txt")
    parser.add_argument("--out", default="run/e2e/videos")
    parser.add_argument("--report", default="run/spell-audit.json")
    # Каким маркером размечен таймлайн и как называются границы групп:
    # аудит умений — AOEAI_MARK school_start/school_end, прокачка —
    # AOEPROG_MARK stage_start/stage_end.
    parser.add_argument("--marker", default="AOEAI_MARK")
    parser.add_argument("--group-start", default="school_start")
    parser.add_argument("--group-end", default="school_end")
    # Заголовок README: "умений"/"этапов" и базовый путь отчёта.
    parser.add_argument("--heading", default="Видео: применение всех умений по классам")
    parser.add_argument("--noun", default="умений")
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

    events = parse_timeline(args.timeline, marker=args.marker)
    if not events or video_start_ms is None:
        # Без маркеров делаем один общий ролик: он всё равно полезен.
        out_path = os.path.join(args.out, "audit-all.mp4")
        shutil.copyfile(args.video, out_path)
        poster(args.video, 1.0, os.path.join(args.out, "audit-all.png"))
        write_readme(args.out, {"audit-all": ("Весь прогон", "все группы подряд")},
                     {}, load_report(args.report), args.heading, args.noun)
        print("маркеров нет — опубликован общий ролик")
        return 0

    windows = school_windows(events, start_kind=args.group_start, end_kind=args.group_end)
    report = load_report(args.report)
    by_school = {}
    for spell in report.get("spells", []):
        by_school.setdefault(spell.get("school", ""), []).append(spell)

    produced = {}
    for school, (start_ms, end_ms) in windows.items():
        start_s = max(0.0, (start_ms - video_start_ms) / 1000.0 - 1.0)
        end_s = max(start_s + 1.0, (end_ms - video_start_ms) / 1000.0 + 1.0)
        out_path = os.path.join(args.out, "%s.mp4" % school)
        print("%-10s %7.1f .. %7.1f c" % (school, start_s, end_s))
        if cut(args.video, start_s, end_s, out_path):
            produced[school] = TITLES.get(school, (school, ""))
            poster(args.video, start_s + 2.0, os.path.join(args.out, "%s.png" % school))
        else:
            print("не удалось нарезать %s" % school, file=sys.stderr)

    if not produced:
        return 1
    write_readme(args.out, produced, by_school, report, args.heading, args.noun)
    return 0


def write_readme(outdir, produced, by_school, report, heading="Видео: применение всех умений по классам", noun="умений"):
    lines = ["# %s" % heading, ""]
    total = len(report.get("spells", []))
    failed = [s for s in report.get("spells", []) if s.get("verdict") != "PASS"]
    if total:
        lines.append("Проверено %s: **%d**, провалилось: **%d**." % (noun, total, len(failed)))
    else:
        lines.append("Ролики нарезаны по маркерам таймлайна прогона.")
    lines.append("")
    lines.append("Каждый ролик — один класс. Подпись в кадре показывает название "
                 "умения, его описание из тултипа и живые цифры: урон, попадания, "
                 "лечение, эффекты и итог проверки.")
    lines.append("")
    lines.append("| группа | ролик | %s |" % noun)
    lines.append("|---|---|---|")
    for school, (title, _about) in produced.items():
        lines.append("| %s | [%s.mp4](%s.mp4) | %d |" % (
            title, school, school, len(by_school.get(school, []))))
    lines.append("")
    for school, (title, about) in produced.items():
        lines.append("## %s — %s" % (title, about))
        lines.append("")
        if os.path.exists(os.path.join(outdir, "%s.mp4" % school)):
            lines.append("[%s.mp4](%s.mp4) · [постер](%s.png)" % (school, school, school))
            lines.append("")
            lines.append("[![%s](%s.png)](%s.mp4)" % (school, school, school))
        else:
            lines.append("_(ролик не получился)_")
        lines.append("")
        spells = by_school.get(school, [])
        if spells:
            lines.append("| умение | итог | урон | попаданий | чем подтверждено |")
            lines.append("|---|---|---|---|---|")
            for spell in spells:
                verdict = spell.get("verdict", "?")
                mark = "OK" if verdict == "PASS" else "**" + verdict + "**"
                effects = ", ".join(spell.get("effects") or [])
                proof = spell.get("evidence") or effects or "—"
                lines.append("| `%s` | %s | %s | %s | %s |" % (
                    spell.get("id", "?"), mark, spell.get("damage", 0),
                    spell.get("hits", 0), proof))
            lines.append("")
    with open(os.path.join(outdir, "README.md"), "w", encoding="utf-8") as handle:
        handle.write("\n".join(lines) + "\n")


if __name__ == "__main__":
    sys.exit(main())
