package dev.kingdoms.livingsettlements;

import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.ldtteam.structurize.api.RotationMirror;
import com.minecolonies.api.colony.*;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.tileentities.AbstractTileEntityColonyBuilding;
import com.minecolonies.core.colony.buildings.modules.*;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingBuilder;
import com.minecolonies.core.colony.buildingextensions.FarmField;
import com.minecolonies.core.blocks.BlockScarecrow;
import com.minecolonies.api.blocks.ModBlocks;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Native citizen identity + bounded needs planning. Never ticks unloaded entities or completes buildings. */
@SuppressWarnings("removal")
public final class MayorService {
    private static final Map<Long,Job> JOBS=new HashMap<>();
    private CompletableFuture<List<NaturalBlueprints.Style>> catalog;
    private int cursor;
    private static final Map<String,String> HUTS=Map.ofEntries(Map.entry("home","residence"),Map.entry("courier","deliveryman"),Map.entry("guard","guardtower"));
    private static final class Job {
        CompletableFuture<Blueprint> blueprint;
        Blueprint oriented;
        CompletableFuture<?> saved;
        Runnable afterSave;
        MayorResources.KitScan kit;
        NaturalSettlementPlanner planner;
        MayorFootpaths route;
        NaturalSettlementsData.BuildingPlan target;
        int checked,kitIndex,escrowVerified;
        NaturalSettlementsData.BuildingPlan kitPlan;
        boolean upgrade;
    }
    public void stopped(ServerStoppedEvent event){JOBS.clear();catalog=null;cursor=0;}
    public static void ensureMayor(ServerLevel level,IColony colony,NaturalSettlementsData.Settlement s) {
        if(!NpcSettlementBootstrap.stillOwned(colony,s))return;
        var data=MayorSavedData.get(level);var m=data.ensure(colony.getID(),s.owner);
        if(!m.owner.equals(s.owner))return;
        var existing=colony.getCitizenManager().getCivilian(m.citizenId);
        if(existing!=null&&Objects.equals(existing.getUUID(),m.citizenUuid))return;
        var candidate=colony.getCitizenManager().getCitizens().stream().filter(c->!c.isChild()&&c.getWorkBuilding()==null&&c.getJob()==null)
            .min(Comparator.comparingInt(ICitizenData::getId)).orElse(null);
        if(candidate==null){m.report="Waiting for an unemployed adult mayor; existing jobs preserved";return;}
        m.citizenId=candidate.getId();m.citizenUuid=candidate.getUUID();
        if(!candidate.getName().startsWith("Mayor "))candidate.setName("Mayor "+candidate.getName());
        m.nextRecruit=Math.max(m.nextRecruit,level.getGameTime()+24000);data.changed(m);
    }
    public static boolean isMayor(IColony colony,ICitizenData citizen) {
        if(!(colony.getWorld() instanceof ServerLevel level)||!level.dimension().equals(Level.OVERWORLD))return false;
        var m=MayorSavedData.get(level).at(colony.getID());
        return m!=null&&m.owner.equals(colony.getPermissions().getOwner())&&m.citizenId==citizen.getId()&&Objects.equals(m.citizenUuid,citizen.getUUID());
    }
    public static boolean deliveryPendingFor(ICitizenData citizen){
        var colony=citizen.getColony();if(!(colony.getWorld() instanceof ServerLevel level)||!level.dimension().equals(Level.OVERWORLD)||citizen.getWorkBuilding()==null)return false;
        var m=MayorSavedData.get(level).at(colony.getID());return m!=null&&m.owner.equals(colony.getPermissions().getOwner())&&m.pendingKind.equals("supply")&&m.pendingTarget==citizen.getWorkBuilding().getPosition().asLong();
    }
    public static void limitPopulation(IColony colony){
        if(!(colony.getWorld() instanceof ServerLevel level)||!level.dimension().equals(Level.OVERWORLD)||!NaturalSettlementConfig.MAYOR.get())return;
        if(!NaturalSettlementsData.get(level).ownsNpcColony(colony.getID(),colony.getPermissions().getOwner()))return;
        var manager=colony.getCitizenManager();int cap=NaturalSettlementConfig.MAX_POPULATION.get();
        if(!NaturalSettlementConfig.AUTO_POPULATION.get())cap=Math.min(cap,manager.getCurrentCitizenCount());
        manager.setMaxCitizens(Math.min(manager.getMaxCitizens(),cap));manager.setPotentialMaxCitizens(Math.min(manager.getPotentialMaxCitizens(),cap));
    }
    public static void removed(IColony colony,ICivilianData citizen){
        if(!(citizen instanceof ICitizenData c)||!isMayor(colony,c)||!(colony.getWorld() instanceof ServerLevel level))return;
        var data=MayorSavedData.get(level);var m=data.at(colony.getID());
        if(c.getName().startsWith("Mayor "))c.setName(c.getName().substring(6));
        m.citizenId=0;m.citizenUuid=null;m.nextDecision=level.getGameTime();m.report="Mayor removed; awaiting another existing unemployed adult";data.changed(m);
    }
    public static void playerEdited(ServerLevel level,BlockPos pos) {
        if(!level.dimension().equals(Level.OVERWORLD))return;
        var data=MayorSavedData.get(level);
        for(var s:NaturalSettlementsData.get(level).settlements()) {
            var colony=IColonyManager.getInstance().getColonyByWorld(s.colonyId,level);
            if(!s.complete||!NpcSettlementBootstrap.stillOwned(colony,s))continue;
            for(var plot:s.plans)if(pos.getX()>=plot.minX()&&pos.getX()<plot.minX()+plot.sizeX()&&pos.getZ()>=plot.minZ()&&pos.getZ()<plot.minZ()+plot.sizeZ()&&pos.getY()>=plot.minY()&&pos.getY()<plot.minY()+plot.sizeY()) {
                s.blockedPlots.add(plot.anchor());NaturalSettlementsData.get(level).setDirty();
                for(var work:new ArrayList<>(colony.getWorkManager().getWorkOrders().values()))if(work instanceof com.minecolonies.core.colony.workorders.WorkOrderBuilding&&work.getLocation().asLong()==plot.anchor())colony.getWorkManager().removeWorkOrder(work);
                var mayor=data.at(s.colonyId);if(mayor!=null){mayor.retryAfter.put(plot.anchor(),Long.MAX_VALUE);data.changed(mayor);}
            }
            var m=data.at(s.colonyId);if(m==null||m.target==Long.MIN_VALUE)continue;
            var job=JOBS.get(s.center);var plan=job!=null?job.target:null;
            if(plan==null)plan=s.plans.stream().filter(p->p.anchor()==m.target).findFirst().orElse(null);
            if(plan!=null&&pos.getX()>=plan.minX()&&pos.getX()<plan.minX()+plan.sizeX()&&pos.getZ()>=plan.minZ()&&pos.getZ()<plan.minZ()+plan.sizeZ()) {
                m.retryAfter.put(m.target,Long.MAX_VALUE);m.target=Long.MIN_VALUE;m.goal="";m.report="Player edited proposed construction footprint; intent canceled";
                if(!m.pendingKind.equals("supply")) {
                    if(m.pendingStage==0&&!m.pendingKind.isEmpty()) {
                        var tag=new net.minecraft.nbt.CompoundTag();tag.put("item",m.pendingItem);var item=MayorResources.item(level,tag);
                        if(level.getBlockState(BlockPos.of(m.pendingTarget)).getBlock().asItem()!=item.getItem())MayorEscrow.refund(m);
                    }
                    MayorEscrow.clear(m);
                }
                data.changed(m);JOBS.remove(s.center);
            }
        }
    }
    public void tick(LevelTickEvent.Post event) {
        if(!(event.getLevel() instanceof ServerLevel level)||!level.dimension().equals(Level.OVERWORLD)||level.getGameTime()%20!=0)return;
        boolean enabled=NaturalSettlementConfig.MAYOR.get()&&NaturalSettlementConfig.AUTONOMOUS.get();
        if(catalog==null&&enabled)catalog=NaturalBlueprints.catalog(NaturalSettlementConfig.STYLES.get().stream().map(String::valueOf).toList());
        var existingMayors=MayorSavedData.get(level);
        var settlements=NaturalSettlementsData.get(level).settlements().stream().filter(s->s.complete&&!s.failed)
            .filter(s->enabled||(existingMayors.at(s.colonyId)!=null&&(existingMayors.at(s.colonyId).pendingKind.equals("supply")||(!existingMayors.at(s.colonyId).pendingKind.isEmpty()&&existingMayors.at(s.colonyId).pendingStage==1)))).toList();if(settlements.isEmpty())return;
        for(int i=0;i<Math.min(settlements.size(),NaturalSettlementConfig.UPDATES.get());i++) {
            var s=settlements.get(Math.floorMod(cursor++,settlements.size()));var colony=IColonyManager.getInstance().getColonyByWorld(s.colonyId,level);
            if(!NpcSettlementBootstrap.stillOwned(colony,s)) {JOBS.remove(s.center);continue;}
            if(!OfflineDevelopment.takeBudget(level))break;
            var data=MayorSavedData.get(level);var m=data.at(s.colonyId);if(!enabled&&(m==null||m.pendingKind.isEmpty()))continue;
            if(m==null)m=data.ensure(s.colonyId,s.owner);if(!m.owner.equals(s.owner))continue;
            try{advance(level,colony,s,data,m);}catch(RuntimeException ex){
                long delay=m.kitInitialized?NaturalSettlementConfig.DECISION_INTERVAL.get():12000;
                m.report="Paused safely: "+ex.getMessage();m.nextDecision=level.getGameTime()+delay;m.pausedUntil=level.getGameTime()+(m.pendingKind.isEmpty()?delay:200);
                if(m.target!=Long.MIN_VALUE)m.retryAfter.put(m.target,level.getGameTime()+24000);
                else if(!m.goal.isBlank())m.goalAfter.put(m.goal,level.getGameTime()+24000);
                if(m.pendingKind.isEmpty())m.target=Long.MIN_VALUE;data.changed(m);JOBS.remove(s.center);
                com.mojang.logging.LogUtils.getLogger().warn("Mayor colony {} paused: {}",s.colonyId,ex.toString());
            }
        }
    }
    private void advance(ServerLevel level,IColony colony,NaturalSettlementsData.Settlement s,MayorSavedData data,MayorSavedData.Mayor m) {
        long now=level.getGameTime();
        if(now<m.pausedUntil)return;
        boolean enabled=NaturalSettlementConfig.MAYOR.get()&&NaturalSettlementConfig.AUTONOMOUS.get();
        if(!ColonyIntegration.loaded(level,colony.getCenter())) {
            if(enabled&&NaturalSettlementConfig.OFFLINE.get()&&(m.lastEconomy<0||now<m.lastEconomy||now-m.lastEconomy>=NaturalSettlementConfig.DECISION_INTERVAL.get())){
                forecast(m,now,NaturalSettlementConfig.CATCHUP_HOURS.get(),NaturalSettlementConfig.MAX_BUILDINGS.get(),NaturalSettlementConfig.MAX_STAGE.get());data.changed(m);
            }
            return;
        }
        var job=JOBS.computeIfAbsent(s.center,ignored->new Job());
        if(job.saved!=null) {
            if(!job.saved.isDone())return;
            job.saved.join();job.saved=null;var action=job.afterSave;job.afterSave=null;
            if(action!=null)action.run();return;
        }
        if(!m.pendingKind.isEmpty()){resumeEscrow(level,colony,s,data,m,job,enabled);return;}
        if(!enabled)return;
        ensureMayor(level,colony,s);NpcSettlementBootstrap.addRealVisitors(level,colony);
        if(m.lastSnapshot<0||now<m.lastSnapshot||now-m.lastSnapshot>=NaturalSettlementConfig.DECISION_INTERVAL.get()||m.snapshot.workPending()!=!colony.getWorkManager().getWorkOrders().isEmpty()) {
            var observed=snapshot(level,colony);if(m.lastSnapshot>=0&&now>m.lastSnapshot){
                m.foodRate=(int)Math.clamp((observed.food()-m.snapshot.food())*1200L/(now-m.lastSnapshot),-4096,4096);
                m.woodRate=(int)Math.clamp((observed.wood()-m.snapshot.wood())*1200L/(now-m.lastSnapshot),-4096,4096);
                m.stoneRate=(int)Math.clamp((observed.stone()-m.snapshot.stone())*1200L/(now-m.lastSnapshot),-4096,4096);
            }
            m.snapshot=observed;m.forecastFood=observed.food();m.forecastWood=observed.wood();m.forecastStone=observed.stone();m.stage=observed.stage();m.lastSnapshot=now;m.lastEconomy=now;
            m.retryAfter.entrySet().removeIf(e->e.getValue()<=now);m.goalAfter.entrySet().removeIf(e->e.getValue()<=now);data.changed(m);
        }
        if(NaturalSettlementConfig.AUTO_JOBS.get())assign(level,colony,m);
        if(NaturalSettlementConfig.AUTO_TOOLS.get())MayorResources.equipOne(level,colony);
        if(NaturalSettlementConfig.AUTO_RESOURCES.get()){MayorResources.feedOne(level,colony);if(MayorResources.moveOne(level,colony))return;}
        if(!m.kitInitialized) {
            if(!s.townHallOnly||!NaturalSettlementConfig.START_RESOURCES.get()){m.kitInitialized=true;data.changed(m);return;}
            if(!NaturalSettlementConfig.PROGRESSION.get()||!NaturalSettlementConfig.AUTO_CONSTRUCTION.get()||NaturalSettlementConfig.MAX_STAGE.get()<2){m.report="Town Hall administration active; automatic construction disabled";return;}
            if(!NpcSettlementBootstrap.areaLoaded(level,s))return;
            var kitPlans=s.plans.stream().filter(p->!p.role().equals("townhall")&&!p.role().equals("field")).toList();
            var kitRoles=s.adaptive?NaturalBlueprints.foundingRoles(s.population).stream().filter(r->!r.equals("townhall")&&!r.equals("field")).toList():kitPlans.stream().map(NaturalSettlementsData.BuildingPlan::role).toList();
            if(s.adaptive&&job.kitPlan==null) {
                if(catalog==null||!catalog.isDone())return;
                var style=catalog.join().stream().filter(st->st.name().equals(s.style)).findFirst().orElseThrow();
                String role=kitRoles.get(job.kitIndex),path=style.paths().get(role);if(role.equals("home"))path=path.replace("residence2.blueprint","residence1.blueprint");
                if(job.blueprint==null){if(job.kitIndex==0){m.stock.clear();m.kitUnits=0;}job.blueprint=NaturalBlueprints.load(level,s.style,path,role);return;}
                if(!job.blueprint.isDone())return;var template=job.blueprint.join();if(template==null)throw new IllegalStateException("Missing finite-kit blueprint");
                var anchor=colony.getCenter();var min=anchor.subtract(template.getPrimaryBlockOffset());
                job.kitPlan=new NaturalSettlementsData.BuildingPlan(role,path,anchor.offset(job.kitIndex,0,0).asLong(),min.getX(),min.getY(),min.getZ(),template.getSizeX(),template.getSizeY(),template.getSizeZ(),1);
            }
            var builder=s.adaptive?job.kitPlan:kitPlans.get(job.kitIndex);
            if(job.kit==null) {
                if(job.blueprint==null){if(job.kitIndex==0){m.stock.clear();m.kitUnits=0;}job.blueprint=NaturalBlueprints.load(level,s.style,builder.path(),builder.role());return;}
                if(!job.blueprint.isDone())return;
                job.kit=new MayorResources.KitScan(level,job.blueprint.join(),builder);job.blueprint=null;
            }
            m.report="Preparing finite founding materials "+(job.kitIndex+1)+"/"+kitRoles.size();
            if(!job.kit.tick(level,m))return;
            job.kit=null;job.kitPlan=null;job.kitIndex++;
            if(job.kitIndex<kitRoles.size())return;
            for(String role:kitRoles)if(!role.equals("builder")) {
                var block=BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("minecolonies","blockhut"+HUTS.getOrDefault(role,role)));
                if(block!=null)MayorResources.addStock(level,m,new ItemStack(block.asItem()));
            }
            var residence=BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("minecolonies","blockhutresidence"));
            if(residence!=null)MayorResources.addStock(level,m,new ItemStack(residence.asItem()));
            MayorResources.addStock(level,m,new ItemStack(ModBlocks.blockScarecrow.asItem()));
            if(s.adaptive) {
                // One real soil allocation for the first 9x9 field, not a recurring refill.
                MayorResources.addStock(level,m,new ItemStack(Items.DIRT,81));
                for(var entry:m.stock)if(MayorResources.item(level,entry).is(Items.DIRT))entry.putBoolean("material",true);
            }
            m.kitInitialized=true;job.kit=null;job.saved=MayorPersistence.save(level,data,m);return;
        }
        // One finite allocation for each original level-one founding plot; no upgrades or later expansion receive free materials.
        var firstBuilder=colony.getServerBuildingManager().getBuildings().values().stream().filter(b->b instanceof BuildingBuilder).findFirst().orElse(null);
        if(s.townHallOnly&&firstBuilder!=null&&NaturalSettlementConfig.AUTO_RESOURCES.get()&&supplyKit(level,colony,data,m,job,firstBuilder))return;
        planRoads(colony,m);
        if(NpcSettlementBootstrap.areaLoaded(level,s))for(var work:new ArrayList<>(colony.getWorkManager().getWorkOrders().values())){
            if(work instanceof com.minecolonies.core.colony.workorders.WorkOrderBuilding&&colony.getServerBuildingManager().getBuilding(work.getLocation())==null){
                colony.getWorkManager().removeWorkOrder(work);m.report="Removed orphaned native building order; replanning without duplicating construction";data.changed(m);return;
            }
        }
        if(!colony.getWorkManager().getWorkOrders().isEmpty()){m.report="Supporting native construction; no duplicate order";return;}
        if(colony.isColonyUnderAttack()){m.goal="defense";m.report="Threat: native civilian shelter and guard AI; expansion paused";data.changed(m);return;}
        if(!NaturalSettlementConfig.PROGRESSION.get())return;
        if(job.target==null&&m.target!=Long.MIN_VALUE){
            var pending=s.plans.stream().filter(p->p.anchor()==m.target).findFirst().orElse(null);
            if(pending!=null&&!pending.role().equals("field")){
                var building=colony.getServerBuildingManager().getBuilding(BlockPos.of(pending.anchor()));
                if(building!=null)setTarget(s,m,job,building,Math.max(1,m.targetLevel));else job.target=pending;
            }else {m.target=Long.MIN_VALUE;data.changed(m);}
        }
        if(job.planner!=null) {
            job.planner.tick(level);if(job.planner.rejection!=null)throw new IllegalStateException(job.planner.rejection);
            if(!job.planner.complete())return;
            s.plans.clear();s.plans.addAll(job.planner.plans);job.planner=null;
            job.saved=SettlementPersistence.save(level,NaturalSettlementsData.get(level),s);return;
        }
        if(job.target!=null){prepareAction(level,colony,s,data,m,job);return;}
        if(now<m.nextDecision)return;
        var decision=MayorPolicy.choose(m.snapshot,NaturalSettlementConfig.MAX_BUILDINGS.get(),NaturalSettlementConfig.MAX_STAGE.get());
        m.goal=decision.role();m.report=decision.reason();m.nextDecision=now+NaturalSettlementConfig.DECISION_INTERVAL.get();data.changed(m);
        if(NaturalSettlementConfig.AUTO_POPULATION.get())recruit(level,colony,m);
        String role=decision.role();
        if(role.equals("idle")||role.equals("support")||role.equals("defense"))return;
        if(role.equals("specialize")) {
            if(NaturalSettlementConfig.AUTO_RESOURCES.get()&&NaturalSettlementConfig.AUTO_UPGRADES.get()&&MayorResearch.start(level,colony,m)){data.changed(m);return;}
            role=List.of("university","sawmill","blacksmith","smeltery","stonemason").stream().filter(r->buildings(colony,r).isEmpty()&&!m.goalAfter.containsKey(r)).findFirst().orElse("upgrade");
        }
        if(m.goalAfter.containsKey(role)){m.report="Blocked need on cooldown; considering safe upgrades";selectUpgrade(level,colony,s,m,job);return;}
        m.goal=role;data.changed(m);
        if(role.equals("upgrade")){selectUpgrade(level,colony,s,m,job);return;}
        if(role.equals("guard")&&!NaturalSettlementConfig.AUTO_DEFENSE.get())return;
        var existing=buildings(colony,role).stream().filter(b->!s.blockedPlots.contains(b.getPosition().asLong())).toList();
        if(role.equals("farmer")&&!existing.isEmpty()&&existing.getFirst().getBuildingLevel()>0) {
            if(needsFieldSite(s)) {
                if(!NaturalSettlementConfig.AUTO_EXPANSION.get()||!NaturalSettlementConfig.AUTO_CONSTRUCTION.get())return;
                if(catalog==null||!catalog.isDone())return;
                var style=catalog.join().stream().filter(st->st.name().equals(s.style)).findFirst().orElse(null);if(style==null)return;
                job.planner=new NaturalSettlementPlanner(s,style,List.of("field"),true);m.report="Surveying a real farm field before farmer upgrades";data.changed(m);return;
            }
            if(field(level,colony,s,data,m,job))return;
        }
        var unfinished=existing.stream().filter(b->b.getBuildingLevel()==0&&!m.retryAfter.containsKey(b.getPosition().asLong())).findFirst().orElse(null);
        if(unfinished!=null){setTarget(s,m,job,unfinished,1);return;}
        if((role.equals("home")||role.equals("farmer"))&&!existing.isEmpty()&&m.snapshot.builder()>=2&&NaturalSettlementConfig.AUTO_UPGRADES.get()) {
            var lower=existing.stream().filter(b->b.getBuildingLevel()>0&&b.getBuildingLevel()<Math.min(m.snapshot.builder(),NaturalSettlementConfig.MAX_STAGE.get())&&b.getBuildingLevel()<b.getMaxBuildingLevel()&&!m.retryAfter.containsKey(b.getPosition().asLong())).min(Comparator.comparingInt(IBuilding::getBuildingLevel)).orElse(null);if(lower!=null){setTarget(s,m,job,lower,lower.getBuildingLevel()+1);return;}
        }
        if(!existing.isEmpty()&&!role.equals("home")) {selectUpgrade(level,colony,s,m,job);return;}
        if(!NaturalSettlementConfig.AUTO_CONSTRUCTION.get()||m.snapshot.buildings()>=NaturalSettlementConfig.MAX_BUILDINGS.get())return;
        String selectedRole=role;
        var nearestBuilder=buildings(colony,"builder").stream().findFirst().map(IBuilding::getPosition).orElse(colony.getCenter());
        var open=s.plans.stream().filter(p->p.role().equals(selectedRole)&&!s.blockedPlots.contains(p.anchor())&&colony.getServerBuildingManager().getBuilding(BlockPos.of(p.anchor()))==null&&!m.retryAfter.containsKey(p.anchor())).min(Comparator.comparingDouble(p->BlockPos.of(p.anchor()).distSqr(nearestBuilder))).orElse(null);
        if(open!=null){job.target=open;job.upgrade=false;m.target=open.anchor();m.targetLevel=1;data.changed(m);return;}
        if(role.equals("home")&&!s.adaptive&&m.snapshot.builder()<2){selectUpgrade(level,colony,s,m,job);return;}
        if(!NaturalSettlementConfig.AUTO_EXPANSION.get())return;
        if(catalog==null||!catalog.isDone())return;
        if(s.plans.size()>=95){m.report="Recorded expansion limit reached; existing buildings retained";return;}
        var style=catalog.join().stream().filter(st->st.name().equals(s.style)).findFirst().orElse(null);
        if(style==null||!style.paths().containsKey(role)){m.goalAfter.put(role,now+24000);m.report="Installed style lacks optional "+role+"; considering other needs after cooldown";data.changed(m);return;}
        job.planner=new NaturalSettlementPlanner(s,style,List.of(role),true);m.report="Surveying safe expansion for "+role;
    }
    static void forecast(MayorSavedData.Mayor m,long now,int hours) {
        forecast(m,now,hours,64,5);
    }
    static void forecast(MayorSavedData.Mayor m,long now,int hours,int maxBuildings,int maxStage) {
        long previous=m.lastSnapshot<0?now:m.lastSnapshot;long elapsed=Math.clamp(now-previous,0,hours*72000L);
        m.forecastFood=(int)Math.clamp(m.snapshot.food()+m.foodRate*(elapsed/1200),0,1_000_000);m.lastEconomy=now;
        m.forecastWood=(int)Math.clamp(m.snapshot.wood()+m.woodRate*(elapsed/1200),0,1_000_000);m.forecastStone=(int)Math.clamp(m.snapshot.stone()+m.stoneRate*(elapsed/1200),0,1_000_000);
        var s=m.snapshot;var projected=new MayorPolicy.Snapshot(s.population(),s.housing(),m.forecastFood,m.forecastWood,m.forecastStone,s.builder(),s.farmer(),s.homes(),s.lumberjack(),s.miner(),s.warehouse(),s.courier(),s.guards(),s.buildings(),s.stage(),s.threatened(),s.workPending());
        var decision=MayorPolicy.choose(projected,maxBuildings,maxStage);m.goal=decision.role();m.report="Unloaded administrative forecast: "+decision.reason()+"; no physical resources/buildings created";
    }
    private static void assign(ServerLevel level,IColony colony,MayorSavedData.Mayor m) {
        for(var citizen:colony.getCitizenManager().getCitizens())if(citizen.getHomeBuilding()==null) {
            for(var home:buildings(colony,"home"))if(home.getBuildingLevel()>0&&ColonyIntegration.buildingAreaLoaded(level,home))for(var module:home.getModulesByType(LivingBuildingModule.class))if(module.assignCitizen(citizen))break;
        }
        for(String role:List.of("builder","farmer","lumberjack","courier","miner","cook","guard","university","sawmill","blacksmith","smeltery","stonemason")) {
            for(var building:buildings(colony,role))if(building.canAssignCitizens()&&ColonyIntegration.buildingAreaLoaded(level,building)) {
                for(var module:building.getModulesByType(WorkerBuildingModule.class)) {
                    if(module.getAssignedCitizen().size()>=module.getModuleMax())continue;
                    var worker=colony.getCitizenManager().getCitizens().stream().filter(c->!c.isChild()&&c.getWorkBuilding()==null&&!isMayor(colony,c)).min(Comparator.comparingInt(ICitizenData::getId)).orElse(null);
                    if(worker!=null)module.assignCitizen(worker);
                }
            }
        }
    }
    private static void recruit(ServerLevel level,IColony colony,MayorSavedData.Mayor m) {
        long now=level.getGameTime();if(now<m.nextRecruit||m.snapshot.food()<m.snapshot.population()*8||m.snapshot.housing()<=m.snapshot.population()||m.snapshot.population()>=NaturalSettlementConfig.MAX_POPULATION.get())return;
        colony.getCitizenManager().calculateMaxCitizens();if(colony.getCitizenManager().getCurrentCitizenCount()>=colony.getCitizenManager().getMaxCitizens())return;
        colony.getCitizenManager().spawnOrCreateCivilian(null,level,List.of(colony.getCenter()),false);m.nextRecruit=now+24000;
    }
    static String role(IBuilding b) {
        String id=BuiltInRegistries.BLOCK.getKey(b.getBuildingType().getBuildingBlock()).getPath().replace("blockhut","");
        return switch(id){case "residence"->"home";case "deliveryman"->"courier";case "guardtower"->"guard";default->id;};
    }
    private static List<IBuilding> buildings(IColony c,String role){return c.getServerBuildingManager().getBuildings().values().stream().filter(b->role(b).equals(role)).sorted(Comparator.comparingLong(b->b.getPosition().asLong())).toList();}
    private static int level(IColony c,String role){return buildings(c,role).stream().mapToInt(IBuilding::getBuildingLevel).max().orElse(0);}
    static MayorPolicy.Snapshot snapshot(ServerLevel level,IColony colony) {
        int food=0,wood=0,stone=0,housing=0;
        for(var inv:MayorResources.inventories(level,colony))for(int slot=0;slot<inv.getSlots();slot++) {
            var stack=inv.getStackInSlot(slot);if(stack.has(DataComponents.FOOD))food+=stack.getCount();
            if(stack.is(net.minecraft.tags.ItemTags.LOGS)||stack.is(net.minecraft.tags.ItemTags.PLANKS))wood+=stack.getCount();
            if(stack.is(Items.COBBLESTONE)||stack.is(Items.STONE))stone+=stack.getCount();
        }
        for(var citizen:colony.getCitizenManager().getCitizens())for(int slot=0;slot<citizen.getInventory().getSlots();slot++)if(citizen.getInventory().getStackInSlot(slot).has(DataComponents.FOOD))food+=citizen.getInventory().getStackInSlot(slot).getCount();
        for(var home:buildings(colony,"home"))if(home.getBuildingLevel()>0)for(var module:home.getModulesByType(LivingBuildingModule.class))housing+=module.getModuleMax();
        int pop=colony.getCitizenManager().getCurrentCitizenCount(),builder=level(colony,"builder"),farmer=level(colony,"farmer");
        if(colony.getServerBuildingManager().getMatchingBuildingExtension(e->e instanceof FarmField f&&f.isTaken()).isEmpty())farmer=0;
        int stage=builder==0?1:farmer==0||housing<pop?2:level(colony,"lumberjack")==0||level(colony,"miner")==0||level(colony,"warehouse")==0||level(colony,"courier")==0?3:level(colony,"guard")==0||builder<3?4:5;
        return new MayorPolicy.Snapshot(pop,housing,Math.min(food,1_000_000),Math.min(wood,1_000_000),Math.min(stone,1_000_000),builder,farmer,buildings(colony,"home").size(),level(colony,"lumberjack"),level(colony,"miner"),level(colony,"warehouse"),level(colony,"courier"),level(colony,"guard"),colony.getServerBuildingManager().getBuildings().size(),stage,colony.isColonyUnderAttack(),!colony.getWorkManager().getWorkOrders().isEmpty());
    }
    private static void planRoads(IColony colony,MayorSavedData.Mayor m) {
        for(var b:colony.getServerBuildingManager().getBuildings().values())if(!role(b).equals("townhall")&&!m.roads.contains(b.getPosition().asLong())&&m.roads.size()<64)m.roads.add(b.getPosition().asLong());
    }
    private static void setTarget(NaturalSettlementsData.Settlement s,MayorSavedData.Mayor m,Job job,IBuilding building,int target) {
        if(s.blockedPlots.contains(building.getPosition().asLong()))return;
        if(m.retryAfter.containsKey(building.getPosition().asLong()))return;
        var old=s.plans.stream().filter(p->p.anchor()==building.getPosition().asLong()).findFirst().orElse(null);
        if(old==null){m.report="No addon-owned footprint recorded for upgrade; retained safely";return;}
        var path=building.getBlueprintPath();if(path==null||!path.endsWith(".blueprint"))return;
        path=path.substring(0,path.length()-11)+target+".blueprint";
        job.target=new NaturalSettlementsData.BuildingPlan(role(building),path,old.anchor(),old.minX(),old.minY(),old.minZ(),old.sizeX(),old.sizeY(),old.sizeZ(),target,old.rotation());
        job.upgrade=building.getBuildingLevel()>0;m.target=old.anchor();m.targetLevel=target;
    }
    private static void selectUpgrade(ServerLevel level,IColony colony,NaturalSettlementsData.Settlement s,MayorSavedData.Mayor m,Job job) {
        if(!NaturalSettlementConfig.AUTO_UPGRADES.get())return;
        var candidates=colony.getServerBuildingManager().getBuildings().values().stream().filter(b->b.getBuildingLevel()>0&&b.getBuildingLevel()<Math.min(5,NaturalSettlementConfig.MAX_STAGE.get())&&!m.retryAfter.containsKey(b.getPosition().asLong())&&!s.blockedPlots.contains(b.getPosition().asLong()))
            .sorted(Comparator.comparingInt((IBuilding b)->role(b).equals("builder")?0:role(b).equals("home")?1:2).thenComparingInt(IBuilding::getBuildingLevel).thenComparingLong(b->b.getPosition().asLong())).toList();
        if(!candidates.isEmpty()){var b=candidates.getFirst();setTarget(s,m,job,b,b.getBuildingLevel()+1);}
    }
    private static boolean supplyKit(ServerLevel level,IColony colony,MayorSavedData data,MayorSavedData.Mayor m,Job job,IBuilding builder) {
        if(!ColonyIntegration.buildingAreaLoaded(level,builder))return false;var inv=builder.getItemHandlerCap();if(inv==null)return false;
        for(var entry:m.stock) {
            var work=colony.getWorkManager().getWorkOrders().values().stream().filter(w->w.getTargetLevel()>0).findFirst().orElse(null);
            if(work==null||!entry.getBooleanOr("material",false)||entry.getIntOr("remaining",0)<=0)continue;
            var stack=MayorResources.item(level,entry).copyWithCount(Math.min(64,entry.getIntOr("remaining",0)));
            if(entry.getLongOr("for",Long.MIN_VALUE)!=work.getLocation().asLong()||work.getTargetLevel()!=1){
                if(!(builder instanceof BuildingBuilder nativeBuilder))continue;
                int required=nativeBuilder.getNeededResources().values().stream().filter(r->ItemStack.isSameItemSameComponents(r.getItemStack(),stack)).mapToInt(r->r.getAmount()).sum()-count(inv,stack);
                for(var citizen:builder.getAllAssignedCitizen())required-=count(citizen.getInventory(),stack);
                if(required<=0)continue;stack.setCount(Math.min(stack.getCount(),required));
            }
            int accepted=MayorResources.insert(inv,stack,true);if(accepted==0)continue;
            var debited=stack.copyWithCount(accepted);entry.putInt("remaining",entry.getIntOr("remaining",0)-accepted);
            beginEscrow(level,data,m,"supply",builder.getPosition().asLong(),debited,count(inv,debited),m.stock.indexOf(entry));job.escrowVerified=0;return true;
        }
        return false;
    }
    private static void prepareAction(ServerLevel level,IColony colony,NaturalSettlementsData.Settlement s,MayorSavedData data,MayorSavedData.Mayor m,Job job) {
        var planned=job.target;if(m.target!=planned.anchor()){job.target=null;job.blueprint=null;return;}
        if(!NpcSettlementBootstrap.areaLoaded(level,s))return;
        if(job.blueprint==null){job.blueprint=NaturalBlueprints.load(level,s.style,planned.path(),planned.role());return;}
        if(!job.blueprint.isDone())return;if(job.oriented==null)job.oriented=NaturalBlueprints.orient(level,job.blueprint.join(),planned.rotation());var bp=job.oriented;if(bp==null||bp.getMissingMods().length>0)throw new IllegalStateException("Missing target blueprint");
        var min=BlockPos.of(planned.anchor()).subtract(bp.getPrimaryBlockOffset());
        var target=new NaturalSettlementsData.BuildingPlan(planned.role(),planned.path(),planned.anchor(),min.getX(),min.getY(),min.getZ(),bp.getSizeX(),bp.getSizeY(),bp.getSizeZ(),planned.level(),planned.rotation());
        if((long)target.sizeX()*target.sizeY()*target.sizeZ()>180000)throw new IllegalStateException("Oversized development blueprint");
        var center=BlockPos.of(s.center);if(target.minX()<center.getX()-s.radius||target.minZ()<center.getZ()-s.radius||target.minX()+target.sizeX()>center.getX()+s.radius+1||target.minZ()+target.sizeZ()>center.getZ()+s.radius+1)throw new IllegalStateException("Upgrade exceeds reserved expansion area");
        if(s.plans.stream().anyMatch(other->other.anchor()!=target.anchor()&&target.box().overlaps(other.box(),5)))throw new IllegalStateException("Upgrade footprint overlaps another reserved building");
        var existing=colony.getServerBuildingManager().getBuilding(BlockPos.of(target.anchor()));var old=s.plans.stream().filter(p->p.anchor()==target.anchor()).findFirst().orElse(null);
        int volume=target.sizeX()*target.sizeY()*target.sizeZ();
        for(int budget=NaturalSettlementConfig.SCAN_BUDGET.get()*8;budget>0&&job.checked<volume;budget--) {
            int i=job.checked++;var pos=new BlockPos(target.minX()+i%target.sizeX(),target.minY()+i/(target.sizeX()*target.sizeZ()),target.minZ()+(i/target.sizeX())%target.sizeZ());
            if(!ColonyIntegration.loaded(level,pos))return;
            var chunk=level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);
            var claim=IColonyManager.getInstance().getClaimData(level.dimension(),chunk.getPos());
            if(!level.getWorldBorder().isWithinBounds(pos)||(claim!=null&&claim.getOwningColony()!=0&&claim.getOwningColony()!=s.colonyId)||!VillageReplacement.structuresClear(level,chunk,s))throw new IllegalStateException("Protected claim/structure in development footprint");
            boolean insideOld=existing!=null&&((existing.getBuildingLevel()>0&&old!=null&&pos.getX()>=old.minX()&&pos.getX()<old.minX()+old.sizeX()&&pos.getZ()>=old.minZ()&&pos.getZ()<old.minZ()+old.sizeZ()&&pos.getY()>=old.minY()&&pos.getY()<old.minY()+old.sizeY())||pos.equals(existing.getPosition()));
            if(!insideOld&&(level.getBlockEntity(pos)!=null||!NaturalSettlementPlanner.natural(level.getBlockState(pos))))throw new IllegalStateException("Protected block in new development footprint");
        }
        if(job.checked<volume)return;
        if(existing!=null){
            if(NaturalSettlementConfig.MAX_STAGE.get()<2||target.level()>NaturalSettlementConfig.MAX_STAGE.get()){finishAction(level,data,m,job);return;}
            if((existing.getBuildingLevel()>0&&!NaturalSettlementConfig.AUTO_UPGRADES.get())||(existing.getBuildingLevel()==0&&!NaturalSettlementConfig.AUTO_CONSTRUCTION.get())){finishAction(level,data,m,job);return;}
            if(request(level,colony,m,target))recordTarget(level,s,target);finishAction(level,data,m,job);return;
        }
        if(!NaturalSettlementConfig.AUTO_CONSTRUCTION.get())return;
        if(job.route==null){job.route=new MayorFootpaths(level,s,target);return;}
        job.route.tick(level);if(job.route.rejection!=null)throw new IllegalStateException(job.route.rejection);if(!job.route.complete())return;
        for(long point:job.route.roadPoints())if(m.roadPoints.size()<2048&&!m.roadPoints.contains(point))m.roadPoints.add(point);
        boolean builderReady=buildings(colony,"builder").stream().anyMatch(b->b.getBuildingLevel()>0);
        if(s.adaptive&&builderReady) {
            if(!InfrastructureService.routes(level,colony,s,job.route.roadPoints())){m.report="Native workers preparing access infrastructure";return;}
            if(!InfrastructureService.foundation(level,colony,s,target)){m.report="Native workers constructing supported foundations";return;}
        }
        var wanted=new ItemStack(bp.getBlockState(bp.getPrimaryBlockOffset()).getBlock().asItem());if(wanted.isEmpty())throw new IllegalStateException("Target has no legitimate hut item");
        ItemStack consumed=ItemStack.EMPTY;int stockSource=-1;
        for(var source:MayorResources.inventories(level,colony)){consumed=MayorResources.extract(source,wanted,1);if(!consumed.isEmpty())break;}
        if(consumed.isEmpty()){stockSource=MayorResources.stockIndex(level,m,wanted);consumed=MayorResources.debitStock(level,m,wanted,1);}
        if(consumed.isEmpty()) {
            var hall=colony.getServerBuildingManager().getTownHall();
            boolean requested=hall.getOpenRequests(-1).stream().anyMatch(r->r.getRequest() instanceof com.minecolonies.api.colony.requestsystem.requestable.IDeliverable deliverable&&deliverable.matches(wanted));
            if(!requested)hall.createRequest(new com.minecolonies.api.colony.requestsystem.requestable.Stack(wanted),false);
            m.report="Waiting for native hut-item production/delivery: "+wanted.getHoverName().getString();finishAction(level,data,m,job);return;
        }
        beginEscrow(level,data,m,"hut",target.anchor(),consumed,0,stockSource);job.escrowVerified=0;
    }
    private static boolean request(ServerLevel level,IColony colony,MayorSavedData.Mayor m,NaturalSettlementsData.BuildingPlan target) {
        var actor=NaturalSettlementService.npcActor(level,colony,m.owner);
        if(actor==null||!ColonyIntegration.requestUpgrade(level,colony,actor,target.anchor())) {
            m.retryAfter.put(target.anchor(),level.getGameTime()+12000);m.report="Native prerequisites/resources/builder declined request; cooldown applied";m.attempts=Math.min(100,m.attempts+1);return false;
        } else {m.report="Native construction queued: "+target.role()+" to level "+target.level();m.attempts=0;return true;}
    }
    private static void recordTarget(ServerLevel level,NaturalSettlementsData.Settlement s,NaturalSettlementsData.BuildingPlan target){
        for(int i=0;i<s.plans.size();i++)if(s.plans.get(i).anchor()==target.anchor()){s.plans.set(i,target);NaturalSettlementsData.get(level).setDirty();return;}
    }
    private static void finishAction(ServerLevel level,MayorSavedData data,MayorSavedData.Mayor m,Job job){job.target=null;job.blueprint=null;job.oriented=null;job.route=null;job.checked=0;m.target=Long.MIN_VALUE;data.changed(m);}
    static boolean needsFieldSite(NaturalSettlementsData.Settlement s){return s.adaptive&&s.plans.stream().noneMatch(p->p.role().equals("field")&&!s.blockedPlots.contains(p.anchor()));}
    private static boolean field(ServerLevel level,IColony colony,NaturalSettlementsData.Settlement s,MayorSavedData data,MayorSavedData.Mayor m,Job job) {
        if(!NaturalSettlementConfig.AUTO_CONSTRUCTION.get())return false;
        if(colony.getServerBuildingManager().getMatchingBuildingExtension(e->e instanceof FarmField).isPresent())return false;
        var plan=s.plans.stream().filter(p->p.role().equals("field")&&!s.blockedPlots.contains(p.anchor())&&!m.retryAfter.containsKey(p.anchor())).findFirst().orElse(null);if(plan==null||!ready(level,colony,s,plan))return false;
        if(s.adaptive) {
            if(!InfrastructureService.foundation(level,colony,s,plan)||!InfrastructureService.fieldSoil(level,colony,s,plan)){m.report="Native builder preparing real farm soil and lighting";m.nextDecision=level.getGameTime()+20;data.changed(m);return true;}
        }
        var pos=BlockPos.of(plan.anchor());
        if(!fieldClear(level,pos)){m.retryAfter.put(plan.anchor(),level.getGameTime()+24000);m.report="Field footprint obstructed; no item spent";data.changed(m);return false;}
        var item=new ItemStack(ModBlocks.blockScarecrow.asItem());int source=MayorResources.stockIndex(level,m,item);var consumed=MayorResources.debitStock(level,m,item,1);if(consumed.isEmpty())return false;
        m.target=plan.anchor();job.target=plan;beginEscrow(level,data,m,"field",plan.anchor(),consumed,0,source);job.escrowVerified=0;return true;
    }
    private static void beginEscrow(ServerLevel level,MayorSavedData data,MayorSavedData.Mayor m,String kind,long target,ItemStack item,int before,int source){
        m.pendingKind=kind;m.pendingItem=MayorResources.itemTag(level,item);m.pendingTarget=target;m.pendingCount=item.getCount();m.pendingBefore=before;m.pendingStage=0;m.pendingSource=source;data.changed(m);
    }
    private static int count(net.neoforged.neoforge.items.IItemHandler inventory,ItemStack wanted){int count=0;for(int i=0;i<inventory.getSlots();i++)if(ItemStack.isSameItemSameComponents(inventory.getStackInSlot(i),wanted))count+=inventory.getStackInSlot(i).getCount();return count;}
    private static void resumeEscrow(ServerLevel level,IColony colony,NaturalSettlementsData.Settlement s,MayorSavedData data,MayorSavedData.Mayor m,Job job,boolean enabled){
        if(job.escrowVerified!=m.pendingStage+1){int stage=m.pendingStage;job.saved=MayorPersistence.save(level,data,m);job.afterSave=()->job.escrowVerified=stage+1;return;}
        var tag=new net.minecraft.nbt.CompoundTag();tag.put("item",m.pendingItem);var item=MayorResources.item(level,tag);
        var target=colony.getServerBuildingManager().getBuilding(BlockPos.of(m.pendingTarget));
        if(m.pendingStage==1){
            if(m.pendingKind.equals("hut")&&enabled&&NaturalSettlementConfig.AUTO_CONSTRUCTION.get()&&NaturalSettlementConfig.PROGRESSION.get()&&NaturalSettlementConfig.MAX_STAGE.get()>1&&target!=null&&target.getBuildingLevel()==0&&colony.getWorkManager().getWorkOrders().isEmpty()){
                if(NaturalSettlementConfig.AUTO_JOBS.get())assign(level,colony,m);
                var plan=s.plans.stream().filter(p->p.anchor()==m.pendingTarget).findFirst().orElse(null);if(plan!=null&&request(level,colony,m,plan))recordTarget(level,s,plan);
            }
            MayorEscrow.clear(m);job.escrowVerified=0;finishAction(level,data,m,job);return;
        }
        if(m.pendingKind.equals("supply")){
            if(target==null){if(ColonyIntegration.loaded(level,BlockPos.of(m.pendingTarget)))cancelPaid(level,data,m,job,"Delivery recipient removed; stock refunded",true);return;}
            if(!ColonyIntegration.buildingAreaLoaded(level,target))return;var inventory=target.getItemHandlerCap();if(inventory==null)return;
            int already=Math.min(m.pendingCount,Math.max(0,count(inventory,item)-m.pendingBefore));
            int inserted=MayorResources.insert(inventory,item.copyWithCount(m.pendingCount-already),false);
            if(inserted>0)target.overruleNextOpenRequestWithStack(item.copyWithCount(inserted));
            MayorEscrow.received(m,already+inserted);target.markDirty();data.changed(m);job.escrowVerified=0;return;
        }
        if(!enabled||!NaturalSettlementConfig.AUTO_CONSTRUCTION.get()||!NaturalSettlementConfig.PROGRESSION.get()||NaturalSettlementConfig.MAX_STAGE.get()<2||colony.isColonyUnderAttack())return;
        var plan=s.plans.stream().filter(p->p.anchor()==m.pendingTarget).findFirst().orElse(null);if(plan==null||!NpcSettlementBootstrap.areaLoaded(level,s))return;
        if(!ready(level,colony,s,plan)){
            var at=BlockPos.of(plan.anchor());
            cancelPaid(level,data,m,job,"Protected development site canceled; paid item retained/refunded safely",level.getBlockState(at).getBlock().asItem()!=item.getItem());return;
        }
        var pos=BlockPos.of(plan.anchor());
        if(m.pendingKind.equals("field")){
            boolean existing=colony.getServerBuildingManager().getMatchingBuildingExtension(e->e instanceof FarmField&&e.getPosition().equals(pos)).isPresent();
            if(!existing){
                if(!fieldClear(level,pos)){
                    cancelPaid(level,data,m,job,"Obstructed field canceled; unplaced kit item refunded",!level.getBlockState(pos).is(ModBlocks.blockScarecrow));return;
                }
                if(buildings(colony,"farmer").stream().noneMatch(b->b.getBuildingLevel()>0))return;
                level.setBlock(pos,ModBlocks.blockScarecrow.defaultBlockState().setValue(BlockScarecrow.HALF,DoubleBlockHalf.LOWER),3);level.setBlock(pos.above(),ModBlocks.blockScarecrow.defaultBlockState().setValue(BlockScarecrow.HALF,DoubleBlockHalf.UPPER),3);
                NpcSettlementBootstrap.finishBuilding(level,colony,s,plan);
            }
            MayorEscrow.received(m,1);data.changed(m);job.escrowVerified=0;return;
        }
        if(target==null){
            if(job.route==null){job.route=new MayorFootpaths(level,s,plan);return;}job.route.tick(level);
            if(job.route.rejection!=null){cancelPaid(level,data,m,job,"Changed access route canceled; paid item refunded",level.getBlockState(pos).getBlock().asItem()!=item.getItem());return;}
            if(!job.route.complete())return;
            if(job.blueprint==null){job.blueprint=NaturalBlueprints.load(level,s.style,plan.path(),plan.role());return;}if(!job.blueprint.isDone())return;var bp=job.blueprint.join();
            if(bp==null)throw new IllegalStateException("Paid hut blueprint missing; escrow retained");
            bp=NaturalBlueprints.orient(level,bp,plan.rotation());
            if(level.getBlockEntity(pos)==null){if(!NaturalSettlementPlanner.natural(level.getBlockState(pos)))return;level.setBlock(pos,bp.getBlockState(bp.getPrimaryBlockOffset()),3);}
            if(!(level.getBlockEntity(pos) instanceof AbstractTileEntityColonyBuilding tile))return;
            var min=pos.subtract(bp.getPrimaryBlockOffset());tile.setPackName(s.style);tile.setBlueprintPath(plan.path());tile.setRotationMirror(RotationMirror.NOT_MIRRORED[plan.rotation()]);tile.setSchematicCorners(min,new BlockPos(min.getX()+bp.getSizeX()-1,min.getY()+bp.getSizeY()-1,min.getZ()+bp.getSizeZ()-1));
            target=colony.getServerBuildingManager().addNewBuilding(tile,level);if(target==null)throw new IllegalStateException("Native colony rejected paid hut");target.setBuildingLevel(0);target.setCorners(min,new BlockPos(min.getX()+bp.getSizeX()-1,min.getY()+bp.getSizeY()-1,min.getZ()+bp.getSizeZ()-1));
        }
        MayorEscrow.received(m,1);data.changed(m);job.escrowVerified=0;
    }
    private static void cancelPaid(ServerLevel level,MayorSavedData data,MayorSavedData.Mayor m,Job job,String reason,boolean refund){
        if(refund)MayorEscrow.refund(m);m.retryAfter.put(m.pendingTarget,level.getGameTime()+24000);MayorEscrow.clear(m);m.report=reason;job.escrowVerified=0;finishAction(level,data,m,job);
    }
    private static boolean fieldClear(ServerLevel level,BlockPos pos){return level.getWorldBorder().isWithinBounds(pos)&&level.getWorldBorder().isWithinBounds(pos.above())&&level.getBlockState(pos).canBeReplaced()&&level.getBlockState(pos.above()).canBeReplaced()&&level.getBlockEntity(pos)==null&&level.getBlockEntity(pos.above())==null;}
    private static boolean ready(ServerLevel level,IColony colony,NaturalSettlementsData.Settlement s,NaturalSettlementsData.BuildingPlan plan){
        if(!NpcSettlementBootstrap.areaLoaded(level,s))return false;
        for(int x=plan.minX()>>4;x<=(plan.minX()+plan.sizeX()-1)>>4;x++)for(int z=plan.minZ()>>4;z<=(plan.minZ()+plan.sizeZ()-1)>>4;z++){
            var chunk=level.getChunkSource().getChunkNow(x,z);if(chunk==null||!VillageReplacement.structuresClear(level,chunk,s))return false;
            var claim=IColonyManager.getInstance().getClaimData(level.dimension(),chunk.getPos());if(claim!=null&&claim.getOwningColony()!=0&&claim.getOwningColony()!=colony.getID())return false;
        }
        return true;
    }
}
