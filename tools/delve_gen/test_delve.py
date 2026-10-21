"""Offline resource / topology regressions, not an execution of Minecraft.
Requires nbtlib. Run: python3 -m unittest discover -s tools/delve_gen -v
"""
from pathlib import Path
import json
import sys
import unittest
from collections import Counter, deque
import nbtlib

sys.path.insert(0, str(Path(__file__).resolve().parent))
import generate as gen

ROOT=Path(__file__).resolve().parents[2]
DATA=ROOT/'src/main/resources/data/world_of_exile'


def unpack(path):
    n=nbtlib.load(path)
    assert int(n['DataVersion'])==5023
    palette=n['palette']
    assert all('id' in p and 'Name' not in p and 'Properties' not in p for p in palette)
    blocks={tuple(map(int,b['pos'])):(str(palette[int(b['state'])]['id']),palette[int(b['state'])].get('properties',{}),b.get('nbt')) for b in n['blocks']}
    return tuple(map(int,n['size'])),blocks


def reachable(edges,start):
    return gen.distances(edges,tuple(start))


class DelveTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.plans=json.loads((ROOT/'tools/delve_gen/layouts.json').read_text())

    def test_twelve_distinct_complete_plans(self):
        self.assertEqual(len(self.plans),12)
        signatures=set()
        for plan in self.plans:
            self.assertEqual(len(plan['floors']),4)
            expected=list(gen.START)
            for floor in plan['floors']:
                self.assertEqual(floor['start'],expected)
                self.assertEqual(len(floor['rooms']),16)
                reach=reachable(floor['edges'],floor['start'])
                self.assertEqual(len(reach),16)
                self.assertGreaterEqual(reach[tuple(floor['end'])],9)
                kinds=Counter(r['kind'] for r in floor['rooms'])
                self.assertEqual(kinds['combat'],12)
                self.assertEqual(kinds['rest'],1)
                self.assertEqual(kinds['treasure'],2)
                self.assertEqual(kinds['final']+kinds['warden'],1)
                expected=floor['end']
            signature=json.dumps([floor['edges'] for floor in plan['floors']])
            self.assertNotIn(signature,signatures);signatures.add(signature)

    def test_overworld_only_and_disjoint_biomes(self):
        biomes=set()
        for theme in gen.THEMES:
            self.assertFalse(biomes.intersection(theme['biomes']))
            biomes.update(theme['biomes'])
            self.assertFalse(set(theme['biomes']) & {'nether_wastes','crimson_forest','warped_forest','basalt_deltas','soul_sand_valley'})
            path=DATA/'worldgen/structure'/f"delve_{theme['id']}.json"
            d=json.loads(path.read_text())
            self.assertEqual(d['biomes'],['minecraft:'+b for b in theme['biomes']])
            self.assertEqual(d['size'],1)
            self.assertEqual(d['terrain_adaptation'],'none')
            self.assertEqual(d['start_height'],{'absolute':0})

    def test_pools_and_loot_exist(self):
        for path in (DATA/'worldgen/template_pool/delves').rglob('*.json'):
            pool=json.loads(path.read_text())
            for element in pool['elements']:
                e=element['element']
                self.assertEqual(e['processors'],'world_of_exile:delve_processor')
                self.assertEqual(e['projection'],'rigid')
                self.assertTrue((DATA/'structure'/(e['location'].split(':')[1]+'.nbt')).exists())
        for name in ['delve_1','delve_2','delve_3','delve_4','delve_rest','delve_supplies']:
            self.assertEqual(json.loads((ROOT/f'src/main/resources/data/mmorpg/loot_table/chests/{name}.json').read_text())['type'],'minecraft:chest')

    def test_actual_voxels_routes_ladders_markers_and_rewards(self):
        for plan in self.plans:
            with self.subTest(theme=plan['theme'],variant=plan['variant']):
                path=DATA/'structure/delves'/plan['theme']/f"body{plan['variant']}.nbt"
                size,blocks=unpack(path)
                self.assertEqual(len(blocks),size[0]*size[1]*size[2])
                markers=[(pos,b[2]['AgeOfEpochData']) for pos,b in blocks.items() if b[0]=='mmorpg:delve_marker']
                self.assertEqual(len(markers),56)
                self.assertEqual(sum(b[0]=='minecraft:jigsaw' for b in blocks.values()),1)
                self.assertEqual(sum(str(d['kind'])=='final' for _,d in markers),1)
                for pos,d in markers:
                    depth=int(d['depth'])
                    self.assertIn(depth,range(1,5))
                    self.assertEqual(str(d['theme']),plan['theme'])
                    self.assertEqual([int(d['exit_'+a]) for a in 'xyz'],plan['exit'])
                    reward=tuple(int(d['reward_'+a]) for a in 'xyz')
                    self.assertEqual(blocks[reward][0],'mmorpg:delve_seal')
                    cx,cy,cz=[int(d['center_'+a]) for a in 'xyz']
                    # All 25 candidate spawn columns have headroom and a floor.
                    for dx in range(-2,3):
                        for dz in range(-2,3):
                            self.assertEqual(blocks[(cx+dx,cy,cz+dz)][0],'minecraft:air')
                            self.assertEqual(blocks[(cx+dx,cy+1,cz+dz)][0],'minecraft:air')
                            self.assertNotEqual(blocks[(cx+dx,cy-1,cz+dz)][0],'minecraft:air')
                for floor in plan['floors']:
                    y=floor['y']+1
                    start=gen.center(floor['start']);seen={start};q=deque([start])
                    while q:
                        x,z=q.popleft()
                        for nx,nz in [(x-1,z),(x+1,z),(x,z-1),(x,z+1)]:
                            if (nx,nz) in seen:continue
                            if blocks.get((nx,y,nz),('',))[0]!='minecraft:air' or blocks.get((nx,y+1,nz),('',))[0]!='minecraft:air':continue
                            seen.add((nx,nz));q.append((nx,nz))
                    for room in floor['rooms']:
                        # Treasure sits at centre, so test its adjacent aisle instead.
                        x,z=gen.center(room['cell'])
                        self.assertIn((x,z-1),seen)
                for (x,y,z),b in blocks.items():
                    if b[0]=='minecraft:ladder':
                        self.assertEqual(str(b[1]['facing']),'west')
                        self.assertNotIn(blocks[(x+1,y,z)][0],['minecraft:air','minecraft:ladder'])
                for f in plan['floors'][:-1]:
                    cx,cz=gen.center(f['end']); y=f['y']
                    for h in range(y-gen.STEP+1,y+3):
                        self.assertEqual(blocks[(cx+3,h,cz)][0],'mmorpg:delve_seal' if h==y else 'minecraft:ladder')

    def test_entrance_join_and_return_pad_for_all_rotations(self):
        def rotate(p,n):
            x,y,z=p
            for _ in range(n): x,z=-z,x
            return x,y,z
        for theme in gen.THEMES:
            _,entry=unpack(DATA/'structure/delves'/theme['id']/'entrance.nbt')
            block=entry[(4,0,4)]
            self.assertEqual(str(block[1]['orientation']),'down_east')
            self.assertEqual(str(block[2]['joint']),'aligned')
            for plan in [p for p in self.plans if p['theme']==theme['id']]:
                for n in range(4):
                    # Aligned connectors permit the same rotation on both pieces.
                    shaft=rotate(plan['shaft'],n); anchor=rotate((4,0,4),n)
                    origin=tuple(a-b-(1 if i==1 else 0) for i,(a,b) in enumerate(zip(anchor,shaft)))
                    dest=tuple(a+b for a,b in zip(rotate(plan['exit'],n),origin))
                    self.assertEqual(dest,rotate((1,1,1),n))
                    # The literal landing before rotation must be clear above a solid floor.
                    self.assertEqual(entry[(1,1,1)][0],'minecraft:air')
                    self.assertEqual(entry[(1,2,1)][0],'minecraft:air')
                    self.assertNotEqual(entry[(1,0,1)][0],'minecraft:air')

    def test_new_localization_keys_match(self):
        langs=[json.loads((ROOT/f'src/main/resources/assets/mmorpg/lang/{lang}.json').read_text()) for lang in ['ru_ru','en_us']]
        keys=[{k for k in d if k.startswith('mmorpg.delve.') or k.startswith('block.mmorpg.delve_')} for d in langs]
        self.assertEqual(keys[0],keys[1]);self.assertEqual(len(keys[0]),29)

    def test_runtime_safety_contracts(self):
        source=(ROOT/'src/main/java/com/robertx22/world_of_exile/delve/DelveMarkerBlockEntity.java').read_text()
        self.assertIn('output.store("AgeOfEpochData"',source)
        self.assertIn('UUID.fromString',source)
        self.assertIn('data.putBoolean("cleared", true)',source)
        self.assertIn('if (stand == null) return null;',source)
        self.assertNotIn('aoe_tower_boss',source)
        self.assertNotIn('TowerCollapse',source)
        self.assertIn('Difficulty.PEACEFUL',source)
        self.assertIn('data.putInt("base_level", base)',source)
        self.assertIn('!world.noCollision(box)',source)

if __name__=='__main__': unittest.main()
