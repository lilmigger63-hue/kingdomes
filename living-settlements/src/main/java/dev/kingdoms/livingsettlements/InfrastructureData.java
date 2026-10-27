package dev.kingdoms.livingsettlements;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.nbt.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.*;
import java.util.*;

/** Native decoration intents; physical progress lives in native worker/work-order saves. */
public final class InfrastructureData extends SavedData {
    static final class Task {
        final String key;final int colony;final UUID owner;final long origin;CompoundTag blueprint;
        int order;boolean done,blocked;
        int sizeX=1,sizeY=1,sizeZ=1;
        Task(String key,int colony,UUID owner,long origin,CompoundTag blueprint){this.key=key;this.colony=colony;this.owner=owner;this.origin=origin;this.blueprint=blueprint.copy();}
    }
    final Map<String,Task> tasks=new LinkedHashMap<>();
    public static final Codec<InfrastructureData> CODEC=CompoundTag.CODEC.flatXmap(tag->{
        try {
            if(tag.getIntOr("version",0)!=1)throw new IllegalArgumentException("Unsupported infrastructure schema");
            var data=new InfrastructureData();
            for(var entry:tag.getListOrEmpty("tasks")) {
                if(!(entry instanceof CompoundTag t))throw new IllegalArgumentException("Invalid task");
                String key=t.getStringOr("key","");int colony=t.getIntOr("colony",0);
                if(!key.matches("[a-z0-9_-]{1,100}")||colony<=0||data.tasks.containsKey(key)||data.tasks.size()>=131072)throw new IllegalArgumentException("Invalid/duplicate infrastructure identity");
                var task=new Task(key,colony,UUID.fromString(t.getStringOr("owner","")),t.getLongOr("origin",0),t.getCompoundOrEmpty("blueprint"));
                task.order=t.getIntOr("order",0);task.done=t.getBooleanOr("done",false);task.blocked=t.getBooleanOr("blocked",false);data.tasks.put(key,task);
                task.sizeX=t.getIntOr("sx",1);task.sizeY=t.getIntOr("sy",1);task.sizeZ=t.getIntOr("sz",1);
                if(task.sizeX<1||task.sizeY<1||task.sizeZ<1||task.sizeX>96||task.sizeY>96||task.sizeZ>96||(long)task.sizeX*task.sizeY*task.sizeZ>4096)throw new IllegalArgumentException("Invalid infrastructure volume");
            }
            return DataResult.success(data);
        }catch(RuntimeException ex){return DataResult.error(()->ex.getMessage());}
    },data->{
        var tag=new CompoundTag();tag.putInt("version",1);var entries=new ListTag();
        for(var task:data.tasks.values()) {
            var t=new CompoundTag();t.putString("key",task.key);t.putInt("colony",task.colony);t.putString("owner",task.owner.toString());t.putLong("origin",task.origin);t.put("blueprint",task.blueprint.copy());
            t.putInt("order",task.order);t.putBoolean("done",task.done);t.putBoolean("blocked",task.blocked);entries.add(t);
            t.putInt("sx",task.sizeX);t.putInt("sy",task.sizeY);t.putInt("sz",task.sizeZ);
        }
        tag.put("tasks",entries);return DataResult.success(tag);
    });
    static final SavedDataType<InfrastructureData> TYPE=new SavedDataType<>(Identifier.fromNamespaceAndPath(LivingSettlements.MOD_ID,"infrastructure"),InfrastructureData::new,CODEC);
    static InfrastructureData get(ServerLevel level){return level.getDataStorage().computeIfAbsent(TYPE);}
}
