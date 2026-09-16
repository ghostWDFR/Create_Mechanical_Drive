package dev.createmechanicaldrive.content.adjustment_wrench;

import dev.createmechanicaldrive.content.suspension.SuspensionSpringTuning;
import dev.createmechanicaldrive.content.suspension.SpringTuningWrenchTarget;
import dev.createmechanicaldrive.content.suspension_strut.CompactStrutSupportShapes;
import dev.createmechanicaldrive.content.suspension_strut.SuspensionStrutBlockEntity;
import dev.createmechanicaldrive.content.suspension_strut.SuspensionStrutSettings;
import dev.createmechanicaldrive.content.tracks.mounts.idler.IdlerMountBlockEntity;
import dev.createmechanicaldrive.content.tracks.mounts.torsion.TorsionMountBlockEntity;
import dev.createmechanicaldrive.content.wheel_mount_offset.WheelMountOffset;
import dev.createmechanicaldrive.content.wheel_mount_offset.WheelMountOffsetState;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class AdjustmentWrenchItem extends Item {

    private static final String MODE_TAG =
            "MechanicalDriveAdjustmentWrenchMode";
    private static final String PAPER_DATA_TAG =
            "MechanicalDriveMountSettings";
    private static final String STRUT_PAPER_DATA_TAG =
            "MechanicalDriveSuspensionStrutSettings";
    private static final String TANK_PAPER_DATA_TAG =
            "MechanicalDriveTankSuspensionSettings";
    private static final String PROFILE_VERSION_TAG =
            "Version";
    private static final String OFFSETS_TAG =
            "Offsets";
    private static final String TUNING_TAG =
            "SuspensionTuning";
    private static final String STRUT_SETTINGS_TAG = "Settings";
    private static final int PROFILE_VERSION = 1;
    private static final String[] TUNING_KEYS = {
            SuspensionSpringTuning.SPRING,
            SuspensionSpringTuning.DAMPING,
            SuspensionSpringTuning.BUMP_CLEARANCE,
            SuspensionSpringTuning.BUMP_FORCE,
            SuspensionSpringTuning.MAX_IMPULSE,
            SuspensionSpringTuning.DRIVE,
            SuspensionSpringTuning.GRIP
    };

    public AdjustmentWrenchItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult onItemUseFirst(
            ItemStack stack,
            UseOnContext context
    ) {
        return handleUseOn(context);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return handleUseOn(context);
    }

    private static InteractionResult handleUseOn(UseOnContext context) {
        BlockEntity blockEntity = context.getLevel()
                .getBlockEntity(context.getClickedPos());
        SuspensionStrutBlockEntity strut =
                blockEntity instanceof SuspensionStrutBlockEntity direct
                        ? direct
                        : CompactStrutSupportShapes.findAttachedStrut(
                                context.getLevel(),
                                context.getClickedPos(),
                                context.getClickLocation()
                        );
        InteractionResult strutResult = interactWithStrut(context, strut);
        if (strutResult != InteractionResult.PASS) {
            return strutResult;
        }
        if (blockEntity instanceof IdlerMountBlockEntity idlerMount
                && getMode(context.getItemInHand()) == WrenchMode.ADJUST) {
            return adjustIdlerOffset(context, idlerMount);
        }
        if (blockEntity instanceof TorsionMountBlockEntity torsionMount) {
            return switch (getMode(context.getItemInHand())) {
                case ADJUST -> InteractionResult.PASS;
                case COPY -> copyTankSettings(context, torsionMount);
                case PASTE -> pasteTankSettings(context, torsionMount);
            };
        }
        if (!(blockEntity instanceof WheelMountOffset offset)) {
            return InteractionResult.PASS;
        }

        return switch (getMode(context.getItemInHand())) {
            case ADJUST -> adjustOffset(context, offset);
            case COPY -> copySettings(context, offset);
            case PASTE -> pasteSettings(context, offset);
        };
    }

    public static InteractionResult interactWithStrut(
            UseOnContext context,
            SuspensionStrutBlockEntity strut
    ) {
        if (strut == null
                || !strut.hasLink()
                || strut.isPending()) {
            return InteractionResult.PASS;
        }
        return switch (getMode(context.getItemInHand())) {
            case ADJUST -> InteractionResult.PASS;
            case COPY -> copyStrutSettings(context, strut);
            case PASTE -> pasteStrutSettings(context, strut);
        };
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        ItemStack stack = player.getItemInHand(hand);
        WrenchMode mode = cycleMode(
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
                            "item.mechanical_drive.adjustment_wrench.mode",
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
        WrenchMode mode = getMode(stack);
        tooltipComponents.add(loreText(
                Component.translatable(
                        "item.mechanical_drive.adjustment_wrench.tooltip.mode",
                        Component.translatable(mode.translationKey)
                ),
                ChatFormatting.GOLD
        ));
        tooltipComponents.add(loreText(
                Component.translatable(
                        "item.mechanical_drive.adjustment_wrench.tooltip.use"
                ),
                ChatFormatting.GRAY
        ));
        tooltipComponents.add(loreText(
                Component.translatable(
                        "item.mechanical_drive.adjustment_wrench.tooltip.cycle"
                ),
                ChatFormatting.DARK_GRAY
        ));
    }

    public static InteractionResult adjustOffset(
            UseOnContext context,
            WheelMountOffset offset
    ) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        int steps = player != null && player.isShiftKeyDown() ? -1 : 1;
        Direction clickedFace = context.getClickedFace();

        if (!level.isClientSide) {
            String axis;
            double value;
            if (offset.mechanicalDrive$isHeightOffsetFace(clickedFace)) {
                axis = "height";
                value = offset.mechanicalDrive$adjustHeightOffset(steps);
            } else if (clickedFace.getAxis() == Direction.Axis.Y) {
                axis = "longitudinal";
                value = offset.mechanicalDrive$adjustLongitudinalOffset(steps);
            } else {
                axis = "lateral";
                value = offset.mechanicalDrive$adjustLateralOffset(steps);
            }

            level.playSound(
                    null,
                    pos,
                    SoundEvents.UI_BUTTON_CLICK.value(),
                    SoundSource.BLOCKS,
                    0.45F,
                    0.9F + level.random.nextFloat() * 0.2F
            );
            if (player != null) {
                player.displayClientMessage(
                        Component.translatable(
                                "item.mechanical_drive.adjustment_wrench.adjusted",
                                Component.translatable(
                                        "item.mechanical_drive.adjustment_wrench.axis." + axis
                                ),
                                String.format(Locale.ROOT, "%.3f", value)
                        ).withStyle(ChatFormatting.GRAY),
                        true
                );
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static InteractionResult adjustIdlerOffset(
            UseOnContext context,
            IdlerMountBlockEntity mount
    ) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        int steps = player != null && player.isShiftKeyDown() ? -1 : 1;

        if (!level.isClientSide) {
            double value = mount.adjustAxleOffset(steps);
            level.playSound(
                    null,
                    pos,
                    SoundEvents.UI_BUTTON_CLICK.value(),
                    SoundSource.BLOCKS,
                    0.45F,
                    steps < 0 ? 0.9F : 1.1F
            );
            if (player != null) {
                player.displayClientMessage(
                        Component.translatable(
                                "item.mechanical_drive.adjustment_wrench.adjusted",
                                Component.translatable(
                                        "item.mechanical_drive.adjustment_wrench.axis.lateral"
                                ),
                                String.format(Locale.ROOT, "%.3f", value)
                        ).withStyle(ChatFormatting.GRAY),
                        true
                );
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static InteractionResult copySettings(
            UseOnContext context,
            WheelMountOffset offset
    ) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }

        Player player = context.getPlayer();
        ItemStack paper = getOtherHandItem(context, player);
        if (!paper.is(Items.PAPER)) {
            return fail(
                    level,
                    context.getClickedPos(),
                    player,
                    "item.mechanical_drive.adjustment_wrench.paper_required"
            );
        }
        if (paper.has(DataComponents.CUSTOM_NAME)) {
            return fail(
                    level,
                    context.getClickedPos(),
                    player,
                    "item.mechanical_drive.adjustment_wrench.paper_renamed"
            );
        }
        if (!paper.isComponentsPatchEmpty()) {
            return fail(
                    level,
                    context.getClickedPos(),
                    player,
                    "item.mechanical_drive.adjustment_wrench.paper_not_empty"
            );
        }

        CompoundTag profile = new CompoundTag();
        profile.putInt(PROFILE_VERSION_TAG, PROFILE_VERSION);

        CompoundTag offsets = new CompoundTag();
        offset.mechanicalDrive$getWheelOffsetState().write(offsets);
        profile.put(OFFSETS_TAG, offsets);

        CompoundTag suspension = new CompoundTag();
        if (offset instanceof SuspensionSpringTuning tuning) {
            for (String key : TUNING_KEYS) {
                if (tuning.mechanicalDrive$supportsSuspensionTuning(key)) {
                    suspension.putDouble(
                            key,
                            tuning.mechanicalDrive$getSuspensionTuning(key)
                    );
                }
            }
        }
        profile.put(TUNING_TAG, suspension);

        CompoundTag paperData = new CompoundTag();
        paperData.put(PAPER_DATA_TAG, profile);
        paper.set(
                DataComponents.CUSTOM_DATA,
                CustomData.of(paperData)
        );
        paper.set(
                DataComponents.ITEM_NAME,
                Component.translatable(
                        "item.mechanical_drive.mount_settings_paper"
                ).withStyle(ChatFormatting.AQUA)
        );
        paper.set(
                DataComponents.LORE,
                createPaperLore(offsets, suspension)
        );

        return succeed(
                level,
                context.getClickedPos(),
                player,
                "item.mechanical_drive.adjustment_wrench.copied"
        );
    }

    private static InteractionResult pasteSettings(
            UseOnContext context,
            WheelMountOffset offset
    ) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }

        Player player = context.getPlayer();
        ItemStack paper = getOtherHandItem(context, player);
        if (!paper.is(Items.PAPER)) {
            return fail(
                    level,
                    context.getClickedPos(),
                    player,
                    "item.mechanical_drive.adjustment_wrench.paper_required"
            );
        }
        if (paper.has(DataComponents.CUSTOM_NAME)) {
            return fail(
                    level,
                    context.getClickedPos(),
                    player,
                    "item.mechanical_drive.adjustment_wrench.paper_renamed"
            );
        }

        CompoundTag paperData = paper.getOrDefault(
                        DataComponents.CUSTOM_DATA,
                        CustomData.EMPTY
                )
                .copyTag();
        if (paperData.contains(STRUT_PAPER_DATA_TAG, Tag.TAG_COMPOUND)) {
            return fail(
                    level,
                    context.getClickedPos(),
                    player,
                    "item.mechanical_drive.adjustment_wrench.paper_wrong_type"
            );
        }
        if (paperData.contains(TANK_PAPER_DATA_TAG, Tag.TAG_COMPOUND)) {
            return fail(
                    level,
                    context.getClickedPos(),
                    player,
                    "item.mechanical_drive.adjustment_wrench.paper_wrong_type"
            );
        }
        if (!paperData.contains(PAPER_DATA_TAG, Tag.TAG_COMPOUND)) {
            return fail(
                    level,
                    context.getClickedPos(),
                    player,
                    "item.mechanical_drive.adjustment_wrench.paper_no_data"
            );
        }

        CompoundTag profile = paperData.getCompound(PAPER_DATA_TAG);
        if (!isValidProfile(profile)) {
            return fail(
                    level,
                    context.getClickedPos(),
                    player,
                    "item.mechanical_drive.adjustment_wrench.paper_invalid"
            );
        }

        offset.mechanicalDrive$getWheelOffsetState().read(
                profile.getCompound(OFFSETS_TAG),
                false
        );
        offset.mechanicalDrive$syncWheelOffset();

        if (offset instanceof SuspensionSpringTuning tuning) {
            applySuspensionTuning(
                    tuning,
                    profile.getCompound(TUNING_TAG)
            );
        }

        if (player != null) {
            paper.shrink(1);
            player.getInventory()
                    .placeItemBackInInventory(
                            new ItemStack(Items.PAPER)
                    );
            player.setItemInHand(
                    getOtherHand(context),
                    paper
            );
        }

        return succeed(
                level,
                context.getClickedPos(),
                player,
                "item.mechanical_drive.adjustment_wrench.pasted"
        );
    }

    private static InteractionResult copyTankSettings(
            UseOnContext context,
            TorsionMountBlockEntity tuning
    ) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }

        Player player = context.getPlayer();
        ItemStack paper = getOtherHandItem(context, player);
        if (!paper.is(Items.PAPER)) {
            return fail(
                    level,
                    context.getClickedPos(),
                    player,
                    "item.mechanical_drive.adjustment_wrench.paper_required"
            );
        }
        if (paper.has(DataComponents.CUSTOM_NAME)) {
            return fail(
                    level,
                    context.getClickedPos(),
                    player,
                    "item.mechanical_drive.adjustment_wrench.paper_renamed"
            );
        }
        if (!paper.isComponentsPatchEmpty()) {
            return fail(
                    level,
                    context.getClickedPos(),
                    player,
                    "item.mechanical_drive.adjustment_wrench.paper_not_empty"
            );
        }

        CompoundTag suspension = writeTankSuspensionTuning(tuning);
        CompoundTag profile = new CompoundTag();
        profile.putInt(PROFILE_VERSION_TAG, PROFILE_VERSION);
        profile.put(TUNING_TAG, suspension);

        CompoundTag paperData = new CompoundTag();
        paperData.put(TANK_PAPER_DATA_TAG, profile);
        paper.set(DataComponents.CUSTOM_DATA, CustomData.of(paperData));
        paper.set(
                DataComponents.ITEM_NAME,
                Component.translatable(
                        "item.mechanical_drive.tank_suspension_settings_paper"
                ).withStyle(ChatFormatting.AQUA)
        );
        paper.set(
                DataComponents.LORE,
                createTankPaperLore(suspension)
        );

        return succeed(
                level,
                context.getClickedPos(),
                player,
                "item.mechanical_drive.adjustment_wrench.tank_copied"
        );
    }

    private static InteractionResult pasteTankSettings(
            UseOnContext context,
            TorsionMountBlockEntity tuning
    ) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }

        Player player = context.getPlayer();
        ItemStack paper = getOtherHandItem(context, player);
        if (!paper.is(Items.PAPER)) {
            return fail(
                    level,
                    context.getClickedPos(),
                    player,
                    "item.mechanical_drive.adjustment_wrench.paper_required"
            );
        }
        if (paper.has(DataComponents.CUSTOM_NAME)) {
            return fail(
                    level,
                    context.getClickedPos(),
                    player,
                    "item.mechanical_drive.adjustment_wrench.paper_renamed"
            );
        }

        CompoundTag paperData = paper.getOrDefault(
                DataComponents.CUSTOM_DATA,
                CustomData.EMPTY
        ).copyTag();
        if (paperData.contains(PAPER_DATA_TAG, Tag.TAG_COMPOUND)
                || paperData.contains(STRUT_PAPER_DATA_TAG, Tag.TAG_COMPOUND)) {
            return fail(
                    level,
                    context.getClickedPos(),
                    player,
                    "item.mechanical_drive.adjustment_wrench.paper_wrong_type"
            );
        }
        if (!paperData.contains(TANK_PAPER_DATA_TAG, Tag.TAG_COMPOUND)) {
            return fail(
                    level,
                    context.getClickedPos(),
                    player,
                    "item.mechanical_drive.adjustment_wrench.tank_paper_no_data"
            );
        }

        CompoundTag profile = paperData.getCompound(TANK_PAPER_DATA_TAG);
        if (!isValidTankProfile(profile)) {
            return fail(
                    level,
                    context.getClickedPos(),
                    player,
                    "item.mechanical_drive.adjustment_wrench.tank_paper_invalid"
            );
        }

        CompoundTag values = profile.getCompound(TUNING_TAG);
        applySuspensionTuning(tuning, values);
        tuning.mechanicalDrive$setSpringWrenchStrength(
                values.getDouble(SuspensionSpringTuning.SPRING)
        );
        if (values.contains(
                SpringTuningWrenchTarget.REST_ANGLE,
                Tag.TAG_ANY_NUMERIC
        )) {
            tuning.mechanicalDrive$setSpringWrenchRestAngle(
                    values.getDouble(SpringTuningWrenchTarget.REST_ANGLE)
            );
        }
        if (player != null) {
            paper.shrink(1);
            player.getInventory().placeItemBackInInventory(
                    new ItemStack(Items.PAPER)
            );
            player.setItemInHand(getOtherHand(context), paper);
        }

        return succeed(
                level,
                context.getClickedPos(),
                player,
                "item.mechanical_drive.adjustment_wrench.tank_pasted"
        );
    }

    private static InteractionResult copyStrutSettings(
            UseOnContext context,
            SuspensionStrutBlockEntity strut
    ) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }

        Player player = context.getPlayer();
        ItemStack paper = getOtherHandItem(context, player);
        if (!paper.is(Items.PAPER)) {
            return fail(
                    level,
                    context.getClickedPos(),
                    player,
                    "item.mechanical_drive.adjustment_wrench.paper_required"
            );
        }
        if (paper.has(DataComponents.CUSTOM_NAME)) {
            return fail(
                    level,
                    context.getClickedPos(),
                    player,
                    "item.mechanical_drive.adjustment_wrench.paper_renamed"
            );
        }
        if (!paper.isComponentsPatchEmpty()) {
            return fail(
                    level,
                    context.getClickedPos(),
                    player,
                    "item.mechanical_drive.adjustment_wrench.paper_not_empty"
            );
        }

        CompoundTag settings = strut.writeSettingsProfile();
        CompoundTag profile = new CompoundTag();
        profile.putInt(PROFILE_VERSION_TAG, PROFILE_VERSION);
        profile.put(STRUT_SETTINGS_TAG, settings);

        CompoundTag paperData = new CompoundTag();
        paperData.put(STRUT_PAPER_DATA_TAG, profile);
        paper.set(DataComponents.CUSTOM_DATA, CustomData.of(paperData));
        paper.set(
                DataComponents.ITEM_NAME,
                Component.translatable(
                        "item.mechanical_drive.suspension_strut_settings_paper"
                ).withStyle(ChatFormatting.AQUA)
        );
        paper.set(
                DataComponents.LORE,
                createStrutPaperLore(settings)
        );

        return succeed(
                level,
                context.getClickedPos(),
                player,
                "item.mechanical_drive.adjustment_wrench.strut_copied"
        );
    }

    private static InteractionResult pasteStrutSettings(
            UseOnContext context,
            SuspensionStrutBlockEntity strut
    ) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }

        Player player = context.getPlayer();
        ItemStack paper = getOtherHandItem(context, player);
        if (!paper.is(Items.PAPER)) {
            return fail(
                    level,
                    context.getClickedPos(),
                    player,
                    "item.mechanical_drive.adjustment_wrench.paper_required"
            );
        }
        if (paper.has(DataComponents.CUSTOM_NAME)) {
            return fail(
                    level,
                    context.getClickedPos(),
                    player,
                    "item.mechanical_drive.adjustment_wrench.paper_renamed"
            );
        }

        CompoundTag paperData = paper.getOrDefault(
                DataComponents.CUSTOM_DATA,
                CustomData.EMPTY
        ).copyTag();
        if (paperData.contains(PAPER_DATA_TAG, Tag.TAG_COMPOUND)
                || paperData.contains(TANK_PAPER_DATA_TAG, Tag.TAG_COMPOUND)) {
            return fail(
                    level,
                    context.getClickedPos(),
                    player,
                    "item.mechanical_drive.adjustment_wrench.paper_wrong_type"
            );
        }
        if (!paperData.contains(STRUT_PAPER_DATA_TAG, Tag.TAG_COMPOUND)) {
            return fail(
                    level,
                    context.getClickedPos(),
                    player,
                    "item.mechanical_drive.adjustment_wrench.strut_paper_no_data"
            );
        }

        CompoundTag profile = paperData.getCompound(STRUT_PAPER_DATA_TAG);
        if (!isValidStrutProfile(profile)
                || !strut.applySettingsProfile(
                profile.getCompound(STRUT_SETTINGS_TAG)
        )) {
            return fail(
                    level,
                    context.getClickedPos(),
                    player,
                    "item.mechanical_drive.adjustment_wrench.strut_paper_invalid"
            );
        }

        if (player != null) {
            paper.shrink(1);
            player.getInventory().placeItemBackInInventory(
                    new ItemStack(Items.PAPER)
            );
            player.setItemInHand(getOtherHand(context), paper);
        }

        return succeed(
                level,
                context.getClickedPos(),
                player,
                "item.mechanical_drive.adjustment_wrench.strut_pasted"
        );
    }

    private static boolean isValidStrutProfile(CompoundTag profile) {
        return profile.contains(PROFILE_VERSION_TAG, Tag.TAG_ANY_NUMERIC)
                && profile.getInt(PROFILE_VERSION_TAG) == PROFILE_VERSION
                && profile.contains(STRUT_SETTINGS_TAG, Tag.TAG_COMPOUND)
                && SuspensionStrutSettings.isValid(
                profile.getCompound(STRUT_SETTINGS_TAG)
        );
    }

    private static ItemLore createStrutPaperLore(CompoundTag settings) {
        List<Component> lines = new ArrayList<>();
        lines.add(sectionTitle(
                "item.mechanical_drive.suspension_strut_settings_paper.dynamics"
        ));
        lines.add(strutSettingLine(
                "item.mechanical_drive.spring_tuning_wrench.setting.strength",
                String.format(
                        Locale.ROOT,
                        "%.2fx",
                        settings.getDouble(SuspensionStrutSettings.STRENGTH_TAG)
                )
        ));
        lines.add(strutSettingLine(
                "item.mechanical_drive.spring_tuning_wrench.setting.length",
                Component.translatable(
                        "item.mechanical_drive.spring_tuning_wrench.value.blocks",
                        String.format(
                                Locale.ROOT,
                                "%.2f",
                                settings.getDouble(SuspensionStrutSettings.LENGTH_TAG)
                        )
                )
        ));
        lines.add(strutSettingLine(
                "item.mechanical_drive.spring_tuning_wrench.setting.damping",
                String.format(
                        Locale.ROOT,
                        "%.2fx",
                        settings.getDouble(SuspensionStrutSettings.DAMPING_TAG)
                )
        ));
        lines.add(strutSettingLine(
                "item.mechanical_drive.spring_tuning_wrench.setting.max_impulse",
                Component.translatable(
                        "item.mechanical_drive.spring_tuning_wrench.value.impulse",
                        String.format(
                                Locale.ROOT,
                                "%.0f",
                                settings.getDouble(SuspensionStrutSettings.MAX_IMPULSE_TAG)
                        )
                )
        ));
        lines.add(loreText(Component.empty(), ChatFormatting.GRAY));
        lines.add(sectionTitle(
                "item.mechanical_drive.suspension_strut_settings_paper.constraints"
        ));
        lines.add(strutSettingLine(
                "item.mechanical_drive.spring_tuning_wrench.setting.constraint",
                String.format(
                        Locale.ROOT,
                        "%.2fx",
                        settings.getDouble(SuspensionStrutSettings.CONSTRAINT_TAG)
                )
        ));
        SuspensionStrutSettings.Behavior behavior =
                SuspensionStrutSettings.Behavior.fromSerializedName(
                        settings.getString(SuspensionStrutSettings.BEHAVIOR_TAG)
                );
        lines.add(strutSettingLine(
                "item.mechanical_drive.spring_tuning_wrench.setting.behavior",
                Component.translatable(behavior.translationKey())
        ));
        lines.add(loreText(
                Component.translatable(behavior.descriptionKey()),
                ChatFormatting.DARK_GRAY
        ));
        lines.add(loreText(Component.empty(), ChatFormatting.GRAY));
        lines.add(loreText(
                Component.translatable(
                        "item.mechanical_drive.suspension_strut_settings_paper.hint"
                ),
                ChatFormatting.DARK_GRAY
        ));
        return new ItemLore(List.copyOf(lines));
    }

    private static Component strutSettingLine(
            String labelKey,
            String value
    ) {
        return strutSettingLine(labelKey, Component.literal(value));
    }

    private static Component strutSettingLine(
            String labelKey,
            Component value
    ) {
        return loreText(
                Component.translatable(
                        "item.mechanical_drive.suspension_strut_settings_paper.value",
                        loreText(
                                Component.translatable(labelKey),
                                ChatFormatting.GRAY
                        ),
                        loreText(value, ChatFormatting.AQUA)
                ),
                ChatFormatting.GRAY
        );
    }

    private static ItemLore createPaperLore(
            CompoundTag offsets,
            CompoundTag suspension
    ) {
        List<Component> lines = new ArrayList<>();
        lines.add(sectionTitle(
                "item.mechanical_drive.mount_settings_paper.offsets"
        ));
        lines.add(offsetLine(
                "item.mechanical_drive.adjustment_wrench.axis.lateral",
                offsets.getDouble(
                        WheelMountOffsetState.LATERAL_NBT_KEY
                )
        ));
        lines.add(offsetLine(
                "item.mechanical_drive.adjustment_wrench.axis.longitudinal",
                offsets.getDouble(
                        WheelMountOffsetState.LONGITUDINAL_NBT_KEY
                )
        ));
        lines.add(offsetLine(
                "item.mechanical_drive.adjustment_wrench.axis.height",
                offsets.getDouble(
                        WheelMountOffsetState.HEIGHT_NBT_KEY
                )
        ));

        if (!suspension.isEmpty()) {
            lines.add(loreText(Component.empty(), ChatFormatting.GRAY));
            lines.add(sectionTitle(
                    "item.mechanical_drive.mount_settings_paper.suspension"
            ));
            for (String key : TUNING_KEYS) {
                if (suspension.contains(key, Tag.TAG_ANY_NUMERIC)) {
                    lines.add(multiplierLine(
                            "item.mechanical_drive.screwdriver.mode." + key,
                            suspension.getDouble(key)
                    ));
                }
            }
        }

        lines.add(loreText(Component.empty(), ChatFormatting.GRAY));
        lines.add(loreText(
                Component.translatable(
                        "item.mechanical_drive.mount_settings_paper.hint"
                ),
                ChatFormatting.DARK_GRAY
        ));
        return new ItemLore(List.copyOf(lines));
    }

    private static ItemLore createTankPaperLore(CompoundTag suspension) {
        List<Component> lines = new ArrayList<>();
        lines.add(sectionTitle(
                "item.mechanical_drive.mount_settings_paper.suspension"
        ));
        for (String key : TUNING_KEYS) {
            if (suspension.contains(key, Tag.TAG_ANY_NUMERIC)) {
                lines.add(multiplierLine(
                        "item.mechanical_drive.screwdriver.mode." + key,
                        suspension.getDouble(key)
                ));
            }
        }
        if (suspension.contains(
                SpringTuningWrenchTarget.REST_ANGLE,
                Tag.TAG_ANY_NUMERIC
        )) {
            lines.add(strutSettingLine(
                    "item.mechanical_drive.spring_tuning_wrench.setting.length",
                    Component.translatable(
                            "item.mechanical_drive.spring_tuning_wrench.value.degrees",
                            String.format(
                                    Locale.ROOT,
                                    "%+.0f",
                                    suspension.getDouble(
                                            SpringTuningWrenchTarget.REST_ANGLE
                                    )
                            )
                    )
            ));
        }
        lines.add(loreText(Component.empty(), ChatFormatting.GRAY));
        lines.add(loreText(
                Component.translatable(
                        "item.mechanical_drive.mount_settings_paper.hint"
                ),
                ChatFormatting.DARK_GRAY
        ));
        return new ItemLore(List.copyOf(lines));
    }

    private static Component sectionTitle(String translationKey) {
        return Component.translatable(translationKey)
                .withStyle(style -> style
                        .withColor(ChatFormatting.GOLD)
                        .withBold(true)
                        .withItalic(false));
    }

    private static Component offsetLine(
            String labelKey,
            double value
    ) {
        return loreText(
                Component.translatable(
                        "item.mechanical_drive.mount_settings_paper.offset",
                        loreText(
                                Component.translatable(labelKey),
                                ChatFormatting.GRAY
                        ),
                        valueText(String.format(
                                Locale.ROOT,
                                "%+.3f",
                                value
                        ))
                ),
                ChatFormatting.GRAY
        );
    }

    private static Component multiplierLine(
            String labelKey,
            double value
    ) {
        return loreText(
                Component.translatable(
                        "item.mechanical_drive.mount_settings_paper.multiplier",
                        loreText(
                                Component.translatable(labelKey),
                                ChatFormatting.GRAY
                        ),
                        valueText(String.format(
                                Locale.ROOT,
                                "%.2fx",
                                value
                        ))
                ),
                ChatFormatting.GRAY
        );
    }

    private static Component valueText(String value) {
        return loreText(
                Component.literal(value),
                ChatFormatting.AQUA
        );
    }

    private static Component loreText(
            Component component,
            ChatFormatting color
    ) {
        return component.copy()
                .withStyle(style -> style
                        .withColor(color)
                        .withItalic(false));
    }

    private static void applySuspensionTuning(
            SuspensionSpringTuning tuning,
            CompoundTag values
    ) {
        for (String key : TUNING_KEYS) {
            if (!values.contains(key, Tag.TAG_ANY_NUMERIC)
                    || !tuning.mechanicalDrive$supportsSuspensionTuning(key)) {
                continue;
            }

            double target = SuspensionSpringTuning.clampMultiplier(
                    values.getDouble(key)
            );
            double current =
                    tuning.mechanicalDrive$getSuspensionTuning(key);
            double step = SuspensionSpringTuning.DRIVE.equals(key)
                    ? SuspensionSpringTuning.DRIVE_STEP
                    : SuspensionSpringTuning.SPRING_AND_GRIP_STEP;
            int steps = (int) Math.round((target - current) / step);
            if (steps != 0) {
                tuning.mechanicalDrive$adjustSuspensionTuning(
                        key,
                        steps
                );
            }
        }
    }

    private static CompoundTag writeSuspensionTuning(
            SuspensionSpringTuning tuning
    ) {
        CompoundTag suspension = new CompoundTag();
        for (String key : TUNING_KEYS) {
            if (tuning.mechanicalDrive$supportsSuspensionTuning(key)) {
                suspension.putDouble(
                        key,
                        tuning.mechanicalDrive$getSuspensionTuning(key)
                );
            }
        }
        return suspension;
    }

    private static CompoundTag writeTankSuspensionTuning(
            TorsionMountBlockEntity tuning
    ) {
        CompoundTag suspension = writeSuspensionTuning(tuning);
        suspension.putDouble(
                SuspensionSpringTuning.SPRING,
                tuning.mechanicalDrive$getSpringWrenchStrength()
        );
        suspension.putDouble(
                SpringTuningWrenchTarget.REST_ANGLE,
                tuning.mechanicalDrive$getSpringWrenchRestAngle()
        );
        return suspension;
    }

    private static boolean isValidTankProfile(CompoundTag profile) {
        if (!profile.contains(PROFILE_VERSION_TAG, Tag.TAG_ANY_NUMERIC)
                || profile.getInt(PROFILE_VERSION_TAG) != PROFILE_VERSION
                || !profile.contains(TUNING_TAG, Tag.TAG_COMPOUND)) {
            return false;
        }

        CompoundTag tuning = profile.getCompound(TUNING_TAG);
        for (String key : TUNING_KEYS) {
            if (!tuning.contains(key, Tag.TAG_ANY_NUMERIC)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidProfile(CompoundTag profile) {
        if (!profile.contains(
                PROFILE_VERSION_TAG,
                Tag.TAG_ANY_NUMERIC
        ) || profile.getInt(PROFILE_VERSION_TAG) != PROFILE_VERSION
                || !profile.contains(OFFSETS_TAG, Tag.TAG_COMPOUND)
                || !profile.contains(TUNING_TAG, Tag.TAG_COMPOUND)) {
            return false;
        }

        CompoundTag offsets = profile.getCompound(OFFSETS_TAG);
        if (!offsets.contains(
                WheelMountOffsetState.LATERAL_NBT_KEY,
                Tag.TAG_ANY_NUMERIC
        ) || !offsets.contains(
                WheelMountOffsetState.LONGITUDINAL_NBT_KEY,
                Tag.TAG_ANY_NUMERIC
        ) || !offsets.contains(
                WheelMountOffsetState.HEIGHT_NBT_KEY,
                Tag.TAG_ANY_NUMERIC
        )) {
            return false;
        }

        CompoundTag tuning = profile.getCompound(TUNING_TAG);
        for (String key : TUNING_KEYS) {
            if (tuning.contains(key)
                    && !tuning.contains(key, Tag.TAG_ANY_NUMERIC)) {
                return false;
            }
        }
        return true;
    }

    private static ItemStack getOtherHandItem(
            UseOnContext context,
            Player player
    ) {
        return player == null
                ? ItemStack.EMPTY
                : player.getItemInHand(getOtherHand(context));
    }

    private static InteractionHand getOtherHand(UseOnContext context) {
        return context.getHand() == InteractionHand.MAIN_HAND
                ? InteractionHand.OFF_HAND
                : InteractionHand.MAIN_HAND;
    }

    private static InteractionResult fail(
            Level level,
            BlockPos pos,
            Player player,
            String translationKey
    ) {
        level.playSound(
                null,
                pos,
                SoundEvents.DISPENSER_FAIL,
                SoundSource.BLOCKS,
                0.45F,
                1.0F
        );
        showMessage(
                player,
                translationKey,
                ChatFormatting.RED
        );
        return InteractionResult.sidedSuccess(false);
    }

    private static InteractionResult succeed(
            Level level,
            BlockPos pos,
            Player player,
            String translationKey
    ) {
        level.playSound(
                null,
                pos,
                SoundEvents.UI_BUTTON_CLICK.value(),
                SoundSource.BLOCKS,
                0.5F,
                1.1F
        );
        showMessage(
                player,
                translationKey,
                ChatFormatting.GREEN
        );
        return InteractionResult.sidedSuccess(false);
    }

    private static void showMessage(
            Player player,
            String translationKey,
            ChatFormatting formatting
    ) {
        if (player != null) {
            player.displayClientMessage(
                    Component.translatable(translationKey)
                            .withStyle(formatting),
                    true
            );
        }
    }

    private static WrenchMode cycleMode(ItemStack stack, int direction) {
        WrenchMode[] modes = WrenchMode.values();
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

    private static WrenchMode getMode(ItemStack stack) {
        WrenchMode[] modes = WrenchMode.values();
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

    private enum WrenchMode {
        ADJUST(
                "item.mechanical_drive.adjustment_wrench.mode.adjust"
        ),
        COPY(
                "item.mechanical_drive.adjustment_wrench.mode.copy"
        ),
        PASTE(
                "item.mechanical_drive.adjustment_wrench.mode.paste"
        );

        private final String translationKey;

        WrenchMode(String translationKey) {
            this.translationKey = translationKey;
        }
    }
}
