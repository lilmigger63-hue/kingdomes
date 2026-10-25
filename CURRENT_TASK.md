# Current task

Stage: Foundation (0.1.0-alpha candidate, unreleased).

Completed locally: official MDK and Java 25 setup; campaign clock and file storage; 18 core checks; three release-gate tests; full NeoForge build; isolated Minecraft status/autosave test and controlled campaign restart across two server processes.

Checkpoint: tested foundation pushed as dc081212c15ee69e56916afa3018970f9b237390; draft PR https://github.com/lilmigger63-hue/kingdomes/pull/1. GitHub Build and Core contracts checks passed on that source. A later documentation-only checkpoint records these results. Repeatable setup and independent source review completed. Controlled campaign restart passed; full vanilla world reload is still unrun.

Active foundation acceptance: perform full vanilla world/client reload and Windows singleplayer checks and configure release-environment required reviewers. Do not mark 0.1.0-alpha released until these prerequisites and the guarded release workflow pass.

Release prerequisites: Windows singleplayer create/save/reload and manual gameplay validation; required reviewers on milestone-release GitHub environment; no release tag before recorded acceptance.

Next gameplay work: deterministic culture/clan/kingdom model and save schema expansion, then the early two-faction playable slice. Full specification in docs/MASTER_PROMPT.md.

No scheduled continuation configured; see AUTO_RESUME.md. No authoritative token counter. Do not claim milestone completion from core tests alone.
