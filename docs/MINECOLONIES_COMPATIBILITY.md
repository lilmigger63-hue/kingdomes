# MineColonies compatibility investigation (2026-10-25)

V5.2 instructions in MINECOLONIES_V5_2.md override custom settlement construction and the old no-gameplay-dependency requirement for this integration. Target remains Minecraft 26.1.2, NeoForge and Java 25.

## Candidates

- Official ldtteam/minecolonies: inspected default version/main (Minecraft 1.20.1/Java 17), version/1.21 (Minecraft 1.21.1/Java 21), branch list and recent official release tags (1.20.1/1.21.1). No official 26.1.2 build found in these sources. The Modrinth minecolonies slug only lists 1.18.2 and has no source URL, so it is not authoritative evidence of current official release availability.
- Supplied community port: Forgotten409/NeoForge-MineColonies-Unofficial, commit 9901957777e3dc8cf8119826c619654a48ae486e, version 1.1.1403-port26.1.2-0.7.1. Minecraft 26.1.2, Java 25, NeoForge 26.1.2.109, NeoGradle 7.1.38, Gradle 9.2.1. Latest v0.7.1 release has no downloadable asset; v0.6.0 has a published JAR but predates server fixes documented by the maintainer.

## Build and boot evidence

Pinned port source compiled and publishJar passed in 3m 27s with TLS/system trust and platform proxy enabled. No upstream source modifications. The publish JAR SHA-256 is 6019926009e7924b6dca854f85e719efe985b88961b19863a57a583a62d7f327. It is retained outside this repository under /workspace/.dependency-cache/minecolonies/<commit>/.

The first runtime probe did not include the dependency in the isolated GameTest source set; it is explicitly discarded as dependency evidence. The corrected probe adds it to gameTestRuntimeOnly. MineColonies then loaded beside Kingdoms on NeoForge 26.1.2.122 and the selected Kingdoms test passed. Missing external assets caused missing-loot-table diagnostics, so this is not construction readiness. Supported serverAutoDownload fetched upstream MineColonies assets, TownTalk voices and the two style packs into ignored runtime storage. Asset conversion and the subsequent boot passed; the prior missing-loot-table diagnostics disappeared. Direct typed API discovery and explicit political binding then passed in two selected Minecraft sessions with a real colony. This verifies initial compatibility/discovery/mapping, not construction UI, governor AI or full synchronized world reload.

## Dependency and licensing notes

The port merges MineColonies, Structurize, BlockUI, Domum Ornamentum and Multi-Piston; its metadata requires Minecraft/NeoForge, while JEI, JourneyMap and Dynamic Trees are optional. Source build additionally resolves JEI and JourneyMap API and Dynamic Trees compile dependencies. Optional runtime shader/performance mods are not needed for the adapter probe.

Upstream LICENSE and the port LICENSE are GPL-3.0 texts. Port NOTICE and LICENSE-THIRD-PARTY.md identify GPL merged code and separately restricted MineColonies assets/data/blueprints, TownTalk and external style packs. Use publishJar only for the separately installed dependency; do not embed it or its assets in Kingdoms releases. Kingdoms now uses GPL-3.0-only for compatible linked distribution, with full LICENSE text and retained upstream/template attribution. The earlier All Rights Reserved declaration is superseded. No third-party source/assets were copied into this repository.

## Public extension points inspected

- IColonyManager.getInstance(), getColonies(Level), getColonyByDimension(id, dimension), getColonyByPosFromWorld(Level, BlockPos).
- IColony.getID(), getName(), getDimension(), getCenter(), getCitizenManager(), getServerBuildingManager(), getPermissions().
- ICitizenManager.getCurrentCitizenCount(); registered building manager getBuildings()/getTownHall(). Read metadata without calling chunk-loading or tile-entity retrieval methods.
- IMinecoloniesAPI.getInstance().getEventBus() is MineColonies' own event bus (not NeoForge.EVENT_BUS); ColonyCreatedModEvent/ColonyDeletedModEvent supply IColony.
- IColonyManager.createColony requires a Player and blueprint pack. IPermissions.setOwner also requires a Player. AI-only founding and conquest therefore need a supported player/governor model or separately reviewed dependency extension; never patch save NBT to bypass ownership.

## Integration boundary

MineColonies owns physical colonies/buildings/jobs/citizens/inventories/permissions. Kingdoms owns political allegiance and campaigns. Use (dimension, colony ID) as the physical identity, with an explicit political mapping; names/centers can change and are not identities. Read population/buildings/owner from MineColonies, not a second simulated physical registry. Existing standalone site records are legacy strategic metadata, not proof that a colony exists. Do not map closest colonies or change permissions automatically, duplicate guards as recruits, or force-load colonies to synchronize them.

Full 0.6.0-alpha acceptance remains discovery, mapping, building reuse, governor construction, military logistics/recruitment, synchronized saves, safe conquest and performance, in addition to a compatible dependency. Successful boot alone is insufficient.

## Adapter validation

The adapter uses typed IColonyManager/IColony APIs and stores only (dimension, colony ID) → kingdom ID political mappings in schema 5. It does not write MineColonies permissions, buildings, citizens or inventories. Legacy standalone site creation is disabled; old records remain readable but do not become colonies automatically.

Actual tests created a real colony through the public API, read authoritative name/owner/population/building metadata, rejected non-operator binding and conflicting rebinding, and checked owner permissions stayed unchanged. Two server sessions preserved a custom political mapping and resumed clock 94 → 203. The test uses NeoForge FakePlayer only for test ownership; upstream logs subscriber warnings for this actor. This is not proof of production AI governors. Vanilla mock login was unsuitable because it lacks negotiated mod payload channels.

The positional API implementation calls getChunkAt. A test caught its force-load behavior; the adapter now checks hasChunk before calling it and reports skipped lookup for unloaded positions. The remote unloaded-region test now passes. No unsafe save manipulation or dependency patch was used.

The reproducible setup script passed a cache-miss clone/build from the exact pin, uses the Kingdoms checksummed Gradle wrapper, records/checks the built JAR digest, and supports KINGDOMS_DEPENDENCY_CACHE consistently in setup and Gradle. Review found dirty source could survive checkout; source cleanliness is now checked before building. A deliberate dirty build first succeeded (reproducing the defect), then was refused after the fix. External-cache compilation first failed with the project-local JAR absent, then passed after cache resolution was fixed.

Full vanilla+MineColonies world reload, player build tools, builder jobs, warehouse requests/upgrades, governors, logistics/recruitment, safe conquest and performance remain unverified. Do not mark the 0.6.0-alpha milestone complete.
