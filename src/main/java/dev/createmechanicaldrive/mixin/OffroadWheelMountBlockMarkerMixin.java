package dev.createmechanicaldrive.mixin;

import dev.createmechanicaldrive.content.shaft_marker.ShaftMarkerItem;
import dev.ryanhcode.offroad.content.blocks.wheel_mount.WheelMountBlock;
import dev.ryanhcode.offroad.content.blocks.wheel_mount.WheelMountBlockEntity;
import dev.ryanhcode.offroad.index.OffroadDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WheelMountBlock.class)
public abstract class OffroadWheelMountBlockMarkerMixin {
    @Inject(
            method = "useItemOn",
            at = @At("HEAD"),
            cancellable = true
    )
    private void mechanicalDrive$swapShaftMarker(
            ItemStack held,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit,
            CallbackInfoReturnable<ItemInteractionResult> cir
    ) {
        Direction hitFace = hit.getDirection();
        if (hitFace != state.getValue(WheelMountBlock.HORIZONTAL_FACING)
                && hitFace != Direction.DOWN) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof WheelMountBlockEntity mount)) {
            return;
        }

        ItemStack mounted = mount.getHeldItem();
        boolean markerSwap = ShaftMarkerItem.isMarker(held)
                || (ShaftMarkerItem.isMarker(mounted)
                && (held.isEmpty()
                || held.has(OffroadDataComponents.TIRE)));
        if (!markerSwap) {
            return;
        }

        if (level.isClientSide) {
            cir.setReturnValue(ItemInteractionResult.SUCCESS);
            return;
        }

        ItemStack previous = mounted.copy();
        ItemStack replacement = held.isEmpty()
                ? ItemStack.EMPTY
                : held.copyWithCount(1);
        mount.getInventory().slot.setStack(replacement);

        if (!held.isEmpty() && !player.hasInfiniteMaterials()) {
            held.shrink(1);
        }
        if (!previous.isEmpty()) {
            player.getInventory().placeItemBackInInventory(previous);
        }

        mount.setChanged();
        mount.sendData();
        float pitch = 0.8F + level.random.nextFloat() * 0.4F;
        level.playSound(
                null,
                pos,
                replacement.isEmpty()
                        ? SoundEvents.ITEM_PICKUP
                        : SoundEvents.ITEM_FRAME_ADD_ITEM,
                SoundSource.PLAYERS,
                0.75F,
                pitch
        );
        cir.setReturnValue(ItemInteractionResult.CONSUME);
    }
}
