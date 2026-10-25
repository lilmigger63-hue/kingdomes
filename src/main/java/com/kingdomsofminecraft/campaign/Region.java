package com.kingdomsofminecraft.campaign;

/** A dimension-qualified 512 x 512 block strategic cell, independent of chunk loading. */
public record Region(String dimension, int x, int z) {
    public Region {
        if (dimension == null || dimension.length() > 128 || !dimension.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))
            throw new IllegalArgumentException("Invalid dimension identifier");
    }
    public static Region atBlock(String dimension, int blockX, int blockZ) {
        return new Region(dimension, Math.floorDiv(blockX,512), Math.floorDiv(blockZ,512));
    }
}
