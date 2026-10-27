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
