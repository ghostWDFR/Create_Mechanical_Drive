package dev.createmechanicaldrive.content.tracks.chain;

import org.jetbrains.annotations.Nullable;

public enum TrackType {
    NARROW("narrow", 1.0D / 16.0D),
    WIDE("wide", 1.0D / 16.0D);

    /** The unscaled longitudinal size and nominal pitch of a track link. */
    public static final double LINK_MODEL_LENGTH = 8.0D / 16.0D;
    public static final double LINK_PITCH = LINK_MODEL_LENGTH;

    private final String serializedName;
    private final double groundContactOffset;

    TrackType(String serializedName, double groundContactOffset) {
        this.serializedName = serializedName;
        this.groundContactOffset = groundContactOffset;
    }

    public String serializedName() {
        return serializedName;
    }

    public double groundContactOffset() {
        return groundContactOffset;
    }

    @Nullable
    public static TrackType fromSerializedName(String name) {
        for (TrackType type : values()) {
            if (type.serializedName.equals(name)) {
                return type;
            }
        }
        return null;
    }
}
