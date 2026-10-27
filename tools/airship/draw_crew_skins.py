#!/usr/bin/env python3
"""Original Age of Epoch crew artwork, 64x64 Java skins. No third-party pixels.
Deterministic pixel-art construction with wrapped faces, shaded seams and outer layers.
"""
from pathlib import Path
from PIL import Image, ImageDraw
import random

OUT=Path('src/main/resources/assets/mmorpg/textures/entity/airship')
PARTS={
 'head':[(8,0,8,8),(16,0,8,8),(0,8,8,8),(8,8,8,8),(16,8,8,8),(24,8,8,8)],
 'body':[(20,16,8,4),(28,16,8,4),(16,20,4,12),(20,20,8,12),(28,20,4,12),(32,20,8,12)],
 'arm_r':[(44,16,4,4),(48,16,4,4),(40,20,4,12),(44,20,4,12),(48,20,4,12),(52,20,4,12)],
 'arm_l':[(36,48,4,4),(40,48,4,4),(32,52,4,12),(36,52,4,12),(40,52,4,12),(44,52,4,12)],
 'leg_r':[(4,16,4,4),(8,16,4,4),(0,20,4,12),(4,20,4,12),(8,20,4,12),(12,20,4,12)],
 'leg_l':[(20,48,4,4),(24,48,4,4),(16,52,4,12),(20,52,4,12),(24,52,4,12),(28,52,4,12)]}

def shade(c,n):return tuple(max(0,min(255,v+n)) for v in c)+(255,)
def skin(role):
 rng=random.Random(role);im=Image.new('RGBA',(64,64));d=ImageDraw.Draw(im)
 navy=(33,51,64);teal=(42,84,79);leather=(80,55,40);gold=(174,140,74)
 coat={'captain':navy,'mara':teal,'quartermaster':(112,79,42)}[role]
 flesh={'captain':(173,130,103),'mara':(209,162,129),'quartermaster':(184,140,108)}[role]
 hair={'captain':(133,132,121),'mara':(126,57,32),'quartermaster':(75,44,28)}[role]
 for part,faces in PARTS.items():
  base=flesh if part=='head' else (coat if part in ('body','arm_l','arm_r') else (44,46,44))
  for fi,(x,y,w,h) in enumerate(faces):
   for yy in range(h):
    for xx in range(w):
     color=base
     if part.startswith('arm') and yy>=9:color=flesh
     if part.startswith('leg') and yy>=8:color=(37,29,25)
     tone=(2,-10,-8,0,-12,-5)[fi]+rng.choice([-3,-1,0,0,1,2])
     im.putpixel((x+xx,y+yy),shade(color,tone))
 # Face and wrapped hair.
 for x,y in [(0,8),(16,8),(24,8)]:
  for yy in range(8):
   for xx in range(8):
    if yy<3 or x==24:im.putpixel((x+xx,y+yy),shade(hair,rng.randrange(-8,7)))
 d.rectangle((8,0,15,7),fill=shade(hair,0))
 d.rectangle((8,8,15,9),fill=shade(hair,1))
 d.point((10,11),fill=(225,219,197,255));d.point((13,11),fill=(225,219,197,255))
 eyes=(54,76,66,255) if role=='mara' else (43,52,51,255)
 d.point((10,11),fill=eyes);d.point((13,11),fill=eyes)
 d.point((11,12),fill=shade(flesh,-19));d.point((12,12),fill=shade(flesh,-12))
 d.rectangle((11,14,12,14),fill=shade(flesh,-35))
 if role!='mara':
  for yy in range(13,16):
   for xx in range(8,16):
    if yy==13 and xx in (10,11,12,13):continue
    im.putpixel((xx,yy),shade(hair,rng.randrange(-9,10)))
  d.rectangle((11,14,12,14),fill=(79,57,44,255))
 # Seams, waistbelt and brass buckle, wrapped around torso.
 for x,y,w,h in PARTS['body'][2:]:
  d.rectangle((x,y+9,x+w-1,y+10),fill=shade(leather,-4))
 d.rectangle((23,29,24,30),fill=shade(gold,10));d.point((23,29),fill=shade(gold,35))
 for x in (21,26):d.line((x,21,x,28),fill=shade(gold,-10))
 d.rectangle((23,20,24,26),fill=(180,173,147,255))
 for y in (23,26,28):d.point((25,y),fill=shade(gold,20))
 if role=='captain':
  # Naval cap: second-layer crown, visor and insignia. Beard stays visible.
  for x,y,w,h in PARTS['head']:
   for yy in range(h):
    for xx in range(w):
     if y==0 or yy<3:im.putpixel((x+32+xx,y+yy),shade(navy,4 if yy==0 else -3))
  d.rectangle((40,10,47,10),fill=shade(gold,3));d.rectangle((43,8,44,9),fill=shade(gold,25))
  d.line((9,10,11,10),fill=shade(hair,-25));d.line((13,10,14,10),fill=shade(hair,-25))
  for x,y in ((44,20),(36,52)):
   d.line((x,y+1,x+3,y+1),fill=shade(gold,2));d.point((x+1,y+2),fill=shade(gold,25))
 elif role=='mara':
  # Long auburn hair is a real outer head layer, not a painted flat portrait.
  for x,y in ((32,8),(48,8),(56,8)):
   d.rectangle((x,y,x+7,y+7),fill=shade(hair,-3))
   for xx in range(0,8,3):d.line((x+xx,y,x+xx,y+7),fill=shade(hair,13))
  d.rectangle((40,0,47,7),fill=shade(hair,0));d.rectangle((40,8,47,9),fill=shade(hair,3))
  d.point((40,10),fill=shade(hair,-3));d.point((47,10),fill=shade(hair,-3))
  d.rectangle((20,20,27,21),fill=shade(gold,-1));d.line((25,22,25,25),fill=shade(gold,-8))
  for k in range(8):d.point((20+k,22+k//2),fill=shade(leather,7))
  d.rectangle((29,26,31,30),fill=shade(leather,5));d.point((30,27),fill=shade(gold,10))
  # Model is classic-width for correctly wrapped hand pixels, same proportions as other crew.
 elif role=='quartermaster':
  for x,y in ((44,20),(36,52)):
   d.rectangle((x,y+2,x+3,y+7),fill=(157,143,111,255));d.line((x,y+7,x+3,y+7),fill=shade(leather,2))
  d.rectangle((21,23,26,28),fill=shade(leather,4));d.rectangle((22,25,25,27),fill=shade(leather,-10))
  d.point((22,24),fill=shade(gold,12));d.point((25,24),fill=shade(gold,12))
  # Leather pilot goggles on the forehead/outer layer.
  d.rectangle((40,8,47,9),fill=shade(leather,6))
  for x in (40,45):d.rectangle((x,8,x+2,10),fill=shade(gold,-5));d.point((x+1,9),fill=(100,139,139,255))
 return im

if __name__=='__main__':
 OUT.mkdir(parents=True,exist_ok=True)
 for role in ('captain','mara','quartermaster'):
  im=skin(role);im.save(OUT/(role+'.png'))
  assert im.size==(64,64)
 print('Original crew skins written to',OUT)
