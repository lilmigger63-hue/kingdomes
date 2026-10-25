# 0.1.0-alpha candidate — unreleased

Foundation campaign clock and schema 1 storage with corruption detection, plus `/kingdoms status`. Adds 11–20 seed-based kingdom foundations, stored clan/ruler/culture/banner definitions and `/kingdoms list`. Separate clan allegiances, core war/peace transitions and /kingdoms politics are implemented. Physical NPCs, autonomous diplomacy, armies, recruitment, combat, world generation and custom visual assets are not implemented yet.

Requires Minecraft 26.1.2, NeoForge 26.1.2.122 and Java 25. After verified release, put the mod JAR into the instance's `mods` folder. Do not install development sources or the MDK as a mod. Back up worlds before using any alpha.

Save format: schema 3, stored in `<world>/kingdomsofminecraft/campaign.dat`. Schema 1 saves retain their seed/clock and receive initial kingdoms; schema 2 retains actual kingdoms and assigns founding clan allegiances. Both initialize peaceful diplomacy and upgrade to schema 3 on their next save. Back up before upgrading: old builds cannot read schema 3. Future incompatible formats must be migrated explicitly; unknown formats stop loading instead of discarding data.

Windows singleplayer validation is required before publication. See VALIDATION.md for actual executed checks.
