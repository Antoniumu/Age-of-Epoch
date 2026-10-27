#!/usr/bin/env python3
"""Convert the supplied Sponge v2 schematic to a vanilla sparse structure.
Requires nbtlib==2.0.4. Never copies source entities, inventories or command NBT.
"""
import argparse
import collections
import gzip
import hashlib
import io
import json
from pathlib import Path
import nbtlib as n

SPOTS={'captain':[58,63,67],'mara':[59,56,50],'quartermaster':[56,51,61],'lift':[57,51,38]}
AIR={'minecraft:air','minecraft:cave_air','minecraft:void_air','minecraft:structure_void'}
FORBIDDEN={'minecraft:command_block','minecraft:chain_command_block','minecraft:repeating_command_block',
           'minecraft:structure_block','minecraft:jigsaw','minecraft:spawner','minecraft:tnt','minecraft:end_portal'}

def convert(source, output, report):
    src=n.load(source)
    if int(src['Version'])!=2: raise ValueError('Only Sponge version 2 is supported')
    w,h,l=(int(src[k]) for k in ('Width','Height','Length'))
    if max(w,h,l)>256 or w*h*l>2_000_000: raise ValueError('Unreasonable structure size')
    palette={int(v):k for k,v in src['Palette'].items()}
    data=[]; value=shift=0
    for byte in bytes(src['BlockData']):
        value|=(byte&127)<<shift
        if byte&128:
            shift+=7
            if shift>28: raise ValueError('Invalid palette VarInt')
        else:
            if value not in palette: raise ValueError('Unknown palette ID')
            data.append(value);value=shift=0
    if shift or len(data)!=w*h*l: raise ValueError('Truncated/oversized block data')
    def state(x,y,z): return palette[data[x+z*w+y*w*l]]
    for role,(x,y,z) in SPOTS.items():
        if state(x,y,z) not in AIR or state(x,y+1,z) not in AIR or 'planks' not in state(x,y-1,z):
            raise ValueError('Unsafe crew/lift spot: '+role)
    states=[]; remap={}; blocks=[]; omitted=collections.Counter()
    for index,pid in enumerate(data):
        text=palette[pid]; name=text.split('[',1)[0]
        if name in AIR: continue
        if name in FORBIDDEN: omitted[name]+=1;continue
        if pid not in remap:
            tag=n.Compound({'Name':n.String(name)})
            if '[' in text:
                props=dict(pair.split('=',1) for pair in text.split('[',1)[1].rstrip(']').split(','))
                tag['Properties']=n.Compound({k:n.String(v) for k,v in props.items()})
            remap[pid]=len(states);states.append(tag)
        blocks.append(n.Compound({'pos':n.List[n.Int]([index%w,index//(w*l),(index//w)%l]),'state':n.Int(remap[pid])}))
    root=n.File({'DataVersion':n.Int(src['DataVersion']), 'size':n.List[n.Int]([w,h,l]),
                 'palette':n.List[n.Compound](states),'blocks':n.List[n.Compound](blocks),'entities':n.List[n.Compound]([])})
    buffer=io.BytesIO();root.write(buffer)
    output.parent.mkdir(parents=True,exist_ok=True); output.write_bytes(gzip.compress(buffer.getvalue(),mtime=0))
    result={'source':str(source),'source_sha256':hashlib.sha256(source.read_bytes()).hexdigest(),
            'dimensions':[w,h,l],'blocks':len(blocks),'palette_states':len(states),'crew_feet_local':SPOTS,
            'source_block_entities_stripped':len(src.get('BlockEntities',[])),'forbidden_blocks_omitted':dict(omitted),
            'source_entities_copied':0,'note':'All block entity NBT stripped, including inventory and sign text; states retained.'}
    report.parent.mkdir(parents=True,exist_ok=True);report.write_text(json.dumps(result,indent=2)+'\n')
    return result

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('source',type=Path);p.add_argument('output',type=Path);p.add_argument('report',type=Path)
    a=p.parse_args();print(json.dumps(convert(a.source,a.output,a.report),indent=2))
