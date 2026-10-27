package dev.kingdoms.livingsettlements.mixin;
import com.minecolonies.api.colony.buildings.IBuilding;
import dev.kingdoms.livingsettlements.NativeWorkBudget;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(com.minecolonies.core.entity.ai.workers.util.BuildingStructureHandler.class)
public abstract class NativeWorkerStepMixin {
    @Shadow private IBuilding building;
    @Inject(method="getStepsPerCall",at=@At("HEAD"),cancellable=true)
    private void livingSettlements$oneStep(CallbackInfoReturnable<Integer> result) {
        if(building!=null&&NativeWorkBudget.managed(building.getColony()))result.setReturnValue(1);
    }
}
