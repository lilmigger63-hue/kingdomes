package dev.kingdoms.livingsettlements;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.levelgen.Heightmap;

/** Loaded-only ground access. Tree trunks/canopies are not terrain elevation. */
final class AdaptiveTerrain {
    static BlockPos ground(ServerLevel level,int x,int z) {
        if(!ColonyIntegration.loaded(level,new BlockPos(x,0,z)))return null;
        int top=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z)-1;
        for(int y=top;y>level.getMinY();y--) {
            var pos=new BlockPos(x,y,z);var state=level.getBlockState(pos);
            if(state.is(BlockTags.LOGS)||state.is(BlockTags.LEAVES)||state.canBeReplaced())continue;
            if(!state.getFluidState().isEmpty())return pos;
            if(state.isFaceSturdy(level,pos,Direction.UP))return pos;
        }
        return null;
    }
    static boolean dry(ServerLevel level,BlockPos pos) {
        return pos!=null&&level.getBlockState(pos).getFluidState().isEmpty()
            &&level.getBlockState(pos).isFaceSturdy(level,pos,Direction.UP)
            &&!level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK);
    }
}
