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

## MineColonies V5.2 initial adapter — 2026-10-25

- Official branch/version/license/API research recorded in docs/MINECOLONIES_COMPATIBILITY.md. Target stays 26.1.2; no older-version substitution. Pinned community port 9901957777e3dc8cf8119826c619654a48ae486e compiled unchanged, publishJar passed (3m 27s first build). A fresh source-cache setup-script build passed, and clean reused-source rebuild passed after integrity checks were added.
- Initial runtime probe omitted the isolated GameTest dependency and was discarded as MineColonies evidence. Corrected runtime injection loaded the actual port on NeoForge 26.1.2.122. Cold boot without external assets logged missing loot tables; supported upstream provisioning completed and those diagnostics disappeared. Official MineColonies base blueprints are explicitly required by the integration test; missing core loot tables fail the runner.
- Final suite: 1,528 Java checks (21 persistence, 1,411 founding, 38 malformed save, 23 politics/capacity, 21 legacy territory, 14 colony mapping), six Python release-gate tests, full Gradle build and tested cloud-setup commands. Golden schema 4 fixture preserves prior site/capital and political state without inventing colonies or rewriting on load.
- Two selected actual Minecraft test sessions passed, clock 0 → 94 → 94 → 203. Public API created a real MineColonies colony from a real registered base blueprint pack, adapter read its name/player owner/population/building metadata, operator binding persisted, non-operator/conflicting bindings were rejected and MineColonies player ownership stayed unchanged. Controlled political mapping restart is verified; actual MineColonies world restoration remains separate.
- Test owner uses NeoForge FakePlayer only. Vanilla mock login failed because mod payload channels were unnegotiated. Upstream logs warnings when this fake test actor is offered as a colony subscriber; it is not a verified production AI governor pattern. Normal client management/packets still need client validation.
- A test caught upstream positional lookup force-loading getChunkAt. Guarding with hasChunk restored the remote-unloaded-region invariant; the test now passes. Review found no campaign preservation/binding/permission/packaging defect.
- Both P2 setup review findings reproduced and fixed: custom external-cache compilation failed with the local JAR removed, then passed after Gradle and setup resolved the same cache; a dirty source build first succeeded, then was rejected before rebuilding after cleanliness checks were added. Intentional test edits were restored; no dependency source patch was retained. Clean source setup rebuilt successfully afterward.
- Release-gate tests first exposed acceptance of incomplete 0.6 evidence, then passed after all nine integration flags became required. Positive release fixtures are synthetic tests, not real milestone acceptance. Real 0.6 release remains blocked.
- No vendor source, dependency JAR or restricted assets enter the Kingdoms JAR. Only own classes/resources and own GPL license/notice are packaged. No release, tag or scheduling capability enabled. Source CI is checked independently after push.

## Tectonic V5.3 initial terrain slice — 2026-10-25

- Official target-filtered release metadata, JAR metadata and published SHA-512 hashes verified for Tectonic 3.0.28 / Lithostitched 1.8.1; Tectonic includes its Apollib library. Separate fresh-cache download passed; deliberate cached corruption was refused without modification. MineColonies dependency/source pin remains unchanged.
- Full build and cloud setup passed. Java suite: previous 1,528 checks plus 20 terrain-role/cache checks (1,548 total). Eight Python release-gate tests pass; incomplete combined terrain integration is rejected even when MineColonies flags are complete. Positive acceptance fixtures remain synthetic, not release evidence.
- Real normal-noise probe was initially absent because GameTest discards standalone dimensions; fixed with a test-only preset containing the extra dimension. Tests verify active Tectonic density data, NoiseBasedChunkGenerator and four explicitly generated probe chunks per session. Production JAR excludes this fixture and all worldgen overrides.
- Real probe cells are ocean, Y46–49 seabed and 25 water samples; village advice rejects them. Synthetic fixtures cover valley/mountain/ridge/forest/river/coast preferences and hazard rejection, but actual terrain-category colony construction is unrun and remains unverified.
- Lava regression first failed in a real loaded chunk (non-water fluid counted dry), then passed after the sampler rejected that surface. Test fixture restored. Remote sampling leaves chunks unloaded; generated observations leave the observed surface unchanged. Conservative surface data can include vegetation/structures; no construction-safety approval is claimed.
- Two Minecraft sessions passed exactly two selected tests each (campaign_lifecycle and terrain_sampling), clock 0 → 185 → 185 → 217. Existing real MineColonies discovery/mapping/permission invariants remain covered. GameTest still resets its universe, so full actual world save/reload is not established.
- Independent read-only review found the lava issue above; fixed and revalidated. No other important setup/cache/packaging/chunk-loading defect reported. New source CI is checked separately after push. No release/tag/scheduler enabled.

## Blueprint expansion preflight — 2026-10-25

- Test-first pure compilation failed because BlueprintFootprint did not exist; 23 geometry/reference checks then passed, for 1,571 total Java checks. Bounds include offset/inclusive endpoints, negative chunk boundaries, exact budget limits, arithmetic overflow and unsafe relative paths. Eight Python release-gate checks remain green; no acceptance flag was relaxed.
- Full Gradle build passed. Production JAR SHA-256: d682557a27dba906cefee992d6290d436d6be6ce716325e1895307e5b47fb574. Audit confirms own GPL license/notice, no dependency classes/JARs, no GameTest classes or test terrain presets.
- Final two Minecraft sessions passed exactly campaign_lifecycle and terrain_sampling in each session, campaign clock 0 → 32 → 32 → 80. Real MineColonies colony permissions and actual upstream builder blueprint loading/dimensions are exercised. Tests cover owner acceptance, unauthorized rejection, excessive cut/fill, a loaded anchor whose blueprint corner is unloaded, and no remote generation.
- Full-volume disposable fixtures reject water/lava/chests/bedrock between coarse sample positions; chest contents are preserved. The deterministic dry platform and hazards are restored. An earlier boundary search assumed world origin, and a natural-flat positive fixture encountered GameTest scaffolding; both test assumptions were corrected using the actual relocated fixture and explicit restored platform. This does not prove real Tectonic mountain/valley/river/coastal construction.
- Actual asynchronous blueprint command returns a passed/rejected result (exceptions do not count), bounds repeated work, and emits no later feedback after lifecycle clear. Server-thread feedback is asserted. Dimension-change and creative-operator-specific fixtures are unrun; guards and upstream operator semantics were inspected in review.
- Independent read-only review reported no defects. Resources, accessibility, full existing-structure bounds, other mods' protection, actual worker construction and all prior full-world/client/performance limitations remain pending. Installed blueprint packs use the trusted upstream decoder before inspection limits apply. Source CI is checked after push; no release/tag/scheduler enabled.
