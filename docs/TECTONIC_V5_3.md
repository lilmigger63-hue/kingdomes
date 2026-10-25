# KINGDOMS OF MINECRAFT — V5.3
## TECTONIC WORLD GENERATION INTEGRATION

### MANDATORY OVERRIDE

This prompt modifies the existing Kingdoms of Minecraft master development instructions.

**Do NOT create a new terrain generation engine. Use Tectonic for all terrain generation.**

Continue using MineColonies for settlement building.

All remaining features, including kingdoms, armies, warfare, diplomacy, economy, fantasy races, magic, token optimization, staged GitHub releases, progress tracking, and scheduled continuation, remain unchanged.

### Project details

Minecraft: 26.1.2

Loader: NeoForge

GitHub:
https://github.com/lilmigger63-hue/kingdomes

Tectonic:
https://modrinth.com/mod/tectonic

MineColonies:
https://github.com/ldtteam/minecolonies

## 1. TECTONIC AS THE TERRAIN SYSTEM

Use the existing Tectonic mod for:

- Mountain ranges
- Large valleys
- Rivers
- Continents
- Oceans
- Canyons
- Terrain elevation
- Natural landscapes

Do not create competing terrain noise generators, continent generators, river generators, or biome terrain replacements.

Use the official compatible NeoForge 26.1.2 version.

Declare and document the required Tectonic dependency.

Do not redistribute Tectonic's JAR inside Kingdoms of Minecraft unless explicitly authorized and licensing requirements are satisfied.

## 2. MINECOLONIES SETTLEMENTS ON TECTONIC TERRAIN

Make MineColonies construction work naturally with Tectonic-generated landscapes.

Settlement selection must consider:

- Terrain slope
- Elevation
- Available building space
- Water access
- River proximity
- Nearby resources
- Forest coverage
- Mountain barriers
- Accessibility
- Flooding and steep-cliff risks

Preferred settlement locations:

- Villages in valleys and plains
- Castles on hills and elevated ridges
- Trading settlements near rivers
- Dwarven settlements in mountain regions
- Elven settlements near forests
- Military forts at mountain passes
- Coastal settlements near oceans

Do not automatically flatten entire mountains or valleys.

Use existing MineColonies building-placement and terrain-adaptation systems where possible.

## 3. STRATEGIC WORLD MAP

Build the Kingdoms of Minecraft strategic map using actual generated terrain data.

Display:

- Mountains
- Rivers
- Settlements
- Kingdom borders
- Roads
- Armies
- Trade routes
- Fortresses
- Strategic passes

Terrain must affect army movement and military strategy.

Examples:

- Mountains slow armies.
- Valleys provide easier routes.
- Rivers create natural defensive borders.
- Mountain passes become strategic military locations.
- Fortresses control important travel routes.

Use cached and coarse-resolution geographic data.

Do not scan or generate the entire Minecraft world to construct the strategy map.

## 4. KINGDOM TERRITORY DISTRIBUTION

Generate the requested 11–20 kingdoms across suitable Tectonic landscapes.

Use geographical features to influence territory boundaries.

Prefer coherent territories separated by:

- Mountain ranges
- Rivers
- Coastlines
- Natural barriers

Use a regional strategic simulation.

Avoid placing kingdoms or settlements in inaccessible terrain without appropriate infrastructure.

Do not require all kingdom territories to be physically generated at world creation.

## 5. ROADS AND INFRASTRUCTURE

Reuse existing MineColonies and Minecraft-compatible construction mechanics wherever possible.

Develop only the lightweight integration needed for strategic roads and travel.

Road selection must consider Tectonic terrain.

Prefer:

- Valleys
- Gentle slopes
- River crossings
- Mountain passes

Avoid unrealistic straight roads through steep mountains.

Physical roads may be built progressively by MineColonies workers where supported.

Distant routes should remain abstract until physical construction is needed.

## 6. PERFORMANCE

Preserve Tectonic's terrain-generation behavior.

Avoid:

- Duplicate noise calculations
- Large chunk pre-generation requirements
- Persistent remote chunk loading
- Repeated terrain scanning
- Redundant world-generation infrastructure

Cache geographic information as needed.

Use strategic abstraction for distant territories.

Respect Minecraft's loaded/unloaded chunk boundaries.

Never perform unsafe asynchronous block or world mutations.

## 7. COMPATIBILITY VERIFICATION

Before integration:

1. Identify the compatible Tectonic NeoForge 26.1.2 release.
2. Verify installation and runtime behavior.
3. Verify the MineColonies version or port being used.
4. Identify any incompatibilities between settlement placement and Tectonic terrain.
5. Implement only necessary compatibility adapters.
6. Test fresh-world generation.
7. Test mountain, valley, river, and coastal settlement placement.
8. Verify save/load stability.

If MineColonies itself is not yet compatible with Minecraft 26.1.2, report this as a separate dependency blocker.

Do not attempt to fix that by rewriting Tectonic or creating a new settlement engine.

## 8. DEVELOPMENT ROADMAP UPDATE

Replace the original custom world-generation stage with:

**Stage 6 — MineColonies and Tectonic Integration**

Deliverables:

- Verified compatible dependencies
- MineColonies settlement integration
- Tectonic terrain integration
- Terrain-aware settlement placement
- Kingdom geographic distribution
- Geographic military movement costs
- Territory mapping
- Strategic road planning
- Save/load verification
- Performance testing

Do not count existing Tectonic functionality as newly implemented work.

Only count the actual Kingdoms of Minecraft integration.

## 9. TOKEN OPTIMIZATION

Using Tectonic and MineColonies must eliminate unnecessary redevelopment.

Do not generate custom replacement code for features already provided by these mods.

Prioritize:

- Small integration adapters
- Reusable geographic data
- Data-driven settings
- Targeted compatibility testing
- Efficient strategic simulation

Update the token budget to reflect the reduced custom-development scope.

Do not claim exact savings without actual measured development data.

## 10. GITHUB RELEASES AND CONTINUATION

Preserve the existing automated development systems.

Push verified progress to:

https://github.com/lilmigger63-hue/kingdomes

Publish functional milestone builds through GitHub Releases.

Maintain:

- PROGRESS.md
- progress.json
- TOKEN_USAGE.md
- ROADMAP.md
- release-status.json

Preserve automatic continuation instructions through supported Codex scheduling.

Never falsely claim that releases or scheduled tasks have been created.

## 11. IMMEDIATE ACTION

Inspect the existing Kingdoms of Minecraft repository.

Remove planned custom terrain-generation work.

Integrate the official compatible Tectonic version.

Use MineColonies for building construction and colony management.

Implement terrain-aware kingdom and settlement placement.

Test compatibility with NeoForge 26.1.2.

Continue building the complete Bannerlord-inspired mod.

**DO NOT DEVELOP A NEW TERRAIN ENGINE OR A NEW COLONY CONSTRUCTION ENGINE. REUSE TECTONIC AND MINECOLONIES.**
