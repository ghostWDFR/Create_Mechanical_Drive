package dev.createmechanicaldrive.content.steering_wheel;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.DyeColor;

public enum SteeringWheelColor
        implements StringRepresentable {

    BLACK("black", DyeColor.BLACK),
    BLUE("blue", DyeColor.BLUE),
    GRAY("gray", DyeColor.GRAY),
    GREEN("green", DyeColor.GREEN),
    RED("red", DyeColor.RED),
    YELLOW("yellow", DyeColor.YELLOW);

    private final String name;
    private final DyeColor dyeColor;

    SteeringWheelColor(
            String name,
            DyeColor dyeColor
    ) {
        this.name =
                name;

        this.dyeColor =
                dyeColor;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public static SteeringWheelColor fromDyeColor(
            DyeColor dyeColor
    ) {
        for (SteeringWheelColor color : values()) {
            if (color.dyeColor == dyeColor) {
                return color;
            }
        }

        return null;
    }
}