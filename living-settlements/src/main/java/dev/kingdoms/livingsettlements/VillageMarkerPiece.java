package dev.kingdoms.livingsettlements;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.*;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.bus.api.IEventBus;
import java.util.function.Supplier;

/** A saved generation decision, not a physical building. Original vanilla pieces allow safe fallback. */
public final class VillageMarkerPiece extends StructurePiece implements net.neoforged.neoforge.common.world.PieceBeardifierModifier {
    private static final DeferredRegister<StructurePieceType> TYPES = DeferredRegister.create(Registries.STRUCTURE_PIECE, LivingSettlements.MOD_ID);
    private static final Supplier<StructurePieceType> TYPE = TYPES.register("village_reservation", () -> VillageMarkerPiece::new);
    public final BlockPos center;
    public final PiecesContainer vanilla;
    public final int population, radius;
    public final boolean townHallOnly;
    public final boolean adaptive;
    public final boolean unrestricted;
    public static void register(IEventBus bus) { TYPES.register(bus); }
    public VillageMarkerPiece(BlockPos center, PiecesContainer vanilla, int population, int radius) {
        super(TYPE.get(), 0, new BoundingBox(center.getX()-radius, center.getY()-8, center.getZ()-radius,
            center.getX()+radius, center.getY()+96, center.getZ()+radius));
        this.center = center.immutable(); this.vanilla = vanilla; this.population = population; this.radius = radius;
        townHallOnly=NaturalSettlementConfig.TOWN_HALL_START.get();
        adaptive=townHallOnly&&NaturalSettlementConfig.ADAPTIVE.get();
        unrestricted=true;
        // Include the retained village so its references remain valid if native fallback is necessary.
        this.boundingBox = BoundingBox.encapsulatingBoxes(java.util.List.of(boundingBox, vanilla.calculateBoundingBox())).orElseThrow();
    }
    private VillageMarkerPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(TYPE.get(), tag);
        center = BlockPos.of(tag.getLongOr("settlementCenter", 0));
        population = tag.getIntOr("population", 6); radius = tag.getIntOr("radius", 112);
        townHallOnly=tag.getBooleanOr("townHallOnly",false);
        adaptive=tag.getBooleanOr("adaptive",false);
        unrestricted=tag.getBooleanOr("unrestricted",false);
        vanilla = PiecesContainer.load(tag.getListOrEmpty("vanilla"), context);
    }
    @Override protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putLong("settlementCenter", center.asLong()); tag.putInt("population", population); tag.putInt("radius", radius);
        tag.putBoolean("townHallOnly",townHallOnly);
        tag.putBoolean("adaptive",adaptive);
        tag.putBoolean("unrestricted",unrestricted);
        tag.put("vanilla", vanilla.save(context));
    }
    @Override public void postProcess(WorldGenLevel level, StructureManager manager, ChunkGenerator generator,
        RandomSource random, BoundingBox bounds, ChunkPos chunk, BlockPos reference) { /* Deferred bounded server job. */ }
    @Override public BoundingBox getBeardifierBox() { return boundingBox; }
    @Override public TerrainAdjustment getTerrainAdjustment() { return TerrainAdjustment.NONE; }
    @Override public int getGroundLevelDelta() { return 0; }
}
