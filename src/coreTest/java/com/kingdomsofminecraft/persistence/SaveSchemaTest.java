package com.kingdomsofminecraft.persistence;

import java.io.*;
import java.nio.file.*;
import java.util.Arrays;
import java.util.zip.CRC32;

public final class SaveSchemaTest {
    private static int checks;
    public static void main(String[] args) throws Exception {
        Path file=Files.createTempFile("kingdoms-schema", ".dat");
        try {
            reject(file, fixture(2,-1,"FOREST",0,0,"Realm"), "negative count");
            reject(file, fixture(2,21,"FOREST",0,0,"Realm"), "excess count");
            reject(file, fixture(999,0,"FOREST",0,0,"Realm"), "future schema");
            reject(file, fixture(2,1,"UNKNOWN",0,0,"Realm"), "unknown culture");
            reject(file, fixture(2,1,"FOREST",-1,0,"Realm"), "negative kingdom ID");
            reject(file, fixture(2,1,"FOREST",0,-1,"Realm"), "negative clan ID");
            reject(file, fixture(2,1,"FOREST",0,0," "), "blank name");
            reject(file, fixture(2,2,"FOREST",0,0,"Realm"), "duplicate IDs and clans");
            reject(file, politics(-1,0,0), "negative clan count");
            reject(file, politics(65,0,0), "excess clan count");
            reject(file, politics(0,-1,0), "negative treaty count");
            reject(file, politics(0,191,0), "excess treaty count");
            reject(file, politics(0,1,2), "noncanonical war flag");
            byte[] schema2=fixture(2,1,"FOREST",7,9,"Custom");
            Files.write(file,schema2);
            var migrated=new CampaignStore(file).loadOrCreate(999);
            if(migrated.worldSeed()!=5 || migrated.simulationStep()!=200 || !migrated.kingdoms().getFirst().name().equals("Custom")
                    || migrated.allegiances().getFirst().kingdomId()!=7 || migrated.allegiances().getFirst().clan().id()!=9 || !migrated.treaties().isEmpty())
                throw new AssertionError("Schema 2 migration");
            checks++;
            if(!Arrays.equals(schema2,Files.readAllBytes(file)))throw new AssertionError("Migration modified schema 2 input");
            checks++;
            reject(file, new byte[65537], "oversized input");
            System.out.println("PASS: " + checks + " malformed save checks");
        } finally { Files.delete(file); }
    }
    private static byte[] politics(int clans,int treaties,int flag) throws IOException {
        var bytes=new ByteArrayOutputStream(); var out=new DataOutputStream(bytes);
        out.writeInt(0x4b4f4d43);out.writeInt(3);out.writeLong(5);out.writeLong(200);out.writeInt(0);
        out.writeInt(clans);out.writeInt(treaties);
        if(treaties==1){out.writeInt(0);out.writeInt(1);out.writeByte(flag);out.writeLong(100);}
        CRC32 crc=new CRC32();crc.update(bytes.toByteArray());out.writeLong(crc.getValue());return bytes.toByteArray();
    }
    private static byte[] fixture(int schema,int count,String culture,int id,int clan,String name) throws IOException {
        var bytes=new ByteArrayOutputStream();
        var output=new DataOutputStream(bytes);
        output.writeInt(0x4b4f4d43);output.writeInt(schema);output.writeLong(5);output.writeLong(200);output.writeInt(count);
        if(count>0 && count<=20) for(int i=0;i<count;i++) {
            output.writeInt(id);output.writeUTF(name);output.writeInt(clan);output.writeUTF("Clan");output.writeUTF("Ruler");output.writeUTF(culture);
            output.writeInt(1);output.writeInt(2);output.writeInt(0);
        }
        CRC32 crc=new CRC32();crc.update(bytes.toByteArray());output.writeLong(crc.getValue());return bytes.toByteArray();
    }
    private static void reject(Path file,byte[] bytes,String label) throws Exception {
        Files.write(file,bytes);
        try {new CampaignStore(file).loadOrCreate(5);throw new AssertionError(label);}
        catch(IOException expected) {checks++;}
        if(!Arrays.equals(bytes,Files.readAllBytes(file)))throw new AssertionError("Read mutated input: "+label);
        checks++;
    }
}
