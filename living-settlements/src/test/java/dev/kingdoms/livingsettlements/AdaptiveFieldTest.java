package dev.kingdoms.livingsettlements;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AdaptiveFieldTest {
    @Test void deferredAdaptiveFarmerMustObtainAFieldBeforeBeingUpgraded() {
        var data=new NaturalSettlementsData();var s=data.discover(1,new BlockPos(0,0,0),4,112);s.adaptive=true;
        assertTrue(MayorService.needsFieldSite(s));
        var field=new NaturalSettlementsData.BuildingPlan("field","natural_field",new BlockPos(20,81,20).asLong(),16,80,16,9,3,9,0);s.plans.add(field);
        assertFalse(MayorService.needsFieldSite(s));
        s.blockedPlots.add(field.anchor());assertTrue(MayorService.needsFieldSite(s));
    }
    @Test void legacyLayoutsRetainTheirExistingFieldBehavior() {
        var data=new NaturalSettlementsData();var s=data.discover(1,new BlockPos(0,0,0),4,112);
        assertFalse(MayorService.needsFieldSite(s));
    }
}
