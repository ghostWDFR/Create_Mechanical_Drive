package dev.createmechanicaldrive.content.wheel_mount_offset;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public interface WheelMountOffset {

    WheelMountOffsetState mechanicalDrive$getWheelOffsetState();

    void mechanicalDrive$syncWheelOffset();

    default double mechanicalDrive$adjustLateralOffset(int steps) {
        double value = mechanicalDrive$getWheelOffsetState().adjustLateral(steps);
        mechanicalDrive$syncWheelOffset();
        return value;
    }

    default double mechanicalDrive$adjustLongitudinalOffset(int steps) {
        double value = mechanicalDrive$getWheelOffsetState().adjustLongitudinal(steps);
        mechanicalDrive$syncWheelOffset();
        return value;
    }

    default double mechanicalDrive$adjustHeightOffset(int steps) {
        double value = mechanicalDrive$getWheelOffsetState().adjustHeight(steps);
        mechanicalDrive$syncWheelOffset();
        return value;
    }

    default double mechanicalDrive$getLerpedLateralOffset(float partialTicks) {
        return mechanicalDrive$getWheelOffsetState().getLerpedLateral(partialTicks);
    }

    default double mechanicalDrive$getLerpedLongitudinalOffset(float partialTicks) {
        return mechanicalDrive$getWheelOffsetState().getLerpedLongitudinal(partialTicks);
    }

    default double mechanicalDrive$getLerpedHeightOffset(float partialTicks) {
        return mechanicalDrive$getWheelOffsetState().getLerpedHeight(partialTicks);
    }

    Direction mechanicalDrive$getOffsetFacing();

    default boolean mechanicalDrive$isDoubleSidedOffset() {
        return false;
    }

    default boolean mechanicalDrive$isHeightOffsetFace(Direction clickedFace) {
        Direction facing = mechanicalDrive$getOffsetFacing();
        return clickedFace == facing
                || mechanicalDrive$isDoubleSidedOffset()
                && clickedFace == facing.getOpposite();
    }

    default Vec3 mechanicalDrive$applyWheelOffset(
            Vec3 original,
            Direction wheelSide
    ) {
        Direction facing = mechanicalDrive$getOffsetFacing();
        Direction lateral = facing.getClockWise();
        // Renderer-local +Z points back towards the mount. Use the same
        // direction for the physical contact center so traces and wheels
        // remain aligned on either side of a double mount.
        Direction longitudinal = wheelSide.getOpposite();

        return original
                .add(Vec3.atLowerCornerOf(lateral.getNormal())
                        .scale(mechanicalDrive$getLerpedLateralOffset(1.0F)))
                .add(Vec3.atLowerCornerOf(longitudinal.getNormal())
                        .scale(mechanicalDrive$getLerpedLongitudinalOffset(1.0F)))
                .add(0.0D, mechanicalDrive$getLerpedHeightOffset(1.0F), 0.0D);
    }

    default double mechanicalDrive$getRenderedLateralOffset(
            float partialTicks,
            Direction wheelSide
    ) {
        double offset = mechanicalDrive$getLerpedLateralOffset(partialTicks);
        return wheelSide == mechanicalDrive$getOffsetFacing().getOpposite()
                ? -offset
                : offset;
    }

    default double mechanicalDrive$getRenderedLongitudinalOffset(
            float partialTicks
    ) {
        return mechanicalDrive$getLerpedLongitudinalOffset(partialTicks);
    }

    default double mechanicalDrive$getRenderedHeightOffset(
            float partialTicks
    ) {
        return mechanicalDrive$getLerpedHeightOffset(partialTicks);
    }
}