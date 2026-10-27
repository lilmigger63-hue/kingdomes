package dev.kingdoms.livingsettlements;

import com.minecolonies.api.colony.IColonyManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Restore retained native pieces only before founding, only on loaded, untouched, unclaimed land. */
final class VillageFallback {
    private static final Map<Long, Job> JOBS = new HashMap<>();
    private static final class Job { int checked, failures; boolean validated; CompletableFuture<?> save; int pending = -1; }
    static void clear() { JOBS.clear(); }
    static void cancel(long center) { JOBS.remove(center); }
    static void tick(ServerLevel level, NaturalSettlementsData data, NaturalSettlementsData.Settlement s) {
        var marker = VillageReplacement.marker(level, s);
        if (marker == null || marker.vanilla.isEmpty() || s.colonyId != 0) { stop(data,s,"Native fallback unavailable"); return; }
        var box = marker.vanilla.calculateBoundingBox();
        for (int x = (box.minX()-16)>>4; x <= (box.maxX()+16)>>4; x++) for (int z = (box.minZ()-16)>>4; z <= (box.maxZ()+16)>>4; z++) {
            var chunk = level.getChunkSource().getChunkNow(x,z);
            if (chunk == null) { s.status="Waiting for loaded native village fallback footprint"; return; }
            var claim = IColonyManager.getInstance().getClaimData(level.dimension(),new ChunkPos(x,z));
            if ((claim != null && claim.getOwningColony()!=0) || !VillageReplacement.structuresClear(level,chunk,s)) {
                stop(data,s,"Native village fallback blocked by a claim/structure; no terrain overwritten"); return;
            }
        }
        var job = JOBS.computeIfAbsent(s.center, ignored -> new Job());
        int sx=box.maxX()-box.minX()+1, sz=box.maxZ()-box.minZ()+1, sy=box.maxY()-box.minY()+1;
        long volume=(long)sx*sz*sy;
        if (volume>8_000_000 || marker.vanilla.pieces().stream().anyMatch(p -> (long)p.getBoundingBox().getXSpan()*p.getBoundingBox().getYSpan()*p.getBoundingBox().getZSpan()>50000)) {
            stop(data,s,"Oversized native fallback retained without placing"); return;
        }
        // Before the first fallback mutation, reject edited/occupied blocks incrementally.
        if (s.fallbackCursor==0 && !job.validated) {
            for (int budget=NaturalSettlementConfig.SCAN_BUDGET.get()*8; budget>0 && job.checked<volume; budget--) {
                int i=job.checked++; var pos=new BlockPos(box.minX()+i%sx,box.minY()+i/(sx*sz),box.minZ()+(i/sx)%sz);
                if (!level.getWorldBorder().isWithinBounds(pos) || level.getBlockEntity(pos)!=null || !NaturalSettlementPlanner.natural(level.getBlockState(pos))) {
                    stop(data,s,"Native village fallback blocked by edited/protected terrain"); return;
                }
            }
            if (job.checked<volume) { s.status="Checking untouched native fallback terrain"; return; }
            job.validated=true;
        }
        int width=(box.maxX()>>4)-(box.minX()>>4)+1, depth=(box.maxZ()>>4)-(box.minZ()>>4)+1;
        int count=marker.vanilla.pieces().size(), total=width*depth*count;
        if (job.save!=null) {
            if (!job.save.isDone()) return;
            try { job.save.join(); }
            catch(RuntimeException failure) {
                s.fallbackCursor=job.pending; job.save=null; job.pending=-1; data.setDirty();
                if(++job.failures>=3) stop(data,s,"Native fallback stopped: journal could not be durably verified");
                return;
            }
            job.save=null; job.failures=0;
            int segment=job.pending, tile=segment/count;
            int x=(box.minX()>>4)+tile%width, z=(box.minZ()>>4)+tile/width;
            var bounds=new BoundingBox(x*16,level.getMinY(),z*16,x*16+15,level.getMaxY()-1,z*16+15);
            var first=marker.vanilla.pieces().getFirst().getBoundingBox(); var center=first.getCenter();
            marker.vanilla.pieces().get(segment%count).postProcess(level,level.structureManager(),level.getChunkSource().getGenerator(),
                RandomSource.create(level.getSeed() ^ s.center ^ segment),bounds,new ChunkPos(x,z),new BlockPos(center.getX(),first.minY(),center.getZ()));
            level.getChunkSource().getChunkNow(x,z).markUnsaved(); job.pending=-1;
            return;
        }
        for (int attempts=0; attempts<32 && s.fallbackCursor<total; attempts++) {
            int segment=s.fallbackCursor++, tile=segment/count;
            int x=(box.minX()>>4)+tile%width, z=(box.minZ()>>4)+tile/width;
            var bounds=new BoundingBox(x*16,level.getMinY(),z*16,x*16+15,level.getMaxY()-1,z*16+15);
            data.setDirty();
            if (!marker.vanilla.pieces().get(segment%count).getBoundingBox().intersects(bounds)) continue;
            // At-most-once journal: never replay native entity spawning after a restart.
            job.pending=segment; job.save=SettlementPersistence.save(level,data,s); s.status="Restoring retained native village"; return;
        }
        if (s.fallbackCursor>=total) stop(data,s,"Original vanilla village fallback completed");
    }
    private static void stop(NaturalSettlementsData data,NaturalSettlementsData.Settlement s,String status) {
        s.fallback=false; s.failed=true; s.status=status; data.setDirty(); JOBS.remove(s.center);
    }
}
