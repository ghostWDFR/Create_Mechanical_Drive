package dev.createmechanicaldrive.content.suspension_strut;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.Locale;

/**
 * Persisted, copyable tuning values for a Suspension Strut.
 *
 * <p>The multipliers are deliberately expressed relative to Aeronautics'
 * medium spring.  A strength of {@value #DEFAULT_STRENGTH} therefore matches
 * Aeronautics' strengthened (large) spring.</p>
 */
public final class SuspensionStrutSettings {
    public static final String STRENGTH_TAG = "Strength";
    public static final String LENGTH_TAG = "Length";
    public static final String DAMPING_TAG = "Damping";
    public static final String MAX_IMPULSE_TAG = "MaxImpulse";
    public static final String CONSTRAINT_TAG = "Constraint";
    public static final String BEHAVIOR_TAG = "Behavior";

    public static final double DEFAULT_STRENGTH = 8.0D;
    public static final double DEFAULT_DAMPING = 1.0D;
    public static final double DEFAULT_MAX_IMPULSE = 64.0D;
    public static final double DEFAULT_CONSTRAINT = 8.0D;

    public static final double MIN_STRENGTH = 0.25D;
    public static final double MAX_STRENGTH = 16.0D;
    public static final double MIN_DAMPING = 0.0D;
    public static final double MAX_DAMPING = 12.0D;
    public static final double MIN_MAX_IMPULSE = 4.0D;
    public static final double MAX_MAX_IMPULSE = 256.0D;
    public static final double MIN_CONSTRAINT = 0.25D;
    public static final double MAX_CONSTRAINT = 16.0D;

    public static final double MULTIPLIER_STEP = 0.25D;
    public static final double LENGTH_STEP = 0.25D;
    public static final double MAX_IMPULSE_STEP = 4.0D;

    private double strength;
    private double length;
    private double damping;
    private double maxImpulse;
    private double constraint;
    private Behavior behavior;

    public SuspensionStrutSettings(double length) {
        reset(length);
    }

    public double strength() {
        return strength;
    }

    public double length() {
        return length;
    }

    public double damping() {
        return damping;
    }

    public double maxImpulse() {
        return maxImpulse;
    }

    public double constraint() {
        return constraint;
    }

    public Behavior behavior() {
        return behavior;
    }

    public void setStrength(double value) {
        strength = snap(value, MULTIPLIER_STEP, MIN_STRENGTH, MAX_STRENGTH);
    }

    public void setLength(double value) {
        length = snap(
                value,
                LENGTH_STEP,
                SuspensionStrutBlockEntity.MIN_LENGTH,
                SuspensionStrutBlockEntity.MAX_LENGTH
        );
    }

    public void setDamping(double value) {
        damping = snap(value, MULTIPLIER_STEP, MIN_DAMPING, MAX_DAMPING);
    }

    public void setMaxImpulse(double value) {
        maxImpulse = snap(
                value,
                MAX_IMPULSE_STEP,
                MIN_MAX_IMPULSE,
                MAX_MAX_IMPULSE
        );
    }

    public void setConstraint(double value) {
        constraint = snap(value, MULTIPLIER_STEP, MIN_CONSTRAINT, MAX_CONSTRAINT);
    }

    public void setBehavior(Behavior value) {
        behavior = value == null ? Behavior.ALIGN : value;
    }

    public void reset(double currentLength) {
        strength = DEFAULT_STRENGTH;
        length = clamp(
                currentLength,
                SuspensionStrutBlockEntity.MIN_LENGTH,
                SuspensionStrutBlockEntity.MAX_LENGTH
        );
        damping = DEFAULT_DAMPING;
        maxImpulse = DEFAULT_MAX_IMPULSE;
        constraint = DEFAULT_CONSTRAINT;
        behavior = Behavior.ALIGN;
    }

    public void copyFrom(SuspensionStrutSettings other) {
        setStrength(other.strength);
        setLength(other.length);
        setDamping(other.damping);
        setMaxImpulse(other.maxImpulse);
        setConstraint(other.constraint);
        setBehavior(other.behavior);
    }

    public CompoundTag write() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble(STRENGTH_TAG, strength);
        tag.putDouble(LENGTH_TAG, length);
        tag.putDouble(DAMPING_TAG, damping);
        tag.putDouble(MAX_IMPULSE_TAG, maxImpulse);
        tag.putDouble(CONSTRAINT_TAG, constraint);
        tag.putString(BEHAVIOR_TAG, behavior.serializedName());
        return tag;
    }

    public boolean read(CompoundTag tag) {
        if (!isValid(tag)) {
            return false;
        }

        setStrength(tag.getDouble(STRENGTH_TAG));
        setLength(tag.getDouble(LENGTH_TAG));
        setDamping(tag.getDouble(DAMPING_TAG));
        setMaxImpulse(tag.getDouble(MAX_IMPULSE_TAG));
        setConstraint(tag.getDouble(CONSTRAINT_TAG));
        setBehavior(Behavior.fromSerializedName(tag.getString(BEHAVIOR_TAG)));
        return true;
    }

    public static boolean isValid(CompoundTag tag) {
        if (!(tag.contains(STRENGTH_TAG, Tag.TAG_ANY_NUMERIC)
                && tag.contains(LENGTH_TAG, Tag.TAG_ANY_NUMERIC)
                && tag.contains(DAMPING_TAG, Tag.TAG_ANY_NUMERIC)
                && tag.contains(MAX_IMPULSE_TAG, Tag.TAG_ANY_NUMERIC)
                && tag.contains(CONSTRAINT_TAG, Tag.TAG_ANY_NUMERIC)
                && tag.contains(BEHAVIOR_TAG, Tag.TAG_STRING)
                && Behavior.isKnown(tag.getString(BEHAVIOR_TAG)))) {
            return false;
        }
        return Double.isFinite(tag.getDouble(STRENGTH_TAG))
                && Double.isFinite(tag.getDouble(LENGTH_TAG))
                && Double.isFinite(tag.getDouble(DAMPING_TAG))
                && Double.isFinite(tag.getDouble(MAX_IMPULSE_TAG))
                && Double.isFinite(tag.getDouble(CONSTRAINT_TAG));
    }

    private static double snap(
            double value,
            double step,
            double minimum,
            double maximum
    ) {
        if (!Double.isFinite(value)) {
            return minimum;
        }
        return clamp(Math.round(value / step) * step, minimum, maximum);
    }

    private static double clamp(double value, double minimum, double maximum) {
        if (!Double.isFinite(value)) {
            return minimum;
        }
        return Math.max(minimum, Math.min(maximum, value));
    }

    public enum Behavior {
        ALIGN("align"),
        AXIAL("axial"),
        LOCKED_AXIS("locked_axis");

        private final String serializedName;

        Behavior(String serializedName) {
            this.serializedName = serializedName;
        }

        public String serializedName() {
            return serializedName;
        }

        public String translationKey() {
            return "item.mechanical_drive.spring_tuning_wrench.behavior."
                    + serializedName;
        }

        public String descriptionKey() {
            return translationKey() + ".description";
        }

        public Behavior cycle(int direction) {
            Behavior[] values = values();
            return values[Math.floorMod(ordinal() + direction, values.length)];
        }

        public static Behavior fromSerializedName(String name) {
            String normalized = name == null
                    ? ""
                    : name.toLowerCase(Locale.ROOT);
            for (Behavior value : values()) {
                if (value.serializedName.equals(normalized)) {
                    return value;
                }
            }
            return ALIGN;
        }

        private static boolean isKnown(String name) {
            if (name == null) {
                return false;
            }
            for (Behavior value : values()) {
                if (value.serializedName.equalsIgnoreCase(name)) {
                    return true;
                }
            }
            return false;
        }
    }
}
