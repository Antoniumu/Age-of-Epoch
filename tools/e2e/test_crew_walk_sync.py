import unittest
from check_crew_walk_sync import validate

class SyncTest(unittest.TestCase):
    def test_valid(self):
        log=['AIRSHIPWALK START npc=veya uuid=abc-123 path=[] position=(0, 1, 0)',
             'AIRSHIPWALK CLIENT uuid=abc-123 time=1 pos=(1.5, 1, 0) player=(0,0,0)',
             'AIRSHIPWALK COMPLETE npc=veya steps=100 max_distance=1.5 end=(0, 1, 0)',
             'AIRSHIPWALK CLIENT uuid=abc-123 time=2 pos=(0.01, 1, 0) player=(0,0,0)']
        self.assertEqual(validate(log)[0], 'abc-123')
        with self.assertRaises(ValueError): validate(log[:-1])
        with self.assertRaises(ValueError): validate(log[:-1]+[log[-1].replace('0.01, 1, 0','2, 1, 0')])
    def test_wrong_actor_or_nonfinite(self):
        with self.assertRaises(ValueError):
            validate(['AIRSHIPWALK START npc=veya uuid=a position=(NaN, 0, 0)'])

if __name__ == '__main__': unittest.main()
