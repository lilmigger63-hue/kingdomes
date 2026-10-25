package com.kingdomsofminecraft.campaign;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

/** Immutable strategic data. No Minecraft objects or loaded chunks are retained. */
public record CampaignState(long worldSeed, long simulationStep, List<Kingdom> kingdoms,
                            List<ClanAllegiance> allegiances, List<Treaty> treaties) {
    public CampaignState(long worldSeed, long simulationStep) {
        this(worldSeed, simulationStep, KingdomFounding.generate(worldSeed));
    }
    public CampaignState(long worldSeed, long simulationStep, List<Kingdom> kingdoms) {
        this(worldSeed, simulationStep, kingdoms,
                kingdoms.stream().map(k -> new ClanAllegiance(k.founder(), k.id())).toList(), List.of());
    }
    public CampaignState {
        if (simulationStep < 0) throw new IllegalArgumentException("Negative campaign time");
        kingdoms = List.copyOf(kingdoms);
        allegiances = List.copyOf(allegiances);
        treaties = List.copyOf(treaties);
        if (allegiances.size() > 64 || treaties.size() > 190) throw new IllegalArgumentException("Political state limit exceeded");
        if (kingdoms.size() > 20) throw new IllegalArgumentException("At most 20 kingdoms");
        var ids=new HashSet<Integer>(); var clans=new HashSet<Integer>(); var names=new HashSet<String>();
        for(var kingdom:kingdoms) {
            if(!ids.add(kingdom.id()) || !clans.add(kingdom.founder().id()) || !names.add(kingdom.name().toLowerCase(Locale.ROOT)))
                throw new IllegalArgumentException("Duplicate kingdom or founding clan");
        }
        var memberships = new java.util.HashMap<Integer, ClanAllegiance>();
        for (var allegiance : allegiances) {
            if (!ids.contains(allegiance.kingdomId()) || memberships.putIfAbsent(allegiance.clan().id(), allegiance) != null)
                throw new IllegalArgumentException("Unknown kingdom or duplicate clan allegiance");
        }
        for (var kingdom : kingdoms) {
            var membership = memberships.get(kingdom.founder().id());
            if (membership == null || membership.kingdomId() != kingdom.id() || !membership.clan().equals(kingdom.founder()))
                throw new IllegalArgumentException("Ruling clan allegiance mismatch");
        }
        var pairs = new HashSet<String>();
        for (var treaty : treaties) {
            if (!ids.contains(treaty.first()) || !ids.contains(treaty.second()) || treaty.changedAt() > simulationStep
                    || !pairs.add(treaty.first() + ":" + treaty.second()))
                throw new IllegalArgumentException("Invalid or duplicate treaty");
        }
    }
    public CampaignState advance() {
        return new CampaignState(worldSeed, Math.incrementExact(simulationStep), kingdoms, allegiances, treaties);
    }
    public CampaignState foundKingdom(Clan founder, String name, Banner banner) {
        if (kingdoms.size() == 20) throw new IllegalArgumentException("At most 20 kingdoms");
        int id=kingdoms.stream().mapToInt(Kingdom::id).max().orElse(-1);
        if (id == Integer.MAX_VALUE) throw new IllegalArgumentException("Kingdom ID space exhausted");
        var updated = new ArrayList<>(kingdoms);
        updated.add(new Kingdom(id+1, name, founder, banner));
        var members = new ArrayList<>(allegiances);
        members.add(new ClanAllegiance(founder, id+1));
        return new CampaignState(worldSeed, simulationStep, updated, members, treaties);
    }

    public CampaignState joinClan(Clan clan, int kingdomId) {
        requireKingdom(kingdomId);
        var members = new ArrayList<>(allegiances);
        members.add(new ClanAllegiance(clan, kingdomId));
        return new CampaignState(worldSeed, simulationStep, kingdoms, members, treaties);
    }

    public CampaignState transferClan(int clanId, int kingdomId) {
        requireKingdom(kingdomId);
        var current = allegiances.stream().filter(a -> a.clan().id() == clanId).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown clan"));
        if (current.kingdomId() == kingdomId) return this;
        if (kingdoms.stream().anyMatch(k -> k.founder().id() == clanId))
            throw new IllegalArgumentException("Ruling clans cannot transfer allegiance");
        var members = new ArrayList<>(allegiances);
        members.set(members.indexOf(current), new ClanAllegiance(current.clan(), kingdomId));
        return new CampaignState(worldSeed, simulationStep, kingdoms, members, treaties);
    }

    public boolean atWar(int a, int b) {
        requirePair(a,b);
        int first=Math.min(a,b), second=Math.max(a,b);
        return treaties.stream().anyMatch(t -> t.first()==first && t.second()==second && t.war());
    }

    public CampaignState setWar(int a, int b, boolean war) {
        requirePair(a,b);
        if (atWar(a,b) == war) return this;
        int first=Math.min(a,b), second=Math.max(a,b);
        var updated=new ArrayList<>(treaties);
        updated.removeIf(t -> t.first()==first && t.second()==second);
        updated.add(new Treaty(first, second, war, simulationStep));
        return new CampaignState(worldSeed, simulationStep, kingdoms, allegiances, updated);
    }

    private void requireKingdom(int id) {
        if (kingdoms.stream().noneMatch(k -> k.id()==id)) throw new IllegalArgumentException("Unknown kingdom");
    }
    private void requirePair(int a,int b) {
        requireKingdom(a); requireKingdom(b);
        if (a==b) throw new IllegalArgumentException("A kingdom cannot negotiate with itself");
    }
}
