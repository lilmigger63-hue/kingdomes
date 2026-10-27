package dev.kingdoms.livingsettlements;

import com.mojang.serialization.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.*;
import java.util.*;

/** Additive mayor state; native MineColonies remains the authority for citizens, items and work. */
public final class MayorSavedData extends SavedData {
    public static final class Mayor {
        final int colonyId; final UUID owner;
        int citizenId, stage=1, forecastFood, foodRate, attempts;
        int woodRate,stoneRate,forecastWood,forecastStone;
        UUID citizenUuid;
        long revision, nextDecision, nextRecruit, pausedUntil, lastSnapshot=-1, lastEconomy=-1, target=Long.MIN_VALUE;
        int targetLevel;
        String goal="", report="Assessing founding needs";
        MayorPolicy.Snapshot snapshot=new MayorPolicy.Snapshot(0,0,0,0,0,0,0,0,0,0,0,0,0,1,1,false,false);
        boolean kitInitialized;
        int kitUnits;
        String pendingKind="";
        CompoundTag pendingItem=new CompoundTag();
        long pendingTarget=Long.MIN_VALUE;
        int pendingCount,pendingBefore,pendingStage,pendingSource=-1;
        final List<CompoundTag> stock=new ArrayList<>();
        final Map<Long,Long> retryAfter=new LinkedHashMap<>();
        final Map<String,Long> goalAfter=new LinkedHashMap<>();
        final List<Long> roads=new ArrayList<>();
        final List<Long> roadPoints=new ArrayList<>();
        Mayor(int id,UUID owner) { this.colonyId=id; this.owner=Objects.requireNonNull(owner); }
        public int citizenId() { return citizenId; }
        public UUID citizenUuid() { return citizenUuid; }
        public String report() { return report; }
        public String goal() { return goal; }
        public int stage() { return stage; }
        public int stockUnits() { return stock.stream().mapToInt(t->t.getIntOr("remaining",0)).sum(); }
        public boolean kitReady() { return kitInitialized; }
        public int foundingUnits() { return kitUnits; }
        public String pendingDelivery() { return pendingKind; }
        public int roadWaypoints() { return roadPoints.size(); }
    }
    public static final Codec<MayorSavedData> CODEC=CompoundTag.CODEC.flatXmap(tag->{
        try { return DataResult.success(read(tag)); } catch(RuntimeException ex) { return DataResult.error(()->"Invalid mayor save: "+ex.getMessage()); }
    },data->DataResult.success(data.write()));
    public static final SavedDataType<MayorSavedData> TYPE=new SavedDataType<>(Identifier.fromNamespaceAndPath(LivingSettlements.MOD_ID,"mayors"),MayorSavedData::new,CODEC);
    private final Map<Integer,Mayor> mayors=new LinkedHashMap<>();
    public static MayorSavedData get(ServerLevel level) { return level.getDataStorage().computeIfAbsent(TYPE); }
    public Mayor at(int id) { return mayors.get(id); }
    Mayor ensure(int id,UUID owner) {
        if(id<=0) throw new IllegalArgumentException("Invalid colony ID");
        return mayors.computeIfAbsent(id,key->{setDirty();return new Mayor(key,owner);});
    }
    void changed(Mayor m) { m.revision++;setDirty(); }
    private CompoundTag write() {
        var root=new CompoundTag();root.putInt("version",1);var entries=new ListTag();
        for(var m:mayors.values()) {
            var t=new CompoundTag();t.putInt("colony",m.colonyId);t.putString("owner",m.owner.toString());
            t.putInt("citizen",m.citizenId);if(m.citizenUuid!=null)t.putString("uuid",m.citizenUuid.toString());
            t.putLong("revision",m.revision);t.putInt("stage",m.stage);t.putLong("next",m.nextDecision);t.putLong("recruit",m.nextRecruit);t.putLong("paused",m.pausedUntil);
            t.putLong("snapshotTick",m.lastSnapshot);t.putLong("economyTick",m.lastEconomy);t.putInt("foodRate",m.foodRate);t.putInt("forecastFood",m.forecastFood);
            t.putInt("woodRate",m.woodRate);t.putInt("stoneRate",m.stoneRate);t.putInt("forecastWood",m.forecastWood);t.putInt("forecastStone",m.forecastStone);
            t.putString("goal",m.goal);t.putString("report",m.report);t.putLong("target",m.target);t.putInt("targetLevel",m.targetLevel);t.putInt("attempts",m.attempts);
            t.putBoolean("kit",m.kitInitialized);t.putInt("kitUnits",m.kitUnits);var stock=new ListTag();m.stock.forEach(s->stock.add(s.copy()));t.put("stock",stock);
            t.putString("pendingKind",m.pendingKind);t.put("pendingItem",m.pendingItem.copy());t.putLong("pendingTarget",m.pendingTarget);t.putInt("pendingCount",m.pendingCount);t.putInt("pendingBefore",m.pendingBefore);t.putInt("pendingStage",m.pendingStage);t.putInt("pendingSource",m.pendingSource);
            var cooldowns=new ListTag();m.retryAfter.forEach((pos,time)->{var c=new CompoundTag();c.putLong("pos",pos);c.putLong("time",time);cooldowns.add(c);});t.put("cooldowns",cooldowns);
            var goals=new ListTag();m.goalAfter.forEach((role,time)->{var c=new CompoundTag();c.putString("role",role);c.putLong("time",time);goals.add(c);});t.put("goalCooldowns",goals);
            var roads=new ListTag();for(long pos:m.roads){var r=new CompoundTag();r.putLong("pos",pos);roads.add(r);}t.put("roads",roads);
            var points=new ListTag();for(long pos:m.roadPoints){var r=new CompoundTag();r.putLong("pos",pos);points.add(r);}t.put("roadPoints",points);
            var s=m.snapshot;var a=new int[]{s.population(),s.housing(),s.food(),s.wood(),s.stone(),s.builder(),s.farmer(),s.homes(),s.lumberjack(),s.miner(),s.warehouse(),s.courier(),s.guards(),s.buildings(),s.stage()};
            var amounts=new ListTag();for(int amount:a)amounts.add(IntTag.valueOf(amount));t.put("snapshot",amounts);
            t.putBoolean("raid",s.threatened());t.putBoolean("work",s.workPending());entries.add(t);
        }
        root.put("mayors",entries);return root;
    }
    private static MayorSavedData read(CompoundTag root) {
        if(root.getIntOr("version",0)!=1)throw new IllegalArgumentException("Unsupported mayor schema");
        var data=new MayorSavedData();
        for(var element:root.getListOrEmpty("mayors")) {
            if(!(element instanceof CompoundTag t))throw new IllegalArgumentException("Invalid entry");
            int id=t.getIntOr("colony",0);if(id<=0||data.mayors.containsKey(id))throw new IllegalArgumentException("Duplicate/invalid colony");
            var m=data.ensure(id,UUID.fromString(t.getStringOr("owner","")));
            m.citizenId=t.getIntOr("citizen",0);m.citizenUuid=t.getString("uuid").map(UUID::fromString).orElse(null);
            m.revision=t.getLongOr("revision",0);m.stage=t.getIntOr("stage",1);m.nextDecision=t.getLongOr("next",0);m.nextRecruit=t.getLongOr("recruit",0);m.pausedUntil=t.getLongOr("paused",0);
            m.lastSnapshot=t.getLongOr("snapshotTick",-1);m.lastEconomy=t.getLongOr("economyTick",-1);m.foodRate=t.getIntOr("foodRate",0);m.forecastFood=t.getIntOr("forecastFood",0);
            m.woodRate=t.getIntOr("woodRate",0);m.stoneRate=t.getIntOr("stoneRate",0);m.forecastWood=t.getIntOr("forecastWood",0);m.forecastStone=t.getIntOr("forecastStone",0);
            m.goal=t.getStringOr("goal","");m.report=t.getStringOr("report","");m.target=t.getLongOr("target",Long.MIN_VALUE);m.targetLevel=t.getIntOr("targetLevel",0);m.attempts=t.getIntOr("attempts",0);
            m.kitInitialized=t.getBooleanOr("kit",false);m.kitUnits=t.getIntOr("kitUnits",0);
            m.pendingKind=t.getStringOr("pendingKind","");m.pendingItem=t.getCompoundOrEmpty("pendingItem").copy();m.pendingTarget=t.getLongOr("pendingTarget",Long.MIN_VALUE);m.pendingCount=t.getIntOr("pendingCount",0);m.pendingBefore=t.getIntOr("pendingBefore",0);m.pendingStage=t.getIntOr("pendingStage",0);m.pendingSource=t.getIntOr("pendingSource",-1);
            for(var value:t.getListOrEmpty("stock")) {if(!(value instanceof CompoundTag entry)||!entry.contains("item")||entry.getIntOr("remaining",-1)<0||entry.getIntOr("remaining",0)>32768)throw new IllegalArgumentException("Invalid stock");m.stock.add(entry.copy());}
            for(var value:t.getListOrEmpty("cooldowns"))if(value instanceof CompoundTag c)m.retryAfter.put(c.getLongOr("pos",0),c.getLongOr("time",0));
            for(var value:t.getListOrEmpty("goalCooldowns"))if(value instanceof CompoundTag c)m.goalAfter.put(c.getStringOr("role",""),c.getLongOr("time",0));
            for(var value:t.getListOrEmpty("roads"))if(value instanceof CompoundTag r)m.roads.add(r.getLongOr("pos",0));
            for(var value:t.getListOrEmpty("roadPoints"))if(value instanceof CompoundTag r)m.roadPoints.add(r.getLongOr("pos",0));
            var values=t.getListOrEmpty("snapshot");var a=new int[values.size()];for(int i=0;i<a.length;i++)a[i]=values.getIntOr(i,-1);
            if(a.length!=15)throw new IllegalArgumentException("Invalid snapshot");
            for(int n:a)if(n<0||n>1_000_000)throw new IllegalArgumentException("Invalid snapshot amount");
            m.snapshot=new MayorPolicy.Snapshot(a[0],a[1],a[2],a[3],a[4],a[5],a[6],a[7],a[8],a[9],a[10],a[11],a[12],a[13],a[14],t.getBooleanOr("raid",false),t.getBooleanOr("work",false));
            int foundingRemaining=m.stock.stream().filter(entry->!entry.getBooleanOr("external",false)).mapToInt(entry->entry.getIntOr("remaining",0)).sum();
            if(m.citizenId<0||(m.citizenId==0)!=(m.citizenUuid==null)||m.stage<1||m.stage>5||m.targetLevel<0||m.targetLevel>5||m.attempts<0||m.attempts>100||m.stock.size()>1024||m.kitUnits<0||m.kitUnits>32768||foundingRemaining>m.kitUnits||m.stockUnits()>65536||m.retryAfter.size()>128||m.goalAfter.size()>32||m.roads.size()>64)throw new IllegalArgumentException("Mayor limits exceeded");
            if(m.roadPoints.size()>2048||!Set.of("","supply","hut","field").contains(m.pendingKind)||m.pendingCount<0||m.pendingCount>64||m.pendingBefore<0||m.pendingBefore>1000000||m.pendingStage<0||m.pendingStage>1||m.pendingSource>=m.stock.size()||m.pendingSource< -1||(!m.pendingKind.isEmpty()&&m.pendingItem.isEmpty()))throw new IllegalArgumentException("Invalid delivery escrow");
        }
        data.setDirty(false);return data;
    }
}
