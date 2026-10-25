package com.kingdomsofminecraft.campaign;

/** Persisted original heraldry definition: RGB colors and one of eight geometric sigils. */
public record Banner(int primaryColor, int secondaryColor, int sigil) {
    public Banner {
        if (primaryColor < 0 || primaryColor > 0xffffff || secondaryColor < 0 || secondaryColor > 0xffffff || sigil < 0 || sigil > 7)
            throw new IllegalArgumentException("Invalid heraldry");
    }
}
