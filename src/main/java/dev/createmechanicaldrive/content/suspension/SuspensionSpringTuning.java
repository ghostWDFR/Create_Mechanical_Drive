package dev.createmechanicaldrive.content.suspension;

public interface SuspensionSpringTuning {

    String SPRING = "spring";
    String DAMPING = "damping";
    String BUMP_CLEARANCE = "bump_clearance";
    String BUMP_FORCE = "bump_force";
    String MAX_IMPULSE = "max_impulse";
    String DRIVE = "drive";
    String GRIP = "grip";

    String SPRING_NBT_KEY = "TracksWheelSpringMultiplier";
    String DAMPING_NBT_KEY = "MechanicalDriveWheelDampingMultiplier";
    String BUMP_CLEARANCE_NBT_KEY =
            "MechanicalDriveWheelBumpClearanceMultiplier";
    String BUMP_FORCE_NBT_KEY = "MechanicalDriveWheelBumpForceMultiplier";
    String MAX_IMPULSE_NBT_KEY =
            "MechanicalDriveWheelMaxImpulseMultiplier";
    String DRIVE_NBT_KEY = "TracksWheelDriveMultiplier";
    String GRIP_NBT_KEY = "TracksWheelGripMultiplier";
    String NBT_KEY = SPRING_NBT_KEY;

    double DEFAULT_MULTIPLIER = 1.0D;
    double MIN_MULTIPLIER = 0.1D;
    double MAX_MULTIPLIER = 4.0D;
    double SPRING_AND_GRIP_STEP = 0.05D;
    double DRIVE_STEP = 0.1D;

    double mechanicalDrive$adjustSuspensionTuning(
            String tuning,
            int steps
    );

    double mechanicalDrive$getSuspensionTuning(String tuning);

    void mechanicalDrive$resetSuspensionTuning();

    default double mechanicalDrive$adjustSuspensionSpring(int steps) {
        return mechanicalDrive$adjustSuspensionTuning(SPRING, steps);
    }

    default double mechanicalDrive$getSuspensionSpringMultiplier() {
        return mechanicalDrive$getSuspensionTuning(SPRING);
    }

    default void mechanicalDrive$resetSuspensionSpring() {
        mechanicalDrive$resetSuspensionTuning();
    }

    default boolean mechanicalDrive$supportsSuspensionTuning(
            String tuning
    ) {
        return SPRING.equals(tuning)
                || DAMPING.equals(tuning)
                || BUMP_CLEARANCE.equals(tuning)
                || BUMP_FORCE.equals(tuning)
                || MAX_IMPULSE.equals(tuning)
                || DRIVE.equals(tuning)
                || GRIP.equals(tuning);
    }


    static double steppedMultiplier(double current, int steps) {
        return steppedMultiplier(SPRING, current, steps);
    }

    static double steppedMultiplier(
            String tuning,
            double current,
            int steps
    ) {
        double step = DRIVE.equals(tuning)
                ? DRIVE_STEP
                : SPRING_AND_GRIP_STEP;
        double stepped = Math.round(
                (current + steps * step) / step
        ) * step;

        return clampMultiplier(stepped);
    }

    static double clampMultiplier(double multiplier) {
        return Math.max(
                MIN_MULTIPLIER,
                Math.min(MAX_MULTIPLIER, multiplier)
        );
    }
}
