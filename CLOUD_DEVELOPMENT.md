# Continue while the PC is off

This repository contains the source and published 0.5.0 release. Uploading code does not move an existing local Codex conversation to a remote machine. A local desktop task stops when its computer sleeps or shuts down.

## One-time cloud setup

1. Sign in to ChatGPT and choose **Work in > Cloud > Select environment > Create environment** (or select an existing cloud environment).
2. Choose `lilmigger63-hue/kingdomes`, branch `main`, and let Codex prepare the project. Point setup at `living-settlements/` and require **JDK 25**.
3. Allow the package-download hosts needed by Gradle, NeoForge, Maven Central, and CurseMaven through the environment's normal network settings. Keep TLS verification enabled. Do not commit credentials.
4. Have setup run `cd living-settlements && bash scripts/build.sh`. The first run downloads the pinned dependencies. Review the result and resolve missing dependencies before publishing the environment.
5. Select **Publish**, wait for **Environment published**, then **Start a new task** and send a concrete development request. A running cloud task can continue while the local PC is asleep or off; a local task cannot.

No cloud task was started by this upload. Linux/cloud compilation is not claimed as verified by the Windows release checks. Cloud tasks complete the request they are given; this setup does not create an indefinite development loop or a paid always-on Minecraft server.

Official setup guide: https://learn.chatgpt.com/docs/cloud

## Suggested first cloud task

> Work on the Living Settlements addon in living-settlements/. Read AGENTS.md, README.md and docs/validation-0.5.0.md. Preserve all pinned versions. Reproduce the JUnit/build checks under Linux with JDK 25 and investigate any reproducible failures. Do not change gameplay merely to make setup pass. If native tests cannot run because building assets are absent, report the exact prerequisites instead of claiming success. Put any necessary fixes on a codex/ branch and open a pull request with test evidence. Keep the existing 0.5.0 release unchanged.

For gameplay work, supply the specific next feature or reproducible bug and the relevant world observations. Remaining manual validation includes full surface-generated terrain, complete roads/bridges/farms, long-term economy, multiplayer stress, and full restart/crash behavior; see the validation report.
