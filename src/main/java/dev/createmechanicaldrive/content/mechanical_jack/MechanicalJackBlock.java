package dev.createmechanicaldrive.content.mechanical_jack;

import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.createmechanicaldrive.compat.SableSubLevelHelper;
import dev.ryanhcode.sable.api.block.BlockSubLevelAssemblyListener;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import com.simibubi.create.content.equipment.wrench.IWrenchable;

import java.util.List;
import net.minecraft.world.level.storage.loot.LootParams;

public class MechanicalJackBlock
        extends HorizontalKineticBlock
        implements IBE<MechanicalJackBlockEntity>,
        BlockSubLevelAssemblyListener {

    public static final DirectionProperty FACING =
            BlockStateProperties.HORIZONTAL_FACING;

    public static final DirectionProperty MOUNT =
            DirectionProperty.create(
                    "mount"
            );

    private static final VoxelShape FULL_BLOCK =
            box(
                    0.0D, 0.0D, 0.0D,
                    16.0D, 16.0D, 16.0D
            );

    private static final VoxelShape SHAPE_UP =
            box(
                    0.0D, 0.0D, 0.0D,
                    16.0D, 14.0D, 16.0D
            );

    private static final VoxelShape SHAPE_DOWN =
            box(
                    0.0D, 2.0D, 0.0D,
                    16.0D, 16.0D, 16.0D
            );

    private static final VoxelShape SHAPE_NORTH =
            box(
                    0.0D, 0.0D, 2.0D,
                    16.0D, 16.0D, 16.0D
            );

    private static final VoxelShape SHAPE_SOUTH =
            box(
                    0.0D, 0.0D, 0.0D,
                    16.0D, 16.0D, 14.0D
            );

    private static final VoxelShape SHAPE_EAST =
            box(
                    0.0D, 0.0D, 0.0D,
                    14.0D, 16.0D, 16.0D
            );

    private static final VoxelShape SHAPE_WEST =
            box(
                    2.0D, 0.0D, 0.0D,
                    16.0D, 16.0D, 16.0D
            );

    public MechanicalJackBlock(
            Properties properties
    ) {
        super(properties);

        registerDefaultState(
                defaultBlockState()
                        .setValue(
                                FACING,
                                Direction.NORTH
                        )
                        .setValue(
                                MOUNT,
                                Direction.UP
                        )
        );
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        super.createBlockStateDefinition(
                builder
        );

        builder.add(
                MOUNT
        );
    }

    @Override
    public BlockState getStateForPlacement(
            BlockPlaceContext context
    ) {
        Direction mount =
                context.getClickedFace();

        boolean inverted =
                isInvertedPlacement(
                        context
                );

        Vec3 lookDirection =
                getLookDirectionForPlacement(
                        context
                );

        Direction horizontalFacing =
                adjustHorizontalFacingForSubLevelModel(
                        context,
                        getNearestHorizontalDirection(
                                context,
                                lookDirection,
                                context.getHorizontalDirection()
                        )
                );

        Direction facing;

        if (mount == Direction.UP) {
            facing =
                    horizontalFacing
                            .getOpposite();
        } else if (mount == Direction.DOWN) {
            facing =
                    horizontalFacing;
        } else {
            boolean lookingDown =
                    isLookingDown(
                            context,
                            lookDirection
                    );

            facing =
                    lookingDown
                            ? mount.getOpposite()
                            : mount;
        }

        if (inverted) {
            facing =
                    facing.getOpposite();
        }

        return defaultBlockState()
                .setValue(
                        FACING,
                        facing
                )
                .setValue(
                        MOUNT,
                        mount
                );
    }

    private static boolean isInvertedPlacement(
            BlockPlaceContext context
    ) {
        if (context.getPlayer() == null) {
            return false;
        }

        Player player =
                context.getPlayer();

        return context.isSecondaryUseActive()
                || player.isShiftKeyDown()
                || player.isCrouching();
    }

    private static Vec3 getLookDirectionForPlacement(
            BlockPlaceContext context
    ) {
        if (context.getPlayer() == null) {
            return Vec3.ZERO;
        }

        return context.getPlayer()
                .getLookAngle();
    }

    private static Direction getNearestHorizontalDirection(
            BlockPlaceContext context,
            Vec3 lookDirection,
            Direction fallback
    ) {
        if (lookDirection.lengthSqr() < 1.0E-8D) {
            return fallback;
        }

        Direction nearest =
                fallback;

        double nearestDot =
                -Double.MAX_VALUE;

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            Vec3 worldDirection =
                    SableSubLevelHelper.getWorldNormal(
                            context.getLevel(),
                            context.getClickedPos(),
                            directionVector(
                                    direction
                            )
                    );

            double dot =
                    dot(
                            lookDirection,
                            worldDirection
                    );

            if (dot > nearestDot) {
                nearestDot =
                        dot;

                nearest =
                        direction;
            }
        }

        return nearest;
    }

    private static Direction adjustHorizontalFacingForSubLevelModel(
            BlockPlaceContext context,
            Direction horizontalFacing
    ) {
        if (SableSubLevelHelper.getSubLevel(
                context.getLevel(),
                context.getClickedPos()
        ) == null) {
            return horizontalFacing;
        }

        Direction adjustedFacing =
                horizontalFacing.getAxis() == Direction.Axis.Z
                        ? horizontalFacing.getOpposite()
                        : horizontalFacing;

        return adjustedFacing.getOpposite();
    }

    private static boolean isLookingDown(
            BlockPlaceContext context,
            Vec3 lookDirection
    ) {
        if (lookDirection.lengthSqr() < 1.0E-8D) {
            return context.getPlayer() != null
                    && context.getPlayer().getXRot() > 0.0F;
        }

        Vec3 down =
                SableSubLevelHelper.getWorldNormal(
                        context.getLevel(),
                        context.getClickedPos(),
                        directionVector(
                                Direction.DOWN
                        )
                );

        Vec3 up =
                SableSubLevelHelper.getWorldNormal(
                        context.getLevel(),
                        context.getClickedPos(),
                        directionVector(
                                Direction.UP
                        )
                );

        double downDot =
                dot(
                        lookDirection,
                        down
                );

        double upDot =
                dot(
                        lookDirection,
                        up
                );

        if (Math.abs(
                downDot - upDot
        ) < 1.0E-8D) {
            return context.getPlayer() != null
                    && context.getPlayer().getXRot() > 0.0F;
        }

        return downDot > upDot;
    }

    private static Vec3 directionVector(
            Direction direction
    ) {
        return new Vec3(
                direction.getStepX(),
                direction.getStepY(),
                direction.getStepZ()
        );
    }

    private static double dot(
            Vec3 first,
            Vec3 second
    ) {
        return first.x * second.x
                + first.y * second.y
                + first.z * second.z;
    }

    public static Direction getInputSide(
            BlockState state
    ) {
        Direction originalInput =
                state
                        .getValue(FACING)
                        .getOpposite();

        Direction mount =
                state.getValue(MOUNT);

        return rotateForMount(
                originalInput,
                mount
        );
    }

    private static Direction rotateForMount(
            Direction direction,
            Direction mount
    ) {
        if (mount == Direction.UP) {
            return direction;
        }

        return switch (mount) {
            case DOWN ->
                    switch (direction) {
                        case NORTH -> Direction.SOUTH;
                        case SOUTH -> Direction.NORTH;
                        case EAST -> Direction.EAST;
                        case WEST -> Direction.WEST;
                        case UP -> Direction.DOWN;
                        case DOWN -> Direction.UP;
                    };

            case NORTH ->
                    switch (direction) {
                        case NORTH -> Direction.DOWN;
                        case SOUTH -> Direction.UP;
                        case EAST -> Direction.EAST;
                        case WEST -> Direction.WEST;
                        case UP -> Direction.NORTH;
                        case DOWN -> Direction.SOUTH;
                    };

            case SOUTH ->
                    switch (direction) {
                        case NORTH -> Direction.UP;
                        case SOUTH -> Direction.DOWN;
                        case EAST -> Direction.EAST;
                        case WEST -> Direction.WEST;
                        case UP -> Direction.SOUTH;
                        case DOWN -> Direction.NORTH;
                    };

            case EAST ->
                    switch (direction) {
                        case NORTH -> Direction.NORTH;
                        case SOUTH -> Direction.SOUTH;
                        case EAST -> Direction.DOWN;
                        case WEST -> Direction.UP;
                        case UP -> Direction.EAST;
                        case DOWN -> Direction.WEST;
                    };

            case WEST ->
                    switch (direction) {
                        case NORTH -> Direction.NORTH;
                        case SOUTH -> Direction.SOUTH;
                        case EAST -> Direction.UP;
                        case WEST -> Direction.DOWN;
                        case UP -> Direction.WEST;
                        case DOWN -> Direction.EAST;
                    };

            default -> direction;
        };
    }

    @Override
    public boolean hasShaftTowards(
            LevelReader level,
            BlockPos pos,
            BlockState state,
            Direction direction
    ) {
        return direction
                == getInputSide(state);
    }

    @Override
    public Direction.Axis getRotationAxis(
            BlockState state
    ) {
        return getInputSide(
                state
        ).getAxis();
    }

    private static VoxelShape getShapeForMount(
            BlockState state
    ) {
        return switch (
                state.getValue(MOUNT)
                ) {
            case DOWN -> SHAPE_DOWN;
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
            default -> SHAPE_UP;
        };
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return hasFullBlockCollision(
                level,
                pos
        )
                ? FULL_BLOCK
                : getShapeForMount(
                state
        );
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return hasFullBlockCollision(
                level,
                pos
        )
                ? FULL_BLOCK
                : getShapeForMount(
                state
        );
    }

    private static boolean hasFullBlockCollision(
            BlockGetter level,
            BlockPos pos
    ) {
        return level.getBlockEntity(
                pos
        ) instanceof MechanicalJackBlockEntity jack
                && jack.hasFullBlockCollision();
    }

    @Override
    protected void tick(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            RandomSource random
    ) {
        if (!(level.getBlockEntity(pos)
                instanceof MechanicalJackBlockEntity blockEntity)) {
            return;
        }

        blockEntity.tryAssembleIntoSable();
    }

    @Override
    public List<ItemStack> getDrops(
            BlockState state,
            LootParams.Builder params
    ) {
        return List.of(
                new ItemStack(
                        CreateMechanicalDrive
                                .MECHANICAL_JACK_ITEM
                                .get()
                )
        );
    }

    @Override
    public Class<MechanicalJackBlockEntity>
    getBlockEntityClass() {
        return MechanicalJackBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends MechanicalJackBlockEntity>
    getBlockEntityType() {
        return CreateMechanicalDrive
                .MECHANICAL_JACK_BLOCK_ENTITY
                .get();
    }

    @Override
    public void beforeMove(
            ServerLevel originLevel,
            ServerLevel resultingLevel,
            BlockState state,
            BlockPos oldPos,
            BlockPos newPos
    ) {
        MechanicalJackPhysics.beforeBaseMoved(
                originLevel,
                state,
                oldPos,
                newPos
        );
    }

    @Override
    public void afterMove(
            ServerLevel originLevel,
            ServerLevel resultingLevel,
            BlockState newState,
            BlockPos oldPos,
            BlockPos newPos
    ) {
        try {
            MechanicalJackPhysics.onBaseAssembled(
                    originLevel,
                    resultingLevel,
                    newPos
            );
        } finally {
            MechanicalJackPhysics.afterBaseMoved(
                    oldPos,
                    newPos
            );
        }
    }

    @Override
    public InteractionResult onWrenched(
            BlockState state,
            UseOnContext context
    ) {
        return InteractionResult.PASS;
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

        if (level.getBlockEntity(pos)
                instanceof MechanicalJackBlockEntity jack) {

            MechanicalJackPhysics.removeFromBase(
                    jack
            );
        }

        if (player != null
                && !player.isCreative()) {

            player.getInventory()
                    .placeItemBackInInventory(
                            new ItemStack(
                                    CreateMechanicalDrive
                                            .MECHANICAL_JACK_ITEM
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

    @Override
    public void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean movedByPiston
    ) {
        if (!state.is(newState.getBlock())
                && !level.isClientSide
                && !MechanicalJackPhysics.isDetachingHeadPayload()
                && !MechanicalJackPhysics.isMovingBase(pos)
                && level.getBlockEntity(pos)
                instanceof MechanicalJackBlockEntity jack) {

            MechanicalJackPhysics.removeFromBase(
                    jack
            );
        }

        super.onRemove(
                state,
                level,
                pos,
                newState,
                movedByPiston
        );
    }

    @Override
    public BlockState playerWillDestroy(
            Level level,
            BlockPos pos,
            BlockState state,
            Player player
    ) {
        if (!level.isClientSide
                && level.getBlockEntity(pos)
                instanceof MechanicalJackBlockEntity jack) {

            MechanicalJackPhysics.removeFromBase(jack);
        }

        return super.playerWillDestroy(
                level,
                pos,
                state,
                player
        );
    }
}