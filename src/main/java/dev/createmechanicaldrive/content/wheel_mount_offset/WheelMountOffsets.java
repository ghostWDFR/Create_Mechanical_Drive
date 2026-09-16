package dev.createmechanicaldrive.content.wheel_mount_offset;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public final class WheelMountOffsets {

    private static final double MOUNT_PART_OFFSET_STEP = 0.125D;
    private static final double OFFSET_EPSILON = 1.0E-7D;

    private WheelMountOffsets() {
    }

    public static Vec3 apply(Object mount, Vec3 original, Direction wheelSide) {
        return ((WheelMountOffset) mount)
                .mechanicalDrive$applyWheelOffset(original, wheelSide);
    }

    public static double lateral(
            Object mount,
            float partialTicks,
            Direction wheelSide
    ) {
        return ((WheelMountOffset) mount)
                .mechanicalDrive$getRenderedLateralOffset(partialTicks, wheelSide);
    }

    public static double longitudinal(Object mount, float partialTicks) {
        return ((WheelMountOffset) mount)
                .mechanicalDrive$getRenderedLongitudinalOffset(partialTicks);
    }

    public static double height(Object mount, float partialTicks) {
        return ((WheelMountOffset) mount)
                .mechanicalDrive$getRenderedHeightOffset(partialTicks);
    }

    public static double maximumAbsolute(Object mount) {
        return ((WheelMountOffset) mount)
                .mechanicalDrive$getWheelOffsetState()
                .getMaximumAbsoluteOffset();
    }

    public static boolean shouldRenderRigidMountPart(Object mount) {
        WheelMountOffset offset = (WheelMountOffset) mount;
        double longitudinal =
                offset.mechanicalDrive$getLerpedLongitudinalOffset(1.0F);

        boolean outsideSideClearance =
                Math.abs(offset.mechanicalDrive$getLerpedLateralOffset(1.0F))
                        > MOUNT_PART_OFFSET_STEP + OFFSET_EPSILON
                || Math.abs(offset.mechanicalDrive$getLerpedHeightOffset(1.0F))
                        > MOUNT_PART_OFFSET_STEP + OFFSET_EPSILON;

        if (outsideSideClearance) {
            return false;
        }

        if (longitudinal > OFFSET_EPSILON) {
            return true;
        }
        if (longitudinal < -OFFSET_EPSILON) {
            return false;
        }

        return true;
    }}
