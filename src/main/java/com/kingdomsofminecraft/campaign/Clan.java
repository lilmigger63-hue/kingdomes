package com.kingdomsofminecraft.campaign;

import java.util.Objects;

public record Clan(int id, String name, String ruler, Culture culture) {
    public Clan {
        if (id < 0) throw new IllegalArgumentException("Negative clan ID");
        validateName(name); validateName(ruler); Objects.requireNonNull(culture);
    }
    static void validateName(String name) {
        if (name == null || name.isBlank() || name.length() > 96 || name.codePoints().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("Names must contain 1–96 characters without control characters");
    }
}
