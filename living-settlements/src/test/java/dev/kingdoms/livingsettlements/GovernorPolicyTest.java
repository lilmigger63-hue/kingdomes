package dev.kingdoms.livingsettlements;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class GovernorPolicyTest {
    private final GovernorPolicy.Gate ready = new GovernorPolicy.Gate(true, true, false, false);
    private final Map<String, Integer> priorities = Map.of("builder", 0, "townhall", 1, "home", 2);
    private GovernorPolicy.Candidate hut(long id, String type, int level, int max, boolean loaded) {
        return new GovernorPolicy.Candidate(id, type, level, max, loaded);
    }
    @Test void unfinishedHutsComeBeforeUpgradesAndBuilderWinsTies() {
        var huts = List.of(hut(1, "townhall", 1, 5, true), hut(2, "home", 0, 5, true),
            hut(3, "builder", 0, 5, true));
        assertEquals(3, GovernorPolicy.choose(huts, ready, 2, priorities).orElseThrow().position());
    }
    @Test void playerCapAndBuildingCapAreRespected() {
        var huts = List.of(hut(1, "builder", 2, 5, true), hut(2, "home", 1, 1, true));
        assertTrue(GovernorPolicy.choose(huts, ready, 2, priorities).isEmpty());
    }
    @Test void unloadedBuildingsCannotQueueConstruction() {
        assertTrue(GovernorPolicy.choose(List.of(hut(1, "builder", 0, 5, false)), ready, 2, priorities).isEmpty());
    }
    @Test void revokedPermissionUnloadedCenterRaidsAndPendingWorkPauseGovernor() {
        var huts = List.of(hut(1, "builder", 0, 5, true));
        for (var gate : List.of(new GovernorPolicy.Gate(false, true, false, false),
                new GovernorPolicy.Gate(true, false, false, false),
                new GovernorPolicy.Gate(true, true, true, false),
                new GovernorPolicy.Gate(true, true, false, true))) {
            assertTrue(GovernorPolicy.choose(huts, gate, 2, priorities).isEmpty());
        }
    }
    @Test void orderingIsStableIndependentOfMapIterationOrder() {
        assertEquals(1, GovernorPolicy.choose(List.of(hut(2, "home", 1, 5, true),
            hut(1, "home", 1, 5, true)), ready, 2, priorities).orElseThrow().position());
    }
    @Test void invalidCapsCannotStartAutomation() {
        assertThrows(IllegalArgumentException.class,
            () -> GovernorPolicy.choose(List.of(), ready, 0, priorities));
    }
}
