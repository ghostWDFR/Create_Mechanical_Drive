package dev.createmechanicaldrive.content.gearbox;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.Mth;

import java.util.Locale;

public enum GearboxPosition implements StringRepresentable {
    FIRST("first", -1.0F, -1.0F, 0.5F, 0.0F),
    SECOND("second", 0.0F, -1.0F, 1.0F, -0.5F),
    THIRD("third", 1.0F, -1.0F, 1.5F, -1.0F),
    NEUTRAL("neutral", -1.0F, 1.0F, 0.0F, 0.5F),
    REVERSE("reverse", 0.0F, 1.0F, -0.5F, 1.0F);

    private static final float VERTICAL_SLOT_GRAB_WIDTH = 0.24F;
    private static final float CROSS_SLOT_GRAB_HEIGHT = 0.12F;
    private static final GearboxPosition[] BY_ID = values();

    private final String serializedName;
    private final float gateX;
    private final float gateY;
    private final float speedMultiplier;
    private final float linearValue;

    GearboxPosition(
            String serializedName,
            float gateX,
            float gateY,
            float speedMultiplier,
            float linearValue
    ) {
        this.serializedName = serializedName;
        this.gateX = gateX;
        this.gateY = gateY;
        this.speedMultiplier = speedMultiplier;
        this.linearValue = linearValue;
    }

    public static GearboxPosition byId(int id) {
        if (id < 0 || id >= BY_ID.length) {
            return NEUTRAL;
        }

        return BY_ID[id];
    }

    public static GearboxPosition nearest(float rawX, float rawY) {
        GatePoint point = projectToGate(rawX, rawY);
        GearboxPosition nearest = NEUTRAL;
        float nearestDistance = Float.MAX_VALUE;

        for (GearboxPosition position : values()) {
            float dx = point.x - position.gateX;
            float dy = point.y - position.gateY;
            float distance = dx * dx + dy * dy;
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = position;
            }
        }

        return nearest;
    }

    public static GearboxPosition nearestLinear(float rawLinear) {
        float linear = Mth.clamp(rawLinear, -1.0F, 1.0F);

        GearboxPosition nearest = NEUTRAL;
        float nearestDistance = Float.MAX_VALUE;

        for (GearboxPosition position : values()) {
            float distance = Math.abs(linear - position.linearValue);

            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = position;
            }
        }

        return nearest;
    }

    public static GatePoint projectToGate(float rawX, float rawY) {
        float x = Mth.clamp(rawX, -1.0F, 1.0F);
        float y = Mth.clamp(rawY, -1.0F, 1.0F);

        GatePoint best = new GatePoint(-1.0F, Mth.clamp(y, -1.0F, 1.0F));
        float bestDistance = verticalSlotDistance(x, y, best.x, -1.0F, 1.0F);

        GatePoint secondLane = new GatePoint(0.0F, Mth.clamp(y, -1.0F, 1.0F));
        float secondDistance = verticalSlotDistance(x, y, secondLane.x, -1.0F, 1.0F);
        if (secondDistance < bestDistance) {
            best = secondLane;
            bestDistance = secondDistance;
        }

        GatePoint thirdLane = new GatePoint(1.0F, Mth.clamp(y, -1.0F, 0.0F));
        float thirdDistance = verticalSlotDistance(x, y, thirdLane.x, -1.0F, 0.0F);
        if (thirdDistance < bestDistance) {
            best = thirdLane;
            bestDistance = thirdDistance;
        }

        GatePoint crossGate = new GatePoint(Mth.clamp(x, -1.0F, 1.0F), 0.0F);
        float crossDistance = crossSlotDistance(y);
        if (crossDistance <= bestDistance) {
            best = crossGate;
        }

        return best;
    }

    private static float verticalSlotDistance(float x, float y, float laneX, float minY, float maxY) {
        float dx = Math.max(Math.abs(x - laneX) - VERTICAL_SLOT_GRAB_WIDTH, 0.0F);
        float dy = y < minY ? minY - y : y > maxY ? y - maxY : 0.0F;
        return dx * dx + dy * dy;
    }

    private static float crossSlotDistance(float y) {
        float dy = Math.max(Math.abs(y) - CROSS_SLOT_GRAB_HEIGHT, 0.0F);
        return dy * dy;
    }

    private static float distanceSquared(float ax, float ay, float bx, float by) {
        float dx = ax - bx;
        float dy = ay - by;
        return dx * dx + dy * dy;
    }

    public int id() {
        return ordinal();
    }

    public float gateX() {
        return gateX;
    }

    public float gateY() {
        return gateY;
    }

    public float speedMultiplier() {
        return speedMultiplier;
    }

    public float linearValue() {
        return linearValue;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    public String label() {
        return serializedName.toUpperCase(Locale.ROOT);
    }

    public MutableComponent labelComponent() {
        return Component.translatable(
                "mechanical_drive.gearbox.position." + serializedName
        );
    }

    public record GatePoint(float x, float y) {
    }
}
