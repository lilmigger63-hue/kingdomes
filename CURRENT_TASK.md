# Current task

Stage: Kingdoms — settlement ownership slice (milestone incomplete). Foundation platform/release acceptance remains pending.

Completed: persistent kingdom/clan/treaty data plus up to 128 settlement sites, exclusive dimension-qualified 512-block region ownership, capital replacement and validated wartime ownership transfer core operation. All existing transitions preserve sites. Schema 4 migrates schemas 1–3 with empty sites and retains political state.

Commands: /kingdoms list shows kingdom IDs. Operators can use /kingdoms settlement <kingdomId> <name> at their current position (quote names with spaces), and /kingdoms capital <settlementId>. Public /kingdoms settlements and /kingdoms territory inspect metadata. No buildings, block protection, world generation or live conquest command exists. Registration does not change blocks or load chunks.

Verified: full build; 1,510 Java checks (21 persistence, 1,411 founding, 34 malformed saves, 23 politics/capacity, 21 territory); three Python release checks; two selected Minecraft sessions with operator permission rejection, remote chunk non-loading and custom settlement/capital restart. Clock 0 → 32 → 32 → 83. Independent review found no important defect. New source CI is checked after push.

Next: build the early playable recruitment/army slice and settlement recruitment reserves, then derive actual military strength/supplies before autonomous strategic diplomacy. Political influence and full Kingdoms integration remain unfinished. Preserve survival compatibility and do not substitute arbitrary war timers for missing strategy.

Release gates remain pending: full world/client reload, Windows singleplayer, owner-required reviewer protection for milestone-release. No tag, release or scheduler enabled.

Branch development/foundation, draft PR https://github.com/lilmigger63-hue/kingdomes/pull/1. Exact checkpoint: git HEAD/remote. Authoritative token usage unavailable. Read this and CODEX_CONTEXT.md first; do not recreate completed work.
