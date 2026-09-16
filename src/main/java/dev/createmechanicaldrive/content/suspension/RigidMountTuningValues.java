package dev.createmechanicaldrive.content.suspension;

import net.minecraft.nbt.CompoundTag;

public final class RigidMountTuningValues {

    private static final String DAMPING_TAG =
            "MechanicalDriveRigidDampingMultiplier";
    private static final String BUMP_CLEARANCE_TAG =
            "MechanicalDriveRigidBumpClearanceMultiplier";
    private static final String BUMP_FORCE_TAG =
            "MechanicalDriveRigidBumpForceMultiplier";
    private static final String MAX_IMPULSE_TAG =
            "MechanicalDriveRigidMaxImpulseMultiplier";
    private static final String DRIVE_TAG =
            "MechanicalDriveRigidDriveMultiplier";
    private static final String GRIP_TAG =
            "MechanicalDriveRigidGripMultiplier";

    private double damping = SuspensionSpringTuning.DEFAULT_MULTIPLIER;
    private double bumpClearance = SuspensionSpringTuning.DEFAULT_MULTIPLIER;
    private double bumpForce = SuspensionSpringTuning.DEFAULT_MULTIPLIER;
    private double maxImpulse = SuspensionSpringTuning.DEFAULT_MULTIPLIER;
    private double drive = SuspensionSpringTuning.DEFAULT_MULTIPLIER;
    private double grip = SuspensionSpringTuning.DEFAULT_MULTIPLIER;

    public double adjust(String tuning, int steps) {
        double value = SuspensionSpringTuning.steppedMultiplier(
                tuning,
                get(tuning),
                steps
        );
        set(tuning, value);
        return value;
    }

    public double get(String tuning) {
        return switch (tuning) {
            case SuspensionSpringTuning.DAMPING -> damping;
            case SuspensionSpringTuning.BUMP_CLEARANCE -> bumpClearance;
            case SuspensionSpringTuning.BUMP_FORCE -> bumpForce;
            case SuspensionSpringTuning.MAX_IMPULSE -> maxImpulse;
            case SuspensionSpringTuning.DRIVE -> drive;
            case SuspensionSpringTuning.GRIP -> grip;
            default -> SuspensionSpringTuning.DEFAULT_MULTIPLIER;
        };
    }

    public void reset() {
        damping = SuspensionSpringTuning.DEFAULT_MULTIPLIER;
        bumpClearance = SuspensionSpringTuning.DEFAULT_MULTIPLIER;
        bumpForce = SuspensionSpringTuning.DEFAULT_MULTIPLIER;
        maxImpulse = SuspensionSpringTuning.DEFAULT_MULTIPLIER;
        drive = SuspensionSpringTuning.DEFAULT_MULTIPLIER;
        grip = SuspensionSpringTuning.DEFAULT_MULTIPLIER;
    }

    public void write(CompoundTag tag) {
        tag.putDouble(DAMPING_TAG, damping);
        tag.putDouble(BUMP_CLEARANCE_TAG, bumpClearance);
        tag.putDouble(BUMP_FORCE_TAG, bumpForce);
        tag.putDouble(MAX_IMPULSE_TAG, maxImpulse);
        tag.putDouble(DRIVE_TAG, drive);
        tag.putDouble(GRIP_TAG, grip);
    }

    public void read(CompoundTag tag) {
        damping = read(tag, DAMPING_TAG);
        bumpClearance = read(tag, BUMP_CLEARANCE_TAG);
        bumpForce = read(tag, BUMP_FORCE_TAG);
        maxImpulse = read(tag, MAX_IMPULSE_TAG);
        drive = read(tag, DRIVE_TAG);
        grip = read(tag, GRIP_TAG);
    }

    public static boolean supports(String tuning) {
        return SuspensionSpringTuning.DAMPING.equals(tuning)
                || SuspensionSpringTuning.BUMP_CLEARANCE.equals(tuning)
                || SuspensionSpringTuning.BUMP_FORCE.equals(tuning)
                || SuspensionSpringTuning.MAX_IMPULSE.equals(tuning)
                || SuspensionSpringTuning.DRIVE.equals(tuning)
                || SuspensionSpringTuning.GRIP.equals(tuning);
    }

    private void set(String tuning, double value) {
        switch (tuning) {
            case SuspensionSpringTuning.DAMPING -> damping = value;
            case SuspensionSpringTuning.BUMP_CLEARANCE ->
                    bumpClearance = value;
            case SuspensionSpringTuning.BUMP_FORCE -> bumpForce = value;
            case SuspensionSpringTuning.MAX_IMPULSE -> maxImpulse = value;
            case SuspensionSpringTuning.DRIVE -> drive = value;
            case SuspensionSpringTuning.GRIP -> grip = value;
            default -> {
            }
        }
    }

    private static double read(CompoundTag tag, String key) {
        return tag.contains(key)
                ? SuspensionSpringTuning.clampMultiplier(
                        tag.getDouble(key)
                )
                : SuspensionSpringTuning.DEFAULT_MULTIPLIER;
    }
}
