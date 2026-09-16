package dev.createmechanicaldrive.content.steering_wheel_mount;

import com.simibubi.create.api.schematic.requirement.SpecialBlockItemRequirement;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.ItemUseType;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.StackRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.StrictNbtStackRequirement;
import com.simibubi.create.foundation.block.IBE;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.createmechanicaldrive.content.shaft_marker.ShaftMarkerItem;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class SteeringWheelMountBlock extends HorizontalKineticBlock
        implements IBE<SteeringWheelMountBlockEntity>, SpecialBlockItemRequirement {

    public SteeringWheelMountBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        Direction facing = state.getValue(HORIZONTAL_FACING);
        return face == facing.getOpposite()
                || face.getAxis() == getSteeringShaftDirection(state).getAxis();
    }

    public static Direction getSteeringShaftDirection(BlockState state) {
        return state.getValue(HORIZONTAL_FACING).getClockWise();
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        return state.getValue(HORIZONTAL_FACING).getAxis();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction preferred = getPreferredHorizontalFacing(context);
        boolean reversed = context.getPlayer() != null && context.getPlayer().isShiftKeyDown();
        if (preferred != null && !reversed) {
            return defaultBlockState().setValue(HORIZONTAL_FACING, preferred.getOpposite());
        }

        Direction playerFacing = context.getHorizontalDirection();
        return defaultBlockState().setValue(
                HORIZONTAL_FACING,
                reversed ? playerFacing : playerFacing.getOpposite()
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
        if (!isWheelAccessFace(state, hit.getDirection())) {
            return super.useItemOn(held, state, level, pos, player, hand, hit);
        }

        if (!held.isEmpty() && !ShaftMarkerItem.isWheelSlotItem(held)) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }

        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }

        return swapWheel(level, pos, player, hand, held)
                ? ItemInteractionResult.CONSUME
                : super.useItemOn(held, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit
    ) {
        if (!isWheelAccessFace(state, hit.getDirection())) {
            return super.useWithoutItem(state, level, pos, player, hit);
        }

        SteeringWheelMountBlockEntity mount = getBlockEntity(level, pos);
        if (mount == null || mount.getWheel().isEmpty()) {
            return super.useWithoutItem(state, level, pos, player, hit);
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        return swapWheel(level, pos, player, InteractionHand.MAIN_HAND, ItemStack.EMPTY)
                ? InteractionResult.CONSUME
                : InteractionResult.PASS;
    }

    private static boolean isWheelAccessFace(BlockState state, Direction hitFace) {
        return hitFace == state.getValue(HORIZONTAL_FACING) || hitFace == Direction.DOWN;
    }

    private static boolean swapWheel(
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            ItemStack held
    ) {
        SteeringWheelMountBlockEntity mount = getBlockEntity(level, pos);
        if (mount == null || (!held.isEmpty()
                && !ShaftMarkerItem.isWheelSlotItem(held))) {
            return false;
        }

        ItemStack previous = mount.getWheel().copy();
        ItemStack replacement = held.isEmpty() ? ItemStack.EMPTY : held.copyWithCount(1);
        mount.getInventory().setStackInSlot(0, replacement);

        if (!held.isEmpty() && !player.hasInfiniteMaterials()) {
            held.shrink(1);
        }
        if (!previous.isEmpty()) {
            player.getInventory().placeItemBackInInventory(previous);
        }

        float pitch = 0.8F + level.random.nextFloat() * 0.4F;
        if (replacement.isEmpty()) {
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.75F, pitch);
        } else {
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.PLAYERS, 0.75F, pitch);
        }
        return true;
    }

    @Nullable
    private static SteeringWheelMountBlockEntity getBlockEntity(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof SteeringWheelMountBlockEntity mount ? mount : null;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (state.getBlock() != newState.getBlock()) {
            SteeringWheelMountBlockEntity mount = getBlockEntity(level, pos);
            if (mount != null && !mount.getWheel().isEmpty()) {
                Direction front = state.getValue(HORIZONTAL_FACING);
                BlockPos dropPos = pos.relative(front);
                Containers.dropItemStack(
                        level,
                        dropPos.getX() + 0.5,
                        dropPos.getY() + 0.5,
                        dropPos.getZ() + 0.5,
                        mount.getWheel().copy()
                );
                mount.getInventory().setWithoutNotification(ItemStack.EMPTY);
            }
        }
        super.onRemove(state, level, pos, newState, moving);
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
    public Class<SteeringWheelMountBlockEntity> getBlockEntityClass() {
        return SteeringWheelMountBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SteeringWheelMountBlockEntity> getBlockEntityType() {
        return CreateMechanicalDrive.STEERING_WHEEL_MOUNT_BLOCK_ENTITY.get();
    }

    @Override
    public ItemRequirement getRequiredItems(BlockState state, @Nullable BlockEntity blockEntity) {
        ItemStack mount = CreateMechanicalDrive.STEERING_WHEEL_MOUNT_ITEM.get().getDefaultInstance();
        if (blockEntity instanceof SteeringWheelMountBlockEntity wheelMount) {
            ItemStack wheel = wheelMount.getWheel();
            if (!wheel.isEmpty()) {
                return new ItemRequirement(List.of(
                        new StackRequirement(mount, ItemUseType.CONSUME),
                        new StrictNbtStackRequirement(wheel, ItemUseType.CONSUME)
                ));
            }
        }
        return new ItemRequirement(ItemUseType.CONSUME, mount);
    }
}
