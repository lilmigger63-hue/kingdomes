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

Confirmed source push dc081212c15ee69e56916afa3018970f9b237390 and draft PR #1. GitHub Build and Core contracts succeeded on fresh runners. Verified weighted progress: 4.0% (four of five foundation acceptance groups, all later stages unfinished). No release; platform acceptance and owner-configured required reviewer protection remain pending.

## 2026-10-25 — Kingdom founding slice

Implemented seed-based clan founding of 11–20 realms, immutable identity constraints, cultures/rulers/banner definitions, schema 2 storage with explicit schema 1 migration and /kingdoms list. Verified 1,450 Java checks, three release-gate tests, full build and two selected Minecraft sessions with custom record preservation. Reviewed independently; strengthened restart test to expose accidental regeneration.

Ruling: continue independent kingdom implementation while manual foundation release checks remain pending; no release milestone is marked complete. Kingdoms stage split into six transparent acceptance groups; only founding records/persistence count as verified (2 percentage points, total 6%). Generated heraldry and rulers are definitions, not rendered banners or physical NPC entities. Next: persistent diplomacy and clan relationships.

## 2026-10-25 — Political state slice

Implemented persistent clan membership/transfer and ruler protection, canonical war/peace transitions, schema 3 migrations and read-only politics command. Verified 1,483 Java checks, three release-gate tests, full build and real Minecraft political-state restart. Review found no important defect.

Ruling: implement explicit treaty transitions before autonomous decisions; real army/territory/supply conditions are absent and must not be fabricated. Only clan allegiance transitions/persistence now count as another Kingdoms acceptance group (8% total); autonomous diplomacy remains unverified. Full release gates remain pending.

## 2026-10-25 — Settlement ownership slice

Added dimension-aware regions, exclusive settlement ownership, capitals, wartime transfer core transition, schema 4 migration and permission-checked operator setup/public inspection. Verified 1,510 Java checks, three release checks, full build and real Minecraft command/restart behavior. Reviewer found no important defect.

Ruling: settlement registration claims only explicit strategic metadata, with no automatic claims on existing land and no block protection or building generation. Claims derive from site ownership to avoid duplicated owner state. Verified settlement registry/ownership/capitals adds one Kingdoms group (10% total); physical settlements and autonomous diplomacy remain incomplete. Next prioritize playable recruitment/army slice to supply real strategy inputs.
