package dev.createmechanicaldrive.content.tracks.mounts.torsion;

import dev.createmechanicaldrive.content.shaft_marker.ShaftMarkerItem;
import dev.createmechanicaldrive.content.tracks.wheels.drive.BigDriveWheelItem;
import dev.createmechanicaldrive.content.tracks.wheels.drive.DriveWheelItem;
import dev.createmechanicaldrive.content.tracks.wheels.support.SupportWheelItem;
import net.minecraft.world.item.ItemStack;

public final class TorsionMountAttachments {
    private TorsionMountAttachments() {
    }

    public static boolean supports(ItemStack stack) {
        return ShaftMarkerItem.isMarker(stack)
                || DriveWheelItem.isDriveWheel(stack);
    }

    public static boolean hasSuspensionWheel(ItemStack stack) {
        return DriveWheelItem.isDriveWheel(stack)
                || BigDriveWheelItem.isBigDriveWheel(stack);
    }

    public static float wheelRadius(ItemStack stack) {
        if (DriveWheelItem.isDriveWheel(stack)) {
            return DriveWheelItem.RADIUS;
        }
        if (BigDriveWheelItem.isBigDriveWheel(stack)) {
            return BigDriveWheelItem.RADIUS;
        }
        return 0.0F;
    }

    public static boolean supportsSupportWheel(ItemStack stack) {
        return SupportWheelItem.isSupportWheel(stack);
    }
}
