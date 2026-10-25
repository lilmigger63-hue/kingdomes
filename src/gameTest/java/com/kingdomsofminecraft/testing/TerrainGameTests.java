package com.kingdomsofminecraft.testing;

import com.kingdomsofminecraft.geography.SiteRole;
import com.kingdomsofminecraft.integration.LoadedTerrainSampler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;

/** Explicit bounded generation is confined to this isolated test mod. */
public final class TerrainGameTests {
    private TerrainGameTests() {}
    public static void terrain(GameTestHelper helper) {
        var server=helper.getLevel().getServer();
        helper.assertTrue(net.neoforged.fml.ModList.get().isLoaded("tectonic")
                && net.neoforged.fml.ModList.get().isLoaded("lithostitched")
                && net.neoforged.fml.ModList.get().isLoaded("minecolonies"),"Actual required terrain/colony mods must load");
        var level=server.getLevel(ResourceKey.create(Registries.DIMENSION,
                Identifier.fromNamespaceAndPath(CampaignGameTests.ID,"tectonic_probe")));
        helper.assertTrue(level!=null,"Fresh normal-noise probe dimension must exist");
        helper.assertTrue(level.getChunkSource().getGenerator() instanceof NoiseBasedChunkGenerator,"Probe must not use the flat GameTest generator");
        try {
            var resource=server.getResourceManager().getResource(Identifier.fromNamespaceAndPath("minecraft",
                    "worldgen/density_function/overworld/noise_router/final_density.json")).orElseThrow();
            try(var reader=new java.io.BufferedReader(resource.openAsReader())) {
                String density=reader.lines().collect(java.util.stream.Collectors.joining());
                helper.assertTrue(density.contains("tectonic:base_terrain"),"Resolved active overworld density must come from Tectonic");
            }
        } catch(java.io.IOException error) {throw helper.assertionException("Cannot inspect active terrain pack: "+error.getMessage());}
        var sampler=new LoadedTerrainSampler();
        helper.assertTrue(sampler.sample(level,312500,312500).isEmpty(),"Unloaded geography stays unknown");
        helper.assertTrue(!level.hasChunk(312500,312500),"Sampler must not generate remote chunks");
        // Four explicit FULL chunk requests test fresh generation only. Production never does this.
        for(int x=0;x<2;x++)for(int z=0;z<2;z++) {
            var chunk=level.getChunk(x,z);
            var cell=sampler.sample(level,x,z).orElseThrow(() -> helper.assertionException("Generated surface must be observable"));
            for(var role:SiteRole.values()) {
                var assessment=role.assess(cell);
                helper.assertTrue(assessment.score()>=0 && assessment.score()<=100,"Terrain score bounded");
            }
            com.mojang.logging.LogUtils.getLogger().info("TECTONIC_PROBE cell {},{} height {}..{} water {} forest {} river {} ocean {} village {}",
                    x,z,cell.minY(),cell.maxY(),cell.water(),cell.forest(),cell.river(),cell.ocean(),SiteRole.VILLAGE.assess(cell).suitable());
            // Observe without altering a generated surface block.
            var pos=new BlockPos(x*16,chunk.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,0,0),z*16);
            var before=chunk.getBlockState(pos);
            sampler.sample(level,x,z);
            helper.assertTrue(before.equals(chunk.getBlockState(pos)),"Sampling must preserve generated blocks");
        }
        var source=server.createCommandSourceStack().withLevel(level).withPosition(new net.minecraft.world.phys.Vec3(8,100,8)).withPermission(PermissionSet.NO_PERMISSIONS);
        try {
            helper.assertTrue(server.getCommands().getDispatcher().execute("kingdoms site village",source)==1,"Public advisory site inspection on generated terrain");
            helper.assertTrue(server.getCommands().getDispatcher().execute("kingdoms site invalid",source)==0,"Unknown profile rejected");
        } catch(com.mojang.brigadier.exceptions.CommandSyntaxException error) {throw helper.assertionException(error.getMessage());}
        helper.assertTrue(!level.hasChunk(312500,312500),"No persistent remote terrain loading");
        // Disposable loaded test-world fixture; restored before any fluid tick can run.
        var marker=helper.absolutePos(BlockPos.ZERO);
        var fixtureChunk=helper.getLevel().getChunkSource().getChunkNow(Math.floorDiv(marker.getX(),16),Math.floorDiv(marker.getZ(),16));
        helper.assertTrue(fixtureChunk!=null,"Fixture chunk is already loaded");
        var lavaPos=new BlockPos(fixtureChunk.getPos().getMinBlockX(),fixtureChunk.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,0,0)+3,fixtureChunk.getPos().getMinBlockZ());
        var original=helper.getLevel().getBlockState(lavaPos);
        try {
            helper.getLevel().setBlock(lavaPos,net.minecraft.world.level.block.Blocks.LAVA.defaultBlockState(),2);
            var lavaSampler=new LoadedTerrainSampler();
            helper.assertTrue(lavaSampler.sample(helper.getLevel(),Math.floorDiv(marker.getX(),16),Math.floorDiv(marker.getZ(),16)).isEmpty(),"Lava surface must be rejected as unsafe, not dry land");
        } finally {helper.getLevel().setBlock(lavaPos,original,2);}
        helper.assertTrue(helper.getLevel().getBlockState(lavaPos).equals(original),"Test fixture restored");
        helper.succeed();
    }
}
