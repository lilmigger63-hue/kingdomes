package dev.kingdoms.livingsettlements;

import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class SettlementPersistenceTest {
    @Test void staleAndMissingDiskCheckpointsCannotAuthorizeWorldChanges() {
        var owner=UUID.randomUUID(); var expected=new SettlementPersistence.Checkpoint(42,owner,1,1,5,true);
        var root=new CompoundTag(); assertFalse(SettlementPersistence.contains(root,expected));
        var entry=new CompoundTag(); entry.putLong("center",42); entry.putString("owner",owner.toString());
        var list=new ListTag(); list.add(entry); root.put("settlements",list);
        assertFalse(SettlementPersistence.contains(root,expected));
        var plans=new ListTag(); plans.add(new CompoundTag()); entry.put("plans",plans);
        entry.putInt("next",1); entry.putInt("fallbackCursor",5); entry.putBoolean("supplies",true);
        assertTrue(SettlementPersistence.contains(root,expected));
        entry.putInt("fallbackCursor",4); assertFalse(SettlementPersistence.contains(root,expected));
    }
}
