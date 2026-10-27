# Crew and Winged Sailor asset ledger

The published `airship-preview-34095a2` JAR is immutable and still contains villager placeholders.
The current working code uses vanilla mannequins with **three original 64×64 skins**:
`assets/mmorpg/textures/entity/airship/{captain,mara,quartermaster}.png`.
They are authored replacements, **not the selected PlanetMinecraft downloads**.
Source: deterministic pixel-art generator `tools/airship/draw_crew_skins.py`.
Captain: grey beard, navy coat, brass cap; Mara: auburn hair, teal explorer coat,
scarf and satchel; Bram: ochre workwear, leather apron, brass goggles. All use wide arms.
No Steve / Alex / Efe defaults remain in new unconfigured crews. Existing per-server
resource overrides are still respected. The GUI projects real rotating cube faces
and a separate enlarged outer layer, using exactly the NPC texture.

Captain audio: eight newly generated Russian spoken-word clips using the user's
previously selected mature voice. Mara and Bram have **no generated voice**.
The OGG files are mono 48 kHz Vorbis; measured durations are in `audio/captain-durations.json`.
Audition MP3s in `audio/` are historical references, not the in-game sound resources.

## Selected community skin candidates (not bundled)

| Role | Candidate / creator | Source | Download |
|---|---|---|---|
| Captain | Steampunk Captain — TacosJo; old captain, beard, glasses | https://www.planetminecraft.com/skin/steampunk-captain-2738032/ | https://www.planetminecraft.com/skin/steampunk-captain-2738032/download/file/7146365/ |
| Mara | Explorer — deasonata; female explorer, Alex model | https://www.planetminecraft.com/skin/explorer-6044259/ | https://www.planetminecraft.com/skin/explorer-6044259/download/file/17080595/ |
| Bram | Casual Steampunk Engineer — MentallyFloridian; workwear, suspenders | https://www.planetminecraft.com/skin/casual-steampunk-engineer/ | https://www.planetminecraft.com/skin/casual-steampunk-engineer/download/file/17684340/ |

Pages were reviewed on 2026-10-27. No explicit license allowing redistribution in a public mod
was found on these pages. They are candidates, not permission-cleared release assets. Downloading
for a personal resource pack is different from rehosting them in the published JAR. Preserve
creator attribution; obtain permission before distributing the pack. No thumbnails have been
mislabelled as usable skin PNGs.

For local use with permission: `pip install Pillow`, then:

```
python tools/airship/install_skins.py --game-dir /path/to/.minecraft \
  --captain captain.png --mara mara.png --quartermaster bram.png
```

Enable the resulting resource pack on each client, and copy the generated
`config/mmorpg/airship-crew.json` to the server. Summon a new crew after configuring it.
Existing villagers are not automatically replaced; back up the world before testing.
The current built-in crew uses wide arms. Missing config uses the original bundled skins.
A slim community skin needs a matching model override before it can be used faithfully.

## Winged Sailor

- Build: https://www.planetminecraft.com/project/large-fantasy-airship-the-winged-sailor/
- Author: Chiisai_Khoahri.
- Credited tail-fin reference: Captain_JEK, https://www.planetminecraft.com/project/steampunk-airship-4142376/
- Schematic endpoint: https://www.planetminecraft.com/project/large-fantasy-airship-the-winged-sailor/download/schematic/
- Author provides a separate world zip via Google Drive, linked from the source page.
- Page includes captain's office, navigation room, storage, garden, reception and mess hall.

The inspected schematic endpoint returned the project page to the page-fetch tool, not a binary
schematic. That earlier download blocker was subsequently resolved by the user upload described below.
The procedural `/airship summon` ship remains separate from the imported `/airship winged` ship.

The author's replies permit specific users' private-server use with attribution. Those replies
are not an explicit license to redistribute the build inside a public mod JAR. Before bundling,
obtain appropriate permission, including the credited derivative part if necessary.

Original intended placements (subsequently measured below): captain in the office,
Mara in navigation, Bram at storage/reception. Actual numeric positions must be chosen after
import and checked for floor, headroom, access to the lift and interaction reach.

## User-supplied schematic import (2026-10-27)

The user subsequently uploaded `airshipfix.txt` and explicitly requested renaming it to
`.schem`. It is preserved byte-for-byte at `import/airshipfix.schem` (SHA-256 in
`import/manifest.json`). It is a Sponge v2 schematic, 117×100×89, with 20,279 non-air
blocks and 1,000 used non-air palette states. The download blocker above is resolved
by that upload; upstream redistribution permission remains unverified.

`tools/airship/convert_schematic.py` produces the vanilla structure
`data/mmorpg/structure/winged_sailor.nbt`. All 657 source block-entity NBT records
are omitted (inventories, sign text, custom container data), and no entities are copied.
Block names, directional properties, slabs and stair shapes are retained. Vanilla's
structure manager performs the old DataVersion upgrade when loading it.

Operator-only **`/airship winged`** previews the imported ship in a new test world or
on a character without an existing mooring. It never overwrites the old ship. This
is deliberately separate from `/airship summon`, and is NOT in the previously
published `airship-preview-34095a2` jar. Full-volume clearance and loaded-chunk/border/
build-height checks precede placement. Increase render distance and let chunks load.

Crew feet in schematic-local coordinates are checked against the source palette:
captain `(58,63,67)`, Mara beside the chart table `(59,56,50)`, Bram on the lower
furnished deck `(56,51,61)`; lift arrival `(57,51,38)`. Actual floor and headroom are
checked again in-game after data fixing, before spawning the crew. Room names are
provisional until visual inspection; these are measured coordinates, not guesses
from the original web screenshots.
