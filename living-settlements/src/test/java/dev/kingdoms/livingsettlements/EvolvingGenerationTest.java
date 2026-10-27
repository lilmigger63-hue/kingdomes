package dev.kingdoms.livingsettlements;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EvolvingGenerationTest {
    @Test void densityAddsOpportunitiesRatherThanOnlyChangingVillageCoin() {
        int baseline=0,additional=0;
        for(int x=-500;x<500;x++)for(int z=-500;z<500;z++) {
            if(NaturalGenerationRules.independentCandidate(42,x,z,1,false))baseline++;
            if(NaturalGenerationRules.independentCandidate(42,x,z,2.5,true))additional++;
        }
        assertTrue(baseline>400&&baseline<600);
        assertEquals(2.5,(baseline+additional)/(double)baseline,.2);
    }
    @Test void generationIsStableAndHasNoMinimumDistanceVeto() {
        int adjacent=0;
        for(int seed=0;seed<100000;seed++) {
            boolean a=NaturalGenerationRules.independentCandidate(seed,0,0,20,false);
            assertEquals(a,NaturalGenerationRules.independentCandidate(seed,0,0,20,false));
            if(a&&NaturalGenerationRules.independentCandidate(seed,1,0,20,false))adjacent++;
        }
        assertTrue(adjacent>0,"Adjacent opportunities are allowed; physical safety is a separate check");
        assertFalse(NaturalGenerationRules.independentCandidate(1,0,0,0,false));
        assertFalse(NaturalGenerationRules.independentCandidate(1,0,0,1,true));
    }
    @Test void invalidMultipliersAreRejected() {
        assertThrows(IllegalArgumentException.class,()->NaturalGenerationRules.independentCandidate(1,0,0,Double.NaN,false));
        assertThrows(IllegalArgumentException.class,()->NaturalGenerationRules.independentCandidate(1,0,0,-1,false));
    }
}
