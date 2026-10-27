package dev.kingdoms.livingsettlements.mixin;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(com.minecolonies.core.entity.ai.workers.AbstractAISkeleton.class)
public interface NativeWorkerAccess {
    @Accessor("worker") AbstractEntityCitizen livingSettlements$worker();
}
