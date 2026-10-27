package dev.kingdoms.livingsettlements;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import java.util.List;
import java.util.Set;

public final class VillageReplacement {
    private static final Set<String> VILLAGES = Set.of("village_plains", "village_desert", "village_savanna", "village_snowy", "village_taiga");
    private VillageReplacement() {}
    public static boolean eligible(Identifier id) { return id != null && id.getNamespace().equals("minecraft") && VILLAGES.contains(id.getPath()); }
    public static StructureStart replace(StructureStart original, Structure.GenerationContext context, Identifier id, String dimension) {
        if (!original.isValid() || !eligible(id) || !dimension.equals("minecraft:overworld")
                || !NaturalSettlementConfig.REPLACE_VILLAGES.get() || !NaturalSettlementConfig.ENABLED.get()
                || !VillageSelection.selected(context.seed(), dimension, context.chunkPos().x(), context.chunkPos().z(), NaturalSettlementConfig.REPLACEMENT_CHANCE.get()*Math.min(1,NaturalSettlementConfig.DENSITY.get()))) return original;
        int x = context.chunkPos().getMiddleBlockX(), z = context.chunkPos().getMiddleBlockZ();
        int radius = NaturalSettlementConfig.RADIUS.get();
        int ground = context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
        // A marker changes no terrain. Loaded footprint planning finds a safe site;
        // the original village remains saved for fallback when no site is possible.
        var marker = new VillageMarkerPiece(new BlockPos(x, ground+1, z), new PiecesContainer(original.getPieces()),
            Math.min(NaturalSettlementConfig.POPULATION.get(),NaturalSettlementConfig.MAX_POPULATION.get()), radius);
        return new StructureStart(original.getStructure(), original.getChunkPos(), original.getReferences(), new PiecesContainer(List.of(marker)));
    }
    static VillageMarkerPiece marker(StructureStart start) {
        return start != null && start.getPieces().size() == 1 && start.getPieces().getFirst() instanceof VillageMarkerPiece marker ? marker : null;
    }
    static VillageMarkerPiece marker(ServerLevel level, NaturalSettlementsData.Settlement settlement) {
        if (settlement.villageChunk == Long.MIN_VALUE) return null;
        ChunkPos origin = ChunkPos.unpack(settlement.villageChunk);
        var chunk = level.getChunkSource().getChunkNow(origin.x(), origin.z());
        if (chunk == null) return null;
        for (var start : chunk.getAllStarts().values()) {
            var marker = marker(start);
            if (marker != null && marker.center.getX() == BlockPos.of(settlement.center).getX()
                    && marker.center.getZ() == BlockPos.of(settlement.center).getZ()) return marker;
        }
        return null;
    }
    static boolean structuresClear(ServerLevel level, LevelChunk chunk, NaturalSettlementsData.Settlement settlement) {
        var own = marker(level, settlement);
        for (var start : chunk.getAllStarts().values()) if (start.isValid() && (own == null || marker(start) != own)) return false;
        for (var entry : chunk.getAllReferences().entrySet()) {
            for (long origin : entry.getValue()) {
                if (own == null || origin != settlement.villageChunk) return false;
                var originChunk = level.getChunkSource().getChunkNow(ChunkPos.unpack(origin).x(), ChunkPos.unpack(origin).z());
                if (originChunk == null || marker(originChunk.getAllStarts().get(entry.getKey())) != own) return false;
            }
        }
        return true;
    }
}

