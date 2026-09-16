package dev.createmechanicaldrive.content.tracks.wheels.sprocket;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class SprocketWheelItem extends Item {
    public static final float RADIUS = 7.0F / 16.0F;

    public SprocketWheelItem(Properties properties) {
        super(properties);
    }

    public static boolean isSprocketWheel(ItemStack stack) {
        return stack.getItem() instanceof SprocketWheelItem;
    }
}
