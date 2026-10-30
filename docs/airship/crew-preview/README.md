# Crew model casting — approved and applied to working source

Serve this folder over HTTP. `index.html` renders actual textured 3D player models and a separate non-humanoid bird. All assets are local; no browser requests go to external texture hosts.

The user approved this casting on 2026-10-28. These seven adaptations are now bundled in the
working mod source; Tik has a dedicated in-game brass-bird renderer and Oren scale is 1.6.
See the main airship README for tested revisions and migration. This HTML remains the separate
casting preview; its historical on-page disclaimer describes its state before application.
The old published `airship-preview-176482f` JAR is unchanged.

## Original casting scope

The seven initial PlanetMinecraft candidates in `manifest.json` could not be downloaded: both catalogue and CDN returned HTTP 403, recorded in `fetch-report.txt`. They are **not** what this preview displays.

`alternatives.json` records seven actually acquired MineColonies textures: pinned source links, original byte hashes, original atlases, and adaptations. Each 128×64 custom NPC atlas is cropped to its standard 64×32 legacy UV region; skinview-utils creates mirrored left limbs at render time. MineColonies custom geometry, equipment and hats are omitted. The resulting models are alternatives for review, not representations of the original custom NPC geometry or approved final crew designs. In particular, these do not exactly match the previously described hairstyles/clothes.

Tik is an original procedural brass-bird geometry study in `preview.js`, not a downloaded online skin. Suitable online Tik assets remain unresolved. Do not represent this as completion of the request for eight found internet skins.

Oren is proportionally scaled 1.6 in all axes, with his feet on the same baseline as normal adults. All cards use the same default camera framing; scroll to inspect a character more closely. The wide group view adjusts distance for narrow displays.

## Attribution and license

Seven originals: MineColonies contributors, https://github.com/ldtteam/minecolonies, pinned revision in `alternatives.json`. Repository license copied verbatim to `LICENSE-MineColonies.txt`; no separate per-file artist permission asserted. Original sources retained under `originals/`; modified image derivatives under `skins/`. Changes: standard legacy-UV crop and subsequent locally authored repaints as described below. Preserve these attribution/license/source records when distributing these derivatives. This does not grant rights to any external PlanetMinecraft candidate.

Renderer libraries: skinview3d 3.4.2, skinview-utils, Three.js, MIT notices under `vendor/`.

## Reproduction

From `tools/airship/crew-preview`: `npm ci && npm run build`. To verify screenshots, install Chromium with `npx playwright-core install --with-deps chromium` then `npm test`. The test serves the preview on loopback for its own browser only; the user-facing preview server binds to 0.0.0.0. Browser tests are run by `.github/workflows/crew-model-preview.yml`; generated proofs only become evidence after that run succeeds.


## Исправление 3px UV и новый капитан

`tools/airship/apply_approved_skins.py` теперь читает закреплённые оригиналы, раздельно переносит 3px грани рук Мары/Веи/Селин и вызывает `refine_captain_skin.py`. Просмотрщик и игровые ресурсы используют одинаковые 64×64 PNG и одинаковый выбор wide/slim. `audit_crew_uv.py` проверяет именно грани выбранной модели; прозрачные исходные пиксели на месте wide-краёв доказывают причину прежних чёрных полос этих трёх скинов. Это не обещание устранить любые другие артефакты освещения.

Капитан перерисован локально поверх GPL-атрибутированной основы; это не новый скачанный PMC-скин. Старый голос сохранён. Источники и лицензия не удалены. Геометрия остаётся ванильным манекеном.

## Индивидуальные образы всей команды (ревизия 3)

Шесть прежних адаптаций MineColonies дополнительно перерисованы: Мара — бирюзовая картографическая форма; Брам — фартук механика; Орен — светлые волосы, борода и шарф; Вея — рыжие волосы и лиловый костюм; Селин — серо-голубой наряд со светлым знаком; Лисандр — бордовый китель и шнуровка. Капитан остаётся в ранее переработанном синем кителе. Тик остаётся авторской механической птицей, но получил бирюзовый акцент на груди и хохолке. Это **локальные производные работы**, а не новые скачанные авторские скины; лицензионные исходники, указание авторов и скрипт сохранены.

`apply_approved_skins.py` восстанавливает исходные атласы, применяет независимые wide/slim UV и затем вызывает `refine_crew_skins.py`. Все игровые текстуры и модели предпросмотра используют одинаковые файлы. Новые реплики не меняют выбранные голоса. Старые NPC используют те же resource ID, модель, UUID и профиль; внешние overrides в конфиге намеренно остаются под контролем владельца.
