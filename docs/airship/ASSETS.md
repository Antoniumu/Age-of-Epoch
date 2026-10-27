# Crew and Winged Sailor asset ledger

The published `airship-preview-34095a2` JAR is immutable and still contains villager placeholders.
The subsequent working code uses vanilla mannequins and resource-pack skin IDs, no account lookup.
Built-in Steve / Alex / Efe are temporary fallback skins; NOT the selected final crew art.
The face and hat in the quest card use the same configured skin as the NPC. The head subtly
turns/shifts toward the cursor, not independently moving pupils. This is a 2D skin-UV view, not
full 3D head rendering.

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
Mara uses slim arms, other roles wide arms. Missing config uses vanilla skins.

## Winged Sailor

- Build: https://www.planetminecraft.com/project/large-fantasy-airship-the-winged-sailor/
- Author: Chiisai_Khoahri.
- Credited tail-fin reference: Captain_JEK, https://www.planetminecraft.com/project/steampunk-airship-4142376/
- Schematic endpoint: https://www.planetminecraft.com/project/large-fantasy-airship-the-winged-sailor/download/schematic/
- Author provides a separate world zip via Google Drive, linked from the source page.
- Page includes captain's office, navigation room, storage, garden, reception and mess hall.

The inspected schematic endpoint returned the project page to the page-fetch tool, not a binary
schematic. The actual schematic/NBT has NOT been imported or measured. Do not claim that the
procedural ship is Winged Sailor, or guess walkable NPC coordinates from screenshots.

The author's replies permit specific users' private-server use with attribution. Those replies
are not an explicit license to redistribute the build inside a public mod JAR. Before bundling,
obtain appropriate permission, including the credited derivative part if necessary.

Intended placements, pending the actual file and collision checks: captain in the office,
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
