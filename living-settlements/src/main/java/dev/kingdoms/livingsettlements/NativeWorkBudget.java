package dev.kingdoms.livingsettlements;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import net.minecraft.server.level.ServerLevel;
import java.util.*;

/** Fair native work-step admission. It never substitutes for native tool/material checks. */
public final class NativeWorkBudget {
    private static final Map<ServerLevel,Gate> gates=new WeakHashMap<>();
    static final class Gate {
        private long tick=Long.MIN_VALUE;
        private int used;
        private final Map<Integer,Set<Integer>> workers=new HashMap<>();
        private final LinkedHashMap<Long,Long> waiting=new LinkedHashMap<>();
        boolean permit(long now,int colony,int citizen,int limit,int maxWorkers) {
            if(now!=tick){tick=now;used=0;workers.clear();waiting.entrySet().removeIf(e->now-e.getValue()>40||now<e.getValue());}
            long key=((long)colony<<32)^Integer.toUnsignedLong(citizen);waiting.putIfAbsent(key,now);
            if(used>=limit||waiting.keySet().iterator().next()!=key)return false;
            var active=workers.computeIfAbsent(colony,k->new HashSet<>());
            if(!active.contains(citizen)&&active.size()>=maxWorkers)return false;
            waiting.remove(key);active.add(citizen);used++;return true;
        }
    }
    public static boolean managed(IColony colony) {
        if(colony==null||!(colony.getWorld() instanceof ServerLevel level))return false;
        var mayor=MayorSavedData.get(level).at(colony.getID());
        return mayor!=null&&mayor.owner.equals(colony.getPermissions().getOwner());
    }
    public static boolean permit(AbstractEntityCitizen worker) {
        var data=worker.getCitizenData();if(data==null||!managed(data.getColony())||!(worker.level() instanceof ServerLevel level))return true;
        return gates.computeIfAbsent(level,k->new Gate()).permit(level.getGameTime(),data.getColony().getID(),data.getId(),NaturalSettlementConfig.TERRAFORM_BLOCKS.get(),NaturalSettlementConfig.TERRAFORM_WORKERS.get());
    }
}
