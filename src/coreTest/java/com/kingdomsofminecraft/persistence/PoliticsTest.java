package com.kingdomsofminecraft.persistence;

import com.kingdomsofminecraft.campaign.*;
import java.nio.file.Files;
import java.util.List;

public final class PoliticsTest {
    private static int checks;
    public static void main(String[] args) throws Exception {
        var initial = new CampaignState(7, 100);
        int a=initial.kingdoms().get(0).id(), b=initial.kingdoms().get(1).id();
        check(initial.allegiances().size()==initial.kingdoms().size(), "founders own allegiance records");
        var clan = new Clan(100,"House Meadow","Lira",Culture.FOREST);
        var recruited=initial.joinClan(clan,a);
        check(initial.allegiances().size()+1==recruited.allegiances().size(), "joining is immutable");
        var transferred=recruited.transferClan(100,b);
        check(transferred.allegiances().stream().filter(x->x.clan().id()==100).findFirst().orElseThrow().kingdomId()==b,"transfer changes owner");
        reject(()->recruited.joinClan(clan,b),"one kingdom per clan");
        reject(()->initial.transferClan(initial.kingdoms().getFirst().founder().id(),b),"rulers cannot defect");
        reject(()->initial.joinClan(clan,999),"unknown kingdom");
        reject(()->initial.transferClan(999,b),"unknown clan");
        check(!initial.atWar(a,b),"initial peace");
        var war=transferred.setWar(a,b,true);
        check(war.atWar(b,a) && !transferred.atWar(a,b),"symmetric immutable war");
        check(war.advance().setWar(b,a,true).treaties().getFirst().changedAt()==100,"duplicate declaration preserves transition time");
        var peace=war.advance().setWar(b,a,false);
        check(!peace.atWar(a,b) && peace.treaties().size()==1 && peace.treaties().getFirst().changedAt()==101,"peace replaces war");
        reject(()->initial.setWar(a,a,true),"self war");
        reject(()->initial.setWar(a,999,true),"unknown enemy");
        reject(()->new CampaignState(7,100,initial.kingdoms(),initial.allegiances(),List.of(new Treaty(a,b,true,101))),"future treaty");
        reject(()->new CampaignState(7,100,initial.kingdoms(),initial.allegiances(),List.of(new Treaty(a,b,true,100),new Treaty(a,b,false,100))),"duplicate treaty");
        reject(()->new CampaignState(7,100,initial.kingdoms(),List.of(),List.of()),"missing ruling clans");
        check(war.advance().allegiances().equals(war.allegiances()) && war.advance().treaties().equals(war.treaties()),"ticks preserve politics");
        var file=Files.createTempFile("kingdoms-politics", ".dat");
        try {
            var store=new CampaignStore(file);store.save(war);
            check(store.loadOrCreate(999).equals(war),"custom vassal and war survive save");
            var realms=new java.util.ArrayList<Kingdom>();
            var members=new java.util.ArrayList<ClanAllegiance>();
            String longName="\u0800".repeat(90);
            for(int i=0;i<64;i++) {
                var noble=new Clan(i,longName+i,longName+i,Culture.NORTHERN);
                members.add(new ClanAllegiance(noble,i%20));
                if(i<20) realms.add(new Kingdom(i,longName+i,noble,new Banner(1,2,0)));
            }
            var treaties=new java.util.ArrayList<Treaty>();
            for(int i=0;i<20;i++)for(int j=i+1;j<20;j++)treaties.add(new Treaty(i,j,true,100));
            var capacity=new CampaignState(9,100,realms,members,treaties);
            store.save(capacity);
            check(store.loadOrCreate(999).equals(capacity) && Files.size(file)<=65536,"full political capacity with multibyte names fits and roundtrips");
            members.add(new ClanAllegiance(new Clan(100,"Too many","Ruler",Culture.WESTERN),0));
            reject(()->new CampaignState(9,100,realms,members,treaties),"clan cap enforced");
        } finally {Files.delete(file);}
        var small=new CampaignState(1,100,List.of(initial.kingdoms().getFirst()));
        var withWar=small.foundKingdom(new Clan(110,"Other","Other ruler",Culture.STEPPE),"Other realm",new Banner(1,2,0)).setWar(a,1,true);
        var extended=withWar.joinClan(clan,a).foundKingdom(new Clan(111,"Third","Third ruler",Culture.FOREST),"Third realm",new Banner(1,2,0));
        check(extended.atWar(a,1) && extended.allegiances().size()==4,"founding preserves vassals and treaties");
        System.out.println("PASS: "+checks+" political state checks");
    }
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);checks++;}
    private static void reject(Runnable action,String label){try{action.run();}catch(IllegalArgumentException expected){checks++;return;}throw new AssertionError(label);}
}
