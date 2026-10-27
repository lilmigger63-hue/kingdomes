package dev.kingdoms.livingsettlements;

import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelResource;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;

final class MayorPersistence {
    static CompletableFuture<?> save(ServerLevel level,MayorSavedData data,MayorSavedData.Mayor m) {
        data.changed(m);long revision=m.revision;
        var path=DimensionType.getStorageFolder(level.dimension(),level.getServer().getWorldPath(LevelResource.ROOT)).resolve("data/living_settlements/mayors.dat");
        return level.getDataStorage().scheduleSave().thenRunAsync(()->{
            try {
                var root=NbtIo.readCompressed(new ByteArrayInputStream(Files.readAllBytes(path)),NbtAccounter.create(64L*1024*1024)).getCompoundOrEmpty("data");
                for(var value:root.getListOrEmpty("mayors"))if(value instanceof CompoundTag t&&t.getIntOr("colony",0)==m.colonyId
                    &&t.getStringOr("owner","").equals(m.owner.toString())&&t.getLongOr("revision",0)>=revision)return;
                throw new IllegalStateException("Mayor checkpoint was not durably saved");
            }catch(java.io.IOException ex){throw new java.util.concurrent.CompletionException(ex);}
        });
    }
}
