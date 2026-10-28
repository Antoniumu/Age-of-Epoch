#!/usr/bin/env python3
"""Offline integrity checks for authored batches; does not claim client playback QA."""
import json
from pathlib import Path
root=Path(__file__).resolve().parents[2]
assets=root/'src/main/resources/assets/mmorpg'
clips=json.loads((assets/'airship/adventure-voice.json').read_text())
sounds=json.loads((assets/'sounds.json').read_text())
voices={'captain':'voice-01','mara':'voice-03','quartermaster':'voice-05','oren':'voice-08','veya':'voice-10','selin':'voice-12','lysander':'voice-14','tik':'voice-18'}
assert len(clips)==20, 'Two batches must contain exactly twenty lines'
for batch in (1,2):
    selected={key:clip for key,clip in clips.items() if clip['batch']==batch}
    assert len(selected)==10, f'Batch {batch} must contain ten clips'
    assert set(key.rsplit('_',1)[0] for key in selected)==set(voices)
assert set(key.rsplit('_',1)[0] for key in clips)==set(voices)
catalog='\n'.join((root/('src/main/java/com/robertx22/age_of_exile/airship/adventure/'+name+'.java')).read_text() for name in ['AdventureCatalog','AdventureEngine','Investigation'])
for key,clip in clips.items():
    npc=key.rsplit('_',1)[0]
    assert clip['voice']==voices[npc], key
    assert clip['text'] in catalog, f'Text drift: {key}'
    assert clip['durationMillis']>1000
    entry=sounds['airship.'+clip['sound']]['sounds'][0]
    assert entry['stream'] is True
    audio=assets/'sounds'/(entry['name'].removeprefix('mmorpg:')+'.ogg')
    assert audio.read_bytes().startswith(b'OggS'), f'Missing/invalid OGG: {key}'
    print(f"PASS {key}: {clip['voice']} {clip['durationMillis']}ms {audio.stat().st_size} bytes")
print('PASS: 20 clips in two batches of ten, 8 original voices, exact authored texts, registered OGG assets')
