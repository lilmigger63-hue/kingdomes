# Architecture

`com.kingdomsofminecraft.KingdomsOfMinecraft` owns lifecycle hooks on the logical server thread. Campaign state is loaded from `<world>/kingdomsofminecraft/campaign.dat`, advances once per 20 server ticks and is saved every 30 simulation steps and during shutdown. `/kingdoms status` reports the clock.

`campaign/CampaignState` holds the immutable clock and kingdom list. `KingdomFounding` deterministically creates 11–20 founding clans with original realm names, rulers, six cultural/racial identities and geometric banner definitions. `foundKingdom` supports adding a clan-founded realm while rejecting duplicate clans/names/IDs and populations over 20. These are strategic records; no physical NPCs, capitals, territorial claims or diplomacy exist yet. Clock advancement preserves actual records rather than generating them again.

`persistence/CampaignStore` writes schema 2: big-endian magic and schema ints, seed/step longs, kingdom-count int, then kingdom/clan IDs, modified-UTF names/ruler/culture and three banner ints per record. CRC32 over the entire payload is appended as an eight-byte long. The loader bounds files to 64 KiB and kingdom counts to 20, validates record invariants, and rejects extra payload, unsupported schemas and corruption without modifying the file. Schema 1 (32-byte clock-only format) explicitly migrates by preserving its seed/time and founding initial kingdoms; load itself never writes. The next normal save writes schema 2. Backward loading of schema 2 in old builds is unsupported.

The temporary file is forced before atomic replacement. Unsupported atomic replacement fails without an unsafe fallback. Parent-directory durability across power loss and concurrent writers are not guaranteed; the owning server is the sole writer.

Strategic armies, settlements and populations will remain simulation data. Physical entities must be bounded projections of that data, with no duplicate troop ownership. No chunk loading or global scans are introduced by the foundation.

Tests under `src/coreTest` exercise real files without external libraries. Gradle `check` invokes `coreTest`; `scripts/test-core.sh` runs the same tests independently.
