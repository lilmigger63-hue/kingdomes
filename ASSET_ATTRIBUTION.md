# Attribution and licensing

Kingdoms of Minecraft source and direct MineColonies integration are licensed GPL-3.0-only; full text in LICENSE. Build scaffolding derives from NeoForgeMDKs/MDK-26.1.2-ModDevGradle commit 070b9d29abd37dfcf2f3d94d24843a8654b80cbb; its template license remains in TEMPLATE_LICENSE.txt.

MineColonies is maintained by the LDT Team. The separately installed Forgotten409 community port is pinned in dependencies/minecolonies.json, with GPL-3.0 code and the upstream notices described in its LICENSE, NOTICE and LICENSE-THIRD-PARTY.md. No dependency source, dependency JAR or third-party art/data/blueprints is included in the Kingdoms repository or mod JAR. Players obtain the dependency separately and its restricted assets from upstream through the port's own provisioning flow. The dependency's publishJar is the correct external artifact; never redistribute its development JAR.

No proprietary Bannerlord assets/code are included. No custom visual assets have been added. Minecraft and NeoForge retain their upstream licenses. Existing world/site records are original test-generated data, not MineColonies assets.

Tectonic 3.0.28 and Lithostitched 1.8.1 are separately installed official NeoForge artifacts, listed as MIT in upstream Modrinth metadata and JAR metadata; Tectonic includes its Apollib dependency. Exact version IDs/URLs and published SHA-512 hashes are pinned in dependencies/terrain.json. None of their code, terrain data, assets or JARs is embedded in Kingdoms. No upstream terrain implementation was copied. The normal-noise/flat-preset fixture exists only in the isolated test mod and is not shipped in the production JAR.
