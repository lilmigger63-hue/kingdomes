package dev.kingdoms.livingsettlements;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
class MayorForecastTest {
    @Test void measuredFoodForecastIsCumulativeBoundedAndNeverCreatesPhysicalResources() {
        var m=new MayorSavedData().ensure(1,UUID.randomUUID());m.lastSnapshot=0;m.foodRate=2;
        m.snapshot=new MayorPolicy.Snapshot(4,6,100,64,64,1,1,4,1,1,1,1,0,12,3,false,false);
        MayorService.forecast(m,12000,1);assertEquals(120,m.forecastFood);
        MayorService.forecast(m,24000,1);assertEquals(140,m.forecastFood);
        MayorService.forecast(m,1200000,1);assertEquals(220,m.forecastFood);
        assertEquals(100,m.snapshot.food());assertEquals(4,m.snapshot.population());assertEquals(0,m.stockUnits());
        MayorService.forecast(m,12000,1,8,1);assertEquals("idle",m.goal);
        m.woodRate=-3;m.stoneRate=1;MayorService.forecast(m,1200000,1);assertEquals(0,m.forecastWood);assertEquals(124,m.forecastStone);assertEquals(64,m.snapshot.wood());assertEquals(64,m.snapshot.stone());
        m.foodRate=-10;MayorService.forecast(m,1200000,1);assertEquals(0,m.forecastFood);assertEquals("farmer",m.goal);
    }
}
