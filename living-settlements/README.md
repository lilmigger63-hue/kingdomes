# MineColonies: Living Settlements 0.5.0

Autonomous NPC colonies, adaptive founding and native worker construction for the
kingdoms pack. This extends 0.4.0 and preserves its mayors, player governors,
50% village selection and existing save data.

## Install

| Component | Exact supported version |
|---|---|
| Minecraft | 26.1.2 |
| NeoForge | 26.1.2.124 |
| MineColonies unofficial port | 1.1.1399-port26.1.2-0.6.0-publish |
| Tectonic / Lithostitched tested | 3.0.28 / 1.8.1 |

Replace older Living Settlements JARs with `dist/living-settlements-0.5.0.jar` on
clients and server. Keep the pinned MineColonies dependency and provision its
external building assets before exploration. Assets/dependencies are not shaded
into this addon. A fresh CurseForge profile can import
`dist/kingdoms-living-settlements-0.5.0.zip`; original dependency IDs and eight pack
configs are retained. The supplied addon preset is also available separately.
Back up worlds, including their generated infrastructure schematics, before updates.

Explore **new chunks** to test new discovery. Existing vanilla/abandoned settlements
are not converted or automatically populated. A Town Hall offering **Reinstate
Settlement** belongs to native abandoned-settlement content, not an active addon
NPC colony. Existing active player colonies remain player-controlled.

## Discovery and terrain

The default multiplier is 2.5. Independent seed/chunk decisions add opportunities
alongside the existing configurable 50% vanilla village replacement. They are not
paused by `preserveVanillaVillageFrequency`. Generation no longer reads the old
1500–3000 distance grid to choose new candidates. Adjacent opportunities are legal;
actual building footprints, player protection and native claim ownership must fit.
Native colonies cannot share owned claim chunks. There is no replacement world
noise generator and no fixed addon center-to-center spacing for adaptive starts.

Density is calibrated to the intended vanilla-selection baseline: a default
32x32-chunk village opportunity times50%, or one selected opportunity per2048
chunks. A multiplier2.5 adds1.5/2048 independent opportunities per new chunk.
This is an expected **opportunity** rate, not a promise of2.5 times completed towns.
Biome distribution, claims, protected structures and valid access affect success.
Zero multiplier disables new selections; turning village replacement off makes
all configured opportunities independent.

The broad25-sample/four-block height filter is removed. New adaptive starts search
nearby positions and four schematic orientations. Ground surveys look below tree
trunks/canopies rather than treating their tops as hills. New adaptive placements
ignore the legacy biome whitelist and town-wide height band. Building volumes,
world boundaries, existing structures, native claims and block entities remain
protected. Unloaded alternatives wait and remain retryable; they are not permanently
rejected just because an explorer leaves.

Only a valid Town Hall plot is required initially. Its schematic door/open-entrance
routes must meet safe outside ground; positions without usable access are retried.
The completed Town Hall and bounded support columns are the finite world-generation
endowment. They are placed incrementally. Later huts and infrastructure are not
pasted as completed structures. The mayor locates them as needs arise.

Initial native claims and relevant building/tape chunks must be loaded. Standard
10–12 chunk distances are practical for testing; low loaded distance or moving away
can defer work. No addon force-load tickets are added. A saved vanilla marker keeps
its original village pieces for safe pre-founding fallback. Existing player edits
stop replacement; partially founded colonies are retained rather than overwritten.

## Mayor, citizens and real supplies

New starts default to four real MineColonies citizens. An existing unemployed adult
becomes the saved mayor; the role never adds a duplicate citizen or invents a native
profession. Normal citizen movement, shelter and job systems remain authoritative.
Automatic hiring reserves the mayor; native removal clears its identity before
citizen IDs can be reused. Existing staffed NPC jobs are preserved.

The needs engine prioritizes a builder, food, housing, storage, resources/delivery,
defense and later production/upgrades. Stages1–5 describe infrastructure, not timed
free upgrades. Native jobs, beds, research, population capacity and builder range
remain required. Later huts consume actual hut items, start at level zero and
receive native BUILD/UPGRADE requests. The pinned port permits a level-zero builder
to construct its own hut. Native university research pays real ingredients and
uses native eligibility; completion still belongs to native researchers.

With starting resources enabled, citizens receive finite food and stone tools and
the Town Hall receives finite basic materials. A one-time saved kit is derived
from real native level-one survival blueprint requirements, including style item
components. It covers founding workplaces/homes without requiring their plots to
be found first. New adaptive kits also reserve81 dirt for the first field. The kit is capped at32768 units/1024 entries, debited into real
inventories and never refilled for upgrades or shortages. Saved paid escrow/receipts
protect ordinary interrupted deliveries. Tools can be moved from actual colony
storage into workers' inventories; missing tools/materials use native requests,
crafting and logistics. Durability, mining and resource consumption remain native.

After the endowment, farming, forestry, mining, crafting, warehouses and couriers
must sustain the settlement. Specialized materials or unavailable production can
still stall development; universal long-term self-sufficiency is not established.

## Terraforming and infrastructure

After a working builder exists, the mayor can queue native decoration orders for
short access routes, stone roads/stairs, timber bridge decks, foundation supports,
terraces and perimeter retaining supports. Existing route surveying follows terrain
and can plan clearing through natural vegetation. The first builder requires access
that is already walkable, avoiding a dependency on an unavailable decoration worker.
Workers execute their normal clearing/building phases, walk to sites, use tools,
collect mined resources and consume real materials. A completed farmer without a usable field causes the mayor to survey one before upgrading it. Builders prepare real dirt and lighting before the scarecrow is registered; farmers still till, plant and harvest with real seeds. No new worker profession,
teleporting worker or recurring free infrastructure material grant is introduced.

Generated native schematics live inside the world at
`living_settlements/infrastructure/`. Addon SavedData journals task identities,
owners, footprints, native order IDs and completion/blocking state; MineColonies
persists the actual work orders and inventories. Completed task payloads compact
to their identity/footprint. Missing native work is reconciled with actual blocks;
ordinary reload does not assume a canceled order completed its work.

Real player edits cancel queued addon-managed native building/decoration work.
Edited building plots remain protected across reload, and blocked infrastructure
columns are avoided by later route surveys. The mayor can select other plots/routes.
Existing player-owned colonies are excluded from these automatic edit controls.

## Configuration

Edit the profile/server's `config/living-settlements-server.toml` and restart.
Existing custom root values remain. `defaults/living-settlements-server.toml` lists
the supported preset. Root `enabled` controls discovery; `startingPopulation`
(default4, range4–12) supplies the requested starting-citizen setting. Root
`settlementRadius` (112, range64–192) bounds managed search/expansion work.

- `settlementGeneration.spawnFrequencyMultiplier`:0–20, default2.5.
- `adaptivePlacement`:new Town Hall-only adaptive sites; old saved modes are retained.
- `allowRoughTerrain`, `allowMountainSettlements`, `allowRiversideSettlements`,
  `allowLakesideSettlements`, `allowForestSettlements`, `allowValleySettlements`,
  `allowCoastalSettlements`:all defaulttrue. Exclusions use biome tags, water in
  footprints and a local valley survey; these are broad terrain classifications.
- `enforceArbitraryDistanceLimits=false`, `allowNearbySettlements=true`, overlap
  protection and no existing-village replacement are fixed supported invariants.
- `terraforming`:toggle added excavation, fill, terraces, retaining supports,
  bridges, stairways and roads. Ordinary native building construction still uses
  its own native clearing/filling rules. Workers/tools/materials are always required.
- `maxBlocksModifiedPerTick=8`:shared native work-step budget, with one primary
  placement per native handler call. Native multi-block handlers and neighbor
  physics can produce extra raw block changes; this is not a strict total-change cap.
  `maxConcurrentWorkersPerColony=3` limits admitted workers per colony per tick.
- `autonomousMayor`:retain construction/upgrades/jobs/population/resource/defense
  flags; new terrain planning, expansion, tool-transfer and infrastructure controls
  defaulttrue. Disabling terrain-aware mayor planning confines new plots to the
  earlier elevation band/orientation. Decision interval accepts200–24000ticks.
- `settlementProgression`:retain starting mode/resources, required resources,
  stage1–5, buildings8–64 and population4–128 limits. Lowering caps deletes no citizen.
- `autonomousDevelopment`:shared enable/unloaded controls and update budget
  (default2, range1–8); `forceLoadChunks` remainsfalse.
- `offlineProgression.maximumCatchupHours`:default24, range1–168.

Legacy root `spawnFrequency`, minimum/maximum distance and `allowedBiomes` remain
readable for compatibility but do not govern new adaptive discovery. Existing
pending legacy reservations retain their saved layouts and legacy safety rules.

## Unloaded administration and limits

Unloaded mayors save needs, inventories/rates, population, stage, work and expansion
intents, routes and infrastructure. Bounded food/wood/stone forecasts update
administrative priorities without entity ticking. They create no physical items,
births or buildings. Server-offline wall-clock time is not simulated. Actions
revalidate when relevant chunks return. Native physical workers in this exact port
also need real nearby visitors; the addon creates no fake activity subscribers.

World, entity, native colony and addon files are not one atomic transaction. Normal
reconstruction and save checkpoints reduce duplicate replay; abrupt JVM/disk failure
or mismatched backups are not guaranteed lossless. Initial grants/fallback slices
retain conservative save windows. Native lifecycle/IO costs have no strict tick-time
bound. Full process restart/crash injection, large multiplayer stress, client UI,
Terralith gameplay, complete mountain/forest/coastal infrastructure and long-term
research/economy development still need in-game verification.

## Commands, build and evidence

`/settlements survey`, `colony`, `mayor` and `governor enable [1-5]`, `status`,
`disable` remain. Mayor status reports local needs, stage, stock and routes.
Player governor opt-in still requires MANAGE_HUTS and its authorizer online in the
same dimension. Commands are not needed for NPC settlement discovery/management.

Use Java25 and `scripts/build.ps1` (pinned Gradle9.2.1 / NeoGradle7.1.38).
`./scripts/build.ps1 -Tasks @('build','runGameTestServer','--offline')` runs checks;
`scripts/package.ps1` creates the versioned dist JAR/import ZIP. The exact port is
read from `libs/` or CurseMaven1713567/9004393. Test fixtures alone load/prepare chunks
and use a mock visitor. See `docs/validation-0.5.0.md` for final measured results.

Sources: [official upstream](https://github.com/ldtteam/minecolonies),
[pinned port](https://github.com/Forgotten409/NeoForge-MineColonies-Unofficial/tree/v0.6.0).

