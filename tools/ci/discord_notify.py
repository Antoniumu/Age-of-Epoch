#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Мост GitHub → Discord: одно сообщение на каждое событие репозитория.

Зачем этот файл
---------------
Чтобы не сидеть в GitHub Actions, а видеть жизнь репозитория в Discord: пуши,
pull request-ы, ревью, issues, комментарии, релизы, итоги каждого прогона CI и
аудитов (spell/profession) и раз в сутки — сводку «что произошло».

Как это работает
----------------
1. ``.github/workflows/discord-notify.yml`` запускает скрипт на нужные события
   GitHub и передаёт JSON события в ``$GITHUB_EVENT_PATH``.
2. Скрипт собирает из этого JSON embed и отправляет POST-ом на вебхук Discord
   из секрета ``DISCORD_WEBHOOK_URL``.
3. Если секрет не задан — печатается предупреждение, код выхода 0: workflow
   не красит PR, пока вы не настроили канал (см. ``docs/DISCORD.md``).

Запуск руками (локально, без отправки)::

    DISCORD_DRY_RUN=1 GITHUB_EVENT_NAME=workflow_dispatch \\
    GITHUB_REPOSITORY=owner/repo python3 tools/ci/discord_notify.py

Переменные окружения (все необязательные, кроме вебхука):

===========================  =====================================================
DISCORD_WEBHOOK_URL          вебхук по умолчанию (секрет GitHub)
DISCORD_WEBHOOK_UPDATES_URL  отдельный канал для кода/PR/issues (секрет)
DISCORD_WEBHOOK_CI_URL       отдельный канал для итогов CI (секрет)
DISCORD_WEBHOOK_REPORTS_URL  отдельный канал для аудитов и сводок (секрет)
DISCORD_MENTION_ROLE_ID      роль, которую пинговать при падении/релизе
DISCORD_EVENTS               белый список событий через запятую (по умолчанию все)
DISCORD_IGNORE_BRANCHES      маски веток через запятую, например ``arena/*``
DISCORD_SKIP_MARKER          маркер в коммите/PR, отключающий уведомление
DISCORD_NOTIFY_CANCELLED     ``1`` — присылать и отменённые прогоны
DISCORD_DIGEST_HOURS         окно суточной сводки, по умолчанию 24
DISCORD_DRY_RUN              ``1`` — напечатать payload и не отправлять
GITHUB_TOKEN                 нужен только для списка упавших job-ов и сводки
===========================  =====================================================
"""

from __future__ import annotations

import fnmatch
import json
import os
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from dataclasses import dataclass, field
from datetime import datetime, timedelta, timezone
from typing import Any, Callable, Iterable

# ---------------------------------------------------------------------------
# Ограничения Discord (превышение = 400 Bad Request на весь запрос)
# ---------------------------------------------------------------------------

MAX_CONTENT = 2000
MAX_TITLE = 256
MAX_DESCRIPTION = 4096
MAX_FIELD_NAME = 256
MAX_FIELD_VALUE = 1024
MAX_FIELDS = 25
MAX_FOOTER = 2048
MAX_AUTHOR = 256

COLORS = {
    "info": 0x3498DB,      # синий — обычные изменения
    "success": 0x2ECC71,   # зелёный — смержено/прошло
    "failure": 0xE74C3C,   # красный — упало
    "warning": 0xF1C40F,   # жёлтый — аудит/внимание
    "release": 0x9B59B6,   # фиолетовый — релиз
    "muted": 0x95A5A6,     # серый — закрыто/сводка
}

#: Куда отправлять сообщение: updates — обычные каналы, ci — прогоны, reports — аудиты.
KINDS = ("updates", "ci", "reports")


class DiscordError(RuntimeError):
    """Вебхук ответил ошибкой, которую бессмысленно повторять."""


# ---------------------------------------------------------------------------
# Мелкие утилиты
# ---------------------------------------------------------------------------


def env(name: str, default: str = "") -> str:
    """Значение переменной окружения без пробелов по краям."""
    return (os.environ.get(name) or default).strip()


def env_int(name: str, default: int) -> int:
    try:
        return int(env(name, str(default)))
    except ValueError:
        return default


def clip(text: Any, limit: int) -> str:
    """Обрезает строку до лимита Discord, аккуратно помечая обрезку."""
    text = str(text or "")
    if len(text) <= limit:
        return text
    return text[: max(0, limit - 1)].rstrip() + "…"


def one_line(text: Any, limit: int = 200) -> str:
    """Убирает переводы строк и обратные кавычки — чтобы не ломать markdown."""
    text = " ".join(str(text or "").split())
    return clip(text.replace("`", "'"), limit)


def first_line(text: Any, limit: int = 200) -> str:
    """Первая строка сообщения коммита без markdown-проблем."""
    first = str(text or "").strip().splitlines()[0] if str(text or "").strip() else ""
    return one_line(first, limit)


def short_sha(sha: Any) -> str:
    sha = str(sha or "")
    return sha[:7] if len(sha) >= 7 else sha


def plural_ru(n: int, one: str, few: str, many: str) -> str:
    n = abs(int(n))
    if n % 10 == 1 and n % 100 != 11:
        return one
    if n % 10 in (2, 3, 4) and n % 100 not in (12, 13, 14):
        return few
    return many


def is_true(value: Any) -> bool:
    return str(value or "").strip().lower() in ("1", "true", "yes", "on", "да")


def parse_time(value: Any) -> datetime | None:
    """ISO-8601 из GitHub → datetime в UTC (None, если не распарсилось)."""
    if not value:
        return None
    try:
        return datetime.fromisoformat(str(value).replace("Z", "+00:00")).astimezone(timezone.utc)
    except ValueError:
        return None


def mask_webhook(url: str) -> str:
    """URL вебхука — фактически пароль, поэтому в логах показываем только id."""
    parts = urllib.parse.urlsplit(url)
    path = parts.path.rstrip("/")
    tail = path.rsplit("/", 1)[-1]
    safe = path[: -len(tail)] + "***" if tail else path
    return urllib.parse.urlunsplit((parts.scheme, parts.netloc, safe, "", ""))


# ---------------------------------------------------------------------------
# Модель сообщения
# ---------------------------------------------------------------------------


@dataclass
class Notice:
    """Готовое к отправке уведомление."""

    kind: str = "updates"
    content: str = ""
    embed: dict[str, Any] = field(default_factory=dict)
    ping: bool = False

    def payload(self, mention_role: str = "", repo: str = "") -> dict[str, Any]:
        data: dict[str, Any] = {}
        content = self.content
        role = mention_role if (self.ping and mention_role) else ""
        if role:
            content = f"<@&{role}>" + (f"\n{content}" if content else "")
        if content:
            data["content"] = clip(content, MAX_CONTENT)
        if self.embed:
            data["embeds"] = [self.embed]
        data["allowed_mentions"] = {"parse": [], "roles": [role] if role else []}
        return data


def make_embed(
    title: str,
    description: str = "",
    *,
    url: str | None = None,
    color: int = COLORS["info"],
    fields: Iterable[tuple[str, str, bool]] = (),
    footer: str = "",
    timestamp: Any = None,
    author: dict[str, Any] | None = None,
) -> dict[str, Any]:
    embed: dict[str, Any] = {"title": clip(one_line(title), MAX_TITLE), "color": color}
    if description:
        embed["description"] = clip(description, MAX_DESCRIPTION)
    if url:
        embed["url"] = url
    prepared: list[dict[str, Any]] = []
    for name, value, inline in list(fields)[:MAX_FIELDS]:
        value = clip(str(value), MAX_FIELD_VALUE)
        if not value:
            continue
        prepared.append({"name": clip(str(name), MAX_FIELD_NAME), "value": value, "inline": bool(inline)})
    if prepared:
        embed["fields"] = prepared
    if footer:
        embed["footer"] = {"text": clip(footer, MAX_FOOTER)}
    stamp = parse_time(timestamp) if timestamp else datetime.now(timezone.utc)
    if stamp:
        # Discord ждёт ISO-8601; формат с «Z» понимают все версии API.
        embed["timestamp"] = stamp.astimezone(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
    if author and author.get("name"):
        entry = {"name": clip(str(author["name"]), MAX_AUTHOR)}
        if author.get("icon_url"):
            entry["icon_url"] = author["icon_url"]
        if author.get("url"):
            entry["url"] = author["url"]
        embed["author"] = entry
    return embed


# ---------------------------------------------------------------------------
# Контекст события
# ---------------------------------------------------------------------------


class Ctx:
    """Событие GitHub + окружение + доступ к API (нужен только для сводки)."""

    def __init__(self, event: str, payload: dict[str, Any], environment: dict[str, str] | None = None):
        self.event = event
        self.payload = payload or {}
        self.env = dict(os.environ if environment is None else environment)
        repo = (self.payload.get("repository") or {}).get("full_name") or self.env.get("GITHUB_REPOSITORY", "")
        self.repo = repo
        self.server = self.env.get("GITHUB_SERVER_URL") or "https://github.com"
        self.repo_url = f"{self.server}/{repo}" if repo else self.server
        self.token = self.env.get("GITHUB_TOKEN", "")

    # -- удобные ссылки ----------------------------------------------------
    def commit_url(self, sha: str) -> str:
        return f"{self.repo_url}/commit/{sha}"

    def run_url(self, run_id: Any) -> str:
        return f"{self.repo_url}/actions/runs/{run_id}"

    # -- GitHub API --------------------------------------------------------
    def api(self, path: str) -> Any:
        """GET к GitHub API. Ошибка сети не должна ронять уведомление."""
        if not self.token:
            return None
        url = "https://api.github.com" + path
        request = urllib.request.Request(
            url,
            headers={
                "Accept": "application/vnd.github+json",
                "Authorization": f"Bearer {self.token}",
                "User-Agent": "Age-of-Epoch-DiscordNotify/1.0",
                "X-GitHub-Api-Version": "2022-11-28",
            },
        )
        try:
            with urllib.request.urlopen(request, timeout=15) as response:
                return json.loads(response.read().decode("utf-8"))
        except (urllib.error.URLError, TimeoutError, ValueError) as exc:
            print(f"::warning::GitHub API {path}: {exc}")
            return None


# ---------------------------------------------------------------------------
# Обработчики событий
# ---------------------------------------------------------------------------


def _sender(ctx: Ctx) -> tuple[str, str]:
    sender = ctx.payload.get("sender") or {}
    return sender.get("login", ""), sender.get("avatar_url", "")


def _footer(ctx: Ctx, event_name: str) -> str:
    return f"{ctx.repo or 'GitHub'} · {event_name}"


def _author(ctx: Ctx, login: str = "", avatar: str = "") -> dict[str, Any] | None:
    if not login:
        login, avatar = _sender(ctx)
    if not login:
        return None
    data = {"name": login}
    if avatar:
        data["icon_url"] = avatar
    return data


def _skip_marker_hit(*texts: Any) -> bool:
    marker = env("DISCORD_SKIP_MARKER", "[skip discord]").lower()
    if not marker:
        return False
    return any(marker in str(text or "").lower() for text in texts)


def _branch_ignored(branch: str) -> bool:
    patterns = [p.strip() for p in env("DISCORD_IGNORE_BRANCHES").split(",") if p.strip()]
    return any(fnmatch.fnmatchcase(branch, pattern) for pattern in patterns)


def on_push(ctx: Ctx) -> list[Notice]:
    payload = ctx.payload
    if payload.get("deleted"):
        return []  # удаление ветки/тега — не «обновление»
    ref = str(payload.get("ref") or "")
    commits = payload.get("commits") or []
    head = payload.get("head_commit") or {}
    if _skip_marker_hit(head.get("message")):
        return []

    if ref.startswith("refs/tags/"):
        tag = ref[len("refs/tags/"):]
        title = f"🏷️ Тег {tag}"
        description = f"Опубликован тег [`{tag}`]({ctx.repo_url}/releases/tag/{urllib.parse.quote(tag)})"
        branch = ""
    else:
        branch = ref[len("refs/heads/"):] if ref.startswith("refs/heads/") else ref
        if _branch_ignored(branch):
            return []
        count = len(commits)
        if payload.get("created"):
            title = f"🌱 Новая ветка {branch}"
        else:
            title = f"⬆️ {count} {plural_ru(count, 'коммит', 'коммита', 'коммитов')} в {branch}"
        lines: list[str] = []
        for commit in commits[:5]:
            sha = short_sha(commit.get("id"))
            message = first_line(commit.get("message"), 120)
            author = (commit.get("author") or {}).get("name") or (commit.get("author") or {}).get("username") or ""
            suffix = f" — {author}" if author else ""
            lines.append(f"• [`{sha}`]({ctx.commit_url(commit.get('id', ''))}) {message}{suffix}")
        if count > 5:
            lines.append(f"…и ещё {count - 5}")
        if not lines and head:
            lines.append(f"• [`{short_sha(head.get('id'))}`]({ctx.commit_url(head.get('id', ''))}) {first_line(head.get('message'), 120)}")
        description = "\n".join(lines) or "Изменений в коммитах нет."

    compare = payload.get("compare")
    if compare:
        description += f"\n\n[Сравнить изменения]({compare})"

    pusher = payload.get("pusher") or {}
    fields = []
    if branch:
        fields.append(("Ветка", f"`{branch}`", True))
        fields.append(("Коммитов", str(len(commits)), True))
    return [
        Notice(
            kind="updates",
            embed=make_embed(
                title,
                description,
                color=COLORS["info"],
                fields=fields,
                footer=_footer(ctx, "push"),
                timestamp=head.get("timestamp"),
                author=_author(ctx, pusher.get("name") or "", (payload.get("sender") or {}).get("avatar_url", "")),
            ),
        )
    ]


def on_pull_request(ctx: Ctx) -> list[Notice]:
    payload = ctx.payload
    action = str(payload.get("action") or "")
    pr = payload.get("pull_request") or {}
    if not pr:
        return []
    title_text = pr.get("title") or ""
    if _skip_marker_hit(title_text, pr.get("body")):
        return []

    number = pr.get("number")
    url = pr.get("html_url") or f"{ctx.repo_url}/pull/{number}"
    user = (pr.get("user") or {}).get("login", "")
    base = (pr.get("base") or {}).get("ref", "")
    head = (pr.get("head") or {}).get("ref", "")
    fields = [
        ("Ветки", f"`{head}` → `{base}`", True),
        ("Изменения", f"+{pr.get('additions', 0)} −{pr.get('deletions', 0)} в {pr.get('changed_files', 0)} файлах", True),
    ]
    labels = [label.get("name", "") for label in pr.get("labels") or []]
    if labels:
        fields.append(("Метки", ", ".join(f"`{name}`" for name in labels[:8]), False))

    if action == "closed":
        if pr.get("merged"):
            title = f"✅ PR #{number} смержен: {one_line(title_text, 120)}"
            color = COLORS["success"]
        else:
            title = f"🚫 PR #{number} закрыт без слияния: {one_line(title_text, 120)}"
            color = COLORS["muted"]
        description = f"[{one_line(title_text, 160)}]({url})"
    elif action == "opened":
        title = f"🆕 PR #{number}: {one_line(title_text, 120)}"
        color = COLORS["info"]
        description = f"[{one_line(title_text, 160)}]({url})"
        body = one_line(pr.get("body"), 400)
        if body:
            description += f"\n\n{body}"
    elif action == "reopened":
        title = f"♻️ PR #{number} переоткрыт: {one_line(title_text, 120)}"
        color = COLORS["warning"]
        description = f"[{one_line(title_text, 160)}]({url})"
    elif action == "synchronize":
        commits = pr.get("commits")
        tail = f" ({commits} {plural_ru(commits, 'коммит', 'коммита', 'коммитов')})" if commits else ""
        title = f"🔄 PR #{number} обновлён{tail}"
        color = COLORS["info"]
        description = f"[{one_line(title_text, 160)}]({url})"
    elif action == "ready_for_review":
        title = f"👀 PR #{number} готов к ревью: {one_line(title_text, 120)}"
        color = COLORS["success"]
        description = f"[{one_line(title_text, 160)}]({url})"
    elif action == "converted_to_draft":
        title = f"📝 PR #{number} снова черновик"
        color = COLORS["muted"]
        description = f"[{one_line(title_text, 160)}]({url})"
    else:
        return []

    if pr.get("draft") and action != "converted_to_draft":
        title = f"📝 {title}"

    return [
        Notice(
            kind="updates",
            embed=make_embed(
                title,
                description,
                url=url,
                color=color,
                fields=fields,
                footer=_footer(ctx, "pull_request"),
                timestamp=pr.get("updated_at"),
                author=_author(ctx, user, (pr.get("user") or {}).get("avatar_url", "")),
            ),
        )
    ]


def on_pull_request_review(ctx: Ctx) -> list[Notice]:
    payload = ctx.payload
    review = payload.get("review") or {}
    pr = payload.get("pull_request") or {}
    if not review or not pr:
        return []
    state = str(review.get("state") or "").upper()
    mapping = {
        "APPROVED": ("✅ Ревью одобрено", COLORS["success"]),
        "CHANGES_REQUESTED": ("🔴 Запрошены изменения", COLORS["failure"]),
        "COMMENTED": ("💬 Ревью с комментариями", COLORS["info"]),
        "DISMISSED": ("⚪ Ревью отклонено", COLORS["muted"]),
    }
    label, color = mapping.get(state, ("🧐 Новое ревью", COLORS["info"]))
    number = pr.get("number")
    url = review.get("html_url") or pr.get("html_url")
    description = f"PR #{number}: [{one_line(pr.get('title'), 160)}]({pr.get('html_url')})"
    body = one_line(review.get("body"), 400)
    if body:
        description += f"\n\n{body}"
    return [
        Notice(
            kind="updates",
            embed=make_embed(
                f"{label} — PR #{number}",
                description,
                url=url,
                color=color,
                footer=_footer(ctx, "review"),
                timestamp=review.get("submitted_at"),
                author=_author(ctx, (review.get("user") or {}).get("login", ""), (review.get("user") or {}).get("avatar_url", "")),
            ),
        )
    ]


def on_issues(ctx: Ctx) -> list[Notice]:
    payload = ctx.payload
    action = str(payload.get("action") or "")
    issue = payload.get("issue") or {}
    if not issue or issue.get("pull_request"):
        return []
    number = issue.get("number")
    url = issue.get("html_url") or f"{ctx.repo_url}/issues/{number}"
    title_text = issue.get("title") or ""
    if _skip_marker_hit(title_text, issue.get("body")):
        return []
    verb = {
        "opened": ("🆕 Issue", COLORS["info"]),
        "reopened": ("♻️ Issue переоткрыт", COLORS["warning"]),
        "closed": ("✅ Issue закрыт", COLORS["success"]),
        "labeled": ("🏷️ Issue: новая метка", COLORS["muted"]),
    }.get(action)
    if not verb:
        return []
    label, color = verb
    description = f"[{one_line(title_text, 160)}]({url})"
    if action in ("opened", "reopened"):
        body = one_line(issue.get("body"), 400)
        if body:
            description += f"\n\n{body}"
    if action == "labeled":
        names = [name.get("name", "") for name in issue.get("labels") or []]
        if names:
            description += "\n\nМетки: " + ", ".join(f"`{name}`" for name in names[:10])
    return [
        Notice(
            kind="updates",
            embed=make_embed(
                f"{label} #{number}: {one_line(title_text, 120)}",
                description,
                url=url,
                color=color,
                footer=_footer(ctx, "issues"),
                timestamp=issue.get("updated_at"),
                author=_author(ctx, (issue.get("user") or {}).get("login", ""), (issue.get("user") or {}).get("avatar_url", "")),
            ),
        )
    ]


def on_issue_comment(ctx: Ctx) -> list[Notice]:
    payload = ctx.payload
    action = str(payload.get("action") or "")
    if action not in ("created", "deleted"):
        return []
    comment = payload.get("comment") or {}
    issue = payload.get("issue") or {}
    if not comment or not issue:
        return []
    user = comment.get("user") or {}
    login = str(user.get("login") or "")
    if user.get("type") == "Bot" or login.endswith("[bot]"):
        return []  # комментарии ботов (в том числе наши CI-отчёты) не дублируем
    body = str(comment.get("body") or "")
    if "<!-- aoe-" in body:
        return []  # «липкие» отчёты CI с служебными маркерами
    if _skip_marker_hit(body):
        return []

    number = issue.get("number")
    is_pr = bool(issue.get("pull_request"))
    where = "PR" if is_pr else "issue"
    url = comment.get("html_url") or issue.get("html_url")
    snippet = one_line(body, 500)
    verb = "💬 Новый комментарий" if action == "created" else "🗑️ Комментарий удалён"
    title = f"{verb} в {where} #{number}"
    description = f"[{one_line(issue.get('title'), 140)}]({issue.get('html_url')})"
    if snippet and action == "created":
        description += f"\n\n> {snippet}"
    return [
        Notice(
            kind="updates",
            embed=make_embed(
                title,
                description,
                url=url,
                color=COLORS["info"] if action == "created" else COLORS["muted"],
                footer=_footer(ctx, "issue_comment"),
                timestamp=comment.get("created_at"),
                author=_author(ctx, login, user.get("avatar_url", "")),
            ),
        )
    ]


def on_release(ctx: Ctx) -> list[Notice]:
    payload = ctx.payload
    release = payload.get("release") or {}
    if not release or payload.get("action") != "published":
        return []
    tag = release.get("tag_name") or release.get("name") or ""
    url = release.get("html_url")
    notes = one_line(release.get("body"), 800)
    flags = []
    if release.get("prerelease"):
        flags.append("prerelease")
    if release.get("draft"):
        flags.append("draft")
    suffix = f" ({', '.join(flags)})" if flags else ""
    title = f"🚀 Релиз {tag}{suffix}"
    description = f"[{one_line(release.get('name') or tag, 160)}]({url})"
    if notes:
        description += f"\n\n{notes}"
    assets = release.get("assets") or []
    fields = [("Тег", f"`{tag}`", True)]
    if assets:
        fields.append(("Файлов", str(len(assets)), True))
    return [
        Notice(
            kind="reports",
            ping=True,
            embed=make_embed(
                title,
                description,
                url=url,
                color=COLORS["release"],
                fields=fields,
                footer=_footer(ctx, "release"),
                timestamp=release.get("published_at") or release.get("created_at"),
                author=_author(ctx, (release.get("author") or {}).get("login", ""), (release.get("author") or {}).get("avatar_url", "")),
            ),
        )
    ]


def _failed_jobs(ctx: Ctx, run_id: Any) -> list[str]:
    data = ctx.api(f"/repos/{ctx.repo}/actions/runs/{run_id}/jobs?per_page=100")
    if not isinstance(data, dict):
        return []
    return [str(job.get("name")) for job in data.get("jobs") or [] if job.get("conclusion") == "failure"]


def on_workflow_run(ctx: Ctx) -> list[Notice]:
    payload = ctx.payload
    run = payload.get("workflow_run") or {}
    if not run:
        return []
    name = str(run.get("name") or "workflow")
    if name.lower() == "discord notify":
        return []  # защита от рекурсии, даже если workflow добавят в список
    conclusion = str(run.get("conclusion") or run.get("status") or "")
    if conclusion in ("cancelled", "skipped", "stale") and not is_true(env("DISCORD_NOTIFY_CANCELLED")):
        return []

    kind = "reports" if "audit" in name.lower() else "ci"
    is_audit = kind == "reports"
    mapping = {
        "success": ("✅", "успешно", COLORS["success"]),
        "failure": ("❌", "упал", COLORS["failure"]),
        "timed_out": ("⏱️", "таймаут", COLORS["failure"]),
        "action_required": ("⚠️", "требует действий", COLORS["warning"]),
        "neutral": ("➖", "нейтрально", COLORS["muted"]),
        "cancelled": ("🚫", "отменён", COLORS["muted"]),
        "skipped": ("⏭️", "пропущен", COLORS["muted"]),
        "stale": ("💤", "устарел", COLORS["muted"]),
    }
    icon, verb, color = mapping.get(conclusion, ("ℹ️", conclusion or "завершён", COLORS["info"]))
    prefix = "Аудит" if is_audit else "CI"
    # «CI: CI — упал» выглядит глупо, поэтому одноимённый workflow не дублируем.
    subject = prefix if name.lower() == prefix.lower() else f"{prefix}: {name}"
    title = f"{icon} {subject} — {verb}"

    branch = run.get("head_branch") or ""
    if branch and _branch_ignored(branch):
        return []
    sha = short_sha(run.get("head_sha"))
    fields = [("Ветка", f"`{branch}`" if branch else "—", True)]
    if sha:
        fields.append(("Коммит", f"`{sha}`", True))
    if (run.get("run_attempt") or 1) > 1:
        fields.append(("Попытка", str(run.get("run_attempt")), True))
    fields.append(("Событие", f"`{run.get('event', '')}`", True))

    prs = [pr.get("number") for pr in run.get("pull_requests") or [] if pr.get("number")]
    if prs:
        links = ", ".join(f"[#{number}]({ctx.repo_url}/pull/{number})" for number in prs[:3])
        fields.append(("Pull request", links, True))

    if conclusion == "failure":
        failed = _failed_jobs(ctx, run.get("id"))[:8]
        if failed:
            fields.append(("Упали", "\n".join(f"`{job}`" for job in failed), False))

    description = f"[{name}]({run.get('html_url')}) · прогон #{run.get('run_number', '?')}"
    if is_audit:
        description += "\nПодробности: комментарий с отчётом в PR или ветка `ci-*-audit-*`."
    return [
        Notice(
            kind=kind,
            ping=bool(conclusion in ("failure", "timed_out")),
            embed=make_embed(
                title,
                description,
                url=run.get("html_url"),
                color=color,
                fields=fields,
                footer=_footer(ctx, f"workflow_run #{run.get('id', '')}"),
                timestamp=run.get("updated_at"),
                author=_author(ctx),
            ),
        )
    ]


def on_digest(ctx: Ctx) -> list[Notice]:
    """Сводка за окно DISCORD_DIGEST_HOURS (по умолчанию сутки)."""
    hours = env_int("DISCORD_DIGEST_HOURS", 24)
    now = datetime.now(timezone.utc)
    since = now - timedelta(hours=hours)
    since_iso = since.strftime("%Y-%m-%dT%H:%M:%SZ")

    events = ctx.api(f"/repos/{ctx.repo}/events?per_page=100") or []
    recent = [event for event in events if str(event.get("created_at") or "") >= since_iso]

    pushes = 0
    commits = 0
    pr_opened = pr_merged = pr_closed = 0
    merged: list[dict[str, Any]] = []
    issues_opened = issues_closed = 0
    releases: list[dict[str, Any]] = []
    for event in recent:
        kind = event.get("type")
        payload = event.get("payload") or {}
        if kind == "PushEvent":
            pushes += 1
            commits += int(payload.get("size") or len(payload.get("commits") or []))
        elif kind == "PullRequestEvent":
            pr = payload.get("pull_request") or {}
            action = payload.get("action")
            if action == "opened":
                pr_opened += 1
            elif action == "closed":
                if pr.get("merged"):
                    pr_merged += 1
                    merged.append(pr)
                else:
                    pr_closed += 1
        elif kind == "IssuesEvent":
            if payload.get("action") == "opened":
                issues_opened += 1
            elif payload.get("action") == "closed":
                issues_closed += 1
        elif kind == "ReleaseEvent" and payload.get("action") == "published":
            releases.append(payload.get("release") or {})

    runs_data = ctx.api(f"/repos/{ctx.repo}/actions/runs?created=%3E%3D{urllib.parse.quote(since_iso)}&per_page=100")
    runs = (runs_data or {}).get("runs") or []
    green = sum(1 for run in runs if run.get("conclusion") == "success")
    red = [run for run in runs if run.get("conclusion") in ("failure", "timed_out")]
    total_runs = int((runs_data or {}).get("total_count") or len(runs))

    fields = [
        ("Коммиты", f"{pushes} {plural_ru(pushes, 'пуш', 'пуша', 'пушей')} · {commits} {plural_ru(commits, 'коммит', 'коммита', 'коммитов')}", True),
        ("Pull requests", f"открыто {pr_opened} · смержено {pr_merged} · закрыто {pr_closed}", True),
        ("Issues", f"открыто {issues_opened} · закрыто {issues_closed}", True),
        ("CI", f"зелёных {green} · красных {len(red)} (всего {total_runs})", True),
    ]
    if releases:
        fields.append(("Релизы", ", ".join(f"[{rel.get('tag_name', '?')}]({rel.get('html_url', '')})" for rel in releases[:5]), False))

    lines: list[str] = []
    if merged:
        lines.append("**Смержено:**")
        for pr in merged[:5]:
            lines.append(f"• [#{pr.get('number')}]({pr.get('html_url')}) {one_line(pr.get('title'), 100)}")
    if red:
        lines.append("**Упавшие прогоны:**")
        for run in red[:5]:
            lines.append(f"• [{one_line(run.get('name'), 60)}]({run.get('html_url')}) — `{run.get('head_branch', '')}`")
    if not lines:
        lines.append("За окно не было ни мержей, ни падений — тихий участок.")

    title = f"📊 Сводка за {hours} {plural_ru(hours, 'час', 'часа', 'часов')}"
    description = f"С {since.strftime('%d.%m %H:%M')} UTC по {now.strftime('%d.%m %H:%M')} UTC.\n\n" + "\n".join(lines)
    return [
        Notice(
            kind="reports",
            embed=make_embed(
                title,
                description,
                color=COLORS["muted"],
                fields=fields,
                footer=_footer(ctx, "сводка"),
                timestamp=now,
            ),
        )
    ]


def on_test(ctx: Ctx) -> list[Notice]:
    """Тестовое сообщение: проверка, что вебхук и права настроены."""
    return [
        Notice(
            kind="updates",
            embed=make_embed(
                "✅ Вебхук работает",
                "Это тестовое сообщение из `.github/workflows/discord-notify.yml`.\n"
                "Дальше сюда будут приходить пуши, PR, issues, релизы, итоги CI и аудитов.",
                url=ctx.repo_url,
                color=COLORS["success"],
                fields=[
                    ("Репозиторий", f"[{ctx.repo or '—'}]({ctx.repo_url})", True),
                    ("Событие", "`workflow_dispatch`", True),
                ],
                footer=_footer(ctx, "test"),
                timestamp=datetime.now(timezone.utc),
            ),
        )
    ]


HANDLERS: dict[str, Callable[[Ctx], list[Notice]]] = {
    "push": on_push,
    "pull_request": on_pull_request,
    "pull_request_target": on_pull_request,
    "pull_request_review": on_pull_request_review,
    "issues": on_issues,
    "issue_comment": on_issue_comment,
    "release": on_release,
    "workflow_run": on_workflow_run,
}


# ---------------------------------------------------------------------------
# Куда и как отправлять
# ---------------------------------------------------------------------------


def webhook_for(kind: str, environment: dict[str, str] | None = None) -> str:
    """Вебхук для канала: частный (по типу), иначе общий."""
    environment = os.environ if environment is None else environment
    specific = {
        "updates": "DISCORD_WEBHOOK_UPDATES_URL",
        "ci": "DISCORD_WEBHOOK_CI_URL",
        "reports": "DISCORD_WEBHOOK_REPORTS_URL",
    }.get(kind)
    if specific:
        url = (environment.get(specific) or "").strip()
        if url:
            return url
    return (environment.get("DISCORD_WEBHOOK_URL") or "").strip()


def validate_webhook(url: str) -> str:
    """Проверяет, что это действительно Discord-вебхук, и возвращает URL."""
    if not url:
        raise DiscordError("DISCORD_WEBHOOK_URL не задан")
    parts = urllib.parse.urlsplit(url)
    host = (parts.hostname or "").lower()
    allowed = {
        "discord.com",
        "discordapp.com",
        "ptb.discord.com",
        "canary.discord.com",
    }
    if parts.scheme != "https" or host not in allowed or "/api/webhooks/" not in parts.path:
        raise DiscordError(
            "DISCORD_WEBHOOK_URL не похож на вебхук Discord: ожидается "
            "https://discord.com/api/webhooks/<id>/<token>"
        )
    return url


def _retry_after(error: urllib.error.HTTPError, raw_body: str = "") -> float:
    """Сколько ждать при 429: заголовок Retry-After или поле retry_after в теле."""
    header = error.headers.get("Retry-After") if error.headers else None
    if header:
        try:
            return max(0.0, float(header))
        except ValueError:
            pass
    try:
        body = json.loads(raw_body)
        return max(0.0, float(body.get("retry_after", 1.0)))
    except (ValueError, TypeError):
        return 1.0


def post_webhook(url: str, payload: dict[str, Any], attempts: int = 3) -> None:
    """POST с повторами: 429 — по Retry-After, 4xx — сразу ошибка."""
    body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
    headers = {
        "Content-Type": "application/json",
        "User-Agent": "Age-of-Epoch-DiscordNotify/1.0",
    }
    last_error = "нет ответа"
    for attempt in range(1, attempts + 1):
        request = urllib.request.Request(url, data=body, method="POST", headers=headers)
        try:
            with urllib.request.urlopen(request, timeout=15) as response:
                if response.status not in (200, 204):
                    raise DiscordError(f"Discord ответил HTTP {response.status}")
                return
        except urllib.error.HTTPError as error:
            raw_body = error.read().decode("utf-8", "replace")
            last_error = f"HTTP {error.code}: {raw_body[:300]}"
            if error.code == 429:
                wait = _retry_after(error, raw_body)
                print(f"Discord: лимит запросов, ждём {wait:.1f} с (попытка {attempt}/{attempts})")
                time.sleep(wait)
                continue
            if 500 <= error.code < 600 and attempt < attempts:
                time.sleep(2 ** attempt)
                continue
            if error.code == 404:
                raise DiscordError(
                    "404 Unknown Webhook: вебхук удалён или URL скопирован не полностью "
                    "(проверьте секрет DISCORD_WEBHOOK_URL)"
                ) from error
            if error.code == 401:
                raise DiscordError("401 Unauthorized: токен вебхука неверный или отозван") from error
            raise DiscordError(last_error) from error
        except (urllib.error.URLError, TimeoutError) as error:
            last_error = str(error)
            if attempt < attempts:
                time.sleep(2 ** attempt)
                continue
            raise DiscordError(f"не удалось соединиться с Discord: {last_error}") from error
    raise DiscordError(last_error)


def write_summary(lines: list[str]) -> None:
    """Короткая запись в job summary, чтобы настройку было видно в Actions."""
    path = env("GITHUB_STEP_SUMMARY")
    if not path:
        return
    try:
        with open(path, "a", encoding="utf-8") as handle:
            handle.write("\n".join(lines) + "\n")
    except OSError:
        pass


def deliver(notice: Notice, ctx: Ctx, dry_run: bool) -> bool:
    """Отправляет одно уведомление; True — успех (включая dry-run)."""
    url = webhook_for(notice.kind, ctx.env)
    payload = notice.payload(env("DISCORD_MENTION_ROLE_ID"), ctx.repo)
    title = (notice.embed.get("title") if notice.embed else "") or "(без заголовка)"
    if dry_run or not url:
        print(f"[dry-run] {notice.kind}: {title}")
        print(json.dumps(payload, ensure_ascii=False, indent=2))
        return True
    validate_webhook(url)
    post_webhook(url, payload)
    print(f"отправлено в {notice.kind} ({mask_webhook(url)}): {title}")
    return True


# ---------------------------------------------------------------------------
# Точка входа
# ---------------------------------------------------------------------------


def load_payload(path: str) -> dict[str, Any]:
    if not path:
        return {}
    try:
        with open(path, encoding="utf-8") as handle:
            data = json.load(handle)
        return data if isinstance(data, dict) else {}
    except (OSError, ValueError) as error:
        print(f"::warning::Не удалось прочитать событие {path}: {error}")
        return {}


def enabled_events() -> set[str] | None:
    raw = env("DISCORD_EVENTS")
    if not raw:
        return None
    names = {part.strip() for part in raw.split(",") if part.strip()}
    # pull_request_target и pull_request для пользователя — одно и то же событие
    names = {"pull_request" if name == "pull_request_target" else name for name in names}
    return names


def build_notices(ctx: Ctx) -> list[Notice]:
    event = ctx.event
    if event == "schedule":
        return on_digest(ctx)
    if event == "workflow_dispatch":
        inputs = ctx.payload.get("inputs") or {}
        if is_true(inputs.get("test")):
            return on_test(ctx)
        if is_true(inputs.get("digest")):
            return on_digest(ctx)
        return on_test(ctx)  # ручной запуск без входа — тоже проверка связи
    allowed = enabled_events()
    if allowed is not None:
        normalized = "pull_request" if event == "pull_request_target" else event
        if normalized not in allowed:
            print(f"событие {event} отключено через DISCORD_EVENTS")
            return []
    handler = HANDLERS.get(event)
    if handler is None:
        print(f"событие {event} не поддерживается — пропускаю")
        return []
    return handler(ctx)


def main(argv: list[str] | None = None) -> int:
    event = env("GITHUB_EVENT_NAME") or "workflow_dispatch"
    payload = load_payload(env("GITHUB_EVENT_PATH"))
    ctx = Ctx(event, payload)

    if event == "workflow_dispatch" and not payload:
        # локальный запуск без файла события: пусть будет тестовое сообщение
        payload = {"inputs": {}, "repository": {"full_name": env("GITHUB_REPOSITORY")}}
        ctx = Ctx(event, payload)

    configured = any(webhook_for(kind, ctx.env) for kind in KINDS)
    dry_run = is_true(env("DISCORD_DRY_RUN")) or not configured
    if not configured:
        print(
            "::warning::DISCORD_WEBHOOK_URL не задан — уведомления отключены. "
            "Инструкция: docs/DISCORD.md"
        )

    try:
        notices = [notice for notice in build_notices(ctx) if notice]
    except Exception as error:  # noqa: BLE001 — уведомление не должно ломать CI
        print(f"::error::Не удалось собрать уведомление: {error}")
        return 1

    if not notices:
        write_summary(["### Discord", "", "Событие не требует уведомления."])
        return 0

    failures = 0
    for notice in notices:
        try:
            deliver(notice, ctx, dry_run)
        except DiscordError as error:
            failures += 1
            print(f"::error::Discord: {error}")
    write_summary(
        [
            "### Discord",
            "",
            f"Событие: `{event}`",
            f"Сообщений: {len(notices)} (ошибок: {failures})",
            "" if configured else "Вебхук не настроен — сообщения только напечатаны.",
        ]
    )
    if not configured:
        return 0  # без секрета workflow остаётся зелёным
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
