package dev.createmechanicaldrive.content.tracks.chain;

import dev.createmechanicaldrive.content.tracks.mounts.sprocket.SprocketMountBlockEntity;
import dev.createmechanicaldrive.content.tracks.wheels.idler.IdlerWheelItem;
import dev.createmechanicaldrive.content.tracks.wheels.sprocket.SprocketWheelItem;
import dev.createmechanicaldrive.content.tracks.wheels.support.SupportWheelItem;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Client-side track animation driven by the sprocket's continuous visual
 * angle. Keeping every linked part on this clock prevents speed changes from
 * jumping the chain phase or desynchronising the wheels.
 */
public final class TrackKineticVisuals {
    private static final Map<SprocketMountBlockEntity, TrackPhase>
            TRACK_PHASES = new WeakHashMap<>();

    private TrackKineticVisuals() {
    }

    public static float sprocketAngle(
            SprocketMountBlockEntity sprocket,
            float partialTick
    ) {
        return wrap(sprocket.getLerpedVisualAngle(partialTick));
    }

    public static double sprocketTravel(
            SprocketMountBlockEntity sprocket,
            float partialTick
    ) {
        return sprocket.getLerpedVisualAngle(partialTick)
                * SprocketWheelItem.RADIUS;
    }

    public static double stableTrackPhase(
            SprocketMountBlockEntity sprocket,
            float partialTick,
            double spacing
    ) {
        double movement = -sprocketTravel(sprocket, partialTick);
        TrackPhase state = TRACK_PHASES.computeIfAbsent(
                sprocket,
                ignored -> new TrackPhase()
        );
        if (!state.initialized) {
            state.phase = positiveModulo(movement, spacing);
            state.previousMovement = movement;
            state.initialized = true;
            return state.phase;
        }

        state.phase += movement - state.previousMovement;
        state.previousMovement = movement;
        state.phase = positiveModulo(state.phase, spacing);
        return state.phase;
    }

    public static float driveWheelAngle(
            SprocketMountBlockEntity sprocket,
            double armRotation,
            float wheelRadius,
            float partialTick
    ) {
        double angle = -sprocket.getLerpedVisualAngle(partialTick)
                * SprocketWheelItem.RADIUS
                / wheelRadius
                - armRotation;
        return wrap(angle);
    }

    public static float idlerWheelAngle(
            SprocketMountBlockEntity sprocket,
            float partialTick
    ) {
        double angle = sprocket.getLerpedVisualAngle(partialTick)
                * SprocketWheelItem.RADIUS
                / IdlerWheelItem.RADIUS;
        return wrap(angle);
    }

    public static float supportWheelAngle(
            SprocketMountBlockEntity sprocket,
            float partialTick
    ) {
        double angle = -sprocket.getLerpedVisualAngle(partialTick)
                * SprocketWheelItem.RADIUS
                / SupportWheelItem.RADIUS;
        return wrap(angle);
    }

    private static float wrap(double angle) {
        return (float) Math.IEEEremainder(angle, Math.PI * 2.0D);
    }

    private static double positiveModulo(double value, double divisor) {
        double result = value % divisor;
        return result < 0.0D ? result + divisor : result;
    }

    private static final class TrackPhase {
        private double previousMovement;
        private double phase;
        private boolean initialized;
    }
}
