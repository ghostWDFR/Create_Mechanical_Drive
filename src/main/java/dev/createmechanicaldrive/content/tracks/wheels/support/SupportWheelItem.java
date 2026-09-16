package dev.createmechanicaldrive.content.tracks.wheels.support;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class SupportWheelItem extends Item {
    public static final float RADIUS = 3.25F / 16.0F;

    public SupportWheelItem(Properties properties) {
        super(properties);
    }

    public static boolean isSupportWheel(ItemStack stack) {
        return stack.getItem() instanceof SupportWheelItem;
    }
}
