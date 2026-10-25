"""Fail closed unless a matching milestone has recorded verification evidence."""
import json
import os
from pathlib import Path

state = json.loads(Path('release-status.json').read_text())
tag = os.environ.get('GITHUB_REF_NAME', '')
if not tag or state['candidate_tag'] != tag:
    raise SystemExit('Release tag does not match recorded candidate')
required = ('build', 'core_tests', 'mod_startup', 'world_save_reload', 'windows_singleplayer')
if not all(state['acceptance'].get(key) is True for key in required):
    raise SystemExit('Required milestone acceptance checks are incomplete')
print('Recorded milestone acceptance passed; CI build also required')
