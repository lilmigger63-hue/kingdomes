package dev.kingdoms.livingsettlements;

import com.minecolonies.api.colony.IColonyManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

public final class OfflineDevelopment {
    private OfflineDevelopment() {}
    private static int cursor;
    private static final java.util.Map<ServerLevel,long[]> BUDGETS=new java.util.WeakHashMap<>();
    static boolean takeBudget(ServerLevel level) {
        var budget=BUDGETS.computeIfAbsent(level,ignored->new long[]{Long.MIN_VALUE,0});
        if(budget[0]!=level.getGameTime()) { budget[0]=level.getGameTime(); budget[1]=0; }
        return budget[1]++<NaturalSettlementConfig.UPDATES.get();
    }
    static void accrue(NaturalSettlementsData.Settlement s,long now) {
        if (s.lastDevelopmentTick < 0 || now < s.lastDevelopmentTick) { s.lastDevelopmentTick=now; return; }
        long intervals=(now-s.lastDevelopmentTick)/1200;
        if (intervals==0) return;
        s.lastDevelopmentTick=now;
        if (s.complete && !s.failed && s.observedPopulation>0 && s.observedFood>=s.observedPopulation && s.observedWork==0)
            s.developmentCredit=(int)Math.min(8,s.developmentCredit+Math.min(8,intervals));
    }
    public static void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !level.dimension().equals(Level.OVERWORLD)
            || !NaturalSettlementConfig.AUTONOMOUS.get() || level.getGameTime()%20!=0) return;
        var data=NaturalSettlementsData.get(level); var settlements=data.settlements().stream().filter(s->s.complete&&!s.failed).toList();
        if (settlements.isEmpty()) return;
        for (int i=0; i<Math.min(settlements.size(),NaturalSettlementConfig.UPDATES.get()); i++) {
            if(!takeBudget(level)) break;
            var s=settlements.get(Math.floorMod(cursor++,settlements.size()));
            if(NaturalSettlementConfig.MAYOR.get()&&MayorSavedData.get(level).at(s.colonyId)!=null)continue;
            var colony=IColonyManager.getInstance().getColonyByWorld(s.colonyId,level);
            if (!NpcSettlementBootstrap.stillOwned(colony,s)) continue;
            long now=level.getGameTime(); if (s.lastDevelopmentTick>=0 && now>=s.lastDevelopmentTick && now-s.lastDevelopmentTick<1200) continue;
            // Saved real inventories are read without obtaining chunks or touching citizen entities.
            boolean loaded=ColonyIntegration.loaded(level,BlockPos.of(s.center));
            if (loaded) {
                s.observedPopulation=colony.getCitizenManager().getCitizens().size();
                s.observedWork=colony.getWorkManager().getWorkOrders().size();
                int food=0;
                for (var citizen:colony.getCitizenManager().getCitizens()) {
                    var inventory=citizen.getInventory();
                    for (int slot=0;slot<inventory.getSlots();slot++) {
                        var stack=inventory.getStackInSlot(slot);
                        if (stack.has(DataComponents.FOOD)) food=Math.min(1_000_000,food+stack.getCount());
                    }
                }
                s.observedFood=food;
            }
            if (loaded || NaturalSettlementConfig.OFFLINE.get()) accrue(s,now);
            else s.lastDevelopmentTick=now;
            data.setDirty();
        }
    }
}
