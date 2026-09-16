package dev.createmechanicaldrive.content.hand_crank;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

public class HandCrankItem
        extends BlockItem {

    public HandCrankItem(
            HandCrankBlock block,
            Item.Properties properties
    ) {
        super(
                block,
                properties
        );
    }
}