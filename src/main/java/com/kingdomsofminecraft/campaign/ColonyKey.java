package com.kingdomsofminecraft.campaign;

/** Stable MineColonies identity, independent of mutable colony name/center. */
public record ColonyKey(String dimension, int colonyId) {
    public ColonyKey {
        new Region(dimension,0,0); // shared identifier validation, no world access
        if (colonyId <= 0) throw new IllegalArgumentException("Invalid colony ID");
    }
}
