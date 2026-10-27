package dev.kingdoms.livingsettlements;

import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.ldtteam.structurize.storage.StructurePacks;
import com.minecolonies.api.blocks.ModBlocks;
import com.minecolonies.core.blocks.BlockScarecrow;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class NaturalBlueprints {
    record Style(String name, Map<String, String> paths) {}
    static final Map<String, String> FILES = Map.of("townhall", "townhall1.blueprint", "builder", "builder1.blueprint",
        "home", "residence2.blueprint", "warehouse", "warehouse1.blueprint", "courier", "deliveryman1.blueprint",
        "lumberjack", "lumberjack1.blueprint", "miner", "miner1.blueprint", "farmer", "farmer1.blueprint", "cook", "cook1.blueprint");
    static final Map<String,String> OPTIONAL=Map.of("guard","guardtower1.blueprint","university","university1.blueprint","sawmill","sawmill1.blueprint","blacksmith","blacksmith1.blueprint","smeltery","smeltery1.blueprint","stonemason","stonemason1.blueprint");
    static CompletableFuture<List<Style>> catalog(List<String> allowed) {
        return CompletableFuture.supplyAsync(() -> {
            if (!StructurePacks.waitUntilFinishedLoading()) { return List.of(); }
            var result = new ArrayList<Style>();
            for (var meta : List.copyOf(StructurePacks.getPackMetas())) {
                if (meta.isDisabled() || (!allowed.isEmpty() && !allowed.contains(meta.getName()))) { continue; }
                Map<String, String> found = new HashMap<>();
                try (var files = Files.walk(meta.getPath())) {
                    files.filter(Files::isRegularFile).sorted().forEach(path -> FILES.forEach((role, name) -> {
                        if (path.getFileName().toString().equals(name)) {
                            found.putIfAbsent(role, meta.getSubPath(path).replace('\\', '/'));
                        }
                    }));
                    try(var extra=Files.walk(meta.getPath())) {extra.filter(Files::isRegularFile).sorted().forEach(path->OPTIONAL.forEach((role,name)->{
                        if(path.getFileName().toString().equals(name))found.putIfAbsent(role,meta.getSubPath(path).replace('\\','/'));
                    }));}
                } catch (java.io.IOException ex) { continue; }
                if (found.keySet().containsAll(FILES.keySet())) { result.add(new Style(meta.getName(), Map.copyOf(found))); }
            }
            result.sort(Comparator.comparing(Style::name)); return List.copyOf(result);
        });
    }
    static List<String> roles(int population) {
        var result = new ArrayList<>(List.of("townhall", "builder", "warehouse", "courier", "lumberjack", "miner", "farmer", "cook"));
        for (int i = 0; i < (population + 1) / 2; i++) { result.add("home"); }
        result.add("field"); return result;
    }
    static List<String> foundingRoles(int population) {
        // Native level-one residences house one citizen: reserve enough genuine level-one homes for founding and one arrival.
        var result=new ArrayList<>(roles(population));result.removeIf(r->r.equals("home")||r.equals("field"));
        for(int i=0;i<population+1;i++)result.add("home");result.add("field");return result;
    }
    static CompletableFuture<Blueprint> load(ServerLevel level, String style, String path, String role) {
        if (role.equals("field")) { return CompletableFuture.completedFuture(field(level, style)); }
        return StructurePacks.getBlueprintFuture(style, path, level.registryAccess());
    }
    public static Blueprint orient(ServerLevel level,Blueprint original,int rotation) {
        if(rotation==0)return original;
        var copy=com.ldtteam.structurize.blueprints.v1.BlueprintUtil.readBlueprintFromNBT(
            com.ldtteam.structurize.blueprints.v1.BlueprintUtil.writeBlueprintToNBT(original).copy(),level.registryAccess());
        copy.setMissingMods();copy.setPackName(original.getPackName());
        copy.setFileName(original.getFileName());copy.setFilePath(original.getFilePath());
        copy.setRotationMirrorRelative(com.ldtteam.structurize.api.RotationMirror.NOT_MIRRORED[rotation],level);
        return copy;
    }
    private static Blueprint field(ServerLevel level, String style) {
        var bp = new Blueprint((short) 9, (short) 3, (short) 9, level.registryAccess());
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) {
                boolean water = x == 4 && z == 5;
                bp.addBlockState(new BlockPos(x, 0, z), water ? Blocks.WATER.defaultBlockState()
                    : Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, 7));
                if (!water && !(x == 4 && z == 4)) { bp.addBlockState(new BlockPos(x, 1, z), Blocks.CARROTS.defaultBlockState()); }
            }
        }
        bp.addBlockState(new BlockPos(4, 1, 4), ModBlocks.blockScarecrow.defaultBlockState().setValue(BlockScarecrow.HALF, DoubleBlockHalf.LOWER));
        bp.addBlockState(new BlockPos(4, 2, 4), ModBlocks.blockScarecrow.defaultBlockState().setValue(BlockScarecrow.HALF, DoubleBlockHalf.UPPER));
        bp.setCachePrimaryOffset(new BlockPos(4, 1, 4));
        bp.setMissingMods(); // Native computed missing-mod metadata only exists for blueprints read from disk.
        bp.setPackName(style).setName("Natural starter wheat plot").setFileName("natural_field").setFilePath(StructurePacks.getStructurePack(style).getPath());
        return bp;
    }
}
