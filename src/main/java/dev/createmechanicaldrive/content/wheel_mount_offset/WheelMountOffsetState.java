package dev.createmechanicaldrive.content.wheel_mount_offset;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

public final class WheelMountOffsetState {

    public static final String LATERAL_NBT_KEY = "TracksLateralOffset";
    public static final String LONGITUDINAL_NBT_KEY = "TracksLongitudinalOffset";
    public static final String HEIGHT_NBT_KEY = "TracksHeightOffset";

    private static final double STEP = 0.125D;
    private static final double MIN_HORIZONTAL_OFFSET = -1.0D;
    private static final double MAX_HORIZONTAL_OFFSET = 1.5D;
    private static final double MIN_HEIGHT_OFFSET = -0.75D;
    private static final double MAX_HEIGHT_OFFSET = 0.75D;

    private double lateralOffset;
    private double previousLateralOffset;
    private double longitudinalOffset;
    private double previousLongitudinalOffset;
    private double heightOffset;
    private double previousHeightOffset;

    public double adjustLateral(int steps) {
        lateralOffset = stepped(
                lateralOffset,
                steps,
                MIN_HORIZONTAL_OFFSET,
                MAX_HORIZONTAL_OFFSET
        );
        return lateralOffset;
    }

    public double adjustLongitudinal(int steps) {
        longitudinalOffset = stepped(
                longitudinalOffset,
                steps,
                MIN_HORIZONTAL_OFFSET,
                MAX_HORIZONTAL_OFFSET
        );
        return longitudinalOffset;
    }

    public double adjustHeight(int steps) {
        heightOffset = stepped(
                heightOffset,
                steps,
                MIN_HEIGHT_OFFSET,
                MAX_HEIGHT_OFFSET
        );
        return heightOffset;
    }

    public void tick() {
        previousLateralOffset = lateralOffset;
        previousLongitudinalOffset = longitudinalOffset;
        previousHeightOffset = heightOffset;
    }

    public double getLerpedLateral(float partialTicks) {
        return Mth.lerp(partialTicks, previousLateralOffset, lateralOffset);
    }

    public double getLerpedLongitudinal(float partialTicks) {
        return Mth.lerp(partialTicks, previousLongitudinalOffset, longitudinalOffset);
    }

    public double getLerpedHeight(float partialTicks) {
        return Mth.lerp(partialTicks, previousHeightOffset, heightOffset);
    }

    public double getMaximumAbsoluteOffset() {
        return Math.abs(lateralOffset)
                + Math.abs(longitudinalOffset)
                + Math.abs(heightOffset);
    }

    public void write(CompoundTag tag) {
        tag.putDouble(LATERAL_NBT_KEY, lateralOffset);
        tag.putDouble(LONGITUDINAL_NBT_KEY, longitudinalOffset);
        tag.putDouble(HEIGHT_NBT_KEY, heightOffset);
    }

    public void read(CompoundTag tag, boolean clientPacket) {
        lateralOffset = readClamped(
                tag,
                LATERAL_NBT_KEY,
                MIN_HORIZONTAL_OFFSET,
                MAX_HORIZONTAL_OFFSET
        );
        longitudinalOffset = readClamped(
                tag,
                LONGITUDINAL_NBT_KEY,
                MIN_HORIZONTAL_OFFSET,
                MAX_HORIZONTAL_OFFSET
        );
        heightOffset = readClamped(
                tag,
                HEIGHT_NBT_KEY,
                MIN_HEIGHT_OFFSET,
                MAX_HEIGHT_OFFSET
        );

        if (clientPacket) {
            previousLateralOffset = lateralOffset;
            previousLongitudinalOffset = longitudinalOffset;
            previousHeightOffset = heightOffset;
        }
    }

    private static double stepped(
            double value,
            int steps,
            double minimum,
            double maximum
    ) {
        return Mth.clamp(
                Math.round((value + steps * STEP) / STEP) * STEP,
                minimum,
                maximum
        );
    }

    private static double readClamped(
            CompoundTag tag,
            String key,
            double minimum,
            double maximum
    ) {
        return tag.contains(key)
                ? Mth.clamp(tag.getDouble(key), minimum, maximum)
                : 0.0D;
    }
}
