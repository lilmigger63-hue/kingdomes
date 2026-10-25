# Settlement ownership implementation plan

Spec: docs/MASTER_PROMPT.md; native continuous development authorized.
Goal: persistent sites/capitals and coarse region ownership, usable through an operator setup command and public inspection.
Architecture: dimension-aware 512-block Region keys and immutable Settlement records in CampaignState; ownership derived from registered sites (one per region), schema 4 migration from schemas 1–3.

- [x] Test negative coordinate boundaries, dimension isolation, site registration/identity limits, capital replacement and wartime ownership transfer; migration and persistence before implementation.
- [x] Implement bounded 128-site registry, preserving it through all existing state transitions. No automatic generation/claims on existing player land; no block writes or chunk loading.
- [x] Upgrade storage to schema 4, 256 KiB bound; old saves gain an empty site list, preserving all existing records.
- [x] Add operator /kingdoms settlement <kingdom> <name> and /kingdoms capital <id>; public /kingdoms territory and /kingdoms settlements. Test command authorization and actual saved ownership in Minecraft.
- [x] Validate full suite, migration, controlled restart, independent review and source CI; update checkpoint and push.

Regions are strategic metadata, not build protection or siege gameplay. Conquest transition requires an existing war, transfers the entire site/region and clears capital status; no live player conquest command until siege mechanics exist.
