# 0.1.0-alpha candidate — unreleased

Foundation campaign clock and schema 1 storage with corruption detection, plus `/kingdoms status`. No kingdoms, armies, recruitment, combat, world generation or custom assets are implemented yet.

Requires Minecraft 26.1.2, NeoForge 26.1.2.122 and Java 25. After verified release, put the mod JAR into the instance's `mods` folder. Do not install development sources or the MDK as a mod. Back up worlds before using any alpha.

Save format: schema 1, stored in `<world>/kingdomsofminecraft/campaign.dat`. No prior released save format exists. Future incompatible formats must be migrated explicitly; unknown formats currently stop loading instead of discarding data.

Windows singleplayer validation is required before publication. See VALIDATION.md for actual executed checks.
