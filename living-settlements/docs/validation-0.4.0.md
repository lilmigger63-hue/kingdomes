# Living Settlements 0.4.0 validation — 2026-10-26

## Platform and artifacts

Minecraft 26.1.2, NeoForge 26.1.2.124, Java 25, Gradle 9.2.1 and
NeoGradle 7.1.38 remain pinned. Integration was inspected against the exact local
`minecolonies-1.1.1399-port26.1.2-0.6.0-publish.jar` and its matching port source,
tag v0.6.0, commit `8e5d23fb5c6cee93211cec7787680af04e6a4300`.
The dependency SHA256 is
`F025F911F3980E5FC71FE75BE2385243D95F998DD7C589B9E1E8820057186436`.
Official upstream is a reference, not an interchangeable dependency.

| Artifact | Bytes |
|---|---:|
| `dist/living-settlements-0.4.0.jar` | 176,019 |
| `dist/kingdoms-living-settlements-0.4.0.zip` | 234,923 |

JAR SHA256:
`B9E672C4DA3BFAB519093F243649161A24C1170942F05842C48B646B7DFAC0C1`.
The build JAR, dist JAR and ZIP's embedded JAR match. Metadata says 0.4.0;
mayor implementation/mixins are present and test classes are absent. All original
manifest dependency IDs and eight original configuration files are preserved.
The embedded, default and standalone addon presets match byte for byte.

## Automated checks

Executed from the project root:

```powershell
./scripts/build.ps1 -Tasks @('build','runGameTestServer','--stacktrace')
./scripts/package.ps1
```

Final full verification: `.research/v040-release-tested.log` reports
`BUILD SUCCESSFUL in 1m 23s`, **51 unit tests with zero failures/errors** and
**all 10 required server checks passed**. These include one platform check and
nine addon checks. The final log contains no ERROR entries or save failures.
JUnit XML is in `build/test-results/test/`. Intermediate diagnostic runs are not
the release result.

Unit coverage includes needs-based policy (founding, food, housing, threats,
pending work, upgrade/cap gates), mayor state/legacy migration, finite stock and
escrow receipts/refunds, bounded food/wood/stone forecasts, and the previous
generation, protection, persistence, governor and village replacement regressions.
Policy tests were observed failing before their implementation.

The new native server test runs two independent autonomous colonies. It checks:

- A registered completed Town Hall alone, four native citizens, and one of those
  citizens designated mayor without spawning an extra citizen.
- Stable native mayor identity, exclusion from automatic hiring, capacity limits,
  and safe release/reselection after native citizen removal.
- An actual unbuilt level-zero Builder's Hut and native BUILD order targeting
  level one, assigned to a different citizen; no duplicate work order.
- Stock derived from real installed Medieval Oak blueprint requirements:
  19,077 initial units per fixture, with actual inventory delivery reducing stock.
- Paid handoff recovery after reconstruction of runtime jobs, fresh disk-backed
  SavedData loading, persisted routes, and actual colony chunk unloading.
- Bounded administrative forecasts while unloaded, without loading chunks,
  completing the hut or creating citizens.

Other server checks retain the full legacy bootstrap, actual field assignment,
ownership/claim protection, deferred vanilla fallback, generation persistence and
unloaded intent tests. The native village selection sample remains 91 selected
and 109 vanilla starts across four seeds/all five vanilla village variants.
This tests selection, not an observed 45.5% completed-settlement exploration rate.
The pure selection sample uses 2,000 decisions.

Tectonic 3.0.28 and Lithostitched 1.8.1 were installed for the native server tests.
The Tectonic noise survey retained safe fallback for 16 surveyed starts; the final
survey took 431 ms. This is a test observation, not a production tick-time bound.
Terralith was not installed.

Only disposable fixtures prepare flat terrain or force test chunks. Production
generation and mayor logic do not add chunk-loading tickets.

## Verified port constraints and implementation choices

The exact port permits a level-zero builder to build its own hut. New hut anchors
must remain level zero: copying completed schematic metadata initially produced
an incorrect level-one hut/UPGRADE request and was corrected and tested.
Native arrivals require an explicit spawn-position list and an addon capacity
check; the native force=false overload alone does not enforce the population cap.

No native mayor job is invented. A native adult keeps its registered citizen AI,
with a persisted administrative designation and narrow automatic-hiring hook.
Normal native worker activity requires real visitors in claimed chunks. The
controller never creates fake activity subscribers or permanent chunk tickets.

Native GUI research initiation expects a player network connection. The addon
instead checks native eligibility/prerequisites, pays actual university inventory
ingredients and starts native local research. Its researchers must still finish
the research. Native SavedData can swallow IO failures; relevant addon checkpoints
are reread from disk before mutations. Delivery holds are transient and do not
overwrite a citizen's saved pause flag.

## Behavior still requiring in-game testing

Native BUILD orders, finite material debit and worker assignment do **not** prove
that a builder has completed a whole hut or that a town becomes self-sufficient.
Long-running construction, advanced research completion, specialized production
chains, raids/defense behavior, other styles and all five development stages need
play sessions. Initial supplies are finite; further resources must come from
normal farming, forestry, mining, crafting and native logistics. Missing production
or style-specific materials can still stall development.

Roads are saved route proposals, not automatically constructed road blocks.
Unloaded progression forecasts resources and updates administrative plans; it
does not create physical items, births or buildings, or simulate wall-clock time
while the server is stopped. Physical native workers still require real visitors.

Fresh SavedData loading and runtime reconstruction were automated; a complete
stop/restart cycle, abrupt JVM/disk-failure injection and mismatched backup recovery
remain manual. Native world, colony, entity and addon files are not one atomic
transaction. Existing conservative bootstrap/fallback save windows can lose a
grant or skip an unfinished fallback slice rather than automatically duplicate it.

Client/single-player visuals, live multiplayer exploration and stress, real
conversion ratios and Terralith gameplay remain untested. Two simultaneous native
mayors are a functional regression check, not large-server performance validation.
Native lifecycle hooks and SavedData encoding can exceed column/time scan budgets;
no strict worst-case tick bound is claimed.
