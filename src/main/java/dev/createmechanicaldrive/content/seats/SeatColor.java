package dev.createmechanicaldrive.content.seats;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.DyeColor;

public enum SeatColor
        implements StringRepresentable {

    BLACK("black", DyeColor.BLACK),
    BLUE("blue", DyeColor.BLUE),
    GRAY("gray", DyeColor.GRAY),
    GREEN("green", DyeColor.GREEN),
    RED("red", DyeColor.RED),
    YELLOW("yellow", DyeColor.YELLOW);

    private final String name;
    private final DyeColor dyeColor;

    SeatColor(
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

    public static SeatColor fromDyeColor(
            DyeColor dyeColor
    ) {
        for (SeatColor color : values()) {
            if (color.dyeColor == dyeColor) {
                return color;
            }
        }

        return null;
    }
}
