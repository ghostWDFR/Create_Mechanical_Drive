package dev.createmechanicaldrive.compat.tracks;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Optional bridge to the offset contract injected into Offroad mounts by
 * Tracks. Capability discovery is performed on the transformed target class,
 * so this creates neither a hard dependency nor a mod-id presence check.
 */
public final class TracksWheelMountOffsetBridge {

    private static final ClassValue<Methods> METHODS = new ClassValue<>() {
        @Override
        protected Methods computeValue(Class<?> type) {
            try {
                return new Methods(
                        type.getMethod("tracks$adjustLateralOffset", int.class),
                        type.getMethod("tracks$adjustLongitudinalOffset", int.class),
                        type.getMethod("tracks$adjustHeightOffset", int.class),
                        type.getMethod("tracks$getLerpedLateralOffset", float.class),
                        type.getMethod("tracks$getLerpedLongitudinalOffset", float.class),
                        type.getMethod("tracks$getLerpedHeightOffset", float.class)
                );
            } catch (NoSuchMethodException ignored) {
                return Methods.UNAVAILABLE;
            }
        }
    };

    private TracksWheelMountOffsetBridge() {
    }

    public static boolean isAvailable(Object target) {
        return target != null && METHODS.get(target.getClass()).available();
    }

    public static Double adjustLateral(Object target, int steps) {
        return invoke(METHODS.get(target.getClass()).adjustLateral(), target, steps);
    }

    public static Double adjustLongitudinal(Object target, int steps) {
        return invoke(METHODS.get(target.getClass()).adjustLongitudinal(), target, steps);
    }

    public static Double adjustHeight(Object target, int steps) {
        return invoke(METHODS.get(target.getClass()).adjustHeight(), target, steps);
    }

    public static Double getLerpedLateral(Object target, float partialTicks) {
        return invoke(METHODS.get(target.getClass()).getLerpedLateral(), target, partialTicks);
    }

    public static Double getLerpedLongitudinal(Object target, float partialTicks) {
        return invoke(METHODS.get(target.getClass()).getLerpedLongitudinal(), target, partialTicks);
    }

    public static Double getLerpedHeight(Object target, float partialTicks) {
        return invoke(METHODS.get(target.getClass()).getLerpedHeight(), target, partialTicks);
    }

    private static Double invoke(Method method, Object target, Object argument) {
        if (method == null || target == null) {
            return null;
        }
        try {
            Object result = method.invoke(target, argument);
            return result instanceof Number number ? number.doubleValue() : null;
        } catch (IllegalAccessException
                 | InvocationTargetException
                 | IllegalArgumentException ignored) {
            return null;
        }
    }

    private record Methods(
            Method adjustLateral,
            Method adjustLongitudinal,
            Method adjustHeight,
            Method getLerpedLateral,
            Method getLerpedLongitudinal,
            Method getLerpedHeight
    ) {
        private static final Methods UNAVAILABLE =
                new Methods(null, null, null, null, null, null);

        private boolean available() {
            return adjustLateral != null
                    && adjustLongitudinal != null
                    && adjustHeight != null
                    && getLerpedLateral != null
                    && getLerpedLongitudinal != null
                    && getLerpedHeight != null;
        }
    }
}
