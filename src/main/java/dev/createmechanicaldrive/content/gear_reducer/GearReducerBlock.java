package dev.createmechanicaldrive.content.gear_reducer;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.AbstractEncasedShaftBlock;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.content.kinetics.base.IRotate;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.HitResult;

import javax.annotation.Nullable;
import java.util.List;

public class GearReducerBlock
        extends AbstractEncasedShaftBlock
        implements IBE<GearReducerBlockEntity>, IWrenchable {

    public static final DirectionProperty FACING =
            BlockStateProperties.FACING;

    public static final BooleanProperty POWERED =
            BlockStateProperties.POWERED;

    public GearReducerBlock(
            Properties properties
    ) {
        super(properties);

        registerDefaultState(
                defaultBlockState()
                        .setValue(
                                AXIS,
                                Direction.Axis.Y
                        )
                        .setValue(
                                FACING,
                                Direction.UP
                        )
                        .setValue(
                                POWERED,
                                false
                        )
        );
    }

    @Nullable
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
                crouching
                        ? context.getNearestLookingDirection()
                        : context.getNearestLookingDirection()
                        .getOpposite();

        BlockPos pos =
                context.getClickedPos();

        LevelReader level =
                context.getLevel();

        Direction connectedDirection =
                findConnectableShaftDirection(
                        level,
                        pos
                );

        if (connectedDirection != null) {
            facing =
                    crouching
                            ? connectedDirection.getOpposite()
                            : connectedDirection;
        }

        return defaultBlockState()
                .setValue(
                        FACING,
                        facing
                )
                .setValue(
                        AXIS,
                        facing.getAxis()
                )
                .setValue(
                        POWERED,
                        context.getLevel()
                                .hasNeighborSignal(
                                        pos
                                )
                );
    }

    @Nullable
    private Direction findConnectableShaftDirection(
            LevelReader level,
            BlockPos pos
    ) {
        for (
                Direction direction
                : Direction.values()
        ) {
            BlockPos neighbourPos =
                    pos.relative(
                            direction
                    );

            BlockState neighbourState =
                    level.getBlockState(
                            neighbourPos
                    );

            if (!(
                    neighbourState.getBlock()
                            instanceof IRotate rotate
            )) {
                continue;
            }

            if (!rotate.hasShaftTowards(
                    level,
                    neighbourPos,
                    neighbourState,
                    direction.getOpposite()
            )) {
                continue;
            }

            return direction;
        }

        return null;
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            net.minecraft.world.level.Level level,
            BlockPos pos,
            Block block,
            BlockPos fromPos,
            boolean isMoving
    ) {
        if (level.isClientSide) {
            return;
        }

        boolean powered =
                level.hasNeighborSignal(
                        pos
                );

        if (state.getValue(POWERED) == powered) {
            return;
        }

        level.setBlock(
                pos,
                state.setValue(
                        POWERED,
                        powered
                ),
                3
        );

        if (
                level.getBlockEntity(pos)
                        instanceof GearReducerBlockEntity reducer
        ) {
            RotationPropagator.handleRemoved(
                    level,
                    pos,
                    reducer
            );

            RotationPropagator.handleAdded(
                    level,
                    pos,
                    reducer
            );

            reducer.onRedstoneChanged();
        }
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
    public Direction.Axis getRotationAxis(
            BlockState state
    ) {
        return state.getValue(AXIS);
    }

    @Override
    protected void tick(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            RandomSource random
    ) {
        if (
                level.getBlockEntity(pos)
                        instanceof KineticBlockEntity kinetic
        ) {
            RotationPropagator.handleAdded(
                    level,
                    pos,
                    kinetic
            );
        }
    }

    @Override
    public BlockState getRotatedBlockState(
            BlockState state,
            Direction targetedFace
    ) {
        Direction facing =
                state.getValue(FACING);

        Direction rotated =
                rotateAroundAxis(
                        facing,
                        targetedFace.getAxis()
                );

        return state
                .setValue(
                        FACING,
                        rotated
                )
                .setValue(
                        AXIS,
                        rotated.getAxis()
                );
    }

    private static Direction rotateAroundAxis(
            Direction direction,
            Direction.Axis axis
    ) {
        if (direction.getAxis() == axis) {
            return direction;
        }

        return switch (axis) {
            case X -> switch (direction) {
                case UP -> Direction.SOUTH;
                case SOUTH -> Direction.DOWN;
                case DOWN -> Direction.NORTH;
                case NORTH -> Direction.UP;
                default -> direction;
            };

            case Y -> switch (direction) {
                case NORTH -> Direction.EAST;
                case EAST -> Direction.SOUTH;
                case SOUTH -> Direction.WEST;
                case WEST -> Direction.NORTH;
                default -> direction;
            };

            case Z -> switch (direction) {
                case UP -> Direction.WEST;
                case WEST -> Direction.DOWN;
                case DOWN -> Direction.EAST;
                case EAST -> Direction.UP;
                default -> direction;
            };
        };
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
                                .GEAR_REDUCER_ITEM
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
                        .GEAR_REDUCER_ITEM
                        .get()
        );
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<
                    Block,
                    BlockState
                    > builder
    ) {
        super.createBlockStateDefinition(
                builder
        );

        builder.add(
                FACING,
                POWERED
        );
    }

    @Override
    public Class<GearReducerBlockEntity>
    getBlockEntityClass() {
        return GearReducerBlockEntity.class;
    }

    @Override
    public BlockEntityType<
            ? extends GearReducerBlockEntity
            > getBlockEntityType() {
        return CreateMechanicalDrive
                .GEAR_REDUCER_BLOCK_ENTITY
                .get();
    }
}
