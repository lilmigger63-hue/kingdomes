package dev.kingdoms.livingsettlements;

import com.ldtteam.structurize.api.RotationMirror;
import com.ldtteam.structurize.blueprints.v1.*;
import com.ldtteam.structurize.storage.StructurePacks;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.workorders.WorkOrderType;
import com.minecolonies.core.colony.workorders.WorkOrderDecoration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Generates native worker schematics. Never places, breaks or grants world blocks/items. */
public final class InfrastructureService {
    static final String PACK="Living Settlements Infrastructure";
    private static final Map<String,CompletableFuture<?>> writes=new HashMap<>();
    private static final Map<Path,CompletableFuture<Void>> packs=new HashMap<>();
    private static final Map<String,Blueprint> blueprints=new HashMap<>();
    private static final Map<String,Integer> scans=new HashMap<>();
    private static final Map<ServerLevel,Map<Integer,Set<Long>>> blocked=new WeakHashMap<>();
    static void clear(){writes.clear();packs.clear();blueprints.clear();scans.clear();blocked.clear();StructurePacks.removePack(PACK);}
    static CompletableFuture<Void> preparePack(Path directory) {
        var root=directory.toAbsolutePath().normalize();
        return packs.computeIfAbsent(root,p->CompletableFuture.runAsync(()->{
            try {
                Files.createDirectories(p);
                Files.writeString(p.resolve("pack.json"),"{\"name\":\""+PACK+"\",\"icon\":\"\",\"authors\":[\"Living Settlements\"],\"desc\":\"Saved native worker infrastructure\",\"mods\":[\"minecolonies\"],\"version\":1,\"pack-format\":1}");
            }catch(java.io.IOException ex){throw new java.io.UncheckedIOException(ex);}
        }));
    }
    static void restore(ServerLevel level) {
        var root=level.getServer().getWorldPath(LevelResource.ROOT).resolve("living_settlements/infrastructure");
        var pack=StructurePacks.getStructurePack(PACK);
        if(Files.isRegularFile(root.resolve("pack.json"))&&(pack==null||!pack.getPath().toAbsolutePath().normalize().equals(root.toAbsolutePath().normalize())))StructurePacks.discoverPackAtPath(root,false,List.of("minecolonies"),false,LivingSettlements.MOD_ID);
    }
    static boolean enabled(){return NaturalSettlementConfig.TERRAFORM.get()&&NaturalSettlementConfig.AUTO_INFRASTRUCTURE.get()&&NaturalSettlementConfig.AUTO_CONSTRUCTION.get()&&NaturalSettlementConfig.PROGRESSION.get();}
    static boolean foundation(ServerLevel level,IColony colony,NaturalSettlementsData.Settlement s,NaturalSettlementsData.BuildingPlan plan) {
        if(!s.adaptive||!enabled()||!NaturalSettlementConfig.FILL.get())return true;
        // Columns support the perimeter and interior beams; native BUILD clears its
        // own bounded building volume. No town-wide terrain leveling is performed.
        for(int x=plan.minX();x<plan.minX()+plan.sizeX();x++)for(int z=plan.minZ();z<plan.minZ()+plan.sizeZ();z++) {
            boolean perimeter=x==plan.minX()||z==plan.minZ()||x==plan.minX()+plan.sizeX()-1||z==plan.minZ()+plan.sizeZ()-1;
            if(!(perimeter&&NaturalSettlementConfig.WALLS.get())&&((x-plan.minX())%4!=0||(z-plan.minZ())%4!=0))continue;
            if(!NaturalSettlementConfig.TERRACES.get()&&!perimeter)continue;
            var terrain=AdaptiveTerrain.ground(level,x,z);if(terrain==null)return false;
            while(!level.getBlockState(terrain).getFluidState().isEmpty()&&terrain.getY()>level.getMinY())terrain=terrain.below();
            int top=plan.minY()-1;
            if(terrain.getY()>=top)continue;
            for(int bottom=terrain.getY()+1;bottom<=top;bottom+=32) {
                int height=Math.min(32,top-bottom+1);var origin=new BlockPos(x,bottom,z);
                var bp=new Blueprint((short)1,(short)height,(short)1,level.registryAccess());
                for(int y=0;y<height;y++)bp.addBlockState(new BlockPos(0,y,0),Blocks.COBBLESTONE.defaultBlockState());
                if(!ensure(level,colony,s,"foundation",origin,bp))return false;
            }
        }
        return true;
    }
    static boolean routes(ServerLevel level,IColony colony,NaturalSettlementsData.Settlement s,List<Long> points) {
        if(!s.adaptive||!enabled()||!NaturalSettlementConfig.ROADS.get())return true;
        // Short segments give native work small resumable resource requests.
        for(int i=0;i<points.size();i+=4) {
            var segment=points.subList(i,Math.min(points.size(),i+4));
            if(segment.isEmpty())continue;
            int minX=Integer.MAX_VALUE,minY=Integer.MAX_VALUE,minZ=Integer.MAX_VALUE,maxX=Integer.MIN_VALUE,maxY=Integer.MIN_VALUE,maxZ=Integer.MIN_VALUE;
            for(long key:segment){var p=BlockPos.of(key);minX=Math.min(minX,p.getX());minY=Math.min(minY,p.getY()-2);minZ=Math.min(minZ,p.getZ());maxX=Math.max(maxX,p.getX());maxY=Math.max(maxY,p.getY()+1);maxZ=Math.max(maxZ,p.getZ());}
            int sx=maxX-minX+1,sy=maxY-minY+1,sz=maxZ-minZ+1;if((long)sx*sy*sz>4096)continue;
            var bp=new Blueprint((short)sx,(short)sy,(short)sz,level.registryAccess());var origin=new BlockPos(minX,minY,minZ);
            for(int x=0;x<sx;x++)for(int y=0;y<sy;y++)for(int z=0;z<sz;z++) {
                var at=origin.offset(x,y,z);if(!ColonyIntegration.loaded(level,at))return false;
                bp.addBlockState(new BlockPos(x,y,z),level.getBlockState(at));
            }
            for(int j=0;j<segment.size();j++) {
                var p=BlockPos.of(segment.get(j));var local=p.subtract(origin);var floor=p.below();
                boolean water=!level.getBlockState(floor).getFluidState().isEmpty();
                if(water&&!NaturalSettlementConfig.BRIDGES.get())return false;
                BlockState surface=water?Blocks.OAK_PLANKS.defaultBlockState():Blocks.COBBLESTONE.defaultBlockState();
                if(j+1<segment.size()) {
                    var next=BlockPos.of(segment.get(j+1));
                    if(next.getY()>p.getY()&&NaturalSettlementConfig.STAIRS.get()) {
                        var direction=Direction.getApproximateNearest(next.getX()-p.getX(),0,next.getZ()-p.getZ());
                        surface=Blocks.COBBLESTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING,direction);
                    }
                }
                bp.addBlockState(local.below(),surface);
                if(NaturalSettlementConfig.EXCAVATE.get()){bp.addBlockState(local,Blocks.AIR.defaultBlockState());bp.addBlockState(local.above(),Blocks.AIR.defaultBlockState());}
            }
            if(!ensure(level,colony,s,"road",origin,bp))return false;
        }
        return true;
    }
    static boolean fieldSoil(ServerLevel level,IColony colony,NaturalSettlementsData.Settlement s,NaturalSettlementsData.BuildingPlan plan) {
        if(!s.adaptive)return true;
        if(!enabled()||!NaturalSettlementConfig.FILL.get()||!NaturalSettlementConfig.EXCAVATE.get()) {
            for(int x=0;x<plan.sizeX();x++)for(int z=0;z<plan.sizeZ();z++) {
                var at=new BlockPos(plan.minX()+x,plan.minY(),plan.minZ()+z);
                if(!level.getBlockState(at).is(net.minecraft.tags.BlockTags.DIRT)&&!level.getBlockState(at).is(Blocks.FARMLAND))throw new IllegalStateException("Field needs soil preparation, but added terraforming is disabled");
            }
            return true;
        }
        var bp=new Blueprint((short)plan.sizeX(),(short)3,(short)plan.sizeZ(),level.registryAccess());
        for(int x=0;x<plan.sizeX();x++)for(int z=0;z<plan.sizeZ();z++) {
            bp.addBlockState(new BlockPos(x,0,z),Blocks.DIRT.defaultBlockState());
            bp.addBlockState(new BlockPos(x,1,z),Blocks.AIR.defaultBlockState());bp.addBlockState(new BlockPos(x,2,z),Blocks.AIR.defaultBlockState());
        }
        for(int[] light:new int[][]{{0,0},{0,8},{8,0},{8,8},{3,4},{5,4},{4,3},{4,5}})if(light[0]<plan.sizeX()&&light[1]<plan.sizeZ())bp.addBlockState(new BlockPos(light[0],1,light[1]),Blocks.TORCH.defaultBlockState());
        return ensure(level,colony,s,"fieldsoil",new BlockPos(plan.minX(),plan.minY(),plan.minZ()),bp);
    }
    public static boolean ensure(ServerLevel level,IColony colony,NaturalSettlementsData.Settlement s,String kind,BlockPos origin,Blueprint proposed) {
        if(!kind.matches("[a-z_-]{1,24}")||!NpcSettlementBootstrap.stillOwned(colony,s))throw new IllegalArgumentException("Only recorded NPC-owned infrastructure is supported");
        String key="c"+colony.getID()+"_"+kind+"_"+Long.toUnsignedString(origin.asLong(),36);
        var data=InfrastructureData.get(level);var task=data.tasks.get(key);
        restore(level);
        if(task==null) {
            if(data.tasks.size()>=131072||data.tasks.values().stream().filter(t->t.colony==colony.getID()).count()>=8192)return false;
            proposed.setCachePrimaryOffset(BlockPos.ZERO);proposed.setMissingMods();
            task=new InfrastructureData.Task(key,colony.getID(),s.owner,origin.asLong(),BlueprintUtil.writeBlueprintToNBT(proposed));
            task.sizeX=proposed.getSizeX();task.sizeY=proposed.getSizeY();task.sizeZ=proposed.getSizeZ();
            data.tasks.put(key,task);data.setDirty();
        }
        if(task.blocked)throw new IllegalStateException("Player-edited/protected infrastructure requires an alternative route or site");
        if(!NpcSettlementBootstrap.stillOwned(colony,s))return false;
        if(task.done)return true;
        var bp=blueprints.computeIfAbsent(key,k->BlueprintUtil.readBlueprintFromNBT(data.tasks.get(k).blueprint,level.registryAccess()));bp.setMissingMods();bp.setCachePrimaryOffset(BlockPos.ZERO);
        int volume=bp.getSizeX()*bp.getSizeY()*bp.getSizeZ();if(volume<1||volume>4096){task.blocked=true;data.setDirty();return false;}
        var existing=colony.getWorkManager().getWorkOrders().values().stream().filter(w->w instanceof WorkOrderDecoration&&w.getStructurePack().equals(PACK)&&w.getStructurePath().equals(key+".blueprint")).findFirst().orElse(null);
        if(existing!=null){task.order=existing.getID();return false;}
        int scan=scans.getOrDefault(key,0);long deadline=System.nanoTime()+3_000_000;
        for(int budget=NaturalSettlementConfig.SCAN_BUDGET.get();budget>0&&scan<volume&&System.nanoTime()<deadline;budget--,scan++) {
            var local=new BlockPos(scan%bp.getSizeX(),scan/(bp.getSizeX()*bp.getSizeZ()),(scan/bp.getSizeX())%bp.getSizeZ());var pos=origin.offset(local);
            if(!ColonyIntegration.loaded(level,pos))return false;
            if(!safe(level,s,pos,bp.getBlockState(local))){task.blocked=true;blocked.remove(level);data.setDirty();throw new IllegalStateException("Protected infrastructure footprint requires replanning");}
        }
        scans.put(key,scan);if(scan<volume)return false;
        boolean equal=true;
        for(int i=0;i<volume;i++){var local=new BlockPos(i%bp.getSizeX(),i/(bp.getSizeX()*bp.getSizeZ()),(i/bp.getSizeX())%bp.getSizeZ());if(level.getBlockState(origin.offset(local))!=bp.getBlockState(local)){equal=false;break;}}
        if(equal){task.done=true;task.blueprint=new CompoundTag();data.setDirty();scans.remove(key);blueprints.remove(key);writes.remove(key);return true;}
        if(!colony.getWorkManager().getWorkOrders().isEmpty())return false;
        var builder=colony.getServerBuildingManager().getBuildings().values().stream().filter(b->b instanceof com.minecolonies.core.colony.buildings.workerbuildings.BuildingBuilder&&b.getBuildingLevel()>0).findFirst().orElse(null);
        if(builder==null)return false;
        var write=writes.get(key);
        if(write==null) {
            var root=level.getServer().getWorldPath(LevelResource.ROOT).resolve("living_settlements/infrastructure");var tag=task.blueprint.copy();
            writes.put(key,preparePack(root).thenRunAsync(()->{
                try {
                    NbtIo.writeCompressed(tag,root.resolve(key+".blueprint"));
                }catch(java.io.IOException ex){throw new java.io.UncheckedIOException(ex);}
            }));return false;
        }
        if(!write.isDone())return false;
        try{write.join();}catch(java.util.concurrent.CompletionException ex){writes.remove(key);packs.remove(level.getServer().getWorldPath(LevelResource.ROOT).resolve("living_settlements/infrastructure").toAbsolutePath().normalize());throw new IllegalStateException("Infrastructure IO failed; retry remains possible",ex);}
        var packPath=level.getServer().getWorldPath(LevelResource.ROOT).resolve("living_settlements/infrastructure");
        if(!StructurePacks.hasPack(PACK))StructurePacks.discoverPackAtPath(packPath,false,List.of("minecolonies"),false,LivingSettlements.MOD_ID);
        var order=WorkOrderDecoration.create(WorkOrderType.BUILD,PACK,key+".blueprint",kind,origin,RotationMirror.NONE,0);
        order.setBlueprint(bp,level);order.setClaimedBy(builder.getPosition());order.setPriority(1);
        colony.getWorkManager().addWorkOrder(order,false);task.order=order.getID();data.setDirty();scans.remove(key);
        return false;
    }
    static boolean safe(ServerLevel level,NaturalSettlementsData.Settlement s,BlockPos pos,BlockState desired) {
        if(!ColonyIntegration.loaded(level,pos)||!level.getWorldBorder().isWithinBounds(pos))return false;
        var chunk=level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);
        var claim=IColonyManager.getInstance().getClaimData(level.dimension(),chunk.getPos());
        if(claim!=null&&claim.getOwningColony()!=0&&claim.getOwningColony()!=s.colonyId)return false;
        if(!VillageReplacement.structuresClear(level,chunk,s)||level.getBlockEntity(pos)!=null)return false;
        var actual=level.getBlockState(pos);
        return actual==desired||NaturalSettlementPlanner.natural(actual)||actual.is(Blocks.WATER);
    }
    static void edited(ServerLevel level,BlockPos pos) {
        var data=InfrastructureData.get(level);
        for(var task:data.tasks.values())if(!task.blocked) {
            var min=BlockPos.of(task.origin);
            if(pos.getX()<min.getX()||pos.getY()<min.getY()||pos.getZ()<min.getZ()||pos.getX()>=min.getX()+task.sizeX||pos.getY()>=min.getY()+task.sizeY||pos.getZ()>=min.getZ()+task.sizeZ)continue;
            task.blocked=true;data.setDirty();var colony=IColonyManager.getInstance().getColonyByWorld(task.colony,level);
            if(colony!=null&&colony.getPermissions().getOwner().equals(task.owner)&&task.order>0)colony.getWorkManager().removeWorkOrder(task.order);
        }
        blocked.remove(level);
    }
    static Set<Long> blockedColumns(ServerLevel level,int colony) {
        return blocked.computeIfAbsent(level,l->new HashMap<>()).computeIfAbsent(colony,id->{
            var columns=new HashSet<Long>();for(var task:InfrastructureData.get(level).tasks.values())if(task.colony==id&&task.blocked) {
                var min=BlockPos.of(task.origin);for(int x=0;x<task.sizeX;x++)for(int z=0;z<task.sizeZ;z++)columns.add(new BlockPos(min.getX()+x,0,min.getZ()+z).asLong());
            }return Set.copyOf(columns);
        });
    }
}
