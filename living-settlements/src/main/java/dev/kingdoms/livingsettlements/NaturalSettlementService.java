package dev.kingdoms.livingsettlements;

import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.ldtteam.structurize.operations.PlaceStructureOperation;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.core.MineColonies;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ArrayBlockingQueue;

public final class NaturalSettlementService {
    private record Discovery(ServerLevel level, ChunkPos chunk, boolean isNew) {}
    private final Queue<Discovery> discoveries = new ArrayBlockingQueue<>(4096);
    private final Map<Long, RuntimeJob> runtime = new HashMap<>();
    private CompletableFuture<List<NaturalBlueprints.Style>> catalog;
    private int cursor;

    public void started(ServerStartedEvent event) {
        InfrastructureService.restore(event.getServer().overworld());
        catalog = NaturalBlueprints.catalog(NaturalSettlementConfig.STYLES.get().stream().map(String::valueOf).toList());
    }
    public void stopped(ServerStoppedEvent event) { discoveries.clear(); runtime.clear(); VillageFallback.clear();InfrastructureService.clear(); catalog = null; cursor = 0; }
    public void chunkLoaded(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension().equals(Level.OVERWORLD)) {
            discoveries.offer(new Discovery(level, event.getChunk().getPos(), event.isNewChunk()));
        }
    }
    public void playerPlaced(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !(player instanceof FakePlayer)) { invalidate(player.level(), event.getPos()); }
    }
    public void playerBroke(BreakBlockEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && !(player instanceof FakePlayer)) { invalidate(player.level(), event.getPos()); }
    }
    private void invalidate(ServerLevel level, BlockPos pos) {
        if (!level.dimension().equals(Level.OVERWORLD)) { return; }
        MayorService.playerEdited(level,pos);
        InfrastructureService.edited(level,pos);
        var data = NaturalSettlementsData.get(level);
        for (var s : data.settlements()) {
            if (s.protects(pos)) {
                s.failed = true; s.fallback = false; s.status = "Stopped: player edited the generation reservation";
                data.setDirty(); runtime.remove(s.center); VillageFallback.cancel(s.center);
            }
        }
    }
    public void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !level.dimension().equals(Level.OVERWORLD)) { return; }
        if (!NaturalSettlementConfig.ENABLED.get()) { discoveries.clear(); return; }
        var data = NaturalSettlementsData.get(level);
        for (int i = 0; i < 64; i++) {
            Discovery discovery = discoveries.poll();
            if (discovery == null) { break; }
            if (discovery.level == level) {
                var chunk = level.getChunkSource().getChunkNow(discovery.chunk.x(), discovery.chunk.z());
                if (chunk != null) for (var start : chunk.getAllStarts().values()) {
                    var marker = VillageReplacement.marker(start);
                    if (marker != null) {
                        boolean fresh=data.at(new BlockPos(marker.center.getX(),0,marker.center.getZ()).asLong())==null;
                        var s = data.discover(level.getSeed(), new BlockPos(marker.center.getX(), 0, marker.center.getZ()), marker.population, marker.radius);
                        if(fresh){s.townHallOnly=marker.townHallOnly;s.adaptive=marker.adaptive;s.unrestricted=marker.unrestricted;}
                        if (s.villageChunk == Long.MIN_VALUE) {
                            s.villageChunk = start.getChunkPos().pack();
                            var bounds=marker.getBoundingBox(); s.editMinX=bounds.minX(); s.editMaxX=bounds.maxX();
                            s.editMinZ=bounds.minZ(); s.editMaxZ=bounds.maxZ(); data.setDirty();
                        }
                    }
                }
                if (discovery.isNew) discover(level, data, discovery.chunk);
            }
        }
        if (catalog == null || !catalog.isDone()) { return; }
        var pending = data.settlements().stream().filter(s -> !s.complete && (!s.failed || s.fallback)).toList();
        if (pending.isEmpty()) { runtime.clear(); return; }
        var settlement = pending.get(Math.floorMod(cursor++, pending.size()));
        if(settlement.adaptive&&settlement.plans.isEmpty()&&!ColonyIntegration.loaded(level,BlockPos.of(settlement.center))){settlement.status="Waiting for discovery chunks";return;}
        if ((!settlement.adaptive||!settlement.plans.isEmpty())&&!NpcSettlementBootstrap.areaLoaded(level, settlement)) { settlement.status = "Waiting for footprint and native claim chunks to be loaded"; return; }
        try {
            if (settlement.fallback) { VillageFallback.tick(level, data, settlement); return; }
            advance(level, data, settlement);
        }
        catch (RuntimeException ex) {
            settlement.failed = true; settlement.status = "Stopped safely: " + ex.getMessage(); data.setDirty(); runtime.remove(settlement.center);
            if (settlement.villageChunk != Long.MIN_VALUE && settlement.colonyId == 0 && !settlement.status.contains("player edited")) {
                settlement.fallback = true; settlement.status = "Colony plan rejected; waiting for safe native village fallback";
            }
            if (ex instanceof IllegalStateException) {
                LogUtils.getLogger().warn("Natural settlement {} rejected/stopped (partial colony retained): {}", settlement.owner, ex.getMessage());
            } else {
                LogUtils.getLogger().error("Natural settlement {} stopped; partial colony retained, no rollback/deletion", settlement.owner, ex);
            }
        }
    }
    private void discover(ServerLevel level, NaturalSettlementsData data, ChunkPos chunk) {
        if(!NaturalGenerationRules.independentCandidate(level.getSeed(),chunk.x(),chunk.z(),NaturalSettlementConfig.DENSITY.get(),NaturalSettlementConfig.REPLACE_VILLAGES.get()))return;
        var center = new BlockPos(chunk.getMinBlockX() + 8, 0, chunk.getMinBlockZ() + 8);
        if (data.at(center.asLong()) != null || data.settlements().stream().filter(s -> !s.complete && !s.failed).count() >= 64) { return; }
        var s = data.discover(level.getSeed(), center, Math.min(NaturalSettlementConfig.POPULATION.get(),NaturalSettlementConfig.MAX_POPULATION.get()), NaturalSettlementConfig.RADIUS.get());
        s.townHallOnly=NaturalSettlementConfig.TOWN_HALL_START.get();
        s.adaptive=s.townHallOnly&&NaturalSettlementConfig.ADAPTIVE.get();
        s.unrestricted=true;
        if (!separated(level, data, s)) { s.failed = true; s.status = "Rejected: settlement/colony spacing"; data.setDirty(); }
    }
    private boolean separated(ServerLevel level, NaturalSettlementsData data, NaturalSettlementsData.Settlement s) {
        if(s.unrestricted||s.adaptive) {
            for(var other:data.settlements())if(other!=s&&(!other.failed||other.colonyId!=0))
                for(var plot:s.plans)for(var occupied:other.plans)if(plot.box().overlaps(occupied.box(),1))return false;
            return true;
        }
        BlockPos center = BlockPos.of(s.center);
        int distance = Math.max(s.villageChunk == Long.MIN_VALUE ? NaturalSettlementConfig.DISTANCE.get() : s.radius * 2 + 64, MineColonies.getConfig().getServer().minColonyDistance.get() * 16);
        for (var other : data.settlements()) {
            if (other == s || (other.failed && other.colonyId == 0)) { continue; }
            var pos = BlockPos.of(other.center);
            if (!NaturalGenerationRules.separated(center.getX(), center.getZ(), pos.getX(), pos.getZ(), Math.max(distance, s.radius + other.radius + 64))) { return false; }
        }
        for (var colony : IColonyManager.getInstance().getColonies(level)) {
            if (colony.getPermissions().getOwner().equals(s.owner) && colony.getCenter().getX() == center.getX() && colony.getCenter().getZ() == center.getZ()) { continue; }
            if (!NaturalGenerationRules.separated(center.getX(), center.getZ(), colony.getCenter().getX(), colony.getCenter().getZ(), distance)) { return false; }
        }
        return true;
    }
    private void advance(ServerLevel level, NaturalSettlementsData data, NaturalSettlementsData.Settlement s) {
        if (!separated(level, data, s)) { throw new IllegalStateException("A colony/settlement now occupies the required spacing"); }
        var job = runtime.computeIfAbsent(s.center, ignored -> new RuntimeJob());
        // Recover a native colony persisted before our job cursor, never create a second owner/center.
        if (s.colonyId == 0) {
            for (var colony : IColonyManager.getInstance().getColonies(level)) {
                var center = s.plans.isEmpty()?BlockPos.of(s.center):BlockPos.of(s.plans.getFirst().anchor());
                if (colony.getPermissions().getOwner().equals(s.owner) && colony.getCenter().getX() == center.getX() && colony.getCenter().getZ() == center.getZ()) {
                    if (s.plans.isEmpty()) { throw new IllegalStateException("Existing NPC colony found without a saved plan; retained without regenerating"); }
                    s.colonyId = colony.getID(); data.setDirty(); break;
                }
            }
        }
        if ((!s.adaptive||!s.plans.isEmpty())&&!NpcSettlementBootstrap.claimsClear(level, s)) { throw new IllegalStateException("Claimed land or an existing structure intersects the construction/claim footprint"); }
        if (s.plans.isEmpty()) {
            var styles = catalog.join();
            if (styles.isEmpty()) { throw new IllegalStateException("No installed allowed style provides all required blueprints; provision port assets/check buildingStyles"); }
            if (job.planner == null) {
                var style = styles.get(Math.floorMod(s.owner.hashCode(), styles.size()));
                s.style = style.name(); job.planner = new NaturalSettlementPlanner(s, style);
            }
            s.status = "Planning " + s.style;
            job.planner.tick(level);
            if (job.planner.rejection != null) { throw new IllegalStateException(job.planner.rejection); }
            if (!job.planner.complete()) { return; }
            s.plans.addAll(job.planner.plans); job.planner = null; data.setDirty(); return;
        }
        if (s.colonyId == 0) {
            // Recheck every voxel incrementally immediately before the first world mutation.
            int budget = NaturalSettlementConfig.SCAN_BUDGET.get() * 8;
            while (job.checkPlan < s.plans.size() && budget-- > 0) {
                var p = s.plans.get(job.checkPlan);
                int index = job.checkBlock++;
                int x = index % p.sizeX(), z = (index / p.sizeX()) % p.sizeZ(), y = index / (p.sizeX() * p.sizeZ());
                var pos = new BlockPos(p.minX() + x, p.minY() + y, p.minZ() + z);
                if (level.getBlockEntity(pos) != null || !NaturalSettlementPlanner.natural(level.getBlockState(pos))) {
                    throw new IllegalStateException("Terrain changed or contains a protected block entity/building");
                }
                if (job.checkBlock == p.sizeX() * p.sizeY() * p.sizeZ()) { job.checkBlock = 0; job.checkPlan++; }
            }
            if (job.checkPlan < s.plans.size()) { s.status = "Final protected-block check"; return; }
            if (job.reservationSave == null) {
                data.setDirty(); job.reservationSave = SettlementPersistence.save(level,data,s);
                s.status = "Saving founding reservation before world changes"; return;
            }
            if (!job.reservationSave.isDone()) { return; }
            job.reservationSave.join(); // Already complete; never block the server waiting for disk IO.
            NpcSettlementBootstrap.recoverOrFound(level, s); data.setDirty(); return;
        }
        IColony colony = IColonyManager.getInstance().getColonyByWorld(s.colonyId, level);
        if (!NpcSettlementBootstrap.stillOwned(colony, s)) { throw new IllegalStateException("Colony missing or ownership changed; generation paused permanently"); }
        NpcSettlementBootstrap.addRealVisitors(level, colony);
        if (s.nextBuilding < (s.townHallOnly?1:s.plans.size())) {
            if(s.adaptive&&s.nextBuilding==0&&!supportTownHall(level,data,s,job))return;
            var plan = s.plans.get(s.nextBuilding); s.status = "Placing " + plan.role() + " " + (s.nextBuilding + 1) + "/" + s.plans.size();
            if (job.operation == null) {
                if (job.loading == null) { job.loading = NaturalBlueprints.load(level, s.style, plan.path(), plan.role()); return; }
                if (!job.loading.isDone()) { return; }
                var blueprint = job.loading.join(); job.loading = null;
                if (blueprint == null) { throw new IllegalStateException("Saved schematic is no longer available"); }
                blueprint=NaturalBlueprints.orient(level,blueprint,plan.rotation());
                job.operation = NpcSettlementBootstrap.startBuilding(level, colony, s, plan, blueprint);
            }
            if (job.operation.apply(level)) {
                NpcSettlementBootstrap.finishBuilding(level, colony, s, plan);
                s.nextBuilding++; data.setDirty(); job.operation = null;
            }
            return;
        }
        s.status = "Populating and assigning native citizens";
        if (!NpcSettlementBootstrap.populateOne(level, colony, s.population)) { return; }
        if(NaturalSettlementConfig.MAYOR.get())MayorService.ensureMayor(level,colony,s);
        NpcSettlementBootstrap.assignAndSupply(level, colony, s, false);
        if (!s.suppliesGranted) {
            s.suppliesGranted = true; data.setDirty();
            job.supplySave = SettlementPersistence.save(level,data,s);
            s.status = "Saving one-time supply marker"; return;
        }
        if (job.supplySave != null) {
            if (!job.supplySave.isDone()) { return; }
            job.supplySave.join();
            NpcSettlementBootstrap.assignAndSupply(level, colony, s, true);
            job.supplySave = null;
        }
        s.complete = true; s.status = "Active NPC colony; finite starter supplies granted";
        GovernorSavedData.get(level).enable(s.colonyId, s.owner, 2); data.setDirty(); runtime.remove(s.center);
        LogUtils.getLogger().info("Natural MineColonies settlement {} completed: colony {}, style {}, {} citizens", s.owner, s.colonyId, s.style, s.population);
    }
    public static ServerPlayer npcActor(ServerLevel level, IColony colony, UUID authorizer) {
        if (!level.dimension().equals(Level.OVERWORLD) || !colony.getPermissions().getOwner().equals(authorizer)
                || !NaturalSettlementsData.get(level).ownsNpcColony(colony.getID(), authorizer)) { return null; }
        return NpcSettlementBootstrap.actor(level, authorizer);
    }
    private static final class RuntimeJob {
        NaturalSettlementPlanner planner;
        CompletableFuture<Blueprint> loading;
        PlaceStructureOperation operation;
        CompletableFuture<?> reservationSave, supplySave;
        int checkPlan, checkBlock;
        boolean supportOperation;
    }
    private static boolean supportTownHall(ServerLevel level,NaturalSettlementsData data,NaturalSettlementsData.Settlement s,RuntimeJob job) {
        var plan=s.plans.getFirst();int width=(plan.sizeX()+3)/4,depth=(plan.sizeZ()+3)/4;
        if(s.supportCursor>=width*depth)return true;
        if(job.supportOperation) {
            if(job.operation.apply(level)){job.operation=null;job.supportOperation=false;data.setDirty();}
            return false;
        }
        int x=plan.minX()+(s.supportCursor%width)*4,z=plan.minZ()+(s.supportCursor/width)*4,top=plan.minY()-1;
        var ground=AdaptiveTerrain.ground(level,x,z);if(ground==null)return false;
        while(!level.getBlockState(ground).getFluidState().isEmpty()&&ground.getY()>level.getMinY())ground=ground.below();
        if(ground.getY()>=top){s.supportCursor++;data.setDirty();return false;}
        int height=Math.min(32,top-ground.getY());var start=ground.above();
        var bp=new Blueprint((short)1,(short)height,(short)1,level.registryAccess());bp.setCachePrimaryOffset(BlockPos.ZERO);bp.setMissingMods();
        for(int y=0;y<height;y++) {
            var at=start.above(y);if(level.getBlockEntity(at)!=null||(!NaturalSettlementPlanner.natural(level.getBlockState(at))&&!level.getBlockState(at).is(net.minecraft.world.level.block.Blocks.WATER)))throw new IllegalStateException("Protected founding support column");
            bp.addBlockState(new BlockPos(0,y,0),net.minecraft.world.level.block.Blocks.COBBLESTONE.defaultBlockState());
        }
        var handler=new com.ldtteam.structurize.placement.structure.CreativeStructureHandler(level,start,bp,com.ldtteam.structurize.api.RotationMirror.NONE,false) {
            @Override public int getStepsPerCall(){return NaturalSettlementConfig.PLACE_BUDGET.get();}
            @Override public int getMaxBlocksCheckedPerCall(){return NaturalSettlementConfig.SCAN_BUDGET.get();}
        };
        job.operation=new PlaceStructureOperation(new BudgetedStructurePlacer(handler),null);job.supportOperation=true;return false;
    }
}

