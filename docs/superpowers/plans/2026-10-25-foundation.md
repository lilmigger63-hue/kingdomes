# Foundation implementation plan

Goal: establish a buildable NeoForge mod with tested, versioned campaign storage.
Architecture: keep deterministic campaign state independent of Minecraft; adapt world lifecycle separately.
Tech stack: official MDK commit 070b9d29abd37dfcf2f3d94d24843a8654b80cbb; Minecraft 26.1.2; Java 25; NeoForge 26.1.2.122; ModDevGradle 2.0.148; Gradle 9.2.1.
Spec: docs/MASTER_PROMPT.md. User requests immediate native implementation.

## Constraints
Only lilmigger63-hue/kingdomes is an authorized upload destination. Preserve survival and unrelated files. No release before build, startup and persistence validation. No fabricated token counters or progress.

## Tasks
- [x] Establish baseline MDK build using Java 25 and verified downloads.
- [x] Write campaign storage tests: roundtrip, truncated data, unsupported versions, invalid counters and atomic replacement. Observe failure, implement and rerun.
- [x] Integrate tested storage with world lifecycle only after checking actual 26.1.2 APIs; verify controlled campaign restart in two GameTest servers. Full world/client reload remains separate manual acceptance.
- [x] Add build/test/release workflows; restrict release to a verified milestone tag and approved environment.
- [ ] Update state documents, commit and push validated work; retain artifacts if publication is blocked.

## Review focus
Corrupt saves must fail explicitly without replacement. Future schemas must not be silently downgraded. Separate worlds must not share state. Interrupted writes must preserve the prior save. Headless tests do not establish Windows gameplay or performance.

Later milestones remain in ROADMAP.md. Network or credential blockers are recorded in CURRENT_TASK.md.
