# Уведомления в Discord

Весь репозиторий «вещает» в Discord-канал: коммиты, pull request-ы, ревью, issues,
комментарии, релизы, итоги каждого прогона CI и аудитов, плюс раз в сутки сводка
«что произошло». Настраивается **одним секретом** — URL-ом вебхука.

| Файл | Что делает |
|---|---|
| `.github/workflows/discord-notify.yml` | Ловит события GitHub и запускает скрипт (5 шагов, ~20 секунд) |
| `tools/ci/discord_notify.py` | Собирает embed и отправляет POST на вебхук (только stdlib) |
| `tools/ci/test_discord_notify.py` | 40 тестов: форматирование, фильтры, лимиты Discord; гоняются в CI |

---

## TL;DR — три шага

### 1. Создать вебхук в Discord

1. Правый клик по серверу → **Настройки сервера** → **Интеграции** → **Вебхуки**.
2. **Новый вебхук** → выбрать канал (например `#age-of-epoch`) → задать имя
   (например `Age of Epoch CI`) и аватар, если хочется.
3. **Копировать URL вебхука**. Ссылка выглядит так:

   ```
   https://discord.com/api/webhooks/123456789012345678/AbCdEf...
   ```

   Нужно право **Управлять вебхуками** (`Manage Webhooks`) на сервере.

### 2. Положить URL в секреты репозитория

Через CLI (подставит значение без эха в историю):

```bash
gh secret set DISCORD_WEBHOOK_URL
# вставить URL и нажать Enter
gh secret list          # убедиться, что секрет появился
```

Или в браузере: **Settings → Secrets and variables → Actions → New repository secret**,
имя — `DISCORD_WEBHOOK_URL`, значение — скопированный URL.

### 3. Проверить

```bash
gh workflow run discord-notify.yml -f test=true     # после мержа в main
gh run list --workflow=discord-notify.yml --limit 3
```

В канал должно прийти сообщение «✅ Вебхук работает». Всё, дальше уведомления
уходят сами — ничего дописывать в workflow не нужно.

> Пока workflow не попал в `main`, события `schedule` и `workflow_run` для него
> не срабатывают: GitHub берёт эти триггеры из ветки по умолчанию. Push/PR в
> ветку с этим файлом уже работают.

---

## Что приходит в канал

| Событие | Сообщение | Куда |
|---|---|---|
| `push` | ветка, до 5 коммитов со ссылками, автор, ссылка «сравнить изменения» | updates |
| `pull_request` | открыт / обновлён (с числом коммитов) / готов к ревью / смержен / закрыт без слияния | updates |
| `pull_request_review` | одобрено / запрошены изменения / комментарий ревьюера | updates |
| `issues` | открыт / переоткрыт / закрыт / новая метка (issues-PR не дублируются) | updates |
| `issue_comment` | новый или удалённый комментарий; комментарии ботов и «липкие» отчёты CI игнорируются | updates |
| `release` | тег, заметки, число файлов, **пинг роли**, если `DISCORD_MENTION_ROLE_ID` задан | reports |
| `workflow_run` | итог каждого прогона: ✅/❌, ветка, коммит, PR, **имена упавших job-ов**; аудиты помечены отдельно | ci / reports |
| `schedule` | раз в сутки: пуши и коммиты, PR (открыто/смержено/закрыто), issues, релизы, зелёные/красные прогоны, список мержей и падений | reports |

Цвета: синий — обычные изменения, зелёный — успех, красный — падение,
жёлтый — внимание (аудит), фиолетовый — релиз, серый — закрыто/сводка.

Пуш в ветку, у которой уже открыт PR, даёт два сообщения — «N коммитов в ветку»
и «PR обновлён». Это разные события (пуш может быть и не в PR-ветку), но если
дубли мешают, уберите `push` из `DISCORD_EVENTS` или добавьте ветки в
`DISCORD_IGNORE_BRANCHES`.

### Разделение по каналам (по желанию)

Один вебхук — один канал. Если хочется три канала, создайте три вебхука и
задайте секреты; общий `DISCORD_WEBHOOK_URL` остаётся запасным вариантом:

| Секрет | Что туда идёт |
|---|---|
| `DISCORD_WEBHOOK_UPDATES_URL` | коммиты, PR, ревью, issues, комментарии |
| `DISCORD_WEBHOOK_CI_URL` | итоги CI, релизной сборки, скриншотов |
| `DISCORD_WEBHOOK_REPORTS_URL` | аудиты (spell/profession), релизы, суточная сводка |

---

## Тонкая настройка (Repository variables)

Всё необязательное и задаётся переменными репозитория — секреты не нужны:

```bash
gh variable set DISCORD_EVENTS --body "push,pull_request,issues,release,workflow_run"
gh variable set DISCORD_IGNORE_BRANCHES --body "arena/*,dependabot/*"
gh variable set DISCORD_MENTION_ROLE_ID --body "123456789012345678"
gh variable set DISCORD_DIGEST_HOURS --body "24"
```

| Переменная | По умолчанию | Смысл |
|---|---|---|
| `DISCORD_EVENTS` | все | Белый список событий через запятую: `push,pull_request,pull_request_review,issues,issue_comment,release,workflow_run`. `none` = полностью выключить (сводка по расписанию останется) |
| `DISCORD_IGNORE_BRANCHES` | — | Маски веток, о которых молчим: `arena/*`, `dependabot/*` |
| `DISCORD_SKIP_MARKER` | `[skip discord]` | Маркер в сообщении коммита/PR/комментария, отключающий уведомление |
| `DISCORD_MENTION_ROLE_ID` | — | ID роли для пинга при падении CI и релизе. ID: настройки сервера → роли → правый клик → «Копировать ID роли» (нужен Developer Mode). В настройках роли должно быть разрешено упоминание, иначе Discord не подсветит |
| `DISCORD_NOTIFY_CANCELLED` | `0` | `1` — присылать и отменённые прогоны (по умолчанию молчим: `concurrency` часто отменяет старые прогоны) |
| `DISCORD_DIGEST_HOURS` | `24` | Окно суточной сводки в часах |

Посмотреть/удалить:

```bash
gh variable list
gh variable delete DISCORD_EVENTS
```

### Время суточной сводки

Cron в `.github/workflows/discord-notify.yml`:

```yaml
schedule:
  - cron: '0 7 * * *'   # 07:00 UTC = 09:00 в Копенгагене летом, 08:00 зимой
```

Cron всегда в UTC. Для 09:00 по Копенгагену: летом (CEST, UTC+2) — `0 7 * * *`,
зимой (CET, UTC+1) — `0 8 * * *`. Расписание можно менять, секреты при
`schedule` доступны.

---

## Проверка и отладка

```bash
# тестовое сообщение (по умолчанию при ручном запуске)
gh workflow run discord-notify.yml -f test=true

# сводка за последние 24 часа прямо сейчас
gh workflow run discord-notify.yml -f digest=true

# ничего не отправлять: payload будет напечатан в лог прогона
gh workflow run discord-notify.yml -f test=true -f dry_run=true

# посмотреть, что скрипт сделал
gh run list --workflow=discord-notify.yml --limit 5
gh run view <run-id> --log
```

Локально, без отправки в Discord:

```bash
DISCORD_DRY_RUN=1 GITHUB_EVENT_NAME=workflow_dispatch \
GITHUB_REPOSITORY=Antoniumu/Age-of-Epoch \
python3 tools/ci/discord_notify.py

python3 -m unittest discover -s tools/ci -p 'test_*.py' -v
```

### Если сообщения не приходят

| Симптом | Причина | Что делать |
|---|---|---|
| В Actions `warning: DISCORD_WEBHOOK_URL не задан` | секрет не создан или создан в другом репозитории/организации | `gh secret list`; для организации — `gh secret set --org <org> --repos <repo>` |
| `404 Unknown Webhook` | вебхук удалён или URL скопирован не полностью | создать вебхук заново, обновить секрет |
| `401 Unauthorized` | токен вебхука отозван | то же |
| Workflow зелёный, в канале тихо | сработал фильтр | проверить `DISCORD_EVENTS`, `DISCORD_IGNORE_BRANCHES`, маркер `[skip discord]`; отменённые прогоны не приходят по умолчанию |
| Нет уведомлений о PR из форков | `pull_request` из форка не получает секреты | ничего: workflow использует `pull_request_target`, уведомления придут (после мержа в `main`) |
| `429` в логе | лимит Discord (~30 сообщений/мин на вебхук) | скрипт сам ждёт `Retry-After` и повторяет; при массовых событиях разнесите их по каналам |
| Поток слишком шумный | уведомляется каждая ветка | `DISCORD_IGNORE_BRANCHES=arena/*`, отдельный канал для CI, `DISCORD_EVENTS` |

---

## Безопасность

- **URL вебхука — это пароль**: кто его знает, тот может писать в канал.
  Не вставляйте его в issue/PR/скриншоты. В логах скрипт печатает только id
  вебхука (`…/webhooks/123***`), токен маскируется.
- Утёк URL — в Discord удалите вебхук (Integrations → Webhooks) и создайте
  новый; старый сразу перестанет работать.
- `pull_request_target` используется намеренно: у PR из форков обычный
  `pull_request` не получает секреты. Workflow при этом берёт скрипт из базовой
  ветки (`checkout` без `ref` на PR) и код из PR не запускает — утекать нечему.
- Права workflow — только чтение (`contents/actions/issues/pull-requests: read`),
  писать в репозиторий он не может.

## Чего вебхуки не умеют

Вебхук — односторонний канал: он **только отправляет** сообщения. Discord не
отдаёт «кто прочитал сообщение» (read receipts) и не позволяет вебхуку читать
историю канала. Если нужна обратная связь — реакции на сообщение, опросы или
полноценный бот с Gateway-подключением и `Message Content Intent`; это уже не
вебхук, а отдельное приложение Discord.

«Отчёты» в этом репозитории — это суточная сводка (`schedule`), итоги прогонов
CI/аудитов и ссылки на отчётные комментарии в PR: всё это приходит в канал без
дополнительных настроек.
