# Roadmap

The base requirements are preserved in docs/MASTER_PROMPT.md; docs/MINECOLONIES_V5_2.md overrides settlement building and dependency requirements. Each stage requires its own build and acceptance evidence; none is complete yet.

- Foundation: 5% — toolchain_build, campaign_storage, world_lifecycle, ci_checkpoint, platform_release_acceptance
- Kingdoms: 12% — dynamic_kingdoms_diplomacy
- Armies: 13% — recruitment_troops_armies
- Battlefield AI: 17% — formations_commands_tactical_map
- Combat and magic: 13% — combat_magic_cavalry
- MineColonies Integration and Kingdom Settlement Management: 13% — compatible dependency; colony discovery; political mapping; building reuse; AI governors; recruitment/logistics; save synchronization; conquest compatibility; performance validation
- Sieges: 9% — reversible_siege_conquest
- Economy and civilian simulation: 9% — trade_production_civilians
- UI and visuals: 6% — finished_interfaces_assets
- Final integration: 3% — companions_balance_full_acceptance

Prioritize an early vertical slice: two factions, one settlement, emerald recruitment, a small commandable army, a battle and persistent state before expanding content. The full campaign must ultimately generate 11–20 dynamic kingdoms and support 500+ abstract strategic troops.

MineColonies is authoritative for physical colonies, workers, construction, inventories, housing and building upgrades. Kingdoms owns politics and strategic armies. Do not create a custom settlement engine or count upstream features as newly implemented integration. Existing site records are legacy strategic metadata and require explicit colony mapping; they do not prove MineColonies integration. Release 0.6.0-alpha only after all nine integration acceptance groups pass.
