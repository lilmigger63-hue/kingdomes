# Development log

## 2026-10-25 — Foundation start

Inspected original README-only repository and preserved its contents. Imported official MDK build scaffolding, pinned versions and wrapper checksum. Saved original full requirements. Wrote core persistence contracts before implementation and observed their initial failure. Implemented immutable campaign time and checksummed atomic storage, then added logical-server lifecycle and status command. Added isolated Minecraft integration tests and guarded build/release CI. First full build passed; final setup/CI verification is ongoing.

Ruling: use existing isolated cloud checkout and a development branch because this environment is already isolated and the user requests immediate implementation.
Ruling: keep test fixtures in separate source sets so development tests do not ship in the mod JAR.
Ruling: future schemas fail explicitly until a real migration exists; there are no previous released schemas to migrate.
Ruling: milestone release stays blocked until Windows singleplayer acceptance is observed; headless checks cannot establish that result.

No release or automated continuation is claimed. Token usage is unavailable.

Final local validation: 18 Java checks, 3 Python tests, full build, repeatable setup and two selected Minecraft test sessions passed. Controlled restart steps: 0 → 233 → 233 → 411. No definite blocking defect from independent review.

Ruling: restore campaign data only inside the isolated test mod to validate production startup after GameTest resets its universe; full vanilla world/client reload remains unaccepted. Required repository release-environment configuration is owner action because the integration received HTTP 403.
