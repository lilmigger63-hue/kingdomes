package dev.kingdoms.livingsettlements;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SiteAssessmentTest {
    private List<SiteAssessment.Surface> flat() {
        return new ArrayList<>(java.util.Collections.nCopies(81,
            new SiteAssessment.Surface(70, false, false, false)));
    }

    @Test void flatDryUnclaimedLandIsSuitable() {
        assertTrue(SiteAssessment.evaluate(flat(), 81).suitable());
    }
    @Test void missingSamplesCannotBeAccepted() {
        assertEquals(SiteAssessment.Reason.INCOMPLETE,
            SiteAssessment.evaluate(flat().subList(0, 80), 81).reason());
    }
    @Test void steepTerrainIsRejected() {
        var samples = flat();
        samples.set(0, new SiteAssessment.Surface(90, false, false, false));
        assertEquals(SiteAssessment.Reason.TOO_STEEP, SiteAssessment.evaluate(samples, 81).reason());
    }
    @Test void aCliffIsRejectedEvenWhenTotalReliefIsModerate() {
        var samples = flat();
        samples.set(40, new SiteAssessment.Surface(82, false, false, false));
        assertFalse(SiteAssessment.evaluate(samples, 81).suitable());
    }
    @Test void waterAndHazardsAreRejected() {
        var samples = flat();
        samples.set(0, new SiteAssessment.Surface(70, true, false, false));
        assertEquals(SiteAssessment.Reason.WET_OR_UNSAFE, SiteAssessment.evaluate(samples, 81).reason());
        samples.set(0, new SiteAssessment.Surface(70, false, true, false));
        assertFalse(SiteAssessment.evaluate(samples, 81).suitable());
    }
    @Test void playerClaimsAreRejected() {
        var samples = flat();
        samples.set(0, new SiteAssessment.Surface(70, false, false, true));
        assertEquals(SiteAssessment.Reason.CLAIMED, SiteAssessment.evaluate(samples, 81).reason());
    }
    @Test void anEmptySurveyIsNeverSuitable() {
        assertFalse(SiteAssessment.evaluate(List.of(), 0).suitable());
    }
}
