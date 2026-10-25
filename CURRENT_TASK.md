# Current task

Active spec: docs/MINECOLONIES_V5_2.md overrides custom settlement construction in docs/MASTER_PROMPT.md. No new building/blueprint/citizen engine. Preserve other Kingdoms systems and the existing no-scheduler recovery instructions.

Completed this slice: official source/release compatibility research; pinned community MineColonies 26.1.2 source build and asset-provisioned boot; typed read-only colony discovery; operator political mapping keyed by dimension + colony ID; schema 5 migration from 1–4. No physical state/permissions are duplicated or rewritten. Old standalone registration/capital commands are disabled, with legacy data preserved.

Dependency: Forgotten409/NeoForge-MineColonies-Unofficial commit 9901957777e3dc8cf8119826c619654a48ae486e, version 1.1.1403-port26.1.2-0.7.1, separate publishJar. Run python3 scripts/setup-minecolonies.py before building; cloud-setup does this automatically. Compatible distribution now GPL-3.0-only, notices retained; no vendor source/JAR/assets embedded. See docs/MINECOLONIES_COMPATIBILITY.md for reproducible evidence.

Commands: /kingdoms colonies (or settlements), /kingdoms colonies bind <colonyId> <kingdomId> (gamemaster permission, current dimension), /kingdoms territory in loaded chunks. Mapping does not transfer MineColonies player ownership. Rebinding is rejected until conquest synchronization is implemented.

Local checks: 1,528 Java checks, six release-gate tests, full build and reproducible dependency cache-miss build; two selected real Minecraft sessions create a real colony, read metadata, preserve player ownership, reject unauthorized/conflicting mapping and retain custom political binding across campaign restart. Last clock 0 → 94 → 94 → 203. FakePlayer is test-only and causes upstream subscriber warnings; not a production AI governor. Full vanilla+MineColonies save restoration remains unverified (GameTest resets its world).

Next: implement event-based colony lifecycle synchronization and real world save/reload validation, then supported governor construction/ownership policy through public APIs. Investigate Player-requiring APIs before automating AI establishment or conquest; do not manipulate colony NBT or assume fake-player test success proves production governor support. Recruitment/logistics must not duplicate guards or warehouse items. Normal client tools/builders/upgrades and performance need actual validation.

Do not release 0.6.0-alpha until all nine MineColonies acceptance groups pass. Release gate enforces them. Foundation Windows/full world checks and owner-configured milestone-release reviewer protection remain pending; no release/tag/scheduler enabled.

Existing branch development/foundation, draft PR https://github.com/lilmigger63-hue/kingdomes/pull/1. Exact checkpoint: git HEAD/remote. Authoritative token usage unavailable; do not restart completed investigation.
