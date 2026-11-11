# CI/CD на GitHub Actions

Всё, что раньше делалось руками через `build.bat`, теперь делает GitHub: ставит
**JDK 25**, прогревает кэш Gradle/Loom, собирает мод, проверяет jar и пробует
запустить клиент. Ничего локально устанавливать не нужно — ни JDK, ни Gradle.

Файлы:

| Файл | Что делает |
|---|---|
| `.github/workflows/ci.yml` | Основной прогон: PR, push в `main`, ручной запуск, раз в неделю по расписанию |
| `.github/workflows/release.yml` | Собирает и публикует GitHub Release по тегу `v*` |
| `.github/actions/setup-build-env/action.yml` | JDK 25 (Temurin) + Gradle + кэш; общий шаг для обоих workflow |
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
| `static` | ~1 мин | Синтаксис **всех 1591** Java-файлов (tree-sitter), 74 инварианта из `LAUNCH-READINESS.md`, целостность контента и вики, сходимость модели баланса и симуляции сезона |
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
прогонит проверку содержимого и создаст GitHub Release с автоматическими release notes
(`gh release create --generate-notes`) и приложенными jar-ами — обычным и `-sources`.
Секретов для этого не нужно: используется встроенный `GITHUB_TOKEN`.

Ручной релиз без тега (например, пересобрать существующий):

```bash
gh workflow run release.yml -f tag=v5.0.0
```

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
* **Игру по-настоящему.** Smoke-тест доказывает только то, что мод грузится и
  доходит до меню: мир, бой, GUI и рендер требуют ручной проверки.
  Известные ограничения порта — в [`BUILD-REPORT.md`](BUILD-REPORT.md) §4.
* **Modrinth/CurseForge.** Публикации нет намеренно: нужны project id и токен,
  а формат загрузки у них меняется чаще, чем хочется. Когда понадобится —
  это отдельный gated-шаг в `release.yml` (`if: secrets.MODRINTH_TOKEN != ''`).
