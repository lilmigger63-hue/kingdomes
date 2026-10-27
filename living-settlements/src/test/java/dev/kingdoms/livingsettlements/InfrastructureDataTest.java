package dev.kingdoms.livingsettlements;
import org.junit.jupiter.api.Test;
import net.minecraft.nbt.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
class InfrastructureDataTest {
    @Test void queuedCompletedAndBlockedNativeTasksRoundTripWithoutNewGrants() {
        var data=new InfrastructureData();
        var task=new InfrastructureData.Task("c1_road_123",1,UUID.randomUUID(),123,new CompoundTag());task.order=17;task.blocked=true;data.tasks.put(task.key,task);
        var encoded=InfrastructureData.CODEC.encodeStart(NbtOps.INSTANCE,data).getOrThrow();
        var loaded=InfrastructureData.CODEC.parse(NbtOps.INSTANCE,encoded).getOrThrow();
        assertEquals(1,loaded.tasks.size());assertEquals(17,loaded.tasks.get(task.key).order);assertTrue(loaded.tasks.get(task.key).blocked);
        assertFalse(loaded.tasks.get(task.key).done);
    }
    @Test void unsafePathsAndDuplicateTaskIdsAreRejected() {
        var root=new CompoundTag();root.putInt("version",1);var list=new ListTag();var task=new CompoundTag();task.putString("key","../escape");task.putInt("colony",1);task.putString("owner",UUID.randomUUID().toString());list.add(task);root.put("tasks",list);
        assertTrue(InfrastructureData.CODEC.parse(NbtOps.INSTANCE,root).error().isPresent());
    }
}
