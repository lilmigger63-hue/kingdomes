package com.kingdomsofminecraft.persistence;

import com.kingdomsofminecraft.campaign.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Objects;
import java.util.zip.CRC32;

/** Bounded, checksummed storage with explicit migration and atomic replacement.
 * Call on the owning server thread. Unknown/corrupt saves never reset silently.
 */
public final class CampaignStore {
    private static final int MAGIC = 0x4b4f4d43;
    private static final int SCHEMA = 3;
    private static final int MAX_FILE_BYTES = 65536;
    private final Path file;

    public CampaignStore(Path file) {
        this.file = Objects.requireNonNull(file).toAbsolutePath();
    }

    public CampaignState loadOrCreate(long seed) throws IOException {
        byte[] data;
        try (var input = Files.newInputStream(file)) {
            data = input.readNBytes(MAX_FILE_BYTES + 1);
        } catch (NoSuchFileException missing) {
            return new CampaignState(seed, 0);
        }
        if (data.length < 32 || data.length > MAX_FILE_BYTES) throw new IOException("Invalid campaign save length: " + file);
        int payloadSize = data.length - Long.BYTES;
        CRC32 checksum = new CRC32(); checksum.update(data, 0, payloadSize);
        if (ByteBuffer.wrap(data).getLong(payloadSize) != checksum.getValue()) throw new IOException("Campaign checksum mismatch: " + file);
        try (var input = new DataInputStream(new ByteArrayInputStream(data, 0, payloadSize))) {
            if (input.readInt() != MAGIC) throw new IOException("Invalid campaign save header: " + file);
            int schema = input.readInt();
            if (schema < 1 || schema > SCHEMA) throw new IOException("Unsupported campaign schema " + schema + ": " + file);
            long worldSeed = input.readLong(); long step = input.readLong();
            if (schema == 1) {
                if(input.available() != 0) throw new IOException("Invalid schema 1 save length");
                return new CampaignState(worldSeed, step);
            }
            int count = input.readInt();
            if (count < 0 || count > 20) throw new IOException("Invalid kingdom count");
            var kingdoms = new ArrayList<Kingdom>(count);
            for(int i=0;i<count;i++) {
                int id=input.readInt(); String name=input.readUTF();
                var clan=new Clan(input.readInt(), input.readUTF(), input.readUTF(), Culture.valueOf(input.readUTF()));
                var banner=new Banner(input.readInt(), input.readInt(), input.readInt());
                kingdoms.add(new Kingdom(id, name, clan, banner));
            }
            if (schema == 2) {
                if(input.available() != 0) throw new IOException("Trailing schema 2 data");
                return new CampaignState(worldSeed, step, kingdoms);
            }
            int clanCount = input.readInt();
            if (clanCount < 0 || clanCount > 64) throw new IOException("Invalid clan count");
            var allegiances = new ArrayList<ClanAllegiance>(clanCount);
            for (int i=0;i<clanCount;i++) {
                var clan=new Clan(input.readInt(), input.readUTF(), input.readUTF(), Culture.valueOf(input.readUTF()));
                allegiances.add(new ClanAllegiance(clan, input.readInt()));
            }
            int treatyCount = input.readInt();
            if (treatyCount < 0 || treatyCount > 190) throw new IOException("Invalid treaty count");
            var treaties = new ArrayList<Treaty>(treatyCount);
            for (int i=0;i<treatyCount;i++) {
                int first=input.readInt(), second=input.readInt(), war=input.readUnsignedByte();
                if (war > 1) throw new IOException("Invalid war flag");
                treaties.add(new Treaty(first, second, war==1, input.readLong()));
            }
            if(input.available() != 0) throw new IOException("Trailing campaign data");
            return new CampaignState(worldSeed, step, kingdoms, allegiances, treaties);
        } catch (IllegalArgumentException | NullPointerException error) {
            throw new IOException("Invalid campaign data: " + file, error);
        }
    }

    public void save(CampaignState state) throws IOException {
        Objects.requireNonNull(state);
        var bytes = new ByteArrayOutputStream();
        try (var output=new DataOutputStream(bytes)) {
            output.writeInt(MAGIC); output.writeInt(SCHEMA);
            output.writeLong(state.worldSeed()); output.writeLong(state.simulationStep());
            output.writeInt(state.kingdoms().size());
            for(var kingdom:state.kingdoms()) {
                output.writeInt(kingdom.id()); output.writeUTF(kingdom.name());
                var clan=kingdom.founder();
                output.writeInt(clan.id()); output.writeUTF(clan.name()); output.writeUTF(clan.ruler()); output.writeUTF(clan.culture().name());
                output.writeInt(kingdom.banner().primaryColor()); output.writeInt(kingdom.banner().secondaryColor()); output.writeInt(kingdom.banner().sigil());
            }
            output.writeInt(state.allegiances().size());
            for (var allegiance : state.allegiances()) {
                var clan=allegiance.clan();
                output.writeInt(clan.id()); output.writeUTF(clan.name()); output.writeUTF(clan.ruler()); output.writeUTF(clan.culture().name());
                output.writeInt(allegiance.kingdomId());
            }
            output.writeInt(state.treaties().size());
            for (var treaty : state.treaties()) {
                output.writeInt(treaty.first()); output.writeInt(treaty.second()); output.writeBoolean(treaty.war()); output.writeLong(treaty.changedAt());
            }
            CRC32 checksum=new CRC32(); checksum.update(bytes.toByteArray()); output.writeLong(checksum.getValue());
        }
        byte[] data=bytes.toByteArray();
        if (data.length > MAX_FILE_BYTES) throw new IOException("Campaign exceeds save size limit");
        Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(), file.getFileName().toString(), ".tmp");
        try {
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                var buffer=ByteBuffer.wrap(data);
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
