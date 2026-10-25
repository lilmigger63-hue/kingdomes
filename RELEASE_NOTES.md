# 0.1.0-alpha candidate — unreleased

Minecraft 26.1.2, NeoForge 26.1.2.122, Java 25. Requires separately installed community MineColonies port 1.1.1403-port26.1.2-0.7.1 from source commit 9901957777e3dc8cf8119826c619654a48ae486e; build its publishJar using scripts/setup-minecolonies.py. No dependency JAR or restricted assets are bundled. The port provisions upstream assets locally. See docs/MINECOLONIES_COMPATIBILITY.md for exact compatibility evidence and limitations.

Includes campaign persistence, 11–20 kingdom foundations, clans/cultures/heraldry, political allegiance/treaties, live colony discovery and explicit operator colony-to-kingdom binding. /kingdoms colonies reads MineColonies name, owner, population and building data; colonies bind records only political allegiance. Player permissions and physical state remain MineColonies-owned. Rebinding/conquest is blocked. Standalone site/capital creation is disabled; normal MineColonies tools/Town Halls replace those plans.

Save format: schema 5. Existing schemas 1–4 migrate without rewriting during load; legacy sites remain but no automatic colonies or mappings are invented. Old builds cannot read schema 5. Back up worlds before upgrading.

Distribution license: GPL-3.0-only, with LICENSE and retained template/upstream attribution. Dependency code/assets are separate.

AI governors, construction automation, normal client tool/warehouse/upgrade validation, military recruitment/logistics, full synchronized MineColonies world reload, safe conquest and performance are unfinished. Headless tests do not prove Windows singleplayer/UI readiness. This is not a completed 0.6.0-alpha integration release. No release has been published.

V5.3 additionally requires separate official Tectonic 3.0.28 and Lithostitched 1.8.1 artifacts supporting 26.1.2. Run scripts/setup-terrain.py; hashes/URLs are pinned in dependencies/terrain.json. Tectonic supplies its own Apollib library. No terrain dependency or data is embedded in Kingdoms. /kingdoms site <role> offers cached loaded-chunk advice only; no automatic terrain-aware colony placement or geographic distribution is completed. Real mountain/valley/river/coastal construction, movement/routes, save compatibility and performance remain pending. Use a fresh test world.

Adds /kingdoms plan <pack> <blueprint> for a fresh original-orientation blueprint expansion preflight, with bounded full-footprint checks, colony permissions and hazard/cut/fill rejection. This read-only command does not request construction or validate resources, accessibility, full structure overlap or other mods' protection. Use normal MineColonies build tools; automatic governors remain unfinished.
