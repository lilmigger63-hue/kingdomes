package dev.kingdoms.livingsettlements.gametest;

import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.savedata.IServerColonySaveData;
import com.minecolonies.core.colony.buildingextensions.FarmField;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import dev.kingdoms.livingsettlements.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.level.ChunkEvent;
import java.util.UUID;

/** Full native-asset bootstrap in a disposable world; forced chunks exist ONLY in this test. */
final class NaturalSettlementGameTest {
    static int activeFixtures;
    static void waterBudget(GameTestHelper helper) {
        var level = helper.getLevel(); var origin = helper.absolutePos(new BlockPos(0, 20, 0));
        var bp = new com.ldtteam.structurize.blueprints.v1.Blueprint((short) 16, (short) 1, (short) 16, level.registryAccess());
        for (int x = 0; x < 16; x++) { for (int z = 0; z < 16; z++) {
            bp.addBlockState(new BlockPos(x, 0, z), Blocks.AIR.defaultBlockState());
            level.setBlock(origin.offset(x, 0, z), Blocks.WATER.defaultBlockState(), 2);
        } }
        bp.setCachePrimaryOffset(BlockPos.ZERO); // addBlockState invalidates this native cache.
        var handler = new com.ldtteam.structurize.placement.structure.CreativeStructureHandler(level, origin, bp,
                com.ldtteam.structurize.api.RotationMirror.NONE, false) {
            @Override public int getStepsPerCall() { return 16; }
            @Override public int getMaxBlocksCheckedPerCall() { return 16; }
        };
        var placer = new BudgetedStructurePlacer(handler);
        var pos = com.ldtteam.structurize.placement.AbstractBlueprintIterator.NULL_POS;
        int remaining = 256;
        for (int call = 0; call < 64; call++) {
            var result = placer.clearWaterStep(level, pos); pos = result.getIteratorPos();
            int water = 0;
            for (int x = 0; x < 16; x++) { for (int z = 0; z < 16; z++) { if (!level.getFluidState(origin.offset(x, 0, z)).isEmpty()) { water++; } } }
            helper.assertTrue(remaining - water <= 16, "Native water clearing must respect the 16-step budget, not clear an entire layer");
            remaining = water;
            if (result.getBlockResult().getResult() == com.ldtteam.structurize.placement.BlockPlacementResult.Result.FINISHED) { break; }
        }
        helper.assertTrue(remaining == 0, "Budgeted native water phase must eventually finish"); helper.succeed();
    }
    static void run(GameTestHelper helper) {
        run(helper, false, false);
    }
    static void village(GameTestHelper helper) {
        run(helper, true, false);
    }
    static void fallback(GameTestHelper helper) { run(helper,true,true); }
    private static void run(GameTestHelper helper, boolean village, boolean fallback) {
        activeFixtures++;
        var level = helper.getLevel();
        NaturalSettlementConfig.ENABLED.set(false);
        NaturalSettlementConfig.MAYOR.set(false);
        NaturalSettlementConfig.TOWN_HALL_START.set(false); // Retained 0.3 full-starter regression fixture.
        NaturalSettlementConfig.AUTONOMOUS.set(false); // Explicit requests below isolate governors while fixtures run concurrently.
        NaturalSettlementConfig.FREQUENCY.set(1.0);
        NaturalSettlementConfig.PRESERVE_FREQUENCY.set(false); // Exercise the retained optional grid alongside replacement.
        NaturalSettlementConfig.DISTANCE.set(1024);
        NaturalSettlementConfig.RADIUS.set(112);
        NaturalSettlementConfig.POPULATION.set(6);
        NaturalSettlementConfig.MAX_POPULATION.set(32);
        NaturalSettlementConfig.BIOMES.set(java.util.List.of());
        NaturalSettlementConfig.PLACE_BUDGET.set(512);
        NaturalSettlementConfig.SCAN_BUDGET.set(512);
        var data = NaturalSettlementsData.get(level);
        int cell = fallback ? 48 : village ? 32 : 16;
        NaturalGenerationRules.Candidate chosen;
        do { int chunkX=cell++*64;chosen=new NaturalGenerationRules.Candidate(chunkX,16384+(fallback?48:village?32:16)*512); }
        while (!NaturalGenerationRules.independentCandidate(level.getSeed(),chosen.chunkX(),chosen.chunkZ(),NaturalSettlementConfig.DENSITY.get(),NaturalSettlementConfig.REPLACE_VILLAGES.get())||data.at(new BlockPos(chosen.chunkX() * 16 + 8, 0, chosen.chunkZ() * 16 + 8).asLong()) != null);
        var candidate = chosen;
        BlockPos center = new BlockPos(candidate.chunkX() * 16 + 8, 0, candidate.chunkZ() * 16 + 8);
        // Native preexisting player colony: its ownership must remain unchanged and it must not acquire NPC authorization.
        UUID playerOwner = UUID.randomUUID();
        var protectedColony = IServerColonySaveData.getOrComputeSaveData(level).createColony(level, "Existing player colony",
            center.offset(4096, 70, 0));
        protectedColony.getPermissions().setOwner(FakePlayerFactory.get(level, new GameProfile(playerOwner, "TestPlayer")));
        int[] tick = {0}, loadedChunks = {0}, preparedColumns = {0}, completedAt = {-1};
        int[] offlinePhase = {0}, offlineWait = {0};
        boolean[] fallbackTerrain = {false};
        java.util.concurrent.CompletableFuture<?>[] diskSave = {null};
        int minChunkX = (center.getX() - 160) >> 4, minChunkZ = (center.getZ() - 160) >> 4;
        int maxChunkX = (center.getX() + 160) >> 4, maxChunkZ = (center.getZ() + 160) >> 4;
        int chunkWidth = maxChunkX - minChunkX + 1, chunkDepth = maxChunkZ - minChunkZ + 1;
        level.setChunkForced(candidate.chunkX(), candidate.chunkZ(), true);
        var discoveryChunk = level.getChunk(candidate.chunkX(), candidate.chunkZ());
        if (village) {
            var key=net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.STRUCTURE,
                net.minecraft.resources.Identifier.withDefaultNamespace("village_plains"));
            var holder=level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).getOrThrow(key);
            var generator=level.getChunkSource().getGenerator();
            var original=holder.value().generate(holder,level.dimension(),level.registryAccess(),generator,generator.getBiomeSource(),
                level.getChunkSource().randomState(),level.getStructureManager(),level.getSeed(),discoveryChunk.getPos(),0,level,b->true);
            helper.assertTrue(original.isValid(),"Native village fixture must have real original pieces");
            if (fallback) for(var piece:original.getPieces()) piece.move(0,141,0); // Match prepared ground80 instead of flat-world ground-61.
            var marker=new VillageMarkerPiece(new BlockPos(center.getX(),81,center.getZ()),
                new net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer(original.getPieces()),6,112);
            discoveryChunk.setStartForStructure(holder.value(),new net.minecraft.world.level.levelgen.structure.StructureStart(
                holder.value(),discoveryChunk.getPos(),0,new net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer(java.util.List.of(marker))));
        }
        NaturalSettlementConfig.ENABLED.set(true);
        NeoForge.EVENT_BUS.post(new ChunkEvent.Load(discoveryChunk, !village));
        helper.onEachTick(() -> {
            tick[0]++;
            if (offlinePhase[0] == 1) {
                if (level.getChunkSource().getChunkNow(candidate.chunkX(),candidate.chunkZ()) != null) {
                    helper.assertTrue(++offlineWait[0]<10000,"NPC colony chunks must unload without permanent tickets"); return;
                }
                var saved=data.at(center.asLong()); var clock=(net.minecraft.world.level.storage.ServerLevelData)level.getLevelData();
                long before=level.getGameTime();
                try {
                    NaturalSettlementConfig.AUTONOMOUS.set(true);
                    for(int cycle=0;cycle<4;cycle++) {
                        clock.setGameTime(before+24000+(20-before%20)+cycle*20);
                        OfflineDevelopment.tick(new net.neoforged.neoforge.event.tick.LevelTickEvent.Post(()->true,level));
                    }
                } finally { clock.setGameTime(before); NaturalSettlementConfig.AUTONOMOUS.set(false); }
                helper.assertTrue(saved.developmentCredit()>0,"Unloaded colony must accumulate persisted development intent from its real resource snapshot");
                helper.assertTrue(level.getChunkSource().getChunkNow(candidate.chunkX(),candidate.chunkZ())==null,"Offline progression must not load the colony");
                loadedChunks[0]=0; offlinePhase[0]=2; return;
            }
            if (offlinePhase[0] == 2) {
                if (loadedChunks[0] < chunkWidth*chunkDepth) {
                    int index=loadedChunks[0]++, x=minChunkX+index%chunkWidth,z=minChunkZ+index/chunkWidth;
                    level.setChunkForced(x,z,true); level.getChunk(x,z); return;
                }
                offlinePhase[0]=3;
            }
            if (tick[0] <= 20) { return; }
            if (tick[0] == 21) {
                var waiting = data.at(center.asLong());
                helper.assertTrue(waiting != null && waiting.colonyId() == 0 && !waiting.failed(), "An unloaded reservation must wait without founding");
                helper.assertTrue(level.getChunkSource().getChunkNow(candidate.chunkX() + 9, candidate.chunkZ() + 9) == null,
                    "Generation must not load the missing footprint chunks");
                NaturalSettlementConfig.ENABLED.set(false); return;
            }
            if (loadedChunks[0] < chunkWidth * chunkDepth) {
                int index = loadedChunks[0]++;
                int x = minChunkX + index % chunkWidth, z = minChunkZ + index / chunkWidth;
                level.setChunkForced(x, z, true); level.getChunk(x, z); return;
            }
            if (preparedColumns[0] < 321 * 321) {
                for (int work = 0; work < 2048 && preparedColumns[0] < 321 * 321; work++) {
                    int index = preparedColumns[0]++;
                    int x = center.getX() - 160 + index % 321, z = center.getZ() - 160 + index / 321;
                    level.setBlock(new BlockPos(x, 79, z), Blocks.DIRT.defaultBlockState(), 2);
                    level.setBlock(new BlockPos(x, 80, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                }
                return;
            }
            if(fallback && !fallbackTerrain[0]) {
                    // A small natural bump rejects the town hall survey; original vanilla pieces remain safe to restore.
                    for(int y=81;y<=85;y++) level.setBlock(new BlockPos(center.getX(),y,center.getZ()),Blocks.DIRT.defaultBlockState(),2);
                    fallbackTerrain[0]=true;
            }
            if (!NaturalSettlementConfig.ENABLED.get()) {
                NaturalSettlementConfig.ENABLED.set(true);
                NeoForge.EVENT_BUS.post(new ChunkEvent.Load(level.getChunk(candidate.chunkX(), candidate.chunkZ()), true));
            }
            if (fallback) {
                var s=data.at(center.asLong());
                if(tick[0]%500==0) System.out.println("FALLBACK_TEST_PROGRESS "+tick[0]+" "+s.status());
                helper.assertTrue(s.colonyId()==0,"Rejected replacement must never found a colony during vanilla fallback");
                if (s.status().equals("Original vanilla village fallback completed")) {
                    var villagers=level.getEntitiesOfClass(net.minecraft.world.entity.npc.villager.Villager.class,
                        new net.minecraft.world.phys.AABB(center.getX()-160,60,center.getZ()-160,center.getX()+160,200,center.getZ()+160));
                    helper.assertTrue(!villagers.isEmpty(),"Native fallback must restore actual vanilla villagers and buildings");
                    for(int x=minChunkX;x<=maxChunkX;x++) for(int z=minChunkZ;z<=maxChunkZ;z++) level.setChunkForced(x,z,false);
                    activeFixtures--;helper.succeed();
                } else helper.assertTrue(!s.status().contains("blocked")&&!s.status().contains("unavailable")&&!s.status().contains("Oversized"),"Fallback unexpectedly stopped: "+s.status());
                return;
            }
            var settlement = data.at(center.asLong());
            if (settlement == null) { return; }
            if (settlement.failed()) { helper.fail("Natural generation failed: " + settlement.status()); return; }
            if (tick[0] % 500 == 0) { System.out.println("NATURAL_TEST_PROGRESS " + tick[0] + " " + settlement.status()); }
            if (!settlement.complete()) { return; }
            var colony = IColonyManager.getInstance().getColonyByWorld(settlement.colonyId(), level);
            helper.assertTrue(colony != null && colony.getPermissions().getOwner().equals(settlement.owner()), "Real colony must retain independent NPC ownership");
            helper.assertTrue(colony.getServerBuildingManager().getTownHall() != null, "Native town hall must be registered");
            helper.assertTrue(colony.getServerBuildingManager().getBuildings().size() >= 11, "Town hall, builder, homes and essential workplaces must exist");
            helper.assertTrue(colony.getCitizenManager().getCurrentCitizenCount() == 6, "Six real native citizens must exist");
            helper.assertTrue(colony.getCitizenManager().getCitizens().stream().allMatch(c -> c.getEntity().filter(entity -> entity.isAlive()).isPresent()),
                "Citizen records must correspond to living native entities, not metadata-only population");
            helper.assertTrue(colony.getCitizenManager().getCitizens().stream().allMatch(c -> {
                for (int slot = 0; slot < c.getInventory().getSlots(); slot++) {
                    if (c.getInventory().getStackInSlot(slot).is(net.minecraft.world.item.Items.BREAD)) { return true; }
                }
                return false;
            }), "Every starting citizen must receive real food in its native inventory");
            helper.assertTrue(colony.getCitizenManager().getCitizens().stream().allMatch(c -> c.getHomeBuilding() != null), "Every citizen needs an assigned home");
            helper.assertTrue(colony.getCitizenManager().getCitizens().stream().filter(c -> c.getJob() != null).count() == 6, "Six essential jobs must be assigned");
            helper.assertTrue(colony.getServerBuildingManager().getMatchingBuildingExtension(e -> e instanceof FarmField).isPresent(), "Native farm field must be registered");
            helper.assertTrue(colony.getServerBuildingManager().getMatchingBuildingExtension(e -> e instanceof FarmField field
                && field.getSeed().is(net.minecraft.world.item.Items.CARROT)).isPresent(), "Native farmer field must have its crop seed configured");
            var farmerPosition = BlockPos.of(settlement.plans().stream().filter(p -> p.role().equals("farmer")).findFirst().orElseThrow().anchor());
            helper.assertTrue(colony.getServerBuildingManager().getMatchingBuildingExtension(e -> e instanceof FarmField
                && e.isTaken() && farmerPosition.equals(e.getBuildingId())).isPresent(), "The actual native field must be assigned to the generated farmer");
            helper.assertTrue(GovernorSavedData.get(level).assignment(colony.getID()).authorizer().equals(settlement.owner()), "Existing governor must authorize this NPC owner");
            helper.assertTrue(NaturalSettlementService.npcActor(level, colony, settlement.owner()) != null, "NPC governor must work without a real owner online");
            helper.assertTrue(protectedColony.getPermissions().getOwner().equals(playerOwner), "Existing player ownership must remain unchanged");
            helper.assertTrue(NaturalSettlementService.npcActor(level, protectedColony, playerOwner) == null, "Player colonies must never get NPC authorization");
            if (completedAt[0] < 0) {
                var roundTrip = NaturalSettlementsData.CODEC.parse(JsonOps.INSTANCE,
                    NaturalSettlementsData.CODEC.encodeStart(JsonOps.INSTANCE, data).getOrThrow()).getOrThrow();
                helper.assertTrue(roundTrip.ownsNpcColony(colony.getID(), settlement.owner()), "NPC identity must survive save codec round trip");
                completedAt[0] = tick[0];
                for (int i = 0; i < 3; i++) { NeoForge.EVENT_BUS.post(new ChunkEvent.Load(level.getChunk(candidate.chunkX(), candidate.chunkZ()), true)); }
                return;
            }
            if (tick[0] - completedAt[0] < 20) { return; }
            helper.assertTrue(IColonyManager.getInstance().getColonies(level).stream().filter(c -> c.getPermissions().getOwner().equals(settlement.owner())).count() == 1,
                "Repeated chunk events must not duplicate the colony");
            if (diskSave[0] == null) { data.setDirty(); diskSave[0] = level.getDataStorage().scheduleSave(); return; }
            if (!diskSave[0].isDone()) { return; }
            diskSave[0].join();
            var dataFolder = net.minecraft.world.level.dimension.DimensionType.getStorageFolder(level.dimension(),
                level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)).resolve("data");
            try (var freshStorage = new net.minecraft.world.level.storage.SavedDataStorage(level, dataFolder,
                    net.minecraft.util.datafix.DataFixers.getDataFixer(), level.registryAccess())) {
                var reloaded = freshStorage.get(NaturalSettlementsData.TYPE);
                helper.assertTrue(reloaded != null && reloaded.ownsNpcColony(colony.getID(), settlement.owner()),
                    "Fresh saved-data storage must recover the same completed NPC identity from disk");
            }
            helper.assertTrue(level.getServer().getPlayerList().getPlayer(settlement.owner()) == null, "NPC owner must not be a logged-in real player");
            if (village && offlinePhase[0]==0) {
                var clock=(net.minecraft.world.level.storage.ServerLevelData)level.getLevelData(); long before=level.getGameTime();
                try {
                    NaturalSettlementConfig.AUTONOMOUS.set(true);
                    for(int cycle=0;cycle<4;cycle++) {
                        clock.setGameTime(before+1200+(20-before%20)+cycle*20);
                        OfflineDevelopment.tick(new net.neoforged.neoforge.event.tick.LevelTickEvent.Post(()->true,level));
                    }
                } finally { clock.setGameTime(before); NaturalSettlementConfig.AUTONOMOUS.set(false); }
                for (int x=minChunkX;x<=maxChunkX;x++) for(int z=minChunkZ;z<=maxChunkZ;z++) level.setChunkForced(x,z,false);
                offlinePhase[0]=1; return;
            }
            helper.assertTrue(colony.getWorkManager().getWorkOrders().isEmpty(), "Governor test must start with no existing construction work");
            var clock = (net.minecraft.world.level.storage.ServerLevelData) level.getLevelData();
            long originalTime = level.getGameTime();
            var otherAssignments=GovernorSavedData.get(level).assignments().stream().filter(a->a.colonyId()!=colony.getID()).toList();
            try {
                for(var a:otherAssignments) GovernorSavedData.get(level).disable(a.colonyId());
                long next=originalTime+(200-originalTime%200);
                if (village && next%1200==0) next+=200;
                clock.setGameTime(village ? next : originalTime + (1200 - originalTime % 1200));
                NaturalSettlementConfig.AUTONOMOUS.set(true);
                new GovernorService().tick(new net.neoforged.neoforge.event.tick.LevelTickEvent.Post(() -> true, level));
            } finally {
                clock.setGameTime(originalTime); NaturalSettlementConfig.AUTONOMOUS.set(false);
                for(var a:otherAssignments) GovernorSavedData.get(level).enable(a.colonyId(),a.authorizer(),a.maximumLevel());
            }
            helper.assertTrue(!colony.getWorkManager().getWorkOrders().isEmpty(), "NPC governor must queue a real native construction work order without an online owner");
            // Owner changes must revoke the special NPC path even if the original authorizer had an assignment.
            colony.getPermissions().setOwner(FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "NewOwner")));
            helper.assertTrue(NaturalSettlementService.npcActor(level, colony, settlement.owner()) == null, "Ownership change must stop NPC authorization");
            if(village)colony.getPermissions().setOwner(FakePlayerFactory.get(level,new GameProfile(settlement.owner(),"RestoredTestNPC"))); // Reuse this disposable native colony for worker integration tests.
            for (int x = minChunkX; x <= maxChunkX; x++) { for (int z = minChunkZ; z <= maxChunkZ; z++) { level.setChunkForced(x, z, false); } }
            NaturalSettlementConfig.ENABLED.set(false);
            activeFixtures--;helper.succeed();
        });
    }
}
