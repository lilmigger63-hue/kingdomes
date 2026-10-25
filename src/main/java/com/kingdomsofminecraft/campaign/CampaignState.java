package com.kingdomsofminecraft.campaign;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

/** Immutable strategic data. No Minecraft objects or loaded chunks are retained. */
public record CampaignState(long worldSeed, long simulationStep, List<Kingdom> kingdoms) {
    public CampaignState(long worldSeed, long simulationStep) {
        this(worldSeed, simulationStep, KingdomFounding.generate(worldSeed));
    }
    public CampaignState {
        if (simulationStep < 0) throw new IllegalArgumentException("Negative campaign time");
        kingdoms = List.copyOf(kingdoms);
        if (kingdoms.size() > 20) throw new IllegalArgumentException("At most 20 kingdoms");
        var ids=new HashSet<Integer>(); var clans=new HashSet<Integer>(); var names=new HashSet<String>();
        for(var kingdom:kingdoms) {
            if(!ids.add(kingdom.id()) || !clans.add(kingdom.founder().id()) || !names.add(kingdom.name().toLowerCase(Locale.ROOT)))
                throw new IllegalArgumentException("Duplicate kingdom or founding clan");
        }
    }
    public CampaignState advance() {
        return new CampaignState(worldSeed, Math.incrementExact(simulationStep), kingdoms);
    }
    public CampaignState foundKingdom(Clan founder, String name, Banner banner) {
        if (kingdoms.size() == 20) throw new IllegalArgumentException("At most 20 kingdoms");
        int id=kingdoms.stream().mapToInt(Kingdom::id).max().orElse(-1);
        if (id == Integer.MAX_VALUE) throw new IllegalArgumentException("Kingdom ID space exhausted");
        var updated = new ArrayList<>(kingdoms);
        updated.add(new Kingdom(id+1, name, founder, banner));
        return new CampaignState(worldSeed, simulationStep, updated);
    }
}
