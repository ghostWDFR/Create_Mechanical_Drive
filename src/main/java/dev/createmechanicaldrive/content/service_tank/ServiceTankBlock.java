package dev.createmechanicaldrive.content.service_tank;

import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.foundation.fluid.FluidHelper;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

public class ServiceTankBlock extends FluidTankBlock {
    public static final DirectionProperty HORIZONTAL_FACING =
            BlockStateProperties.HORIZONTAL_FACING;

    public ServiceTankBlock(Properties properties) {
        super(properties, false);
        registerDefaultState(defaultBlockState()
                .setValue(HORIZONTAL_FACING, Direction.NORTH)
                .setValue(TOP, true)
                .setValue(BOTTOM, true)
                .setValue(SHAPE, Shape.PLAIN));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HORIZONTAL_FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(
                HORIZONTAL_FACING,
                context.getHorizontalDirection().getOpposite()
        );
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(
                HORIZONTAL_FACING,
                rotation.rotate(state.getValue(HORIZONTAL_FACING))
        );
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(HORIZONTAL_FACING)));
    }

    @Override
    public BlockEntityType<? extends FluidTankBlockEntity> getBlockEntityType() {
        return CreateMechanicalDrive.SERVICE_TANK_BLOCK_ENTITY.get();
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        if (stack.is(Items.BUCKET)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!(stack.getItem() instanceof BucketItem)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hit);
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof ServiceTankBlockEntity tank)) {
            return ItemInteractionResult.FAIL;
        }
        return FluidHelper.tryEmptyItemIntoBE(level, player, hand, stack, tank)
                ? ItemInteractionResult.SUCCESS
                : ItemInteractionResult.FAIL;
    }

    @Override
    public BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos
    ) {
        if (level.getBlockEntity(pos) instanceof ServiceTankBlockEntity tank) {
            tank.requestNetworkUpdate();
        }
        return state;
    }

    @Override
    public void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean movedByPiston
    ) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof ServiceTankBlockEntity tank) {
            tank.detachAll();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
