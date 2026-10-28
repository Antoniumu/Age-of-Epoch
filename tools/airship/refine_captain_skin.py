"""Reproducible wide-arm repaint of the bundled, GPL-attributed captain adaptation.
Keeps mannequin geometry/UUID/voice; paints explicit vanilla base UVs and transparent overlays.
Run after apply_approved_skins.py to retain this revision when regenerating the pack.
"""
from pathlib import Path
from PIL import Image, ImageDraw
root=Path(__file__).resolve().parents[2]
path=root/'src/main/resources/assets/mmorpg/textures/entity/airship/captain.png'
im=Image.open(path).convert('RGBA');d=ImageDraw.Draw(im)
navy='#293e51';light='#395367';gold='#c6a061';skin='#c59c7e';shadow='#aa7e66';grey='#969c9b';hair='#b7bab2';boot='#343a40'
# Erase all accessory layers. Never make a base face transparent to fake slim arms.
for box in [(32,0,63,15),(0,32,63,47),(0,48,15,63),(48,48,63,63)]:d.rectangle(box,fill=(0,0,0,0))
# Full opaque base UV islands (unused cells inside these islands are opaque too).
for box,color in [((0,0,31,15),grey),((16,16,39,31),navy),((40,16,55,31),navy),((0,16,15,31),light),((16,48,31,63),light),((32,48,47,63),navy)]:d.rectangle(box,fill=color)
# Warm, weathered face, grey temples, compact moustache and beard.
d.rectangle((8,8,15,14),fill=skin);d.line((8,9,8,13),fill=shadow);d.line((15,9,15,13),fill=shadow)
d.rectangle((8,8,15,9),fill=hair);d.point((8,10),fill=hair);d.point((15,10),fill=hair)
for x in (9,13):
    d.line((x,10,x+1,10),fill='#6f797b');d.point((x,11),fill='#e4dfd2');d.point((x+1,11),fill='#354c59')
d.line((11,12,12,12),fill=shadow);d.rectangle((9,13,14,15),fill=grey)
d.line((10,13,13,13),fill=hair);d.line((11,14,12,14),fill='#626d72');d.line((10,15,13,15),fill=hair)
# Cap on the actual hat UV: dark navy crown and restrained brass band/badge.
d.rectangle((40,0,47,7),fill=navy)
for x in (32,40,48,56):
    d.rectangle((x,8,x+7,9),fill=navy);d.line((x,10,x+7,10),fill=gold)
d.rectangle((43,8,44,9),fill='#e6cb86')
# Ivory shirt and vest. Asymmetric watch-chain distinguishes the captain from a generic uniform.
d.rectangle((23,20,24,26),fill='#ded8c3');d.line((22,21,21,24),fill=gold);d.line((25,21,26,24),fill=gold)
d.line((21,25,21,30),fill=gold);d.line((26,25,26,30),fill=gold)
for y in (26,28,30):d.point((24,y),fill=gold)
d.line((25,27,27,28),fill='#e3c47a');d.point((27,29),fill=gold)
d.line((20,31,27,31),fill='#162a3a');d.rectangle((32,20,39,21),fill=light)
# Shoulder braid, cuffs and bare hands on all four arm sides; no black filler at seams.
for x,y in ((40,20),(32,52)):
    d.rectangle((x,y+9,x+15,y+11),fill=skin)
    d.line((x,y+8,x+15,y+8),fill=gold)
    d.line((x+4,y,x+7,y),fill=gold)
    d.line((x+4,y+1,x+7,y+1),fill=light)
# Trousers and boots, separately painted on both standard wide legs.
for x,y in ((0,20),(16,52)):
    d.rectangle((x,y+8,x+15,y+11),fill=boot)
    d.line((x+4,y,x+4,y+7),fill='#8b8c80')
    d.line((x+4,y+8,x+7,y+8),fill='#616971')
im.save(path)
# Portrait and web viewer reference the exact same texture as the real mannequin.
im.save(root/'docs/airship/crew-preview/skins/captain.png')
