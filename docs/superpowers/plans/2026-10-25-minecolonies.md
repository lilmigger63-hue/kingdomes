# MineColonies initial adapter plan/checkpoint

Spec: docs/MINECOLONIES_V5_2.md, overriding settlement construction requirements. Native continuous implementation requested.

- [x] Inspect official/port versions, source licensing, dependencies and public APIs; pin the best candidate.
- [x] Build the pinned source and test actual asset-provisioned startup on target Minecraft/NeoForge.
- [x] Add typed read-only discovery and political mapping with schema migration; disable competing standalone site creation.
- [x] Verify real-colony discovery, authorization, owner preservation, mapping persistence and unloaded-region safety.
- [x] Reproduce/fix review findings: external cache resolution and dirty-source rejection; run full local suites.
- [ ] Confirm source CI, checkpoint and persist reusable configuration.

Remaining stage: full save/load synchronization, normal client tools/builders/warehouses/upgrades, AI governor, recruitment/logistics, safe conquest and performance. No upstream supplied features count as new Kingdoms implementation. No unsafe ownership/NBT bypass or necessary fork introduced.
