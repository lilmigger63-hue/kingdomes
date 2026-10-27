package dev.kingdoms.livingsettlements;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import java.util.ArrayList;

public final class TerrainSurvey {
    private TerrainSurvey() {}

    /** Conservative 64x64 footprint; never generates or force-loads a chunk. */
    public static SiteAssessment.Result survey(ServerLevel level, BlockPos center) {
        var samples = new ArrayList<SiteAssessment.Surface>(81);
        if (!level.dimension().equals(Level.OVERWORLD)) {
            return SiteAssessment.evaluate(samples, 81);
        }
        for (int dx = -32; dx <= 32; dx += 8) {
            for (int dz = -32; dz <= 32; dz += 8) {
                BlockPos column = new BlockPos(center.getX() + dx, center.getY(), center.getZ() + dz);
                if (!level.getWorldBorder().isWithinBounds(column) || !ColonyIntegration.loaded(level, column)) {
                    return SiteAssessment.evaluate(samples, 81);
                }
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ()) - 1;
                BlockPos ground = new BlockPos(column.getX(), y, column.getZ());
                var state = level.getBlockState(ground);
                var biome = level.getBiome(ground);
                boolean fluid = !state.getFluidState().isEmpty() || biome.is(BiomeTags.IS_OCEAN)
                    || biome.is(BiomeTags.IS_RIVER);
                boolean unsafe = !state.isFaceSturdy(level, ground, Direction.UP)
                    || state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.POWDER_SNOW)
                    || state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE);
                boolean claimed = ColonyIntegration.at(level, ground) != null;
                samples.add(new SiteAssessment.Surface(y, fluid, unsafe, claimed));
            }
        }
        return SiteAssessment.evaluate(samples, 81);
    }
}
