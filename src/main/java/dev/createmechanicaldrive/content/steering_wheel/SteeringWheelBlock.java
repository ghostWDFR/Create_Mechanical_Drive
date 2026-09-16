package dev.createmechanicaldrive.content.steering_wheel;

import dev.createmechanicaldrive.CreateMechanicalDrive;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;

public class SteeringWheelBlock
        extends DirectionalKineticBlock
        implements IBE<SteeringWheelBlockEntity>, IWrenchable {

    public static final EnumProperty<SteeringWheelColor> COLOR =
            EnumProperty.create(
                    "color",
                    SteeringWheelColor.class
            );

    private static final VoxelShape NORTH_BASE =
            Block.box(
                    5.0D,
                    5.0D,
                    10.0D,
                    11.0D,
                    11.0D,
                    16.0D
            );

    private static final VoxelShape NORTH_WHEEL =
            Block.box(
                    1.0D,
                    1.0D,
                    8.0D,
                    15.0D,
                    15.0D,
                    13.0D
            );

    private static final VoxelShape SOUTH_BASE =
            Block.box(
                    5.0D,
                    5.0D,
                    0.0D,
                    11.0D,
                    11.0D,
                    6.0D
            );

    private static final VoxelShape SOUTH_WHEEL =
            Block.box(
                    1.0D,
                    1.0D,
                    3.0D,
                    15.0D,
                    15.0D,
                    8.0D
            );

    private static final VoxelShape EAST_BASE =
            Block.box(
                    0.0D,
                    5.0D,
                    5.0D,
                    6.0D,
                    11.0D,
                    11.0D
            );

    private static final VoxelShape EAST_WHEEL =
            Block.box(
                    3.0D,
                    1.0D,
                    1.0D,
                    8.0D,
                    15.0D,
                    15.0D
            );

    private static final VoxelShape WEST_BASE =
            Block.box(
                    10.0D,
                    5.0D,
                    5.0D,
                    16.0D,
                    11.0D,
                    11.0D
            );

    private static final VoxelShape WEST_WHEEL =
            Block.box(
                    8.0D,
                    1.0D,
                    1.0D,
                    13.0D,
                    15.0D,
                    15.0D
            );

    private static final VoxelShape UP_BASE =
            Block.box(
                    5.0D,
                    0.0D,
                    5.0D,
                    11.0D,
                    6.0D,
                    11.0D
            );

    private static final VoxelShape UP_WHEEL =
            Block.box(
                    1.0D,
                    3.0D,
                    1.0D,
                    15.0D,
                    8.0D,
                    15.0D
            );

    private static final VoxelShape DOWN_BASE =
            Block.box(
                    5.0D,
                    10.0D,
                    5.0D,
                    11.0D,
                    16.0D,
                    11.0D
            );

    private static final VoxelShape DOWN_WHEEL =
            Block.box(
                    1.0D,
                    8.0D,
                    1.0D,
                    15.0D,
                    13.0D,
                    15.0D
            );

    public SteeringWheelBlock(Properties properties) {
        super(properties);

        registerDefaultState(
                defaultBlockState()
                        .setValue(
                                FACING,
                                Direction.NORTH
                        )
                        .setValue(
                                COLOR,
                                SteeringWheelColor.BLACK
                        )
        );
    }

    @Override
    public BlockState getStateForPlacement(
            BlockPlaceContext context
    ) {
        return defaultBlockState()
                .setValue(
                        FACING,
                        context.getClickedFace()
                );
    }

    @Override
    public Direction.Axis getRotationAxis(
            BlockState state
    ) {
        return state.getValue(FACING)
                .getAxis();
    }

    @Override
    public boolean hasShaftTowards(
            LevelReader level,
            BlockPos pos,
            BlockState state,
            Direction direction
    ) {
        return direction
                == state.getValue(FACING)
                .getOpposite();
    }

    @Override
    public boolean canSurvive(
            BlockState state,
            LevelReader level,
            BlockPos pos
    ) {
        Direction supportDirection =
                state.getValue(FACING)
                        .getOpposite();

        BlockPos supportPos =
                pos.relative(
                        supportDirection
                );

        BlockState supportState =
                level.getBlockState(
                        supportPos
                );

        return !supportState.isAir();
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

        if (level.isClientSide) {
            return;
        }

        Direction supportDirection =
                state.getValue(FACING)
                        .getOpposite();

        BlockPos supportPos =
                pos.relative(
                        supportDirection
                );

        if (!neighborPos.equals(
                supportPos
        )) {
            return;
        }

        if (state.canSurvive(
                level,
                pos
        )) {
            return;
        }

        Block.dropResources(
                state,
                level,
                pos
        );

        level.destroyBlock(
                pos,
                false
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
        if (stack.getItem()
                instanceof DyeItem dyeItem) {
            SteeringWheelColor color =
                    SteeringWheelColor.fromDyeColor(
                            dyeItem.getDyeColor()
                    );

            if (color == null) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }

            if (state.getValue(COLOR)
                    == color) {
                return ItemInteractionResult.SUCCESS;
            }

            if (!level.isClientSide) {
                level.setBlock(
                        pos,
                        state.setValue(
                                COLOR,
                                color
                        ),
                        3
                );

                if (!player.isCreative()) {
                    stack.shrink(1);
                }
            }

            return ItemInteractionResult.SUCCESS;
        }

        if (!AllBlocks.SHAFT.isIn(stack)) {
            return player.isShiftKeyDown()
                    ? ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION
                    : ItemInteractionResult.SUCCESS;
        }

        Direction shaftDirection =
                state.getValue(FACING)
                        .getOpposite();

        BlockPos shaftPos =
                pos.relative(
                        shaftDirection
                );

        if (!level.getBlockState(shaftPos)
                .canBeReplaced()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        BlockState shaftState =
                AllBlocks.SHAFT
                        .getDefaultState()
                        .setValue(
                                RotatedPillarKineticBlock.AXIS,
                                shaftDirection.getAxis()
                        );

        if (!level.isClientSide) {
            level.setBlock(
                    shaftPos,
                    shaftState,
                    3
            );

            if (!player.isCreative()) {
                stack.shrink(1);
            }
        }

        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return Shapes.or(
                getBaseShape(state),
                getWheelShape(state)
        );
    }

    public static VoxelShape getBaseShape(
            BlockState state
    ) {
        return switch (state.getValue(FACING)) {
            case NORTH -> NORTH_BASE;
            case SOUTH -> SOUTH_BASE;
            case EAST -> EAST_BASE;
            case WEST -> WEST_BASE;
            case UP -> UP_BASE;
            case DOWN -> DOWN_BASE;
        };
    }

    public static VoxelShape getWheelShape(
            BlockState state
    ) {
        return switch (state.getValue(FACING)) {
            case NORTH -> NORTH_WHEEL;
            case SOUTH -> SOUTH_WHEEL;
            case EAST -> EAST_WHEEL;
            case WEST -> WEST_WHEEL;
            case UP -> UP_WHEEL;
            case DOWN -> DOWN_WHEEL;
        };
    }

    public static boolean isWheelHit(
            BlockState state,
            BlockPos pos,
            Vec3 hitLocation
    ) {
        return getWheelShape(state)
                .bounds()
                .move(
                        pos.getX(),
                        pos.getY(),
                        pos.getZ()
                )
                .contains(
                        hitLocation
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
                COLOR
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

        if (player != null
                && !player.isCreative()) {
            ItemStack stack =
                    new ItemStack(
                            CreateMechanicalDrive
                                    .STEERING_WHEEL_ITEM
                                    .get()
                    );

            SteeringWheelColor color =
                    state.getValue(
                            COLOR
                    );

            if (color
                    != SteeringWheelColor.BLACK) {
                stack.set(
                        DataComponents.BLOCK_STATE,
                        BlockItemStateProperties.EMPTY.with(
                                COLOR,
                                color
                        )
                );
            }

            player.getInventory()
                    .placeItemBackInInventory(
                            stack
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
    public List<ItemStack> getDrops(
            BlockState state,
            LootParams.Builder params
    ) {
        ItemStack stack =
                new ItemStack(
                        CreateMechanicalDrive
                                .STEERING_WHEEL_ITEM
                                .get()
                );

        SteeringWheelColor color =
                state.getValue(COLOR);

        if (color != SteeringWheelColor.BLACK) {
            stack.set(
                    DataComponents.BLOCK_STATE,
                    BlockItemStateProperties.EMPTY.with(
                            COLOR,
                            color
                    )
            );
        }

        return List.of(
                stack
        );
    }

    @Override
    public Class<SteeringWheelBlockEntity>
    getBlockEntityClass() {
        return SteeringWheelBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SteeringWheelBlockEntity>
    getBlockEntityType() {
        return CreateMechanicalDrive
                .STEERING_WHEEL_BLOCK_ENTITY
                .get();
    }
}
