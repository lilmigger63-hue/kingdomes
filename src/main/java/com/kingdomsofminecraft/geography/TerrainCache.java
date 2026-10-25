package com.kingdomsofminecraft.geography;

import java.util.LinkedHashMap;
import java.util.Optional;

/** Server-thread LRU with bounded size, expiry and reset protection. */
public final class TerrainCache {
    private final int capacity;
    private final long lifetime;
    private final LinkedHashMap<TerrainKey,TerrainCell> cells=new LinkedHashMap<>(16,0.75f,true);
    public TerrainCache(int capacity,long lifetime) {
        if(capacity<1 || lifetime<1)throw new IllegalArgumentException("Invalid cache bounds");
        this.capacity=capacity;this.lifetime=lifetime;
    }
    public void put(TerrainCell cell) {
        cells.put(cell.key(),cell);
        while(cells.size()>capacity)cells.remove(cells.keySet().iterator().next());
    }
    public Optional<TerrainCell> get(TerrainKey key,long now) {
        var cell=cells.get(key);
        if(cell==null)return Optional.empty();
        if(now<cell.sampledAt() || now-cell.sampledAt()>=lifetime){cells.remove(key);return Optional.empty();}
        return Optional.of(cell);
    }
    public void invalidate(TerrainKey key) {cells.remove(key);}
    public void clear() {cells.clear();}
}
