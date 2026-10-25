# Current task

Active specs: docs/TECTONIC_V5_3.md (terrain), docs/MINECOLONIES_V5_2.md (construction), docs/MASTER_PROMPT.md (remaining game). No new terrain/noise/continent/river or colony construction engine.

Completed: pinned official Tectonic 3.0.28 (RNLd3nvP) and Lithostitched 1.8.1 (pLlJIqyp), upstream SHA-512 checks; required separate artifacts, Apollib embedded upstream; original MineColonies candidate/discovery/political mapping preserved. All run on Minecraft 26.1.2 / NeoForge 26.1.2.122 / Java 25. Run scripts/setup-minecolonies.py and scripts/setup-terrain.py before builds; cloud-setup does both.

New geography adapter: server-thread, loaded-only 25-column observations of surface relief/water/forest/river/ocean biome tags, bounded LRU/TTL, cleared across worlds. /kingdoms site <role> is read-only and advisory. It does not check full blueprint footprint, protection, resources or routes and never authorizes construction. Cached data may be up to 1,200 ticks old; lava/non-water fluid observations are rejected.

Verified: pure role fixtures and cache invariants; real active Tectonic density data; four fresh normal-noise probe chunks per test run with both required mods loaded. Real probe is ocean and is correctly rejected for villages. Tests include non-water-fluid regression, no production remote generation/block writes, existing real-colony/mapping checks, and controlled campaign restart. GameTest uses a test-only flat preset with extra normal-noise dimension because it ignores standalone dimension datapacks; this never ships in the mod JAR.

Next: validate real mountain/valley/river/coast placement with MineColonies blueprint-fit/protection/resources/permissions, then governor placement and coarse geographic kingdom distribution. Add region/route aggregation and real army movement only after the campaign/army inputs exist. Do not claim current 16x16 advisory cells are a full settlement plan or true mountain-pass/route detector. Full world/client reload and performance remain unverified.

Existing MineColonies source pin 9901957777e3dc8cf8119826c619654a48ae486e / 1.1.1403-port26.1.2-0.7.1 remains. Colony permissions/physical state stay authoritative there; no unsafe NBT edits, guard/troop duplication or world-wide scans. Read docs/MINECOLONIES_COMPATIBILITY.md rather than repeating research.

0.6.x release gate requires both original MineColonies nine checks and V5.3 ten combined checks. Many features and Windows/full-world acceptance/reviewer protection are pending. No release, tag or scheduler enabled. Branch development/foundation; draft PR https://github.com/lilmigger63-hue/kingdomes/pull/1. Exact checkpoint git HEAD/remote; authoritative token usage unavailable.
