package dev.createmechanicaldrive.mixin;

import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import dev.createmechanicaldrive.content.service_tank.ServiceTankBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FluidTankBlockEntity.class)
public abstract class FluidTankBlockEntityMixin {
    @Inject(method = "updateConnectivity", at = @At("HEAD"), cancellable = true)
    private void mechanicalDrive$keepServiceTankConnection(CallbackInfo ci) {
        FluidTankBlockEntity tank = (FluidTankBlockEntity) (Object) this;
        if (tank.getLevel() != null
                && ServiceTankBlockEntity.claimForAdjacentService(
                tank.getLevel(),
                tank.getBlockPos()
        )) {
            ci.cancel();
        }
    }
}
