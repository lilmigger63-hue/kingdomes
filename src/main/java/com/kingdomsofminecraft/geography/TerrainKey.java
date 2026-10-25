package com.kingdomsofminecraft.geography;

import com.kingdomsofminecraft.campaign.Region;

public record TerrainKey(String dimension, int chunkX, int chunkZ) {
    public TerrainKey { new Region(dimension,0,0); }
}
