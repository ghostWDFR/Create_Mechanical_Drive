package dev.createmechanicaldrive.content.seats;

import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;

public enum SeatBackPosition implements StringRepresentable {
    FORWARD("forward", -1.0F),
    UPRIGHT("upright", 0.0F),
    BACKWARD("backward", 1.0F);

    private static final float FORWARD_ANGLE_DEGREES =
            -45.0F;

    private static final float BACKWARD_ANGLE_DEGREES =
            22.5F;

    private static final SeatBackPosition[] BY_ID =
            values();

    private final String serializedName;
    private final float recline;

    SeatBackPosition(
            String serializedName,
            float recline
    ) {
        this.serializedName =
                serializedName;

        this.recline =
                recline;
    }

    public static SeatBackPosition byId(
            int id
    ) {
        if (
                id < 0
                        || id >= BY_ID.length
        ) {
            return UPRIGHT;
        }

        return BY_ID[id];
    }

    public static SeatBackPosition nearest(
            float rawRecline
    ) {
        float recline =
                Mth.clamp(
                        rawRecline,
                        -1.0F,
                        1.0F
                );

        SeatBackPosition nearest =
                UPRIGHT;

        float nearestDistance =
                Float.MAX_VALUE;

        for (SeatBackPosition position : values()) {
            float distance =
                    Math.abs(
                            recline
                                    - position.recline
                    );

            if (distance < nearestDistance) {
                nearest =
                        position;

                nearestDistance =
                        distance;
            }
        }

        return nearest;
    }

    public int id() {
        return ordinal();
    }

    public float recline() {
        return recline;
    }

    public static float angleDegrees(
            float rawRecline
    ) {
        float recline =
                Mth.clamp(
                        rawRecline,
                        -1.0F,
                        1.0F
                );

        if (recline < 0.0F) {
            return -recline
                    * FORWARD_ANGLE_DEGREES;
        }

        return recline
                * BACKWARD_ANGLE_DEGREES;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
