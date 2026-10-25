# kingdomes

## Kingdoms of Minecraft

An incremental Minecraft **26.1.2** mod for **NeoForge 26.1.2.122**, using **Java 25**, ModDevGradle **2.0.148** and Gradle **9.2.1**. Requirements: [base prompt](docs/MASTER_PROMPT.md), superseded for settlements by [MineColonies V5.2](docs/MINECOLONIES_V5_2.md) and for terrain by [Tectonic V5.3](docs/TECTONIC_V5_3.md). The large gameplay roadmap is unfinished; this branch contains the foundation and the first persistent kingdom-founding slice.

### Development

Use a JDK 25, run `python3 scripts/setup-minecolonies.py` and `python3 scripts/setup-terrain.py` to prepare the pinned separate dependencies, then run `./gradlew build` (`gradlew.bat build` on Windows). `./scripts/test-core.sh` runs dependency-free campaign contracts. `python3 -m unittest discover -s scripts -p 'test_*.py' -v` checks the release guard. `./gradlew runGameTestServer` exercises actual mod loading and campaign autosave using an isolated test mod. `python3 scripts/test-lifecycle.py` runs two selected GameTest sessions and restores campaign data between them, proving campaign restart persistence. GameTest resets the vanilla test world; full world reload remains a separate check.

In Codex cloud, run `./scripts/cloud-setup.sh`; afterward use `python3 scripts/gradle-cloud.py <task>` to preserve platform proxy and certificate trust. TLS and artifact checksums remain enabled. No credentials are stored in the repository.

For manual play, use `./gradlew runClient`, create a test world, and run `/kingdoms status`. Wait for the clock to advance, save and exit, then reload and verify the saved step continues. `/kingdoms list` displays the 11–20 stored kingdoms and their founding clans, rulers and cultures. The current build does not spawn faction NPCs or implement recruitment, armies or autonomous diplomacy. `/kingdoms politics` lists clan allegiances and recorded treaties; core war/peace transitions are persistent, but player political actions and autonomous decisions are unfinished.

### Progress and continuation

Run `python3 scripts/progress.py` for weighted verified progress. See [validation](VALIDATION.md), [current task](CURRENT_TASK.md), [roadmap](ROADMAP.md), [release plan](RELEASE_PLAN.md), and [resume instructions](AUTO_RESUME.md). No release is available until milestone acceptance is complete.

### MineColonies integration

Requires the separate community port **1.1.1403-port26.1.2-0.7.1**, pinned to source commit `9901957777e3dc8cf8119826c619654a48ae486e`. See [compatibility evidence](docs/MINECOLONIES_COMPATIBILITY.md). No official 26.1.2 build was found in the inspected official branches/releases. Never substitute a 1.21.1 JAR into a 26.1.2 installation. The setup script builds the port's `publishJar` separately from source; it is not embedded in Kingdoms.

Use normal MineColonies tools and Town Hall management to establish a colony. `/kingdoms colonies` discovers its ID and reads its current name, player owner, population, buildings and Town Hall level. An operator can run `/kingdoms colonies bind <colonyId> <kingdomId>` in that dimension to record political allegiance. `/kingdoms list` shows kingdom IDs. Mapping does not change MineColonies player ownership or permissions. Rebinding to another kingdom is rejected until safe conquest integration exists.

`/kingdoms territory` queries MineColonies in loaded chunks only; unloaded areas report that lookup was skipped. `/kingdoms settlements` aliases colony discovery. Old standalone site/capital creation commands are disabled. Schema 5 preserves existing legacy site records without generating colonies or inventing bindings. MineColonies remains authoritative for physical settlement state.

The port fetches its external assets from upstream; dedicated test startup uses its `serverAutoDownload` setting. Do not publish/cache those assets as Kingdoms release artifacts. AI governors, construction automation, military/logistics transfer, safe conquest, full MineColonies world reload and performance validation remain unfinished. Headless tests do not establish Windows client/UI readiness.

License: GPL-3.0-only (LICENSE), with upstream/template notices in ASSET_ATTRIBUTION.md and TEMPLATE_LICENSE.txt. This replaces the initial All Rights Reserved declaration for compatible linked distribution.

### Tectonic terrain integration

Requires official Tectonic **3.0.28** (`3.0.28-neoforge-26.1`, Modrinth `RNLd3nvP`) and Lithostitched **1.8.1** (`pLlJIqyp`), both explicitly supporting NeoForge Minecraft 26.1.2. Apollib comes inside the official Tectonic JAR. `scripts/setup-terrain.py` downloads separate artifacts and checks upstream SHA-512 hashes; none is bundled in Kingdoms. Tectonic owns all production terrain generation and configuration.

`/kingdoms site <village|castle|trade|dwarven|elven|fort|coastal>` reports a conservative **advisory** screen of the current loaded chunk. It samples 25 existing surface columns, uses observed water/biome tags and height relief, rejects exposed non-water fluids, and caches at most 256 cells for 1,200 ticks. Sample age is displayed. Cached/vegetated/built surfaces are not a blueprint footprint or fresh safety validation; MineColonies fit, protected blocks, resources and accessibility still need checks. No blocks are changed or unloaded chunks generated by this command.

Players can additionally run `/kingdoms plan "Minecolonies Original" "fundamentals/builder1.blueprint"` at a chosen hut anchor. This loads the installed upstream blueprint asynchronously and freshly inspects its **original orientation** on the server thread. The entire footprint must already be loaded and within one existing colony where MineColonies grants the player hut-placement and block-placement/break permissions. Fluids, block entities/inventories, unbreakable blocks and more than four blocks of cut/fill fail this conservative screen. Blueprints are limited to 64 blocks per axis and 65,536 blocks of volume, with one request at a time and a 20-tick server-wide cooldown. It does not place anything or approve construction: resources, routes, existing structure bounds and protection from other mods remain unchecked. Use normal MineColonies tools for actual placement.

Fresh Tectonic generation is tested in an isolated normal-noise probe dimension (four explicit test chunks); the production JAR contains no test preset or custom terrain generator. Mountain/valley/river/coastal screening fixtures are synthetic, while the current real probe observes ocean terrain. Actual terrain-category colony construction, automatic geographic kingdom distribution, roads, army movement, world reload and performance remain unverified. Use a fresh test world; existing-world boundary/save compatibility is not established.
