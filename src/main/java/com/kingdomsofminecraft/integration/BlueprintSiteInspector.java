package com.kingdomsofminecraft.integration;

import com.kingdomsofminecraft.geography.BlueprintFootprint;
import com.ldtteam.structurize.api.RotationMirror;
import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.minecolonies.api.blocks.AbstractBlockHut;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.permissions.Action;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;

/** Conservative expansion preflight, never construction approval. No cache, writes or chunk requests. */
public final class BlueprintSiteInspector {
    public record Result(boolean passed,String detail) {}
    private static Result reject(String reason){return new Result(false,reason);}

    public Result inspect(ServerLevel level,ServerPlayer player,BlockPos anchor,Blueprint blueprint) {
        if(!level.getServer().isSameThread())throw new IllegalStateException("Blueprint preflight must run on the server thread");
        if(player.level()!=level)return reject("Player changed dimension; inspect again");
        if(blueprint.getRotationMirror()!=RotationMirror.NONE)return reject("Only the original blueprint orientation is supported");
        var missing=blueprint.getMissingMods();
        if(missing!=null && missing.length>0)return reject("Blueprint has missing required mods");
        // Reject dimensions before asking upstream to enumerate the palette/primary anchor.
        try {BlueprintFootprint.at(blueprint.getSizeX(),blueprint.getSizeY(),blueprint.getSizeZ(),0,0,0,0,0,0);}
        catch(IllegalArgumentException error){return reject(error.getMessage());}
        var offset=blueprint.getPrimaryBlockOffset();
        BlueprintFootprint b;
        try {b=BlueprintFootprint.at(blueprint.getSizeX(),blueprint.getSizeY(),blueprint.getSizeZ(),offset.getX(),offset.getY(),offset.getZ(),anchor.getX(),anchor.getY(),anchor.getZ());}
        catch(IllegalArgumentException error){return reject(error.getMessage());}
        var low=new BlockPos(b.minX(),b.minY()-1,b.minZ());
        var high=new BlockPos(b.maxX(),b.maxY(),b.maxZ());
        if(level.isOutsideBuildHeight(low) || level.isOutsideBuildHeight(high))return reject("Blueprint and foundation must fit inside build height");
        if(!level.getWorldBorder().isWithinBounds(low) || !level.getWorldBorder().isWithinBounds(high))return reject("Blueprint crosses the world border");
        // Guard the ENTIRE footprint before any upstream API that might call getChunkAt.
        for(int x=b.minChunkX();x<=b.maxChunkX();x++)for(int z=b.minChunkZ();z<=b.maxChunkZ();z++)
            if(level.getChunkSource().getChunkNow(x,z)==null)return reject("Whole blueprint footprint must already be loaded");
        IColony colony=null;
        for(int x=b.minChunkX();x<=b.maxChunkX();x++)for(int z=b.minChunkZ();z<=b.maxChunkZ();z++) {
            var found=IColonyManager.getInstance().getColonyByPosFromWorld(level,new BlockPos(x*16,anchor.getY(),z*16));
            if(found==null)return reject("Blueprint expansion must stay inside an existing colony");
            if(colony!=null && colony.getID()!=found.getID())return reject("Blueprint crosses colony boundaries");
            colony=found;
            for(var action:new Action[]{Action.PLACE_HUTS,Action.PLACE_BLOCKS,Action.BREAK_BLOCKS})
                if(!colony.getPermissions().hasPermission(player,action))return reject("MineColonies denied "+action.name().toLowerCase(java.util.Locale.ROOT));
        }
        var hut=blueprint.getBlockState(offset).getBlock();
        if(!(hut instanceof AbstractBlockHut<?>))return reject("Blueprint primary anchor is not a MineColonies hut");
        if(!colony.getServerBuildingManager().canPlaceAt(hut,anchor,player))return reject("MineColonies building placement rules rejected this anchor");
        int maxCut=0,maxFill=0;
        var pos=new BlockPos.MutableBlockPos();
        for(int x=b.minX();x<=b.maxX();x++)for(int z=b.minZ();z<=b.maxZ();z++) {
            var chunk=level.getChunkSource().getChunkNow(Math.floorDiv(x,16),Math.floorDiv(z,16));
            int ground=chunk.getHeight(Heightmap.Types.OCEAN_FLOOR,x & 15,z & 15);
            if(ground<level.getMinY() || chunk.getBlockState(pos.set(x,ground,z)).isAir())return reject("Missing ground below blueprint");
            int delta=ground-(b.minY()-1);
            maxCut=Math.max(maxCut,delta);maxFill=Math.max(maxFill,-delta);
            if(maxCut>4 || maxFill>4)return reject("More than four blocks of terrain cut/fill required");
            for(int y=b.minY()-1;y<=b.maxY();y++) {
                var state=chunk.getBlockState(pos.set(x,y,z));
                if(!state.getFluidState().isEmpty())return reject("Fluid inside blueprint/foundation footprint");
                if(state.hasBlockEntity())return reject("Existing inventory or block entity inside blueprint/foundation footprint");
                if(state.getDestroySpeed(level,pos)<0)return reject("Unbreakable block inside blueprint/foundation footprint");
            }
        }
        return new Result(true,"Original orientation; "+b.volume()+" blueprint blocks checked; max cut "+maxCut+", fill "+maxFill
                +". Advisory only: resources, access, existing structure bounds and other mods' protection still need checking. Use MineColonies build tools for placement.");
    }
}
