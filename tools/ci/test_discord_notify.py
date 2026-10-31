#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Тесты моста GitHub → Discord (tools/ci/discord_notify.py).

Запуск::

    python3 -m unittest discover -s tools/ci -p 'test_*.py' -v

Сети не требуют: проверяют сборку embed-ов, фильтры и разбор payload-ов.
"""

from __future__ import annotations

import contextlib
import importlib.util
import io
import os
import sys
import unittest
import urllib.error
from pathlib import Path
from unittest import mock

MODULE_PATH = Path(__file__).with_name("discord_notify.py")
spec = importlib.util.spec_from_file_location("discord_notify", MODULE_PATH)
discord_notify = importlib.util.module_from_spec(spec)
assert spec.loader is not None
sys.modules["discord_notify"] = discord_notify
spec.loader.exec_module(discord_notify)

Ctx = discord_notify.Ctx
Notice = discord_notify.Notice


def ctx_for(event: str, payload: dict, **environment: str) -> Ctx:
    base = {"GITHUB_REPOSITORY": "owner/repo", "GITHUB_SERVER_URL": "https://github.com"}
    base.update({key: value for key, value in environment.items() if value is not None})
    return Ctx(event, payload, base)


def repository() -> dict:
    return {"full_name": "owner/repo"}


class EmbedLimitsTest(unittest.TestCase):
    def test_title_and_description_are_clipped(self) -> None:
        embed = discord_notify.make_embed("T" * 400, "D" * 5000)
        self.assertLessEqual(len(embed["title"]), discord_notify.MAX_TITLE)
        self.assertLessEqual(len(embed["description"]), discord_notify.MAX_DESCRIPTION)
        self.assertTrue(embed["description"].endswith("…"))

    def test_field_values_are_clipped(self) -> None:
        embed = discord_notify.make_embed("Заголовок", fields=[("Поле", "x" * 3000, True)])
        self.assertLessEqual(len(embed["fields"][0]["value"]), discord_notify.MAX_FIELD_VALUE)

    def test_empty_fields_are_dropped(self) -> None:
        embed = discord_notify.make_embed("Заголовок", fields=[("Поле", "", True)])
        self.assertNotIn("fields", embed)


class PushTest(unittest.TestCase):
    def payload(self, **overrides) -> dict:
        data = {
            "repository": repository(),
            "ref": "refs/heads/main",
            "compare": "https://github.com/owner/repo/compare/a...b",
            "created": False,
            "deleted": False,
            "pusher": {"name": "octocat"},
            "sender": {"login": "octocat", "avatar_url": "https://avatars/1"},
            "head_commit": {"id": "abcdef1234567890", "message": "Merge pull request #1", "timestamp": "2026-10-31T10:00:00Z"},
            "commits": [
                {
                    "id": "abcdef1234567890",
                    "message": "Добавить вебхуки\n\nПодробное описание",
                    "author": {"name": "Anton"},
                }
            ],
        }
        data.update(overrides)
        return data

    def test_push_reports_branch_and_commits(self) -> None:
        notices = discord_notify.on_push(ctx_for("push", self.payload()))
        self.assertEqual(len(notices), 1)
        notice = notices[0]
        self.assertEqual(notice.kind, "updates")
        self.assertIn("main", notice.embed["title"])
        self.assertIn("abcdef1", notice.embed["description"])
        self.assertIn("Добавить вебхуки", notice.embed["description"])
        self.assertIn("https://github.com/owner/repo/compare/a...b", notice.embed["description"])
        self.assertEqual(notice.embed["footer"]["text"], "owner/repo · push")

    def test_deleted_branch_is_ignored(self) -> None:
        self.assertEqual(discord_notify.on_push(ctx_for("push", self.payload(deleted=True))), [])

    def test_skip_marker_in_commit_disables_notice(self) -> None:
        payload = self.payload(head_commit={"id": "abcdef1", "message": "docs: правка [skip discord]"})
        self.assertEqual(discord_notify.on_push(ctx_for("push", payload)), [])

    def test_ignored_branch(self) -> None:
        with mock.patch.dict(os.environ, {"DISCORD_IGNORE_BRANCHES": "arena/*,tmp"}):
            payload = self.payload(ref="refs/heads/arena/abc-age-of-epoch")
            self.assertEqual(discord_notify.on_push(ctx_for("push", payload)), [])
            payload = self.payload(ref="refs/heads/feature/x")
            self.assertEqual(len(discord_notify.on_push(ctx_for("push", payload))), 1)

    def test_tag_push(self) -> None:
        payload = self.payload(ref="refs/tags/v5.0.0", commits=[])
        notices = discord_notify.on_push(ctx_for("push", payload))
        self.assertEqual(len(notices), 1)
        self.assertIn("v5.0.0", notices[0].embed["title"])

    def test_new_branch_note(self) -> None:
        payload = self.payload(ref="refs/heads/feature/new", created=True)
        notices = discord_notify.on_push(ctx_for("push", payload))
        self.assertIn("Новая ветка", notices[0].embed["title"])


class PullRequestTest(unittest.TestCase):
    def payload(self, action: str, **pr_overrides) -> dict:
        pr = {
            "number": 42,
            "title": "Сезон 2: баланс",
            "body": "Описание PR",
            "html_url": "https://github.com/owner/repo/pull/42",
            "user": {"login": "dev", "avatar_url": "https://avatars/2"},
            "base": {"ref": "main"},
            "head": {"ref": "season-2"},
            "draft": False,
            "merged": False,
            "commits": 3,
            "additions": 120,
            "deletions": 30,
            "changed_files": 7,
            "labels": [{"name": "balance"}],
            "updated_at": "2026-10-31T11:00:00Z",
        }
        pr.update(pr_overrides)
        return {"action": action, "pull_request": pr, "repository": repository()}

    def test_opened(self) -> None:
        notice = discord_notify.on_pull_request(ctx_for("pull_request", self.payload("opened")))[0]
        self.assertIn("#42", str(notice.embed["title"]))
        self.assertEqual(notice.embed["color"], discord_notify.COLORS["info"])
        self.assertIn("balance", str(notice.embed["fields"]))
        self.assertEqual(notice.embed["author"]["name"], "dev")

    def test_merged_is_green(self) -> None:
        notice = discord_notify.on_pull_request(
            ctx_for("pull_request", self.payload("closed", merged=True))
        )[0]
        self.assertEqual(notice.embed["color"], discord_notify.COLORS["success"])
        self.assertIn("смержен", notice.embed["title"])

    def test_closed_without_merge_is_grey(self) -> None:
        notice = discord_notify.on_pull_request(ctx_for("pull_request", self.payload("closed")))[0]
        self.assertEqual(notice.embed["color"], discord_notify.COLORS["muted"])

    def test_synchronize(self) -> None:
        notice = discord_notify.on_pull_request(ctx_for("pull_request", self.payload("synchronize")))[0]
        self.assertIn("обновлён", notice.embed["title"])
        self.assertIn("3", notice.embed["title"])

    def test_unknown_action_is_skipped(self) -> None:
        self.assertEqual(discord_notify.on_pull_request(ctx_for("pull_request", self.payload("auto_merge_enabled"))), [])

    def test_pull_request_target_uses_same_handler(self) -> None:
        notices = discord_notify.on_pull_request(ctx_for("pull_request_target", self.payload("opened")))
        self.assertEqual(len(notices), 1)


class ReviewTest(unittest.TestCase):
    def test_approved(self) -> None:
        payload = {
            "action": "submitted",
            "repository": repository(),
            "review": {"state": "approved", "body": "Ок", "html_url": "https://github.com/owner/repo/pull/42#review", "submitted_at": "2026-10-31T11:00:00Z", "user": {"login": "reviewer"}},
            "pull_request": {"number": 42, "title": "Сезон 2", "html_url": "https://github.com/owner/repo/pull/42"},
        }
        notice = discord_notify.on_pull_request_review(ctx_for("pull_request_review", payload))[0]
        self.assertIn("одобрено", notice.embed["title"])
        self.assertEqual(notice.embed["color"], discord_notify.COLORS["success"])


class IssueTest(unittest.TestCase):
    def test_issue_opened(self) -> None:
        payload = {
            "action": "opened",
            "repository": repository(),
            "issue": {
                "number": 7,
                "title": "Падение при входе",
                "body": "Клиент падает",
                "html_url": "https://github.com/owner/repo/issues/7",
                "user": {"login": "player"},
                "updated_at": "2026-10-31T11:00:00Z",
            },
        }
        notice = discord_notify.on_issues(ctx_for("issues", payload))[0]
        self.assertIn("#7", notice.embed["title"])
        self.assertIn("Падение", notice.embed["description"])

    def test_issue_that_is_pr_is_skipped(self) -> None:
        payload = {
            "action": "opened",
            "repository": repository(),
            "issue": {"number": 7, "title": "x", "html_url": "u", "pull_request": {"url": "u"}},
        }
        self.assertEqual(discord_notify.on_issues(ctx_for("issues", payload)), [])


class CommentTest(unittest.TestCase):
    def payload(self, login: str = "player", body: str = "Как это работает?") -> dict:
        return {
            "action": "created",
            "repository": repository(),
            "comment": {
                "body": body,
                "html_url": "https://github.com/owner/repo/issues/7#issuecomment-1",
                "user": {"login": login, "type": "User"},
                "created_at": "2026-10-31T11:00:00Z",
            },
            "issue": {"number": 7, "title": "Вопрос", "html_url": "https://github.com/owner/repo/issues/7"},
        }

    def test_comment(self) -> None:
        notice = discord_notify.on_issue_comment(ctx_for("issue_comment", self.payload()))[0]
        self.assertIn("#7", notice.embed["title"])
        self.assertIn("Как это работает?", notice.embed["description"])

    def test_bot_comment_is_skipped(self) -> None:
        self.assertEqual(discord_notify.on_issue_comment(ctx_for("issue_comment", self.payload("github-actions[bot]"))), [])

    def test_ci_marker_is_skipped(self) -> None:
        payload = self.payload(body="<!-- aoe-ci-report -->\n## Итоги")
        self.assertEqual(discord_notify.on_issue_comment(ctx_for("issue_comment", payload)), [])

    def test_deleted_comment(self) -> None:
        payload = self.payload()
        payload["action"] = "deleted"
        notice = discord_notify.on_issue_comment(ctx_for("issue_comment", payload))[0]
        self.assertIn("удалён", notice.embed["title"])


class ReleaseTest(unittest.TestCase):
    def test_release_pings(self) -> None:
        payload = {
            "action": "published",
            "repository": repository(),
            "release": {
                "tag_name": "v5.0.0",
                "name": "Age of Epoch 5.0.0",
                "body": "Первый сезон",
                "html_url": "https://github.com/owner/repo/releases/tag/v5.0.0",
                "prerelease": False,
                "assets": [{"name": "mod.jar"}],
                "published_at": "2026-10-31T11:00:00Z",
                "author": {"login": "dev"},
            },
        }
        notice = discord_notify.on_release(ctx_for("release", payload))[0]
        self.assertEqual(notice.kind, "reports")
        self.assertTrue(notice.ping)
        self.assertIn("v5.0.0", notice.embed["title"])
        self.assertEqual(notice.embed["color"], discord_notify.COLORS["release"])


class WorkflowRunTest(unittest.TestCase):
    def payload(self, conclusion: str = "success", name: str = "CI", **overrides) -> dict:
        run = {
            "id": 123,
            "name": name,
            "conclusion": conclusion,
            "status": "completed",
            "html_url": "https://github.com/owner/repo/actions/runs/123",
            "head_branch": "main",
            "head_sha": "abcdef1234567890",
            "run_number": 5,
            "run_attempt": 1,
            "event": "push",
            "updated_at": "2026-10-31T11:00:00Z",
            "pull_requests": [],
        }
        run.update(overrides)
        return {"action": "completed", "repository": repository(), "workflow_run": run}

    def test_success(self) -> None:
        notice = discord_notify.on_workflow_run(ctx_for("workflow_run", self.payload()))[0]
        self.assertEqual(notice.kind, "ci")
        self.assertEqual(notice.embed["color"], discord_notify.COLORS["success"])
        self.assertIn("успешно", notice.embed["title"])

    def test_audit_goes_to_reports(self) -> None:
        notice = discord_notify.on_workflow_run(
            ctx_for("workflow_run", self.payload(name="Spell audit (all schools + video)"))
        )[0]
        self.assertEqual(notice.kind, "reports")

    def test_failure_lists_failed_jobs(self) -> None:
        ctx = ctx_for("workflow_run", self.payload("failure"), GITHUB_TOKEN="token")
        jobs = {"jobs": [{"name": "build", "conclusion": "failure"}, {"name": "static", "conclusion": "success"}]}
        with mock.patch.object(Ctx, "api", return_value=jobs):
            notice = discord_notify.on_workflow_run(ctx)[0]
        values = [field["value"] for field in notice.embed["fields"]]
        self.assertTrue(any("build" in value for value in values))
        self.assertTrue(notice.ping)

    def test_cancelled_is_skipped_by_default(self) -> None:
        self.assertEqual(discord_notify.on_workflow_run(ctx_for("workflow_run", self.payload("cancelled"))), [])

    def test_cancelled_can_be_enabled(self) -> None:
        with mock.patch.dict(os.environ, {"DISCORD_NOTIFY_CANCELLED": "1"}):
            self.assertEqual(len(discord_notify.on_workflow_run(ctx_for("workflow_run", self.payload("cancelled")))), 1)

    def test_self_run_is_ignored(self) -> None:
        self.assertEqual(discord_notify.on_workflow_run(ctx_for("workflow_run", self.payload(name="Discord notify"))), [])


class DigestTest(unittest.TestCase):
    def test_digest_counts_events(self) -> None:
        now = discord_notify.datetime.now(discord_notify.timezone.utc)
        stamp = now.strftime("%Y-%m-%dT%H:%M:%SZ")
        events = [
            {"type": "PushEvent", "created_at": stamp, "payload": {"size": 2}},
            {"type": "PullRequestEvent", "created_at": stamp, "payload": {"action": "closed", "pull_request": {"merged": True, "number": 3, "title": "PR", "html_url": "u"}}},
            {"type": "IssuesEvent", "created_at": stamp, "payload": {"action": "opened"}},
        ]
        runs = {"total_count": 2, "runs": [{"conclusion": "success"}, {"conclusion": "failure", "name": "CI", "html_url": "u", "head_branch": "main"}]}
        ctx = ctx_for("schedule", {"repository": repository()}, GITHUB_TOKEN="token")

        def fake_api(path: str):
            return events if path.startswith("/repos/owner/repo/events") else runs

        with mock.patch.object(Ctx, "api", side_effect=fake_api):
            notice = discord_notify.on_digest(ctx)[0]
        values = " ".join(field["value"] for field in notice.embed["fields"])
        self.assertIn("2", values)
        self.assertIn("смержено 1", values)
        self.assertIn("красных 1", values)


class DeliveryTest(unittest.TestCase):
    def test_specific_webhook_wins(self) -> None:
        environment = {
            "DISCORD_WEBHOOK_URL": "https://discord.com/api/webhooks/1/base",
            "DISCORD_WEBHOOK_CI_URL": "https://discord.com/api/webhooks/2/ci",
        }
        self.assertEqual(discord_notify.webhook_for("ci", environment), environment["DISCORD_WEBHOOK_CI_URL"])
        self.assertEqual(discord_notify.webhook_for("updates", environment), environment["DISCORD_WEBHOOK_URL"])

    def test_webhook_validation(self) -> None:
        good = "https://discord.com/api/webhooks/123/token"
        self.assertEqual(discord_notify.validate_webhook(good), good)
        for bad in ("", "http://discord.com/api/webhooks/1/t", "https://example.com/api/webhooks/1/t", "https://discord.com/other"):
            with self.assertRaises(discord_notify.DiscordError):
                discord_notify.validate_webhook(bad)

    def test_masking_keeps_only_id(self) -> None:
        masked = discord_notify.mask_webhook("https://discord.com/api/webhooks/123/super-secret-token")
        self.assertNotIn("super-secret-token", masked)
        self.assertIn("123", masked)

    def test_payload_mentions_role(self) -> None:
        notice = Notice(kind="ci", content="CI упал", embed={"title": "x"}, ping=True)
        payload = notice.payload("999", "owner/repo")
        self.assertTrue(payload["content"].startswith("<@&999>"))
        self.assertEqual(payload["allowed_mentions"], {"parse": [], "roles": ["999"]})

    def test_payload_without_ping_has_no_mentions(self) -> None:
        payload = Notice(kind="ci", content="ok").payload("999")
        self.assertNotIn("<@&999>", payload.get("content", ""))
        self.assertEqual(payload["allowed_mentions"]["roles"], [])


class _Response:
    """Минимальный ответ urlopen для тестов отправки."""

    def __init__(self, status: int = 204) -> None:
        self.status = status

    def __enter__(self) -> "_Response":
        return self

    def __exit__(self, *args: object) -> bool:
        return False


def http_error(code: int, body: bytes = b'{"message": "error"}', headers: dict | None = None) -> urllib.error.HTTPError:
    return urllib.error.HTTPError("https://discord.com/api/webhooks/1/t", code, "error", headers or {}, io.BytesIO(body))


class WebhookPostTest(unittest.TestCase):
    URL = "https://discord.com/api/webhooks/1/token"

    def test_successful_post(self) -> None:
        with mock.patch.object(discord_notify.urllib.request, "urlopen", return_value=_Response()) as opener:
            discord_notify.post_webhook(self.URL, {"content": "Привет"})
        self.assertEqual(opener.call_count, 1)

    def test_401_is_not_retried(self) -> None:
        with mock.patch.object(discord_notify.urllib.request, "urlopen", side_effect=http_error(401)) as opener:
            with self.assertRaises(discord_notify.DiscordError):
                discord_notify.post_webhook(self.URL, {"content": "x"})
        self.assertEqual(opener.call_count, 1)

    def test_429_waits_and_retries(self) -> None:
        error = http_error(429, b'{"retry_after": 1.5}', {"Retry-After": "1.5"})
        with mock.patch.object(discord_notify.urllib.request, "urlopen", side_effect=[error, _Response()]):
            with mock.patch.object(discord_notify.time, "sleep") as sleeper:
                discord_notify.post_webhook(self.URL, {"content": "x"})
        sleeper.assert_called_once_with(1.5)

    def test_429_reads_retry_after_from_body(self) -> None:
        error = http_error(429, b'{"retry_after": 2.5}')
        with mock.patch.object(discord_notify.urllib.request, "urlopen", side_effect=[error, _Response()]):
            with mock.patch.object(discord_notify.time, "sleep") as sleeper:
                discord_notify.post_webhook(self.URL, {"content": "x"})
        sleeper.assert_called_once_with(2.5)

    def test_500_retries_with_backoff(self) -> None:
        with mock.patch.object(discord_notify.urllib.request, "urlopen", side_effect=[http_error(500), _Response()]):
            with mock.patch.object(discord_notify.time, "sleep") as sleeper:
                discord_notify.post_webhook(self.URL, {"content": "x"})
        sleeper.assert_called_once()

    def test_dry_run_does_not_touch_network(self) -> None:
        ctx = ctx_for("push", {"repository": repository()}, DISCORD_WEBHOOK_URL=self.URL)
        notice = Notice(kind="updates", embed=discord_notify.make_embed("Тест"))
        with mock.patch.object(discord_notify, "post_webhook") as poster, contextlib.redirect_stdout(io.StringIO()):
            self.assertTrue(discord_notify.deliver(notice, ctx, dry_run=True))
        poster.assert_not_called()

    def test_missing_webhook_is_not_an_error(self) -> None:
        ctx = ctx_for("push", {"repository": repository()})
        notice = Notice(kind="ci", embed=discord_notify.make_embed("Тест"))
        with contextlib.redirect_stdout(io.StringIO()):
            self.assertTrue(discord_notify.deliver(notice, ctx, dry_run=False))


class BuildNoticesTest(unittest.TestCase):
    def test_dispatch_test(self) -> None:
        ctx = ctx_for("workflow_dispatch", {"repository": repository(), "inputs": {"test": "true"}})
        notices = discord_notify.build_notices(ctx)
        self.assertEqual(len(notices), 1)
        self.assertIn("Вебхук работает", notices[0].embed["title"])

    def test_dispatch_defaults_to_test(self) -> None:
        ctx = ctx_for("workflow_dispatch", {"repository": repository(), "inputs": {}})
        self.assertIn("Вебхук работает", discord_notify.build_notices(ctx)[0].embed["title"])

    def test_events_whitelist(self) -> None:
        ctx = ctx_for("push", {"repository": repository(), "ref": "refs/heads/main", "commits": []})
        with mock.patch.dict(os.environ, {"DISCORD_EVENTS": "pull_request"}):
            self.assertEqual(discord_notify.build_notices(ctx), [])

    def test_events_whitelist_treats_target_as_pull_request(self) -> None:
        payload = {
            "action": "opened",
            "repository": repository(),
            "pull_request": {
                "number": 1, "title": "x", "html_url": "u", "user": {"login": "u"},
                "base": {"ref": "main"}, "head": {"ref": "b"}, "labels": [],
            },
        }
        ctx = ctx_for("pull_request_target", payload)
        with mock.patch.dict(os.environ, {"DISCORD_EVENTS": "pull_request"}):
            self.assertEqual(len(discord_notify.build_notices(ctx)), 1)

    def test_unknown_event_is_silent(self) -> None:
        ctx = ctx_for("watch", {"repository": repository()})
        self.assertEqual(discord_notify.build_notices(ctx), [])


if __name__ == "__main__":
    unittest.main()
