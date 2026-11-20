# CI/CD на GitHub Actions

Всё, что раньше делалось руками через `build.bat`, теперь делает GitHub: ставит
**JDK 25**, прогревает кэш Gradle/Loom, собирает мод, проверяет jar и пробует
запустить клиент. Ничего локально устанавливать не нужно — ни JDK, ни Gradle.

Файлы:

| Файл | Что делает |
|---|---|
| `.github/workflows/ci.yml` | Основной прогон: PR, push в `main`, ручной запуск, раз в неделю по расписанию |
| `.github/workflows/audit.yml` | **Делегирование аудита**: один job матрицы на каждый `tools/audit/test_*.py`, отчёты артефактами, финальная агрегация. Вызывается из `ci.yml` |
| `.github/workflows/release.yml` | Собирает GitHub Release по тегу `v*` или вручную; prerelease для тестовых версий, SHA-256 файлов |
| `.github/workflows/gameplay-e2e.yml` | **Полный игровой цикл на видео**: прокачка 1→56 с роликами по этапам, клики по GUI с серверным подтверждением, видео каждого умения по классам. Публикация — ветка `ci-gameplay-*` |
| `.github/workflows/publish-curseforge.yml` | Вручную загружает проверенный готовый тестовый JAR на CurseForge; см. [`CURSEFORGE.md`](CURSEFORGE.md) |
| `.github/actions/setup-build-env/action.yml` | JDK 25 (Temurin) + Gradle + кэш; общий шаг для обоих workflow |
| `tools/audit/run_audit.py` | Запуск одного suite-а или всех сразу; пишет JSON, JUnit XML и лог |
| `tools/audit/summarize_reports.py` | Сводит отчёты матрицы и проверяет, что **все** suite-ы отчитались |
| `tools/audit/sync_lang.py` | Чинит/проверяет языковые файлы (`--check` в CI) |
| `tools/ci/static-checks.sh` | Проверки без JDK — те же, что в CI, запускаются локально |
| `tools/ci/check-jar.sh` | Проверяет, что jar пригоден для установки в `mods/` |
| `tools/ci/smoke-client.sh` | Запускает `runClient` под xvfb и ждёт главное меню |
| `tools/ci/pr-report.sh` | Обновляет «липкий» комментарий с итогами прогона в PR |
| `tools/ci/requirements.txt` | Закреплённые версии `tree-sitter` для проверки синтаксиса |
| `.github/dependabot.yml` | Раз в месяц PR-ы на новые версии GitHub Actions |
| `.github/workflows/discord-notify.yml` | Уведомления в Discord: коммиты, PR, issues, релизы, итоги CI/аудитов и суточная сводка; см. [`DISCORD.md`](DISCORD.md) |
| `tools/ci/discord_notify.py` | Собирает и отправляет сообщение на вебхук Discord (стандартная библиотека Python) |
| `tools/ci/test_discord_notify.py` | Тесты форматирования и фильтров уведомлений — входят в `static-checks.sh` |

## Что проверяет CI

| Job | Время | Что ловит |
|---|---|---|
| `static` | ~1 мин | Синтаксис **всех 1591** Java-файлов (tree-sitter), 74 инварианта из `LAUNCH-READINESS.md`, целостность контента и вики, сходимость модели баланса и симуляции сезона, синхронность языковых файлов |
| `audit` | ~2 мин | Матрица аудита ресурсов: JSON, локализация, уникатные предметы, таланты, текстуры/модели/партиклы, звуки, рецепты, лут-таблицы, worldgen. Каждый suite — отдельный job со своим отчётом |
| `wrapper` | ~10 сек | Подмену `gradle/wrapper/gradle-wrapper.jar` (сверка с официальной контрольной суммой) |
| `build` | ~10–20 мин | Настоящую компиляцию на JDK 25 (`./gradlew build`), затем `tools/ci/check-jar.sh`: `fabric.mod.json`, подставленная версия, mixin-конфиги, наличие `assets/`, `data/`, классов |
| `client-boot` | ~10–20 мин | Падения инициализации порта: миксины, регистрация экранов и меню, worldgen, загрузка датапака. Ждёт в `run/logs/latest.log` маркер главного меню; падение = красный job |
| `ci-ok` | 5 сек | Сводит всё в одну галочку — её удобно сделать обязательной в branch protection. В PR дополнительно обновляет **один** комментарий с итогами (размер jar, за сколько секунд клиент дошёл до меню), см. `tools/ci/pr-report.sh` |

Уведомления о прогонах (включая итоги аудитов) можно получать в Discord-канале:
достаточно одного секрета `DISCORD_WEBHOOK_URL` — см. [`DISCORD.md`](DISCORD.md).

`client-boot` в pull request-ах помечен как **не блокирующий** (`continue-on-error`):
порт ещё не доведён, и первый запуск в игре может быть красным. На `main`,
по расписанию и при ручном запуске он блокирующий. Когда порт стабилизируется —
уберите `continue-on-error` из `client-boot` и из ветки `client-boot` в `ci-ok`.

## Как устроено делегирование аудита

`tools/audit/` — набор тестов на чистом `unittest` из стандартной библиотеки:
ни JDK, ни Gradle, ни `pip install` не нужны. Каждый модуль `test_*.py` считается
отдельным **suite-ом**. Список suite-ов не захардкожен: job `discover` в
`audit.yml` получает его командой `python3 tools/audit/run_audit.py --list-json`
и передаёт в `strategy.matrix`. Добавленный `test_foo.py` автоматически получает
свой job без правки workflow.

```
ci.yml / job audit
  └── audit.yml (workflow_call)
        ├── discover            → JSON-массив suite-ов
        ├── audit / <suite>     → N параллельных job-ов, fail-fast: false
        │     sync_lang.py --check
        │     run_audit.py --suite <suite> --report-dir audit-reports
        │     upload-artifact: <suite>.json + .xml + .txt
        └── aggregate           → download-artifact (merge-multiple)
                                  summarize_reports.py --expect-suites …
                                  проверка needs.audit.result
```

`fail-fast: false` принципиален: цель аудита — увидеть **все** баги за один прогон,
а не остановиться на первом. Агрегатор не доверяет одному лишь результату
матрицы — он сверяет число отчётов со списком из `discover` и проверяет
`ok: true` в каждом JSON. Job матрицы, упавший до загрузки артефакта, поэтому
не «растворяется» в зелёном прогоне.

Локально то же самое одной командой:

```bash
python3 tools/audit/run_audit.py --list               # какие suite-ы есть
python3 tools/audit/run_audit.py --suite assets        # один suite, как в CI
python3 tools/audit/run_audit.py --all --report-dir audit-reports
python3 tools/audit/summarize_reports.py audit-reports
python3 tools/audit/sync_lang.py                     # починить локали
python3 tools/audit/sync_lang.py --check              # только проверка (CI)
```

Полный `./tools/verify.sh` тоже включает `--all`; `static-checks.sh` в CI
вызывает только быстрый `sync_lang.py --check`, чтобы не гонять аудит дважды.

Отдельно запустить аудит без всего CI:

```bash
gh workflow run audit.yml
```

## Как этим пользоваться

Скачать собранный jar (артефакт называется `Age-of-Epoch-jar`):

```bash
gh run list --workflow=ci.yml --limit 5          # последние прогоны и их статус
gh run watch                                     # следить за текущим в терминале
gh run download --name Age-of-Epoch-jar -D mods  # забрать jar последнего успешного прогона
gh run view --log-failed                         # только упавшие шаги, с логами
gh run rerun <run-id>                            # перезапустить прогон
gh run rerun <run-id> --failed                   # перезапустить только упавшие job-ы
```

Запустить CI на ветке руками, не делая коммита:

```bash
gh workflow run ci.yml --ref arena/c141906f-age-of-epoch
```

Посмотреть, почему упало конкретное место:

```bash
gh run view <run-id> --log              # весь лог
gh run view <run-id> --log-failed       # только ошибки
# логи клиента и crash-report сохраняются артефактом client-logs
gh run download <run-id> --name client-logs -D /tmp/client-logs
```

## Релиз

```bash
# версия внутри jar берётся из gradle.properties (mod_version)
git tag v5.0.0
git push origin v5.0.0
```

Workflow `Release` соберёт jar, сверит тег с `mod_version` (несовпадение = ошибка),
прогонит проверку содержимого и версии внутри JAR. К GitHub Release прикладываются
игровой JAR, `-sources.jar` и `SHA256SUMS.txt`. Заметки берутся из
`docs/releases/<версия>.md`, если файл существует, иначе генерируются GitHub.
Секретов для этого не нужно: используется встроенный `GITHUB_TOKEN`.

Версии с суффиксом (`-beta.1` и т. п.) публикуются как **prerelease** и не
становятся latest stable. Существующие теги и файлы релиза не перезаписываются.

Ручная сборка уже существующего тега, для которого ещё нет GitHub Release:

```bash
gh workflow run release.yml -f tag=v5.0.0
```

Создать новый тег и тестовый релиз на конкретном проверенном коммите:

```bash
gh workflow run release.yml --ref main \
  -f tag=v5.2.6-beta.1 -f source_ref=FULL_COMMIT_SHA
```

`source_ref` нужен только для нового тега; версия в выбранных исходниках обязана
совпадать с тегом. Для загрузки этого же готового тестового JAR на CurseForge
есть отдельный ручной workflow — см. [`CURSEFORGE.md`](CURSEFORGE.md).

Для запроса 5.2.6-beta.1 также разрешён ограниченный запуск от push файла
`release.yml` **только** в ветке `arena/ad2df10f-age-of-epoch`: интеграция Arena
не имеет Actions write для `workflow_dispatch`. Этот путь принимает лишь beta,
прикрепляет сборку к точному SHA события и перед публикацией ждёт успешных
`ci-ok`, полного игрового аудита и включения этого коммита в основную ветку.
Если проверка падает или условия не выполнены за 15 минут, файл не публикуется.

## Branch protection (рекомендуется)

Settings → Branches → правило для `main` → *Require status checks to pass* →
выбрать **`ci-ok`**. Тогда PR нельзя смержить, пока не зелены синтаксис,
инварианты и сборка.

То же самое через `gh`:

```bash
gh api -X PUT repos/Antoniumu/Age-of-Epoch/branches/main/protection \
  -F required_status_checks.strict=true \
  -f 'required_status_checks.contexts[]=ci-ok' \
  -F enforce_admins=false \
  -F 'required_pull_request_reviews=null' \
  -F restrictions=null
```

## Локальный прогон того же, что в CI

Проверки без JDK (нужен только Python 3):

```bash
python3 -m pip install -r tools/ci/requirements.txt
bash tools/ci/static-checks.sh
```

Полная сборка — как в CI (JDK 25 обязателен):

```bash
export JAVA_HOME=/path/to/jdk-25
./gradlew build --stacktrace --no-problems-report
bash tools/ci/check-jar.sh build/libs
```

Smoke-тест клиента (Linux + xvfb):

```bash
xvfb-run -a bash tools/ci/smoke-client.sh   # таймаут 20 мин, переопределяется SMOKE_TIMEOUT
```

## Настройки, которые чаще всего правят

**Версия JDK.** Живёт в одном месте — `.github/actions/setup-build-env/action.yml`
(`java-version: '25'`), плюс сама проверка `javac -version` там же. Менять нужно
одновременно с `options.release` в `build.gradle`.

**Кэш.** `gradle/actions/setup-gradle` кэширует Gradle User Home целиком, включая
Loom/Minecraft — повторная сборка быстрее первой примерно вдвое. Лимит GitHub —
~10 ГБ на репозиторий; если кэш начнёт вытеснять сам себя, в
`gradle-home-cache-excludes` в composite action можно добавить
`caches/fabric-loom/minecraftMaven` (сборка станет медленнее, кэш — меньше).

**Расписание.** Недельный прогон (`cron: '0 4 * * 1'`) нужен, чтобы падение
случайно протухшего URL зависимости или исчезнувшей версии Fabric API
обнаруживалось до релиза, а не в момент релиза.

**Пропуск прогона.** `[skip ci]` в сообщении коммита — стандартное поведение GitHub
для `push`. Для PR надёжнее черновик (`gh pr ready`/`gh pr create --draft`).

## Чего CI пока не проверяет

* **Сервер.** Тестируется только клиент. Выделенный сервер (`runServer`) можно
  добавить тем же способом: job с `./gradlew runServer` и ожиданием `Done (…)`
  в `run/logs/latest.log`.
* **Modrinth.** Публикация не настроена.
* **CurseForge.** Готовый тестовый JAR можно отправить отдельным ручным workflow
  `publish-curseforge.yml`; нужны существующий Project ID и секрет
  `CURSEFORGE_API_TOKEN`. Загрузка и модерация — разные этапы. Подробности и
  ограничения — в [`CURSEFORGE.md`](CURSEFORGE.md).

## Полный игровой цикл (gameplay-e2e)

Workflow `.github/workflows/gameplay-e2e.yml` отвечает на вопрос «игра реально
играется?» тремя прогонами dev-клиента под Xvfb, каждый с видео:

1. **Прокачка** (`tools/e2e/scenarios/progression.txt`): серверный зонд
   `ProgressionProbe` (флаг `run/aoe-progression.flag`, `/aoe devtest progression`)
   качает персонажа настоящим `GiveExp` — в кадре «+N опыта» и level-up'ы, а
   HUD-оверлей подписывает этап. На вехах 10/25/50/56 вкладываются очки
   характеристик, изучается заклинание школы, выбирается путь Вознесения,
   вкачивается цепочка умений и назначается умение на хотбар. Видео режется на
   ролики по этапам маркерами `AOEPROG_MARK` (`run/progression-timeline.txt`).
   `alloc=none` во флаге отключает трату очков — их тратит GUI-сценарий.
2. **Клики в GUI** (`tools/e2e/scenarios/gui.txt`): клиент под флагом
   `run/aoe-gui-dump.flag` пишет `run/gui-widgets.txt` — реальные координаты,
   подписи и классы всех кнопок открытого экрана (`GuiWidgetDumper`). Сценарий
   кликает настоящей мышью по центру виджета (`click_widget <regex>[#N]`),
   а сервер подтверждает обработку пакета строкой `AOEGUI packet=... ok=...`
   (маркеры в `AllocateStatPacket`, `AllocateSpellPacket`, `PerkChangePacket`,
   `AscensionPickPacket`, `AscensionAllocPacket`). Кликабельные области, которые
   не являются кнопками (узлы цепочки Вознесения), экран отдаёт сам через
   интерфейс `IGuiDebugDump` — как `DebugTarget` с id `node:<spellId>`.
3. **Каждое умение** (`tools/e2e/scenarios/spells.txt`): `SpellAuditProbe`
   применяет все умения настоящим путём каста, видео режется на ролики по
   классам (см. также отдельный workflow `spell-audit.yml`).

Итоги — в ветке `ci-gameplay-<ветка>` (ролики играются прямо в GitHub),
комментарием в PR и артефактом `gameplay-e2e`. Job красный, пока не зелёные
все три части: `AOEPROG done ... failed=0`, подтверждены четыре ключевых клика
(`stat_alloc`, `spell_alloc`, `ascension_pick`, `ascension_alloc` с `ok=true`)
и `AOEAI done ... failed=0` плюс структурный аудит.
