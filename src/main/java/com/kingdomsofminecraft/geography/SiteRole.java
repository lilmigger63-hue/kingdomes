package com.kingdomsofminecraft.geography;

import java.util.ArrayList;
import java.util.List;

/** Conservative advisory profiles. Blueprint fit, protection, resources and routes need separate checks. */
public enum SiteRole {
    VILLAGE(6,23,Integer.MIN_VALUE,0,Water.NONE),
    CASTLE(8,25,80,0,Water.NONE),
    TRADE(6,20,Integer.MIN_VALUE,0,Water.ACCESS),
    DWARVEN(10,25,120,0,Water.NONE),
    ELVEN(8,22,Integer.MIN_VALUE,12,Water.NONE),
    FORT(8,24,90,0,Water.NONE),
    COASTAL(5,18,Integer.MIN_VALUE,0,Water.COAST);
    private final int maxRelief,minDry,minElevation,minForest;
    private final Water water;
    private enum Water {NONE,ACCESS,COAST}
    SiteRole(int relief,int dry,int elevation,int forest,Water water) {
        this.maxRelief=relief;this.minDry=dry;this.minElevation=elevation;this.minForest=forest;this.water=water;
    }
    public record Assessment(boolean suitable,int score,List<String> reasons) {
        public Assessment {reasons=List.copyOf(reasons);}
    }
    public Assessment assess(TerrainCell cell) {
        var reasons=new ArrayList<String>();
        if(cell.relief()>maxRelief)reasons.add("steep or uneven dry terrain");
        if(cell.dry()<minDry)reasons.add("insufficient sampled dry space");
        if(cell.minY()<minElevation)reasons.add("elevation below profile preference");
        if(cell.forest()<minForest)reasons.add("insufficient forest biome coverage");
        if(water==Water.ACCESS && cell.water()==0 && cell.river()==0)reasons.add("no sampled water or river access");
        if(water==Water.COAST && (cell.ocean()==0 || cell.water()==0))reasons.add("no sampled coast");
        if(cell.water()>0 && (long)cell.minY()<=(long)cell.seaLevel()+1)reasons.add("low shore flooding risk");
        int score=(int)Math.max(0,100-Math.min(100,cell.relief()*6)-cell.water()*2);
        return new Assessment(reasons.isEmpty(),score,reasons);
    }
}
