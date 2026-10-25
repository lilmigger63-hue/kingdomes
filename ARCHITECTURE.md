# Architecture

`com.kingdomsofminecraft.KingdomsOfMinecraft` owns lifecycle hooks on the logical server thread. Campaign state is loaded from `<world>/kingdomsofminecraft/campaign.dat`, advances once per 20 server ticks and is saved every 30 simulation steps and during shutdown. `/kingdoms status` reports the clock.

`campaign/CampaignState` holds the immutable clock and kingdom list. `KingdomFounding` deterministically creates 11–20 founding clans with original realm names, rulers, six cultural/racial identities and geometric banner definitions. `foundKingdom` supports adding a clan-founded realm while rejecting duplicate clans/names/IDs and populations over 20. These are strategic records; no physical NPCs, capitals, territorial claims or diplomacy exist yet. Clock advancement preserves actual records rather than generating them again.

`persistence/CampaignStore` writes schema 2: big-endian magic and schema ints, seed/step longs, kingdom-count int, then kingdom/clan IDs, modified-UTF names/ruler/culture and three banner ints per record. CRC32 over the entire payload is appended as an eight-byte long. The loader bounds files to 64 KiB and kingdom counts to 20, validates record invariants, and rejects extra payload, unsupported schemas and corruption without modifying the file. Schema 1 (32-byte clock-only format) explicitly migrates by preserving its seed/time and founding initial kingdoms; load itself never writes. The next normal save writes schema 2. Backward loading of schema 2 in old builds is unsupported.

The temporary file is forced before atomic replacement. Unsupported atomic replacement fails without an unsafe fallback. Parent-directory durability across power loss and concurrent writers are not guaranteed; the owning server is the sole writer.

Strategic armies, settlements and populations will remain simulation data. Physical entities must be bounded projections of that data, with no duplicate troop ownership. No chunk loading or global scans are introduced by the foundation.

Tests under `src/coreTest` exercise real files without external libraries. Gradle `check` invokes `coreTest`; `scripts/test-core.sh` runs the same tests independently.


## Political state / schema 3

CampaignState owns a separate allegiance registry (at most 64 clans). Founders must retain matching clan records and remain in their own kingdoms; other clans can join/transfer but never belong to multiple realms. New founding and clock advancement preserve existing vassals/treaties. Diplomacy is one canonical unordered record per kingdom pair, at most 190. Omitted pairs are peaceful; war/peace changes use the campaign clock, and repeated declarations preserve their original timestamp. Unknown/self pairs, duplicate records and future treaty times are rejected.

Schema 3 keeps the schema 2 kingdom payload, then stores clan count and records (clan ID/names/culture plus kingdom ID), followed by treaty count and records (two IDs, canonical byte 0/1 for war and changed-at long), then checksum. Schema 2 migrates by assigning founders to their kingdoms and starting at peace; schema 1 additionally generates initial kingdoms. Neither migration writes on load. File bound remains 64 KiB; maximal valid state with long multibyte names roundtrips in tests. Old builds cannot read schema 3.

`/kingdoms politics` is read-only. There is no autonomous diplomacy yet: real military, territory and economy inputs must drive that later. No arbitrary war timers are substituted for absent strategy.
