"""Exercise the actual gate against isolated release records."""
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

GATE = Path(__file__).with_name('verify-release.py').resolve()

class ReleaseGateTest(unittest.TestCase):
    def gate(self, tag, accepted):
        with tempfile.TemporaryDirectory() as folder:
            record = {'candidate_tag': 'v0.1.0-alpha', 'acceptance': {
                'build': True, 'core_tests': True, 'mod_startup': True,
                'world_save_reload': True, 'windows_singleplayer': accepted}}
            Path(folder, 'release-status.json').write_text(json.dumps(record))
            return subprocess.run([sys.executable, str(GATE)], cwd=folder,
                env={**os.environ, 'GITHUB_REF_NAME': tag}, capture_output=True, text=True)
    def test_wrong_tag_rejected(self):
        self.assertNotEqual(self.gate('v1.0.0', True).returncode, 0)
    def test_missing_platform_acceptance_rejected(self):
        self.assertNotEqual(self.gate('v0.1.0-alpha', False).returncode, 0)
    def test_matching_verified_candidate_accepted(self):
        self.assertEqual(self.gate('v0.1.0-alpha', True).returncode, 0)

if __name__ == '__main__':
    unittest.main()
