package dev.createmechanicaldrive.mixin;

import com.simibubi.create.content.kinetics.base.DirectionalShaftHalvesBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.createmechanicaldrive.content.gear_reducer.GearReducerKinetics;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DirectionalShaftHalvesBlockEntity.class)
public class DirectionalShaftHalvesBlockEntityMixin {

    @Inject(
            method = "getSourceFacing",
            at = @At("HEAD"),
            cancellable = true
    )
    private void createMechanicalDrive$getVirtualReducerSourceFacing(
            CallbackInfoReturnable<Direction> cir
    ) {
        KineticBlockEntity self =
                (KineticBlockEntity) (Object) this;

        if (self.source != null) {
            return;
        }

        Direction sourceFacing =
                GearReducerKinetics
                        .getVirtualReductionSourceFacing(self);

        if (sourceFacing == null) {
            return;
        }

        cir.setReturnValue(
                sourceFacing
        );
    }
}
