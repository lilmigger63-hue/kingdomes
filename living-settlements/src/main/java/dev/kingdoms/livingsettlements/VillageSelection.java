package dev.kingdoms.livingsettlements;

/** Stable selection independent of threads, load order and mutable random sources. */
public final class VillageSelection {
    private VillageSelection() {}
    public static boolean selected(long seed, String dimension, int chunkX, int chunkZ, double chance) {
        if (!Double.isFinite(chance) || chance < 0 || chance > 1) throw new IllegalArgumentException("Chance must be 0..1");
        long value = mix(seed ^ 0x4c6976696e675633L);
        for (int i = 0; i < dimension.length(); i++) value = mix(value ^ dimension.charAt(i));
        value = mix(value ^ ((long) chunkX << 32) ^ Integer.toUnsignedLong(chunkZ));
        return (value >>> 11) * 0x1.0p-53 < chance;
    }
    private static long mix(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }
}
