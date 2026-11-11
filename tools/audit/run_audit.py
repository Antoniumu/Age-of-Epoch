#!/usr/bin/env python3
"""Run the resource audit suite, one shard at a time.

Why this exists
---------------
The audit is deliberately a plain-stdlib ``unittest`` suite, but "one big
command" is a poor CI citizen: a single failure hides every other result and
the whole thing has to be rerun to see anything else. This runner slices the
audit into one *suite* per test module and emits a JSON + JUnit XML report for
each slice. ``.github/workflows/audit.yml`` then delegates every suite to its
own matrix job and aggregates the reports.

Usage
-----
    python3 tools/audit/run_audit.py --list            # suite names
    python3 tools/audit/run_audit.py --list-json       # JSON array for a matrix
    python3 tools/audit/run_audit.py --suite assets    # one shard (CI worker)
    python3 tools/audit/run_audit.py --all --report-dir audit-reports

Exit code is 0 only when every selected suite is green. Reports are always
written, including on failure, so CI can upload them with ``if: always()``.
"""

from __future__ import annotations

import argparse
import io
import json
import sys
import time
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path

AUDIT_DIR = Path(__file__).resolve().parent
if str(AUDIT_DIR) not in sys.path:
    sys.path.insert(0, str(AUDIT_DIR))


def suites():
    """One suite per ``test_*.py`` module, keyed by a short stable name."""
    return {path.stem[len("test_"):]: path.stem
            for path in sorted(AUDIT_DIR.glob("test_*.py"))}


class RecordingResult(unittest.TextTestResult):
    """TextTestResult that also remembers each test's outcome and duration."""

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.records = []
        self._started = {}

    def startTest(self, test):
        super().startTest(test)
        self._started[test.id()] = time.perf_counter()

    def _add(self, test, status, err=None, reason=""):
        elapsed = time.perf_counter() - self._started.get(test.id(),
                                                          time.perf_counter())
        self.records.append({
            "id": test.id(),
            "classname": test.__class__.__name__,
            "name": test._testMethodName,
            "status": status,
            "time": round(elapsed, 4),
            "message": reason or (self._exc_info_to_string(err, test).strip()
                                  if err else ""),
        })

    def addSuccess(self, test):
        super().addSuccess(test)
        self._add(test, "ok")

    def addFailure(self, test, err):
        super().addFailure(test, err)
        self._add(test, "failure", err)

    def addError(self, test, err):
        super().addError(test, err)
        self._add(test, "error", err)

    def addSkip(self, test, reason):
        super().addSkip(test, reason)
        self._add(test, "skipped", reason=reason)

    def addExpectedFailure(self, test, err):
        super().addExpectedFailure(test, err)
        self._add(test, "expected_failure", err)

    def addUnexpectedSuccess(self, test):
        super().addUnexpectedSuccess(test)
        self._add(test, "unexpected_success")


def run_suite(name: str, module: str, verbosity: int):
    loader = unittest.TestLoader()
    suite = loader.loadTestsFromName(module)
    if loader.errors:
        # Import/attribute errors surface here instead of silently producing
        # an empty (and therefore "green") run.
        stream = io.StringIO()
        runner = unittest.TextTestRunner(stream=stream, verbosity=verbosity,
                                         resultclass=RecordingResult)
        result = runner.run(unittest.TestSuite())
        result.records.append({
            "id": f"{module} (collection)", "classname": module,
            "name": "collection", "status": "error", "time": 0.0,
            "message": "\n".join(loader.errors),
        })
        return result, stream.getvalue(), ["\n".join(loader.errors)]
    stream = io.StringIO()
    runner = unittest.TextTestRunner(stream=stream, verbosity=verbosity,
                                     resultclass=RecordingResult)
    return runner.run(suite), stream.getvalue(), []


def junit_xml(name: str, result) -> bytes:
    """Minimal JUnit XML so any CI test-reporter can consume the results."""
    counts = {status: 0 for status in
              ("ok", "failure", "error", "skipped",
               "expected_failure", "unexpected_success")}
    for record in result.records:
        counts[record["status"]] = counts.get(record["status"], 0) + 1
    root = ET.Element("testsuites")
    node = ET.SubElement(root, "testsuite", {
        "name": name,
        "tests": str(len(result.records)),
        "failures": str(counts["failure"]),
        "errors": str(counts["error"]),
        "skipped": str(counts["skipped"]),
        "time": str(round(sum(r["time"] for r in result.records), 4)),
    })
    for record in result.records:
        case = ET.SubElement(node, "testcase", {
            "classname": record["classname"],
            "name": record["name"],
            "time": str(record["time"]),
        })
        status = record["status"]
        if status in ("failure", "error"):
            child = ET.SubElement(case, "failure" if status == "failure"
                                  else "error", {"message": status})
            child.text = record["message"]
        elif status == "skipped":
            ET.SubElement(case, "skipped", {"message": record["message"]})
    ET.indent(root, space="  ")
    return ET.tostring(root, encoding="utf-8", xml_declaration=True)


def summary(name: str, result, collection_errors) -> dict:
    failures = sum(r["status"] == "failure" for r in result.records)
    errors = sum(r["status"] == "error" for r in result.records)
    return {
        "suite": name,
        "tests": len(result.records),
        "failures": failures,
        "errors": errors,
        "skipped": sum(r["status"] == "skipped" for r in result.records),
        "time": round(sum(r["time"] for r in result.records), 4),
        "ok": not failures and not errors and not collection_errors
              and len(result.records) > 0,
    }


def emit(name: str, result, log: str, collection_errors, report_dir: Path | None):
    info = summary(name, result, collection_errors)
    if report_dir is not None:
        report_dir.mkdir(parents=True, exist_ok=True)
        (report_dir / f"{name}.json").write_text(
            json.dumps(info, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8")
        (report_dir / f"{name}.xml").write_bytes(junit_xml(name, result))
        (report_dir / f"{name}.txt").write_text(log, encoding="utf-8")
    return info


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    group = parser.add_mutually_exclusive_group()
    group.add_argument("--list", action="store_true", help="print suite names")
    group.add_argument("--list-json", action="store_true",
                       help="print a JSON array of suite names (CI matrix)")
    group.add_argument("--suite", help="run exactly one suite")
    group.add_argument("--all", action="store_true", help="run every suite")
    parser.add_argument("--report-dir", type=Path,
                        help="write <suite>.json/.xml/.txt reports here")
    parser.add_argument("-v", "--verbose", action="store_true")
    args = parser.parse_args()

    all_suites = suites()
    if not all_suites:
        print("No test_*.py modules found in tools/audit/", file=sys.stderr)
        return 2

    if args.list:
        for name in all_suites:
            print(name)
        return 0
    if args.list_json:
        print(json.dumps(list(all_suites)))
        return 0

    if args.suite:
        if args.suite not in all_suites:
            print(f"Unknown suite {args.suite!r}. Available: "
                  f"{', '.join(all_suites)}", file=sys.stderr)
            return 2
        selected = {args.suite: all_suites[args.suite]}
    else:
        selected = all_suites  # default and --all

    verbosity = 2 if args.verbose else 1
    failed = []
    for name, module in selected.items():
        print(f"=== audit suite: {name} ===", flush=True)
        result, log, collection_errors = run_suite(name, module, verbosity)
        info = emit(name, result, log, collection_errors, args.report_dir)
        sys.stdout.write(log)
        for err in collection_errors:
            print(err, file=sys.stderr)
        print(f"--- {name}: {info['tests']} tests, "
              f"{info['failures']} failures, {info['errors']} errors, "
              f"{'OK' if info['ok'] else 'FAILED'} ---\n", flush=True)
        if not info["ok"]:
            failed.append(name)

    if failed:
        print(f"FAILED suites: {', '.join(failed)}", file=sys.stderr)
        return 1
    print(f"All {len(selected)} audit suite(s) passed.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
