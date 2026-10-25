package com.kingdomsofminecraft.campaign;

import java.util.Objects;

/** A registered strategic site. This does not construct buildings or protect Minecraft blocks. */
public record Settlement(int id, String name, int kingdomId, Region region, boolean capital) {
    public Settlement {
        if (id < 0 || kingdomId < 0) throw new IllegalArgumentException("Negative settlement or kingdom ID");
        Clan.validateName(name); Objects.requireNonNull(region);
    }
}
