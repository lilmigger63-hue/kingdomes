#!/usr/bin/env python3
"""Fetch exact upstream terrain artifacts and verify their published SHA-512 hashes."""
import hashlib
import json
import os
from pathlib import Path
import subprocess
import tempfile

root=Path(__file__).resolve().parent.parent
cache=Path(os.environ.get('KINGDOMS_DEPENDENCY_CACHE',str(root/'.gradle')))
if not cache.is_absolute(): cache=root/cache
spec=json.loads((root/'dependencies/terrain.json').read_text())
for artifact in spec['artifacts']:
    target=cache/'terrain'/artifact['version_id']/artifact['filename']
    target.parent.mkdir(parents=True,exist_ok=True)
    if not target.exists():
        with tempfile.NamedTemporaryFile(dir=target.parent,delete=False) as file: temporary=Path(file.name)
        try:
            subprocess.run(['curl','--fail','--location','--retry','2',artifact['url'],'--output',str(temporary)],check=True)
            if hashlib.sha512(temporary.read_bytes()).hexdigest()!=artifact['sha512']:
                raise SystemExit('Upstream checksum mismatch: '+artifact['mod_id'])
            temporary.replace(target)
        finally: temporary.unlink(missing_ok=True)
    if hashlib.sha512(target.read_bytes()).hexdigest()!=artifact['sha512']:
        raise SystemExit('Cached terrain artifact checksum mismatch: '+artifact['mod_id'])
    print('Verified terrain dependency:',artifact['mod_id'],artifact['version'])
