package dev.createmechanicaldrive.content.shaft_distributor;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.HitResult;

import java.util.List;

public class ShaftDistributorBlock
        extends RotatedPillarKineticBlock
        implements IBE<ShaftDistributorBlockEntity>, IWrenchable {

    public static final DirectionProperty FACING =
            BlockStateProperties.FACING;

    public static final DirectionProperty OUTPUT_FACING =
            DirectionProperty.create(
                    "output_facing"
            );

    public ShaftDistributorBlock(
            Properties properties
    ) {
        super(properties);

        registerDefaultState(
                defaultBlockState()
                        .setValue(AXIS, Direction.Axis.Z)
                        .setValue(FACING, Direction.NORTH)
                        .setValue(OUTPUT_FACING, Direction.EAST)
        );
    }

    @Override
    public BlockState getStateForPlacement(
            BlockPlaceContext context
    ) {
        Player player =
                context.getPlayer();

        boolean crouching =
                player != null
                        && player.isCrouching();

        Direction facing =
                context.getNearestLookingDirection();

        if (crouching) {
            facing =
                    facing.getOpposite();
        }

        Direction outputFacing;

        if (facing.getAxis() == Direction.Axis.Y) {
            outputFacing =
                    context.getHorizontalDirection()
                            .getClockWise();
        } else {
            outputFacing =
                    facing.getClockWise();
        }

        return defaultBlockState()
                .setValue(FACING, facing)
                .setValue(AXIS, facing.getAxis())
                .setValue(OUTPUT_FACING, outputFacing);
    }

    public static Direction.Axis getOutputAxis(
            BlockState state
    ) {
        return getOutputDirection(state)
                .getAxis();
    }

    public static Direction getOutputDirection(
            BlockState state
    ) {
        Direction inputSide =
                state.getValue(FACING);

        Direction outputFacing =
                state.getValue(OUTPUT_FACING);

        if (outputFacing.getAxis() != inputSide.getAxis()) {
            return outputFacing;
        }

        return inputSide.getAxis() == Direction.Axis.Y
                ? Direction.EAST
                : inputSide.getClockWise();
    }

    public static boolean isShaftDirection(
            BlockState state,
            Direction direction
    ) {
        return direction == state.getValue(FACING)
                || direction.getAxis() == getOutputAxis(state);
    }

    public static float getInputAxisSign(
            BlockState state
    ) {
        return state.getValue(FACING).getAxisDirection()
                == Direction.AxisDirection.POSITIVE
                ? -1.0F
                : 1.0F;
    }

    public static float getOutputAxisSign(
            BlockState state
    ) {
        return getOutputDirection(state).getAxisDirection()
                == Direction.AxisDirection.POSITIVE
                ? 1.0F
                : -1.0F;
    }

    public static float getCrossAxisSpeedModifier(
            BlockState state,
            Direction source,
            Direction target
    ) {
        if (!isShaftDirection(state, source)
                || !isShaftDirection(state, target)) {
            return 0.0F;
        }

        Direction normalizedSource =
                normalizeShaftSide(
                        state,
                        source
                );

        Direction normalizedTarget =
                normalizeShaftSide(
                        state,
                        target
                );

        if (normalizedSource.getAxis() == normalizedTarget.getAxis()) {
            return 1.0F;
        }

        float modifier =
                getCrossAxisPairModifier(
                        normalizedSource,
                        normalizedTarget
                );

        if (normalizedSource.getAxis() == Direction.Axis.X
                || normalizedTarget.getAxis() == Direction.Axis.X) {
            modifier = -modifier;
        }

        return modifier;
    }

    private static Direction normalizeShaftSide(
            BlockState state,
            Direction side
    ) {
        Direction outputDirection =
                getOutputDirection(state);

        if (side.getAxis() == outputDirection.getAxis()) {
            return outputDirection;
        }

        return state.getValue(FACING);
    }

    private static float getCrossAxisPairModifier(
            Direction source,
            Direction target
    ) {
        return getCanonicalSideModifier(target)
                / getCanonicalSideModifier(source);
    }

    private static float getCanonicalSideModifier(
            Direction direction
    ) {
        return switch (direction) {
            case WEST, SOUTH, UP -> 1.0F;
            case EAST, NORTH, DOWN -> -1.0F;
        };
    }

    @Override
    public boolean hasShaftTowards(
            LevelReader level,
            BlockPos pos,
            BlockState state,
            Direction direction
    ) {
        return isShaftDirection(state, direction);
    }

    @Override
    protected boolean areStatesKineticallyEquivalent(
            BlockState oldState,
            BlockState newState
    ) {
        return oldState.getBlock() == newState.getBlock()
                && oldState.getValue(FACING) == newState.getValue(FACING)
                && getOutputDirection(oldState) == getOutputDirection(newState);
    }

    @Override
    public Direction.Axis getRotationAxis(
            BlockState state
    ) {
        return state.getValue(AXIS);
    }

    @Override
    public InteractionResult onWrenched(
            BlockState state,
            UseOnContext context
    ) {
        Level level =
                context.getLevel();

        BlockPos pos =
                context.getClickedPos();

        BlockState rotatedState =
                getRotatedBlockState(
                        state,
                        context.getClickedFace()
                );

        if (!rotatedState.canSurvive(level, pos)) {
            return InteractionResult.PASS;
        }

        BlockState updatedState =
                updateAfterWrenched(
                        rotatedState,
                        context
                );

        if (updatedState == state) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        KineticBlockEntity.switchToBlockState(
                level,
                pos,
                updatedState
        );

        if (level.getBlockEntity(pos)
                instanceof KineticBlockEntity kinetic) {
            kinetic.updateSpeed = true;
            kinetic.setChanged();
        }

        IWrenchable.playRotateSound(
                level,
                pos
        );

        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockState getRotatedBlockState(
            BlockState state,
            Direction targetedFace
    ) {
        Direction rotatedFacing =
                rotateClockwiseAroundFace(
                        state.getValue(FACING),
                        targetedFace
                );

        Direction rotatedOutput =
                rotateClockwiseAroundFace(
                        getOutputDirection(state),
                        targetedFace
                );

        return state
                .setValue(FACING, rotatedFacing)
                .setValue(AXIS, rotatedFacing.getAxis())
                .setValue(
                        OUTPUT_FACING,
                        rotatedOutput
                );
    }

    private static Direction rotateClockwiseAroundFace(
            Direction direction,
            Direction face
    ) {
        if (direction.getAxis() == face.getAxis()) {
            return direction;
        }

        int normalX = face.getStepX();
        int normalY = face.getStepY();
        int normalZ = face.getStepZ();
        int directionX = direction.getStepX();
        int directionY = direction.getStepY();
        int directionZ = direction.getStepZ();

        return directionFromSteps(
                normalZ * directionY - normalY * directionZ,
                normalX * directionZ - normalZ * directionX,
                normalY * directionX - normalX * directionY
        );
    }

    private static Direction directionFromSteps(
            int x,
            int y,
            int z
    ) {
        if (x > 0) {
            return Direction.EAST;
        }

        if (x < 0) {
            return Direction.WEST;
        }

        if (y > 0) {
            return Direction.UP;
        }

        if (y < 0) {
            return Direction.DOWN;
        }

        if (z > 0) {
            return Direction.SOUTH;
        }

        return Direction.NORTH;
    }

    @Override
    public float getParticleTargetRadius() {
        return 0.85F;
    }

    @Override
    public float getParticleInitialRadius() {
        return 0.75F;
    }

    @Override
    public List<ItemStack> getDrops(
            BlockState state,
            LootParams.Builder params
    ) {
        return List.of(
                new ItemStack(
                        CreateMechanicalDrive
                                .SHAFT_DISTRIBUTOR_ITEM
                                .get()
                )
        );
    }

    @Override
    public ItemStack getCloneItemStack(
            BlockState state,
            HitResult target,
            LevelReader level,
            BlockPos pos,
            Player player
    ) {
        return new ItemStack(
                CreateMechanicalDrive
                        .SHAFT_DISTRIBUTOR_ITEM
                        .get()
        );
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, OUTPUT_FACING);
    }

    @Override
    public Class<ShaftDistributorBlockEntity> getBlockEntityClass() {
        return ShaftDistributorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ShaftDistributorBlockEntity>
    getBlockEntityType() {
        return CreateMechanicalDrive
                .SHAFT_DISTRIBUTOR_BLOCK_ENTITY
                .get();
    }
}