# Repository working instructions

The published MineColonies addon lives in `living-settlements/`. Start there for Living Settlements tasks. The independent Kingdoms foundation exists on `development/foundation`; do not replace it or mix its build with this addon without a specific request.

Read `living-settlements/README.md`, `living-settlements/gradle.properties`, and `living-settlements/docs/validation-0.5.0.md` before changing the addon.

Use JDK 25 and the checked-in Gradle 9.2.1 wrapper. From `living-settlements/`, Linux/cloud: `bash scripts/build.sh`; Windows: `./scripts/build.ps1`. Keep Minecraft 26.1.2, NeoForge 26.1.2.124, NeoGradle 7.1.38, and MineColonies unofficial port 1.1.1399-port26.1.2-0.6.0-publish pinned. The fallback CurseMaven dependency is intentional; do not silently substitute upstream MineColonies or another port.

Inspect actual dependency classes/source before using MineColonies APIs. Never claim behavior works from compilation alone. Separate unit tests, native server tests, and manual in-game verification in reports. Do not disable tests or certificate validation to obtain a green build.

Normal Linux validation is `bash scripts/build.sh` (Gradle build includes JUnit). The full native suite additionally needs the external building assets and exact terrain mods described in the validation report. Report missing assets honestly. Do not redistribute dependency JARs, downloaded building packs, user worlds, credentials, caches, or local logs.

Preserve player colonies, existing saves, and player edits. Keep generation and worker processing bounded per tick. Retain finite starting supplies and real native construction mechanics. Read the report's remaining limitations before choosing follow-up work. Do not claim physically absent visitors, offline simulation, or untested terrain can run full native construction.

For a requested change, implement it, run relevant checks, and report exactly what remains untested. Use `codex/` branches and pull requests for future work. Do not publish a new release merely because a task started; release only verified, requested changes.
