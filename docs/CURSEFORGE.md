# Публикация тестового билда на CurseForge

Workflow [`publish-curseforge.yml`](../.github/workflows/publish-curseforge.yml)
загружает **готовый игровой JAR из GitHub Release**. Он не пересобирает мод и не
загружает `-sources.jar` или `-dev.jar`. Сверяются SHA-256, версия, mod ID,
Minecraft и обязательные зависимости в `fabric.mod.json`.

## Однократная настройка

1. Создайте свой Minecraft-проект в кабинете автора CurseForge. Upload API не
   создаёт проекты: одного токена недостаточно. Скопируйте числовой **Project ID**.
2. В GitHub → Settings → Secrets and variables → Actions → **Secrets** добавьте
   `CURSEFORGE_API_TOKEN` — токен загрузки CurseForge, имеющий доступ к этому проекту.
   Никогда не помещайте его в исходники, логи, workflow inputs или сообщения.
3. В разделе **Variables** можно сохранить `CURSEFORGE_PROJECT_ID`. Либо передавайте
   ID при каждом ручном запуске. Project ID не является секретом.

## Загрузка

Откройте Actions → **Publish test build to CurseForge** → Run workflow.
Укажите существующий тег, например `v5.2.6-beta.1`, и Project ID.

Эквивалентная команда (ID ниже — заполнитель, замените своим):

```bash
gh workflow run publish-curseforge.yml --ref main \
  -f tag=v5.2.6-beta.1 -f project_id=YOUR_PROJECT_ID
```

Workflow принимает только опубликованные GitHub **prerelease** с тегом
`vX.Y.Z-beta.N`. На CurseForge файл всегда помечается как **Beta**, Fabric,
соответствующая версия Minecraft и Java 25. Fabric API и Trinkets Updated
объявляются обязательными зависимостями; их проекты должны быть доступны
на CurseForge. Ошибка API или разрешения зависимости делает прогон красным,
а не маскируется под успешную публикацию.

Успешная отправка файла через API ещё не означает одобрение модерацией.
Проверьте статус проекта и файла в кабинете CurseForge. Не перезапускайте
успешную загрузку без необходимости: это может создать второй файл.
