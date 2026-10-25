package com.kingdomsofminecraft.persistence;

import com.kingdomsofminecraft.geography.*;

public final class TerrainTest {
    private static int checks;
    private static TerrainCell cell(int low,int high,int water,int forest,int river,int ocean,long time) {
        return new TerrainCell(new TerrainKey("minecraft:overworld",0,0),low,high,water,forest,river,ocean,63,time);
    }
    public static void main(String[] args) {
        var valley=cell(70,73,1,3,5,0,10);
        check(SiteRole.VILLAGE.assess(valley).suitable(),"gentle valley settlement");
        check(!SiteRole.VILLAGE.assess(cell(70,95,0,0,0,0,10)).suitable(),"steep cliff rejected");
        check(SiteRole.TRADE.assess(valley).suitable(),"river access trade");
        check(!SiteRole.TRADE.assess(cell(70,73,0,0,0,0,10)).suitable(),"inland trade needs water evidence");
        check(SiteRole.DWARVEN.assess(cell(150,156,0,0,0,0,10)).suitable(),"mountain plateau");
        check(!SiteRole.DWARVEN.assess(valley).suitable(),"low valley not mountain role");
        check(SiteRole.CASTLE.assess(cell(90,95,0,0,0,0,10)).suitable(),"elevated ridge");
        check(SiteRole.FORT.assess(cell(110,114,0,0,0,0,10)).suitable(),"gentle elevated fort screen");
        check(SiteRole.ELVEN.assess(cell(75,78,0,20,0,0,10)).suitable(),"forest role");
        check(!SiteRole.ELVEN.assess(valley).suitable(),"sparse forest rejected");
        check(SiteRole.COASTAL.assess(cell(66,68,5,0,0,15,10)).suitable(),"coastal dry platform");
        check(!SiteRole.COASTAL.assess(cell(63,65,5,0,0,15,10)).suitable(),"low flooded shore rejected");
        check(!SiteRole.VILLAGE.assess(cell(63,63,25,0,0,25,10)).suitable(),"open water not buildable land");
        reject(()->cell(80,70,0,0,0,0,10),"inverted heights");
        reject(()->cell(70,70,26,0,0,0,10),"invalid sample counts");
        var cache=new TerrainCache(2,20);
        cache.put(valley);
        check(cache.get(valley.key(),29).isPresent(),"cached before expiry");
        check(cache.get(valley.key(),30).isEmpty(),"expiry boundary");
        cache.put(valley);check(cache.get(valley.key(),9).isEmpty(),"clock reset invalidates cache");
        var b=new TerrainCell(new TerrainKey("minecraft:overworld",1,0),70,70,0,0,0,0,63,10);
        var c=new TerrainCell(new TerrainKey("minecraft:the_nether",0,0),70,70,0,0,0,0,63,10);
        cache.put(valley);cache.put(b);cache.get(valley.key(),11);cache.put(c);
        check(cache.get(b.key(),11).isEmpty() && cache.get(valley.key(),11).isPresent() && cache.get(c.key(),11).isPresent(),"bounded LRU and dimension isolation");
        cache.clear();check(cache.get(c.key(),11).isEmpty(),"world lifecycle clear");
        System.out.println("PASS: "+checks+" terrain screening/cache checks");
    }
    private static void check(boolean v,String label){if(!v)throw new AssertionError(label);checks++;}
    private static void reject(Runnable action,String label){try{action.run();}catch(IllegalArgumentException expected){checks++;return;}throw new AssertionError(label);}
}
