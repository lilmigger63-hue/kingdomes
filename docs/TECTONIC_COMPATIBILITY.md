# Tectonic V5.3 compatibility and initial geography adapter

## Exact upstream artifacts

Official Modrinth project Tectonic (`lWDHr9jE`, source https://github.com/Apollounknowndev/tectonic) release `RNLd3nvP`, version `3.0.28-neoforge-26.1`, declares NeoForge Minecraft 26.1, 26.1.1 and 26.1.2. Its JAR metadata version is 3.0.28 and license MIT. Separate required Lithostitched (`XaDC71GB`) release `pLlJIqyp`, `1.8.1-neoforge-26.1`, explicitly supports the same targets and declares MIT. Tectonic's published JAR includes Apollib 1.1.6 internally; do not add a duplicate library JAR.

Version IDs, HTTPS CDN URLs, filenames and published SHA-512 hashes are pinned in dependencies/terrain.json. scripts/setup-terrain.py verifies both fresh downloads and cached artifacts; a deliberate corrupt-cache probe was rejected without changing the corrupt file. Source SHA/version research remains in the manifest and original upstream API; no terrain JAR, assets or implementation is embedded in Kingdoms.

Minecraft remains 26.1.2, NeoForge 26.1.2.122 and Java 25. Existing pinned MineColonies 1.1.1403-port26.1.2-0.7.1 is preserved; its previous compatibility notes still apply. Startup with all required mods passes. Tectonic's enabled pack is registered as required/top by its NeoForge entry point when its own configuration is enabled; Kingdoms does not alter that configuration.

## Fresh-world proof and test boundary

Vanilla GameTest intentionally uses the flat world preset and discards standalone dimension datapacks. An initial fresh-dimension test failed on that assumption. The isolated test mod now supplies a flat preset that preserves its usual test dimension and adds a separate normal-noise dimension referring to minecraft:overworld settings/biome source. This fixture is not included in the production mod JAR.

Tests assert actual required mods loaded, a normal NoiseBasedChunkGenerator, and active resolved overworld final-density data referring to tectonic:base_terrain. Each run explicitly requests four FULL chunks in the probe dimension, then samples only the generated data. Observed cells 0,0 through 1,1 are ocean: seabed roughly Y46–49, all 25 sample columns water, and village advice rejects them. This is real generation/observation evidence, not proof of actual mountain/valley/river/coastal colony construction. Those category profiles are covered by separate synthetic observation fixtures only.

Production code never calls chunk generation or adds tickets. LoadedTerrainSampler uses getChunkNow, existing heightmaps/fluids/biome tags and 25 columns per chunk, server-thread only. Missing, void or exposed non-water-fluid observations return unknown. Biome coverage is not tree density/resource discovery, and surface height may include vegetation/buildings. A 256-cell cache expires after 1,200 ticks and clears at world lifecycle boundaries; unloaded cells remain unknown even if cached. The command reports observation age.

Site profiles are conservative advisory thresholds for village/castle/trade/dwarven/elven/fort/coastal roles. No full blueprint footprint, protected-block authorization, water depth/future flooding simulation, nearby inventory/resources, mountain-pass detection or accessibility graph is established. No automatic placement/flattening/construction is triggered. MineColonies remains the physical construction authority.

## Evidence and remaining work

Pure screening/cache checks pass for synthetic valley, steep-cliff, elevated/mountain, forest, river-access, coastal and low-shore observations, plus LRU/expiry/reset behavior. Review found lava was treated as dry; a real loaded-chunk lava fixture first failed, then passed after rejecting non-water fluids. The disposable fixture is restored before fluid ticks. Both selected Minecraft tests pass in each of two sessions (campaign clock 0 → 185 → 185 → 217), preserving the existing real-colony/political mapping checks alongside fresh terrain checks.

Full cloud setup/build, eight release-gate fixtures and source CI are recorded separately in VALIDATION.md/GitHub. Save/load stability of actual existing Tectonic/MineColonies worlds, normal client/Windows behavior, terrain-category colony construction, governor placement, geographic kingdom distribution, army movement, strategic roads and performance remain unverified. The 0.6.x release gate preserves all nine MineColonies requirements and adds all ten V5.3 combined requirements. Neither upstream terrain nor upstream construction counts as newly implemented Kingdoms features.

## Blueprint expansion preflight follow-up

/kingdoms plan inspects actual installed MineColonies blueprints in their original orientation, using upstream dimensions/primary offset and building-specific anchor rules. Before upstream positional lookup it guards every intersecting chunk, then requires one existing colony with the querying player's authoritative PLACE_HUTS, PLACE_BLOCKS and BREAK_BLOCKS permissions. Fresh full-footprint columns and volume/foundation checks reject >4 cut/fill, fluids, inventories/block entities and unbreakable blocks. Work is capped at 64 blocks per axis / 65,536 volume, one pending request and a 20-tick cooldown. No terrain/configuration mutation, permission writes, worker queues or construction are requested.

Actual upstream builder1 blueprint loading and asynchronous command responses are exercised in the real colony test. Deterministic disposable dry-platform fixtures verify successful owner preflight and permission/hazard/boundary rejection; they are not natural Tectonic valley or mountain construction evidence. Existing structure bounds, resources, accessibility, other mods' protection, rotations and new-colony founding remain pending. The screen is never construction authorization and adds no completed stage-6 acceptance group.
