#!/usr/bin/env python3
"""Keep the first-signal subtitles, authored recordings and registered assets aligned."""
import json
import re
from pathlib import Path

root = Path(__file__).resolve().parents[2]
manifest = json.loads((root / 'docs/airship/audio/first-signal-01/manifest.json').read_text())
crew = json.loads((root / 'src/main/resources/assets/mmorpg/airship/crew-voice.json').read_text())
java = root / 'src/main/java/com/robertx22/age_of_exile/airship'
script = (java / 'AirshipVoiceScript.java').read_text()
story = (java / 'AirshipStory.java').read_text()
sounds = (root / 'src/main/resources/assets/mmorpg/sounds.json').read_text()
expected = {'captain_intro_a':'voice-01', 'captain_intro_b':'voice-01', 'captain_welcome':'voice-01',
            'mara_intro':'voice-03', 'bram_intro':'voice-05', 'oren_intro':'voice-08',
            'veya_intro':'voice-10', 'selin_intro':'voice-12', 'lysander_intro':'voice-14', 'tik_intro':'voice-18'}
assert set(manifest) == set(expected)
for key, voice in expected.items():
    record = manifest[key]
    assert record['voice'] == voice and record['durationMillis'] > 0, key
    if key.startswith('captain_'):
        match = re.search(r'new Cue\("' + key + r'",("(?:[^"\\]|\\.)*"),(\d+)\)', script)
        assert match and json.loads(match[1]) == record['text'] and int(match[2]) == record['durationMillis'], key
    else:
        assert all(crew[key][field] == record[field] for field in ('text', 'durationMillis', 'voice')), key
    if key in ('mara_intro', 'bram_intro'):
        assert json.dumps(record['text'], ensure_ascii=False) in story, key
    assert f'"airship.{key}"' in sounds, key
    ogg = root / f'src/main/resources/assets/mmorpg/sounds/airship/{key}.ogg'
    mp3 = root / f'docs/airship/audio/first-signal-01/{key}.mp3'
    assert ogg.is_file() and ogg.stat().st_size > 1000 and mp3.is_file() and mp3.stat().st_size > 1000, key
print('PASS: first signal: 10 recordings, voices, captions, durations and sound registrations')
