# Автопубликация на Modrinth

Workflow `.github/workflows/branch-release.yml` публикует alpha-сборку на Modrinth после успешной сборки push в ветку `arena/ad7c45b3-age-of-epoch`. После настройки секретов можно также вручную запустить workflow (`workflow_dispatch`) на этой ветке, например чтобы опубликовать текущий коммит без нового push. Pull request ничего на Modrinth не загружает.

## Однократная настройка репозитория

В настройках GitHub-репозитория откройте **Settings → Secrets and variables → Actions** и добавьте:

- **Repository secret** `MODRINTH_TOKEN` — токен Modrinth, которому разрешено создавать версии проекта. Не добавляйте токен в файлы репозитория или сообщения.
- **Repository variable** `MODRINTH_PROJECT_ID` — ID проекта Modrinth (значение из страницы проекта / его URL).

Пока любая из этих настроек отсутствует, CI пропускает публикацию на Modrinth и продолжает собирать и выпускать JAR на GitHub.

## Что загружается

- JAR из `build/libs` (sources/Javadoc jar исключены);
- loader: Fabric;
- Minecraft: 26.3; Java: 25;
- канал: **alpha**;
- уникальная версия вида `5.0.0-arena.<номер запуска CI>`.

На каждое обновление ветки создаётся отдельная версия Modrinth. Релиз GitHub с тем же JAR по-прежнему создаётся независимо.
