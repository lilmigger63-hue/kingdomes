package com.kingdomsofminecraft.campaign;

/** Original culture identities; equipment and tactical definitions arrive with troop content. */
public enum Culture {
    WESTERN("Western feudal", "Human"), NORTHERN("Northern highland", "Dwarf"),
    IMPERIAL("Imperial", "Human"), STEPPE("Steppe", "Orc"),
    FOREST("Forest", "Elf"), DESERT("Desert", "Goblin");
    private final String tradition;
    private final String race;
    Culture(String tradition, String race) { this.tradition=tradition; this.race=race; }
    public String tradition() { return tradition; }
    public String race() { return race; }
}
