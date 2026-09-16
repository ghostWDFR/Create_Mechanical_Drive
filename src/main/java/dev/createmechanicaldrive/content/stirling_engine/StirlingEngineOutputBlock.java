package dev.createmechanicaldrive.content.stirling_engine;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.HitResult;

import javax.annotation.Nullable;
import java.util.List;

public class StirlingEngineOutputBlock extends Block implements EntityBlock, IWrenchable {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;

    public StirlingEngineOutputBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(AXIS, Direction.Axis.Z));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();

        for (Direction direction : Direction.values()) {
            BlockState coreState = level.getBlockState(pos.relative(direction));
            if (coreState.is(CreateMechanicalDrive.STIRLING_ENGINE_CORE.get())
                    && coreState.getValue(StirlingEngineCoreBlock.FACING) == direction.getOpposite()) {
                return stateFacing(direction.getOpposite());
            }
        }

        Direction facing = context.getNearestLookingDirection().getOpposite();
        return stateFacing(facing);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        updatePoweredShaft(level, pos, state);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockPos shaftPos = getShaftPos(state, pos);
            BlockState shaftState = level.getBlockState(shaftPos);
            if (shaftState.is(CreateMechanicalDrive.STIRLING_ENGINE_POWERED_SHAFT.get())) {
                level.setBlock(shaftPos, StirlingEnginePoweredShaftBlock.getShaftEquivalent(shaftState), 3);
            }
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    public static void updatePoweredShaft(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) {
            return;
        }

        BlockPos shaftPos = getShaftPos(state, pos);
        BlockState shaftState = level.getBlockState(shaftPos);

        if (!isShaftValid(state, shaftState)) {
            return;
        }

        if (AllBlocks.SHAFT.has(shaftState)) {
            level.setBlock(shaftPos, StirlingEnginePoweredShaftBlock.getPoweredEquivalent(shaftState), 3);
            return;
        }

        if (level.getBlockEntity(shaftPos) instanceof StirlingEnginePoweredShaftBlockEntity poweredShaft) {
            poweredShaft.refreshGeneratedState();
        }
    }

    public static BlockPos getShaftPos(BlockState state, BlockPos pos) {
        return pos.relative(getConnectedDirection(state), 2);
    }

    public static Direction getConnectedDirection(BlockState state) {
        return state.getValue(FACING);
    }

    public static boolean isShaftValid(BlockState engineState, BlockState shaftState) {
        boolean shaft = AllBlocks.SHAFT.has(shaftState)
                || shaftState.is(CreateMechanicalDrive.STIRLING_ENGINE_POWERED_SHAFT.get());

        return shaft
                && shaftState.getValue(StirlingEnginePoweredShaftBlock.AXIS)
                != getConnectedDirection(engineState).getAxis();
    }

    private BlockState stateFacing(Direction facing) {
        return defaultBlockState()
                .setValue(FACING, facing)
                .setValue(AXIS, facing.getAxis());
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(new ItemStack(this));
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        return new ItemStack(this);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, AXIS);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StirlingEngineOutputBlockEntity(
                CreateMechanicalDrive.STIRLING_ENGINE_OUTPUT_BLOCK_ENTITY.get(),
                pos,
                state
        );
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> blockEntityType
    ) {
        if (blockEntityType != CreateMechanicalDrive.STIRLING_ENGINE_OUTPUT_BLOCK_ENTITY.get()) {
            return null;
        }

        return (tickerLevel, tickerPos, tickerState, blockEntity) ->
                ((StirlingEngineOutputBlockEntity) blockEntity).tick();
    }
}

