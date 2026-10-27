package dev.kingdoms.livingsettlements.gametest;

import dev.kingdoms.livingsettlements.*;
import com.minecolonies.api.colony.IColonyManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;

/** Fresh native Town Hall-only settlement; original-mode fixtures finish before toggling their shared test config. */
final class MayorGameTest {
    private static long lastSimulatedTime;
    static void run(GameTestHelper helper) {
        lastSimulatedTime=0;
        int[] ticks={0};Fixture[][] fixtures={null};
        helper.onEachTick(()->{
            if(fixtures[0]==null){if(++ticks[0]<250||NaturalSettlementGameTest.activeFixtures>0)return;fixtures[0]=new Fixture[]{new Fixture(helper,96),new Fixture(helper,128)};return;}
            for(var fixture:fixtures[0])fixture.tick();
            if(java.util.Arrays.stream(fixtures[0]).allMatch(f->f.done)){NaturalSettlementConfig.MAYOR.set(false);NaturalSettlementConfig.ENABLED.set(false);SettlementGameTests.mayorTestComplete=true;helper.succeed();}
        });
    }
    private static final class Fixture {
        final GameTestHelper helper;final net.minecraft.server.level.ServerLevel level;
        final BlockPos center;final ChunkPos origin;final int minX,minZ,width;
        int chunks,columns,ticks,phase,wait;int firstStock,mayorId,workId;java.util.UUID mayorUuid;
        java.util.concurrent.CompletableFuture<?> saved;
        boolean done;
        boolean reconstructed;
        Fixture(GameTestHelper helper,int startingCell){
            this.helper=helper;level=helper.getLevel();
            NaturalSettlementConfig.ENABLED.set(false);NaturalSettlementConfig.MAYOR.set(true);NaturalSettlementConfig.AUTONOMOUS.set(true);
            NaturalSettlementConfig.TOWN_HALL_START.set(true);NaturalSettlementConfig.PROGRESSION.set(true);NaturalSettlementConfig.PRESERVE_FREQUENCY.set(true);
            NaturalSettlementConfig.ADAPTIVE.set(false); // Preserve the original complete-layout mayor regression.
            NaturalSettlementConfig.POPULATION.set(4);NaturalSettlementConfig.RADIUS.set(112);NaturalSettlementConfig.BIOMES.set(java.util.List.of());
            NaturalSettlementConfig.MAX_POPULATION.set(4);
            NaturalSettlementConfig.SCAN_BUDGET.set(512);NaturalSettlementConfig.PLACE_BUDGET.set(512);NaturalSettlementConfig.DECISION_INTERVAL.set(200);
            int cell=startingCell;NaturalGenerationRules.Candidate c;
            do {c=NaturalGenerationRules.candidateInRange(level.getSeed(),cell++,96,1500,3000,1).orElseThrow();}
            while(NaturalSettlementsData.get(level).at(new BlockPos(c.chunkX()*16+8,0,c.chunkZ()*16+8).asLong())!=null);
            origin=new ChunkPos(c.chunkX(),c.chunkZ());center=new BlockPos(origin.getMiddleBlockX(),0,origin.getMiddleBlockZ());
            minX=(center.getX()-160)>>4;minZ=(center.getZ()-160)>>4;width=((center.getX()+160)>>4)-minX+1;
            level.setChunkForced(origin.x(),origin.z(),true);var chunk=level.getChunk(origin.x(),origin.z());
            var holder=level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(ResourceKey.create(Registries.STRUCTURE,Identifier.withDefaultNamespace("village_plains")));
            var generator=level.getChunkSource().getGenerator();
            var vanilla=holder.value().generate(holder,level.dimension(),level.registryAccess(),generator,generator.getBiomeSource(),level.getChunkSource().randomState(),level.getStructureManager(),level.getSeed(),origin,0,level,b->true);
            var marker=new VillageMarkerPiece(new BlockPos(center.getX(),81,center.getZ()),new PiecesContainer(vanilla.getPieces()),4,112);
            chunk.setStartForStructure(holder.value(),new StructureStart(holder.value(),origin,0,new PiecesContainer(java.util.List.of(marker))));
            NaturalSettlementConfig.ENABLED.set(true);NeoForge.EVENT_BUS.post(new ChunkEvent.Load(chunk,false));
        }
        void force(boolean value){for(int x=minX;x<minX+width;x++)for(int z=minZ;z<minZ+width;z++)level.setChunkForced(x,z,value);}
        void tick(){
            if(done)return;
            ticks++;
            if(ticks<=20)return;
            if(ticks==21){var s=NaturalSettlementsData.get(level).at(center.asLong());helper.assertTrue(s!=null&&s.townHallOnly()&&s.colonyId()==0,"New marker must persist Town Hall-only intent without loading missing chunks");NaturalSettlementConfig.ENABLED.set(false);return;}
            if(chunks<width*width){int i=chunks++;int x=minX+i%width,z=minZ+i/width;level.setChunkForced(x,z,true);level.getChunk(x,z);return;}
            if(columns<321*321){for(int i=0;i<2048&&columns<321*321;i++){int n=columns++;int x=center.getX()-160+n%321,z=center.getZ()-160+n/321;level.setBlock(new BlockPos(x,79,z),Blocks.DIRT.defaultBlockState(),2);level.setBlock(new BlockPos(x,80,z),Blocks.GRASS_BLOCK.defaultBlockState(),2);}return;}
            if(!NaturalSettlementConfig.ENABLED.get()){NaturalSettlementConfig.ENABLED.set(true);NeoForge.EVENT_BUS.post(new ChunkEvent.Load(level.getChunk(origin.x(),origin.z()),false));}
            var s=NaturalSettlementsData.get(level).at(center.asLong());helper.assertTrue(s!=null&&!s.failed(),"Town Hall bootstrap must not fail: "+(s==null?"missing":s.status()));if(!s.complete())return;
            var colony=IColonyManager.getInstance().getColonyByWorld(s.colonyId(),level);var mayor=MayorSavedData.get(level).at(s.colonyId());
            helper.assertTrue(colony!=null&&mayor!=null,"Native colony and saved mayor must exist");
            if(phase==0){
                helper.assertTrue(colony.getServerBuildingManager().getBuildings().size()==1,"Only Town Hall may be completed at founding");
                helper.assertTrue(colony.getServerBuildingManager().getTownHall().getBuildingLevel()==1,"Town Hall must be genuinely completed and registered");
                helper.assertTrue(colony.getCitizenManager().getCurrentCitizenCount()==4,"Mayor must reuse one of four native citizens");
                colony.getCitizenManager().calculateMaxCitizens();helper.assertTrue(colony.getCitizenManager().getMaxCitizens()<=4,"Autonomous population capacity must respect its configured cap");
                var citizen=colony.getCitizenManager().getCivilian(mayor.citizenId());helper.assertTrue(citizen!=null&&citizen.getName().startsWith("Mayor ")&&citizen.getEntity().filter(e->e.isAlive()).isPresent(),"Mayor must be a visible living native citizen");
                mayorId=mayor.citizenId();mayorUuid=mayor.citizenUuid();
                helper.assertTrue(colony.getCitizenManager().getJoblessCitizen().getId()!=mayorId,"Native automatic hiring must exclude the mayor");phase=1;return;
            }
            helper.assertTrue(mayor.citizenId()==mayorId&&mayor.citizenUuid().equals(mayorUuid),"Mayor identity must remain stable");
            if(ticks%500==0)System.out.println("MAYOR_TEST_PROGRESS "+ticks+" "+mayor.report()+" stock="+mayor.stockUnits());
            if(phase==1){
                if(!mayor.kitReady()||colony.getWorkManager().getWorkOrders().isEmpty())return;
                var work=colony.getWorkManager().getWorkOrders().values().iterator().next();var building=colony.getServerBuildingManager().getBuilding(work.getLocation());
                helper.assertTrue(work.getWorkOrderType()==com.minecolonies.api.colony.workorders.WorkOrderType.BUILD&&work.getTargetLevel()==1,"First request must be a real native level-one BUILD: "+work.getWorkOrderType()+" target="+work.getTargetLevel()+" path="+work.getStructurePath());
                helper.assertTrue(building instanceof com.minecolonies.core.colony.buildings.workerbuildings.BuildingBuilder&&building.getBuildingLevel()==0,"First builder must remain unbuilt until actual native construction");
                helper.assertTrue(mayor.roadWaypoints()>0,"First construction must have a surveyed walkable connection and persisted road proposal");
                helper.assertTrue(building.getAllAssignedCitizen().stream().anyMatch(c->c.getId()!=mayorId),"A non-mayor worker must be assigned to the level-zero builder");
                helper.assertTrue(mayor.foundingUnits()>0&&mayor.foundingUnits()<=32768,"Founding supply must be finite and bounded");firstStock=mayor.stockUnits();workId=work.getID();phase=2;wait=0;return;
            }
            if(phase==2){
                if(!reconstructed&&mayor.pendingDelivery().equals("supply")){
                    new MayorService().stopped(new net.neoforged.neoforge.event.server.ServerStoppedEvent(level.getServer()));reconstructed=true;
                }
                if(++wait<600)return;
                helper.assertTrue(reconstructed,"Delivery must survive reconstruction of transient mayor jobs");
                helper.assertTrue(colony.getWorkManager().getWorkOrders().size()==1&&colony.getWorkManager().getWorkOrder(workId)!=null,"Repeated mayor updates must not duplicate construction");
                var builder=colony.getServerBuildingManager().getBuilding(colony.getWorkManager().getWorkOrder(workId).getLocation());var inventory=builder.getItemHandlerCap();boolean items=false;
                for(int slot=0;slot<inventory.getSlots();slot++)if(!inventory.getStackInSlot(slot).isEmpty())items=true;
                helper.assertTrue(items&&mayor.stockUnits()<firstStock,"First builder must receive actual items debited from finite saved stock");
                if(saved==null){MayorSavedData.get(level).setDirty();saved=level.getDataStorage().scheduleSave();return;}if(!saved.isDone())return;saved.join();
                var folder=net.minecraft.world.level.dimension.DimensionType.getStorageFolder(level.dimension(),level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)).resolve("data");
                try(var storage=new net.minecraft.world.level.storage.SavedDataStorage(level,folder,net.minecraft.util.datafix.DataFixers.getDataFixer(),level.registryAccess())){
                    var loaded=storage.get(MayorSavedData.TYPE).at(s.colonyId());helper.assertTrue(loaded.citizenId()==mayorId&&loaded.citizenUuid().equals(mayorUuid)&&loaded.stockUnits()<=firstStock,"Native disk reload must preserve mayor identity and depleted founding stock");
                }
                force(false);phase=3;wait=0;return;
            }
            if(phase==3){
                if(level.getChunkSource().getChunkNow(origin.x(),origin.z())!=null){helper.assertTrue(++wait<10000,"Mayor settlement must unload without permanent tickets");return;}
                var clock=(net.minecraft.world.level.storage.ServerLevelData)level.getLevelData();long before=level.getGameTime();
                try{
                    long future=Math.max(before+3*1728000,lastSimulatedTime+NaturalSettlementConfig.DECISION_INTERVAL.get()*2L);
                    future+=20-future%20;lastSimulatedTime=future+120;
                    clock.setGameTime(future);var service=new MayorService();
                    for(int i=0;i<6;i++){clock.setGameTime(level.getGameTime()+20);service.tick(new net.neoforged.neoforge.event.tick.LevelTickEvent.Post(()->true,level));}
                }finally{clock.setGameTime(before);}
                helper.assertTrue(level.getChunkSource().getChunkNow(origin.x(),origin.z())==null,"Administrative catchup must not load world chunks");
                helper.assertTrue(mayor.report().startsWith("Unloaded administrative forecast"),"Unloaded mayor must persist a bounded administrative plan");
                helper.assertTrue(colony.getCitizenManager().getCurrentCitizenCount()==4&&colony.getServerBuildingManager().getBuildings().values().stream().filter(b->b instanceof com.minecolonies.core.colony.buildings.workerbuildings.BuildingBuilder).allMatch(b->b.getBuildingLevel()==0),"Offline simulation must not invent citizens or completed buildings");
                helper.assertTrue(colony.getWorkManager().getWorkOrders().size()==1,"Offline queue must retain one native work order");
                System.out.println("MAYOR_NATIVE_VERIFIED mayor="+mayorId+" stockInitial="+mayor.foundingUnits()+" remaining="+mayor.stockUnits()+" nativeBuildOrder="+workId);
                var removed=colony.getCitizenManager().getCivilian(mayorId);colony.getCitizenManager().removeCivilian(removed);
                helper.assertTrue(mayor.citizenId()==0&&!MayorService.isMayor(colony,removed),"Native citizen removal must clear identity before IDs can be reused");
                MayorService.ensureMayor(level,colony,s);
                helper.assertTrue(colony.getCitizenManager().getCurrentCitizenCount()==3&&mayor.citizenId()!=mayorId,"Replacement mayor must reuse a remaining citizen, never spawn a duplicate");
                done=true;
            }
        }
    }
}
