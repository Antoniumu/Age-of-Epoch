# Crew model casting — NOT installed in Minecraft

Serve this folder over HTTP. `index.html` renders actual textured 3D player models and a separate non-humanoid bird. All assets are local; no browser requests go to external texture hosts.

## Honest scope

The seven initial PlanetMinecraft candidates in `manifest.json` could not be downloaded: both catalogue and CDN returned HTTP 403, recorded in `fetch-report.txt`. They are **not** what this preview displays.

`alternatives.json` records seven actually acquired MineColonies textures: pinned source links, original byte hashes, original atlases, and adaptations. Each 128×64 custom NPC atlas is cropped to its standard 64×32 legacy UV region; skinview-utils creates mirrored left limbs at render time. MineColonies custom geometry, equipment and hats are omitted. The resulting models are alternatives for review, not representations of the original custom NPC geometry or approved final crew designs. In particular, these do not exactly match the previously described hairstyles/clothes.

Tik is an original procedural brass-bird geometry study in `preview.js`, not a downloaded online skin. Suitable online Tik assets remain unresolved. Do not represent this as completion of the request for eight found internet skins.

Oren is proportionally scaled 1.6 in all axes, with his feet on the same baseline as normal adults. All cards use the same default camera framing; scroll to inspect a character more closely. The wide group view adjusts distance for narrow displays.

## Attribution and license

Seven originals: MineColonies contributors, https://github.com/ldtteam/minecolonies, pinned revision in `alternatives.json`. Repository license copied verbatim to `LICENSE-MineColonies.txt`; no separate per-file artist permission asserted. Original sources retained under `originals/`; modified image derivatives under `skins/`. Changes: standard legacy-UV crop only, no repainting. Preserve these attribution/license/source records when distributing these derivatives. This does not grant rights to any external PlanetMinecraft candidate.

Renderer libraries: skinview3d 3.4.2, skinview-utils, Three.js, MIT notices under `vendor/`.

## Reproduction

From `tools/airship/crew-preview`: `npm ci && npm run build`. To verify screenshots, install Chromium with `npx playwright-core install --with-deps chromium` then `npm test`. The test serves the preview on loopback for its own browser only; the user-facing preview server binds to 0.0.0.0. Browser tests are run by `.github/workflows/crew-model-preview.yml`; generated proofs only become evidence after that run succeeds.
