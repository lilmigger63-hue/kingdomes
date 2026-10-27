package dev.kingdoms.livingsettlements;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VillageSelectionTest {
    @Test void twoThousandCandidatesAcrossSeedsApproachHalfAndRepeatInReverseOrder() {
        int selected = 0;
        for (long seed : new long[] {0, 1, -42, Long.MAX_VALUE}) {
            boolean[] decisions = new boolean[500];
            for (int i = 0; i < decisions.length; i++) {
                decisions[i] = VillageSelection.selected(seed, "minecraft:overworld", i * 34 - 8500, i * -17, .5);
                if (decisions[i]) selected++;
            }
            for (int i = decisions.length - 1; i >= 0; i--)
                assertEquals(decisions[i], VillageSelection.selected(seed, "minecraft:overworld", i * 34 - 8500, i * -17, .5));
        }
        assertTrue(selected > 900 && selected < 1100, "Actual selected count: " + selected);
    }
    @Test void boundariesAndInvalidChance() {
        assertFalse(VillageSelection.selected(1, "minecraft:overworld", -1, -2, 0));
        assertTrue(VillageSelection.selected(1, "minecraft:overworld", -1, -2, 1));
        assertThrows(IllegalArgumentException.class, () -> VillageSelection.selected(1, "x", 0, 0, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> VillageSelection.selected(1, "x", 0, 0, 1.1));
    }
}
