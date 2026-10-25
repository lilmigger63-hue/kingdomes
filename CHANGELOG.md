# Changelog

## Unreleased
- Strategic settlement/capital registry, dimension-aware regional ownership and wartime transfer core operation.
- Operator registration/capital commands and public settlement/territory inspection, without block writes or chunk loading.
- Schema 4 migration preserving old political state and creating no automatic claims.
- Separate clan allegiances, protected ruling clans and validated vassal transfers.
- Symmetric war/peace treaty state and `/kingdoms politics` inspection.
- Schema 3 with explicit schema 1/2 migration and bounded political state.
- Persistent seed-based kingdom founding: clans, rulers, cultures, heraldry definitions and `/kingdoms list`.
- Explicit clock-only schema 1 migration to bounded kingdom-record schema 2.
- Seed-sweep/invariant tests, malformed schema fixtures and custom kingdom restart validation.
- Official Minecraft 26.1.2 NeoForge MDK configuration and checksummed Gradle wrapper.
- Immutable strategic clock and versioned, checksummed, atomic campaign file storage.
- Server campaign lifecycle and status command.
- Core persistence contracts, CI, guarded release workflow and continuation records.
