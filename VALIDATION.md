# Validation evidence — 2026-10-25

- Official MDK remote and exact commit verified against GitHub.
- Temurin JDK 25+36 archive verified against upstream SHA-256. Gradle 9.2.1 distribution verified against upstream SHA-256; wrapper checksum pinned.
- Initial test-first campaign fixture failed on missing behavior, then passed. Dependency-free Java 25 persistence contracts: 18 checks passed, covering roundtrip/replacement, header/schema/checksum/length corruption, negative/overflow clocks, failed-write preservation/cleanup and world isolation.
- Release gate: three Python tests passed (wrong tag rejected, incomplete acceptance rejected, complete matching fixture accepted). No real release acceptance is implied by the positive fixture.
- First full NeoForge build: PASS, 4m 50s. Current code is undergoing final repeatable setup and GameTest validation; see later entries.
- Initial direct wrapper download failed DNS because Java did not use the platform proxy. First proxy attempt failed trust resolution. Configuring platform proxy and system Java trust store restored dependency resolution, without bypassing verification.

Unrun: Windows singleplayer, graphics/FPS benchmarks, all future gameplay systems. CI workflow execution and GitHub push publication are recorded separately after confirmation.

## Final local checks

- `./scripts/cloud-setup.sh`: PASS, 28s Gradle build after wrapper checksum verification. This executes the saved installation instructions; a new published task has not been tested.
- `python3 scripts/gradle-cloud.py build`: PASS, 6s final build with all 18 core checks. Gradle emits upstream/tooling deprecation warnings for eventual Gradle 10 compatibility; pinned Gradle 9.2.1 succeeds.
- `python3 scripts/test-lifecycle.py`: PASS, exactly one selected `kingdomsofminecraft_tests:campaign_lifecycle` GameTest in each of two actual server processes. First startup step 0, shutdown step 233, next startup step 233, final shutdown step 411. Reports are generated under `build/reports/` (ignored).
- Restart test was first observed failing (startup 0 versus prior shutdown 116), because upstream GameTestMain deletes its universe. The isolated test mod now restores the prior campaign file before production startup. This proves campaign-file startup/shutdown persistence across real servers; it does **not** establish vanilla world reload or integrated client behavior. `world_save_reload` acceptance remains false.
- Release JAR inspected: expected mod metadata and production classes, no isolated test classes. SHA-256 recorded in release-status.json.
- Three workflow files parsed as YAML. Release policy fixtures: 3/3 passing. Real release gate remains closed; no tag pushed.
- Independent read-only reviewer found no definite blocking source defect. Requested actual lifecycle restart evidence is now captured above. Integrated world switching remains manual acceptance. Parent-directory synchronization after rename is not implemented; sudden-power-loss durability is filesystem dependent and is not promised.
- GitHub repository read and API permissions verified. Creating the protected release environment failed with HTTP 403 `Resource not accessible by integration`; no environment was created. The release workflow checks required-reviewer protection before publishing.
- Environment draft saved: network destinations, install_script and start_skill. Draft saving does not itself publish a snapshot.

## Manual acceptance still needed

On Windows with Java 25 and the target NeoForge/Minecraft versions, start the client, create world A, run `/kingdoms status`, play, save/quit, and reopen A. Confirm its persisted step continues. Switch to world B, then back to A in the same client and verify each state remains independent. Verify normal survival exploration/mining/crafting still works and inspect logs for errors. Record actual results before changing acceptance flags or tagging a milestone.

## GitHub checkpoint

- Confirmed source push: `dc081212c15ee69e56916afa3018970f9b237390` on `development/foundation`; remote ref matched local HEAD after push.
- Draft PR: https://github.com/lilmigger63-hue/kingdomes/pull/1 (main remains unchanged).
- GitHub Build: https://github.com/lilmigger63-hue/kingdomes/actions/runs/40183584762 — completed SUCCESS; includes full build, two-session campaign integration and development JAR artifact upload.
- GitHub Core contracts: https://github.com/lilmigger63-hue/kingdomes/actions/runs/40183584848 — completed SUCCESS.
- PR status checks independently confirmed all build/persistence jobs succeeded on the source commit. Detailed hosted log download was unavailable through the current network route; conclusions and successful steps were verified through the GitHub API.
- No tag or release was published. A documentation-only follow-up records this evidence; production source is unchanged from the tested commit.

## Kingdom founding slice — 2026-10-25

- Test-first custom-founded save roundtrip failed against schema 1 storage, then passed with schema 2. Core suite: 21 persistence checks, 1,411 founding checks across 200 seeds/invariants, 18 malformed-save checks (1,450 Java checks total). Three Python release-gate tests pass.
- Schema 1 fixture preserves literal seed -18 and step 456, is not rewritten by load, and upgrades on the next save. Custom-founded state survives storage without regenerating initial kingdoms. Bounded/count/culture/name/ID corruption fixtures all reject without altering input files.
- Full NeoForge build passed. Exactly one selected Minecraft test passed in each of two server sessions, including /kingdoms list, kingdom autosave and restart from 881 to 970.
- Review identified a regeneration blind spot in the original restart test. The strengthened test first failed with the custom fixture, then passed after asserting the fixture's literal custom name/banner, preserved founding clan and unchanged other records. A stored-clock reset is checked by both the game test and runner's startup/shutdown comparison. No production correctness defect was reported by the read-only reviewer.
- GameTest still resets its universe: these checks establish campaign-record lifecycle persistence, not full vanilla world/client reload or Windows singleplayer. Release flags for those checks remain false. No release or tag created.
- New source CI will be checked separately after push; prior source CI success is not claimed for this slice.

## Political state slice — 2026-10-25

- Test-first suite failed when schema 2 discarded the custom vassal and war treaty, then passed with schema 3. Final Java suite: 21 persistence + 1,411 founding + 30 malformed-save + 21 political checks (1,483 total). Existing three Python release-gate tests pass.
- Clan transfer, ruler protection, unknown/duplicate references, symmetric/idempotent transitions, future treaty time rejection and preservation through ticking/new founding are exercised. Full 64-clan/190-treaty capacity with long three-byte Unicode names fits the 64 KiB bound and roundtrips.
- Hand-built schema 2 fixture preserves custom kingdom/clan IDs and names, seed/time and original file bytes during migration. Schema 1 migration remains covered. Invalid political counts/flags are rejected without rewriting.
- Full NeoForge build passed. Two selected Minecraft GameTest sessions verify /kingdoms politics and preserve a custom vassal, war treaty and transition time through production startup and autosave; clock 0 → 98 → 98 → 198. GameTest's reset universe still precludes claiming full vanilla world/client reload.
- Independent read-only review found no important actionable defect. No autonomous diplomacy, player political actions or release completion claimed. Source GitHub CI is checked after push and is separate from local results.

## Settlement ownership slice — 2026-10-25

- Observed test-first ownership roundtrip fail against schema 3, then pass with schema 4. Full Java suite: 21 persistence, 1,411 founding, 34 malformed-save, 23 political/capacity and 21 territory checks (1,510 total). Three Python release-gate tests pass; full NeoForge build passed.
- Golden schema 3 save generated before the codec change verifies seed 7, step 100, 13 kingdoms, custom clan 100 and war 0/1 are preserved, with no newly created settlements and no write during load. Schema 1/2 migration coverage remains active.
- Tests cover region floor division, dimension isolation, duplicate ownership/name rejection, one capital per realm, wartime transfer and preservation across ticks, politics and founding. Maximal valid state (20 kingdoms, 64 clans, 190 treaties, 128 sites) with long multibyte names/dimension strings roundtrips within 256 KiB.
- Two selected Minecraft sessions passed, clock 0 → 32 → 32 → 83. Commands reject non-operator registration and capital changes, accept operator registration/designation, reject a conflicting site, allow public inspection, and preserve custom settlement name/capital/ownership through restart. Commands left a remote region's chunks unloaded. Production adds no block-writing operations.
- Read-only independent review found no important concrete defect. Full vanilla world/client reload and Windows singleplayer remain unrun, and release flags remain false. Physical buildings, world generation, protection and live siege/conquest are not validated or claimed.
- GitHub CI for this source is checked separately after push; local results above are independently confirmed.
