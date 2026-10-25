package com.kingdomsofminecraft.persistence;

import com.kingdomsofminecraft.campaign.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public final class KingdomFoundingTest {
    private static int checks;
    public static void main(String[] args) throws Exception {
        for (long seed = -100; seed < 100; seed++) {
            var state = new CampaignState(seed, 0);
            var kingdoms = state.kingdoms();
            check(kingdoms.size() >= 11 && kingdoms.size() <= 20, "initial population range");
            check(kingdoms.equals(new CampaignState(seed, 9).kingdoms()), "seed determinism independent of clock");
            check(new HashSet<>(kingdoms.stream().map(Kingdom::id).toList()).size() == kingdoms.size(), "unique kingdom IDs");
            check(new HashSet<>(kingdoms.stream().map(k -> k.founder().id()).toList()).size() == kingdoms.size(), "unique founding clans");
            check(new HashSet<>(kingdoms.stream().map(Kingdom::name).toList()).size() == kingdoms.size(), "unique kingdom names");
            check(new HashSet<>(kingdoms.stream().map(k -> k.founder().culture()).toList()).size() == 6, "all cultures represented");
            check(state.advance().kingdoms().equals(kingdoms), "clock advance preserves kingdoms");
        }
        check(!new CampaignState(1, 0).kingdoms().equals(new CampaignState(2, 0).kingdoms()), "world variation");
        var clan = new Clan(70, "Copperbranch", "Arel", Culture.FOREST);
        var empty = new CampaignState(5, 0, List.of());
        var founded = empty.foundKingdom(clan, "Copperwood", new Banner(0x123456, 0x654321, 2));
        check(empty.kingdoms().isEmpty() && founded.kingdoms().size() == 1, "immutable dynamic founding");
        rejects(() -> founded.foundKingdom(clan, "Another", new Banner(1, 2, 0)), "clan cannot found twice");
        rejects(() -> founded.foundKingdom(new Clan(71, "Other", "Arel", Culture.FOREST), "Copperwood", new Banner(1,2,0)), "duplicate names rejected");
        var source = new ArrayList<>(founded.kingdoms());
        var copied = new CampaignState(5, 0, source); source.clear();
        check(copied.kingdoms().size() == 1, "defensive list copy");
        rejects(() -> new Clan(-1, "C", "R", Culture.FOREST), "negative clan ID");
        rejects(() -> new Clan(1, " ", "R", Culture.FOREST), "blank clan");
        rejects(() -> new Banner(-1, 1, 0), "invalid banner color");
        rejects(() -> new CampaignState(5, 0, List.of(founded.kingdoms().getFirst(), founded.kingdoms().getFirst())), "duplicate kingdom records");
        var many = empty;
        for(int i=0;i<20;i++) many=many.foundKingdom(new Clan(i,"Clan "+i,"Ruler "+i,Culture.WESTERN),"Realm "+i,new Banner(1,2,0));
        var full=many;
        rejects(() -> full.foundKingdom(new Clan(21,"Extra","R",Culture.WESTERN),"Extra",new Banner(1,2,0)), "kingdom cap");
        var file = Files.createTempFile("kingdoms-founders", ".dat");
        try {
            var original = new CampaignState(5, 200, founded.kingdoms());
            var store = new CampaignStore(file); store.save(original);
            check(store.loadOrCreate(999).equals(original), "actual founded records survive storage without regeneration");
        } finally { Files.delete(file); }
        System.out.println("PASS: " + checks + " kingdom founding checks (200 seeds plus invariants)");
    }
    private static void check(boolean value, String label) { if(!value) throw new AssertionError(label); checks++; }
    private static void rejects(Runnable action, String label) {
        try { action.run(); } catch (IllegalArgumentException | UnsupportedOperationException expected) { checks++; return; }
        throw new AssertionError(label);
    }
}
