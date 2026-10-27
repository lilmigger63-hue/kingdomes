package dev.kingdoms.livingsettlements;

import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OfflineDevelopmentTest {
    @Test void boundedIntentUsesRealSnapshotWithoutInventingSuppliesOrCitizens() {
        var data = new NaturalSettlementsData(); var s = data.discover(1,BlockPos.ZERO,6,112);
        s.complete=true; s.observedPopulation=6; s.observedFood=32; s.observedWork=0;
        OfflineDevelopment.accrue(s,100);
        OfflineDevelopment.accrue(s,100+24000*100L);
        assertEquals(8,s.developmentCredit);
        assertEquals(32,s.observedFood); assertEquals(6,s.observedPopulation); assertEquals(0,s.nextBuilding);
        OfflineDevelopment.accrue(s,100+24000*100L);
        assertEquals(8,s.developmentCredit);
        var loaded=NaturalSettlementsData.CODEC.parse(JsonOps.INSTANCE,NaturalSettlementsData.CODEC.encodeStart(JsonOps.INSTANCE,data).getOrThrow()).getOrThrow().at(s.center);
        assertEquals(s.lastDevelopmentTick,loaded.lastDevelopmentTick); assertEquals(8,loaded.developmentCredit);
    }
    @Test void missingResourcesAndRewoundClockCannotCreateProgress() {
        var data = new NaturalSettlementsData(); var s=data.discover(1,BlockPos.ZERO,6,112);
        s.complete=true; s.observedPopulation=6; s.observedFood=0;
        OfflineDevelopment.accrue(s,100); OfflineDevelopment.accrue(s,50000);
        assertEquals(0,s.developmentCredit);
        s.observedFood=40; OfflineDevelopment.accrue(s,5);
        assertEquals(0,s.developmentCredit);
    }
}
