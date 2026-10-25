package com.kingdomsofminecraft.persistence;

import com.kingdomsofminecraft.campaign.CampaignState;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.zip.CRC32;

/** Versioned, checksummed storage. Unsupported or corrupt saves are never reset silently.
 * Call on the owning server thread. Atomic replacement is required; no unsafe fallback.
 */
public final class CampaignStore {
    private static final int MAGIC = 0x4b4f4d43;
    private static final int SCHEMA = 1;
    private static final int PAYLOAD_BYTES = 24;
    private static final int FILE_BYTES = 32;
    private final Path file;

    public CampaignStore(Path file) {
        this.file = Objects.requireNonNull(file).toAbsolutePath();
    }

    public CampaignState loadOrCreate(long seed) throws IOException {
        byte[] data;
        try (var input = Files.newInputStream(file)) {
            data = input.readNBytes(FILE_BYTES + 1);
        } catch (NoSuchFileException missing) {
            return new CampaignState(seed, 0);
        }
        if (data.length != FILE_BYTES) throw new IOException("Invalid campaign save length: " + file);
        ByteBuffer buffer = ByteBuffer.wrap(data);
        if (buffer.getInt() != MAGIC) throw new IOException("Invalid campaign save header: " + file);
        int schema = buffer.getInt();
        // Schema 1 is the first format. Future versions must introduce explicit migrations.
        if (schema != SCHEMA) throw new IOException("Unsupported campaign schema " + schema + ": " + file);
        CRC32 checksum = new CRC32();
        checksum.update(data, 0, PAYLOAD_BYTES);
        if (buffer.getLong(PAYLOAD_BYTES) != checksum.getValue()) throw new IOException("Campaign checksum mismatch: " + file);
        long worldSeed = buffer.getLong();
        long step = buffer.getLong();
        if (step < 0) throw new IOException("Invalid campaign time: " + file);
        return new CampaignState(worldSeed, step);
    }

    public void save(CampaignState state) throws IOException {
        Objects.requireNonNull(state);
        byte[] data = new byte[FILE_BYTES];
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.putInt(MAGIC).putInt(SCHEMA).putLong(state.worldSeed()).putLong(state.simulationStep());
        CRC32 checksum = new CRC32();
        checksum.update(data, 0, PAYLOAD_BYTES);
        buffer.putLong(checksum.getValue());
        Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(), file.getFileName().toString(), ".tmp");
        try {
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                buffer.flip();
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
