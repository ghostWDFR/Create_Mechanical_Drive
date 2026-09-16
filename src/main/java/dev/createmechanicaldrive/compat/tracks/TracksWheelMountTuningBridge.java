package dev.createmechanicaldrive.compat.tracks;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Bridges to the public contract injected by Create Tracks without linking
 * Mechanical Drive against any Tracks class or checking whether the mod is
 * installed. The transformed target class itself advertises the capability.
 */
public final class TracksWheelMountTuningBridge {

    private static final ClassValue<Methods> METHODS =
            new ClassValue<>() {
                @Override
                protected Methods computeValue(Class<?> type) {
                    try {
                        return new Methods(
                                type.getMethod(
                                        "tracks$adjustTuning",
                                        String.class,
                                        int.class
                                ),
                                type.getMethod(
                                        "tracks$getTuning",
                                        String.class
                                ),
                                type.getMethod("tracks$resetTuning")
                        );
                    } catch (NoSuchMethodException ignored) {
                        return Methods.UNAVAILABLE;
                    }
                }
            };

    private TracksWheelMountTuningBridge() {
    }

    public static boolean isAvailable(Object target) {
        return target != null
                && METHODS.get(target.getClass()).available();
    }

    public static Double adjustSpring(Object target, int steps) {
        return adjustTuning(target, "spring", steps);
    }

    public static Double adjustTuning(
            Object target,
            String tuning,
            int steps
    ) {
        Methods methods = methods(target);

        if (!methods.available()) {
            return null;
        }

        Object result = invoke(
                methods.adjust(),
                target,
                tuning,
                steps
        );

        return result instanceof Number number
                ? number.doubleValue()
                : null;
    }

    public static Double getSpring(Object target) {
        return getTuning(target, "spring");
    }

    public static Double getTuning(Object target, String tuning) {
        Methods methods = methods(target);

        if (!methods.available()) {
            return null;
        }

        Object result = invoke(
                methods.get(),
                target,
                tuning
        );

        return result instanceof Number number
                ? number.doubleValue()
                : null;
    }

    public static boolean reset(Object target) {
        Methods methods = methods(target);

        if (!methods.available()) {
            return false;
        }

        return invoke(methods.reset(), target) != InvocationFailure.INSTANCE;
    }

    private static Methods methods(Object target) {
        return target == null
                ? Methods.UNAVAILABLE
                : METHODS.get(target.getClass());
    }

    private static Object invoke(
            Method method,
            Object target,
            Object... arguments
    ) {
        try {
            return method.invoke(target, arguments);
        } catch (IllegalAccessException
                 | InvocationTargetException
                 | IllegalArgumentException ignored) {
            return InvocationFailure.INSTANCE;
        }
    }

    private record Methods(
            Method adjust,
            Method get,
            Method reset
    ) {
        private static final Methods UNAVAILABLE =
                new Methods(null, null, null);

        private boolean available() {
            return adjust != null && get != null && reset != null;
        }
    }

    private enum InvocationFailure {
        INSTANCE
    }
}
