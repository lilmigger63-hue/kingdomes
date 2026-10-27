package dev.kingdoms.livingsettlements;

/** Needs-based administrative decisions; native actions/resources remain authoritative. */
public final class MayorPolicy {
    public record Snapshot(int population,int housing,int food,int wood,int stone,int builder,int farmer,int homes,
        int lumberjack,int miner,int warehouse,int courier,int guards,int buildings,int stage,boolean threatened,boolean workPending) {}
    public record Decision(String role,String reason) {}
    private MayorPolicy() {}
    public static Decision choose(Snapshot s,int maxBuildings,int maximumStage) {
        if(s.threatened())return new Decision("defense","Threat: shelter civilians and prioritize native guards");
        if(s.workPending())return new Decision("support","Supply existing construction before adding another order");
        if(maximumStage<2)return new Decision("idle","Configured founding stage only");
        if(s.buildings()>=maxBuildings)return new Decision("upgrade","Building limit reached; improve existing infrastructure");
        if(s.builder()<1)return new Decision("builder","A working builder must bootstrap all later construction");
        if(s.farmer()==0||s.food()<Math.max(8,s.population()*4))return new Decision("farmer","Food production or reserves are insufficient");
        if(s.housing()<s.population()+1)return new Decision("home","Provide beds before immigration or expansion");
        if(s.warehouse()==0)return new Decision("warehouse","Central storage is missing");
        if(maximumStage<3)return new Decision("upgrade","Survival infrastructure established");
        if(s.lumberjack()==0||s.wood()<32)return new Decision("lumberjack","Secure renewable construction wood");
        if(s.miner()==0||s.stone()<32)return new Decision("miner","Secure construction stone");
        if(s.courier()==0)return new Decision("courier","Connect stores to native delivery requests");
        if(maximumStage>=4&&s.population()>=8&&s.guards()==0)return new Decision("guard","Provide native defense before further growth");
        if(maximumStage>=4&&s.stage()>=4)return new Decision("specialize","Develop production and research before higher tiers");
        return new Decision("upgrade","Balanced infrastructure: develop existing buildings");
    }
}
