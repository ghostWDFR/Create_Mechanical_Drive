package dev.createmechanicaldrive.content.suspension;

public interface SpringTuningWrenchTarget {

    String REST_ANGLE = "rest_angle";
    String REST_ANGLE_NBT_KEY = "MechanicalDriveRestAngleDegrees";

    double DEFAULT_REST_ANGLE_DEGREES = 30.0D;
    double MIN_REST_ANGLE_DEGREES = -20.0D;
    double MAX_REST_ANGLE_DEGREES = 50.0D;
    double REST_ANGLE_STEP_DEGREES = 5.0D;

    double mechanicalDrive$adjustSpringWrenchStrength(int steps);

    double mechanicalDrive$getSpringWrenchStrength();

    void mechanicalDrive$setSpringWrenchStrength(double multiplier);

    double mechanicalDrive$adjustSpringWrenchRestAngle(int steps);

    double mechanicalDrive$getSpringWrenchRestAngle();

    void mechanicalDrive$setSpringWrenchRestAngle(double degrees);

    void mechanicalDrive$resetSpringWrenchTuning();

    static double steppedRestAngle(double current, int steps) {
        double stepped = Math.round(
                (current + steps * REST_ANGLE_STEP_DEGREES)
                        / REST_ANGLE_STEP_DEGREES
        ) * REST_ANGLE_STEP_DEGREES;
        return clampRestAngle(stepped);
    }

    static double clampRestAngle(double degrees) {
        return Math.max(
                MIN_REST_ANGLE_DEGREES,
                Math.min(MAX_REST_ANGLE_DEGREES, degrees)
        );
    }
}
