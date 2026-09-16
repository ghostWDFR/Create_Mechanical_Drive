package dev.createmechanicaldrive.content.double_rigid_steering_wheel_mount;

import com.simibubi.create.api.schematic.requirement.SpecialBlockItemRequirement;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.ItemUseType;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.StackRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.StrictNbtStackRequirement;
import com.simibubi.create.foundation.block.IBE;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.createmechanicaldrive.content.shaft_marker.ShaftMarkerItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class DoubleRigidSteeringWheelMountBlock
        extends HorizontalKineticBlock
        implements IBE<DoubleRigidSteeringWheelMountBlockEntity>,
        SpecialBlockItemRequirement {

    public DoubleRigidSteeringWheelMountBlock(
            Properties properties
    ) {
        super(properties);
    }

    public static Direction getDriveShaftDirection(
            BlockState state
    ) {
        return state
                .getValue(HORIZONTAL_FACING)
                .getCounterClockWise();
    }

    public static Direction getSteeringShaftDirection(
            BlockState state
    ) {
        return getDriveShaftDirection(
                state
        ).getOpposite();
    }

    @Override
    public boolean hasShaftTowards(
            LevelReader level,
            BlockPos pos,
            BlockState state,
            Direction face
    ) {
        return face ==
                getDriveShaftDirection(
                        state
                );
    }

    @Override
    public Axis getRotationAxis(
            BlockState state
    ) {
        return getDriveShaftDirection(
                state
        ).getAxis();
    }

    @Override
    public BlockState getStateForPlacement(
            BlockPlaceContext context
    ) {
        Direction preferred =
                getPreferredHorizontalFacing(
                        context
                );

        boolean reversed =
                context.getPlayer() != null
                        && context.getPlayer()
                        .isShiftKeyDown();

        if (preferred != null
                && !reversed) {

            return defaultBlockState()
                    .setValue(
                            HORIZONTAL_FACING,
                            preferred
                                    .getOpposite()
                                    .getCounterClockWise()
                    );
        }

        Direction playerFacing =
                context.getHorizontalDirection();

        Direction facing =
                reversed
                        ? playerFacing
                        : playerFacing.getOpposite();

        return defaultBlockState()
                .setValue(
                        HORIZONTAL_FACING,
                        facing.getCounterClockWise()
                );
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack held,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        int slot =
                getWheelSlot(
                        state,
                        hit.getDirection()
                );

        if (slot < 0) {
            return super.useItemOn(
                    held,
                    state,
                    level,
                    pos,
                    player,
                    hand,
                    hit
            );
        }

        if (!held.isEmpty() && !ShaftMarkerItem.isWheelSlotItem(held)) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }

        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }

        return swapWheel(
                level,
                pos,
                slot,
                player,
                held
        )
                ? ItemInteractionResult.CONSUME
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit
    ) {
        if (!player.getMainHandItem().isEmpty()) {
            return InteractionResult.PASS;
        }

        int slot =
                getWheelSlot(
                        state,
                        hit.getDirection()
                );

        if (slot < 0) {
            return super.useWithoutItem(
                    state,
                    level,
                    pos,
                    player,
                    hit
            );
        }

        DoubleRigidSteeringWheelMountBlockEntity mount =
                getBlockEntity(
                        level,
                        pos
                );

        if (mount == null
                || mount.getWheel(slot).isEmpty()) {

            return super.useWithoutItem(
                    state,
                    level,
                    pos,
                    player,
                    hit
            );
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        return swapWheel(
                level,
                pos,
                slot,
                player,
                ItemStack.EMPTY
        )
                ? InteractionResult.CONSUME
                : InteractionResult.PASS;
    }

    private static int getWheelSlot(
            BlockState state,
            Direction side
    ) {
        Direction facing =
                state.getValue(
                        HORIZONTAL_FACING
                );

        if (side == facing) {
            return 0;
        }

        if (side == facing.getOpposite()) {
            return 1;
        }

        return -1;
    }

    private static boolean swapWheel(
            Level level,
            BlockPos pos,
            int slot,
            Player player,
            ItemStack held
    ) {
        DoubleRigidSteeringWheelMountBlockEntity mount =
                getBlockEntity(
                        level,
                        pos
                );

        if (mount == null
                || slot < 0
                || slot > 1
                || (
                !held.isEmpty()
                        && !ShaftMarkerItem.isWheelSlotItem(held)
        )) {
            return false;
        }

        ItemStack previous =
                mount.getWheel(
                        slot
                ).copy();

        ItemStack replacement =
                held.isEmpty()
                        ? ItemStack.EMPTY
                        : held.copyWithCount(
                        1
                );

        mount.getInventory()
                .setStackInSlot(
                        slot,
                        replacement
                );

        if (!held.isEmpty()
                && !player.hasInfiniteMaterials()) {

            held.shrink(
                    1
            );
        }

        if (!previous.isEmpty()) {
            player.getInventory()
                    .placeItemBackInInventory(
                            previous
                    );
        }

        float pitch =
                0.8F
                        + level.random.nextFloat()
                        * 0.4F;

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

        return true;
    }

    @Nullable
    private static DoubleRigidSteeringWheelMountBlockEntity getBlockEntity(
            Level level,
            BlockPos pos
    ) {
        BlockEntity blockEntity =
                level.getBlockEntity(
                        pos
                );

        return blockEntity
                instanceof DoubleRigidSteeringWheelMountBlockEntity mount
                ? mount
                : null;
    }

    @Override
    public void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean moving
    ) {
        if (state.getBlock()
                != newState.getBlock()) {

            DoubleRigidSteeringWheelMountBlockEntity mount =
                    getBlockEntity(
                            level,
                            pos
                    );

            if (mount != null) {
                Direction facing =
                        state.getValue(
                                HORIZONTAL_FACING
                        );

                dropWheel(
                        level,
                        pos.relative(
                                facing
                        ),
                        mount,
                        0
                );

                dropWheel(
                        level,
                        pos.relative(
                                facing.getOpposite()
                        ),
                        mount,
                        1
                );
            }
        }

        super.onRemove(
                state,
                level,
                pos,
                newState,
                moving
        );
    }

    private static void dropWheel(
            Level level,
            BlockPos dropPos,
            DoubleRigidSteeringWheelMountBlockEntity mount,
            int slot
    ) {
        ItemStack wheel =
                mount.getWheel(
                        slot
                );

        if (wheel.isEmpty()) {
            return;
        }

        Containers.dropItemStack(
                level,
                dropPos.getX() + 0.5D,
                dropPos.getY() + 0.5D,
                dropPos.getZ() + 0.5D,
                wheel.copy()
        );

        mount.getInventory()
                .setWithoutNotification(
                        slot,
                        ItemStack.EMPTY
                );
    }

    @Override
    public VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return Shapes.block();
    }

    @Override
    public Class<DoubleRigidSteeringWheelMountBlockEntity> getBlockEntityClass() {
        return DoubleRigidSteeringWheelMountBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends DoubleRigidSteeringWheelMountBlockEntity> getBlockEntityType() {
        return CreateMechanicalDrive
                .DOUBLE_RIGID_STEERING_WHEEL_MOUNT_BLOCK_ENTITY
                .get();
    }

    @Override
    public ItemRequirement getRequiredItems(
            BlockState state,
            @Nullable BlockEntity blockEntity
    ) {
        ItemStack mount =
                CreateMechanicalDrive
                        .DOUBLE_RIGID_STEERING_WHEEL_MOUNT_ITEM
                        .get()
                        .getDefaultInstance();

        List<StackRequirement> requirements =
                new ArrayList<>();

        requirements.add(
                new StackRequirement(
                        mount,
                        ItemUseType.CONSUME
                )
        );

        if (blockEntity
                instanceof DoubleRigidSteeringWheelMountBlockEntity wheelMount) {

            for (int slot = 0; slot < 2; slot++) {
                ItemStack wheel =
                        wheelMount.getWheel(
                                slot
                        );

                if (!wheel.isEmpty()) {
                    requirements.add(
                            new StrictNbtStackRequirement(
                                    wheel,
                                    ItemUseType.CONSUME
                            )
                    );
                }
            }
        }

        return new ItemRequirement(
                requirements
        );
    }
}
