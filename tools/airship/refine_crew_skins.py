"""Deterministic character-specific vanilla-skin repaints of the attributed MineColonies bases.
Called after the legacy-UV conversion. Retains face features, slim/wide UV geometry and voices.
"""
from pathlib import Path
from PIL import Image, ImageDraw

ROOT=Path(__file__).resolve().parents[2]
ASSETS=ROOT/'src/main/resources/assets/mmorpg/textures/entity/airship'
PREVIEW=ROOT/'docs/airship/crew-preview/skins'
# coat, trim, shirt, trouser, boot, hair, accent, portrait description
DESIGNS={
 'mara':('#31585d','#c99750','#e5d0a4','#72523e','#333a3a','#53382d','#d3ad66'),
 'quartermaster':('#83513a','#d8aa69','#dfccb0','#54443b','#373335','#7d4732','#bf7743'),
 'oren':('#4c6257','#d0a565','#ded2b6','#5b5143','#343b37','#aa8d63','#bc5945'),
 'veya':('#4e405d','#cf9a54','#d3bdaa','#3d3e4b','#292e39','#a04d35','#c35c48'),
 'selin':('#526e77','#c5d9d2','#e7e4d0','#4e6171','#363d49','#5e4c46','#76aa9d'),
 'lysander':('#713f4e','#d2a466','#e5d7bd','#484557','#353642','#493b3b','#7d9977'),
}

def darken(hexcolor,delta):
    rgb=tuple(int(hexcolor[i:i+2],16) for i in (1,3,5))
    return tuple(max(0,min(255,c+delta)) for c in rgb)+(255,)

for name,(coat,trim,shirt,trousers,boot,hair,accent) in DESIGNS.items():
    path=ASSETS/(name+'.png'); im=Image.open(path).convert('RGBA'); d=ImageDraw.Draw(im)
    # Recolour dark hair on both base and existing hat layers. Preserve eyes on each front face.
    for y in range(16):
        for x in range(64):
            r,g,b,a=im.getpixel((x,y))
            if a==255 and max(r,g,b)<85 and not ((8<=x<=15 or 40<=x<=47) and 10<=y<=12):
                im.putpixel((x,y),darken(hair,((x*3+y*5)%4)*4-6))
    d=ImageDraw.Draw(im)
    # Torso full 16x12 side strip plus top/bottom islands. Subtle pixel-scale cloth shading.
    d.rectangle((16,16,39,31),fill=coat)
    for x in range(16,40):
        for y in range(20,32):
            if (x*7+y*11)%13==0:d.point((x,y),fill=darken(coat,9))
            elif (x*5+y*3)%17==0:d.point((x,y),fill=darken(coat,-10))
    # Front 8px wide, recognisable from normal conversation distance.
    d.line((20,20,27,20),fill=trim);d.line((20,31,27,31),fill=darken(coat,-20))
    d.polygon([(23,20),(25,20),(24,25)],fill=shirt)
    # Back panel and hem provide meaningful back view, not a front-only badge.
    d.line((32,20,39,20),fill=trim);d.line((32,30,39,30),fill=darken(coat,-16))
    # Upper sleeves retain existing hands and the chosen actual 3px/4px UV faces.
    for x,y in ((40,20),(32,52)):
        end=x+(13 if name in {'mara','veya','selin'} else 15)
        d.rectangle((x,y,end,y+7),fill=coat);d.line((x,y+7,end,y+7),fill=trim)
        d.line((x,y+1,end,y+1),fill=darken(coat,10))
    for x,y in ((0,20),(16,52)):
        d.rectangle((x,y,x+15,y+7),fill=trousers);d.rectangle((x,y+8,x+15,y+11),fill=boot)
        d.line((x+4,y,x+4,y+7),fill=darken(trousers,14));d.line((x,y+8,x+15,y+8),fill=trim)
    # Different silhouettes/accents, not simple hue swaps.
    if name=='mara':
        d.line((21,20,26,29),fill=trim,width=2);d.rectangle((25,26,27,29),fill='#dccb96');d.point((26,27),fill='#596b5d')
        d.line((33,21,37,30),fill=trim,width=2)
    elif name=='quartermaster':
        d.rectangle((21,24,26,30),fill='#5a3930');d.line((21,24,26,24),fill=trim)
        d.line((22,25,22,29),fill='#ad7351');d.point((25,28),fill='#e0bc7a')
        d.line((33,21,33,30),fill='#5a3930');d.line((38,21,38,30),fill='#5a3930')
    elif name=='oren':
        d.rectangle((20,20,27,22),fill=accent);d.line((21,23,23,27),fill=accent,width=2)
        d.line((32,20,39,21),fill=accent,width=2);d.line((37,22,37,27),fill=accent,width=2)
        # Mature but friendly beard; eyes remain intact.
        d.rectangle((9,13,14,15),fill=hair);d.line((10,13,13,13),fill=darken(hair,-20));d.line((11,14,12,14),fill=darken(hair,12))
    elif name=='veya':
        d.line((20,21,27,29),fill=accent,width=2);d.line((21,30,27,30),fill=trim)
        d.line((33,21,38,29),fill=accent,width=2);d.point((26,23),fill='#e8c977')
    elif name=='selin':
        d.rectangle((23,24,24,29),fill=shirt);d.rectangle((21,26,26,27),fill=shirt)
        d.rectangle((34,24,37,28),outline=trim);d.line((35,25,36,27),fill=shirt)
    elif name=='lysander':
        d.line((20,22,26,29),fill=trim);d.line((27,22,21,29),fill=trim)
        d.rectangle((23,26,24,27),fill=accent);d.point((23,22),fill='#f0d493')
        d.line((33,22,38,28),fill=trim);d.line((38,22,33,28),fill=trim)
    im.save(path)
    im.save(PREVIEW/('bram.png' if name=='quartermaster' else name+'.png'))
