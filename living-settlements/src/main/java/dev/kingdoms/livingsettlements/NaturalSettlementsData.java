package dev.kingdoms.livingsettlements;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import java.util.*;

/** Only generation reservations/identity/progress; actual colonies remain in MineColonies saves. */
public final class NaturalSettlementsData extends SavedData {
    public record BuildingPlan(String role, String path, long anchor, int minX, int minY, int minZ,
            int sizeX, int sizeY, int sizeZ, int level, int rotation) {
        public BuildingPlan(String role,String path,long anchor,int minX,int minY,int minZ,int sizeX,int sizeY,int sizeZ,int level) {
            this(role,path,anchor,minX,minY,minZ,sizeX,sizeY,sizeZ,level,0);
        }
        public BuildingPlan {
            if(rotation<0||rotation>3)throw new IllegalArgumentException("Invalid blueprint rotation");
            if (role == null || path == null || path.contains("..") || path.startsWith("/") || path.contains(":")) {
                throw new IllegalArgumentException("Unsafe natural blueprint path");
            }
            if (sizeX < 1 || sizeY < 1 || sizeZ < 1 || sizeX > 96 || sizeY > 96 || sizeZ > 96 || level < 0 || level > 5) {
                throw new IllegalArgumentException("Invalid natural building dimensions/level");
            }
        }
        NaturalGenerationRules.Box box() { return new NaturalGenerationRules.Box(minX, minZ, minX + sizeX - 1, minZ + sizeZ - 1); }
        CompoundTag write() {
            var tag = new CompoundTag(); tag.putString("role", role); tag.putString("path", path); tag.putLong("anchor", anchor);
            tag.putInt("x", minX); tag.putInt("y", minY); tag.putInt("z", minZ);
            tag.putInt("sx", sizeX); tag.putInt("sy", sizeY); tag.putInt("sz", sizeZ); tag.putInt("level", level);tag.putInt("rotation",rotation); return tag;
        }
        static BuildingPlan read(CompoundTag t) {
            return new BuildingPlan(t.getStringOr("role", ""), t.getStringOr("path", ""), t.getLongOr("anchor", 0),
                t.getIntOr("x", 0), t.getIntOr("y", 0), t.getIntOr("z", 0), t.getIntOr("sx", 0), t.getIntOr("sy", 0), t.getIntOr("sz", 0), t.getIntOr("level", 0),t.getIntOr("rotation",0));
        }
    }
    public static final class Settlement {
        final long center;
        final UUID owner;
        final int population;
        final int radius;
        String style = "";
        String status = "Waiting for loaded terrain";
        int colonyId;
        int nextBuilding;
        boolean suppliesGranted;
        boolean complete;
        boolean failed;
        long villageChunk = Long.MIN_VALUE;
        boolean fallback;
        int fallbackCursor;
        long lastDevelopmentTick = -1;
        int developmentCredit, observedPopulation, observedFood, observedWork;
        int editMinX,editMaxX,editMinZ,editMaxZ;
        boolean townHallOnly;
        boolean adaptive;
        boolean unrestricted;
        int supportCursor;
        final Set<Long> blockedPlots=new LinkedHashSet<>();
        final Map<Long,List<Long>> accesses=new LinkedHashMap<>();
        int groundY;
        final List<BuildingPlan> plans = new ArrayList<>();
        Settlement(long center, UUID owner, int population, int radius) {
            if (population < 4 || population > 12 || radius < 64 || radius > 192) { throw new IllegalArgumentException("Invalid settlement size"); }
            this.center = center; this.owner = Objects.requireNonNull(owner); this.population = population; this.radius = radius;
            var pos=BlockPos.of(center); editMinX=pos.getX()-radius; editMaxX=pos.getX()+radius;
            editMinZ=pos.getZ()-radius; editMaxZ=pos.getZ()+radius;
        }
        boolean protects(BlockPos pos) {
            return !complete && (!failed || fallback) && pos.getX()>=editMinX && pos.getX()<=editMaxX
                && pos.getZ()>=editMinZ && pos.getZ()<=editMaxZ;
        }
        public long center() { return center; }
        public UUID owner() { return owner; }
        public int colonyId() { return colonyId; }
        public boolean complete() { return complete; }
        public boolean failed() { return failed; }
        public String status() { return status; }
        public int developmentCredit() { return developmentCredit; }
        public boolean townHallOnly() { return townHallOnly; }
        public List<BuildingPlan> plans() { return List.copyOf(plans); }
    }
    public static final Codec<NaturalSettlementsData> CODEC = CompoundTag.CODEC.flatXmap(tag -> {
        try { return DataResult.success(read(tag)); }
        catch (RuntimeException ex) { return DataResult.error(() -> "Invalid natural settlements save: " + ex.getMessage()); }
    }, data -> DataResult.success(data.write()));
    public static final SavedDataType<NaturalSettlementsData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(LivingSettlements.MOD_ID, "natural_settlements"), NaturalSettlementsData::new, CODEC);
    private final Map<Long, Settlement> settlements = new LinkedHashMap<>();
    public static NaturalSettlementsData get(ServerLevel level) { return level.getDataStorage().computeIfAbsent(TYPE); }
    public Collection<Settlement> settlements() { return List.copyOf(settlements.values()); }
    public Settlement at(long center) { return settlements.get(center); }
    Settlement discover(long seed, BlockPos center, int population, int radius) {
        return settlements.computeIfAbsent(center.asLong(), key -> {
            setDirty(); return new Settlement(key, UUID.nameUUIDFromBytes(("living_settlements:" + seed + ":" + key).getBytes(java.nio.charset.StandardCharsets.UTF_8)), population, radius);
        });
    }
    public boolean ownsNpcColony(int colonyId, UUID owner) {
        return settlements.values().stream().anyMatch(s -> s.colonyId == colonyId && s.owner.equals(owner) && !s.failed && s.complete);
    }
    private CompoundTag write() {
        var root = new CompoundTag(); root.putInt("version", 1); var list = new ListTag();
        for (var s : settlements.values()) {
            var t = new CompoundTag(); t.putLong("center", s.center); t.putString("owner", s.owner.toString());
            t.putInt("population", s.population); t.putInt("radius", s.radius); t.putInt("colony", s.colonyId);
            t.putString("style", s.style); t.putString("status", s.status); t.putInt("next", s.nextBuilding);
            t.putBoolean("supplies", s.suppliesGranted); t.putBoolean("complete", s.complete); t.putBoolean("failed", s.failed);
            t.putLong("villageChunk", s.villageChunk); t.putBoolean("fallback", s.fallback); t.putInt("fallbackCursor", s.fallbackCursor);
            t.putLong("developmentTick", s.lastDevelopmentTick); t.putInt("developmentCredit", s.developmentCredit);
            t.putInt("observedPopulation", s.observedPopulation); t.putInt("observedFood", s.observedFood); t.putInt("observedWork", s.observedWork);
            t.putInt("editMinX",s.editMinX); t.putInt("editMaxX",s.editMaxX); t.putInt("editMinZ",s.editMinZ); t.putInt("editMaxZ",s.editMaxZ);
            t.putBoolean("townHallOnly",s.townHallOnly);t.putInt("ground",s.groundY);t.putBoolean("adaptive",s.adaptive);t.putInt("supportCursor",s.supportCursor);
            t.putBoolean("unrestricted",s.unrestricted);
            var vetoes=new ListTag();for(long anchor:s.blockedPlots){var v=new CompoundTag();v.putLong("anchor",anchor);vetoes.add(v);}t.put("blockedPlots",vetoes);
            var access=new ListTag();s.accesses.forEach((anchor,points)->{var a=new CompoundTag();a.putLong("anchor",anchor);var positions=new ListTag();for(long pos:points){var p=new CompoundTag();p.putLong("pos",pos);positions.add(p);}a.put("points",positions);access.add(a);});t.put("accesses",access);
            var plans = new ListTag(); s.plans.forEach(p -> plans.add(p.write())); t.put("plans", plans); list.add(t);
        }
        root.put("settlements", list); return root;
    }
    private static NaturalSettlementsData read(CompoundTag root) {
        if (root.getIntOr("version", 0) != 1) { throw new IllegalArgumentException("Unsupported schema"); }
        var data = new NaturalSettlementsData(); Set<UUID> owners = new HashSet<>();
        for (var tag : root.getListOrEmpty("settlements")) {
            if (!(tag instanceof CompoundTag t)) { throw new IllegalArgumentException("Invalid settlement entry"); }
            var s = new Settlement(t.getLongOr("center", 0), UUID.fromString(t.getStringOr("owner", "")), t.getIntOr("population", 0), t.getIntOr("radius", 0));
            if (data.settlements.containsKey(s.center) || !owners.add(s.owner)) { throw new IllegalArgumentException("Duplicate reservation/owner"); }
            s.colonyId = t.getIntOr("colony", 0); s.style = t.getStringOr("style", ""); s.status = t.getStringOr("status", "");
            s.nextBuilding = t.getIntOr("next", 0); s.suppliesGranted = t.getBooleanOr("supplies", false);
            s.complete = t.getBooleanOr("complete", false); s.failed = t.getBooleanOr("failed", false);
            s.villageChunk = t.getLongOr("villageChunk", Long.MIN_VALUE); s.fallback = t.getBooleanOr("fallback", false);
            s.townHallOnly=t.getBooleanOr("townHallOnly",false);s.groundY=t.getIntOr("ground",0);s.adaptive=t.getBooleanOr("adaptive",false);
            s.unrestricted=t.getBooleanOr("unrestricted",false);
            s.supportCursor=t.getIntOr("supportCursor",0);if(s.supportCursor<0||s.supportCursor>1024)throw new IllegalArgumentException("Invalid founding support cursor");
            for(var veto:t.getListOrEmpty("blockedPlots"))if(veto instanceof CompoundTag v)s.blockedPlots.add(v.getLongOr("anchor",0));
            for(var item:t.getListOrEmpty("accesses"))if(item instanceof CompoundTag a){var points=new ArrayList<Long>();for(var point:a.getListOrEmpty("points"))if(point instanceof CompoundTag p)points.add(p.getLongOr("pos",0));if(points.size()>32)throw new IllegalArgumentException("Too many access points");s.accesses.put(a.getLongOr("anchor",0),List.copyOf(points));}
            if(s.blockedPlots.size()>96||s.accesses.size()>96)throw new IllegalArgumentException("Too many plot controls");
            s.fallbackCursor = t.getIntOr("fallbackCursor", 0); s.lastDevelopmentTick = t.getLongOr("developmentTick", -1);
            s.developmentCredit = t.getIntOr("developmentCredit", 0); s.observedPopulation = t.getIntOr("observedPopulation", 0);
            s.observedFood = t.getIntOr("observedFood", 0); s.observedWork = t.getIntOr("observedWork", 0);
            s.editMinX=t.getIntOr("editMinX",s.editMinX); s.editMaxX=t.getIntOr("editMaxX",s.editMaxX);
            s.editMinZ=t.getIntOr("editMinZ",s.editMinZ); s.editMaxZ=t.getIntOr("editMaxZ",s.editMaxZ);
            var center=BlockPos.of(s.center);
            if(s.editMinX>s.editMaxX || s.editMinZ>s.editMaxZ || Math.abs((long)s.editMinX-center.getX())>512
                || Math.abs((long)s.editMaxX-center.getX())>512 || Math.abs((long)s.editMinZ-center.getZ())>512
                || Math.abs((long)s.editMaxZ-center.getZ())>512) throw new IllegalArgumentException("Invalid reservation protection bounds");
            if (s.fallbackCursor < 0 || s.fallbackCursor > 100000 || s.developmentCredit < 0 || s.developmentCredit > 8
                || s.observedPopulation < 0 || s.observedFood < 0 || s.observedWork < 0) throw new IllegalArgumentException("Invalid development/fallback state");
            for (var plan : t.getListOrEmpty("plans")) {
                if (!(plan instanceof CompoundTag p)) { throw new IllegalArgumentException("Invalid plan"); }
                s.plans.add(BuildingPlan.read(p));
            }
            if (s.nextBuilding < 0 || s.nextBuilding > s.plans.size() || s.plans.size() > 96 || s.colonyId < 0) { throw new IllegalArgumentException("Invalid placement cursor"); }
            data.settlements.put(s.center, s);
        }
        return data;
    }
}
