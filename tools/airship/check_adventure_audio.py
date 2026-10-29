#!/usr/bin/env python3
"""Offline integrity checks for authored batches; does not claim client playback QA."""
import json
from pathlib import Path
root=Path(__file__).resolve().parents[2]
assets=root/'src/main/resources/assets/mmorpg'
clips=json.loads((assets/'airship/adventure-voice.json').read_text())
sounds=json.loads((assets/'sounds.json').read_text())
voices={'captain':'voice-01','mara':'voice-03','quartermaster':'voice-05','oren':'voice-08','veya':'voice-10','selin':'voice-12','lysander':'voice-14','tik':'voice-18'}
assert len(clips)==60, 'Six integrated batches must contain sixty lines'
for batch in (1,2,3,7,8,9):
    selected={key:clip for key,clip in clips.items() if clip['batch']==batch}
    assert len(selected)==10, f'Batch {batch} must contain ten clips'
    if batch!=9: assert set(key.split('_')[0] for key in selected)==set(voices)
    else: assert set(key.split('_')[0] for key in selected)=={'quartermaster','oren','veya','selin','lysander','tik'}
assert set(key.split('_')[0] for key in clips)==set(voices)
catalog='\n'.join((root/('src/main/java/com/robertx22/age_of_exile/airship/adventure/'+name+'.java')).read_text() for name in ['AdventureCatalog','AdventureEngine','Investigation','Voyage','Council','Recon','CrewWorkshop'])
for key,clip in clips.items():
    npc=key.split('_')[0]
    assert clip['voice']==voices[npc], key
    assert clip['text'] in catalog, f'Text drift: {key}'
    assert clip['durationMillis']>1000
    entry=sounds['airship.'+clip['sound']]['sounds'][0]
    assert entry['stream'] is True
    audio=assets/'sounds'/(entry['name'].removeprefix('mmorpg:')+'.ogg')
    assert audio.read_bytes().startswith(b'OggS'), f'Missing/invalid OGG: {key}'
    print(f"PASS {key}: {clip['voice']} {clip['durationMillis']}ms {audio.stat().st_size} bytes")
manifest=json.loads((root/'docs/airship/audio/rewrite-05/manifest.json').read_text())
assert len(manifest)==10
assert {v['clipKey'] for v in manifest.values()}=={k for k,v in clips.items() if v['batch']==9}
for filename,item in manifest.items():
    assert all(clips[item['clipKey']][field]==item[field] for field in ('sound','text','durationMillis','voice'))
    assert (root/'docs/airship/audio/rewrite-05'/f'{filename}.mp3').stat().st_size>1000
rewrite=json.loads((root/'docs/airship/audio/rewrite-06/manifest.json').read_text())
assert len(rewrite)==10
for key,item in rewrite.items():
    assert all(clips[key][field]==item[field] for field in ('sound','text','durationMillis','voice'))
    assert (root/'docs/airship/audio/rewrite-06'/f'{key}.mp3').stat().st_size>1000
print('PASS: 60 clips in six integrated batches of ten, 8 original voices, exact authored texts, registered OGG assets')
