package dev.createmechanicaldrive.content.chain_linkage;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.createmechanicaldrive.content.mechanical_jack.MechanicalJackPhysics;
import dev.ryanhcode.sable.api.block.BlockSubLevelAssemblyListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

public class ChainGearBlock
        extends RotatedPillarKineticBlock
        implements IBE<ChainGearBlockEntity>, IWrenchable,
        BlockSubLevelAssemblyListener {
    public static final BooleanProperty CONNECTED_NEGATIVE =
            BooleanProperty.create("connected_negative");

    public static final BooleanProperty CONNECTED_POSITIVE =
            BooleanProperty.create("connected_positive");

    private static final VoxelShape SHAPE_X =
            Block.box(
                    0.0D,
                    3.0D,
                    3.0D,
                    16.0D,
                    13.0D,
                    13.0D
            );

    private static final VoxelShape SHAPE_Y =
            Block.box(
                    3.0D,
                    0.0D,
                    3.0D,
                    13.0D,
                    16.0D,
                    13.0D
            );

    private static final VoxelShape SHAPE_Z =
            Block.box(
                    3.0D,
                    3.0D,
                    0.0D,
                    13.0D,
                    13.0D,
                    16.0D
            );

    private static final VoxelShape SHAPE_X_NEGATIVE =
            Block.box(
                    0.0D,
                    3.0D,
                    3.0D,
                    10.0D,
                    13.0D,
                    13.0D
            );

    private static final VoxelShape SHAPE_X_POSITIVE =
            Block.box(
                    6.0D,
                    3.0D,
                    3.0D,
                    16.0D,
                    13.0D,
                    13.0D
            );

    private static final VoxelShape SHAPE_Y_NEGATIVE =
            Block.box(
                    3.0D,
                    0.0D,
                    3.0D,
                    13.0D,
                    10.0D,
                    13.0D
            );

    private static final VoxelShape SHAPE_Y_POSITIVE =
            Block.box(
                    3.0D,
                    6.0D,
                    3.0D,
                    13.0D,
                    16.0D,
                    13.0D
            );

    private static final VoxelShape SHAPE_Z_NEGATIVE =
            Block.box(
                    3.0D,
                    3.0D,
                    0.0D,
                    13.0D,
                    13.0D,
                    10.0D
            );

    private static final VoxelShape SHAPE_Z_POSITIVE =
            Block.box(
                    3.0D,
                    3.0D,
                    6.0D,
                    13.0D,
                    13.0D,
                    16.0D
            );

    public ChainGearBlock(
            Properties properties
    ) {
        super(properties);

        registerDefaultState(
                defaultBlockState()
                        .setValue(CONNECTED_NEGATIVE, false)
                        .setValue(CONNECTED_POSITIVE, false)
        );
    }

    @Override
    public BlockState getStateForPlacement(
            BlockPlaceContext context
    ) {
        BlockPos pos =
                context.getClickedPos();

        LevelReader level =
                context.getLevel();

        Direction.Axis axis =
                findConnectableShaftAxis(
                        level,
                        pos
                );

        if (axis == null) {
            axis =
                    context.getNearestLookingDirection()
                            .getAxis();
        }

        return withShaftConnections(
                level,
                pos,
                defaultBlockState()
                        .setValue(
                                AXIS,
                                axis
                        )
        );
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        boolean connectedNegative =
                state.getValue(CONNECTED_NEGATIVE);

        boolean connectedPositive =
                state.getValue(CONNECTED_POSITIVE);

        Direction.Axis axis =
                state.getValue(AXIS);

        if (connectedNegative && !connectedPositive) {
            return switch (axis) {
                case X -> SHAPE_X_NEGATIVE;
                case Y -> SHAPE_Y_NEGATIVE;
                case Z -> SHAPE_Z_NEGATIVE;
            };
        }

        if (!connectedNegative && connectedPositive) {
            return switch (axis) {
                case X -> SHAPE_X_POSITIVE;
                case Y -> SHAPE_Y_POSITIVE;
                case Z -> SHAPE_Z_POSITIVE;
            };
        }

        return switch (axis) {
            case X -> SHAPE_X;
            case Y -> SHAPE_Y;
            case Z -> SHAPE_Z;
        };
    }

    @Override
    public Direction.Axis getRotationAxis(
            BlockState state
    ) {
        return state.getValue(
                AXIS
        );
    }

    @Override
    public boolean hasShaftTowards(
            LevelReader level,
            BlockPos pos,
            BlockState state,
            Direction direction
    ) {
        return direction.getAxis()
                == state.getValue(AXIS);
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            LevelAccessor level,
            BlockPos currentPos,
            BlockPos neighborPos
    ) {
        if (direction.getAxis()
                != state.getValue(AXIS)) {
            return super.updateShape(
                    state,
                    direction,
                    neighborState,
                    level,
                    currentPos,
                    neighborPos
            );
        }

        boolean connected =
                hasVisualConnection(
                        level,
                        neighborPos,
                        neighborState,
                        direction.getOpposite()
                );

        if (direction.getAxisDirection()
                == Direction.AxisDirection.NEGATIVE) {
            return state.setValue(
                    CONNECTED_NEGATIVE,
                    connected
            );
        }

        return state.setValue(
                CONNECTED_POSITIVE,
                connected
        );
    }

    @Override
    public void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean isMoving
    ) {
        if (!state.is(newState.getBlock())
                && !MechanicalJackPhysics.isDetachingHeadPayload()
                && level.getBlockEntity(pos)
                instanceof ChainGearBlockEntity gear
                && !gear.isAssemblyMoving()) {

            gear.destroyChain(
                    !level.isClientSide
            );
        }

        super.onRemove(
                state,
                level,
                pos,
                newState,
                isMoving
        );
    }

    @Override
    public InteractionResult onSneakWrenched(
            BlockState state,
            UseOnContext context
    ) {
        if (!(context.getLevel()
                instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }

        level.destroyBlock(
                context.getClickedPos(),
                true
        );

        IWrenchable.playRemoveSound(
                level,
                context.getClickedPos()
        );

        return InteractionResult.SUCCESS;
    }

    @Override
    public void beforeMove(
            ServerLevel originLevel,
            ServerLevel resultingLevel,
            BlockState newState,
            BlockPos oldPos,
            BlockPos newPos
    ) {
        if (originLevel.getBlockEntity(oldPos)
                instanceof ChainGearBlockEntity movingGear) {
            movingGear.prepareAssemblyMove();
            return;
        }

        if (resultingLevel.getBlockEntity(oldPos)
                instanceof ChainGearBlockEntity movingGear) {
            movingGear.prepareAssemblyMove();
        }
    }

    @Override
    public void afterMove(
            ServerLevel originLevel,
            ServerLevel resultingLevel,
            BlockState newState,
            BlockPos oldPos,
            BlockPos newPos
    ) {
        if (resultingLevel.getBlockEntity(newPos)
                instanceof ChainGearBlockEntity movedGear) {
            movedGear.afterAssemblyMove(
                    originLevel,
                    resultingLevel,
                    oldPos
            );
        }
    }

    @Override
    public List<ItemStack> getDrops(
            BlockState state,
            LootParams.Builder params
    ) {
        List<ItemStack> drops =
                new java.util.ArrayList<>();

        drops.add(
                new ItemStack(
                        CreateMechanicalDrive
                                .CHAIN_GEAR_ITEM
                                .get()
                )
        );

        return drops;
    }

    @Override
    protected RenderShape getRenderShape(
            BlockState state
    ) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public Class<ChainGearBlockEntity> getBlockEntityClass() {
        return ChainGearBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ChainGearBlockEntity> getBlockEntityType() {
        return CreateMechanicalDrive
                .CHAIN_GEAR_BLOCK_ENTITY
                .get();
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        super.createBlockStateDefinition(
                builder
        );

        builder.add(
                CONNECTED_NEGATIVE,
                CONNECTED_POSITIVE
        );
    }

    private Direction.Axis findConnectableShaftAxis(
            LevelReader level,
            BlockPos pos
    ) {
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos =
                    pos.relative(direction);

            BlockState neighborState =
                    level.getBlockState(neighborPos);

            if (hasConnectableShaft(
                    level,
                    neighborPos,
                    neighborState,
                    direction.getOpposite()
            )) {
                return direction.getAxis();
            }
        }

        return null;
    }

    private BlockState withShaftConnections(
            LevelReader level,
            BlockPos pos,
            BlockState state
    ) {
        Direction.Axis axis =
                state.getValue(AXIS);

        Direction negative =
                Direction.fromAxisAndDirection(
                        axis,
                        Direction.AxisDirection.NEGATIVE
                );

        Direction positive =
                Direction.fromAxisAndDirection(
                        axis,
                        Direction.AxisDirection.POSITIVE
                );

        BlockPos negativePos =
                pos.relative(negative);

        BlockPos positivePos =
                pos.relative(positive);

        return state
                .setValue(
                        CONNECTED_NEGATIVE,
                        hasVisualConnection(
                                level,
                                negativePos,
                                level.getBlockState(negativePos),
                                negative.getOpposite()
                        )
                )
                .setValue(
                        CONNECTED_POSITIVE,
                        hasVisualConnection(
                                level,
                                positivePos,
                                level.getBlockState(positivePos),
                                positive.getOpposite()
                        )
                );
    }

    private static boolean hasConnectableShaft(
            LevelReader level,
            BlockPos pos,
            BlockState state,
            Direction direction
    ) {
        if (!(state.getBlock() instanceof IRotate rotate)) {
            return false;
        }

        return rotate.hasShaftTowards(
                level,
                pos,
                state,
                direction
        );
    }

    private static boolean hasVisualConnection(
            LevelReader level,
            BlockPos pos,
            BlockState state,
            Direction direction
    ) {
        return hasConnectableShaft(
                level,
                pos,
                state,
                direction
        ) || state.isFaceSturdy(
                level,
                pos,
                direction
        );
    }
}
