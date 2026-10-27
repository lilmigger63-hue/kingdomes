# Version 0.3.0 validation — 2026-10-26

Build: successful (`scripts/build.ps1 -Tasks @('build','runGameTestServer','--stacktrace')`).
Final log: `.research/v030-release.log`.

## Automated checks actually run

- 39 JUnit tests, zero failures/errors. Includes 2,000 deterministic selection
  decisions across four seeds, reverse traversal, chance limits, persistent
  fallback edit bounds, stale disk checkpoints, bounded offline intent and all
  previous spatial/governor/save tests. New policy regressions were observed red
  before implementation, then green.
- All nine required server checks passed: one built-in plus commands,
  unloaded survey, water budget, original grid colony, native village selection,
  marker-based colony with unload/reload, native vanilla fallback and installed
  noise terrain compatibility.
- In a fresh disposable flat GameTest world, directly exercised 200 native
  village generation starts across four seeds and all five variants: 91 selected,
  109 retained vanilla. This exercises the actual generation Mixin; it is not a
  measurement of naturally explored biome distribution. Native NBT reload retains
  marker decision and original fallback pieces. Reservations explicitly disable
  terrain shaping through the exact NeoForge piece modifier.
- Full Medieval Oak native colony bootstrap verified real registered town hall,
  essential huts, six living citizens, native food inventories, homes/jobs, actual
  assigned carrot field, finite supplies, independent ownership and native work
  orders. Both the retained grid path and saved marker path were exercised.
- Actual forced test chunks were released until the marker colony center became
  unloaded. Advancing game time accumulated intent without loading the colony.
  Reloading and advancing to a 200-tick boundary outside the normal1,200 cadence
  produced real native construction work. No buildings/citizens were fabricated
  during the unloaded interval.
- A local natural terrain bump deliberately rejected the full town-hall plan
  before founding. The deferred fallback restored retained native village pieces
  and actual vanilla villagers, with no colony founded at that site.
- Real saved-data disk reread through fresh storage, duplicate chunk events,
  player-colony preservation and owner-change revocation were checked.
- Tectonic3.0.28/Lithostitched1.8.1 were installed in the test server. Sixteen
  normal Overworld noise-query samples all safely retained their original start;
  total survey time425ms in this run, with no queried chunks created. This is
  terrain API compatibility coverage, not an exploration/performance benchmark.
- Final run contained no ERROR/save-failure messages. Earlier high-pressure
  disposable fixture runs exposed native Windows atomic-save errors; addon
  checkpoints now verify the actual saved bytes before their relevant changes.
- Independent review found terrain shaping, fallback edit protection, inactive
  offline credits and unloaded-origin protection gaps. All were fixed; final
  review reported no important remaining issue in those fixes.

## Release artifact checks

`dist/living-settlements-0.3.0.jar` —108,394 bytes.
SHA256: `544CA363CF482D59E8D4AFEE556CF7DD4C5E9EF0FA8F9674F54503092FFB5020`.
`dist/kingdoms-living-settlements-0.3.0.zip` —169,006 bytes.
Packaged JAR matches build output and embedded ZIP JAR. Metadata0.3.0, Mixin
configuration/class and new default configuration are present; test classes absent.
All eight original pack configs and original dependency file IDs are unchanged.
No live profile, installed world or original export was modified.

## Still requires gameplay testing

Real exploration with vanilla structure-set/biome distribution, finished-colony
share on Tectonic landscapes, Terralith (not installed), client/single-player UI,
long-running worker economy and upgrade completion, multiplayer stress and abrupt
JVM/IO crash injection. Fresh disk-storage reload is not a full process restart.
No claim of a strict worst-case tick time or measured no-regression performance.

Offline progression is saved development planning, not physical building
completion or resource gathering. Native worker AI still needs real visitors.
Unsafe terrain/fallback and blocking claims can reduce finished replacements below
50%; partial or interrupted sites are retained safely. At-most-once fallback
journaling may skip an unfinished slice on abrupt crash; no cross-file atomic-save
guarantee. See README and KNOWN_ISSUES for operational limits.
