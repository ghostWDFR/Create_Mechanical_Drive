package dev.createmechanicaldrive.content.worm_gears;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.foundation.block.IBE;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.List;

public class WormGearSmallBlock
        extends RotatedPillarKineticBlock
        implements IBE<WormGearSmallBlockEntity>, IWrenchable {

    public static final BooleanProperty CONNECTED_NEGATIVE =
            BooleanProperty.create("connected_negative");

    public static final BooleanProperty CONNECTED_POSITIVE =
            BooleanProperty.create("connected_positive");

    public static final BooleanProperty ENCASED =
            BooleanProperty.create("encased");

    private static final VoxelShape FULL_BLOCK =
            Block.box(
                    0.0D,
                    0.0D,
                    0.0D,
                    16.0D,
                    16.0D,
                    16.0D
            );

    private static final VoxelShape SHAPE_X =
            Block.box(
                    0.0D,
                    5.0D,
                    5.0D,
                    16.0D,
                    11.0D,
                    11.0D
            );

    private static final VoxelShape SHAPE_Y =
            Block.box(
                    5.0D,
                    0.0D,
                    5.0D,
                    11.0D,
                    16.0D,
                    11.0D
            );

    private static final VoxelShape SHAPE_Z =
            Block.box(
                    5.0D,
                    5.0D,
                    0.0D,
                    11.0D,
                    11.0D,
                    16.0D
            );

    public WormGearSmallBlock(
            Properties properties
    ) {
        super(properties);

        registerDefaultState(
                defaultBlockState()
                        .setValue(CONNECTED_NEGATIVE, false)
                        .setValue(CONNECTED_POSITIVE, false)
                        .setValue(ENCASED, false)
        );
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        if (state.getValue(ENCASED)) {
            return FULL_BLOCK;
        }

        return switch (
                state.getValue(
                        AXIS
                )
                ) {
            case X -> SHAPE_X;
            case Y -> SHAPE_Y;
            case Z -> SHAPE_Z;
        };
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return getShape(
                state,
                level,
                pos,
                context
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
                findSupportedCogAxis(
                        level,
                        pos
                );

        if (axis == null) {
            axis =
                    findConnectableShaftAxis(
                            level,
                            pos
                    );
        }

        if (axis == null) {
            axis =
                    context.getNearestLookingDirection()
                            .getAxis();
        }

        return withConnections(
                level,
                pos,
                defaultBlockState()
                        .setValue(AXIS, axis)
                        .setValue(
                                ENCASED,
                                false
                        )
        );
    }

    private Direction.Axis findConnectableShaftAxis(
            LevelReader level,
            BlockPos pos
    ) {
        for (
                Direction direction
                : Direction.values()
        ) {
            BlockPos neighborPos =
                    pos.relative(direction);

            BlockState neighborState =
                    level.getBlockState(
                            neighborPos
                    );

            if (!(
                    neighborState.getBlock()
                            instanceof IRotate rotate
            )) {
                continue;
            }

            if (!rotate.hasShaftTowards(
                    level,
                    neighborPos,
                    neighborState,
                    direction.getOpposite()
            )) {
                continue;
            }

            return direction.getAxis();
        }

        return null;
    }

    private Direction.Axis findSupportedCogAxis(
            LevelReader level,
            BlockPos pos
    ) {
        for (
                Direction direction
                : Direction.values()
        ) {
            BlockPos neighborPos =
                    pos.relative(direction);

            BlockState neighborState =
                    level.getBlockState(
                            neighborPos
                    );

            if (!ICogWheel.isLargeCog(
                    neighborState
            )) {
                continue;
            }

            if (!(
                    neighborState.getBlock()
                            instanceof IRotate cog
            )) {
                continue;
            }

            Direction.Axis connectionAxis =
                    direction.getAxis();

            Direction.Axis cogAxis =
                    cog.getRotationAxis(
                            neighborState
                    );

            if (connectionAxis == cogAxis) {
                continue;
            }

            return getRemainingAxis(
                    connectionAxis,
                    cogAxis
            );
        }

        return null;
    }

    private Direction.Axis getRemainingAxis(
            Direction.Axis first,
            Direction.Axis second
    ) {
        for (
                Direction.Axis axis
                : Direction.Axis.values()
        ) {
            if (
                    axis != first
                            && axis != second
            ) {
                return axis;
            }
        }

        return null;
    }

    private boolean isSmallWormAlongAxis(
            LevelReader level,
            BlockPos pos,
            Direction.Axis axis
    ) {
        BlockState neighborState =
                level.getBlockState(pos);

        return isConnectableSmallWorm(
                neighborState,
                axis
        );
    }

    private boolean isConnectableSmallWorm(
            BlockState state,
            Direction.Axis axis
    ) {
        if (!state.is(this)) {
            return false;
        }

        if (state.getValue(ENCASED)) {
            return false;
        }

        return state.getValue(AXIS) == axis;
    }

    private BlockState withConnections(
            LevelReader level,
            BlockPos pos,
            BlockState state
    ) {
        if (state.getValue(ENCASED)) {
            return withoutConnections(
                    state
            );
        }

        Direction.Axis axis =
                state.getValue(
                        AXIS
                );

        boolean connectedNegative =
                isSmallWormAlongAxis(
                        level,
                        pos.relative(
                                Direction.fromAxisAndDirection(
                                        axis,
                                        Direction.AxisDirection.NEGATIVE
                                )
                        ),
                        axis
                );

        boolean connectedPositive =
                isSmallWormAlongAxis(
                        level,
                        pos.relative(
                                Direction.fromAxisAndDirection(
                                        axis,
                                        Direction.AxisDirection.POSITIVE
                                )
                        ),
                        axis
                );

        return state
                .setValue(
                        CONNECTED_NEGATIVE,
                        connectedNegative
                )
                .setValue(
                        CONNECTED_POSITIVE,
                        connectedPositive
                );
    }

    private BlockState withoutConnections(
            BlockState state
    ) {
        return state
                .setValue(
                        CONNECTED_NEGATIVE,
                        false
                )
                .setValue(
                        CONNECTED_POSITIVE,
                        false
                );
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
        if (state.getValue(ENCASED)) {
            return withoutConnections(
                    state
            );
        }

        Direction.Axis axis =
                state.getValue(AXIS);

        if (direction.getAxis() != axis) {
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
                isConnectableSmallWorm(
                        neighborState,
                        axis
                );

        if (
                direction.getAxisDirection()
                        == Direction.AxisDirection.NEGATIVE
        ) {
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
                == state.getValue(
                AXIS
        );
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        if (!AllBlocks.ANDESITE_CASING.isIn(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (state.getValue(ENCASED)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!level.isClientSide) {
            BlockState encasedState =
                    withoutConnections(
                            state.setValue(
                                    ENCASED,
                                    true
                            )
                    );

            level.setBlock(
                    pos,
                    encasedState,
                    3
            );

            playCasingPlaceSound(
                    level,
                    pos,
                    player
            );

            if (!player.isCreative()) {
                stack.shrink(
                        1
                );
            }
        }

        return ItemInteractionResult.SUCCESS;
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

        BlockPos pos =
                context.getClickedPos();

        Player player =
                context.getPlayer();

        BlockEvent.BreakEvent event =
                new BlockEvent.BreakEvent(
                        level,
                        pos,
                        state,
                        player
                );

        NeoForge.EVENT_BUS.post(
                event
        );

        if (event.isCanceled()) {
            return InteractionResult.SUCCESS;
        }

        if (state.getValue(ENCASED)) {
            level.setBlock(
                    pos,
                    withConnections(
                            level,
                            pos,
                            state.setValue(
                                    ENCASED,
                                    false
                            )
                    ),
                    3
            );

            if (player != null
                    && !player.isCreative()) {
                player.getInventory()
                        .placeItemBackInInventory(
                                new ItemStack(
                                        AllBlocks
                                                .ANDESITE_CASING
                                                .get()
                                )
                        );
            }

            IWrenchable.playRemoveSound(
                    level,
                    pos
            );

            return InteractionResult.SUCCESS;
        }

        if (player != null
                && !player.isCreative()) {
            player.getInventory()
                    .placeItemBackInInventory(
                            new ItemStack(
                                    CreateMechanicalDrive
                                            .WORM_GEAR_SMALL_ITEM
                                            .get()
                            )
                    );
        }

        level.destroyBlock(
                pos,
                false
        );

        IWrenchable.playRemoveSound(
                level,
                pos
        );

        return InteractionResult.SUCCESS;
    }

    private static void playCasingPlaceSound(
            Level level,
            BlockPos pos,
            Player player
    ) {
        SoundType soundType =
                AllBlocks
                        .ANDESITE_CASING
                        .get()
                        .defaultBlockState()
                        .getSoundType(
                                level,
                                pos,
                                player
                        );

        level.playSound(
                null,
                pos,
                soundType.getPlaceSound(),
                SoundSource.BLOCKS,
                (soundType.getVolume()
                        + 1.0F)
                        / 2.0F,
                soundType.getPitch()
                        * 0.8F
        );
    }

    @Override
    public List<ItemStack> getDrops(
            BlockState state,
            LootParams.Builder params
    ) {
        if (state.getValue(ENCASED)) {
            return List.of(
                    new ItemStack(
                            CreateMechanicalDrive
                                    .WORM_GEAR_SMALL_ITEM
                                    .get()
                    ),
                    new ItemStack(
                            AllBlocks
                                    .ANDESITE_CASING
                                    .get()
                    )
            );
        }

        return List.of(
                new ItemStack(
                        CreateMechanicalDrive
                                .WORM_GEAR_SMALL_ITEM
                                .get()
                )
        );
    }

    @Override
    protected RenderShape getRenderShape(
            BlockState state
    ) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public Class<WormGearSmallBlockEntity>
    getBlockEntityClass() {
        return WormGearSmallBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends WormGearSmallBlockEntity>
    getBlockEntityType() {
        return CreateMechanicalDrive
                .WORM_GEAR_SMALL_BLOCK_ENTITY
                .get();
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
                CONNECTED_NEGATIVE,
                CONNECTED_POSITIVE,
                ENCASED
        );
    }
}
