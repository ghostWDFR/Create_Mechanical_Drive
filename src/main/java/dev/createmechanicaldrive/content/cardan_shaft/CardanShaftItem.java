package dev.createmechanicaldrive.content.cardan_shaft;

import dev.createmechanicaldrive.network.PlaceCardanShaftPayload;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.PacketDistributor;

public class CardanShaftItem
        extends BlockItem {
    public CardanShaftItem(
            Block block,
            Properties properties
    ) {
        super(block, properties);
    }

    @Override
    public String getDescriptionId() {
        return "item.mechanical_drive.cardan_shaft";
    }

    @Override
    public InteractionResult useOn(
            UseOnContext context
    ) {
        if (!context.getLevel().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        PacketDistributor.sendToServer(
                PlaceCardanShaftPayload.place(
                        context
                )
        );

        return InteractionResult.SUCCESS;
    }
}
