package dev.createmechanicaldrive.content.tracks.wheels.idler;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class IdlerWheelItem extends Item {
    public static final float RADIUS = 7.0F / 16.0F;

    public IdlerWheelItem(Properties properties) {
        super(properties);
    }

    public static boolean isIdlerWheel(ItemStack stack) {
        return stack.getItem() instanceof IdlerWheelItem;
    }
}
