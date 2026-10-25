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
if tag.startswith('v0.6.'):
    integration_required = ('compatible_dependency','colony_discovery_state','settlement_kingdom_mapping',
                            'building_reuse','ai_governor_construction','recruitment_logistics',
                            'save_load_synchronization','conquest_compatibility','performance_validation')
    integration = state.get('minecolonies', {}).get('acceptance', {})
    if not all(integration.get(key) is True for key in integration_required):
        raise SystemExit('MineColonies milestone acceptance is incomplete')
    terrain_required = ('compatible_dependencies','minecolonies_settlement_integration','tectonic_terrain_integration',
                        'terrain_aware_settlement_placement','kingdom_geographic_distribution','geographic_military_movement_costs',
                        'territory_mapping','strategic_road_planning','save_load_verification','performance_testing')
    terrain = state.get('terrain_integration', {}).get('acceptance', {})
    if not all(terrain.get(key) is True for key in terrain_required):
        raise SystemExit('Combined MineColonies/Tectonic milestone acceptance is incomplete')
print('Recorded milestone acceptance passed; CI build also required')
