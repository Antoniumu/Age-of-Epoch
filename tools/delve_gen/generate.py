"""Finite, four-level overworld expeditions. Deterministic 26.3 NBT.
Run from anywhere: python3 tools/delve_gen/generate.py
"""
from pathlib import Path
import sys
import json
import random
from collections import deque

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools/tower_gen'))
from nbt_writer import Structure, Compound, Int, String

DATA = ROOT / 'src/main/resources/data/world_of_exile'
N, CELL, FLOORS, STEP, HEIGHT = 4, 12, 4, 10, 44
WIDTH = N * CELL + 1
START = (1, 1)
THEMES = [
    dict(id='crypt', wall='stone_bricks', floor='polished_andesite', accent='chiseled_stone_bricks',
         biomes=['plains','sunflower_plains','forest','birch_forest','old_growth_birch_forest','flower_forest',
                 'dark_forest','pale_garden','taiga','old_growth_pine_taiga','old_growth_spruce_taiga',
                 'meadow','cherry_grove','windswept_hills','windswept_forest','windswept_gravelly_hills']),
    dict(id='frost', wall='deepslate_bricks', floor='polished_deepslate', accent='blue_ice',
         biomes=['snowy_plains','ice_spikes','snowy_taiga','snowy_beach','grove','snowy_slopes','frozen_peaks','jagged_peaks']),
    dict(id='sands', wall='cut_sandstone', floor='smooth_sandstone', accent='chiseled_red_sandstone',
         biomes=['desert','badlands','eroded_badlands','wooded_badlands','savanna','savanna_plateau','windswept_savanna']),
    dict(id='roots', wall='mossy_stone_bricks', floor='mud_bricks', accent='jungle_planks',
         biomes=['jungle','sparse_jungle','bamboo_jungle','swamp','mangrove_swamp','mushroom_fields']),
]

def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

def center(cell):
    return cell[0] * CELL + 6, cell[1] * CELL + 6

def neighbors(cell):
    x, z = cell
    return [(a,b) for a,b in [(x-1,z),(x+1,z),(x,z-1),(x,z+1)] if 0<=a<N and 0<=b<N]

def distances(edges, start):
    graph = { (x,z): [] for x in range(N) for z in range(N) }
    for a,b in edges:
        graph[tuple(a)].append(tuple(b)); graph[tuple(b)].append(tuple(a))
    result, queue = {tuple(start): 0}, deque([tuple(start)])
    while queue:
        at = queue.popleft()
        for to in graph[at]:
            if to not in result:
                result[to] = result[at] + 1; queue.append(to)
    return result

def maze(rng, start):
    # A long guaranteed main route plus small loops, never an accidental one-room shortcut.
    for attempt in range(200):
        seen, stack, edges = {start}, [start], []
        while stack:
            at = stack[-1]; choices = [p for p in neighbors(at) if p not in seen]
            if not choices:
                stack.pop(); continue
            to = rng.choice(choices); edges.append((at,to)); seen.add(to); stack.append(to)
        dist = distances(edges, start); end = max(dist, key=dist.get)
        if dist[end] >= 10:
            break
    assert dist[end] >= 10
    possible = [(a,b) for a in seen for b in neighbors(a) if a < b and (a,b) not in edges and (b,a) not in edges]
    rng.shuffle(possible)
    loops = 0
    for edge in possible:
        if distances(edges + [edge], start)[end] >= 9:
            edges.append(edge); loops += 1
            if loops == 2: break
    return edges, end

def block(s,x,y,z,name,props=None,nbt=None):
    s.put(x,y,z, name if ':' in name else 'minecraft:'+name, props, nbt)

def connector(s,x,y,z,orientation,pool):
    block(s,x,y,z,'jigsaw',{'orientation':orientation},{
        'id':'minecraft:jigsaw','name':'world_of_exile:delve_shaft','target':'world_of_exile:delve_shaft',
        'pool':pool,'joint':'aligned','final_state':'minecraft:ladder[facing=west,waterlogged=false]'})

def chest(s,x,y,z,depth):
    block(s,x,y,z,'chest', {'facing':'north','waterlogged':'false'}, {
        'id':'minecraft:chest','LootTable':f'mmorpg:chests/delve_{depth}'})

def marker(s, pos, theme, depth, kind, room_center, exit_pos, reward, gate=None):
    config = dict(theme=theme,depth=depth,kind=kind)
    for prefix, point in [('center',room_center),('exit',exit_pos),('reward',reward)]:
        for axis,value in zip('xyz',point): config[f'{prefix}_{axis}']=value
    config['has_gate'] = gate is not None
    if gate:
        for axis,value in zip('xyz',gate): config[f'gate_{axis}']=value
    block(s,*pos,'mmorpg:delve_marker',nbt={'id':'mmorpg:delve_marker','AgeOfEpochData':config})

def entrance(theme):
    s=Structure(9,7,9)
    s.fill(0,0,0,8,0,8,'minecraft:'+theme['floor'])
    s.outline(0,1,0,8,5,8,'minecraft:'+theme['wall'])
    s.fill(1,1,1,7,4,7,'minecraft:air')
    for x,z in [(4,0),(0,4),(8,4),(4,8)]:
        s.fill(max(0,x-1) if z in (0,8) else x,1,max(0,z-1) if x in (0,8) else z,
               min(8,x+1) if z in (0,8) else x,3,min(8,z+1) if x in (0,8) else z,'minecraft:air')
    for y in range(0,5):
        block(s,5,y,4,theme['wall']); block(s,4,y,4,'ladder',{'facing':'west','waterlogged':'false'})
    connector(s,4,0,4,'down_east',f"world_of_exile:delves/{theme['id']}/body")
    for x,z in [(1,1),(7,1),(1,7),(7,7)]:
        block(s,x,5,z,theme['accent']); block(s,x,4,z,'lantern',{'hanging':'true','waterlogged':'false'})
    return s

def dungeon(theme, variant):
    rng=random.Random(84021 + variant*103 + sum(map(ord,theme['id'])))
    s=Structure(WIDTH,HEIGHT,WIDTH)
    s.fill(0,0,0,WIDTH-1,HEIGHT-1,WIDTH-1,'minecraft:'+theme['wall'])
    firstx, firstz=center(START); shaftx=firstx+3
    # Entrance is placed with its bottom connector adjacent to body y=HEIGHT-1.
    exit_pos=(shaftx-3,HEIGHT+1,firstz-3)
    start=START; floors=[]; descents=[]
    for floor in range(FLOORS):
        y=(FLOORS-1-floor)*STEP+2; depth=floor+1
        edges,end=maze(rng,start)
        cells=[(x,z) for x in range(N) for z in range(N)]
        side=[p for p in cells if p not in (start,end)]; rng.shuffle(side)
        treasures=set(side[:2])
        kinds={p: ('rest' if p==start else ('final' if floor==FLOORS-1 else 'warden') if p==end else 'treasure' if p in treasures else 'combat') for p in cells}
        for p in cells:
            cx,cz=center(p)
            s.fill(cx-4,y+1,cz-4,cx+4,y+6,cz+4,'minecraft:air')
            s.fill(cx-4,y,cz-4,cx+4,y,cz+4,'minecraft:'+theme['floor'])
            for dx,dz in [(-4,-4),(-4,4),(4,-4),(4,4)]:
                for h in range(1,6): block(s,cx+dx,y+h,cz+dz,theme['accent'])
            for dx,dz in [(-3,3),(3,3)]: block(s,cx+dx,y+5,cz+dz,'lantern',{'hanging':'true','waterlogged':'false'})
            # Four furnishing styles; central lanes and spawn pads stay unobstructed.
            style=rng.randrange(4)
            deco=['bookshelf','chiseled_stone_bricks','moss_block','barrel'][style]
            for dx in [-2,0,2]: block(s,cx+dx,y+1,cz+4,deco)
            if kinds[p]=='treasure':
                chest(s,cx,y+1,cz,depth)
            else:
                reward=(cx+3,y+1,cz-3)
                block(s,*reward,'mmorpg:delve_seal')
                gate=(cx+3,y,cz) if p==end and floor< FLOORS-1 else None
                marker(s,(cx-3,y+1,cz-3),theme['id'],depth,kinds[p],(cx,y+1,cz),exit_pos,reward,gate)
        for a,b in edges:
            ax,az=center(a); bx,bz=center(b)
            if ax==bx:
                s.fill(ax-1,y+1,min(az,bz),ax+1,y+3,max(az,bz),'minecraft:air')
                s.fill(ax-1,y,min(az,bz),ax+1,y,max(az,bz),'minecraft:'+theme['floor'])
            else:
                s.fill(min(ax,bx),y+1,az-1,max(ax,bx),y+3,az+1,'minecraft:air')
                s.fill(min(ax,bx),y,az-1,max(ax,bx),y,az+1,'minecraft:'+theme['floor'])
        floors.append(dict(depth=depth,y=y,start=start,end=end,edges=edges,
                           rooms=[dict(cell=p,kind=kinds[p]) for p in cells],main_route_edges=distances(edges,start)[end]))
        if floor<FLOORS-1: descents.append((*center(end),y))
        start=end
    # Vertical corridors are carved last, after both floors and their roofs.
    for cx,cz,y in descents:
        for h in range(y-STEP+1,y+3):
            block(s,cx+4,h,cz,theme['wall'])
            block(s,cx+3,h,cz,'ladder',{'facing':'west','waterlogged':'false'})
        block(s,cx+3,y,cz,'mmorpg:delve_seal')
    s.fill(0,40,0,WIDTH-1,HEIGHT-1,WIDTH-1,'minecraft:structure_void')
    for h in range((FLOORS-1)*STEP+3,HEIGHT):
        if h >= 40:
            s.fill(shaftx-1,h,firstz-1,shaftx+1,h,firstz+1,'minecraft:'+theme['wall'])
        block(s,shaftx+1,h,firstz,theme['wall'])
        block(s,shaftx,h,firstz,'ladder',{'facing':'west','waterlogged':'false'})
    connector(s,shaftx,HEIGHT-1,firstz,'up_east','minecraft:empty')
    return s,dict(theme=theme['id'],variant=variant,exit=exit_pos,shaft=[shaftx,HEIGHT-1,firstz],floors=floors)

def pool_element(location):
    return dict(weight=1,element=dict(element_type='minecraft:single_pool_element',location=location,
                                      processors='world_of_exile:delve_processor',projection='rigid',
                                      override_liquid_settings='ignore_waterlogging'))

def main():
    manifest=[]
    for index,theme in enumerate(THEMES):
        tid=theme['id'];base=f'world_of_exile:delves/{tid}';directory=DATA/'structure/delves'/tid
        directory.mkdir(parents=True,exist_ok=True)
        entrance(theme).save(directory/'entrance.nbt')
        for variant in range(3):
            s,plan=dungeon(theme,variant); s.save(directory/f'body{variant}.nbt'); manifest.append(plan)
        for name,locations in [('entrance',[f'{base}/entrance']),('body',[f'{base}/body{v}' for v in range(3)])]:
            write_json(DATA/'worldgen/template_pool/delves'/tid/f'{name}.json',dict(fallback='minecraft:empty',elements=[pool_element(loc) for loc in locations]))
        write_json(DATA/'worldgen/structure'/f'delve_{tid}.json',dict(type='minecraft:jigsaw',biomes=['minecraft:'+b for b in theme['biomes']],
                   step='underground_structures',spawn_overrides={},terrain_adaptation='none',start_pool=f'{base}/entrance',size=1,
                   start_height=dict(absolute=0),project_start_to_heightmap='WORLD_SURFACE_WG',use_expansion_hack=False,
                   max_distance_from_center=80))
        write_json(DATA/'worldgen/structure_set'/f'delve_{tid}.json',dict(structures=[dict(structure=f'world_of_exile:delve_{tid}',weight=1)],
                   placement=dict(type='minecraft:random_spread',spacing=48,separation=16,salt=907710+index)))
    write_json(DATA/'worldgen/processor_list/delve_processor.json',dict(processors=[dict(processor_type='world_of_exile:delve_processor')]))
    # Machine-readable generated topology, compact to keep code review focused on logic.
    (ROOT/'tools/delve_gen/layouts.json').write_text(
        '[\n' + ',\n'.join(json.dumps(plan, ensure_ascii=False, separators=(',', ':')) for plan in manifest) + '\n]\n',
        encoding='utf-8')
    for depth in range(1,5):
        items=[('bread',8+depth*2),('torch',16),('iron_ingot',3+depth*2),('emerald',2+depth*2),('golden_apple',1 if depth<4 else 3)]
        if depth>=3: items.append(('diamond',depth-1))
        pools=[dict(rolls=1,entries=[dict(type='minecraft:item',name='minecraft:'+item,functions=[dict(function='minecraft:set_count',count=count)])]) for item,count in items]
        write_json(ROOT/f'src/main/resources/data/mmorpg/loot_table/chests/delve_{depth}.json',dict(type='minecraft:chest',pools=pools))
    for name,items in [('delve_supplies',[('bread',3),('torch',4),('iron_nugget',6)]),
                       ('delve_rest',[('cooked_beef',12),('torch',16),('golden_apple',1)])]:
        write_json(ROOT/f'src/main/resources/data/mmorpg/loot_table/chests/{name}.json',dict(type='minecraft:chest',pools=[
            dict(rolls=1,entries=[dict(type='minecraft:item',name='minecraft:'+item,functions=[dict(function='minecraft:set_count',count=count)])]) for item,count in items]))
    print(f'Generated {len(manifest)} expeditions, {len(manifest)*FLOORS*N*N} rooms, 4 entrances')

if __name__=='__main__': main()
