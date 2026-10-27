package dev.kingdoms.livingsettlements;
import com.mojang.serialization.JsonOps;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
class MayorEscrowTest {
    @Test void canceledPaidActionReturnsExactlyItsRealAssetAndCannotRefundTwice(){
        var m=new MayorSavedData().ensure(1,UUID.randomUUID());m.pendingKind="hut";m.pendingCount=1;m.pendingItem.putString("id","minecolonies:blockhutbuilder");
        MayorEscrow.refund(m);MayorEscrow.clear(m);assertEquals(1,m.stockUnits());assertEquals(0,m.kitUnits);
        MayorEscrow.refund(m);assertEquals(1,m.stockUnits());
    }
    @Test void interruptedDeliveryPersistsItsItemsAndPartialAcceptanceRefundsOnlyTheRemainder(){
        var data=new MayorSavedData();var m=data.ensure(1,UUID.randomUUID());m.kitInitialized=true;m.kitUnits=64;
        var item=new CompoundTag();item.putString("id","minecraft:oak_planks");var entry=new CompoundTag();entry.put("item",item);entry.putInt("remaining",0);m.stock.add(entry);
        m.pendingKind="supply";m.pendingItem=item;m.pendingTarget=123;m.pendingCount=64;m.pendingSource=0;
        var loaded=MayorSavedData.CODEC.parse(JsonOps.INSTANCE,MayorSavedData.CODEC.encodeStart(JsonOps.INSTANCE,data).getOrThrow()).getOrThrow().at(1);
        assertEquals("supply",loaded.pendingKind);assertEquals(64,loaded.pendingCount);assertEquals(123,loaded.pendingTarget);
        MayorEscrow.received(loaded,16);assertEquals(48,loaded.stockUnits());assertEquals(1,loaded.pendingStage);assertEquals(16,loaded.pendingCount);
        assertThrows(IllegalArgumentException.class,()->MayorEscrow.received(loaded,0));assertEquals(48,loaded.stockUnits());
        MayorEscrow.clear(loaded);assertEquals(48,loaded.stockUnits());assertEquals("",loaded.pendingKind);
    }
}
