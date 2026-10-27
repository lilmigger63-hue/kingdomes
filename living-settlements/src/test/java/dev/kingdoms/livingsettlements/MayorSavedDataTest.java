package dev.kingdoms.livingsettlements;

import com.mojang.serialization.JsonOps;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class MayorSavedDataTest {
    @Test void identityPlanStockAndCooldownSurviveRestart() {
        var d=new MayorSavedData();var m=d.ensure(7,UUID.randomUUID());m.citizenId=3;m.citizenUuid=UUID.randomUUID();
        m.goal="home";m.target=42;m.nextDecision=1200;m.retryAfter.put(42L,12000L);m.roads.add(42L);m.kitInitialized=true;
        var stack=new CompoundTag();stack.putInt("remaining",16);var item=new CompoundTag();item.putString("id","minecraft:oak_log");stack.put("item",item);m.stock.add(stack);m.kitUnits=16;d.changed(m);
        var loaded=MayorSavedData.CODEC.parse(JsonOps.INSTANCE,MayorSavedData.CODEC.encodeStart(JsonOps.INSTANCE,d).getOrThrow()).getOrThrow().at(7);
        assertEquals(m.citizenUuid,loaded.citizenUuid);assertEquals("home",loaded.goal);assertEquals(16,loaded.stockUnits());assertEquals(12000,loaded.retryAfter.get(42L));
        assertSame(m,d.ensure(7,m.owner));assertEquals(m.owner,d.ensure(7,UUID.randomUUID()).owner);
    }
    @Test void corruptStockAndDuplicateMayorsCannotLoad() {
        var d=new MayorSavedData();d.ensure(7,UUID.randomUUID());var json=MayorSavedData.CODEC.encodeStart(JsonOps.INSTANCE,d).getOrThrow().getAsJsonObject();
        json.getAsJsonArray("mayors").add(json.getAsJsonArray("mayors").get(0).deepCopy());
        assertTrue(MayorSavedData.CODEC.parse(JsonOps.INSTANCE,json).error().isPresent());
    }
}
