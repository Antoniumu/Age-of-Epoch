#!/usr/bin/env python3
"""Aggregate the per-suite JSON reports produced by ``run_audit.py``.

This is the last job of the audit workflow. It deliberately does NOT trust the
matrix job result alone: it verifies that every discovered suite actually
uploaded a report, that each report is well-formed and green, and prints one
table for the GitHub job summary. A matrix worker that died before uploading
is therefore detected instead of silently disappearing from the results.

    python3 tools/audit/summarize_reports.py audit-reports \
        --expect-suites '["assets","json",...]'
"""

from __future__ import annotations

import argparse
import json
import os
import sys
from pathlib import Path


def load_reports(directory: Path, expect: list[str] | None):
    """(name -> report-or-None) for every expected suite, in a stable order."""
    found = {}
    for path in sorted(directory.rglob("*.json")):
        if path.name == "summary.json":
            continue
        try:
            value = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, ValueError) as exc:
            value = {"suite": path.stem, "ok": False,
                     "error": f"unreadable report: {exc}"}
        name = value.get("suite") if isinstance(value, dict) else None
        if not isinstance(name, str):
            name = path.stem
        # Prefer the report closest to the root when artifacts were merged.
        found.setdefault(name, value)
    names = expect if expect is not None else sorted(found)
    return [(name, found.get(name)) for name in names]


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("directory", type=Path,
                        help="directory containing the downloaded reports")
    parser.add_argument("--expect-suites",
                        help="JSON array of suite names that must be present")
    args = parser.parse_args()

    expect = None
    if args.expect_suites:
        expect = json.loads(args.expect_suites)
        if not isinstance(expect, list) or not all(isinstance(x, str)
                                                   for x in expect):
            print("--expect-suites must be a JSON array of strings",
                  file=sys.stderr)
            return 2

    rows = load_reports(args.directory, expect)
    lines = [
        "## Resource audit",
        "",
        "| Suite | Tests | Failures | Errors | Skipped | Time | Result |",
        "|---|---:|---:|---:|---:|---:|---|",
    ]
    problems = []
    for name, report in rows:
        if report is None:
            lines.append(f"| `{name}` | — | — | — | — | — | ❌ no report |")
            problems.append(f"{name}: no report was uploaded")
            continue
        ok = bool(report.get("ok"))
        lines.append(
            f"| `{name}` | {report.get('tests', '?')} | "
            f"{report.get('failures', '?')} | {report.get('errors', '?')} | "
            f"{report.get('skipped', '?')} | {report.get('time', '?')}s | "
            f"{'✅ OK' if ok else '❌ FAILED'} |")
        if not ok:
            problems.append(f"{name}: {report.get('error') or 'suite failed'} "
                            f"({report.get('failures')} failures, "
                            f"{report.get('errors')} errors)")

    total_tests = sum(r.get("tests", 0) or 0 for _n, r in rows if r)
    lines += ["", f"**{len(rows)} suites, {total_tests} tests.**", ""]
    text = "\n".join(lines)
    print(text)

    summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary_path:
        with open(summary_path, "a", encoding="utf-8") as handle:
            handle.write(text + "\n")

    if problems:
        print("\nAudit aggregation failed:", file=sys.stderr)
        for problem in problems:
            print(f"  - {problem}", file=sys.stderr)
        return 1
    print("All audit suites reported green.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
