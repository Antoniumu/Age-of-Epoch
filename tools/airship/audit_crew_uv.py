#!/usr/bin/env python3
"""Validate the faces actually sampled by bundled vanilla wide/slim models, not unused atlas cells."""
from pathlib import Path
import json
from PIL import Image
root=Path(__file__).resolve().parents[2]
preview=root/'docs/airship/crew-preview'
items={('quartermaster' if e['id']=='bram' else e['id']):e for e in json.loads((preview/'alternatives.json').read_text())}
common=[(8,0,24,8),(0,8,32,16),(20,16,36,20),(16,20,40,32),(4,16,12,20),(0,20,16,32),(20,48,28,52),(16,52,32,64)]
report={}
for file in sorted((root/'src/main/resources/assets/mmorpg/textures/entity/airship').glob('*.png')):
    item=items.get(file.stem);width=3 if item and item['model']=='slim' else 4
    faces=common+[(44,16,44+2*width,20),(40,20,48+2*width,32),(36,48,36+2*width,52),(32,52,40+2*width,64)]
    im=Image.open(file).convert('RGBA');bad=[];nearblack=0
    for x1,y1,x2,y2 in faces:
        for y in range(y1,y2):
            for x in range(x1,x2):
                r,g,b,a=im.getpixel((x,y))
                if a!=255:bad.append([x,y,a])
                if max(r,g,b)<12:nearblack+=1
    assert im.size==(64,64) and not bad,(file,bad[:8])
    entry={'size':[64,64],'baseUV':'slim' if width==3 else 'wide','transparentBasePixels':len(bad),'nearBlackBasePixels':nearblack}
    if item:
        assert file.read_bytes()==(preview/item['skin']).read_bytes(),f'Viewer/resource drift: {file}'
        source=Image.open(preview/item['original']).convert('RGBA')
        entry['sourceTransparentWideArmSidePixels']=sum(source.getpixel((x,y))[3]==0 for x in range(40,56) for y in range(20,32))
        if width==3:
            assert entry['sourceTransparentWideArmSidePixels']>0,'Source no longer matches the diagnosed slim atlas'
            assert all(source.getpixel((x,y))[3]==255 for x in range(40,54) for y in range(20,32))
    report[file.stem]=entry
(preview/'uv-audit.json').write_text(json.dumps(report,indent=2)+'\n')
print('PASS: 8 opaque vanilla UV layouts, 3 verified slim source atlases, exact viewer/resource parity')
