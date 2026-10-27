package dev.kingdoms.livingsettlements;

import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.ldtteam.structurize.blueprints.v1.BlueprintTagUtils;
import com.minecolonies.api.blocks.AbstractBlockHut;
import com.minecolonies.api.colony.IColonyManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Incremental planning: no colony is created until every building has a safe footprint. */
final class NaturalSettlementPlanner {
    final NaturalSettlementsData.Settlement settlement;
    final List<NaturalSettlementsData.BuildingPlan> plans = new ArrayList<>();
    private final NaturalBlueprints.Style style;
    private final List<String> roles;
    private CompletableFuture<Blueprint> loading;
    private Blueprint blueprint;
    private Blueprint sourceBlueprint;
    private Blueprint[] orientations;
    private int roleIndex;
    private int townHallGround;
    private int attempt;
    private boolean sawUnloaded;
    private Scan scan;
    private final boolean expansion;
    String rejection;
    NaturalSettlementPlanner(NaturalSettlementsData.Settlement settlement, NaturalBlueprints.Style style) {
        this(settlement,style,settlement.adaptive?List.of("townhall"):settlement.townHallOnly?NaturalBlueprints.foundingRoles(settlement.population):NaturalBlueprints.roles(settlement.population),false);
    }
    NaturalSettlementPlanner(NaturalSettlementsData.Settlement settlement,NaturalBlueprints.Style style,List<String> roles,boolean expansion) {
        this.settlement=settlement;this.style=style;this.roles=List.copyOf(roles);this.expansion=expansion;
        if(expansion){plans.addAll(settlement.plans);townHallGround=settlement.groundY!=0?settlement.groundY:BlockPos.of(settlement.plans.getFirst().anchor()).getY()-1;}
    }
    boolean complete() { return roleIndex == roles.size(); }
    void tick(ServerLevel level) {
        if(settlement.adaptive&&!ColonyIntegration.loaded(level,BlockPos.of(settlement.center))){settlement.status="Waiting for discovery chunks";return;}
        long deadline = System.nanoTime() + 3_000_000L;
        int budget = NaturalSettlementConfig.SCAN_BUDGET.get();
        while (budget > 0 && System.nanoTime() < deadline && !complete() && rejection == null) {
            String role = roles.get(roleIndex);
            if (blueprint == null) {
                if (loading == null) { loading = NaturalBlueprints.load(level, style.name(), path(role), role); return; }
                if (!loading.isDone()) { return; }
                blueprint = loading.join(); loading = null;
                sourceBlueprint=blueprint;
                orientations=new Blueprint[]{blueprint,null,null,null};
                if (blueprint == null || blueprint.getMissingMods().length > 0 || blueprint.getSizeX() > 96
                        || blueprint.getSizeZ() > 96 || blueprint.getSizeY() > 96
                        || (long) blueprint.getSizeX() * blueprint.getSizeZ() * blueprint.getSizeY() > 180_000) {
                    rejection = "Missing/unsupported/oversized blueprint for " + role; return;
                }
                if (!role.equals("field") && !(blueprint.getBlockState(blueprint.getPrimaryBlockOffset()).getBlock() instanceof AbstractBlockHut<?>)) {
                    rejection = "Blueprint has no unambiguous hut anchor: " + role; return;
                }
            }
            if (scan == null) {
                if (attempt > (settlement.adaptive?4*(1+settlement.radius*settlement.radius/64):160)) {
                    if(settlement.adaptive&&sawUnloaded){attempt=0;sawUnloaded=false;settlement.status="Waiting for unvisited alternatives to load";return;}
                    rejection = "No safe accessible non-overlapping location for " + role; return;
                }
                BlockPos center = BlockPos.of(settlement.center);
                int site=settlement.adaptive?attempt/4:attempt,rotation=settlement.adaptive&&(!expansion||NaturalSettlementConfig.TERRAIN_PLANNING.get())?attempt%4:0;
                if(settlement.adaptive){if(orientations[rotation]==null)orientations[rotation]=NaturalBlueprints.orient(level,sourceBlueprint,rotation);blueprint=orientations[rotation];}
                int dx = 0, dz = 0;
                if (roleIndex > 0 || expansion || (settlement.adaptive&&site>0)) {
                    double angle = site * 2.399963229728653;
                    double radius = 18 + Math.sqrt(site) * 8;
                    dx = (int) Math.round(Math.cos(angle) * radius); dz = (int) Math.round(Math.sin(angle) * radius);
                }
                attempt++;
                BlockPos base = role.equals("field") ? BlockPos.of(plans.stream().filter(p -> p.role().equals("farmer")).findFirst().orElseThrow().anchor()) : center;
                BlockPos anchorColumn = new BlockPos(base.getX() + dx, 0, base.getZ() + dz);
                if (!ColonyIntegration.loaded(level, anchorColumn)) { if(settlement.adaptive){sawUnloaded=true;continue;}settlement.status = "Waiting for footprint chunks"; return; }
                var terrain=settlement.adaptive?AdaptiveTerrain.ground(level,anchorColumn.getX(),anchorColumn.getZ()):null;
                if(settlement.adaptive&&!AdaptiveTerrain.dry(level,terrain))continue;
                if(settlement.adaptive&&!environmentAllowed(level,terrain))continue;
                int ground = settlement.adaptive?terrain.getY():level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, anchorColumn.getX(), anchorColumn.getZ()) - 1;
                if ((!settlement.adaptive||(expansion&&!NaturalSettlementConfig.TERRAIN_PLANNING.get()))&&(roleIndex > 0||expansion) && !NaturalGenerationRules.sameTerrainBand(townHallGround, ground)) { continue; }
                int offset = role.equals("field") ? 1 : BlueprintTagUtils.getGroundAnchorOffset(blueprint, 1);
                BlockPos anchor = new BlockPos(anchorColumn.getX(), ground + offset, anchorColumn.getZ());
                BlockPos min = anchor.subtract(blueprint.getPrimaryBlockOffset());
                var plan = new NaturalSettlementsData.BuildingPlan(role, path(role), anchor.asLong(), min.getX(), min.getY(), min.getZ(),
                    blueprint.getSizeX(), blueprint.getSizeY(), blueprint.getSizeZ(), role.equals("home")&&!settlement.townHallOnly ? 2 : role.equals("field") ? 0 : 1,rotation);
                var bounds = plan.box();
                if(settlement.adaptive&&NaturalSettlementsData.get(level).settlements().stream().filter(other->other!=settlement&&(!other.failed||other.colonyId!=0)).flatMap(other->other.plans.stream()).anyMatch(other->bounds.overlaps(other.box(),1)))continue;
                if(settlement.adaptive&&!expansion&&!initialClaimsClear(level,anchor))continue;
                if (Math.max(Math.abs(bounds.minX() - center.getX()), Math.abs(bounds.maxX() - center.getX())) > settlement.radius
                        || Math.max(Math.abs(bounds.minZ() - center.getZ()), Math.abs(bounds.maxZ() - center.getZ())) > settlement.radius
                        || plans.stream().anyMatch(other -> bounds.overlaps(other.box(), 5))
                        || min.getY() < level.getMinY() + 1 || min.getY() + plan.sizeY() >= level.getMaxY()) {
                    if (roleIndex == 0&&!expansion&&!settlement.adaptive) { rejection = "Town hall does not fit terrain/build height"; return; }
                    continue;
                }
                scan = new Scan(plan, ground, settlement);
            }
            if (!scan.column(level)) {
                if(scan.unloaded)sawUnloaded=true;
                if (scan.unloaded&&!settlement.adaptive) { settlement.status = "Waiting for footprint chunks"; return; }
                if (roleIndex == 0&&!expansion&&!settlement.adaptive) { rejection = "Town hall terrain rejected: " + scan.reason; }
                scan = null;
            } else if (scan.finished()) {
                if(settlement.adaptive) {
                    var exits=SchematicAccess.exits(level,scan.plan,blueprint,!expansion);
                    if(exits.isEmpty()){scan=null;continue;}
                    settlement.accesses.put(scan.plan.anchor(),exits);
                }
                if (roleIndex == 0&&!expansion) { townHallGround = scan.ground;settlement.groundY=scan.ground; }
                plans.add(scan.plan); scan = null; blueprint = null; roleIndex++; attempt = 0;
            }
            budget--;
        }
    }
    private String path(String role) { var path=role.equals("field")?"natural_field":style.paths().get(role);return role.equals("home")&&settlement.townHallOnly&&path!=null?path.replace("residence2.blueprint","residence1.blueprint"):path; }
    private static boolean environmentAllowed(ServerLevel level,BlockPos pos) {
        var biome=level.getBiome(pos);
        if(!NaturalSettlementConfig.VALLEYS.get()) {
            int higher=0;for(var direction:Direction.Plane.HORIZONTAL){var sample=pos.relative(direction,24);var ground=AdaptiveTerrain.ground(level,sample.getX(),sample.getZ());if(ground!=null&&ground.getY()>pos.getY()+3)higher++;}
            if(higher>=3)return false;
        }
        if(!level.getBlockState(pos).getFluidState().isEmpty()) {
            if(biome.is(BiomeTags.IS_RIVER))return NaturalSettlementConfig.RIVERS.get();
            if(biome.is(BiomeTags.IS_OCEAN)||biome.is(BiomeTags.IS_BEACH))return NaturalSettlementConfig.COASTS.get();
            return NaturalSettlementConfig.LAKES.get();
        }
        return (NaturalSettlementConfig.MOUNTAINS.get()||!biome.is(BiomeTags.IS_MOUNTAIN))
            &&(NaturalSettlementConfig.FORESTS.get()||!biome.is(BiomeTags.IS_FOREST))
            &&(NaturalSettlementConfig.RIVERS.get()||!biome.is(BiomeTags.IS_RIVER))
            &&(NaturalSettlementConfig.COASTS.get()||(!biome.is(BiomeTags.IS_BEACH)&&!biome.is(BiomeTags.IS_OCEAN)));
    }
    private boolean initialClaimsClear(ServerLevel level,BlockPos anchor) {
        int radius=com.minecolonies.core.MineColonies.getConfig().getServer().initialColonySize.get()*16;
        if(radius>512)return false;
        for(int x=(anchor.getX()-radius)>>4;x<=(anchor.getX()+radius)>>4;x++)for(int z=(anchor.getZ()-radius)>>4;z<=(anchor.getZ()+radius)>>4;z++) {
            var claim=IColonyManager.getInstance().getClaimData(level.dimension(),new net.minecraft.world.level.ChunkPos(x,z));
            if(claim!=null&&claim.getOwningColony()!=0&&claim.getOwningColony()!=settlement.colonyId)return false;
        }
        return true;
    }
    static boolean biomeAllowed(ServerLevel level, BlockPos ground) {
        return biomeAllowed(level, ground, false);
    }
    static boolean biomeAllowed(ServerLevel level, BlockPos ground, boolean village) {
        var biome = level.getBiome(ground);
        if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_RIVER)) { return false; }
        var allowed = NaturalSettlementConfig.BIOMES.get();
        if (village || allowed.isEmpty()) { return true; }
        return allowed.stream().anyMatch(value -> {
            Identifier id = Identifier.tryParse(value.startsWith("#") ? value.substring(1) : value);
            return id != null && (value.startsWith("#") ? biome.is(TagKey.create(Registries.BIOME, id)) : biome.is(ResourceKey.create(Registries.BIOME, id)));
        });
    }
    static boolean natural(net.minecraft.world.level.block.state.BlockState state) {
        return state.isAir() || state.canBeReplaced() || state.is(BlockTags.DIRT) || state.is(Blocks.GRASS_BLOCK)
            || state.is(Blocks.PODZOL) || state.is(Blocks.MYCELIUM) || state.is(Blocks.DIRT_PATH) || state.is(Blocks.MOSS_BLOCK)
            || state.is(BlockTags.BASE_STONE_OVERWORLD)
            || state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES) || state.is(BlockTags.SAND)
            || state.is(BlockTags.TERRACOTTA) || state.is(Blocks.GRAVEL) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.CLAY)
            || state.is(Blocks.SANDSTONE) || state.is(Blocks.RED_SANDSTONE);
    }
    private static final class Scan {
        final NaturalSettlementsData.BuildingPlan plan;
        final int ground;
        final NaturalSettlementsData.Settlement settlement;
        int column;
        boolean unloaded;
        String reason;
        Scan(NaturalSettlementsData.BuildingPlan plan, int ground, NaturalSettlementsData.Settlement settlement) { this.plan = plan; this.ground = ground; this.settlement = settlement; }
        boolean finished() { return column == plan.sizeX() * plan.sizeZ(); }
        boolean column(ServerLevel level) {
            int x = plan.minX() + column % plan.sizeX(), z = plan.minZ() + column / plan.sizeX();
            BlockPos pos = new BlockPos(x, ground, z);
            var chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
            unloaded = chunk == null;
            if (unloaded) { return false; }
            if (!level.getWorldBorder().isWithinBounds(pos) || !VillageReplacement.structuresClear(level, chunk, settlement)) { reason = "World border or generated structure at " + pos; return false; }
            var claim = IColonyManager.getInstance().getClaimData(level.dimension(), chunk.getPos());
            if (claim != null && claim.getOwningColony() != 0 && claim.getOwningColony()!=settlement.colonyId) { reason = "Existing claim at " + pos; return false; }
            var actual=settlement.adaptive?AdaptiveTerrain.ground(level,x,z):null;
            if(settlement.adaptive&&actual==null){reason="No supported ground";return false;}
            int surfaceY = settlement.adaptive?actual.getY():level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
            BlockPos surface = new BlockPos(x, surfaceY, z);
            var state = level.getBlockState(surface);
            if(settlement.adaptive&&(!environmentAllowed(level,surface)||(!NaturalSettlementConfig.ROUGH.get()&&Math.abs(surfaceY-ground)>3))) {reason="Disabled terrain category";return false;}
            if(settlement.adaptive&&!state.getFluidState().isEmpty()&&!NaturalSettlementConfig.LAKES.get()&&!NaturalSettlementConfig.RIVERS.get()&&!NaturalSettlementConfig.COASTS.get()){reason="Waterfront terrain disabled";return false;}
            if ((!settlement.adaptive&&(Math.abs(surfaceY - ground) > 3 || !state.getFluidState().isEmpty() || !state.isFaceSturdy(level, surface, Direction.UP)))
                    || state.is(Blocks.MAGMA_BLOCK) || (!settlement.adaptive&&!biomeAllowed(level, surface, settlement.villageChunk != Long.MIN_VALUE))) { reason = "Unsafe ground/biome at " + surface + " (reference " + ground + ", " + state + ")"; return false; }
            for (int y = plan.minY(); y < plan.minY() + plan.sizeY(); y++) {
                BlockPos check = new BlockPos(x, y, z);
                if (level.getBlockEntity(check) != null || !natural(level.getBlockState(check))) { reason = "Protected block at " + check + ": " + level.getBlockState(check); return false; }
            }
            column++; return true;
        }
    }
}
