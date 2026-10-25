package com.kingdomsofminecraft.integration;

import com.kingdomsofminecraft.geography.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.levelgen.Heightmap;
import java.util.Optional;

/** Bounded surface observations on the server thread. No generation, tickets or block writes. */
public final class LoadedTerrainSampler {
    private final TerrainCache cache=new TerrainCache(256,1200);
    private static final int[] OFFSETS={0,4,8,12,15};
    public Optional<TerrainCell> sample(ServerLevel level,int chunkX,int chunkZ) {
        if(!level.getServer().isSameThread())throw new IllegalStateException("Terrain sampling must run on the server thread");
        var key=new TerrainKey(level.dimension().identifier().toString(),chunkX,chunkZ);
        var chunk=level.getChunkSource().getChunkNow(chunkX,chunkZ);
        if(chunk==null){cache.invalidate(key);return Optional.empty();}
        var cached=cache.get(key,level.getGameTime());
        if(cached.isPresent())return cached;
        int low=Integer.MAX_VALUE,high=Integer.MIN_VALUE,water=0,forest=0,river=0,ocean=0;
        int fallbackLow=Integer.MAX_VALUE,fallbackHigh=Integer.MIN_VALUE;
        var position=new BlockPos.MutableBlockPos();
        for(int x:OFFSETS)for(int z:OFFSETS) {
            int surface=chunk.getHeight(Heightmap.Types.WORLD_SURFACE,x,z);
            int ground=chunk.getHeight(Heightmap.Types.OCEAN_FLOOR,x,z);
            int worldX=chunk.getPos().getMinBlockX()+x,worldZ=chunk.getPos().getMinBlockZ()+z;
            if(ground<level.getMinY() || chunk.getBlockState(position.set(worldX,ground,worldZ)).isAir())return Optional.empty();
            fallbackLow=Math.min(fallbackLow,ground);fallbackHigh=Math.max(fallbackHigh,ground);
            var fluid=chunk.getFluidState(position.set(worldX,surface,worldZ));
            if(!fluid.isEmpty() && !fluid.is(FluidTags.WATER))return Optional.empty();
            boolean wet=fluid.is(FluidTags.WATER);
            if(wet)water++;else {low=Math.min(low,ground);high=Math.max(high,ground);}
            var biome=chunk.getNoiseBiome(QuartPos.fromBlock(worldX),QuartPos.fromBlock(surface),QuartPos.fromBlock(worldZ));
            if(biome.is(BiomeTags.IS_FOREST))forest++;
            if(biome.is(BiomeTags.IS_RIVER))river++;
            if(biome.is(BiomeTags.IS_OCEAN))ocean++;
        }
        if(water==TerrainCell.SAMPLES){low=fallbackLow;high=fallbackHigh;}
        var cell=new TerrainCell(key,low,high,water,forest,river,ocean,level.getSeaLevel(),level.getGameTime());
        cache.put(cell);return Optional.of(cell);
    }
    public void clear(){cache.clear();}
}
