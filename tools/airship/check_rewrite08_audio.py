#!/usr/bin/env python3
"""Validate both legacy crew and adventure records in rewrite-08."""
import json
from pathlib import Path
root=Path(__file__).resolve().parents[2]
assets=root/'src/main/resources/assets/mmorpg'
manifest=json.loads((root/'docs/airship/audio/rewrite-08/manifest.json').read_text())
crew=json.loads((assets/'airship/crew-voice.json').read_text())
adventure=json.loads((assets/'airship/adventure-voice.json').read_text())
sounds=json.loads((assets/'sounds.json').read_text())
story=(root/'src/main/java/com/robertx22/age_of_exile/airship/AirshipStory.java').read_text()
assert len(manifest)==10
for key,item in manifest.items():
    source=crew if item['source']=='crew' else adventure
    assert all(source[key][field]==item[field] for field in ('sound','text','durationMillis','voice')),key
    if key=='bram_idle':assert item['text'] in story
    assert item['durationMillis']>1000
    assert 'airship.'+item['sound'] in sounds
    assert (assets/'sounds/airship'/f"{item['sound']}.ogg").read_bytes().startswith(b'OggS')
    assert (root/'docs/airship/audio/rewrite-08'/f'{key}.mp3').stat().st_size>1000
print('PASS: rewrite-08 crew and adventure voices, texts, durations and registered audio')
