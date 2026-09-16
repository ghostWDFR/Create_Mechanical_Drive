package dev.createmechanicaldrive.content.shaft_marker;

import dev.ryanhcode.offroad.index.OffroadDataComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.Block;

public class ShaftMarkerItem extends BlockItem {
    public ShaftMarkerItem(Block block, Properties properties) {
        super(block, properties);
    }

    public static ItemStack colored(ItemStack stack, DyeColor color) {
        stack.set(
                DataComponents.BLOCK_STATE,
                BlockItemStateProperties.EMPTY.with(
                        ShaftMarkerBlock.COLOR,
                        color
                )
        );
        return stack;
    }

    public static DyeColor getColor(ItemStack stack) {
        DyeColor color = stack.getOrDefault(
                DataComponents.BLOCK_STATE,
                BlockItemStateProperties.EMPTY
        ).get(ShaftMarkerBlock.COLOR);
        return color == null ? DyeColor.WHITE : color;
    }

    public static boolean isMarker(ItemStack stack) {
        return stack.getItem() instanceof ShaftMarkerItem;
    }

    public static boolean isWheelSlotItem(ItemStack stack) {
        return stack.has(OffroadDataComponents.TIRE)
                || isMarker(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(
                "item.mechanical_drive.shaft_marker."
                        + getColor(stack).getName()
        );
    }
}
