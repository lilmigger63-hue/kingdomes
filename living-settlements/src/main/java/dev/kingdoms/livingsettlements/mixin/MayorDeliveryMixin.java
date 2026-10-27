package dev.kingdoms.livingsettlements.mixin;
import com.minecolonies.core.colony.CitizenData;
import com.minecolonies.api.colony.ICitizenData;
import dev.kingdoms.livingsettlements.MayorService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A brief transient hold prevents builders consuming a handoff before its receipt is durable. */
@Mixin(CitizenData.class)
public abstract class MayorDeliveryMixin {
    @Inject(method="isPaused",at=@At("RETURN"),cancellable=true)
    private void livingSettlements$holdDelivery(CallbackInfoReturnable<Boolean> callback){
        if(!callback.getReturnValue()&&MayorService.deliveryPendingFor((ICitizenData)(Object)this))callback.setReturnValue(true);
    }
}
