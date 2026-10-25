# Current task

Stage: Kingdoms — political state slice (milestone incomplete). Foundation platform/release acceptance remains pending.

Completed: 11–20 persistent kingdom foundations; clan allegiance with join/transfer constraints and ruling-clan protection; symmetric, idempotent war/peace treaty transitions and timestamps; schema 3 storage with explicit schema 1/2 migration. /kingdoms list and /kingdoms politics inspect state. All political changes are core operations; autonomous diplomatic decisions and player political actions are not implemented.

Verification: full build; 1,483 Java checks (21 persistence, 1,411 founding, 30 malformed-save, 21 politics); three release-gate tests. Two selected Minecraft sessions preserve a custom vassal/war treaty, including treaty timestamp, and resume clock 98 → 198. Independent read-only review found no important defect. Source CI is checked separately after push.

Next: settlement/capital records and coarse territorial claims to supply actual strategic inputs, then army strength/supplies before autonomous war/peace decisions. Do not introduce arbitrary war timers or pretend absent strategic data exists. Continue the early playable slice with recruitment and commandable troops after political foundations.

Release prerequisites unchanged: full vanilla world/client reload, Windows singleplayer checks, owner-configured required reviewers on milestone-release. No release/tag or scheduler is enabled.

Branch development/foundation; draft PR https://github.com/lilmigger63-hue/kingdomes/pull/1. Exact checkpoint: git HEAD/remote ref. Token counters unavailable. Preserve existing work; do not recreate the foundation.
