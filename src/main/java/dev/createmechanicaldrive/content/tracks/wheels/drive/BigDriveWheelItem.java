package dev.createmechanicaldrive.content.tracks.wheels.drive;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class BigDriveWheelItem extends Item {
    public static final float DIAMETER = 22.0F / 16.0F;
    public static final float RADIUS = DIAMETER / 2.0F;

    public BigDriveWheelItem(Properties properties) {
        super(properties);
    }

    public static boolean isBigDriveWheel(ItemStack stack) {
        return stack.getItem() instanceof BigDriveWheelItem;
    }
}
