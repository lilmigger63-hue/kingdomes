"""Exercise the actual gate against isolated release records."""
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

MC_REQUIRED=('compatible_dependency','colony_discovery_state','settlement_kingdom_mapping','building_reuse','ai_governor_construction','recruitment_logistics','save_load_synchronization','conquest_compatibility','performance_validation')
TERRAIN_REQUIRED=('compatible_dependencies','minecolonies_settlement_integration','tectonic_terrain_integration','terrain_aware_settlement_placement','kingdom_geographic_distribution','geographic_military_movement_costs','territory_mapping','strategic_road_planning','save_load_verification','performance_testing')

GATE = Path(__file__).with_name('verify-release.py').resolve()

class ReleaseGateTest(unittest.TestCase):
    def gate(self, tag, accepted, candidate="v0.1.0-alpha", integration=None, terrain=None):
        with tempfile.TemporaryDirectory() as folder:
            record = {'candidate_tag': candidate, 'acceptance': {
                'build': True, 'core_tests': True, 'mod_startup': True,
                'world_save_reload': True, 'windows_singleplayer': accepted}}
            if integration is not None: record['minecolonies']={'acceptance':integration}
            if terrain is not None: record['terrain_integration']={'acceptance':terrain}
            Path(folder, 'release-status.json').write_text(json.dumps(record))
            return subprocess.run([sys.executable, str(GATE)], cwd=folder,
                env={**os.environ, 'GITHUB_REF_NAME': tag}, capture_output=True, text=True)
    def test_wrong_tag_rejected(self):
        self.assertNotEqual(self.gate('v1.0.0', True).returncode, 0)
    def test_missing_platform_acceptance_rejected(self):
        self.assertNotEqual(self.gate('v0.1.0-alpha', False).returncode, 0)
    def test_matching_verified_candidate_accepted(self):
        self.assertEqual(self.gate('v0.1.0-alpha', True).returncode, 0)

    def test_minecolonies_milestone_requires_integration_evidence(self):
        self.assertNotEqual(self.gate('v0.6.0-alpha',True,candidate='v0.6.0-alpha').returncode,0)
    def test_minecolonies_partial_integration_rejected(self):
        self.assertNotEqual(self.gate('v0.6.0-alpha',True,candidate='v0.6.0-alpha',integration={'compatible_dependency':True}).returncode,0)
    def test_combined_integration_complete_fixture_accepted(self):
        required=('compatible_dependency','colony_discovery_state','settlement_kingdom_mapping','building_reuse','ai_governor_construction','recruitment_logistics','save_load_synchronization','conquest_compatibility','performance_validation')
        self.assertEqual(self.gate('v0.6.0-alpha',True,candidate='v0.6.0-alpha',integration=dict.fromkeys(required,True),terrain=dict.fromkeys(TERRAIN_REQUIRED,True)).returncode,0)

    def test_complete_minecolonies_without_terrain_rejected(self):
        self.assertNotEqual(self.gate('v0.6.0-alpha',True,candidate='v0.6.0-alpha',integration=dict.fromkeys(MC_REQUIRED,True)).returncode,0)
    def test_partial_terrain_rejected(self):
        self.assertNotEqual(self.gate('v0.6.0-alpha',True,candidate='v0.6.0-alpha',integration=dict.fromkeys(MC_REQUIRED,True),terrain={'compatible_dependencies':True}).returncode,0)

if __name__ == '__main__':
    unittest.main()
