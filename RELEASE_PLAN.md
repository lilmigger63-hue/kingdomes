# Releases

Only https://github.com/lilmigger63-hue/kingdomes/releases is authorized.

Candidates: 0.1.0-alpha foundation; 0.2.0-alpha kingdoms; 0.3.0-alpha armies; 0.4.0-alpha tactical AI; 0.5.0-alpha combat/magic; 0.6.0-alpha MineColonies and Tectonic Integration; 0.7.0-alpha sieges; 0.8.0-alpha economy; 0.9.0-beta visuals; 1.0.0 final integration.

Do not tag a milestone until its build, automated contracts, mod startup, world save/reload and Windows singleplayer checks have passed and `release-status.json` records their evidence. Configure required reviewers on the GitHub `milestone-release` environment before using the release workflow. The current integration cannot create environments (HTTP 403 Resource not accessible by integration). The owner must create this environment under repository Settings → Environments and add a required reviewer; restrict deployments to release tags `v*`. CI checks that reviewer protection exists before publishing. Tag publication is authorized only after these criteria; no tags have been pushed yet.

Release CI rebuilds and checks acceptance, then uploads a new prerelease with `gh release create`. Existing releases are never edited or replaced. Stable 1.0.0 will need a deliberate stable release workflow update after complete acceptance. Each release must include installation, compatibility, limitations and migration notes. Keep older releases available.

MineColonies 0.6.0-alpha acceptance: compatible Minecraft 26.1.2 dependency; discovery; colony/kingdom mapping; building reuse; AI governor; military recruitment/logistics without duplication; synchronization; safe conquest; measured performance. No older Minecraft substitution, bundled dependency JAR or copied restricted assets. Exact dependency/version and license conditions must be recorded before publishing integration.

V5.3 adds ten combined integration checks to the existing nine MineColonies checks. Every 0.6.x tag requires both acceptance sets plus the ordinary platform/build/world checks. Verify fresh Tectonic worlds, real terrain-category construction, geographic distribution/movement/routes, save stability and performance before release; simple advisory screening and upstream features are not milestone completion.
