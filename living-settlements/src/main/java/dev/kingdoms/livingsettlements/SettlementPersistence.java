package dev.kingdoms.livingsettlements;

import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelResource;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Native SavedData logs IO failures instead of failing its future. Verify the actual journal before mutation. */
final class SettlementPersistence {
    record Checkpoint(long center, UUID owner, int plans, int building, int fallback, boolean supplies) {}
    static CompletableFuture<?> save(ServerLevel level,NaturalSettlementsData data,NaturalSettlementsData.Settlement s) {
        var expected=new Checkpoint(s.center,s.owner,s.plans.size(),s.nextBuilding,s.fallbackCursor,s.suppliesGranted);
        var path=DimensionType.getStorageFolder(level.dimension(),level.getServer().getWorldPath(LevelResource.ROOT))
            .resolve("data/living_settlements/natural_settlements.dat");
        data.setDirty();
        return level.getDataStorage().scheduleSave().thenRunAsync(()->{
            try {
                byte[] bytes=Files.readAllBytes(path);
                var root=NbtIo.readCompressed(new ByteArrayInputStream(bytes),NbtAccounter.create(64L*1024*1024));
                if(!contains(root.getCompoundOrEmpty("data"),expected)) throw new IllegalStateException("Settlement journal did not persist the required checkpoint");
            } catch(java.io.IOException ex) { throw new java.util.concurrent.CompletionException(ex); }
        });
    }
    static boolean contains(CompoundTag data,Checkpoint expected) {
        for(var element:data.getListOrEmpty("settlements")) if(element instanceof CompoundTag entry
            && entry.getLongOr("center",Long.MIN_VALUE)==expected.center()
            && entry.getStringOr("owner","").equals(expected.owner().toString())) {
            return entry.getListOrEmpty("plans").size()>=expected.plans()
                && entry.getIntOr("next",0)>=expected.building()
                && entry.getIntOr("fallbackCursor",0)>=expected.fallback()
                && (!expected.supplies() || entry.getBooleanOr("supplies",false));
        }
        return false;
    }
}
