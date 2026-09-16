package dev.createmechanicaldrive.content.stirling_engine;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.HitResult;

import java.util.List;

public class StirlingEngineCoreBlock extends Block implements IWrenchable {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;

    public StirlingEngineCoreBlock(Properties properties) {
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
            BlockState outputState = level.getBlockState(pos.relative(direction));
            if (outputState.is(CreateMechanicalDrive.STIRLING_ENGINE_OUTPUT.get())
                    && outputState.getValue(StirlingEngineOutputBlock.FACING) == direction) {
                return stateFacing(direction);
            }

            BlockState heaterState = level.getBlockState(pos.relative(direction));
            if (heaterState.is(CreateMechanicalDrive.STIRLING_ENGINE_HEATER.get())
                    && heaterState.getValue(StirlingEngineHeaterBlock.FACING) == direction.getOpposite()) {
                return stateFacing(direction.getOpposite());
            }
        }

        return stateFacing(context.getNearestLookingDirection().getOpposite());
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        notifyOutput(level, pos, state);
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            BlockPos neighborPos,
            boolean movedByPiston
    ) {
        super.neighborChanged(
                state,
                level,
                pos,
                block,
                neighborPos,
                movedByPiston
        );

        notifyOutput(level, pos, state);
    }

    static boolean isValidAssembly(LevelReader level, BlockPos corePos, BlockState coreState) {
        if (!coreState.is(CreateMechanicalDrive.STIRLING_ENGINE_CORE.get())) {
            return false;
        }

        Direction facing = coreState.getValue(FACING);
        BlockState outputState = level.getBlockState(corePos.relative(facing));
        BlockState heaterState = level.getBlockState(corePos.relative(facing.getOpposite()));

        return outputState.is(CreateMechanicalDrive.STIRLING_ENGINE_OUTPUT.get())
                && outputState.getValue(StirlingEngineOutputBlock.FACING) == facing
                && heaterState.is(CreateMechanicalDrive.STIRLING_ENGINE_HEATER.get())
                && heaterState.getValue(StirlingEngineHeaterBlock.FACING) == facing;
    }

    private static void notifyOutput(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide || !state.hasProperty(FACING)) {
            return;
        }

        BlockPos outputPos = pos.relative(state.getValue(FACING));
        if (level.getBlockEntity(outputPos) instanceof StirlingEngineOutputBlockEntity output) {
            output.refreshPoweredShaft();
        }
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
}
