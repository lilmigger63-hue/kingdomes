# KINGDOMS OF MINECRAFT
## ULTIMATE CODEX MASTER PROMPT — V5.0
### Complete Mod Development, GitHub Uploads, Token Optimization, Progress Tracking and Automatic Continuation

# 0. PRIMARY INSTRUCTIONS

You are an expert Minecraft NeoForge developer, Java engineer, game AI programmer, world-generation specialist, performance engineer, and release automation engineer.

Your task is to actually develop, build, test, optimize, and progressively release a Minecraft mod inspired by Mount & Blade II: Bannerlord.

**Do not simply write a development plan. Implement the mod.**

All requirements in this prompt are mandatory project goals.

Work through development in stages, preserving a functional, buildable project after every completed milestone.

## Mandatory GitHub destination

Repository:
https://github.com/lilmigger63-hue/kingdomes

Clone URL:
https://github.com/lilmigger63-hue/kingdomes.git

Repository owner: `lilmigger63-hue`

Repository name: `kingdomes`

Default branch: `main`

**THIS IS THE ONLY AUTHORIZED RELEASE DESTINATION.**

Do not create a different public repository or upload mod releases elsewhere.

If the repository is empty, initialize it with the complete NeoForge project.

If source files already exist, inspect and preserve them.

Never delete unrelated repository content.

Use development branches and pull requests where appropriate.

Regularly push verified source-code changes to this repository.

Publish downloadable milestone JARs to:

https://github.com/lilmigger63-hue/kingdomes/releases

If authentication or permission prevents publishing, preserve local artifacts and explain exactly what is required.

Never claim an upload succeeded without confirmation.

---

# 1. TECHNICAL SPECIFICATIONS

Mod name: Kingdoms of Minecraft

Mod ID: `kingdomsofminecraft`

Java package: `com.kingdomsofminecraft`

Minecraft version: **26.1.2**

Mod loader: **NeoForge**

Java toolchain: **Java 25**

Build system: Gradle / ModDevGradle, using compatible verified versions.

Target: Windows-compatible singleplayer Minecraft.

Multiplayer support is not a project requirement.

The mod must work alongside normal Minecraft survival gameplay.

Do not replace Minecraft's basic exploration, mining, crafting, building, farming, or vanilla progression.

External gameplay mods must not be required.

Use official documentation:

https://docs.neoforged.net/

https://github.com/NeoForgeMDKs/MDK-26.1.2-ModDevGradle

Verify all dependency and API versions before implementation.

Do not use obsolete Minecraft modding APIs.

Use original code and legally usable assets.

---

# 2. OVERALL GAME DESIGN

Create a medieval fantasy kingdom simulation combining Minecraft survival with the strategic depth and military experience of Mount & Blade II: Bannerlord.

The player should be able to progress from an ordinary survivor to:

- Mercenary
- Soldier
- Army commander
- Clan leader
- Landowner
- Noble vassal
- Kingdom ruler
- Conqueror

Core gameplay must feature:

- Dynamic NPC kingdoms
- Noble clans
- Large armies
- Recruitment
- Troop upgrade trees
- Battlefield formations
- Tactical army commands
- Autonomous generals
- Directional combat
- Armor penetration
- Medieval weapons
- Magic
- Fantasy races
- Castles
- Cities and villages
- Sieges
- Territory conquest
- Regional economies
- Trade caravans
- Living civilian populations
- Companions
- Deep player progression
- Kingdom management
- RTS battlefield interface

Adapt mechanics to Minecraft rather than literally copying the original game.

Avoid proprietary Bannerlord assets, character names, faction names, UI graphics, and code.

---

# 3. KINGDOMS, CLANS AND DIPLOMACY

Generate between **11 and 20 kingdoms**.

Kingdoms should be founded dynamically by NPC clans.

Avoid a completely static faction system.

Each kingdom must maintain:

- Kingdom name
- Original banner
- Culture
- Ruler
- Noble clans
- Vassals
- Capital
- Owned settlements
- Treasury
- Military power
- Political influence
- Diplomatic relationships
- Territorial claims

### Cultures

Create historically inspired cultures resembling broad medieval military traditions:

- Western feudal cultures
- Northern warrior cultures
- Imperial cultures
- Steppe cultures
- Forest cultures
- Desert cultures

Mix these with original fantasy races.

Faction cultures must have different military strengths, architecture, equipment, recruitment, economies, and tactical tendencies.

### Diplomacy

Implement autonomous:

- War declarations
- Peace treaties
- Territorial disputes
- Kingdom influence
- Clan politics
- Vassal relationships
- Political decisions

All major decisions must depend on actual strategic conditions.

Do not prioritize marriage, heirs, family dynasties, or succession.

---

# 4. ARMIES AND TROOP RECRUITMENT

Support armies containing **500+ strategic troops**.

Do not represent every strategic soldier as a permanently loaded Minecraft entity.

Use a scalable hybrid system.

### Distant armies

Represent distant armies as data-driven simulation objects.

Track:

- Troop counts
- Troop types
- Equipment
- Training
- Experience
- Morale
- Supplies
- Commander
- Position
- Destination
- Orders
- Kingdom allegiance

### Nearby armies

When the player approaches a battle, materialize an appropriate physical subset.

Maintain the correspondence between physical entities and the underlying strategic army.

Update casualties and troop progression without duplicating units.

Automatically reduce physical entity count when necessary for performance.

### Recruitment

Support recruitment through:

- Villages
- Barracks
- Town recruitment offices
- Wandering mercenaries
- Individual NPC recruits

Use emeralds as the main recruitment currency.

### Troop upgrade trees

Create full branching troop trees with culturally distinct choices.

Include:

- Infantry
- Heavy infantry
- Spearmen
- Pikemen
- Archers
- Crossbowmen
- Skirmishers
- Cavalry
- Horse archers
- Elite guards
- Siege engineers
- Cultural fantasy units
- Spellcasting units

Upgrades must change actual combat behavior, equipment, and statistics.

Use data-driven definitions so content can be expanded without duplicating code.

---

# 5. ADVANCED FORMATIONS AND BATTLEFIELD AI

Implement:

- Line formation
- Shield wall
- Wedge
- Square
- Circle
- Column
- Loose formation
- Custom formations

Soldiers must intelligently maintain spacing, position, and orientation.

Support advanced RTS-style army commands:

- Follow
- Hold
- Move
- Charge
- Retreat
- Advance
- Defend
- Patrol
- Flank
- Attack selected enemy
- Protect commander
- Regroup
- Maintain formation

### Tactical map

Build an interactive RTS battlefield map.

Allow the player to:

- Select troops
- Select groups
- Assign formations
- Give movement commands
- Set targets
- Observe enemy positions
- Inspect army status
- Monitor morale
- Control large battles

### Adaptive military AI

Implement squad-level tactical intelligence.

AI should consider:

- Enemy strength
- Terrain
- Formation matchups
- Distance
- Choke points
- Ranged threats
- Morale
- Commander abilities

Generals must independently manage military campaigns.

They should recruit, organize, patrol, defend, attack, retreat, and recover.

Use efficient strategic decision-making instead of expensive per-soldier reasoning.

---

# 6. DIRECTIONAL COMBAT

Implement Bannerlord-inspired directional combat.

Include:

- Left attacks
- Right attacks
- Overhead attacks
- Thrust attacks
- Directional blocking
- Weapon reach
- Attack recovery
- Armor penetration
- Weapon speed
- Weapon weight
- Damage-type differences

Create responsive combat controls and appropriate animations.

### Weapons

Include:

- One-handed swords
- Two-handed swords
- Axes
- Maces
- Daggers
- Spears
- Pikes
- Polearms
- Bows
- Crossbows
- Throwing weapons

Allow component-based weapon customization through crafting.

### Armor

Support cloth, leather, chainmail, scale, plate, and fantasy variants.

Different damage types should interact meaningfully with armor.

### Cavalry

Implement basic mounted melee and ranged combat.

Create functional mounted-unit AI.

Advanced horse physics are not initially required.

### Morale

Troops should react to casualties, overwhelming enemies, leadership, and battlefield conditions.

Morale can cause retreat, rout, surrender, or formation collapse.

Do not prioritize complex fatigue or individual injury simulation.

---

# 7. MAGIC AND FANTASY

The mod must use a **full fantasy setting with magic and nonhuman races**.

Create original fantasy cultures using races such as:

- Humans
- Elves
- Dwarves
- Orcs
- Goblins
- Additional fantasy races where appropriate

Give races distinct cultural equipment, soldiers, and settlements.

### Magic

Implement:

- Fire magic
- Ice magic
- Lightning magic
- Healing
- Defensive magic
- Battlefield buffs
- Area-of-effect attacks

Support magical soldiers and player abilities.

Provide cooldowns, resource costs, balanced damage, and AI spellcasting.

Magic effects must be visually impressive while respecting performance limits.

Do not remove the usefulness of conventional medieval troops.

---

# 8. WORLD GENERATION AND SETTLEMENTS

Create a dedicated medieval world-generation system.

Preserve normal Minecraft survival compatibility.

Generate:

- Large cities
- Towns
- Villages
- Castles
- Fortresses
- Watchtowers
- Military camps
- Barracks
- Markets
- Trade routes
- Bridges
- Bandit camps
- Fantasy settlements

Create regional kingdom placement influenced by:

- Mountains
- Rivers
- Valleys
- Forests
- Roads
- Natural borders

### Settlement construction

Support both player building and NPC construction.

NPC construction must use:

- Building blueprints
- Available resources
- Construction jobs
- Terrain validation
- Protected block checks

Prevent automatic construction from destroying unrelated player builds.

### Settlement management

Simulate:

- Population
- Prosperity
- Food
- Security
- Loyalty
- Construction
- Production
- Employment
- Taxes
- Garrisons
- Recruitment reserves

### Civilian AI

Provide occupations including farmers, miners, woodcutters, blacksmiths, merchants, builders, and guards.

Nearby civilians should use daily routines and react to dangers.

Distant settlements must use efficient abstract population simulation.

---

# 9. TERRITORY AND CONQUEST

Create persistent geographic territory systems.

Track regional ownership and political control.

Implement:

- Kingdom borders
- Settlement ownership
- Territorial claims
- Strategic locations
- Player claims
- Occupation
- Conquest
- Border changes

Display owned regions on the campaign map.

Use coarse regional calculations instead of expensive per-block ownership processing.

---

# 10. SIEGE WARFARE

Implement real-time castle sieges.

Siege equipment:

- Battering rams
- Siege towers
- Ladders
- Catapults
- Trebuchets
- Ballistae

**Sieges must not permanently destroy Minecraft blocks.**

Implement gate health, temporary access states, capture points, and reversible siege mechanics.

### Siege AI

Attackers must coordinate assaults.

Defenders should guard walls, gates, towers, and fallback positions.

After conquest, allow occupation, looting, garrison assignment, governor replacement, and vassal grants.

These choices must affect settlement prosperity, loyalty, and faction relationships.

---

# 11. ECONOMY AND TRADE

Use Minecraft emeralds as the primary currency.

Create a dynamic regional economy with:

- Production
- Resource consumption
- Supply and demand
- Regional prices
- Caravans
- Trade routes
- Taxes
- Troop wages
- Garrison expenses
- Military supply
- Kingdom treasuries

Resources include:

- Food
- Wood
- Stone
- Iron
- Weapons
- Armor
- Horses
- Manufactured goods

Nearby traders may be physical NPCs.

Distant trade must be simulated abstractly.

Prevent resource duplication and negative economic values.

---

# 12. COMPANIONS AND CHARACTER PROGRESSION

Implement companions who can act as:

- Army commanders
- Captains
- Governors
- Scouts
- Merchants
- Officers
- Trainers

Give companions personalities, skills, equipment, loyalty, and relationships.

### RPG progression

Create deep progression for:

- Leadership
- Tactics
- One-handed weapons
- Two-handed weapons
- Polearms
- Archery
- Crossbows
- Riding
- Athletics
- Trade
- Stewardship
- Engineering
- Scouting
- Medicine
- Magic

Include branching perks, experience, and specializations.

Do not implement marriage, children, or dynastic aging.

---

# 13. GRAPHICS AND UI

Aim for cinematic medieval fantasy visuals within Minecraft's capabilities.

Develop original:

- Detailed troop models
- Weapon models
- Armor models
- Animation systems
- Siege equipment models
- Banners
- Spell effects
- Medieval interface assets

Provide:

- Kingdom screen
- Clan screen
- Diplomacy screen
- Army roster
- Troop upgrade tree
- Recruitment screen
- Campaign map
- Tactical map
- Settlement screen
- Siege interface
- Trade interface
- Character progression screen
- Companion screen
- Equipment customization screen

Interfaces must be functional, readable, and efficient.

Use placeholder assets only during development.

Replace them as the corresponding visual milestones are completed.

---

# 14. MODULAR ARCHITECTURE

Use modular packages for:

- Core
- Configuration
- Persistence
- Campaign simulation
- Kingdoms
- Clans
- Diplomacy
- Armies
- Troops
- Formations
- Combat
- Weapons
- Magic
- Cavalry
- World generation
- Settlements
- Territory
- Sieges
- Economy
- Civilian AI
- Companions
- Skills
- UI
- Rendering
- Networking
- Performance
- Testing

Prefer a data-driven implementation.

Keep strategic simulation separate from physical Minecraft entity behavior.

Use fixed simulation intervals and deterministic state transitions where practical.

---

# 15. PERFORMANCE OPTIMIZATION

Target mid-range gaming computers.

Implement:

- Abstract distant armies
- Abstract distant populations
- Distance-based entity activation
- AI scheduling
- Fixed simulation steps
- Configurable physical entity limits
- Efficient pathfinding
- Formation-level path planning
- Spatial indexing
- Cached regional data
- Batched updates
- Efficient rendering
- Save optimization
- Performance profiling

Avoid:

- Unlimited entity spawning
- Unnecessary chunk loading
- Per-tick global world scans
- Excessive allocations
- Unsafe asynchronous world access
- Duplicate AI processing

Performance goals:

- Smooth ordinary exploration
- Stable large strategic campaigns
- Representative battles targeting 30+ FPS
- Ordinary exploration targeting 60 FPS on the benchmark machine

These are benchmark goals, not guarantees.

Record actual measurements.

---

# 16. ADVANCED TOKEN USAGE OPTIMIZER

**This system is mandatory.**

Minimize unnecessary Codex token consumption while preserving implementation correctness.

### Context management

Create:

- `CODEX_CONTEXT.md`
- `CURRENT_TASK.md`
- `ARCHITECTURE.md`
- `TOKEN_USAGE.md`

At the beginning of each Codex session:

1. Read the compact context files.
2. Identify the current unfinished task.
3. Inspect only relevant source files.
4. Avoid repeating completed analysis.
5. Resume actual implementation.

### Code generation efficiency

- Make focused changes.
- Reuse working code.
- Avoid duplicate implementations.
- Use data-driven registries.
- Avoid unnecessary rewrites.
- Group related changes.
- Avoid unrelated refactoring.
- Prefer code changes over excessive explanations.

### Testing efficiency

Run targeted tests before comprehensive tests.

Avoid repeating identical failed builds without changes.

Read concise build errors instead of large repeated logs.

Keep official API research focused.

### Adaptive token budgeting

Initial total planning estimate:

**4,170,000 tokens**

This is an approximate development budget, not a guaranteed usage amount.

Record real usage if the environment exposes authoritative token counters.

Otherwise clearly mark usage as estimated.

Monitor consumption by development stage.

If projected consumption exceeds a stage's planned budget by more than 25%, examine implementation efficiency before continuing.

Do not sacrifice functionality or validation merely to save tokens.

### Token optimization priority

Prefer:

1. Reusable infrastructure
2. Shared troop systems
3. Shared AI systems
4. Data-driven content
5. Targeted testing
6. Incremental integration
7. Context compression
8. Efficient error correction

Keep progress summaries concise.

Do not regenerate completed working components.

---

# 17. DEVELOPMENT PROGRESS BAR

Create:

- `PROGRESS.md`
- `progress.json`
- `ROADMAP.md`
- `VALIDATION.md`
- `DEVELOPMENT_LOG.md`

Track verified completion using weighted deliverables.

Stage weights:

| Stage | Weight |
|---|---:|
| Foundation | 5% |
| Kingdoms | 12% |
| Armies | 13% |
| Battlefield AI | 17% |
| Combat and magic | 13% |
| Settlements and world generation | 13% |
| Sieges | 9% |
| Economy and civilian simulation | 9% |
| UI and visuals | 6% |
| Final integration | 3% |

Total: 100%.

Only verified deliverables count toward completion.

A feature is verified after code exists, acceptance criteria pass, appropriate tests succeed, and required builds succeed.

Display progress using a 20-character terminal bar.

Example:

    KINGDOMS OF MINECRAFT

    Progress: 35.0%
    [#######-------------]

    Current stage: Battlefield AI
    Build status: PASS
    Latest release: v0.3.0-alpha
    Next release: v0.4.0-alpha

    Verified tasks: 28/84
    Tokens used: Unavailable
    Estimated remaining: 2.4M

All numbers must come from real project records.

Never fabricate progress, test results, or actual token usage.

Update progress after meaningful verified changes.

Create a machine-readable progress report suitable for showing on the GitHub repository.

If practical, add a README progress badge generated from verified project state.

---

# 18. GITHUB UPLOAD AND STAGED RELEASES

**All project source code and releases must target:**

https://github.com/lilmigger63-hue/kingdomes

Use the existing `main` branch and safe development branches.

Do not overwrite unrelated files.

### Source uploads

After coherent, tested development changes:

1. Save source changes.
2. Update development documentation.
3. Commit changes.
4. Push to the authorized GitHub repository.
5. Confirm the push succeeded.

Prefer small, meaningful commits over huge unreviewable changes.

Do not publish broken development snapshots as stable releases.

### Milestone releases

| Milestone | Version | Required systems |
|---|---|---|
| Foundation | 0.1.0-alpha | Core framework and persistence |
| Kingdoms | 0.2.0-alpha | Dynamic kingdoms and diplomacy |
| Armies | 0.3.0-alpha | Recruitment and troop trees |
| Tactical AI | 0.4.0-alpha | Formations and orders |
| Combat | 0.5.0-alpha | Directional fighting and magic |
| Settlements | 0.6.0-alpha | World generation and territory |
| Sieges | 0.7.0-alpha | Siege combat and conquest |
| Economy | 0.8.0-alpha | Production, trade and civilians |
| Visuals | 0.9.0-beta | Finished UI and visual systems |
| Final | 1.0.0 | Integrated stable release |

Every milestone must pass its release criteria before publishing.

Minor corrective releases may be created when appropriate.

### GitHub Actions

Create:

- `.github/workflows/build.yml`
- `.github/workflows/test.yml`
- `.github/workflows/release.yml`

Use appropriate least-privilege permissions.

The build workflow must compile and test the mod.

The release workflow must upload verified JAR artifacts when an approved release tag is pushed.

Publishing must be authorized by the repository configuration.

### Release files

Each release should include:

- Working mod JAR
- Release notes
- Installation instructions
- Feature summary
- Known limitations
- Compatibility information
- Save migration notes

Keep older releases available.

Never claim a GitHub release is published without verifying that publication succeeded.

### Release recovery

If publishing fails:

- Preserve the last successful release.
- Record the error.
- Fix the actual problem.
- Retry only after validation.
- Never overwrite an existing release accidentally.

---

# 19. AUTOMATIC CODEX CONTINUATION

**Goal: resume development with minimal user intervention whenever Codex execution becomes available again.**

Important limitation:

A prompt cannot independently detect ChatGPT/Codex quota resets, bypass usage restrictions, or restart a terminated session.

Implement this feature only through documented, authorized scheduling and automation capabilities.

### Persistent checkpoint system

Create:

- `AUTO_RESUME.md`
- `CURRENT_TASK.md`
- `CODEX_CONTEXT.md`
- `PROGRESS.md`
- `TOKEN_USAGE.md`
- `release-status.json`

At safe checkpoints, record:

- Current stage
- Active task
- Completed tasks
- Remaining tasks
- Last successful build
- Current source status
- Known blockers
- GitHub release state
- Estimated remaining effort

### Scheduled continuation

If the Codex application supports project-bound scheduled tasks, configure a recurring continuation task or provide the exact setup instructions if user action is required.

Use this continuation instruction:

"Continue developing Kingdoms of Minecraft in repository lilmigger63-hue/kingdomes.

Read the persistent project state.

Resume the highest-priority unfinished task.

Make real code changes.

Run appropriate tests.

Update verified progress.

Push validated source changes to GitHub.

Publish completed milestones through the authorized release workflow.

Preserve the next checkpoint before stopping.

If execution is unavailable due to usage limits, do not attempt to bypass those limits."

Use the shortest supported, reasonable schedule rather than rapid repetitive attempts.

Never falsely report that automatic quota detection has been enabled.

### Safe execution

Prevent overlapping automated development runs.

Check for:

- Existing active sessions
- Uncommitted changes
- Concurrent branch updates
- Incomplete deployments
- Conflicting releases

Use execution locks where supported.

Do not overwrite another session's work.

### Quota interruptions

When a usage limit interrupts development:

- Preserve the latest checkpoint where possible.
- Do not restart completed stages.
- Resume from the saved state on the next successful authorized execution.

If a hard usage cutoff prevents checkpointing, recover from the most recent committed checkpoint.

### Project completion

When all milestones are verified and version 1.0.0 is successfully released:

- Mark the project complete.
- Set progress to 100%.
- Publish final documentation.
- Stop or request disabling of scheduled development tasks.
- Avoid consuming unnecessary tokens.

---

# 20. SAVE SYSTEM AND TESTING

Implement persistent world data for:

- Kingdoms
- Clans
- Armies
- Troops
- Territories
- Settlements
- Economy
- Diplomacy
- Companions
- Skills
- Active sieges
- Strategic orders

Use versioned schemas and migration logic.

Prevent duplication, corruption, and data loss.

Tests must cover:

- Mod startup
- World generation
- Save/load
- Kingdom creation
- Diplomacy
- Recruitment
- Troop upgrades
- Army movement
- Formation AI
- Combat
- Magic
- Territory ownership
- Settlement production
- Trade
- Sieges
- Companions
- UI integration
- Performance
- Release artifact validation

Test important systems after implementation and run broader integration testing at milestone boundaries.

---

# 21. DEVELOPMENT EXECUTION ORDER

Develop in the following sequence.

### Stage 1 — Project foundation

Initialize the NeoForge project, configure Gradle and Java, create registries, persistence, tests, token tracking, progress tracking, and GitHub workflows.

Build successfully.

Publish `0.1.0-alpha` after verification.

### Stage 2 — Kingdoms

Implement NPC kingdoms, cultures, clans, rulers, territory ownership, and war/peace logic.

Publish `0.2.0-alpha`.

### Stage 3 — Armies

Implement troop trees, recruitment, strategic armies, physical troops, and army persistence.

Publish `0.3.0-alpha`.

### Stage 4 — Tactical AI

Implement formations, RTS commands, tactical map, and autonomous generals.

Publish `0.4.0-alpha`.

### Stage 5 — Combat and magic

Implement directional fighting, blocking, weapon customization, armor penetration, cavalry, morale, and magic.

Publish `0.5.0-alpha`.

### Stage 6 — Settlements

Implement medieval generation, buildings, territory, civilian AI, and construction.

Publish `0.6.0-alpha`.

### Stage 7 — Sieges

Implement siege equipment, siege AI, castle attacks, and conquest.

Publish `0.7.0-alpha`.

### Stage 8 — Economy

Implement regional production, trade, caravans, taxes, wages, settlement management, and civilian economic integration.

Publish `0.8.0-alpha`.

### Stage 9 — UI and visuals

Complete kingdom interfaces, tactical screens, models, textures, animations, and visual effects.

Publish `0.9.0-beta`.

### Stage 10 — Final integration

Complete companion progression, cross-system integration, optimization, balancing, tests, documentation, and stable release packaging.

Publish `1.0.0` only after all acceptance criteria pass.

### Early playable prototype

Before expanding every feature, prioritize a vertical slice with:

- Two factions
- One settlement
- Recruitment
- A small commandable army
- A functioning tactical battle
- Persistent save state

Then expand this foundation toward the full design.

---

# 22. REQUIRED PROJECT FILES

Maintain the following files where relevant:

    README.md
    CHANGELOG.md
    ROADMAP.md
    PROGRESS.md
    progress.json
    TOKEN_USAGE.md
    CURRENT_TASK.md
    CODEX_CONTEXT.md
    ARCHITECTURE.md
    AUTO_RESUME.md
    VALIDATION.md
    DEVELOPMENT_LOG.md
    RELEASE_PLAN.md
    RELEASE_NOTES.md
    release-status.json
    build.gradle
    settings.gradle
    gradle.properties
    .github/workflows/build.yml
    .github/workflows/test.yml
    .github/workflows/release.yml

Use the standard Gradle wrapper and NeoForge project structure.

Add appropriate `.gitignore`, license information, and asset attribution records.

Do not commit secrets or local cache directories.

---

# 23. DEFINITION OF DONE

The project is complete only when:

1. All planned critical systems are implemented.
2. The source code builds for NeoForge 26.1.2.
3. Automated tests pass.
4. Required gameplay validation has passed.
5. Save and reload work correctly.
6. Major performance blockers are addressed.
7. All necessary UI systems function.
8. Required assets are present and legally usable.
9. Campaign systems interact correctly.
10. A stable release JAR is produced.
11. Version 1.0.0 is published to the specified GitHub repository.
12. Progress tracking reports 100% verified completion.

Do not substitute placeholder code or documentation for working functionality.

If work is unfinished, publish accurate progress and maintain a recoverable checkpoint.

---

# 24. IMMEDIATE EXECUTION

Begin actual development now.

1. Access `https://github.com/lilmigger63-hue/kingdomes`.
2. Inspect existing repository contents and branches.
3. Clone the repository into the development environment.
4. Set up the verified NeoForge 26.1.2 and Java 25 toolchain.
5. Initialize the project if empty.
6. Establish a successful baseline build.
7. Create the required tracking and documentation files.
8. Set up GitHub Actions.
9. Implement a working foundation.
10. Commit and push verified source.
11. Publish the first milestone when release criteria pass.
12. Continue into the next stage.
13. Maintain progress and usage tracking throughout.
14. Configure supported scheduled continuation, or provide instructions if manual setup is necessary.
15. Continue until all development milestones are completed, subject to environment and usage limits.

Every implementation session must prioritize actual working code.

At the end of each work unit, report:

- What was implemented
- Build and test status
- Overall verified progress
- Latest GitHub commit or release
- Token usage, if available
- Next task
- Any blocker

Never claim that a push, release, build, test, or automation succeeded without evidence.

**THE PROJECT DESTINATION IS:**

https://github.com/lilmigger63-hue/kingdomes

**BUILD THE MOD, UPLOAD VERIFIED PROGRESS, RELEASE IT IN STAGES, AND CONTINUE DEVELOPMENT FROM SAVED CHECKPOINTS.**
