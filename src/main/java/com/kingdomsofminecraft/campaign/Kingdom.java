package com.kingdomsofminecraft.campaign;

import java.util.Objects;

/** A political foundation, not a loaded NPC or a claim on Minecraft blocks. */
public record Kingdom(int id, String name, Clan founder, Banner banner) {
    public Kingdom {
        if (id < 0) throw new IllegalArgumentException("Negative kingdom ID");
        Clan.validateName(name); Objects.requireNonNull(founder); Objects.requireNonNull(banner);
    }
}
