package com.kingdomsofminecraft.campaign;

import java.util.Objects;

/** Political allegiance only: MineColonies retains physical state and player permissions. */
public record ColonyBinding(ColonyKey colony, int kingdomId) {
    public ColonyBinding {
        Objects.requireNonNull(colony);
        if (kingdomId < 0) throw new IllegalArgumentException("Invalid kingdom ID");
    }
}
