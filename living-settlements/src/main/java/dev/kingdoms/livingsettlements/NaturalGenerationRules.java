package dev.kingdoms.livingsettlements;

import java.util.Optional;

/** Deterministic discovery and inclusive footprint geometry, independent of world access. */
public final class NaturalGenerationRules {
    public record Candidate(int chunkX, int chunkZ) {}
    public record Box(int minX, int minZ, int maxX, int maxZ) {
        public boolean overlaps(Box other, int gap) {
            return (long) minX <= (long) other.maxX + gap && (long) maxX + gap >= other.minX
                && (long) minZ <= (long) other.maxZ + gap && (long) maxZ + gap >= other.minZ;
        }
    }
    private NaturalGenerationRules() {}
    public static boolean independentCandidate(long seed,int chunkX,int chunkZ,double multiplier,boolean villageBaseline) {
        if(!Double.isFinite(multiplier)||multiplier<0||multiplier>20)throw new IllegalArgumentException("Frequency multiplier must be 0..20");
        double opportunities=Math.max(0,multiplier-(villageBaseline?1:0));
        return VillageSelection.selected(seed^0x496e646570656e64L,"living_settlements:independent",chunkX,chunkZ,opportunities/2048.0);
    }
    public static Optional<Candidate> candidate(long seed, int cellX, int cellZ, int spacingChunks, double chance) {
        if (spacingChunks < 1 || !Double.isFinite(chance) || chance < 0 || chance > 1) {
            throw new IllegalArgumentException("Invalid natural settlement frequency or spacing");
        }
        var random = new java.util.Random(seed ^ ((long) cellX * 341873128712L) ^ ((long) cellZ * 132897987541L));
        if (random.nextDouble() >= chance) { return Optional.empty(); }
        return Optional.of(new Candidate(Math.addExact(Math.multiplyExact(cellX, spacingChunks), random.nextInt(spacingChunks)),
            Math.addExact(Math.multiplyExact(cellZ, spacingChunks), random.nextInt(spacingChunks))));
    }
    public static boolean separated(int x, int z, int otherX, int otherZ, int distance) {
        double dx = (double) x - otherX;
        double dz = (double) z - otherZ;
        return dx * dx + dz * dz >= (double) distance * distance;
    }
    public static boolean sameTerrainBand(int townHallGround, int plotGround) {
        return Math.abs((long) townHallGround - plotGround) <= 4;
    }
    public static int regionSizeChunks(int minimumDistance, int maximumDistance) {
        if (minimumDistance < 1 || (long) maximumDistance - minimumDistance < 64) {
            throw new IllegalArgumentException("Maximum settlement distance must be at least 64 blocks above minimum");
        }
        return Math.toIntExact(((long) minimumDistance + maximumDistance + 31) / 32);
    }
    public static Optional<Candidate> candidateInRange(long seed, int cellX, int cellZ,
            int minimumDistance, int maximumDistance, double chance) {
        int span = regionSizeChunks(minimumDistance, maximumDistance);
        int jitter = (maximumDistance - minimumDistance) / 128;
        int width = jitter * 2 + 1;
        return candidate(seed, cellX, cellZ, span, chance).map(raw -> {
            int originX = Math.multiplyExact(cellX, span), originZ = Math.multiplyExact(cellZ, span);
            return new Candidate(Math.addExact(originX, span / 2 + Math.floorMod(raw.chunkX() - originX, width) - jitter),
                Math.addExact(originZ, span / 2 + Math.floorMod(raw.chunkZ() - originZ, width) - jitter));
        });
    }
}
