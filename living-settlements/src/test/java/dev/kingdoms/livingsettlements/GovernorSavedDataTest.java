package dev.kingdoms.livingsettlements;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class GovernorSavedDataTest {
    private final UUID owner = UUID.fromString("a69a7af0-67ee-4321-b63a-ed02efca3900");

    @Test void authorizationAndCapSurviveSaveReload() {
        var data = new GovernorSavedData(List.of());
        data.enable(17, owner, 3);
        var json = GovernorSavedData.CODEC.encodeStart(JsonOps.INSTANCE, data).getOrThrow();
        var loaded = GovernorSavedData.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        assertEquals(new GovernorSavedData.Assignment(17, owner, 3), loaded.assignment(17));
        assertNull(loaded.assignment(18));
        assertTrue(data.isDirty());
    }
    @Test void disabledGovernorStaysDisabledAfterReload() {
        var data = new GovernorSavedData(List.of(new GovernorSavedData.Assignment(17, owner, 3)));
        data.disable(17);
        var json = GovernorSavedData.CODEC.encodeStart(JsonOps.INSTANCE, data).getOrThrow();
        assertTrue(GovernorSavedData.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow().assignments().isEmpty());
        assertTrue(data.isDirty());
    }
    @Test void malformedAuthorizerAndOutOfRangeCapsAreRejected() {
        for (String fields : List.of(
                "\"authorizer\":\"broken\",\"maximum_level\":2",
                "\"authorizer\":\"" + owner + "\",\"maximum_level\":0",
                "\"authorizer\":\"" + owner + "\",\"maximum_level\":6")) {
            var json = JsonParser.parseString("{\"governors\":[{\"colony_id\":17," + fields + "}]}");
            assertTrue(GovernorSavedData.CODEC.parse(JsonOps.INSTANCE, json).error().isPresent());
        }
    }
    @Test void duplicateColonyAssignmentsCannotSilentlyReplaceAuthorization() {
        String entry = "{\"colony_id\":17,\"authorizer\":\"" + owner + "\",\"maximum_level\":2}";
        var json = JsonParser.parseString("{\"governors\":[" + entry + "," + entry + "]}");
        assertTrue(GovernorSavedData.CODEC.parse(JsonOps.INSTANCE, json).error().isPresent());
    }
}
