package dev.kingdoms.livingsettlements;

import com.ldtteam.structurize.placement.StructurePlacer;
import com.ldtteam.structurize.placement.structure.IStructureHandler;
import com.ldtteam.structurize.placement.StructurePhasePlacementResult;
import com.ldtteam.structurize.blocks.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Native-compatible placer with every phase subject to the handler's work budget. */
public final class BudgetedStructurePlacer extends StructurePlacer {
    public BudgetedStructurePlacer(IStructureHandler handler) { super(handler); }
    @Override
    public StructurePhasePlacementResult clearWaterStep(Level world, BlockPos inputPos) {
        // Port 0.6.0's layer loop ignores budgets. Reuse its bounded iterator and native
        // WATER_REMOVAL operation instead, retaining water/substitution blocks in the blueprint.
        // The ordinary placement iterator may treat air over water as already matching.
        // Native removal mode disables that placement-only equality shortcut; reset at
        // end of the phase restores normal placement behavior.
        iterator.setRemoving();
        return executeStructureStep(world, null, inputPos, Operation.WATER_REMOVAL,
            () -> iterator.increment((info, pos, context) -> {
                var state = info.getBlockInfo().getState();
                return !state.getFluidState().isEmpty() || state.is(ModBlocks.blockSubstitution.get())
                    || state.is(ModBlocks.blockFluidSubstitution.get());
            }), false);
    }
}
