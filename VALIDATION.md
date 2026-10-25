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
