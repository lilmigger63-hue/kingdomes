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


## Settlement sites / schema 4

A `Region` is a dimension-qualified 512-block square using floor division (including negative coordinates). `Settlement` records identify a site, owning kingdom, region and capital status. There is at most one site per region, one capital per kingdom, and 128 sites overall. Ownership is derived directly from sites, so transferring a site cannot leave a stale separate regional owner. Registering metadata never writes blocks, loads chunks, generates buildings or changes vanilla build permissions.

All campaign transitions carry the site registry. Capital designation atomically demotes the old capital of the same kingdom. The core conquest transition requires an existing war and clears the captured site's capital flag; it is not exposed as player conquest before siege gameplay exists.

Schema 4 appends site count and records (ID, modified-UTF name, owner ID, dimension identifier, region x/z, canonical capital byte) to schema 3's payload before CRC. Schemas 1–3 migrate with empty sites; no old campaign gains territorial claims automatically. The file bound is now 256 KiB, tested with maximal 20 kingdoms, 64 clans, 190 treaties and 128 sites including long UTF names/dimension identifiers. Older builds cannot load schema 4.

Operator setup commands require Minecraft's gamemaster permission; inspection is public. The Minecraft integration test runs at a remote unloaded region and checks that metadata operations do not load it.


## MineColonies V5.2 boundary / schema 5

V5.2 supersedes standalone physical settlement plans. The required, separately built port is pinned in dependencies/minecolonies.json. MineColonies owns physical identity, buildings, jobs, inventories and player permissions; Kingdoms owns political allegiance. MineColoniesAdapter reads live IColony metadata on the server thread. Its positional API guard avoids upstream getChunkAt for unloaded chunks; no new physical-state simulation or force-loading is introduced.

ColonyKey=(dimension, positive colony ID), not mutable name/center. CampaignState stores at most 128 ColonyBindings, with unique physical keys and known kingdom references, and carries them through every existing transition. Explicit operator binding never changes MineColonies permissions and rejects different-kingdom rebinding until safe conquest exists. Old site/capital setup is disabled; legacy records remain for migration, with no automatic mapping to colonies. Current colony snapshots are not serialized as competing physical state.

Schema 5 appends binding count and (dimension UTF, colony ID int, kingdom ID int) after schema 4 sites, before CRC. Older schemas gain an empty binding list and retain their state. Bounds and atomic replacement remain. Read/write of MineColonies world data remains exclusively its own responsibility; full lifecycle synchronization and integrated reload acceptance are pending.

The integration is GPL-3.0-only with retained notices. Dependency code, JAR and restricted assets never enter the Kingdoms JAR. The source-build helper rejects dirty reused source checkouts and verifies cached artifact checksums. Setup and Gradle resolve the same optional KINGDOMS_DEPENDENCY_CACHE root. Source cache is a build dependency cache, not a separate project checkout.
