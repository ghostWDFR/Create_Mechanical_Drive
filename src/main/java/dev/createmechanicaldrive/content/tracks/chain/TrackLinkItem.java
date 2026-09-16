package dev.createmechanicaldrive.content.tracks.chain;

import dev.createmechanicaldrive.content.tracks.mounts.sprocket.SprocketMountBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class TrackLinkItem extends Item {
    private static final String SELECTION_TAG =
            "MechanicalDriveTrackSelection";
    private static final String TYPE_TAG = "Type";
    private static final String DIMENSION_TAG = "Dimension";
    private static final String NODES_TAG = "Nodes";
    private static final String OFF_HAND_TAG = "OffHand";
    private static final double TRACK_CLEARANCE = 0.5D / 16.0D;

    private final TrackType type;

    public TrackLinkItem(TrackType type, Properties properties) {
        super(properties);
        this.type = type;
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

    private InteractionResult handleUseOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos clickedPos = context.getClickedPos();
        TrackWheelLookup.WheelNode clicked = TrackWheelLookup.find(
                level,
                clickedPos,
                1.0F
        );
        if (player == null || clicked == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        CompoundTag persistentData = player.getPersistentData();
        CompoundTag selection = persistentData.getCompound(SELECTION_TAG);
        if (selection.isEmpty()) {
            if (clicked.kind() != TrackWheelLookup.Kind.SPROCKET) {
                message(player, "track_link.start_with_sprocket",
                        ChatFormatting.RED);
                return InteractionResult.CONSUME;
            }
            BlockEntity blockEntity = level.getBlockEntity(clickedPos);
            if (!(blockEntity instanceof SprocketMountBlockEntity sprocket)
                    || sprocket.getTrackAssembly() != null) {
                message(player, "track_link.sprocket_in_use",
                        ChatFormatting.RED);
                return InteractionResult.CONSUME;
            }

            selection.putString(TYPE_TAG, type.serializedName());
            selection.putString(
                    DIMENSION_TAG,
                    level.dimension().location().toString()
            );
            selection.putBoolean(
                    OFF_HAND_TAG,
                    context.getHand() == InteractionHand.OFF_HAND
            );
            selection.putLongArray(NODES_TAG, new long[]{clickedPos.asLong()});
            persistentData.put(SELECTION_TAG, selection);
            message(player, "track_link.started", ChatFormatting.GOLD);
            playClick(level, clickedPos, 0.95F);
            return InteractionResult.CONSUME;
        }

        TrackType selectedType = TrackType.fromSerializedName(
                selection.getString(TYPE_TAG)
        );
        String dimension = level.dimension().location().toString();
        if (selectedType != type
                || !dimension.equals(selection.getString(DIMENSION_TAG))) {
            message(player, "track_link.wrong_selection",
                    ChatFormatting.RED);
            return InteractionResult.CONSUME;
        }

        List<BlockPos> nodes = readNodes(selection);
        if (nodes.isEmpty()) {
            clearSelection(player);
            return InteractionResult.CONSUME;
        }
        BlockPos sprocketPos = nodes.get(0);
        if (clickedPos.equals(sprocketPos)) {
            return finish(level, player, nodes);
        }

        TrackWheelLookup.WheelNode sprocket = TrackWheelLookup.find(
                level,
                sprocketPos,
                1.0F
        );
        if (sprocket == null
                || sprocket.kind() != TrackWheelLookup.Kind.SPROCKET) {
            clearSelection(player);
            message(player, "track_link.sprocket_missing",
                    ChatFormatting.RED);
            return InteractionResult.CONSUME;
        }
        if (clicked.kind() == TrackWheelLookup.Kind.SPROCKET) {
            message(player, "track_link.only_one_sprocket",
                    ChatFormatting.RED);
            return InteractionResult.CONSUME;
        }
        if (clicked.facing() != sprocket.facing()
                || coordinate(clickedPos, sprocket.facing())
                != coordinate(sprocketPos, sprocket.facing())) {
            message(player, "track_link.wrong_plane", ChatFormatting.RED);
            return InteractionResult.CONSUME;
        }
        if (nodes.contains(clickedPos)) {
            message(player, "track_link.already_selected",
                    ChatFormatting.RED);
            return InteractionResult.CONSUME;
        }
        BlockEntity selectedBlockEntity = level.getBlockEntity(clickedPos);
        if (!(selectedBlockEntity instanceof TrackLinkedWheel linked)
                || linked.mechanicalDrive$getTrackSprocket() != null) {
            message(player, "track_link.wheel_in_use", ChatFormatting.RED);
            return InteractionResult.CONSUME;
        }

        nodes.add(clickedPos.immutable());
        selection.putLongArray(
                NODES_TAG,
                nodes.stream().mapToLong(BlockPos::asLong).toArray()
        );
        persistentData.put(SELECTION_TAG, selection);
        message(player, "track_link.wheel_added", ChatFormatting.GOLD,
                nodes.size() - 1);
        playClick(level, clickedPos, 1.05F);
        return InteractionResult.CONSUME;
    }

    private InteractionResult finish(
            Level level,
            Player player,
            List<BlockPos> nodes
    ) {
        BlockPos sprocketPos = nodes.get(0);
        BlockEntity ownerBlockEntity = level.getBlockEntity(sprocketPos);
        if (!(ownerBlockEntity instanceof SprocketMountBlockEntity owner)
                || owner.getTrackAssembly() != null) {
            clearSelection(player);
            message(player, "track_link.sprocket_missing",
                    ChatFormatting.RED);
            return InteractionResult.CONSUME;
        }

        TrackWheelLookup.WheelNode sprocket = TrackWheelLookup.find(
                level,
                sprocketPos,
                1.0F
        );
        if (sprocket == null) {
            clearSelection(player);
            message(player, "track_link.sprocket_missing",
                    ChatFormatting.RED);
            return InteractionResult.CONSUME;
        }

        List<TrackPath.Wheel> pathWheels = new ArrayList<>(nodes.size());
        boolean hasDriveWheel = false;
        Vec3 anchor = sprocket.center();
        var rolling = sprocket.facing().getClockWise();
        for (int index = 0; index < nodes.size(); index++) {
            BlockPos nodePos = nodes.get(index);
            TrackWheelLookup.WheelNode wheel = TrackWheelLookup.find(
                    level,
                    nodePos,
                    1.0F
            );
            if (wheel == null
                    || wheel.facing() != sprocket.facing()
                    || coordinate(nodePos, sprocket.facing())
                    != coordinate(sprocketPos, sprocket.facing())
                    || index > 0 && wheel.kind()
                    == TrackWheelLookup.Kind.SPROCKET) {
                message(player, "track_link.invalid_path",
                        ChatFormatting.RED);
                return InteractionResult.CONSUME;
            }
            if (index > 0) {
                BlockEntity blockEntity = level.getBlockEntity(nodePos);
                if (!(blockEntity instanceof TrackLinkedWheel linked)
                        || linked.mechanicalDrive$getTrackSprocket() != null) {
                    message(player, "track_link.wheel_in_use",
                            ChatFormatting.RED);
                    return InteractionResult.CONSUME;
                }
            }
            hasDriveWheel |= wheel.kind() == TrackWheelLookup.Kind.DRIVE;
            Vec3 relative = wheel.center().subtract(anchor);
            double u = relative.x * rolling.getStepX()
                    + relative.z * rolling.getStepZ();
            pathWheels.add(new TrackPath.Wheel(
                    u,
                    relative.y,
                    wheel.radius() + TRACK_CLEARANCE,
                    wheel.kind() == TrackWheelLookup.Kind.DRIVE
            ));
        }

        if (!hasDriveWheel) {
            message(player, "track_link.needs_drive_wheel",
                    ChatFormatting.RED);
            return InteractionResult.CONSUME;
        }
        TrackPath path = TrackPath.build(pathWheels);
        if (path == null) {
            message(player, "track_link.invalid_path",
                    ChatFormatting.RED);
            return InteractionResult.CONSUME;
        }

        int required = Math.max(
                1,
                (int) Math.floor(path.length() / TrackType.LINK_PITCH)
        );
        int available = countLinks(player);
        if (!player.hasInfiniteMaterials() && available < required) {
            message(player, "track_link.not_enough", ChatFormatting.RED,
                    required, available);
            return InteractionResult.CONSUME;
        }
        if (!player.hasInfiniteMaterials()) {
            consumeLinks(player, required);
        }

        owner.setTrackAssembly(new TrackAssembly(type, nodes, required));
        for (int index = 1; index < nodes.size(); index++) {
            BlockEntity blockEntity = level.getBlockEntity(nodes.get(index));
            if (blockEntity instanceof TrackLinkedWheel linked) {
                linked.mechanicalDrive$setTrackSprocket(sprocketPos);
            }
        }
        clearSelection(player);
        message(player, "track_link.completed", ChatFormatting.GREEN,
                required);
        level.playSound(
                null,
                sprocketPos,
                SoundEvents.CHAIN_PLACE,
                SoundSource.BLOCKS,
                0.8F,
                1.0F
        );
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide
                && player.getPersistentData().contains(SELECTION_TAG)) {
            clearSelection(player);
            message(player, "track_link.cancelled", ChatFormatting.GRAY);
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable(
                "item.mechanical_drive.track_link.tooltip."
                        + type.serializedName()
        ).withStyle(ChatFormatting.GRAY));
    }

    private int countLinks(Player player) {
        int count = 0;
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(this)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private void consumeLinks(Player player, int count) {
        Inventory inventory = player.getInventory();
        int remaining = count;
        for (int slot = 0;
                slot < inventory.getContainerSize() && remaining > 0;
                slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.is(this)) {
                continue;
            }
            int removed = Math.min(stack.getCount(), remaining);
            stack.shrink(removed);
            remaining -= removed;
        }
        inventory.setChanged();
    }

    private static List<BlockPos> readNodes(CompoundTag selection) {
        long[] packed = selection.getLongArray(NODES_TAG);
        List<BlockPos> nodes = new ArrayList<>(packed.length);
        for (long value : packed) {
            nodes.add(BlockPos.of(value));
        }
        return nodes;
    }

    private static int coordinate(BlockPos pos, net.minecraft.core.Direction facing) {
        return switch (facing.getAxis()) {
            case X -> pos.getX();
            case Y -> pos.getY();
            case Z -> pos.getZ();
        };
    }

    private static void clearSelection(Player player) {
        player.getPersistentData().remove(SELECTION_TAG);
    }

    static void cancelSelectionIfItemChanged(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(SELECTION_TAG)) {
            return;
        }

        CompoundTag selection = persistentData.getCompound(SELECTION_TAG);
        TrackType selectedType = TrackType.fromSerializedName(
                selection.getString(TYPE_TAG)
        );
        InteractionHand selectionHand = selection.getBoolean(OFF_HAND_TAG)
                ? InteractionHand.OFF_HAND
                : InteractionHand.MAIN_HAND;
        ItemStack held = player.getItemInHand(selectionHand);
        if (selectedType != null
                && held.getItem() instanceof TrackLinkItem link
                && link.type == selectedType) {
            return;
        }

        clearSelection(player);
        message(player, "track_link.cancelled", ChatFormatting.GRAY);
    }

    private static void playClick(Level level, BlockPos pos, float pitch) {
        level.playSound(
                null,
                pos,
                SoundEvents.CHAIN_PLACE,
                SoundSource.BLOCKS,
                0.45F,
                pitch
        );
    }

    private static void message(
            Player player,
            String suffix,
            ChatFormatting formatting,
            Object... arguments
    ) {
        player.displayClientMessage(
                Component.translatable(
                        "item.mechanical_drive." + suffix,
                        arguments
                ).withStyle(formatting),
                true
        );
    }
}
