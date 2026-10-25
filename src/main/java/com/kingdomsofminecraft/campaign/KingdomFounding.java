package com.kingdomsofminecraft.campaign;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Seed-stable founding; stores results so later name/content changes cannot rewrite a campaign. */
public final class KingdomFounding {
    private KingdomFounding() {}
    private static final List<String> ROOTS = List.of("Amber", "Birch", "Copper", "Dawn", "Ember", "Flint",
            "Glen", "Hazel", "Ivory", "Juniper", "Kestrel", "Larch", "Mist", "Night", "Oak", "Pine",
            "Quartz", "Reed", "Silver", "Thorn", "Umber", "Vale", "Willow", "Yarrow");
    public static List<Kingdom> generate(long seed) {
        Random random = new Random(seed ^ 0x4b494e47444f4d53L);
        int count = 11 + random.nextInt(10);
        var roots = new ArrayList<>(ROOTS);
        // Specify the shuffle instead of depending on collection implementation details.
        for (int i=roots.size()-1;i>0;i--) { int j=random.nextInt(i+1); String old=roots.get(i); roots.set(i,roots.get(j)); roots.set(j,old); }
        var state = new CampaignState(seed, 0, List.of());
        int cultureOffset=random.nextInt(Culture.values().length);
        for(int i=0;i<count;i++) {
            String root=roots.get(i);
            var clan=new Clan(i, "House " + root + "ward", root + "wyn", Culture.values()[(i+cultureOffset)%Culture.values().length]);
            state=state.foundKingdom(clan, root + "reach", new Banner(random.nextInt(0x1000000), random.nextInt(0x1000000), random.nextInt(8)));
        }
        return state.kingdoms();
    }
}
