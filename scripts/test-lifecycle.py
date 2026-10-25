#!/usr/bin/env python3
"""Two real GameTest sessions; restore only campaign data into the reset test world.

GameTestMain intentionally deletes its universe before each run. This verifies
campaign shutdown/startup persistence, not a vanilla world or client reload.
"""
import json
import re
import shutil
import subprocess
import sys
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parent.parent
reports = root / 'build/reports'
reports.mkdir(parents=True, exist_ok=True)
report = reports / 'gametest.xml'
settings = root / 'run/port-assets/settings.json'
settings.parent.mkdir(parents=True, exist_ok=True)
config = json.loads(settings.read_text()) if settings.exists() else {'mode':'auto'}
config['serverAutoDownload'] = True
settings.write_text(json.dumps(config,indent=2)+'\n')

def run(label, fixture=None):
    report.unlink(missing_ok=True)
    log = reports / f'lifecycle-{label}.log'
    args = [sys.executable, str(root / 'scripts/gradle-cloud.py'), 'runGameTestServer']
    if fixture:
        args.append(f'-PcampaignReloadFixture={fixture}')
    with log.open('w') as output:
        subprocess.run(args, cwd=root, stdout=output, stderr=subprocess.STDOUT, check=True, timeout=600)
    tests = list(ET.parse(report).getroot().iter('testcase'))
    expected={'kingdomsofminecraft_tests:campaign_lifecycle','kingdomsofminecraft_tests:terrain_sampling'}
    if len(tests) != 2 or {t.attrib.get('name') for t in tests} != expected or any(list(t.iter('failure')) or list(t.iter('error')) or list(t.iter('skipped')) for t in tests):
        raise RuntimeError(f'Expected both selected GameTests to pass; inspect {report}')
    text = log.read_text()
    if 'Missing element ResourceKey[minecraft:loot_table / minecolonies:' in text:
        raise RuntimeError(f'MineColonies data provisioning is incomplete: {log}')
    loaded = [int(x) for x in re.findall(r'Kingdoms campaign loaded at step (\d+)', text)]
    saved = [int(x) for x in re.findall(r'Kingdoms campaign saved at step (\d+)', text)]
    if len(loaded) != 1 or not saved or saved[-1] <= loaded[0]:
        raise RuntimeError(f'Missing campaign lifecycle evidence: {log}')
    return loaded[0], saved[-1]

first_loaded, first_saved = run('first')
fixture = reports / 'campaign-reload.dat'
shutil.copyfile(root / 'run/gametestserver/gametestworld/kingdomsofminecraft/campaign.dat', fixture)
second_loaded, second_saved = run('reload', fixture)
if second_loaded != first_saved:
    raise RuntimeError(f'Campaign startup loaded {second_loaded}; shutdown saved {first_saved}')
print(f'PASS: 2 selected Minecraft sessions (2 GameTests each); campaign steps {first_loaded} -> {first_saved} -> {second_loaded} -> {second_saved}')
print('Full vanilla world reload and Windows singleplayer remain separate acceptance checks.')
