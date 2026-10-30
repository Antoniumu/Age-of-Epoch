#!/usr/bin/env python3
"""Install user-provided/permission-cleared skins, never rehost community art in the mod jar.
python install_skins.py --game-dir /path/to/.minecraft --captain captain.png --mara mara.png --quartermaster bram.png
Requires Pillow; accepts modern 64x64 and legacy 64x32 skins. Install on each client;
copy config/mmorpg/airship-crew.json to the server. Enable the resource pack in-game.
"""
import argparse
import json
from pathlib import Path
from PIL import Image


def convert(image):
    if image.size == (64, 64):
        return image.convert('RGBA')
    if image.size != (64, 32):
        raise ValueError('Expected 64x64 or legacy 64x32 skin, not a preview image')
    # Legacy skins reused right limbs, so mirror each cuboid face into the modern left-limb UVs.
    old=image.convert('RGBA')
    result=Image.new('RGBA',(64,64)); result.paste(old,(0,0))
    for sx,sy,dx,dy in [(0,16,16,48),(40,16,32,48)]:
        for ux,uy,w,h,vx,vy in [(4,0,4,4,8,0),(8,0,4,4,4,0),(0,4,4,12,8,4),
                                  (4,4,4,12,4,4),(8,4,4,12,0,4),(12,4,4,12,12,4)]:
            face=old.crop((sx+ux,sy+uy,sx+ux+w,sy+uy+h)).transpose(Image.Transpose.FLIP_LEFT_RIGHT)
            result.paste(face,(dx+vx,dy+vy))
    return result


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--game-dir',type=Path,required=True)
    for role in ('captain','mara','quartermaster'): parser.add_argument('--'+role,type=Path,required=True)
    args=parser.parse_args()
    # Validate every input before writing anything.
    skins={role:convert(Image.open(getattr(args,role))) for role in ('captain','mara','quartermaster')}
    pack=args.game_dir/'resourcepacks'/'aoe-airship-crew'
    textures=pack/'assets'/'mmorpg'/'textures'/'entity'/'airship'
    textures.mkdir(parents=True,exist_ok=True)
    for role,skin in skins.items(): skin.save(textures/(role+'.png'))
    (pack/'pack.mcmeta').write_text(json.dumps({'pack':{'pack_format':97,'min_format':82,'max_format':121,
        'description':'Age of Epoch: locally installed crew skins'}},indent=2))
    config=args.game_dir/'config'/'mmorpg'; config.mkdir(parents=True,exist_ok=True)
    (config/'airship-crew.json').write_text(json.dumps({role:'mmorpg:entity/airship/'+role for role in skins},indent=2))
    print('Enable resource pack:',pack)
    print('Copy config/mmorpg/airship-crew.json to your server BEFORE summoning a new crew.')
    print('The released preview jar predates this feature; use the subsequent mannequin build.')

if __name__=='__main__': main()
