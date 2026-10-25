package com.kingdomsofminecraft.campaign;

import java.util.Objects;

/** A clan belongs to exactly one kingdom; the campaign validates the reference. */
public record ClanAllegiance(Clan clan, int kingdomId) {
    public ClanAllegiance {
        Objects.requireNonNull(clan);
        if (kingdomId < 0) throw new IllegalArgumentException("Negative kingdom ID");
    }
}
