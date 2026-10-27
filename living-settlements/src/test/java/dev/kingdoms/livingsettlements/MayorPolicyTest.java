package dev.kingdoms.livingsettlements;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MayorPolicyTest {
    private MayorPolicy.Snapshot state(int builder,int farmer,int housing,int food,boolean raid,boolean work,int buildings) {
        return new MayorPolicy.Snapshot(4,housing,food,64,64,builder,farmer,2,1,1,1,1,0,buildings,3,raid,work);
    }
    @Test void builderIsFirstEvenWithHungryHomelessCitizens() { assertEquals("builder",MayorPolicy.choose(state(0,0,0,0,false,false,1),32,5).role()); }
    @Test void hungerPrioritizesFoodBeforeHousing() { assertEquals("farmer",MayorPolicy.choose(state(1,0,0,1,false,false,2),32,5).role()); }
    @Test void housingShortagePrecedesOrdinaryExpansion() { assertEquals("home",MayorPolicy.choose(state(1,1,2,64,false,false,4),32,5).role()); }
    @Test void activeWorkGetsSupportInsteadOfDuplicateConstruction() { assertEquals("support",MayorPolicy.choose(state(1,0,0,0,false,true,2),32,5).role()); }
    @Test void raidPrioritizesSafetyAndDefense() { assertEquals("defense",MayorPolicy.choose(state(1,1,8,64,true,false,8),32,5).role()); }
    @Test void limitsCannotStartUnboundedExpansion() { assertEquals("upgrade",MayorPolicy.choose(state(2,1,12,64,false,false,32),32,5).role()); }
}
