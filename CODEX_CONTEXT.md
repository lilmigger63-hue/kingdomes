# Compact project context

Repository: lilmigger63-hue/kingdomes (only authorized destination). Existing cloud checkout: /workspace/kingdomes. Branch: development/foundation. Base main: 06b2e42633f40d1760b80e5aa7a09d4bf4303ae6.

Read CURRENT_TASK.md, progress.json and VALIDATION.md first; inspect only relevant source. User requests actual staged implementation, verified commits and prereleases, then continuation from checkpoints. Preserve README content and unrelated files. The cloud task is isolated; do not create another worktree unless requested.

Pinned stack: Minecraft 26.1.2, NeoForge 26.1.2.122, ModDevGradle 2.0.148, Gradle 9.2.1, Java 25. Official MDK commit: 070b9d29abd37dfcf2f3d94d24843a8654b80cbb. Toolchain under /workspace/toolchains/jdk-25+36. Use scripts/cloud-setup.sh and python3 scripts/gradle-cloud.py <task> for proxy/system trust. Never disable TLS or checksums or print credentials.

Main adapter: KingdomsOfMinecraft; pure core: campaign/CampaignState, persistence/CampaignStore. Schema 3 stores clock, kingdom records, separate clan allegiances and war/peace treaties; schema 1/2 saves migrate explicitly. KingdomFounding creates 11–20 initial realms. /kingdoms list inspects them. Core diplomacy transitions and /kingdoms politics exist; no autonomous political decisions, physical faction NPCs, territory or troops yet. Isolated gameTest test mod is excluded from the release JAR. Read ARCHITECTURE.md for storage invariants.

Main has not been merged and no release has been published. Actual remaining work lives in CURRENT_TASK.md and ROADMAP.md; do not regenerate working components. Read git log to recover after interruptions.
