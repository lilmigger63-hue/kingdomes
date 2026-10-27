package dev.kingdoms.livingsettlements.mixin;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.entity.ai.statemachine.states.*;
import dev.kingdoms.livingsettlements.NativeWorkBudget;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(com.minecolonies.core.entity.ai.workers.AbstractEntityAIStructure.class)
public abstract class NativeWorkerBudgetMixin {
    @Inject(method="structureStep",at=@At("HEAD"),cancellable=true)
    private void livingSettlements$budget(CallbackInfoReturnable<IAIState> result) {
        if(!NativeWorkBudget.permit(((NativeWorkerAccess)(Object)this).livingSettlements$worker()))result.setReturnValue(AIWorkerState.BUILDING_STEP);
    }
}
