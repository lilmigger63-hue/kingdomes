package dev.kingdoms.livingsettlements;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class NativeWorkBudgetTest {
    @Test void manyColoniesShareBoundedWorkWithoutStarvingLaterCitizens() {
        var gate=new NativeWorkBudget.Gate();var served=new java.util.HashSet<Integer>();
        for(long tick=0;tick<30;tick++) {
            int count=0;
            for(int worker=0;worker<100;worker++)if(gate.permit(tick,worker/5+1,worker,8,3)){count++;served.add(worker);}
            assertTrue(count<=8);
        }
        assertEquals(100,served.size());
    }
    @Test void concurrentColonyWorkersAreBoundedAndExpiredWaitersDoNotStall() {
        var gate=new NativeWorkBudget.Gate();
        assertTrue(gate.permit(0,1,1,8,1));
        assertFalse(gate.permit(0,1,2,8,1));
        assertTrue(gate.permit(100,2,3,8,1));
    }
}
