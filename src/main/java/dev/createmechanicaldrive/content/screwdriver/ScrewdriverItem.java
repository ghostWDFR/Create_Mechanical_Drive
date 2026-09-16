package dev.createmechanicaldrive.content.screwdriver;

import dev.createmechanicaldrive.content.double_rigid_steering_wheel_mount.DoubleRigidSteeringWheelMountBlock;
import dev.createmechanicaldrive.content.rigid_steering_wheel_mount.RigidSteeringWheelMountBlock;
import dev.createmechanicaldrive.content.rigid_wheel_mount.RigidWheelMountBlock;
import dev.createmechanicaldrive.content.suspension.SuspensionSpringTuning;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Locale;
import java.util.List;

public class ScrewdriverItem extends Item {

    private static final String MODE_TAG =
            "MechanicalDriveScrewdriverMode";

    public ScrewdriverItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult onItemUseFirst(
            ItemStack stack,
            UseOnContext context
    ) {
        return handleMountInteraction(context);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return handleMountInteraction(context);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        ItemStack stack = player.getItemInHand(hand);
        TuningMode mode = cycleMode(
                stack,
                player.isShiftKeyDown() ? -1 : 1
        );

        if (!level.isClientSide) {
            level.playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.UI_BUTTON_CLICK.value(),
                    SoundSource.PLAYERS,
                    0.5F,
                    player.isShiftKeyDown() ? 0.9F : 1.1F
            );
            player.displayClientMessage(
                    Component.translatable(
                            "item.mechanical_drive.screwdriver.mode",
                            Component.translatable(mode.translationKey)
                    ).withStyle(ChatFormatting.GOLD),
                    true
            );
        }

        return InteractionResultHolder.sidedSuccess(
                stack,
                level.isClientSide
        );
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltipComponents,
            TooltipFlag tooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        TuningMode mode = getMode(stack);
        tooltipComponents.add(tooltipLine(
                Component.translatable(
                        "item.mechanical_drive.screwdriver.tooltip.mode",
                        Component.translatable(mode.translationKey)
                ),
                ChatFormatting.GOLD
        ));
        tooltipComponents.add(tooltipLine(
                Component.translatable(
                        "item.mechanical_drive.screwdriver.tooltip.adjust"
                ),
                ChatFormatting.GRAY
        ));
        tooltipComponents.add(tooltipLine(
                Component.translatable(
                        "item.mechanical_drive.screwdriver.tooltip.cycle"
                ),
                ChatFormatting.DARK_GRAY
        ));
    }

    private static Component tooltipLine(
            Component component,
            ChatFormatting color
    ) {
        return component.copy().withStyle(style -> style
                .withColor(color)
                .withItalic(false));
    }

    private static InteractionResult handleMountInteraction(
            UseOnContext context
    ) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (blockEntity instanceof SuspensionSpringTuning tuning) {
            return tuneSuspension(context, tuning);
        }

        Object block = level.getBlockState(pos).getBlock();
        if (block instanceof RigidWheelMountBlock
                || block instanceof RigidSteeringWheelMountBlock
                || block instanceof DoubleRigidSteeringWheelMountBlock) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return InteractionResult.PASS;
    }

    private static InteractionResult tuneSuspension(
            UseOnContext context,
            SuspensionSpringTuning tuning
    ) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        TuningMode mode = getMode(context.getItemInHand());

        if (!level.isClientSide) {
            if (mode != TuningMode.RESET
                    && !tuning.mechanicalDrive$supportsSuspensionTuning(
                            mode.tuningKey
                    )) {
                showUnsupportedFeedback(level, pos, player);
                return InteractionResult.sidedSuccess(false);
            }

            if (mode == TuningMode.RESET) {
                tuning.mechanicalDrive$resetSuspensionTuning();
                showResetFeedback(level, pos, player);
            } else {
                int steps = player != null && player.isShiftKeyDown()
                        ? -1
                        : 1;
                double multiplier =
                        tuning.mechanicalDrive$adjustSuspensionTuning(
                                mode.tuningKey,
                                steps
                        );

                showTuningFeedback(
                        level,
                        pos,
                        player,
                        mode.tuningKey,
                        multiplier,
                        steps
                );
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public static void showFeedback(
            Level level,
            BlockPos pos,
            Player player,
            double multiplier,
            int direction
    ) {
        showTuningFeedback(
                level,
                pos,
                player,
                SuspensionSpringTuning.SPRING,
                multiplier,
                direction
        );
    }

    public static void showTuningFeedback(
            Level level,
            BlockPos pos,
            Player player,
            String tuning,
            double multiplier,
            int direction
    ) {
        if (level.isClientSide) {
            return;
        }

        level.playSound(
                null,
                pos,
                SoundEvents.UI_BUTTON_CLICK.value(),
                SoundSource.BLOCKS,
                0.5F,
                direction < 0 ? 0.9F : 1.1F
        );

        if (player != null) {
            player.displayClientMessage(
                    Component.translatable(
                            "item.mechanical_drive.screwdriver.tuned",
                            Component.translatable(
                                    TuningMode.forKey(tuning).translationKey
                            ),
                            String.format(
                                    Locale.ROOT,
                                    "%.2fx",
                                    multiplier
                            )
                    ),
                    true
            );
        }
    }

    public static void showResetFeedback(
            Level level,
            BlockPos pos,
            Player player
    ) {
        if (level.isClientSide) {
            return;
        }

        level.playSound(
                null,
                pos,
                SoundEvents.UI_BUTTON_CLICK.value(),
                SoundSource.BLOCKS,
                0.5F,
                1.0F
        );

        if (player != null) {
            player.displayClientMessage(
                    Component.translatable(
                            "item.mechanical_drive.screwdriver.reset"
                    ),
                    true
            );
        }
    }

    public static void showUnsupportedFeedback(
            Level level,
            BlockPos pos,
            Player player
    ) {
        if (level.isClientSide) {
            return;
        }

        level.playSound(
                null,
                pos,
                SoundEvents.DISPENSER_FAIL,
                SoundSource.BLOCKS,
                0.4F,
                1.0F
        );
        if (player != null) {
            player.displayClientMessage(
                    Component.translatable(
                            "item.mechanical_drive.screwdriver.unsupported"
                    ),
                    true
            );
        }
    }

    private static TuningMode cycleMode(ItemStack stack, int direction) {
        TuningMode[] modes = TuningMode.values();
        int index = Math.floorMod(
                getModeIndex(stack) + direction,
                modes.length
        );

        CustomData customData = stack.getOrDefault(
                DataComponents.CUSTOM_DATA,
                CustomData.EMPTY
        );
        CompoundTag tag = customData.copyTag();
        tag.putInt(MODE_TAG, index);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return modes[index];
    }

    private static TuningMode getMode(ItemStack stack) {
        TuningMode[] modes = TuningMode.values();
        return modes[Math.floorMod(getModeIndex(stack), modes.length)];
    }

    private static int getModeIndex(ItemStack stack) {
        return stack.getOrDefault(
                        DataComponents.CUSTOM_DATA,
                        CustomData.EMPTY
                )
                .copyTag()
                .getInt(MODE_TAG);
    }

    private enum TuningMode {
        SPRING(
                SuspensionSpringTuning.SPRING,
                "item.mechanical_drive.screwdriver.mode.spring"
        ),
        DAMPING(
                SuspensionSpringTuning.DAMPING,
                "item.mechanical_drive.screwdriver.mode.damping"
        ),
        BUMP_CLEARANCE(
                SuspensionSpringTuning.BUMP_CLEARANCE,
                "item.mechanical_drive.screwdriver.mode.bump_clearance"
        ),
        BUMP_FORCE(
                SuspensionSpringTuning.BUMP_FORCE,
                "item.mechanical_drive.screwdriver.mode.bump_force"
        ),
        MAX_IMPULSE(
                SuspensionSpringTuning.MAX_IMPULSE,
                "item.mechanical_drive.screwdriver.mode.max_impulse"
        ),
        DRIVE(
                SuspensionSpringTuning.DRIVE,
                "item.mechanical_drive.screwdriver.mode.drive"
        ),
        GRIP(
                SuspensionSpringTuning.GRIP,
                "item.mechanical_drive.screwdriver.mode.grip"
        ),
        RESET(
                null,
                "item.mechanical_drive.screwdriver.mode.reset"
        );

        private final String tuningKey;
        private final String translationKey;

        TuningMode(String tuningKey, String translationKey) {
            this.tuningKey = tuningKey;
            this.translationKey = translationKey;
        }

        private static TuningMode forKey(String tuning) {
            for (TuningMode mode : values()) {
                if (mode.tuningKey != null
                        && mode.tuningKey.equals(tuning)) {
                    return mode;
                }
            }

            return SPRING;
        }
    }
}
