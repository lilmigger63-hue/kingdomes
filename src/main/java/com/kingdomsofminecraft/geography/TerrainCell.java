package com.kingdomsofminecraft.geography;

import java.util.Objects;

/** Coarse observation of 25 surface columns, never a terrain generator.
 * Heights describe dry sample columns, or seabed fallback when no dry column exists.
 * Forest/river/ocean coverage uses observed biome tags, not inferred distant geography.
 */
public record TerrainCell(TerrainKey key, int minY, int maxY, int water, int forest, int river, int ocean,
                          int seaLevel, long sampledAt) {
    public static final int SAMPLES=25;
    public TerrainCell {
        Objects.requireNonNull(key);
        if (minY>maxY || sampledAt<0 || water<0 || water>SAMPLES || forest<0 || forest>SAMPLES
                || river<0 || river>SAMPLES || ocean<0 || ocean>SAMPLES) throw new IllegalArgumentException("Invalid terrain observations");
    }
    public long relief() { return (long)maxY-minY; }
    public int dry() { return SAMPLES-water; }
}
