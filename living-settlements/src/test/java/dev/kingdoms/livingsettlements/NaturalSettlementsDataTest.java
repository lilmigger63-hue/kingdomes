package dev.kingdoms.livingsettlements;

import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NaturalSettlementsDataTest {
    @Test void legacyReservationsDoNotChangeToTownHallOnlyAfterUpdate() {
        var data=new NaturalSettlementsData();data.discover(42,BlockPos.ZERO,6,112);
        var json=NaturalSettlementsData.CODEC.encodeStart(JsonOps.INSTANCE,data).getOrThrow().getAsJsonObject();
        json.getAsJsonArray("settlements").get(0).getAsJsonObject().remove("townHallOnly");
        var loaded=NaturalSettlementsData.CODEC.parse(JsonOps.INSTANCE,json).getOrThrow().at(0);
        assertFalse(loaded.townHallOnly());
    }
    @Test void retainedVillageProtectionSurvivesReloadWithoutItsOriginChunk() {
        var data=new NaturalSettlementsData(); var s=data.discover(42,BlockPos.ZERO,6,64);
        s.failed=true; s.fallback=true; s.editMinX=-128; s.editMaxX=150; s.editMinZ=-100; s.editMaxZ=100;
        assertTrue(s.protects(new BlockPos(140,70,0)));
        var loaded=NaturalSettlementsData.CODEC.parse(JsonOps.INSTANCE,NaturalSettlementsData.CODEC.encodeStart(JsonOps.INSTANCE,data).getOrThrow()).getOrThrow().at(s.center);
        assertTrue(loaded.protects(new BlockPos(140,70,0)));
        assertFalse(loaded.protects(new BlockPos(151,70,0)));
        loaded.fallback=false; assertFalse(loaded.protects(new BlockPos(140,70,0)));
    }
    @Test void repeatedDiscoveryAndReloadPreserveOneReservationAndProgress() {
        var data = new NaturalSettlementsData();
        var s = data.discover(42, new BlockPos(-100, 0, 200), 6, 112);
        assertSame(s, data.discover(42, new BlockPos(-100, 0, 200), 8, 128));
        s.plans.add(new NaturalSettlementsData.BuildingPlan("townhall", "fundamentals/townhall1.blueprint", new BlockPos(-100, 70, 200).asLong(), -110, 68, 190, 20, 15, 20, 1));
        s.colonyId = 7; s.nextBuilding = 1; s.suppliesGranted = true; s.complete = true;
        var loaded = NaturalSettlementsData.CODEC.parse(JsonOps.INSTANCE,
            NaturalSettlementsData.CODEC.encodeStart(JsonOps.INSTANCE, data).getOrThrow()).getOrThrow();
        assertEquals(1, loaded.settlements().size());
        assertTrue(loaded.ownsNpcColony(7, s.owner));
        assertFalse(loaded.ownsNpcColony(8, s.owner));
        assertTrue(loaded.at(s.center).suppliesGranted);
        assertEquals(1, loaded.at(s.center).nextBuilding);
    }
    @Test void duplicateReservationsAndFutureSchemasAreRejected() {
        var data = new NaturalSettlementsData(); data.discover(42, BlockPos.ZERO, 6, 112);
        var json = NaturalSettlementsData.CODEC.encodeStart(JsonOps.INSTANCE, data).getOrThrow().getAsJsonObject();
        json.getAsJsonArray("settlements").add(json.getAsJsonArray("settlements").get(0).deepCopy());
        assertTrue(NaturalSettlementsData.CODEC.parse(JsonOps.INSTANCE, json).error().isPresent());
        json.addProperty("version", 999);
        assertTrue(NaturalSettlementsData.CODEC.parse(JsonOps.INSTANCE, json).error().isPresent());
    }
    @Test void unsafePathsAndUnboundedSchematicSizesCannotBeReplayed() {
        assertThrows(IllegalArgumentException.class, () -> new NaturalSettlementsData.BuildingPlan("home", "../x", 0, 0, 0, 0, 20, 20, 20, 1));
        assertThrows(IllegalArgumentException.class, () -> new NaturalSettlementsData.BuildingPlan("home", "x", 0, 0, 0, 0, 10000, 20, 20, 1));
    }
}
