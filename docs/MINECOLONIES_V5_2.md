# KINGDOMS OF MINECRAFT — V5.2
## DIRECT MINECOLONIES INTEGRATION

### IMPORTANT: REPLACE THE PREVIOUS SETTLEMENT SYSTEM

This instruction overrides the settlement-building sections of the existing Kingdoms of Minecraft V5 master prompt.

**Do NOT develop a new settlement-building engine. Use the real MineColonies mod as the foundation.**

All other systems remain unchanged, including armies, combat, kingdoms, diplomacy, magic, economy, GitHub releases, token optimization, progress tracking, and automatic continuation.

## 1. Target project

Minecraft: 26.1.2

Loader: NeoForge

GitHub repository:
https://github.com/lilmigger63-hue/kingdomes

Official MineColonies:
https://github.com/ldtteam/minecolonies

Unofficial NeoForge 26.1.2 port:
https://github.com/Forgotten409/NeoForge-MineColonies-Unofficial

Official documentation:
https://minecolonies.com/wiki/

## 2. Reuse MineColonies

Use MineColonies for:

- Building construction
- Building placement and blueprints
- Builder NPCs
- Citizen professions
- Worker job assignments
- Building upgrades
- Warehouses
- Couriers
- Resource requests
- Town Halls
- Housing
- Farms and production buildings
- Citizen schedules
- Colony management
- Existing building styles

Do not recreate these systems in Kingdoms of Minecraft.

Instead, implement an integration layer connecting MineColonies to the existing Bannerlord-inspired gameplay.

## 3. Compatibility verification

Before coding:

1. Inspect the official MineColonies repository.
2. Check whether an official NeoForge 26.1.2 build is available.
3. Examine the unofficial 26.1.2 port if necessary.
4. Verify source licensing.
5. Verify compatible dependencies.
6. Inspect available public APIs.
7. Test compilation and startup.
8. Select the most maintainable working integration.

Prefer an official supported version when available.

Do not silently substitute an older Minecraft version.

If no working 26.1.2 build exists, report the blocker and prepare a separate compatibility-porting task.

Do not pretend that an incompatible version works.

Do not copy and redistribute MineColonies source or assets without satisfying their respective licenses.

## 4. Colony ownership and kingdoms

Make every strategic settlement use a MineColonies colony wherever the installed implementation supports it.

Connect MineColonies colony ownership to the Kingdoms of Minecraft political system.

Map:

- MineColonies colony → Kingdom settlement
- Town Hall → Settlement administration
- Citizen population → Settlement population
- Guards → Local defense forces
- Warehouse inventory → Regional logistics
- Building levels → Settlement development
- Builder work requests → Construction economy

Keep MineColonies authoritative for physical settlement state.

Keep Kingdoms of Minecraft authoritative for kingdoms, diplomacy, army campaigns, and territorial politics.

Avoid duplicate ownership systems and conflicting updates.

## 5. AI kingdoms must build settlements

NPC rulers and governors must be able to use MineColonies construction systems.

Create a settlement governor integration that can:

- Establish colonies
- Place Town Halls
- Select building sites
- Queue construction
- Assign workers
- Request resources
- Expand housing
- Build production facilities
- Improve defenses
- Upgrade structures

Use existing MineColonies APIs wherever possible.

For distant settlements, maintain an optimized strategic construction queue.

Do not force-load entire colonies.

Synchronize strategic development with actual MineColonies buildings only through safe supported mechanisms.

## 6. Military integration

Connect MineColonies settlements with the Bannerlord army system.

Examples:

- Barracks support troop recruitment.
- Guard towers support settlement defense.
- Warehouses provide military supplies.
- Farms supply food to armies.
- Blacksmiths support weapons and equipment.
- Stables support mounted troops.
- Settlement prosperity affects recruitment.
- Building upgrades improve defense and production.

MineColonies guards must not automatically become duplicated Kingdoms of Minecraft troops.

Establish explicit recruitment and transfer rules.

## 7. Conquest and territorial ownership

Allow kingdoms to conquer MineColonies settlements.

When a settlement is captured:

- Update strategic ownership.
- Handle colony permissions safely.
- Transfer or appoint a governor.
- Update defending forces.
- Apply political consequences.
- Preserve buildings and inventories.

Do not bypass MineColonies ownership checks through unsafe save manipulation.

If the installed version lacks a suitable colony-transfer API, implement a compatible integration through documented extension points or an appropriately licensed, separately maintained dependency patch.

Do not mark conquest integration complete until it works in-game.

## 8. Player experience

Players must be able to interact with normal MineColonies construction and management features.

Preserve functioning:

- Building tools
- Builder jobs
- Town Halls
- Colonist management
- Warehouse requests
- Building upgrades
- Worker management

Add Bannerlord features alongside them.

Players should be able to develop their own settlement manually while AI kingdoms expand theirs.

## 9. Performance

Use MineColonies' existing worker and construction systems instead of duplicating them.

Optimize the integration through:

- Aggregated distant economy
- Budgeted strategic updates
- Reduced duplicate AI calculations
- Cached colony data
- Safe chunk-loading policies
- Event-based synchronization
- Efficient army-to-colony interactions

Do not interfere with the internal behavior of MineColonies unnecessarily.

Benchmark settlements with increasing worker populations.

## 10. Replace the old milestone

Modify the previous development roadmap.

Stage 6 must now be:

**MineColonies Integration and Kingdom Settlement Management**

Required functionality:

1. Compatible MineColonies dependency.
2. Colony discovery and state access.
3. Settlement-to-kingdom mapping.
4. MineColonies building reuse.
5. AI governor construction decisions.
6. Recruitment and logistics integration.
7. Save/load synchronization.
8. Conquest compatibility.
9. Performance validation.

Release as `0.6.0-alpha` only after the required features pass verification.

Do not count building features supplied by MineColonies as newly implemented Kingdoms of Minecraft features.

Track integration completion separately.

## 11. Token optimizer

Prioritize using existing MineColonies functionality.

Do not spend tokens rewriting stable systems that are already available.

Inspect only relevant APIs and source files.

Use focused adapter classes.

Avoid maintaining an unnecessary fork unless compatibility requires it.

Update token estimates to reflect removed settlement-engine work and added integration/porting work.

Record measured token usage only when available.

## 12. GitHub publishing

Continue using:

https://github.com/lilmigger63-hue/kingdomes

After verified implementation:

- Commit source changes.
- Push to the repository.
- Update PROGRESS.md.
- Update TOKEN_USAGE.md.
- Build the mod.
- Test the MineColonies integration.
- Publish the staged release when ready.

If MineColonies is a separate dependency, document its exact compatible version and installation requirements.

Do not bundle its JAR or assets without appropriate redistribution rights.

## 13. Automatic continuation

Preserve the existing scheduled continuation and recovery instructions.

If execution is interrupted:

- Save the current integration state.
- Record the next task.
- Preserve tested changes.
- Resume from the last verified checkpoint when execution is next authorized.

Do not restart the MineColonies integration from scratch.

## 14. Begin implementation

Immediately inspect the Kingdoms of Minecraft project and available MineColonies versions.

Determine the best valid NeoForge 26.1.2 integration.

Replace the custom settlement-building plans with actual MineColonies integration.

Implement and test the adapter layer.

Preserve all other gameplay systems.

Continue staged GitHub development.

**DO NOT CREATE A NEW SETTLEMENT ENGINE. REUSE MINECOLONIES.**
