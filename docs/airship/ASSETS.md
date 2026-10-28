# Crew and Winged Sailor asset ledger

The published `airship-preview-34095a2` JAR is immutable and still contains villager placeholders.
## Approved crew assets applied (2026-10-28)

The seven MineColonies alternatives shown in `crew-preview/` were approved by the user and
are now applied to the seven humanoid NPCs. Captain, Mara, Bram, Oren, Veya, Selin, Lysander
use the exact approved crop, with legacy left limbs mirrored into a standard 64×64 atlas.
All use wide arms. Oren's native scale attribute is 1.6 on all axes.

- Original atlases: `crew-preview/originals/`.
- Pinned sources, author/project credit and hashes: `crew-preview/alternatives.json`.
- Reproduction: `tools/airship/apply_approved_skins.py` (Pillow).
- Source project: MineColonies contributors, GPL-3.0 repository license. Full license,
  attribution and pinned source records are also bundled at `META-INF/airship-crew/`.
- Custom MineColonies hats/equipment/geometry are not included; this is the approved
  standard player-model adaptation, not a faithful copy of their custom NPC model.
- Tik is an original procedural brass bird, not an online humanoid skin. A tagged native
  parrot supplies the server-side body/hitbox; its synchronized name selects a separate
  client bird mesh. Ordinary parrots retain vanilla rendering. His portrait is an original
  brass-head atlas. The source geometry is in `AirshipTikRenderer.java`.

Earlier original generated skins can still be reproduced with `draw_crew_skins.py`, but are
no longer the default assets. Existing resource/config overrides remain respected.
The blocked PlanetMinecraft candidates below are **not** included.

## Applied audio

Captain: eight retained clips, voice-01. Crew: sixteen newly generated and integrated mono
Vorbis clips, 285.24 seconds total: Mara voice-03, Bram voice-05, Oren voice-08,
Veya voice-10, Selin voice-12, Lysander voice-14, Tik voice-18. The rejected Bram voice-04
recordings are not registered or committed. Runtime cue text, measured durations and selected
voice IDs: `assets/mmorpg/airship/crew-voice.json`. Captain durations remain in
`audio/captain-durations.json`.

All current `AirshipStory.dialogue` branches now have audio, including Mara/Bram idle and
not-ready responses. This does not mean every unrelated UI notification, favor result or
future quest is voiced. Captions bind to actual clip playback/completion, not guessed word
alignment. Long introductions grow the bottom strip only as needed to avoid cutting off text.

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
The current built-in crew uses wide arms. Missing config uses the approved bundled adaptations.
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

## Headless derived template update (2026-10-28)

The original user schematic remains byte-identical. The converter now excludes 46 player_head
and 2 player_wall_head blocks whose profile NBT was not retained. The current derived template
contains 20,231 blocks and 987 used palette states. Current conversion records:
`import/manifest.json` and `import/conversion.json`. Earlier counts above describe the original
conversion. Existing placed ships are not retroactively edited by merely replacing the template.
