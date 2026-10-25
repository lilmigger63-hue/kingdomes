#!/usr/bin/env python3
"""Render weighted progress exclusively from recorded deliverable states."""
import json
from pathlib import Path

state = json.loads((Path(__file__).resolve().parent.parent / 'progress.json').read_text())
total = 0.0
verified = count = 0
for stage in state['stages']:
    tasks = stage['deliverables']
    done = sum(task['verified'] is True for task in tasks)
    total += stage['weight'] * done / len(tasks)
    verified += done
    count += len(tasks)
bar = '#' * int(total / 5) + '-' * (20 - int(total / 5))
print(f'KINGDOMS OF MINECRAFT\nProgress: {total:.1f}%\n[{bar}]\nVerified tasks: {verified}/{count}')
print('Current stage: ' + state['current_stage'])
print('Build status: ' + state['build_status'])
print('Latest release: ' + (state['latest_release'] or 'None'))
print('Tokens used: Unavailable')
