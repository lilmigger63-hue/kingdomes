package dev.kingdoms.livingsettlements;

/** Persisted handoff of already-paid founding items; callbacks are never the only copy of a delivery. */
final class MayorEscrow {
    static void received(MayorSavedData.Mayor m,int delivered){
        if(m.pendingStage!=0||delivered<0||delivered>m.pendingCount)throw new IllegalArgumentException("Invalid delivered amount/receipt replay");
        int refund=m.pendingCount-delivered;
        if(refund>0&&m.pendingSource>=0){var source=m.stock.get(m.pendingSource);source.putInt("remaining",source.getIntOr("remaining",0)+refund);}
        m.pendingCount=delivered;m.pendingStage=1;
    }
    static void clear(MayorSavedData.Mayor m){
        m.pendingKind="";m.pendingItem=new net.minecraft.nbt.CompoundTag();m.pendingTarget=Long.MIN_VALUE;m.pendingCount=0;m.pendingBefore=0;m.pendingStage=0;m.pendingSource=-1;
    }
    static void refund(MayorSavedData.Mayor m){
        if(m.pendingKind.isEmpty()||m.pendingCount==0)return;
        if(m.pendingStage!=0)throw new IllegalArgumentException("Received items cannot be refunded");
        if(m.pendingSource>=0){var entry=m.stock.get(m.pendingSource);entry.putInt("remaining",entry.getIntOr("remaining",0)+m.pendingCount);return;}
        // A real item taken from a native inventory remains a real owned asset on canceled placement.
        for(var entry:m.stock)if(entry.getBooleanOr("external",false)&&entry.getCompoundOrEmpty("item").equals(m.pendingItem)){
            entry.putInt("remaining",entry.getIntOr("remaining",0)+m.pendingCount);return;
        }
        if(m.stock.size()>=1024)throw new IllegalStateException("Refund inventory is full; escrow retained");
        var entry=new net.minecraft.nbt.CompoundTag();entry.put("item",m.pendingItem.copy());entry.putInt("remaining",m.pendingCount);entry.putBoolean("external",true);entry.putLong("for",Long.MIN_VALUE);m.stock.add(entry);
    }
}
