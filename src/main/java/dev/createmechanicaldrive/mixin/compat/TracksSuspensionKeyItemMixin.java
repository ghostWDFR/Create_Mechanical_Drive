package dev.createmechanicaldrive.mixin.compat;

import dev.createmechanicaldrive.content.adjustment_wrench.AdjustmentWrenchItem;
import dev.createmechanicaldrive.content.screwdriver.ScrewdriverItem;

import dev.createmechanicaldrive.content.suspension.SuspensionSpringTuning;
import dev.createmechanicaldrive.content.wheel_mount_offset.WheelMountOffset;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(
        targets = "dev.qwxon.tracks.content.items.SuspensionKeyItem",
        remap = false
)
public abstract class TracksSuspensionKeyItemMixin {

    @Unique
    private static final String TRACKS_MODE_TAG =
            "TracksSuspensionKeyMode";
    @Unique
    private static final int TRACKS_MODE_COUNT = 10;
    @Unique
    private static final int TRACKS_POSITION_MODE = 0;
    @Unique
    private static final int TRACKS_SPRING_MODE = 1;
    @Unique
    private static final int TRACKS_DAMPING_MODE = 2;
    @Unique
    private static final int TRACKS_BUMP_CLEARANCE_MODE = 3;
    @Unique
    private static final int TRACKS_BUMP_FORCE_MODE = 4;
    @Unique
    private static final int TRACKS_MAX_IMPULSE_MODE = 5;
    @Unique
    private static final int TRACKS_DRIVE_MODE = 6;
    @Unique
    private static final int TRACKS_GRIP_MODE = 7;
    @Unique
    private static final int TRACKS_RESET_MODE = 8;

    @Inject(
            method = "useOn",
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            remap = false
    )
    private void mechanicalDrive$adjustOwnWheelMount(
            UseOnContext context,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        Level level = context.getLevel();
        BlockEntity blockEntity =
                level.getBlockEntity(context.getClickedPos());

        int mode = mechanicalDrive$getTracksMode(
                context.getItemInHand()
        );

        if (mode == TRACKS_POSITION_MODE
                && blockEntity instanceof WheelMountOffset offset) {
            cir.setReturnValue(
                    AdjustmentWrenchItem.adjustOffset(context, offset)
            );
            return;
        }

        if (!(blockEntity instanceof SuspensionSpringTuning tuning)) {
            return;
        }

        if (mode != TRACKS_SPRING_MODE
                && mode != TRACKS_DAMPING_MODE
                && mode != TRACKS_BUMP_CLEARANCE_MODE
                && mode != TRACKS_BUMP_FORCE_MODE
                && mode != TRACKS_MAX_IMPULSE_MODE
                && mode != TRACKS_DRIVE_MODE
                && mode != TRACKS_GRIP_MODE
                && mode != TRACKS_RESET_MODE) {
            return;
        }

        Player player = context.getPlayer();
        int steps = player != null && player.isShiftKeyDown()
                ? -1
                : 1;

        if (!level.isClientSide) {
            String tuningKey = mechanicalDrive$getTuningKey(mode);
            if (mode != TRACKS_RESET_MODE
                    && !tuning.mechanicalDrive$supportsSuspensionTuning(
                            tuningKey
                    )) {
                ScrewdriverItem.showUnsupportedFeedback(
                        level,
                        context.getClickedPos(),
                        player
                );
                cir.setReturnValue(InteractionResult.SUCCESS);
                return;
            }

            if (mode == TRACKS_RESET_MODE) {
                tuning.mechanicalDrive$resetSuspensionTuning();
                ScrewdriverItem.showResetFeedback(
                        level,
                        context.getClickedPos(),
                        player
                );
            } else {
                double multiplier =
                        tuning.mechanicalDrive$adjustSuspensionTuning(
                                tuningKey,
                                steps
                        );

                ScrewdriverItem.showTuningFeedback(
                        level,
                        context.getClickedPos(),
                        player,
                        tuningKey,
                        multiplier,
                        steps
                );
            }
        }

        cir.setReturnValue(
                InteractionResult.sidedSuccess(level.isClientSide)
        );
    }

    @Unique
    private static String mechanicalDrive$getTuningKey(int mode) {
        return switch (mode) {
            case TRACKS_DAMPING_MODE -> SuspensionSpringTuning.DAMPING;
            case TRACKS_BUMP_CLEARANCE_MODE ->
                    SuspensionSpringTuning.BUMP_CLEARANCE;
            case TRACKS_BUMP_FORCE_MODE -> SuspensionSpringTuning.BUMP_FORCE;
            case TRACKS_MAX_IMPULSE_MODE ->
                    SuspensionSpringTuning.MAX_IMPULSE;
            case TRACKS_DRIVE_MODE -> SuspensionSpringTuning.DRIVE;
            case TRACKS_GRIP_MODE -> SuspensionSpringTuning.GRIP;
            default -> SuspensionSpringTuning.SPRING;
        };
    }

    @Unique
    private static int mechanicalDrive$getTracksMode(ItemStack stack) {
        CustomData customData = stack.getOrDefault(
                DataComponents.CUSTOM_DATA,
                CustomData.EMPTY
        );

        CompoundTag tag = customData.copyTag();
        return Math.floorMod(
                tag.getInt(TRACKS_MODE_TAG),
                TRACKS_MODE_COUNT
        );
    }
}
