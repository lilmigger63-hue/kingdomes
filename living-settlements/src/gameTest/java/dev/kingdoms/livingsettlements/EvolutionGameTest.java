package dev.kingdoms.livingsettlements.gametest;
import dev.kingdoms.livingsettlements.*;

import com.minecolonies.api.colony.IColonyManager;
import dev.kingdoms.livingsettlements.gametest.SettlementGameTests;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Positive native-noise founding; copied generator sections, never flattened terrain. */
public final class EvolutionGameTest {
    private static void afterMayor(GameTestHelper helper,Runnable check) {
        boolean[] done={false};helper.onEachTick(()->{if(done[0]||!SettlementGameTests.mayorTestComplete)return;done[0]=true;check.run();});
    }
    private static NaturalSettlementsData.Settlement ownedLegacy(net.minecraft.server.level.ServerLevel level) {
        return NaturalSettlementsData.get(level).settlements().stream().filter(s->s.complete()&&!s.failed()&&s.plans().size()>3)
            .filter(s->{var c=IColonyManager.getInstance().getColonyByWorld(s.colonyId(),level);return c!=null&&c.getPermissions().getOwner().equals(s.owner());}).findFirst().orElseThrow();
    }
    public static void unloaded(GameTestHelper helper) {
        Object[] active={null};java.lang.reflect.Method[] advance={null};java.lang.reflect.Field[] rejection={null};int[] calls={0};
        helper.onEachTick(()->{
        if(!SettlementGameTests.mayorTestComplete)return;
        try {
            if(active[0]==null){
            var level=helper.getLevel();var known=ownedLegacy(level);var colony=IColonyManager.getInstance().getColonyByWorld(known.colonyId(),level);
            var ctor=NaturalSettlementsData.Settlement.class.getDeclaredConstructor(long.class,UUID.class,int.class,int.class);ctor.setAccessible(true);
            var center=new BlockPos(-28000000,0,-28000000);var settlement=ctor.newInstance(center.asLong(),UUID.randomUUID(),4,112);
            for(String flag:List.of("adaptive","townHallOnly")){var field=settlement.getClass().getDeclaredField(flag);field.setAccessible(true);field.setBoolean(settlement,true);}
            var styleClass=Class.forName("dev.kingdoms.livingsettlements.NaturalBlueprints$Style");var styleCtor=styleClass.getDeclaredConstructor(String.class,Map.class);styleCtor.setAccessible(true);
            var style=styleCtor.newInstance(colony.getStructurePack(),Map.of("townhall",known.plans().getFirst().path()));
            var type=Class.forName("dev.kingdoms.livingsettlements.NaturalSettlementPlanner");var plannerCtor=type.getDeclaredConstructor(settlement.getClass(),styleClass);plannerCtor.setAccessible(true);var planner=plannerCtor.newInstance(settlement,style);
            var tick=type.getDeclaredMethod("tick",net.minecraft.server.level.ServerLevel.class);tick.setAccessible(true);var rejected=type.getDeclaredField("rejection");rejected.setAccessible(true);
            active[0]=planner;advance[0]=tick;rejection[0]=rejected;
            }
            advance[0].invoke(active[0],helper.getLevel());helper.assertTrue(rejection[0].get(active[0])==null,"Unloaded alternatives must remain retryable, never permanently rejected");
            if(++calls[0]==100){helper.assertTrue(!ColonyIntegration.loaded(helper.getLevel(),new BlockPos(-28000000,0,-28000000)),"Adaptive planning must not load chunks");helper.succeed();}
        }catch(ReflectiveOperationException ex){throw new RuntimeException(ex.getCause()==null?ex:ex.getCause());}
    });}
    public static void editedBuilding(GameTestHelper helper) {afterMayor(helper,()->{
        var level=helper.getLevel();var s=ownedLegacy(level);var colony=IColonyManager.getInstance().getColonyByWorld(s.colonyId(),level);
        var building=colony.getServerBuildingManager().getBuildings().values().stream().filter(b->b.getBuildingLevel()>0&&b instanceof com.minecolonies.core.colony.buildings.workerbuildings.BuildingBuilder).findFirst().orElseThrow();
        MayorService.ensureMayor(level,colony,s);
        var order=com.minecolonies.core.colony.workorders.WorkOrderBuilding.create(com.minecolonies.api.colony.workorders.WorkOrderType.UPGRADE,building);colony.getWorkManager().addWorkOrder(order,true);
        try{MayorService.playerEdited(level,building.getPosition().above());helper.assertTrue(colony.getWorkManager().getWorkOrder(order.getID())==null,"Real player edits must cancel queued native building work even after planning target was cleared");helper.succeed();}
        finally{colony.getWorkManager().removeWorkOrder(order.getID());}
    });}
    public static void blockedInfrastructure(GameTestHelper helper) {afterMayor(helper,()->{
        var level=helper.getLevel();var s=ownedLegacy(level);var colony=IColonyManager.getInstance().getColonyByWorld(s.colonyId(),level);var pos=colony.getCenter().offset(60,1,60);
        var bp=new com.ldtteam.structurize.blueprints.v1.Blueprint((short)1,(short)1,(short)1,level.registryAccess());bp.addBlockState(BlockPos.ZERO,net.minecraft.world.level.block.Blocks.COBBLESTONE.defaultBlockState());
        InfrastructureService.ensure(level,colony,s,"blockedproof",pos,bp);
        try{var edit=InfrastructureService.class.getDeclaredMethod("edited",net.minecraft.server.level.ServerLevel.class,BlockPos.class);edit.setAccessible(true);edit.invoke(null,level,pos);}catch(ReflectiveOperationException ex){throw new RuntimeException(ex);}
        boolean refused=false;try{InfrastructureService.ensure(level,colony,s,"blockedproof",pos,bp);}catch(IllegalStateException expected){refused=true;}
        helper.assertTrue(refused,"Blocked infrastructure must signal replanning instead of remaining pending forever");helper.succeed();
    });}
    public static void run(GameTestHelper helper) {
        var level=helper.getLevel();var origin=new ChunkPos(43,38);var center=new BlockPos(origin.getMiddleBlockX(),0,origin.getMiddleBlockZ());
        int[] ticks={0},index={0},phase={0};
        @SuppressWarnings("unchecked") CompletableFuture<ChunkAccess>[] pending=new CompletableFuture[1];
        NoiseBasedChunkGenerator[] generator={null};RandomState[] random={null};
        @SuppressWarnings("unchecked") CompletableFuture<com.ldtteam.structurize.blueprints.v1.Blueprint>[] metadata=new CompletableFuture[1];
        NativeWorkerProof[] workerProof={null};
        helper.onEachTick(()->{
            if(!SettlementGameTests.mayorTestComplete)return;
            ticks[0]++;
            if(phase[0]==3){if(workerProof[0].tick())helper.succeed();return;}
            if(phase[0]==4) {
                var s=NaturalSettlementsData.get(level).at(center.asLong());var colony=IColonyManager.getInstance().getColonyByWorld(s.colonyId(),level);var mayor=MayorSavedData.get(level).at(colony.getID());
                if(ticks[0]%500==0)System.out.println("EVOLUTION_ADAPTIVE_PROGRESS "+ticks[0]+" "+mayor.report()+" stock="+mayor.stockUnits());
                var builder=colony.getServerBuildingManager().getBuildings().values().stream().filter(b->b instanceof com.minecolonies.core.colony.buildings.workerbuildings.BuildingBuilder).findFirst().orElse(null);
                if(!mayor.kitReady()||builder==null||colony.getWorkManager().getWorkOrders().isEmpty())return;
                helper.assertTrue(builder.getBuildingLevel()==0&&mayor.foundingUnits()>0,"Adaptive founding must fund and queue a genuine level-zero builder");
                System.out.println("EVOLUTION_ADAPTIVE_BUILD colony="+colony.getID()+" finiteKit="+mayor.foundingUnits()+" builderLevel="+builder.getBuildingLevel());
                NaturalSettlementConfig.ENABLED.set(false);NaturalSettlementConfig.MAYOR.set(false);workerProof[0]=new NativeWorkerProof(helper);phase[0]=3;return;
            }
            if(generator[0]==null) {
                NaturalSettlementConfig.ENABLED.set(false);NaturalSettlementConfig.ADAPTIVE.set(true);NaturalSettlementConfig.TOWN_HALL_START.set(true);NaturalSettlementConfig.MAYOR.set(true);NaturalSettlementConfig.AUTONOMOUS.set(true);
                NaturalSettlementConfig.POPULATION.set(4);NaturalSettlementConfig.MAX_POPULATION.set(4);NaturalSettlementConfig.SCAN_BUDGET.set(512);NaturalSettlementConfig.PLACE_BUDGET.set(512);
                var preset=level.registryAccess().lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(ResourceKey.create(Registries.WORLD_PRESET,Identifier.withDefaultNamespace("normal"))).value();
                generator[0]=(NoiseBasedChunkGenerator)preset.overworld().orElseThrow().generator();
                random[0]=RandomState.create(generator[0].generatorSettings().value(),level.registryAccess().lookupOrThrow(Registries.NOISE),3478827910715368652L);
            }
            if(phase[0]==0) {
                if(index[0]>=13*13){phase[0]=1;return;}
                var chunkPos=new ChunkPos(origin.x()-6+index[0]%13,origin.z()-6+index[0]/13);
                if(pending[0]==null) {
                    var proto=new ProtoChunk(chunkPos,UpgradeData.EMPTY,level,level.palettedContainerFactory(),null);
                    pending[0]=generator[0].createBiomes(random[0],Blender.empty(),level.structureManager(),proto)
                        .thenCompose(chunk->generator[0].fillFromNoise(Blender.empty(),random[0],level.structureManager(),chunk));return;
                }
                if(!pending[0].isDone())return;var proto=pending[0].join();pending[0]=null;
                level.setChunkForced(chunkPos.x(),chunkPos.z(),true);var actual=level.getChunk(chunkPos.x(),chunkPos.z());
                System.arraycopy(proto.getSections(),0,actual.getSections(),0,actual.getSections().length);
                Heightmap.primeHeightmaps(actual,EnumSet.allOf(Heightmap.Types.class));actual.markUnsaved();index[0]++;return;
            }
            if(phase[0]==1) {
                var holder=level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(ResourceKey.create(Registries.STRUCTURE,Identifier.withDefaultNamespace("village_plains")));
                var flat=level.getChunkSource().getGenerator();var vanilla=holder.value().generate(holder,level.dimension(),level.registryAccess(),flat,flat.getBiomeSource(),level.getChunkSource().randomState(),level.getStructureManager(),level.getSeed(),origin,0,level,b->true);
                var marker=new VillageMarkerPiece(new BlockPos(center.getX(),80,center.getZ()),new PiecesContainer(vanilla.getPieces()),4,112);
                var chunk=level.getChunk(origin.x(),origin.z());chunk.setStartForStructure(holder.value(),new StructureStart(holder.value(),origin,0,new PiecesContainer(List.of(marker))));
                NaturalSettlementConfig.ENABLED.set(true);NeoForge.EVENT_BUS.post(new ChunkEvent.Load(chunk,false));phase[0]=2;return;
            }
            var s=NaturalSettlementsData.get(level).at(center.asLong());
            if(ticks[0]%500==0)System.out.println("EVOLUTION_NOISE_PROGRESS "+ticks[0]+" "+(s==null?"not discovered":s.status()));
            helper.assertTrue(s==null||!s.failed(),"Actual generated terrain founding failed: "+(s==null?"":s.status()));
            if(s==null||!s.complete())return;
            var colony=IColonyManager.getInstance().getColonyByWorld(s.colonyId(),level);
            helper.assertTrue(colony!=null&&colony.getServerBuildingManager().getTownHall()!=null,"Actual noise terrain needs a registered Town Hall");
            helper.assertTrue(colony.getCitizenManager().getCurrentCitizenCount()==4,"Actual noise terrain needs four real citizens");
            helper.assertTrue(MayorSavedData.get(level).at(colony.getID()).citizenId()>0,"Actual noise terrain needs a designated mayor");
            helper.assertTrue(s.plans().size()==1,"Founding must not depend on fitting every future workplace/home");
            if(metadata[0]==null){metadata[0]=com.ldtteam.structurize.storage.StructurePacks.getBlueprintFuture(colony.getStructurePack(),s.plans().getFirst().path(),level.registryAccess());return;}
            if(!metadata[0].isDone())return;
            var original=metadata[0].join();var rotated=NaturalBlueprints.orient(level,original,1);
            helper.assertTrue(java.util.Objects.equals(original.getFilePath(),rotated.getFilePath())&&java.util.Objects.equals(original.getFileName(),rotated.getFileName()),"Rotated native blueprints must retain required source file metadata");
            System.out.println("EVOLUTION_NOISE_FOUNDED colony="+colony.getID()+" citizens=4 center="+colony.getCenter()+" tectonic="+net.neoforged.fml.ModList.get().isLoaded("tectonic"));
            phase[0]=4;
        });
    }
    private static final class NativeWorkerProof {
        final GameTestHelper helper;final net.minecraft.server.level.ServerLevel level;
        final NaturalSettlementsData.Settlement settlement;final com.minecolonies.api.colony.IColony colony;
        final com.minecolonies.api.colony.buildings.IBuilding builder;
        final net.minecraft.server.level.ServerPlayer visitor;
        final BlockPos position;final com.ldtteam.structurize.blueprints.v1.Blueprint blueprint;
        int ticks;boolean queued;
        int initialCobble,initialShovelDamage;
        NativeWorkerProof(GameTestHelper helper) {
            this.helper=helper;level=helper.getLevel();
            settlement=NaturalSettlementsData.get(level).settlements().stream().filter(s->s.complete()&&!s.failed()&&s.plans().size()>3)
                .filter(s->{var c=IColonyManager.getInstance().getColonyByWorld(s.colonyId(),level);return c!=null&&c.getPermissions().getOwner().equals(s.owner())&&c.getServerBuildingManager().getBuildings().values().stream().anyMatch(b->b instanceof com.minecolonies.core.colony.buildings.workerbuildings.BuildingBuilder&&b.getBuildingLevel()>0);}).findFirst().orElseThrow();
            colony=IColonyManager.getInstance().getColonyByWorld(settlement.colonyId(),level);
            builder=colony.getServerBuildingManager().getBuildings().values().stream().filter(b->b instanceof com.minecolonies.core.colony.buildings.workerbuildings.BuildingBuilder&&b.getBuildingLevel()>0).findFirst().orElseThrow();
            var center=colony.getCenter();
            for(int x=(center.getX()-160)>>4;x<=(center.getX()+160)>>4;x++)for(int z=(center.getZ()-160)>>4;z<=(center.getZ()+160)>>4;z++)level.setChunkForced(x,z,true);
            var p=builder.getPosition();BlockPos chosen=null;
            for(int r=8;r<40&&chosen==null;r++)for(var direction:Direction.Plane.HORIZONTAL) {
                var at=p.relative(direction,r);int y=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,at.getX(),at.getZ())-1;at=new BlockPos(at.getX(),y,at.getZ());
                var probe=at;
                if(settlement.plans().stream().anyMatch(plan->probe.getX()>=plan.minX()&&probe.getX()<plan.minX()+plan.sizeX()&&probe.getZ()>=plan.minZ()&&probe.getZ()<plan.minZ()+plan.sizeZ()))continue;
                if(level.getBlockState(at).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)&&level.getBlockState(at.above()).isAir()){chosen=at;break;}
            }
            position=java.util.Objects.requireNonNull(chosen,"No open native worker proof plot");
            for(var order:new ArrayList<>(colony.getWorkManager().getWorkOrders().values()))colony.getWorkManager().removeWorkOrder(order);
            var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"worker-proof"),false);
            visitor=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,cookie.gameProfile(),cookie.clientInformation());
            var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
            new io.netty.channel.embedded.EmbeddedChannel(connection);
            // Test-only visitor: no remote client negotiated NeoForge custom payloads.
            new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),connection,visitor,cookie) {
                @Override public void send(net.minecraft.network.protocol.Packet<?> packet) { }
            };
            visitor.setGameMode(net.minecraft.world.level.GameType.CREATIVE);visitor.teleportTo(center.getX()+.5,81,center.getZ()+.5);level.addNewPlayer(visitor);colony.getPackageManager().addCloseSubscriber(visitor);
            var worker=builder.getAllAssignedCitizen().iterator().next();var inventory=worker.getInventory();net.minecraft.world.item.ItemStack tool=net.minecraft.world.item.ItemStack.EMPTY;
            for(int slot=0;slot<inventory.getSlots();slot++)if(inventory.getStackInSlot(slot).is(net.minecraft.tags.ItemTags.SHOVELS)){tool=inventory.extractItem(slot,1,false);break;}
            helper.assertTrue(!tool.isEmpty(),"Founding worker must own a real shovel");
            var store=colony.getServerBuildingManager().getTownHall().getItemHandlerCap();var remaining=tool.copy();
            for(int slot=0;slot<store.getSlots()&&!remaining.isEmpty();slot++)remaining=store.insertItem(slot,remaining,false);
            helper.assertTrue(remaining.isEmpty(),"Test shovel transfer must not lose the real item");
            try{var equip=Class.forName("dev.kingdoms.livingsettlements.MayorResources").getDeclaredMethod("equipOne",net.minecraft.server.level.ServerLevel.class,com.minecolonies.api.colony.IColony.class);equip.setAccessible(true);equip.invoke(null,level,colony);}catch(ReflectiveOperationException ex){throw new RuntimeException(ex);}
            helper.assertTrue(shovelDamage()>=0,"Mayor must reacquire a real stored shovel for the worker");initialShovelDamage=shovelDamage();initialCobble=cobbleCount();
            blueprint=new com.ldtteam.structurize.blueprints.v1.Blueprint((short)1,(short)1,(short)1,level.registryAccess());blueprint.addBlockState(BlockPos.ZERO,net.minecraft.world.level.block.Blocks.COBBLESTONE.defaultBlockState());
        }
        boolean tick() {
            ticks++;colony.getPackageManager().addCloseSubscriber(visitor);
            boolean complete=InfrastructureService.ensure(level,colony,settlement,"workerproof",position,blueprint);
            queued|=colony.getWorkManager().getWorkOrders().values().stream().anyMatch(w->w instanceof com.minecolonies.core.colony.workorders.WorkOrderDecoration);
            if(ticks%1000==0)System.out.println("EVOLUTION_WORKER_PROGRESS "+ticks+" queued="+queued+" workers="+builder.getAllAssignedCitizen().size()+" block="+level.getBlockState(position));
            helper.assertTrue(ticks<30000,"Native worker did not finish one paid decoration block; inspect tool/path/activity evidence");
            if(!complete)return false;
            helper.assertTrue(queued,"Infrastructure must use a real native decoration order");
            helper.assertTrue(level.getBlockState(position).is(net.minecraft.world.level.block.Blocks.COBBLESTONE),"Native worker must change the actual world block");
            helper.assertTrue(cobbleCount()<initialCobble,"Actual native construction must consume real cobblestone inventory");
            helper.assertTrue(shovelDamage()>initialShovelDamage,"Native excavation must use and wear a real shovel");
            System.out.println("EVOLUTION_NATIVE_WORKER_MODIFIED "+position+" ticks="+ticks+" cobble="+initialCobble+"->"+cobbleCount()+" shovelDamage="+initialShovelDamage+"->"+shovelDamage());
            level.removePlayerImmediately(visitor,net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);return true;
        }
        int shovelDamage(){int damage=-1;for(var citizen:builder.getAllAssignedCitizen())for(int slot=0;slot<citizen.getInventory().getSlots();slot++){var stack=citizen.getInventory().getStackInSlot(slot);if(stack.is(net.minecraft.tags.ItemTags.SHOVELS))damage=Math.max(damage,stack.getDamageValue());}return damage;}
        int cobbleCount(){int count=0;var inv=builder.getItemHandlerCap();for(int slot=0;slot<inv.getSlots();slot++)if(inv.getStackInSlot(slot).is(net.minecraft.world.item.Items.COBBLESTONE))count+=inv.getStackInSlot(slot).getCount();for(var citizen:builder.getAllAssignedCitizen())for(int slot=0;slot<citizen.getInventory().getSlots();slot++)if(citizen.getInventory().getStackInSlot(slot).is(net.minecraft.world.item.Items.COBBLESTONE))count+=citizen.getInventory().getStackInSlot(slot).getCount();return count;}
    }
}
