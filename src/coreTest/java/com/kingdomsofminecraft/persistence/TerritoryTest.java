package com.kingdomsofminecraft.persistence;

import com.kingdomsofminecraft.campaign.*;
import java.nio.file.*;
import java.util.List;

public final class TerritoryTest {
    private static int checks;
    public static void main(String[] args)throws Exception {
        check(Region.atBlock("minecraft:overworld",-1,-512).equals(new Region("minecraft:overworld",-1,-1)),"negative floor division");
        check(Region.atBlock("minecraft:overworld",511,512).equals(new Region("minecraft:overworld",0,1)),"positive boundary");
        reject(()->new Region("bad dimension",0,0),"invalid dimension");
        var initial=new CampaignState(7,100);
        var region=new Region("minecraft:overworld",0,0);
        var founded=initial.registerSettlement("First",0,region);
        check(initial.settlements().isEmpty() && founded.ownerOf(region).orElseThrow()==0,"immutable ownership registration");
        check(founded.ownerOf(new Region("minecraft:the_nether",0,0)).isEmpty(),"dimension isolation");
        reject(()->founded.registerSettlement("Second",1,region),"one site per region");
        reject(()->founded.registerSettlement("first",1,new Region("minecraft:overworld",1,0)),"unique case-insensitive names");
        reject(()->initial.registerSettlement("Unknown",999,region),"unknown owner");
        var second=founded.registerSettlement("Second",0,new Region("minecraft:overworld",1,0));
        var capital=second.designateCapital(0).designateCapital(1);
        check(!capital.settlements().get(0).capital() && capital.settlements().get(1).capital(),"atomic capital replacement");
        reject(()->new CampaignState(7,100,initial.kingdoms(),initial.allegiances(),List.of(),List.of(
                new Settlement(0,"A",0,region,true),new Settlement(1,"B",0,new Region("minecraft:overworld",2,0),true))),"one capital per kingdom");
        reject(()->new CampaignState(7,100,initial.kingdoms(),initial.allegiances(),List.of(),List.of(
                new Settlement(0,"A",999,region,false))),"unknown saved owner");
        reject(()->founded.designateCapital(999),"unknown capital");
        reject(()->capital.conquerSettlement(1,1),"peaceful seizure forbidden");
        var conquered=capital.setWar(0,1,true).conquerSettlement(1,1);
        check(conquered.ownerOf(new Region("minecraft:overworld",1,0)).orElseThrow()==1 && !conquered.settlements().get(1).capital(),"conquest transfers region and clears capital");
        check(conquered.advance().settlements().equals(conquered.settlements()),"ticks preserve ownership");
        check(conquered.joinClan(new Clan(100,"New","Ruler",Culture.FOREST),0).setWar(0,1,false).settlements().equals(conquered.settlements()),"political transitions preserve settlements");
        check(conquered.foundKingdom(new Clan(200,"New realm","Ruler",Culture.FOREST),"New realm",new Banner(1,2,0)).settlements().equals(conquered.settlements()),"founding preserves settlements");
        var file=Files.createTempFile("kingdoms-territory", ".dat");
        try {
            var store=new CampaignStore(file); store.save(conquered);
            check(store.loadOrCreate(999).equals(conquered),"ownership roundtrip");
            byte[] legacy=Files.readAllBytes(Path.of("src/coreTest/resources/schema3-campaign.dat"));Files.write(file,legacy);
            var migrated=store.loadOrCreate(999);
            check(migrated.worldSeed()==7 && migrated.simulationStep()==100 && migrated.kingdoms().size()==13,"schema3 migration seed/time/kingdoms");
            check(migrated.atWar(0,1) && migrated.allegiances().stream().anyMatch(a->a.clan().id()==100 && a.clan().name().equals("Legacy Vassals")),"schema3 political data preserved");
            check(migrated.settlements().isEmpty() && java.util.Arrays.equals(legacy,Files.readAllBytes(file)),"migration creates no claims and does not rewrite input");
        } finally {Files.delete(file);}
        System.out.println("PASS: "+checks+" territory checks");
    }
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);checks++;}
    private static void reject(Runnable action,String label){try{action.run();}catch(IllegalArgumentException expected){checks++;return;}throw new AssertionError(label);}
}
