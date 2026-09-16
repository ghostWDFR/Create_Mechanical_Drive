package dev.createmechanicaldrive.content.suspension_strut;

import dev.createmechanicaldrive.network.PlaceSuspensionStrutPayload;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.PacketDistributor;

public class SuspensionStrutItem extends BlockItem {
    public SuspensionStrutItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public String getDescriptionId() {
        return "item.mechanical_drive.suspension_strut";
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!context.getLevel().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        PacketDistributor.sendToServer(PlaceSuspensionStrutPayload.place(context));
        return InteractionResult.SUCCESS;
    }
}
