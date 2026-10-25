# kingdomes

## Kingdoms of Minecraft

An incremental Minecraft **26.1.2** mod for **NeoForge 26.1.2.122**, using **Java 25**, ModDevGradle **2.0.148** and Gradle **9.2.1**. Full requirements: [master prompt](docs/MASTER_PROMPT.md). The large gameplay roadmap is unfinished; this branch contains the foundation and the first persistent kingdom-founding slice.

### Development

Use a JDK 25, then run `./gradlew build` (`gradlew.bat build` on Windows). `./scripts/test-core.sh` runs dependency-free campaign contracts. `python3 -m unittest discover -s scripts -p 'test_*.py' -v` checks the release guard. `./gradlew runGameTestServer` exercises actual mod loading and campaign autosave using an isolated test mod. `python3 scripts/test-lifecycle.py` runs two selected GameTest sessions and restores campaign data between them, proving campaign restart persistence. GameTest resets the vanilla test world; full world reload remains a separate check.

In Codex cloud, run `./scripts/cloud-setup.sh`; afterward use `python3 scripts/gradle-cloud.py <task>` to preserve platform proxy and certificate trust. TLS and artifact checksums remain enabled. No credentials are stored in the repository.

For manual play, use `./gradlew runClient`, create a test world, and run `/kingdoms status`. Wait for the clock to advance, save and exit, then reload and verify the saved step continues. `/kingdoms list` displays the 11–20 stored kingdoms and their founding clans, rulers and cultures. The current build does not spawn faction NPCs or implement recruitment, armies, territory or autonomous diplomacy. `/kingdoms politics` lists clan allegiances and recorded treaties; core war/peace transitions are persistent, but player political actions and autonomous decisions are unfinished.

### Progress and continuation

Run `python3 scripts/progress.py` for weighted verified progress. See [validation](VALIDATION.md), [current task](CURRENT_TASK.md), [roadmap](ROADMAP.md), [release plan](RELEASE_PLAN.md), and [resume instructions](AUTO_RESUME.md). No release is available until milestone acceptance is complete.
