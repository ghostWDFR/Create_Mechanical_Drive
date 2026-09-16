package dev.createmechanicaldrive.creative;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.function.Supplier;

public record CreativeTabSection(
        String id,
        Component title,
        int titleColor,
        int titleBackgroundColor,
        int frameTimeMs,
        List<ResourceLocation> frames,
        List<Supplier<? extends Item>> items
) {
}