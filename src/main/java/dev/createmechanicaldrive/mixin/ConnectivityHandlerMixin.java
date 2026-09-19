package dev.createmechanicaldrive.mixin;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import dev.createmechanicaldrive.content.service_tank.ServiceTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ConnectivityHandler.class)
public abstract class ConnectivityHandlerMixin {
    @Inject(method = "partAt", at = @At("HEAD"), cancellable = true)
    private static <T extends BlockEntity & IMultiBlockEntityContainer> void mechanicalDrive$excludeServiceTankParts(
            BlockEntityType<?> type,
            BlockGetter level,
            BlockPos pos,
            CallbackInfoReturnable<T> cir
    ) {
        if (ServiceTankBlockEntity.isServiceLinked(level, pos)) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "isConnected", at = @At("HEAD"), cancellable = true)
    private static void mechanicalDrive$keepServiceTankPartsVisuallySingle(
            BlockGetter level,
            BlockPos first,
            BlockPos second,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (ServiceTankBlockEntity.isServiceConnection(level, first, second)) {
            cir.setReturnValue(false);
        }
    }
}
