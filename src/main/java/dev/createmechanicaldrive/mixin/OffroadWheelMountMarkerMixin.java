package dev.createmechanicaldrive.mixin;

import dev.createmechanicaldrive.content.shaft_marker.ShaftMarkerItem;
import dev.createmechanicaldrive.content.shaft_marker.ShaftMarkerMountHelper;
import dev.ryanhcode.offroad.content.blocks.wheel_mount.WheelMountBlock;
import dev.ryanhcode.offroad.content.blocks.wheel_mount.WheelMountBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WheelMountBlockEntity.class)
public abstract class OffroadWheelMountMarkerMixin {
    @Unique
    private double mechanicalDrive$markerAngle;
    @Unique
    private double mechanicalDrive$previousMarkerAngle;
    @Unique
    private double mechanicalDrive$markerAngularVelocity;
    @Unique
    private boolean mechanicalDrive$markerWasActive;

    @Inject(method = "tick", at = @At("TAIL"))
    private void mechanicalDrive$tickMarkerAngle(CallbackInfo ci) {
        WheelMountBlockEntity mount =
                (WheelMountBlockEntity) (Object) this;
        if (mount.getLevel() == null
                || !mount.getLevel().isClientSide) {
            return;
        }

        if (!ShaftMarkerItem.isMarker(mount.getHeldItem())) {
            mechanicalDrive$markerWasActive = false;
            mechanicalDrive$markerAngularVelocity = 0.0D;
            return;
        }

        if (!mechanicalDrive$markerWasActive) {
            mechanicalDrive$markerAngle = 0.0D;
            mechanicalDrive$previousMarkerAngle = 0.0D;
            mechanicalDrive$markerAngularVelocity = 0.0D;
            mechanicalDrive$markerWasActive = true;
        }

        Direction facing = mount.getBlockState().getValue(
                WheelMountBlock.HORIZONTAL_FACING
        );
        mechanicalDrive$previousMarkerAngle =
                mechanicalDrive$markerAngle;
        mechanicalDrive$markerAngularVelocity =
                ShaftMarkerMountHelper.approachDrivenAngularVelocity(
                        mount,
                        facing,
                        mechanicalDrive$markerAngularVelocity
                );
        mechanicalDrive$markerAngle +=
                mechanicalDrive$markerAngularVelocity;
    }

    @Inject(
            method = "getLerpedAngle",
            at = @At("HEAD"),
            cancellable = true
    )
    private void mechanicalDrive$getMarkerAngle(
            float partialTick,
            CallbackInfoReturnable<Float> cir
    ) {
        WheelMountBlockEntity mount =
                (WheelMountBlockEntity) (Object) this;
        if (ShaftMarkerItem.isMarker(mount.getHeldItem())) {
            cir.setReturnValue((float) Mth.lerp(
                    partialTick,
                    mechanicalDrive$previousMarkerAngle,
                    mechanicalDrive$markerAngle
            ));
        }
    }
}
