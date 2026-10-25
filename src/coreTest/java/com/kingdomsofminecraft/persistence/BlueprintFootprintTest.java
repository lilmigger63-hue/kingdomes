package com.kingdomsofminecraft.persistence;

import com.kingdomsofminecraft.geography.BlueprintFootprint;

public final class BlueprintFootprintTest {
    private static int checks;
    public static void main(String[] args) {
        var b=BlueprintFootprint.at(17,9,17,8,2,8,0,70,0);
        check(b.minX()==-8 && b.maxX()==8 && b.minY()==68 && b.maxY()==76 && b.minZ()==-8 && b.maxZ()==8,"anchor offset and inclusive bounds");
        check(b.minChunkX()==-1 && b.maxChunkX()==0 && b.minChunkZ()==-1 && b.maxChunkZ()==0,"negative boundary chunk coverage");
        check(b.volume()==2601,"exact volume");
        check(BlueprintFootprint.at(64,16,64,0,0,0,0,0,0).volume()==65536,"budget boundary accepted");
        reject(()->BlueprintFootprint.at(64,17,64,0,0,0,0,0,0),"oversized volume");
        reject(()->BlueprintFootprint.at(65,1,1,0,0,0,0,0,0),"axis budget");
        reject(()->BlueprintFootprint.at(0,1,1,0,0,0,0,0,0),"empty footprint");
        reject(()->BlueprintFootprint.at(8,8,8,8,0,0,0,0,0),"offset outside dimensions");
        reject(()->BlueprintFootprint.at(8,8,8,-1,0,0,0,0,0),"negative offset");
        reject(()->BlueprintFootprint.at(8,8,8,0,0,0,Integer.MAX_VALUE,0,0),"coordinate overflow");
        reject(()->BlueprintFootprint.at(8,8,8,7,0,0,Integer.MIN_VALUE,0,0),"negative coordinate overflow");
        check(BlueprintFootprint.validReference("Minecolonies Original","wooden/worker/builder1.blueprint"),"known-style relative path");
        for(String path:new String[]{"../secret.blueprint","wooden/../secret.blueprint","/tmp/a.blueprint","a\\b.blueprint","C:a.blueprint","a//b.blueprint","a/./b.blueprint","a.json","a\n.blueprint"})
            check(!BlueprintFootprint.validReference("Original",path),"reject unsafe path: "+path);
        check(!BlueprintFootprint.validReference("x".repeat(129),"a.blueprint"),"bounded pack name");
        check(!BlueprintFootprint.validReference("Original","a".repeat(257)+".blueprint"),"bounded path");
        System.out.println("PASS: "+checks+" blueprint footprint/reference checks");
    }
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);checks++;}
    private static void reject(Runnable action,String label){try{action.run();}catch(IllegalArgumentException expected){checks++;return;}throw new AssertionError(label);}
}
