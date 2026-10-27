package dev.kingdoms.livingsettlements;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConstructionFootprintTest {
    @Test void constructionTapeBorderMustBeLoadedAtPositiveChunkBoundaries() {
        assertFalse(ColonyIntegration.footprintLoaded(16, 31, 16, 31, (x, z) -> x == 1 && z == 1));
    }
    @Test void constructionTapeBorderMustBeLoadedAtNegativeChunkBoundaries() {
        assertFalse(ColonyIntegration.footprintLoaded(-16, -1, -16, -1, (x, z) -> x == -1 && z == -1));
    }
    @Test void completeFootprintIncludingTapeCanQueueConstruction() {
        assertTrue(ColonyIntegration.footprintLoaded(31, 16, 31, 16,
            (x, z) -> x >= 0 && x <= 2 && z >= 0 && z <= 2));
    }
    @Test void hugeFootprintsAreRejectedWithoutChunkLookups() {
        assertFalse(ColonyIntegration.footprintLoaded(-10000, 10000, -10000, 10000,
            (x, z) -> { fail("Huge footprint must be rejected before any chunk lookup"); return true; }));
    }
}
