package dev.createmechanicaldrive.mixin;

import dev.createmechanicaldrive.content.shaft_marker.ShaftMarkerItem;
import dev.ryanhcode.offroad.content.blocks.wheel_mount.WheelMountInventory;
import dev.simulated_team.simulated.multiloader.inventory.ItemInfoWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WheelMountInventory.class)
public abstract class OffroadWheelMountInventoryMarkerMixin {
    @Inject(
            method = "canInsertItem",
            at = @At("HEAD"),
            cancellable = true
    )
    private void mechanicalDrive$acceptShaftMarker(
            ItemInfoWrapper item,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (ShaftMarkerItem.isMarker(
                ItemInfoWrapper.generateFromInfo(item)
        )) {
            cir.setReturnValue(true);
        }
    }
}
