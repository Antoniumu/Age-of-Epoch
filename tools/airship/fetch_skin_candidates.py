#!/usr/bin/env python3
"""Fetch real skin files for review only; never silently use a thumbnail or a fallback skin.
Run on the network-enabled asset acquisition runner. These candidates are NOT mod resources.
"""
import hashlib, html, io, json, re, urllib.request
from pathlib import Path
from PIL import Image
ROOT=Path('docs/airship/crew-preview')
HEADERS={'User-Agent':'Mozilla/5.0 (compatible; AgeOfEpochSkinReview/1.0)'}
def get(url):
    with urllib.request.urlopen(urllib.request.Request(url,headers=HEADERS),timeout=45) as r:
        return r.read(),r.geturl()
manifest=json.loads((ROOT/'manifest.json').read_text())
report=[];errors=[]
for row in manifest:
    candidates=row.get('rawCandidates',[])+[row['page'].rstrip('/')+'/download/file/'+row['fileId']+'/']
    try:
        data,_=get(row['page'])
        source=html.unescape(data.decode(errors='replace')).replace('\\/','/')
        candidates+=re.findall(r'https://static\.planetminecraft\.com/files/resource_media/skin/[^\s"\'<>\\]+',source)
        # Store matching file references in diagnostics, not the large third-party HTML page.
        report.append(row['id']+' CDN references: '+str(candidates))
    except Exception as e: report.append(row['id']+' page: '+str(e))
    found=False
    for url in dict.fromkeys(candidates):
        try:
            data,resolved=get(url)
            if not data.startswith(b'\x89PNG\r\n\x1a\n'): raise ValueError('Not a PNG skin')
            im=Image.open(io.BytesIO(data));im.load()
            if im.size not in ((64,32),(64,64)): raise ValueError('Not a supported skin atlas: '+str(im.size))
            (ROOT/(row['id']+'.png')).write_bytes(data)
            row.update(download=resolved,sha256=hashlib.sha256(data).hexdigest(),size=list(im.size),permission='Preview candidate only; public mod redistribution permission not verified')
            report.append(row['id']+' OK '+str(im.size)+' '+resolved);found=True;break
        except Exception as e: report.append(row['id']+' '+url+' '+str(e))
    if not found:errors.append(row['id'])
(ROOT/'fetch-report.txt').write_text('\n'.join(report)+'\n')
(ROOT/'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n')
if errors:raise SystemExit('Missing real skins: '+', '.join(errors))
