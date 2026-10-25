package com.kingdomsofminecraft.persistence;

import com.kingdomsofminecraft.campaign.*;
import java.nio.file.*;

public final class ColonyMappingTest {
    private static int checks;
    public static void main(String[] args)throws Exception {
        var initial=new CampaignState(7,100);
        var key=new ColonyKey("minecraft:overworld",1);
        var bound=initial.bindColony(key,0);
        check(initial.colonies().isEmpty() && bound.colonies().getFirst().kingdomId()==0,"immutable political binding");
        check(bound.bindColony(key,0).equals(bound),"idempotent binding");
        reject(()->bound.bindColony(key,1),"cannot bypass conquest by rebinding");
        reject(()->initial.bindColony(key,999),"unknown kingdom");
        reject(()->new ColonyKey("minecraft:overworld",0),"invalid colony ID");
        var other=bound.bindColony(new ColonyKey("minecraft:the_nether",1),1);
        check(other.colonies().size()==2,"dimension qualified identity");
        check(other.advance().setWar(0,1,true).joinClan(new Clan(100,"Other","Ruler",Culture.FOREST),0).colonies().equals(other.colonies()),"transitions preserve bindings");
        check(other.foundKingdom(new Clan(101,"Next","Ruler",Culture.STEPPE),"Next",new Banner(1,2,0)).colonies().equals(other.colonies()),"founding preserves bindings");
        var file=Files.createTempFile("kingdoms-colony-mapping", ".dat");
        try {
            var store=new CampaignStore(file);store.save(other);check(store.loadOrCreate(999).equals(other),"binding persistence");
            byte[] legacy=Files.readAllBytes(Path.of("src/coreTest/resources/schema4-campaign.dat"));Files.write(file,legacy);
            var migrated=store.loadOrCreate(999);
            check(migrated.worldSeed()==7 && migrated.simulationStep()==100 && migrated.atWar(0,1),"schema4 preserves political clock/treaty");
            check(migrated.settlements().getFirst().name().equals("Retained Site") && migrated.settlements().getFirst().capital(),"schema4 preserves legacy sites without constructing colonies");
            check(migrated.colonies().isEmpty() && java.util.Arrays.equals(legacy,Files.readAllBytes(file)),"schema4 migration does not invent bindings or rewrite input");
            var many=initial;
            for(int i=1;i<=128;i++) many=many.bindColony(new ColonyKey("minecraft:overworld",i),0);
            var full=many;
            store.save(full);check(store.loadOrCreate(999).equals(full),"mapping capacity roundtrip");
            reject(()->full.bindColony(new ColonyKey("minecraft:overworld",129),0),"mapping cap");
        }
        finally {Files.delete(file);}
        System.out.println("PASS: "+checks+" colony mapping checks");
    }
    private static void check(boolean v,String label){if(!v)throw new AssertionError(label);checks++;}
    private static void reject(Runnable action,String label){try{action.run();}catch(IllegalArgumentException expected){checks++;return;}throw new AssertionError(label);}
}
