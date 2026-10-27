package dev.kingdoms.livingsettlements;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NaturalGenerationRulesTest {
    @Test void zeroFrequencyNeverSelectsARegion() {
        assertTrue(NaturalGenerationRules.candidate(42, 0, 0, 64, 0).isEmpty());
    }
    @Test void fullFrequencyIsDeterministicAndStaysInsideNegativeCells() {
        var a = NaturalGenerationRules.candidate(42, -2, -3, 64, 1).orElseThrow();
        assertEquals(a, NaturalGenerationRules.candidate(42, -2, -3, 64, 1).orElseThrow());
        assertTrue(a.chunkX() >= -128 && a.chunkX() < -64);
        assertTrue(a.chunkZ() >= -192 && a.chunkZ() < -128);
    }
    @Test void differentSeedsDoNotSelectTheSameWholeLandscape() {
        int differences = 0;
        for (int i = 0; i < 10; i++) {
            if (!NaturalGenerationRules.candidate(42, i, 0, 64, 1)
                    .equals(NaturalGenerationRules.candidate(99, i, 0, 64, 1))) { differences++; }
        }
        assertTrue(differences > 5);
    }
    @Test void invalidFrequencyAndGridSizesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> NaturalGenerationRules.candidate(1, 0, 0, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> NaturalGenerationRules.candidate(1, 0, 0, 64, Double.NaN));
    }
    @Test void spacingIncludesExactBoundaryWithoutIntegerOverflow() {
        assertFalse(NaturalGenerationRules.separated(0, 0, 599, 0, 600));
        assertTrue(NaturalGenerationRules.separated(0, 0, 600, 0, 600));
        assertTrue(NaturalGenerationRules.separated(-29_000_000, 0, 29_000_000, 0, 600));
    }
    @Test void inclusiveBuildingsRequireClearanceOnAllSides() {
        var a = new NaturalGenerationRules.Box(-16, -16, -1, -1);
        assertTrue(a.overlaps(new NaturalGenerationRules.Box(0, -10, 9, 9), 4));
        assertFalse(a.overlaps(new NaturalGenerationRules.Box(4, -10, 9, 9), 4));
        assertTrue(a.overlaps(new NaturalGenerationRules.Box(-10, -10, 0, 0), 0));
    }
    @Test void individuallyFlatPlotsCannotBeSeparatedByATallCliff() {
        assertTrue(NaturalGenerationRules.sameTerrainBand(80, 84));
        assertFalse(NaturalGenerationRules.sameTerrainBand(80, 100));
        assertFalse(NaturalGenerationRules.sameTerrainBand(80, 60));
        assertFalse(NaturalGenerationRules.sameTerrainBand(Integer.MIN_VALUE, Integer.MAX_VALUE));
    }
    @Test void neighboringCandidatesStayBetween1500And3000BlocksAcrossNegativeAndPositiveRegions() {
        for (long seed : new long[] {0, 42, -17, Long.MAX_VALUE}) {
            for (int cell = -20; cell <= 20; cell++) {
                var here = NaturalGenerationRules.candidateInRange(seed, cell, -cell, 1500, 3000, 1).orElseThrow();
                for (var next : java.util.List.of(
                        NaturalGenerationRules.candidateInRange(seed, cell + 1, -cell, 1500, 3000, 1).orElseThrow(),
                        NaturalGenerationRules.candidateInRange(seed, cell, -cell + 1, 1500, 3000, 1).orElseThrow())) {
                    double distance = Math.hypot((next.chunkX() - here.chunkX()) * 16.0, (next.chunkZ() - here.chunkZ()) * 16.0);
                    assertTrue(distance >= 1500 && distance <= 3000, "Neighbor distance: " + distance);
                }
            }
        }
    }
    @Test void boundedCandidatesRespectDisabledFrequencyAndRejectInvalidRange() {
        assertTrue(NaturalGenerationRules.candidateInRange(42, 0, 0, 1500, 3000, 0).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> NaturalGenerationRules.candidateInRange(42, 0, 0, 3000, 1500, 1));
    }
}
