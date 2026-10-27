# Living Settlements 0.5.0 validation — 2026-10-27

## Scope and pinned platform

Extends the existing0.4.0 source, retaining village replacement, governors, mayors,
real native construction, inventories and existing saves. Minecraft26.1.2,
NeoForge26.1.2.124, Java25, Gradle9.2.1 and NeoGradle7.1.38 remain unchanged.
Exact dependency: minecolonies-1.1.1399-port26.1.2-0.6.0-publish.jar, SHA256
F025F911F3980E5FC71FE75BE2385243D95F998DD7C589B9E1E8820057186436.
Matching inspected port: tagv0.6.0, commit8e5d23fb5c6cee93211cec7787680af04e6a4300.

## Implemented changes

| Area | Main files | Behavior |
|---|---|---|
| Discovery | NaturalGenerationRules, NaturalSettlementService, VillageReplacement | Independent seeded opportunities supplement the50% village coin; old distance grid no longer gates new starts; full-area flatness screen removed |
| Adaptive placement | NaturalSettlementPlanner, AdaptiveTerrain, SchematicAccess, NaturalBlueprints | Alternate positions/rotations, actual ground under trees, protected volumes/claims, schematic exits meeting real terrain; only Town Hall needed initially |
| Native bootstrap | NpcSettlementBootstrap, NaturalSettlementService | Registered Town Hall, finite bounded founding supports, real citizens and saved mayor; later plots deferred |
| Progression | MayorService, MayorResources, MayorFootpaths | Finite survival-kit costs independent of reserved future plots; actual tool transfer; existing native jobs, requests, resources and upgrades; terrain routes |
| Infrastructure | InfrastructureData, InfrastructureService | Saved generated schematics and native paid decoration orders for supports/retaining work, roads/stairs and bridge decks; native workers perform changes |
| Protection/budget | NativeWorkBudget, NativeWorker* mixins, GovernorService | Fair shared native work-step admission; ownership checks; queued player-edit cancellation, persisted plot vetoes and route avoidance |

New saved mode/rotation/access/support/veto fields are additive. Missing old fields
retain legacy behavior. Newly created reservations have no arbitrary center-distance
limits even when the earlier full-starter mode is explicitly selected. Native claim
chunks still cannot be owned by two colonies. Physical footprint/protection checks
remain essential safety gates.

## Automated verification

Final complete run passed **63 unit tests with zero failures/errors and all14 required server checks**. Build succeeded in2minutes. Command:

```powershell
./scripts/build.ps1 -Tasks @('build','runGameTestServer','--offline','--max-workers=1')
```

Log: `.research/v050-release-verified.log`. Final results include the adaptive first-builder, concurrent-file, farm-field and native worker checks.

The current suite includes:

- Seeded density/determinism tests over1,000,000 chunk positions: baseline plus
  independent opportunities approximates2.5x the intended baseline; adjacent
  opportunities are allowed. This is not a completed-town exploration sample.
- Fair budget tests with100 workers across multiple colonies, bounded per-tick
  work admission and no starvation; expired waiters do not stall the queue.
- Infrastructure state codec, invalid identity/path rejection, new plot/rotation/
  access/support/veto round trips, and old-save migration regressions.
- Concurrent colony file operations wait for a complete world-specific pack
  manifest; independent world roots remain separate. Adaptive farmer decisions request a missing/unusable field before upgrades; new kits reserve81 real dirt and workers receive native soil/lighting orders. Full farm completion remains a gameplay check.
- Original native bootstrap/field/protection/village/fallback/unload/mayor tests.
  Native village selection still exercises200 starts/four seeds/all five variants;
  the pure selection sample covers2,000 decisions.
- A positive native-terrain fixture creates biome/noise ProtoChunks using the actual
  installed generator preset and copies their sections into disposable test chunks.
  It does not flatten the terrain. With Tectonic3.0.28/Lithostitched1.8.1 installed,
  seed3478827910715368652 at chunk43,38 founded a registered Town Hall at696,116,616
  with four native citizens and a designated mayor. The final run also verified the
  adaptive finite17,161-item kit and a genuine level-zero Builder's Hut/native BUILD request.
- Rotated blueprints retain their source file metadata; deep copies isolate mutable
  tile NBT. Unloaded searches remain retryable without loading their probe chunks.
- Real-player edit hooks cancel queued native building work after the planning
  target has been cleared. Edited infrastructure signals replanning rather than
  waiting forever on a blocked task.
- A native worker fixture reacquires a real stored shovel, queues a native decoration
  request, excavates a grass block and places cobblestone. Final observed values:
  combined builder/citizen cobblestone64->63 and shovel damage0->1. A test-only
  connected mock visitor activates native worker simulation; no real client or
  manual gameplay session is claimed. Production never adds fake activity users.

Only disposable fixtures force/copy terrain chunks or use test visitors. Native
surface decoration, tree population and structures are not generated by the noise
copy fixture: it proves positive founding on actual uneven noise terrain, not full
forest, river, village replacement or live exploration outcomes in a real profile.
Other fixtures test the actual replacement hook and native fallback independently.

## Review and corrected failures

Independent read-only review identified rotation metadata loss, permanent rejection
of unloaded alternatives, missing protection after native BUILD was queued, blocked
infrastructure waiting forever, schematic access gaps and misleading controls.
The corresponding code and tests were updated. Native regressions were observed
failing for the unloaded, queued-edit and blocked-task conditions before fixes.
Rotation copies also needed deep NBT isolation to avoid corrupting cached source
blueprints while rotating tile coordinates. Native tests check metadata preservation.

Intermediate failures are retained as diagnostic history. Test-only failures included
JPMS split package/import errors, two fixtures selecting the same site, a nested
GameTest callback mutating its scheduling map, and a mock visitor lacking a NeoForge
network handshake. Ownership checks correctly refused a fixture whose owner had
changed; a disposable NPC fixture now restores its own original owner after verifying
that ownership changes revoke NPC authorization. No user profile/world was modified.

## Remaining limits and manual checks

- Expected opportunity density is calibrated to vanilla's intended32x32 village
  opportunity and50% selection, not measured2.5x completed settlements. Real vanilla/
  Tectonic/Terralith exploration and end-to-end success ratios remain manual.
- Tectonic is installed in automated tests; Terralith is absent. Vegetated forests,
  substantial mountains, shorelines, entire bridges/roads and multi-building
  infrastructure completion need gameplay sessions. Schematic exit validation and
  route surveys do not prove every native navigation decision succeeds.
- Physical workers need loaded chunks and real nearby visitors in this exact port.
  Offline progress remains bounded administration/resource forecasting: no fabricated
  resources, citizens or completed buildings and no server-offline wall-clock growth.
- Initial Town Hall/supports are a finite bounded world-generation exception. Later
  work requires native workers, tools and real materials. Further production, advanced
  research, complete building upgrades and long-term self-sufficiency remain unproven.
- Work-step budgets constrain primary native operations. Native multi-block placement,
  neighbor physics, lifecycle callbacks and saved-data IO do not have a strict raw
  block-change/tick-time bound.100-worker policy fairness and two native mayor fixtures
  are functional tests, not large-server stress profiling.
- Block entities, generated structures, claims and recorded player edits are protected.
  An unclaimed structure consisting only of natural blocks placed before addon tracking
  cannot always be distinguished from terrain; comprehensive third-party land-claim
  integrations and historical block ownership are not implemented.
- Normal saved-state reconstruction and journal/receipt checks are covered; a complete
  process restart, injected JVM/disk failure and mismatched backups remain manual.
  Native world/colony/entity/addon files are not one atomic transaction. Include the
  world's generated infrastructure folder in backups. No lossless crash guarantee.
- Single-player/client UI, dedicated multiplayer stress and prolonged economy/raids
  remain gameplay checks. Old abandoned MineColonies towns are not auto-reactivated;
  existing0.4 failed sites and already-generated chunks are not silently regenerated.

## Artifacts

Packaged and integrity verified:

| Artifact | Bytes |
|---|---:|
| JAR | 216650 |
| Import ZIP | 274338 |

JAR SHA256: C464646B51BFF320E01D13E3073ECB648877DC8CAAFFB15AC7921F223584E3A1.
Build/dist/embedded JARs match; metadata says0.5.0, required classes are present,
test classes are absent, preset copies match, and original dependency IDs/eight
config files are unchanged. Final log has no ERROR entries or save failures.

Files:
`dist/living-settlements-0.5.0.jar`, matching CurseForge importZIP and server preset.



