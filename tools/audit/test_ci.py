#!/usr/bin/env python3
"""CI/CD self-audit: the pipelines that run these tests are tests too.

A job forgotten in ``ci-ok.needs`` is a check that can fail forever without
blocking a merge; a matrix without ``fail-fast: false`` hides all but the first
bug; a workflow without ``timeout-minutes`` can burn the whole runner budget.
This module keeps the delegation wiring honest.
"""

import re
import unittest
from pathlib import Path

from audit_lib import ROOT, TOOLS, WORKFLOWS, AuditCase, rel

JOB_HEADER = re.compile(r"^  ([A-Za-z0-9_-]+):\s*(?:#.*)?$")
NEEDS_LIST = re.compile(r"^\s+needs:\s*\[([^\]]+)\]", re.M)
# Archival one-shot proof workflows are pinned to a historic run on purpose.
# New hardcoded run ids anywhere else are a regression.
KNOWN_PINNED_WORKFLOWS = {"publish-airship.yml", "airship.yml"}
RUN_ID = re.compile(r"gh run (?:view|download)\s+\d{6,}")
REUSABLE_CALL = re.compile(r"^\s+uses:\s*\./\.github/workflows/", re.M)


def workflow_files():
    return sorted(WORKFLOWS.glob("*.yml"))


def job_blocks(text):
    """name -> the YAML lines belonging to that job."""
    lines = text.splitlines()
    starts = [i for i, line in enumerate(lines) if line.strip() == "jobs:"]
    if not starts:
        return {}
    out, current = {}, None
    for line in lines[starts[0] + 1:]:
        match = JOB_HEADER.match(line)
        if match:
            current = match.group(1)
            out[current] = []
        elif current is not None:
            out[current].append(line)
    return {name: "\n".join(body) for name, body in out.items()}


class TestCI(AuditCase, unittest.TestCase):

    def test_every_workflow_has_permissions_and_concurrency(self):
        bad = []
        files = workflow_files()
        self.assertGreater(len(files), 5, "workflow corpus looks incomplete")
        for path in files:
            text = path.read_text(encoding="utf-8")
            if not re.search(r"^permissions:", text, re.M):
                bad.append(f"{rel(path)}: no explicit permissions block")
            if not re.search(r"^concurrency:", text, re.M):
                bad.append(f"{rel(path)}: no concurrency group")
            if "\t" in text:
                bad.append(f"{rel(path)}: tab character (invalid YAML)")
        self.assertNoFindings(bad, "Unsafe workflow configuration")

    def test_every_job_has_a_timeout(self):
        bad, count = [], 0
        for path in workflow_files():
            for name, block in job_blocks(
                    path.read_text(encoding="utf-8")).items():
                # A job that only calls a reusable workflow cannot carry a
                # timeout; the timeouts belong to the called workflow's jobs,
                # which are checked here themselves.
                if REUSABLE_CALL.search(block):
                    continue
                count += 1
                if not re.search(r"^\s+timeout-minutes:", block, re.M):
                    bad.append(f"{rel(path)}: job '{name}' has no timeout-minutes")
        self.assertGreater(count, 10, "job corpus looks incomplete")
        self.assertNoFindings(bad, "Jobs that can hang forever")

    def test_reusable_workflow_callers_use_only_supported_keys(self):
        """GitHub rejects unsupported keys on `uses:` caller jobs.

        The whole workflow fails to start (every job in it) if a caller job
        carries e.g. ``timeout-minutes`` or ``runs-on``. See the exclusive
        keyword list in the reusable-workflow docs.
        """
        bad, callers = [], 0
        for path in workflow_files():
            for name, block in job_blocks(
                    path.read_text(encoding="utf-8")).items():
                if not REUSABLE_CALL.search(block):
                    continue
                callers += 1
                for forbidden in ("timeout-minutes", "runs-on", "steps",
                                  "container", "services", "outputs", "env"):
                    if re.search(rf"^\s+{forbidden}:", block, re.M):
                        bad.append(f"{rel(path)}: reusable caller job "
                                   f"'{name}' has unsupported '{forbidden}'")
        self.assertGreater(callers, 0, "no reusable workflow callers found")
        self.assertNoFindings(bad, "Invalid reusable-workflow caller jobs")

    def test_ci_ok_covers_every_job(self):
        ci = (WORKFLOWS / "ci.yml").read_text(encoding="utf-8")
        jobs = job_blocks(ci)
        self.assertIn("ci-ok", jobs)
        others = set(jobs) - {"ci-ok"}
        match = NEEDS_LIST.search(jobs["ci-ok"])
        self.assertIsNotNone(match, "ci-ok has no explicit needs list")
        needed = {x.strip() for x in match.group(1).split(",")}
        bad = [f"ci-ok: job '{j}' is not in needs" for j in sorted(others - needed)]
        bad += [f"ci-ok: needs '{n}' is not a job" for n in sorted(needed - others)]
        for job in others:
            if f"needs.{job}.result" not in jobs["ci-ok"]:
                bad.append(f"ci-ok: result of '{job}' is never checked")
        self.assertNoFindings(bad, "CI gate is missing jobs")

    def test_pr_comment_is_fork_safe(self):
        ci = (WORKFLOWS / "ci.yml").read_text(encoding="utf-8")
        self.assertIn("head.repo.fork == false", ci,
                      "PR comment step has no fork guard: read-only tokens "
                      "from forks would make ci-ok red")

    def test_audit_workflow_delegates_every_suite(self):
        path = WORKFLOWS / "audit.yml"
        self.assertTrue(path.is_file(), "audit.yml is missing")
        text = path.read_text(encoding="utf-8")
        required = {
            "workflow_call:": "audit workflow must be callable from ci.yml",
            "fromJSON(needs.discover.outputs.suites)": "matrix is not built from discovery",
            "fail-fast: false": "a single red shard would hide the other bugs",
            "--list-json": "discovery does not use the runner",
            "--suite": "workers do not run a single shard",
            "--report-dir": "workers do not emit reports",
            "actions/upload-artifact": "reports are not uploaded",
            "actions/download-artifact": "reports are never downloaded",
            "merge-multiple": "shard reports are not merged",
            "summarize_reports.py": "no aggregation of shard results",
            "--expect-suites": "aggregation does not check for missing shards",
            "needs.audit.result": "matrix result is not verified",
        }
        bad = [f"audit.yml: {reason} ({token!r} missing)"
               for token, reason in required.items() if token not in text]
        self.assertNoFindings(bad, "Broken audit delegation")

    def test_runner_suites_match_test_modules(self):
        runner = (TOOLS / "audit" / "run_audit.py").read_text(encoding="utf-8")
        modules = {p.stem[len("test_"):]: p.stem
                   for p in sorted((TOOLS / "audit").glob("test_*.py"))}
        self.assertGreater(len(modules), 8, "suite corpus looks incomplete")
        self.assertIn("glob(\"test_*.py\")", runner,
                      "suites are hardcoded instead of discovered")
        self.assertIn("--list-json", runner,
                      "runner cannot feed a CI matrix")
        self.assertIn("junit_xml", runner,
                      "runner emits no JUnit report")
        for name in modules:
            self.assertTrue((TOOLS / "audit" / f"test_{name}.py").is_file(),
                            f"suite {name} has no module")

    def test_local_runners_include_the_audit(self):
        static = (TOOLS / "ci" / "static-checks.sh").read_text(encoding="utf-8")
        verify = (TOOLS / "verify.sh").read_text(encoding="utf-8")
        self.assertIn("tools/audit/sync_lang.py --check", static,
                      "static-checks.sh does not guard locale drift")
        self.assertIn("tools/audit/run_audit.py --all", verify,
                      "verify.sh does not run the full audit")

    def test_e2e_scenarios_have_audit_target_defaults(self):
        """A missing AOE_AUDIT_TARGET must degrade to "all", not to empty."""
        bad = []
        for name in ("spells.txt", "professions.txt"):
            path = TOOLS / "e2e" / "scenarios" / name
            text = path.read_text(encoding="utf-8")
            if "${AOE_AUDIT_TARGET:-" not in text:
                bad.append(f"{rel(path)}: no default for AOE_AUDIT_TARGET")
        self.assertNoFindings(bad, "E2E scenarios with unguarded target vars")

    def test_no_new_hardcoded_run_ids(self):
        bad = []
        for path in workflow_files():
            if path.name in KNOWN_PINNED_WORKFLOWS:
                continue
            for number, line in enumerate(
                    path.read_text(encoding="utf-8").splitlines(), 1):
                if RUN_ID.search(line):
                    bad.append(f"{rel(path)}:{number}: hardcoded run id")
        self.assertNoFindings(bad, "New pinned workflow run ids")


if __name__ == "__main__":
    unittest.main()
