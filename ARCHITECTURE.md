# Architecture

`com.kingdomsofminecraft.KingdomsOfMinecraft` owns lifecycle hooks on the logical server thread. Campaign state is loaded from `<world>/kingdomsofminecraft/campaign.dat`, advances once per 20 server ticks and is saved every 30 simulation steps and during shutdown. `/kingdoms status` reports the clock.

`campaign/CampaignState` is immutable and independent of Minecraft APIs. `persistence/CampaignStore` implements schema 1: big-endian magic (4 bytes), schema (4), seed (8), step (8), CRC32 stored in a long (8). Only exactly 32 bytes are accepted. Future schemas fail closed until explicit migrations are implemented. A temporary file is forced to storage before atomic replacement. Unsupported atomic replacement fails rather than using a non-atomic fallback. Directory durability across power loss and concurrent writers are not guaranteed; the owning server is the sole writer.

Strategic armies, settlements and populations will remain simulation data. Physical entities must be bounded projections of that data, with no duplicate troop ownership. No chunk loading or global scans are introduced by the foundation.

Tests under `src/coreTest` exercise real files without external libraries. Gradle `check` invokes `coreTest`; `scripts/test-core.sh` runs the same tests independently.
