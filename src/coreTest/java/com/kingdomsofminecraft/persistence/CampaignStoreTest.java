package com.kingdomsofminecraft.persistence;

import com.kingdomsofminecraft.campaign.CampaignState;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;

/** Dependency-free contract tests; run by Gradle check and scripts/test-core.sh. */
public final class CampaignStoreTest {
    private static int passed;
    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("kingdoms-test-");
        try {
            Path save = dir.resolve("world/campaign.dat");
            CampaignStore store = new CampaignStore(save);
            check(store.loadOrCreate(42).equals(new CampaignState(42, 0)), "new campaign");
            check(!Files.exists(save), "loading missing state must not write");
            store.save(new CampaignState(42, 700));
            check(new CampaignStore(save).loadOrCreate(99).equals(new CampaignState(42, 700)), "save/reload preserves seed and time");
            store.save(new CampaignState(42, 800));
            check(store.loadOrCreate(99).simulationStep() == 800, "replacement persists newest state");
            byte[] valid = Files.readAllBytes(save);
            Files.write(save, Arrays.copyOf(valid, 6));
            rejects(() -> store.loadOrCreate(42), IOException.class, "truncated save");
            check(Files.size(save) == 6, "invalid save must not be overwritten");
            byte[] future = valid.clone();
            ByteBuffer.wrap(future).putInt(4, 999);
            Files.write(save, future);
            rejects(() -> store.loadOrCreate(42), IOException.class, "future schema");
            byte[] badHeader = valid.clone(); badHeader[0] ^= 1;
            Files.write(save, badHeader);
            rejects(() -> store.loadOrCreate(42), IOException.class, "invalid header");
            byte[] negative = valid.clone();
            ByteBuffer.wrap(negative).putLong(16, -1);
            java.util.zip.CRC32 crc = new java.util.zip.CRC32(); crc.update(negative, 0, 24);
            ByteBuffer.wrap(negative).putLong(24, crc.getValue());
            Files.write(save, negative);
            rejects(() -> store.loadOrCreate(42), IOException.class, "negative persisted time");
            byte[] corrupt = valid.clone(); corrupt[15] ^= 1;
            Files.write(save, corrupt);
            rejects(() -> store.loadOrCreate(42), IOException.class, "checksum mismatch");
            Files.write(save, Arrays.copyOf(valid, valid.length + 1));
            rejects(() -> store.loadOrCreate(42), IOException.class, "trailing bytes");
            rejects(() -> new CampaignState(42, -1), IllegalArgumentException.class, "negative time");
            check(new CampaignState(42, 20).advance().equals(new CampaignState(42, 21)), "deterministic advance");
            rejects(() -> new CampaignState(42, Long.MAX_VALUE).advance(), ArithmeticException.class, "overflow");
            Path other = dir.resolve("other/campaign.dat");
            check(new CampaignStore(other).loadOrCreate(100).equals(new CampaignState(100, 0)), "world isolation");
            Path blocked = dir.resolve("blocked"); Files.createDirectory(blocked);
            Files.writeString(blocked.resolve("keep"), "prior data");
            rejects(() -> new CampaignStore(blocked).save(new CampaignState(42, 1)), IOException.class, "failed replacement");
            check(Files.readString(blocked.resolve("keep")).equals("prior data"), "failed write preserves destination");
            try (var paths = Files.list(dir)) {
                check(paths.noneMatch(p -> p.getFileName().toString().endsWith(".tmp")), "failed write cleans temporary files");
            }
            System.out.println("PASS: " + passed + " campaign persistence checks");
        } finally {
            try (var paths = Files.walk(dir)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }
    private static void check(boolean value, String label) {
        if (!value) throw new AssertionError(label);
        passed++;
    }
    private static void rejects(Action action, Class<? extends Throwable> type, String label) throws Exception {
        try { action.run(); } catch (Throwable failure) {
            if (!type.isInstance(failure)) throw new AssertionError(label, failure);
            passed++; return;
        }
        throw new AssertionError("Expected rejection: " + label);
    }
    private interface Action { void run() throws Exception; }
}
