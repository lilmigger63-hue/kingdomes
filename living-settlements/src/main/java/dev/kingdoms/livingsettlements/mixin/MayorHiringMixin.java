package dev.kingdoms.livingsettlements.mixin;

import com.minecolonies.core.colony.Colony;
import com.minecolonies.core.colony.managers.CitizenManager;
import com.minecolonies.api.colony.ICitizenData;
import dev.kingdoms.livingsettlements.MayorService;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Reserve only the recorded NPC mayor; native hiring of player citizens is unchanged. */
@Mixin(CitizenManager.class)
public abstract class MayorHiringMixin {
    @Shadow @Final private Colony colony;
    @Inject(method="getJoblessCitizen",at=@At("RETURN"),cancellable=true)
    private void livingSettlements$reserveMayor(CallbackInfoReturnable<ICitizenData> callback) {
        var chosen=callback.getReturnValue();if(chosen==null||!MayorService.isMayor(colony,chosen))return;
        callback.setReturnValue(colony.getCitizenManager().getCitizens().stream()
            .filter(c->c.getWorkBuilding()==null&&!c.isChild()&&!MayorService.isMayor(colony,c)).findFirst().orElse(null));
    }
    @Inject(method="removeCivilian",at=@At("RETURN"))
    private void livingSettlements$releaseMayor(com.minecolonies.api.colony.ICivilianData citizen,CallbackInfo callback){
        MayorService.removed(colony,citizen);
    }
    @Inject(method="calculateMaxCitizens",at=@At("RETURN"))
    private void livingSettlements$limitGrowth(CallbackInfo callback){MayorService.limitPopulation(colony);}
}
