package dev.kingdoms.livingsettlements;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.concurrent.CompletableFuture;
import static org.junit.jupiter.api.Assertions.*;
class InfrastructureFilesTest {
    @TempDir Path folder;
    @Test void simultaneousColonyWritesWaitForOneCompleteWorldPackManifest() {
        var operations=new java.util.ArrayList<CompletableFuture<?>>();
        for(int i=0;i<100;i++){int index=i;operations.add(InfrastructureService.preparePack(folder).thenRunAsync(()->{try{
            var json=com.google.gson.JsonParser.parseString(Files.readString(folder.resolve("pack.json"))).getAsJsonObject();
            assertEquals("Living Settlements Infrastructure",json.get("name").getAsString());Files.writeString(folder.resolve(index+".blueprint"),"fixture");
        }catch(java.io.IOException ex){throw new java.io.UncheckedIOException(ex);}}));}
        CompletableFuture.allOf(operations.toArray(CompletableFuture[]::new)).join();
        assertEquals(100,operations.size());
    }
    @Test void independentWorldRootsRemainIndependent() {
        var a=folder.resolve("world-a");var b=folder.resolve("world-b");
        InfrastructureService.preparePack(a).join();InfrastructureService.preparePack(b).join();
        assertTrue(Files.isRegularFile(a.resolve("pack.json")));assertTrue(Files.isRegularFile(b.resolve("pack.json")));
    }
}
