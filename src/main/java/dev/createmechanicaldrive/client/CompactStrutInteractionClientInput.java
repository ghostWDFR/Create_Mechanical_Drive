package dev.createmechanicaldrive.client;

import com.simibubi.create.content.equipment.wrench.WrenchItem;
import dev.createmechanicaldrive.content.adjustment_wrench.AdjustmentWrenchItem;
import dev.createmechanicaldrive.content.suspension_strut.CompactStrutSupportShapes;
import dev.createmechanicaldrive.content.suspension_strut.SpringTuningWrenchItem;
import dev.createmechanicaldrive.content.suspension_strut.SuspensionStrutBlockEntity;
import dev.createmechanicaldrive.network.InteractCompactStrutPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class CompactStrutInteractionClientInput {
    private CompactStrutInteractionClientInput() {
    }

    public static void onClickInput(
            InputEvent.InteractionKeyMappingTriggered event
    ) {
        if (!event.isUseItem() || event.isCanceled()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null
                || minecraft.player == null
                || minecraft.level == null
                || !(minecraft.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }

        InteractionHand hand = event.getHand();
        ItemStack stack = minecraft.player.getItemInHand(hand);
        Item item = stack.getItem();
        if (!(item instanceof SpringTuningWrenchItem)
                && !(item instanceof AdjustmentWrenchItem)
                && (!(item instanceof WrenchItem)
                || !minecraft.player.isShiftKeyDown())) {
            return;
        }

        SuspensionStrutBlockEntity strut =
                CompactStrutSupportShapes.findAttachedStrut(
                        minecraft.level,
                        hit.getBlockPos(),
                        hit.getLocation()
                );
        if (strut == null
                || strut.isPending()
                || !strut.hasLink()
                || !strut.isCompactLink()) {
            return;
        }

        Direction facing = strut.getLinkedFacing();
        if (facing == null
                || !hit.getBlockPos().equals(strut.getLinkedTransformPos())) {
            return;
        }

        PacketDistributor.sendToServer(new InteractCompactStrutPayload(
                hit.getBlockPos(),
                facing,
                hand
        ));
        event.setCanceled(true);
        event.setSwingHand(true);
    }
}
