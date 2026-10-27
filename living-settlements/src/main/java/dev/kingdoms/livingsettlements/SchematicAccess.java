package dev.kingdoms.livingsettlements;

import com.ldtteam.structurize.blueprints.v1.Blueprint;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import java.util.*;

/** Verifies walkable schematic exits against real outside ground, not plot midpoints. */
final class SchematicAccess {
    static List<Long> exits(ServerLevel level,NaturalSettlementsData.BuildingPlan plan,Blueprint bp,boolean founding) {
        var sources=new ArrayList<BlockPos>();var result=new LinkedHashSet<Long>();
        for(int x=0;x<bp.getSizeX();x++)for(int y=1;y<bp.getSizeY()-1;y++)for(int z=0;z<bp.getSizeZ();z++) {
            var pos=new BlockPos(x,y,z);var state=bp.getBlockState(pos);
            if(state.getBlock() instanceof DoorBlock&&state.getValue(DoorBlock.HALF)==DoubleBlockHalf.LOWER)sources.add(pos);
        }
        if(sources.isEmpty()) {
            var anchor=bp.getPrimaryBlockOffset();
            for(var direction:Direction.Plane.HORIZONTAL)for(int r=1;r<=4;r++)for(int dy=-1;dy<=2;dy++) {
                var pos=anchor.relative(direction,r).above(dy);if(walkable(bp,pos))sources.add(pos);
            }
        }
        for(var source:sources)for(var direction:Direction.Plane.HORIZONTAL) {
            var current=source;
            for(int distance=0;distance<bp.getSizeX()+bp.getSizeZ();distance++) {
                var next=current.relative(direction);
                if(next.getX()<0||next.getZ()<0||next.getX()>=bp.getSizeX()||next.getZ()>=bp.getSizeZ()) {
                    int x=plan.minX()+next.getX(),z=plan.minZ()+next.getZ();var ground=AdaptiveTerrain.ground(level,x,z);
                    if(AdaptiveTerrain.dry(level,ground)&&Math.abs(ground.getY()+1-(plan.minY()+current.getY()))<=1&&headroom(level,ground.above(),founding))result.add(ground.above().asLong());
                    break;
                }
                BlockPos step=null;
                for(int dy:new int[]{0,1,-1})if(walkable(bp,next.above(dy))){step=next.above(dy);break;}
                if(step==null)break;current=step;
            }
            if(result.size()>=32)return List.copyOf(result);
        }
        return List.copyOf(result);
    }
    private static boolean walkable(Blueprint bp,BlockPos p) {
        if(p.getX()<0||p.getZ()<0||p.getX()>=bp.getSizeX()||p.getZ()>=bp.getSizeZ()||p.getY()<1||p.getY()+1>=bp.getSizeY())return false;
        var floor=bp.getBlockState(p.below());
        return !floor.getCollisionShape(EmptyBlockGetter.INSTANCE,p.below()).isEmpty()&&floor.getFluidState().isEmpty()&&passable(bp.getBlockState(p),p)&&passable(bp.getBlockState(p.above()),p.above());
    }
    private static boolean passable(BlockState state,BlockPos pos) {
        return state.getFluidState().isEmpty()&&(state.getBlock() instanceof DoorBlock||state.getCollisionShape(EmptyBlockGetter.INSTANCE,pos).isEmpty());
    }
    private static boolean headroom(ServerLevel level,BlockPos pos,boolean founding) {
        for(int dy=0;dy<2;dy++) {
            var at=pos.above(dy);if(!ColonyIntegration.loaded(level,at)||!level.getWorldBorder().isWithinBounds(at)||level.getBlockEntity(at)!=null)return false;
            var state=level.getBlockState(at);if(!state.getCollisionShape(level,at).isEmpty()
                &&(founding||!NaturalSettlementConfig.TERRAFORM.get()||!NaturalSettlementConfig.EXCAVATE.get()||!NaturalSettlementPlanner.natural(state)))return false;
        }
        return true;
    }
}
