"""Reproduce approved MineColonies legacy-UV adaptations; retain original sources/license."""
from pathlib import Path
import json,shutil,subprocess,sys
from PIL import Image
root=Path(__file__).resolve().parents[2]
preview=root/'docs/airship/crew-preview'
out=root/'src/main/resources/assets/mmorpg/textures/entity/airship'
for item in json.loads((preview/'alternatives.json').read_text()):
    old=Image.open(preview/item['original']).convert('RGBA').crop((0,0,64,32))
    skin=Image.new('RGBA',(64,64));skin.paste(old,(0,0))
    copies=[(4,16,4,4,20,48),(8,16,4,4,24,48),(0,20,4,12,24,52),(4,20,4,12,20,52),(8,20,4,12,16,52),(12,20,4,12,28,52),(44,16,4,4,36,48),(48,16,4,4,40,48),(40,20,4,12,40,52),(44,20,4,12,36,52),(48,20,4,12,32,52),(52,20,4,12,44,52)]
    if item['id'] in {'mara','veya','selin'}:
        # Source atlas has a 3px arm (14px side strip), not a 4px arm (16px strip).
        # Copy each slim face independently; forcing a wide model exposes transparent RGB=0 as stripes.
        copies=copies[:6]+[(44,16,3,4,36,48),(47,16,3,4,39,48),(40,20,4,12,39,52),(44,20,3,12,36,52),(47,20,4,12,32,52),(51,20,3,12,43,52)]
    for x,y,w,h,dx,dy in copies:skin.paste(old.crop((x,y,x+w,y+h)).transpose(Image.Transpose.FLIP_LEFT_RIGHT),(dx,dy))
    # Vanilla requires solid base layers; transparent skin overlay remains transparent.
    for x1,y1,x2,y2 in [(0,0,32,16),(0,16,64,32),(16,48,48,64)]:
        for x in range(x1,x2):
            for y in range(y1,y2):
                r,g,b,a=skin.getpixel((x,y));skin.putpixel((x,y),(r,g,b,255))
    name='quartermaster' if item['id']=='bram' else item['id']
    skin.save(out/(name+'.png'))
    skin.save(preview/item['skin'])
# Tik's portrait represents the original brass head, not a humanoid downloaded skin.
from PIL import ImageDraw
skin=Image.new('RGBA',(64,64));d=ImageDraw.Draw(skin)
for r in [(0,0,32,16),(0,16,64,32),(16,48,48,64)]:d.rectangle((r[0],r[1],r[2]-1,r[3]-1),fill='#b98a45')
d.rectangle((8,8,15,15),fill='#b98a45');d.rectangle((9,10,10,11),fill='#ffb354');d.rectangle((13,10,14,11),fill='#ffb354');d.rectangle((11,12,12,14),fill='#e7bd6b');d.line((10,8,13,8),fill='#69a69a');skin.save(out/'tik.png')
legal=root/'src/main/resources/META-INF/airship-crew';legal.mkdir(parents=True,exist_ok=True)
shutil.copy(preview/'LICENSE-MineColonies.txt',legal/'LICENSE-MineColonies.txt')
shutil.copy(preview/'alternatives.json',legal/'sources.json')
(legal/'NOTICE.txt').write_text('Seven approved crew textures are adaptations of MineColonies contributor assets, GPL-3.0 repository license. See sources.json for pinned original links and hashes. Changes: standard legacy UV crop, mirrored left limbs, opaque vanilla base layers, no custom MineColonies equipment/geometry. Original PNG sources and conversion script are in the Age-of-Epoch repository under docs/airship/crew-preview/originals and tools/airship/apply_approved_skins.py. Tik is an original brass bird model, not a third-party skin.\n')

# Preserve the new captain appearance on every complete pack regeneration.
subprocess.run([sys.executable,str(root/'tools/airship/refine_captain_skin.py')],check=True)
with (legal/'NOTICE.txt').open('a') as notice:
    notice.write('Revision 2: Captain navy/grey officer repaint (refine_captain_skin.py); Mara, Veya and Selin retain source 3px arms with correct slim left-arm UV conversion. Earlier wide conversion exposed transparent source pixels as black stripes. Original GPL attribution remains applicable.\n')

# Keep each crew member's authored repaint when regenerating from pinned originals.
subprocess.run([sys.executable,str(root/'tools/airship/refine_crew_skins.py')],check=True)
with (legal/'NOTICE.txt').open('a') as notice:
    notice.write('Revision 3: individual outfits, hair accents, accessories and back details for Mara, Bram, Oren, Veya, Selin and Lysander; deterministic refine_crew_skins.py repaint based on the attributed MineColonies adaptations. Original sources and GPL notice retained.\nRevision 3: Tik brass bird gets verdigris chest ornament and crest, custom authored geometry; not a third-party skin.\n')
