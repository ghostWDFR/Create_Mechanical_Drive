package dev.createmechanicaldrive.content.suspension_strut;

import dev.createmechanicaldrive.content.suspension.SpringTuningWrenchTarget;
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

import java.util.List;
import java.util.Locale;

public class SpringTuningWrenchItem extends Item {
    private static final String MODE_TAG =
            "MechanicalDriveSpringTuningWrenchMode";

    public SpringTuningWrenchItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult onItemUseFirst(
            ItemStack stack,
            UseOnContext context
    ) {
        return handleStrutInteraction(context);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return handleStrutInteraction(context);
    }

    private static InteractionResult handleStrutInteraction(
            UseOnContext context
    ) {
        Level level = context.getLevel();
        if (level.getBlockEntity(context.getClickedPos())
                instanceof SpringTuningWrenchTarget target) {
            return interactWithSpringTarget(context, target);
        }
        SuspensionStrutBlockEntity strut =
                level.getBlockEntity(context.getClickedPos())
                        instanceof SuspensionStrutBlockEntity direct
                        ? direct
                        : CompactStrutSupportShapes.findAttachedStrut(
                                level,
                                context.getClickedPos(),
                                context.getClickLocation()
                        );
        return interactWithStrut(context, strut);
    }

    private static InteractionResult interactWithSpringTarget(
            UseOnContext context,
            SpringTuningWrenchTarget target
    ) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        int direction = player != null && player.isShiftKeyDown() ? -1 : 1;
        TuningMode mode = getMode(context.getItemInHand());
        if (!level.isClientSide) {
            Component value;
            switch (mode) {
                case STRENGTH -> value = multiplier(
                        target.mechanicalDrive$adjustSpringWrenchStrength(
                                direction
                        )
                );
                case LENGTH -> value = Component.translatable(
                        "item.mechanical_drive.spring_tuning_wrench.value.degrees",
                        String.format(
                                Locale.ROOT,
                                "%+.0f",
                                target.mechanicalDrive$adjustSpringWrenchRestAngle(
                                        direction
                                )
                        )
                );
                case RESET -> {
                    target.mechanicalDrive$resetSpringWrenchTuning();
                    showResetFeedback(level, context.getClickedPos(), player);
                    return InteractionResult.sidedSuccess(false);
                }
                default -> {
                    showUnsupportedFeedback(
                            level,
                            context.getClickedPos(),
                            player
                    );
                    return InteractionResult.sidedSuccess(false);
                }
            }

            level.playSound(
                    null,
                    context.getClickedPos(),
                    SoundEvents.UI_BUTTON_CLICK.value(),
                    SoundSource.BLOCKS,
                    0.5F,
                    direction < 0 ? 0.9F : 1.1F
            );
            if (player != null) {
                player.displayClientMessage(
                        Component.translatable(
                                "item.mechanical_drive.spring_tuning_wrench.tuned",
                                Component.translatable(mode.translationKey),
                                value
                        ).withStyle(ChatFormatting.AQUA),
                        true
                );
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public static InteractionResult interactWithStrut(
            UseOnContext context,
            SuspensionStrutBlockEntity strut
    ) {
        Level level = context.getLevel();
        if (strut == null
                || strut.isPending()
                || !strut.hasLink()) {
            return InteractionResult.PASS;
        }

        Player player = context.getPlayer();
        int direction = player != null && player.isShiftKeyDown() ? -1 : 1;
        TuningMode mode = getMode(context.getItemInHand());
        if (!level.isClientSide) {
            Component value;
            switch (mode) {
                case STRENGTH -> value = multiplier(
                        strut.adjustStrength(direction)
                );
                case LENGTH -> value = Component.translatable(
                        "item.mechanical_drive.spring_tuning_wrench.value.blocks",
                        String.format(Locale.ROOT, "%.2f", strut.adjustLength(direction))
                );
                case DAMPING -> value = multiplier(
                        strut.adjustDamping(direction)
                );
                case MAX_IMPULSE -> value = Component.translatable(
                        "item.mechanical_drive.spring_tuning_wrench.value.impulse",
                        String.format(
                                Locale.ROOT,
                                "%.0f",
                                strut.adjustMaxImpulse(direction)
                        )
                );
                case CONSTRAINT -> value = multiplier(
                        strut.adjustConstraintStrength(direction)
                );
                case BEHAVIOR -> value = Component.translatable(
                        strut.cycleBehavior(direction).translationKey()
                );
                case RESET -> {
                    strut.resetSettings();
                    showResetFeedback(level, context.getClickedPos(), player);
                    return InteractionResult.sidedSuccess(false);
                }
                default -> throw new IllegalStateException(
                        "Unknown spring wrench mode: " + mode
                );
            }

            level.playSound(
                    null,
                    context.getClickedPos(),
                    SoundEvents.UI_BUTTON_CLICK.value(),
                    SoundSource.BLOCKS,
                    0.5F,
                    direction < 0 ? 0.9F : 1.1F
            );
            if (player != null) {
                player.displayClientMessage(
                        Component.translatable(
                                "item.mechanical_drive.spring_tuning_wrench.tuned",
                                Component.translatable(mode.translationKey),
                                value
                        ).withStyle(ChatFormatting.AQUA),
                        true
                );
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
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
                            "item.mechanical_drive.spring_tuning_wrench.mode",
                            Component.translatable(mode.translationKey)
                    ).withStyle(ChatFormatting.GOLD),
                    true
            );
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
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
        tooltipComponents.add(styled(
                Component.translatable(
                        "item.mechanical_drive.spring_tuning_wrench.tooltip.mode",
                        Component.translatable(mode.translationKey)
                ),
                ChatFormatting.GOLD
        ));
        tooltipComponents.add(styled(
                Component.translatable(
                        "item.mechanical_drive.spring_tuning_wrench.tooltip.adjust"
                ),
                ChatFormatting.GRAY
        ));
        tooltipComponents.add(styled(
                Component.translatable(
                        "item.mechanical_drive.spring_tuning_wrench.tooltip.cycle"
                ),
                ChatFormatting.DARK_GRAY
        ));
    }

    private static void showResetFeedback(
            Level level,
            BlockPos pos,
            Player player
    ) {
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
                            "item.mechanical_drive.spring_tuning_wrench.reset"
                    ).withStyle(ChatFormatting.GREEN),
                    true
            );
        }
    }

    private static void showUnsupportedFeedback(
            Level level,
            BlockPos pos,
            Player player
    ) {
        level.playSound(
                null,
                pos,
                SoundEvents.UI_BUTTON_CLICK.value(),
                SoundSource.BLOCKS,
                0.5F,
                0.75F
        );
        if (player != null) {
            player.displayClientMessage(
                    Component.translatable(
                            "item.mechanical_drive.spring_tuning_wrench.unsupported_mount_mode"
                    ).withStyle(ChatFormatting.RED),
                    true
            );
        }
    }

    private static Component multiplier(double value) {
        return Component.literal(String.format(Locale.ROOT, "%.2fx", value));
    }

    private static Component styled(
            Component component,
            ChatFormatting color
    ) {
        return component.copy().withStyle(style -> style
                .withColor(color)
                .withItalic(false));
    }

    private static TuningMode cycleMode(ItemStack stack, int direction) {
        TuningMode[] modes = TuningMode.values();
        int index = Math.floorMod(
                getModeIndex(stack) + direction,
                modes.length
        );
        CompoundTag tag = stack.getOrDefault(
                DataComponents.CUSTOM_DATA,
                CustomData.EMPTY
        ).copyTag();
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
        ).copyTag().getInt(MODE_TAG);
    }

    private enum TuningMode {
        STRENGTH("item.mechanical_drive.spring_tuning_wrench.setting.strength"),
        LENGTH("item.mechanical_drive.spring_tuning_wrench.setting.length"),
        DAMPING("item.mechanical_drive.spring_tuning_wrench.setting.damping"),
        MAX_IMPULSE("item.mechanical_drive.spring_tuning_wrench.setting.max_impulse"),
        CONSTRAINT("item.mechanical_drive.spring_tuning_wrench.setting.constraint"),
        BEHAVIOR("item.mechanical_drive.spring_tuning_wrench.setting.behavior"),
        RESET("item.mechanical_drive.spring_tuning_wrench.setting.reset");

        private final String translationKey;

        TuningMode(String translationKey) {
            this.translationKey = translationKey;
        }
    }
}
