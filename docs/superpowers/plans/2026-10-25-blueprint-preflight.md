# MineColonies blueprint preflight implementation plan

> Use superpowers:executing-plans to implement inline with test-first verification.

Goal: inspect an actual upstream building blueprint at a player's chosen anchor before a governor can request construction.
Spec: docs/TECTONIC_V5_3.md sections 2, 6–7; docs/MINECOLONIES_V5_2.md sections 4–5, 8–9.
Architecture: pure bounded footprint geometry; server-thread read-only inspector; asynchronous upstream blueprint IO followed by server-thread inspection. Keep normal MineColonies placement authoritative. This implements a preflight prerequisite, not automatic construction or complete placement acceptance.

Constraints: existing isolated development/foundation checkout, Java 25 / Minecraft 26.1.2 / NeoForge 26.1.2.122 and existing dependency pins. No dependency patches, world mutation, chunk tickets, new engine, remote generation or asset redistribution. No release-gate completion credit for this partial feature.

Design choice: inspect original blueprint orientation only; accept at most 64 blocks per axis and 65,536 volume, with a full loaded-chunk guard before world access. Require an existing single colony and the querying player's PLACE_HUTS/PLACE_BLOCKS/BREAK_BLOCKS permissions. Reject water/lava, protected block entities/unbreakable blocks and more than four blocks of cut/fill relative to the blueprint base. This conservative screen may reject valid specialist designs; resources, access, other mods' protection and actual construction remain pending.

Interfaces: BlueprintFootprint records bounds from dimensions, primary offset and world anchor. BlueprintSiteInspector.inspect(ServerLevel, ServerPlayer, BlockPos, Blueprint) returns immutable Result(passed, detail). BlueprintPreflightCommands exposes /kingdoms plan <pack> <blueprint>, captures the anchor and checks identity/dimension again after IO; one pending request and a server-wide cooldown bound work. World stop invalidates callbacks.

Review focus: negative chunk coordinates/oversized blueprints; async completion after dimension change or server stop; full-footprint unloaded boundaries; denied permissions despite operator status; hazards hidden between coarse samples. Tests pin geometry/bounds/paths and real loaded fixtures for the relevant inputs.

- [x] Task 1: Write and run failing pure tests for bounds, overflow, negative chunk coverage, budget limits and safe blueprint references; implement geometry/reference validation; run core suite.
- [x] Task 2: Write real GameTests for upstream blueprint dimensions/anchor, loaded boundaries, terrain relief, fluids, inventory preservation and colony permission denial; observe missing-feature failure; implement inspector and command; verify both Minecraft sessions and full build.
- [x] Task 3: Update checkpoint/docs without marking placement complete, fresh independent review, fix important findings with regression tests, verify and push to existing draft PR.

Ledger: resumed from 86c3841. Prior Tectonic plan is complete; no implementation repeated. Pre-flight: Task 2 consumes Task 1 bounds and reference validation; both explicitly use unrotated primary offset, inclusive bounds and a 65,536 block budget.

Task 1: complete — missing class RED, then 23 new footprint/reference checks GREEN (1,571 Java total).
Task 2: complete — missing inspector RED; full Gradle build GREEN, two Minecraft sessions × two selected tests GREEN, clock 0 → 32 → 32 → 80. Test-only platform replaces an incorrect natural-ground assumption and is restored. Strengthened async result assertions exclude exception feedback; clear() feedback check tolerates IO completing before clear without accepting later callbacks. Eight Python release checks GREEN. JAR audit GREEN.
Final review: independent read-only reviewer found no Critical/Important/Minor defects.
Final: Ruling: retain upstream creative-operator permission semantics — MineColonies owns permissions — different desired operator policy would require its configuration.
Final: Ruling: dimension-change/operator-specific fixtures remain uncovered — production guards and upstream policy were reviewed, existing direct unauthorized/lifecycle tests pass — corresponding regressions have less explicit coverage.
Final: Ruling: resources/access/structure-overlap/other-mod protection and construction stay pending — this screen is explicitly advisory — passing preflight cannot authorize construction.
Final: Ruling: use the upstream trusted-pack decoder before size rejection — no custom parser — decoding malformed installed packs is outside the inspection budget.

Task 3: complete — checkpoints/docs updated, independent review found no defects, full build/core/release/Minecraft checks and production JAR audit passed. Push uses the existing user-authorized development branch and draft PR; hosted source CI is checked separately. No merge/release.
