package dev.kingdoms.livingsettlements.gametest;

import dev.kingdoms.livingsettlements.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

final class VillageReplacementGameTest {
    static void terrain(GameTestHelper helper) {
        var level=helper.getLevel();
        var preset=level.registryAccess().lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(
            ResourceKey.create(Registries.WORLD_PRESET,Identifier.withDefaultNamespace("normal"))).value();
        var generator=(net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator)preset.overworld().orElseThrow().generator();
        var random=net.minecraft.world.level.levelgen.RandomState.create(generator.generatorSettings().value(),level.registryAccess().lookupOrThrow(Registries.NOISE),42);
        var holder=level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(ResourceKey.create(Registries.STRUCTURE,Identifier.withDefaultNamespace("village_plains")));
        int[] index={0}, rejected={0}; long[] duration={0};
        helper.onEachTick(()->{
            int i=index[0]++; var chunk=new ChunkPos(70000+i*71,-71000+i*53);
            var flat=level.getChunkSource().getGenerator();
            boolean enabled=NaturalSettlementConfig.ENABLED.get(); double chance=NaturalSettlementConfig.REPLACEMENT_CHANCE.get();
            try {
                NaturalSettlementConfig.ENABLED.set(false);
                var original=holder.value().generate(holder,level.dimension(),level.registryAccess(),flat,flat.getBiomeSource(),level.getChunkSource().randomState(),level.getStructureManager(),42,chunk,0,level,b->true);
                NaturalSettlementConfig.ENABLED.set(true); NaturalSettlementConfig.REPLACEMENT_CHANCE.set(1.0);
                var context=new Structure.GenerationContext(level.registryAccess(),generator,generator.getBiomeSource(),random,level.getStructureManager(),42,chunk,level,b->true);
                long started=System.nanoTime(); var result=VillageReplacement.replace(original,context,Identifier.withDefaultNamespace("village_plains"),"minecraft:overworld");
                duration[0]+=System.nanoTime()-started;
                helper.assertTrue(result.isValid(),"Installed noise terrain must always retain a valid vanilla or replacement start");
                if(result==original) rejected[0]++;
                helper.assertTrue(level.getChunkSource().getChunkNow(chunk.x(),chunk.z())==null,"Terrain screening must not create noise-query chunks");
            } finally { NaturalSettlementConfig.ENABLED.set(enabled); NaturalSettlementConfig.REPLACEMENT_CHANCE.set(chance); }
            if(index[0]==16) {
                helper.assertTrue(rejected[0]==0,"Rough noise candidates must reach deferred footprint planning, retaining native fallback pieces");
                System.out.println("TERRAIN_COMPATIBILITY samples=16 fallback="+rejected[0]+" surveyMillis="+duration[0]/1_000_000+" tectonic="+net.neoforged.fml.ModList.get().isLoaded("tectonic")+" terralith="+net.neoforged.fml.ModList.get().isLoaded("terralith"));
                helper.succeed();
            }
        });
    }
    static void run(GameTestHelper helper) {
        var level=helper.getLevel(); var registry=level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        String[] variants={"plains","desert","savanna","snowy","taiga"};
        int[] index={0}, selected={0}, vanilla={0}; boolean[] seen=new boolean[5];
        helper.onEachTick(()->{
            int i=index[0]++, variant=i%5; long seed=new long[]{0,1,-42,9001}[i%4];
            var key=ResourceKey.create(Registries.STRUCTURE,Identifier.withDefaultNamespace("village_"+variants[variant]));
            var holder=registry.getOrThrow(key); var structure=holder.value(); var chunk=new ChunkPos(i*34-3400,i*-17);
            var generator=level.getChunkSource().getGenerator();
            boolean enabled=NaturalSettlementConfig.ENABLED.get();
            int population=NaturalSettlementConfig.POPULATION.get(),cap=NaturalSettlementConfig.MAX_POPULATION.get();
            NaturalSettlementConfig.ENABLED.set(true);
            NaturalSettlementConfig.POPULATION.set(12);NaturalSettlementConfig.MAX_POPULATION.set(4);
            var start=structure.generate(holder,level.dimension(),level.registryAccess(),generator,generator.getBiomeSource(),
                level.getChunkSource().randomState(),level.getStructureManager(),seed,chunk,0,level,b->true);
            NaturalSettlementConfig.ENABLED.set(enabled);
            NaturalSettlementConfig.POPULATION.set(population);NaturalSettlementConfig.MAX_POPULATION.set(cap);
            helper.assertTrue(start.isValid(),"Native village candidate must be valid in flat test terrain");
            boolean replaced=start.getPieces().getFirst() instanceof VillageMarkerPiece;
            helper.assertTrue(replaced==VillageSelection.selected(seed,"minecraft:overworld",chunk.x(),chunk.z(),.5),"Actual generation hook must match deterministic coin at "+chunk);
            if (replaced) {
                selected[0]++; seen[variant]=true;
                var marker=(VillageMarkerPiece)start.getPieces().getFirst();
                helper.assertTrue(marker.population==4,"New generation must clamp conflicting starting population to the configured colony cap");
                helper.assertTrue(marker.getTerrainAdjustment()==TerrainAdjustment.NONE,"Nonphysical reservation must never flatten/carve terrain");
                helper.assertTrue(!marker.vanilla.isEmpty(),"Original native village pieces must be retained for fallback");
                var context=StructurePieceSerializationContext.fromLevel(level);
                var loaded=StructureStart.loadStaticStart(context,start.createTag(context,chunk),seed);
                helper.assertTrue(loaded!=null && loaded.getPieces().getFirst() instanceof VillageMarkerPiece,"Saved structure decision must reload without rerolling");
                helper.assertTrue(((VillageMarkerPiece)loaded.getPieces().getFirst()).vanilla.pieces().size()==marker.vanilla.pieces().size(),"Fallback pieces must survive native NBT reload");
            } else vanilla[0]++;
            if (index[0]>=200) {
                helper.assertTrue(selected[0]>75&&selected[0]<125&&vanilla[0]>75,"200 actual village starts across four seeds must approach half: "+selected[0]);
                for (boolean value:seen) helper.assertTrue(value,"All five vanilla variants must be eligible");
                System.out.println("VILLAGE_SELECTION_SAMPLE selected="+selected[0]+" vanilla="+vanilla[0]+" candidates=200 seeds=4");
                helper.succeed();
            }
        });
    }
}
