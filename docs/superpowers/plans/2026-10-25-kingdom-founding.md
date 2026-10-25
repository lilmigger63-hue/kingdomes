# Kingdom founding implementation plan

Goal: seed-based NPC clan founding with 11–20 persistent kingdoms and an inspectable game command.
Spec: docs/MASTER_PROMPT.md; user requests continuous native implementation.
Architecture: immutable campaign records, deterministic founding service, schema 2 storage with explicit schema 1 migration. No entities, chunk scans or ownership of player builds.

- [x] Write and observe failing tests for seed repeatability, 11–20 population, unique founding clans/kingdoms, six cultures, new founding constraints, defensive copies and persistence/migration.
- [x] Implement Culture, Clan, Banner, Kingdom, KingdomFounding and extend CampaignState while retaining the clock API.
- [x] Store actual kingdom records in bounded checksummed schema 2; migrate schema 1 in memory without rewriting on load. Reject invalid/future saves without modification.
- [x] Add /kingdoms list and extend the isolated game test to verify command and saved kingdoms across restart.
- [x] Run targeted tests, full build and two-session game test; review; update checkpoints and push source to the existing draft PR.

Review focus: migration must preserve seed/time; mutable caller lists cannot alter saved state; duplicate clans cannot found two kingdoms; malformed saved sizes/text/IDs must be rejected; founding must not rerun on every tick or reload. Diplomacy, territory and physical settlements remain unfinished and this slice does not complete the kingdoms milestone.

Local checks passed; source checkpoint is being pushed to existing PR #1. Release prerequisites remain separate and unverified.
