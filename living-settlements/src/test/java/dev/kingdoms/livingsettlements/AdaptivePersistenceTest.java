package dev.kingdoms.livingsettlements;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class AdaptivePersistenceTest {
    @Test void rotationAccessVetoAndSupportProgressRemainAdditiveAcrossReload() {
        var data=new NaturalSettlementsData();var s=data.discover(42,new BlockPos(100,0,100),4,112);
        s.adaptive=true;s.townHallOnly=true;s.supportCursor=14;
        long anchor=new BlockPos(100,80,100).asLong();s.plans.add(new NaturalSettlementsData.BuildingPlan("townhall","townhall1.blueprint",anchor,98,79,98,5,6,5,1,2));
        s.blockedPlots.add(anchor);s.accesses.put(anchor,List.of(new BlockPos(103,81,100).asLong()));
        var restored=NaturalSettlementsData.CODEC.parse(JsonOps.INSTANCE,NaturalSettlementsData.CODEC.encodeStart(JsonOps.INSTANCE,data).getOrThrow()).getOrThrow().at(s.center);
        assertTrue(restored.adaptive);assertEquals(14,restored.supportCursor);assertEquals(2,restored.plans.getFirst().rotation());
        assertEquals(s.blockedPlots,restored.blockedPlots);assertEquals(s.accesses,restored.accesses);
        assertEquals(0,new NaturalSettlementsData.BuildingPlan("home","residence1.blueprint",anchor,98,79,98,5,6,5,1).rotation());
    }
}
