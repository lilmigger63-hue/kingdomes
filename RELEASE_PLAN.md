# Releases

Only https://github.com/lilmigger63-hue/kingdomes/releases is authorized.

Candidates: 0.1.0-alpha foundation; 0.2.0-alpha kingdoms; 0.3.0-alpha armies; 0.4.0-alpha tactical AI; 0.5.0-alpha combat/magic; 0.6.0-alpha settlements; 0.7.0-alpha sieges; 0.8.0-alpha economy; 0.9.0-beta visuals; 1.0.0 final integration.

Do not tag a milestone until its build, automated contracts, mod startup, world save/reload and Windows singleplayer checks have passed and `release-status.json` records their evidence. Configure required reviewers on the GitHub `milestone-release` environment before using the release workflow. The current integration cannot create environments (HTTP 403 Resource not accessible by integration). The owner must create this environment under repository Settings → Environments and add a required reviewer; restrict deployments to release tags `v*`. CI checks that reviewer protection exists before publishing. Tag publication is authorized only after these criteria; no tags have been pushed yet.

Release CI rebuilds and checks acceptance, then uploads a new prerelease with `gh release create`. Existing releases are never edited or replaced. Stable 1.0.0 will need a deliberate stable release workflow update after complete acceptance. Each release must include installation, compatibility, limitations and migration notes. Keep older releases available.
