package dev.kingdoms.livingsettlements;

import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.List;

public final class NaturalSettlementConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec.BooleanValue ENABLED = BUILDER.comment("Generate NPC colonies in new Overworld chunks. Restart after changes.").define("enabled", true);
    public static final ModConfigSpec.DoubleValue FREQUENCY = BUILDER.comment("Legacy key retained for config compatibility; new discovery uses settlementGeneration.spawnFrequencyMultiplier.").defineInRange("spawnFrequency", 1.0, 0.0, 1.0);
    public static final ModConfigSpec.IntValue DISTANCE = BUILDER.comment("Legacy pending reservations only. New generation has no fixed center separation.").defineInRange("minimumSettlementDistance", 1500, 384, 16384);
    public static final ModConfigSpec.IntValue MAX_DISTANCE = BUILDER.comment("Desired maximum neighboring candidate distance. Must exceed minimum by at least 64. Unsuitable terrain can leave larger actual gaps.").defineInRange("maximumSettlementDistance", 3000, 448, 32768);
    public static final ModConfigSpec.ConfigValue<List<? extends String>> BIOMES = BUILDER.comment("Biome IDs or #biome tags. Empty permits all suitable Overworld land.")
        .defineListAllowEmpty("allowedBiomes", List.of("minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow", "minecraft:savanna", "minecraft:forest", "minecraft:birch_forest"), value -> value instanceof String s && !s.isBlank());
    public static final ModConfigSpec.IntValue POPULATION = BUILDER.comment("Also serves settlementProgression startingCitizens; existing custom values are preserved.").defineInRange("startingPopulation", 4, 4, 12);
    public static final ModConfigSpec.IntValue RADIUS = BUILDER.comment("Maximum settlement footprint radius in blocks; larger sizes need more simultaneously loaded terrain.").defineInRange("settlementRadius", 112, 64, 192);
    public static final ModConfigSpec.ConfigValue<List<? extends String>> STYLES = BUILDER.comment("Installed style names. A style must provide every required building; no asset fallback or download is performed.")
        .defineListAllowEmpty("buildingStyles", List.of("Medieval Oak", "Medieval Birch", "Birch"), value -> value instanceof String s && !s.isBlank());
    public static final ModConfigSpec.IntValue SCAN_BUDGET = BUILDER.defineInRange("columnsScannedPerTick", 128, 16, 1024);
    public static final ModConfigSpec.IntValue PLACE_BUDGET = BUILDER.defineInRange("placementStepsPerTick", 128, 16, 1024);
    static { BUILDER.push("villageReplacement"); }
    public static final ModConfigSpec.BooleanValue REPLACE_VILLAGES = BUILDER.define("enabled", true);
    public static final ModConfigSpec.DoubleValue REPLACEMENT_CHANCE = BUILDER.defineInRange("replacementChance", 0.5, 0.0, 1.0);
    public static final ModConfigSpec.BooleanValue PRESERVE_FREQUENCY = BUILDER.comment("Retained legacy preference. Vanilla village locations remain unchanged; the new density multiplier adds independent opportunities.").define("preserveVanillaVillageFrequency", true);
    static {
        BUILDER.comment("Safety invariant; existing villages are never replaced.").define("replaceExistingVillages", false, value -> Boolean.FALSE.equals(value));
        BUILDER.comment("Selection always uses seed/dimension/position; cannot be disabled.").define("deterministicSelection", true, value -> Boolean.TRUE.equals(value));
        BUILDER.pop(); BUILDER.push("settlementGeneration");
        BUILDER.comment("Safety invariant: local adaptation, no massive flattening.").define("terrainAdaptation", true, value -> Boolean.TRUE.equals(value));
        BUILDER.comment("Uses the installed generator's heights; no generator replacement.").define("tectonicCompatibility", true, value -> Boolean.TRUE.equals(value));
        BUILDER.define("terralithCompatibility", true, value -> Boolean.TRUE.equals(value));
        BUILDER.define("preventStructureOverlap", true, value -> Boolean.TRUE.equals(value));
    }
    public static final ModConfigSpec.DoubleValue DENSITY = BUILDER.comment("Expected opportunity multiplier; independent seeded candidates supplement village replacements. Not a distance restriction.").defineInRange("spawnFrequencyMultiplier",2.5,0.0,20.0);
    public static final ModConfigSpec.BooleanValue ADAPTIVE = BUILDER.define("adaptivePlacement",true);
    static {
        BUILDER.comment("Legacy distance/biome keys no longer gate adaptive discovery.").define("enforceArbitraryDistanceLimits",false,value->Boolean.FALSE.equals(value));
        BUILDER.define("allowNearbySettlements",true,value->Boolean.TRUE.equals(value));
    }
    public static final ModConfigSpec.BooleanValue ROUGH = BUILDER.define("allowRoughTerrain",true);
    public static final ModConfigSpec.BooleanValue MOUNTAINS = BUILDER.define("allowMountainSettlements",true);
    public static final ModConfigSpec.BooleanValue RIVERS = BUILDER.define("allowRiversideSettlements",true);
    public static final ModConfigSpec.BooleanValue LAKES = BUILDER.define("allowLakesideSettlements",true);
    public static final ModConfigSpec.BooleanValue FORESTS = BUILDER.define("allowForestSettlements",true);
    public static final ModConfigSpec.BooleanValue VALLEYS = BUILDER.define("allowValleySettlements",true);
    public static final ModConfigSpec.BooleanValue COASTS = BUILDER.define("allowCoastalSettlements",true);
    static {
        BUILDER.pop(); BUILDER.push("autonomousDevelopment");
    }
    public static final ModConfigSpec.BooleanValue AUTONOMOUS = BUILDER.define("enabled", true);
    public static final ModConfigSpec.BooleanValue OFFLINE = BUILDER.comment("Accumulate bounded development intent from real snapshots; no fabricated physical construction or resource gathering.").define("simulateUnloadedColonies", true);
    public static final ModConfigSpec.IntValue UPDATES = BUILDER.defineInRange("maxSettlementUpdatesPerTick", 2, 1, 8);
    static {
        BUILDER.define("forceLoadChunks", false, value -> Boolean.FALSE.equals(value));
        BUILDER.pop();
    }
    static { BUILDER.push("autonomousMayor"); }
    public static final ModConfigSpec.BooleanValue MAYOR = BUILDER.define("enabled", true);
    public static final ModConfigSpec.IntValue DECISION_INTERVAL = BUILDER.defineInRange("decisionIntervalTicks",1200,200,24000);
    public static final ModConfigSpec.BooleanValue AUTO_CONSTRUCTION = BUILDER.define("automaticConstruction",true);
    public static final ModConfigSpec.BooleanValue AUTO_UPGRADES = BUILDER.define("automaticUpgrades",true);
    public static final ModConfigSpec.BooleanValue AUTO_JOBS = BUILDER.define("automaticJobAssignments",true);
    public static final ModConfigSpec.BooleanValue AUTO_POPULATION = BUILDER.define("automaticPopulationGrowth",true);
    public static final ModConfigSpec.BooleanValue AUTO_RESOURCES = BUILDER.define("automaticResourceManagement",true);
    public static final ModConfigSpec.BooleanValue AUTO_DEFENSE = BUILDER.define("automaticDefensePlanning",true);
    public static final ModConfigSpec.BooleanValue TERRAIN_PLANNING = BUILDER.define("terrainAwarePlanning",true);
    public static final ModConfigSpec.BooleanValue AUTO_EXPANSION = BUILDER.define("automaticExpansion",true);
    public static final ModConfigSpec.BooleanValue AUTO_TOOLS = BUILDER.define("automaticToolRequests",true);
    public static final ModConfigSpec.BooleanValue AUTO_INFRASTRUCTURE = BUILDER.define("automaticInfrastructurePlanning",true);
    static { BUILDER.pop();BUILDER.push("settlementProgression"); }
    public static final ModConfigSpec.BooleanValue PROGRESSION = BUILDER.define("enabled",true);
    public static final ModConfigSpec.BooleanValue TOWN_HALL_START = BUILDER.comment("Only new reservations use Town Hall-only founding. Existing colonies/layouts keep their original bootstrap.").define("startingWithTownHall",true);
    public static final ModConfigSpec.BooleanValue START_RESOURCES = BUILDER.define("startingResources",true);
    public static final ModConfigSpec.IntValue MAX_STAGE = BUILDER.defineInRange("maximumDevelopmentStage",5,1,5);
    public static final ModConfigSpec.IntValue MAX_BUILDINGS = BUILDER.defineInRange("maximumBuildings",32,8,64);
    public static final ModConfigSpec.IntValue MAX_POPULATION = BUILDER.defineInRange("maximumPopulation",32,4,128);
    static { BUILDER.define("requireConstructionResources",true,value->Boolean.TRUE.equals(value));BUILDER.pop();BUILDER.push("offlineProgression"); }
    public static final ModConfigSpec.IntValue CATCHUP_HOURS = BUILDER.comment("Reuses autonomousDevelopment enabled/simulateUnloadedColonies/update limit and forceLoadChunks=false, rather than duplicating those settings.").defineInRange("maximumCatchupHours",24,1,168);
    static { BUILDER.pop();BUILDER.push("terraforming"); }
    public static final ModConfigSpec.BooleanValue TERRAFORM = BUILDER.define("enabled",true);
    public static final ModConfigSpec.BooleanValue EXCAVATE = BUILDER.define("allowExcavation",true);
    public static final ModConfigSpec.BooleanValue FILL = BUILDER.define("allowFilling",true);
    public static final ModConfigSpec.BooleanValue TERRACES = BUILDER.define("allowTerraces",true);
    public static final ModConfigSpec.BooleanValue WALLS = BUILDER.define("allowRetainingWalls",true);
    public static final ModConfigSpec.BooleanValue BRIDGES = BUILDER.define("allowBridges",true);
    public static final ModConfigSpec.BooleanValue STAIRS = BUILDER.define("allowStairways",true);
    public static final ModConfigSpec.BooleanValue ROADS = BUILDER.define("allowTerrainAdaptedRoads",true);
    static {
        BUILDER.define("requireRealWorkers",true,value->Boolean.TRUE.equals(value));
        BUILDER.define("requireTools",true,value->Boolean.TRUE.equals(value));
        BUILDER.define("requireMaterials",true,value->Boolean.TRUE.equals(value));
    }
    public static final ModConfigSpec.IntValue TERRAFORM_BLOCKS = BUILDER.comment("Shared native worker step budget. Multi-block placement and neighbor physics can cause additional raw block changes.").defineInRange("maxBlocksModifiedPerTick",8,1,64);
    public static final ModConfigSpec.IntValue TERRAFORM_WORKERS = BUILDER.defineInRange("maxConcurrentWorkersPerColony",3,1,16);
    static { BUILDER.pop(); }
    public static final ModConfigSpec SPEC = BUILDER.build();
    private NaturalSettlementConfig() {}
}
