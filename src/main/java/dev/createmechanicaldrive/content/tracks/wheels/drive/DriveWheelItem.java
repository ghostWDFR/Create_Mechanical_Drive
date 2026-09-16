package dev.createmechanicaldrive.content.tracks.wheels.drive;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class DriveWheelItem extends Item {
    public static final float RADIUS = 7.0F / 16.0F;

    public DriveWheelItem(Properties properties) {
        super(properties);
    }

    public static boolean isDriveWheel(ItemStack stack) {
        return stack.getItem() instanceof DriveWheelItem;
    }
}
