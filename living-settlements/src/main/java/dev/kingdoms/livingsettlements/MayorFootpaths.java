package dev.kingdoms.livingsettlements;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import java.util.*;

/** Bounded surface route survey around reserved buildings. Records road proposals; never places road blocks. */
final class MayorFootpaths {
    private final NaturalSettlementsData.Settlement settlement;
    private final NaturalSettlementsData.BuildingPlan target;
    private final int referenceGround;
    private final Set<Long> blocked;
    private final boolean canPrepare;
    private final Set<Long> goals=new HashSet<>(),seen=new HashSet<>();
    private final Map<Long,Long> previous=new HashMap<>();
    private final Map<Long,Integer> ground=new HashMap<>();
    private final ArrayDeque<Long> pending=new ArrayDeque<>();
    private final List<Long> route=new ArrayList<>();
    String rejection;
    private boolean complete;
    MayorFootpaths(ServerLevel level,NaturalSettlementsData.Settlement s,NaturalSettlementsData.BuildingPlan target){
        settlement=s;
        this.target=target;
        blocked=InfrastructureService.blockedColumns(level,s.colonyId);
        var colony=com.minecolonies.api.colony.IColonyManager.getInstance().getColonyByWorld(s.colonyId,level);
        canPrepare=s.adaptive&&InfrastructureService.enabled()&&colony!=null&&colony.getServerBuildingManager().getBuildings().values().stream().anyMatch(b->b instanceof com.minecolonies.core.colony.buildings.workerbuildings.BuildingBuilder&&b.getBuildingLevel()>0);
        var hall=s.plans.stream().filter(p->p.role().equals("townhall")).findFirst().orElseThrow();
        referenceGround=s.groundY!=0?s.groundY:BlockPos.of(hall.anchor()).getY()-1;
        for(var pos:accesses(target))if(height(level,pos)!=null)goals.add(pos.asLong());
        for(var pos:accesses(hall))if(height(level,pos)!=null&&seen.add(pos.asLong()))pending.add(pos.asLong());
        if(goals.isEmpty()||pending.isEmpty())rejection="No safe walking access at town hall or proposed building";
    }
    private static List<BlockPos> exits(NaturalSettlementsData.BuildingPlan p){
        int x=p.minX()+p.sizeX()/2,z=p.minZ()+p.sizeZ()/2;
        return List.of(new BlockPos(p.minX()-1,0,z),new BlockPos(p.minX()+p.sizeX(),0,z),new BlockPos(x,0,p.minZ()-1),new BlockPos(x,0,p.minZ()+p.sizeZ()));
    }
    private List<BlockPos> accesses(NaturalSettlementsData.BuildingPlan p) {
        var saved=settlement.accesses.get(p.anchor());
        return saved==null||saved.isEmpty()?exits(p):saved.stream().map(BlockPos::of).map(pos->new BlockPos(pos.getX(),0,pos.getZ())).toList();
    }
    boolean complete(){return complete;}
    List<Long> roadPoints(){return List.copyOf(route);}
    void tick(ServerLevel level){
        long deadline=System.nanoTime()+3_000_000;
        for(int budget=NaturalSettlementConfig.SCAN_BUDGET.get();budget>0&&!complete&&rejection==null&&System.nanoTime()<deadline;budget--){
            var key=pending.poll();if(key==null||seen.size()>60000){rejection="No connected walkable route within reserved land";return;}
            if(goals.contains(key)){
                long current=key;int count=0;
                while(true){if(count++%(settlement.adaptive?1:8)==0){var pos=BlockPos.of(current);route.add(new BlockPos(pos.getX(),ground.get(current)+1,pos.getZ()).asLong());}var parent=previous.get(current);if(parent==null)break;current=parent;}
                complete=true;return;
            }
            var pos=BlockPos.of(key);int y=ground.get(key);
            for(var direction:Direction.Plane.HORIZONTAL){var next=pos.relative(direction);long id=next.asLong();if(seen.contains(id))continue;var nextY=height(level,next);if(nextY==null||Math.abs(nextY-y)>1)continue;seen.add(id);previous.put(id,key);pending.add(id);}
        }
    }
    private Integer height(ServerLevel level,BlockPos column){
        long key=column.asLong();if(ground.containsKey(key))return ground.get(key);
        if(blocked.contains(key))return null;
        var center=BlockPos.of(settlement.center);
        if(Math.abs(column.getX()-center.getX())>settlement.radius||Math.abs(column.getZ()-center.getZ())>settlement.radius||!ColonyIntegration.loaded(level,column))return null;
        if(settlement.plans.stream().anyMatch(p->column.getX()>=p.minX()&&column.getX()<p.minX()+p.sizeX()&&column.getZ()>=p.minZ()&&column.getZ()<p.minZ()+p.sizeZ()))return null;
        if(column.getX()>=target.minX()&&column.getX()<target.minX()+target.sizeX()&&column.getZ()>=target.minZ()&&column.getZ()<target.minZ()+target.sizeZ())return null;
        var chunk=level.getChunkSource().getChunkNow(column.getX()>>4,column.getZ()>>4);
        if(chunk==null||!VillageReplacement.structuresClear(level,chunk,settlement))return null;
        var claim=com.minecolonies.api.colony.IColonyManager.getInstance().getClaimData(level.dimension(),chunk.getPos());
        if(claim!=null&&claim.getOwningColony()!=0&&claim.getOwningColony()!=settlement.colonyId)return null;
        var actual=settlement.adaptive?AdaptiveTerrain.ground(level,column.getX(),column.getZ()):null;
        if(settlement.adaptive&&actual==null)return null;
        int y=settlement.adaptive?actual.getY():level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,column.getX(),column.getZ())-1;
        var pos=new BlockPos(column.getX(),y,column.getZ());var state=level.getBlockState(pos);
        boolean bridge=canPrepare&&NaturalSettlementConfig.BRIDGES.get()&&state.is(Blocks.WATER);
        if(!level.getWorldBorder().isWithinBounds(pos)||(!settlement.adaptive&&Math.abs(y-referenceGround)>4)||(!bridge&&(!state.getFluidState().isEmpty()||!state.isFaceSturdy(level,pos,Direction.UP)))||state.is(Blocks.MAGMA_BLOCK)||state.is(Blocks.CACTUS)
            ||!canClear(level,pos.above())||!canClear(level,pos.above(2)))return null;
        ground.put(key,y);return y;
    }
    private boolean canClear(ServerLevel level,BlockPos pos) {
        return level.getBlockState(pos).getCollisionShape(level,pos).isEmpty()
            ||(canPrepare&&NaturalSettlementConfig.EXCAVATE.get()&&level.getBlockEntity(pos)==null&&NaturalSettlementPlanner.natural(level.getBlockState(pos)));
    }
}
