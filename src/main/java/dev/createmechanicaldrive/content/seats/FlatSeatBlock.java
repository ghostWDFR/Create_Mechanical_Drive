package dev.createmechanicaldrive.content.seats;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

import javax.annotation.Nullable;
import java.util.List;

public class FlatSeatBlock
        extends Block
        implements IWrenchable {

    public static final DirectionProperty FACING =
            BlockStateProperties.HORIZONTAL_FACING;

    public static final EnumProperty<SeatColor> COLOR =
            SeatBlock.COLOR;

    private static final Box BASE_BOX =
            new Box(
                    0.0D,
                    0.0D,
                    0.0D,
                    16.0D,
                    4.0D,
                    16.0D
            );


    private static final VoxelShape[] SHAPES =
            createShapes();

    public FlatSeatBlock(
            Properties properties
    ) {
        super(properties);

        registerDefaultState(
                stateDefinition
                        .any()
                        .setValue(
                                FACING,
                                Direction.SOUTH
                        )
                        .setValue(
                                COLOR,
                                SeatColor.BLACK
                        )
        );
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(
            BlockPlaceContext context
    ) {
        Direction facing =
                context.getHorizontalDirection()
                        .getOpposite();

        if (facing.getAxis() == Direction.Axis.Z) {
            facing =
                    facing.getOpposite();
        }

        return defaultBlockState()
                .setValue(
                        FACING,
                        facing
                );
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return getSeatShape(
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
        return getSeatShape(
                state
        );
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        return sitPlayer(
                level,
                pos,
                player
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
            SeatColor color =
                    SeatColor.fromDyeColor(
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

        if (
                player.isShiftKeyDown()
                        || player.isCrouching()
        ) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        InteractionResult result =
                sitPlayer(
                        level,
                        pos,
                        player
                );

        return result == InteractionResult.SUCCESS
                ? ItemInteractionResult.SUCCESS
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private static InteractionResult sitPlayer(
            Level level,
            BlockPos pos,
            Player player
    ) {
        List<SeatEntity> seats =
                level.getEntitiesOfClass(
                        SeatEntity.class,
                        new AABB(
                                pos
                        )
                );

        if (!seats.isEmpty()) {
            SeatEntity seat =
                    seats.getFirst();

            List<Entity> passengers =
                    seat.getPassengers();

            if (
                    !passengers.isEmpty()
                            && passengers.getFirst()
                            instanceof Player
            ) {
                return InteractionResult.PASS;
            }

            if (!level.isClientSide) {
                seat.ejectPassengers();

                player.startRiding(
                        seat
                );
            }

            return InteractionResult.SUCCESS;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        SeatEntity.sitDown(
                level,
                pos,
                player
        );

        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult onSneakWrenched(
            BlockState state,
            UseOnContext context
    ) {
        if (!(
                context.getLevel()
                        instanceof ServerLevel level
        )) {
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

        if (
                player != null
                        && !player.isCreative()
        ) {
            player.getInventory()
                    .placeItemBackInInventory(
                            seatStack(
                                    state
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
    public List<ItemStack> getDrops(
            BlockState state,
            LootParams.Builder params
    ) {
        return List.of(
                seatStack(
                        state
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
        return seatStack(
                state
        );
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(
                FACING,
                COLOR
        );
    }

    private static ItemStack seatStack(
            BlockState state
    ) {
        ItemStack stack =
                new ItemStack(
                        CreateMechanicalDrive
                                .FLAT_SEAT_ITEM
                                .get()
                );

        SeatColor color =
                state.getValue(
                        COLOR
                );

        if (color != SeatColor.BLACK) {
            stack.set(
                    DataComponents.BLOCK_STATE,
                    BlockItemStateProperties.EMPTY.with(
                            COLOR,
                            color
                    )
            );
        }

        return stack;
    }

    private static VoxelShape getSeatShape(
            BlockState state
    ) {
        Direction facing =
                state.getValue(
                        FACING
                );

        return SHAPES[facing.ordinal()];
    }

    private static VoxelShape[] createShapes() {
        VoxelShape[] shapes =
                new VoxelShape[
                        Direction.values().length
                        ];

        for (Direction facing
                : Direction.Plane.HORIZONTAL) {
            Box base =
                    BASE_BOX.rotateY(
                            baseYawDegrees(
                                    facing
                            )
                    );

            shapes[facing.ordinal()] =
                    base.toShape();
        }

        return shapes;
    }

    private static double baseYawDegrees(
            Direction facing
    ) {
        return switch (facing) {
            case EAST -> 90.0D;
            case NORTH -> 180.0D;
            case WEST -> 270.0D;
            case UP, DOWN, SOUTH -> 0.0D;
        };
    }


    private record Box(
            double minX,
            double minY,
            double minZ,
            double maxX,
            double maxY,
            double maxZ
    ) {
        private Box rotateY(
                double degrees
        ) {
            double radians =
                    Math.toRadians(
                            degrees
                    );

            double sin =
                    Math.sin(
                            radians
                    );

            double cos =
                    Math.cos(
                            radians
                    );

            MutableBox result =
                    new MutableBox();

            forEachCorner(
                    (
                            x,
                            y,
                            z
                    ) -> {
                        double localX =
                                x - 8.0D;

                        double localZ =
                                z - 8.0D;

                        result.include(
                                8.0D
                                        + localX
                                        * cos
                                        + localZ
                                        * sin,
                                y,
                                8.0D
                                        - localX
                                        * sin
                                        + localZ
                                        * cos
                        );
                    }
            );

            return result.toBox();
        }

        private VoxelShape toShape() {
            return Block.box(
                    minX,
                    minY,
                    minZ,
                    maxX,
                    maxY,
                    maxZ
            );
        }

        private void forEachCorner(
                CornerConsumer consumer
        ) {
            consumer.accept(
                    minX,
                    minY,
                    minZ
            );

            consumer.accept(
                    minX,
                    minY,
                    maxZ
            );

            consumer.accept(
                    minX,
                    maxY,
                    minZ
            );

            consumer.accept(
                    minX,
                    maxY,
                    maxZ
            );

            consumer.accept(
                    maxX,
                    minY,
                    minZ
            );

            consumer.accept(
                    maxX,
                    minY,
                    maxZ
            );

            consumer.accept(
                    maxX,
                    maxY,
                    minZ
            );

            consumer.accept(
                    maxX,
                    maxY,
                    maxZ
            );
        }
    }

    private static class MutableBox {
        private double minX =
                Double.POSITIVE_INFINITY;

        private double minY =
                Double.POSITIVE_INFINITY;

        private double minZ =
                Double.POSITIVE_INFINITY;

        private double maxX =
                Double.NEGATIVE_INFINITY;

        private double maxY =
                Double.NEGATIVE_INFINITY;

        private double maxZ =
                Double.NEGATIVE_INFINITY;

        private void include(
                double x,
                double y,
                double z
        ) {
            minX =
                    Math.min(
                            minX,
                            x
                    );

            minY =
                    Math.min(
                            minY,
                            y
                    );

            minZ =
                    Math.min(
                            minZ,
                            z
                    );

            maxX =
                    Math.max(
                            maxX,
                            x
                    );

            maxY =
                    Math.max(
                            maxY,
                            y
                    );

            maxZ =
                    Math.max(
                            maxZ,
                            z
                    );
        }

        private Box toBox() {
            return new Box(
                    minX,
                    minY,
                    minZ,
                    maxX,
                    maxY,
                    maxZ
            );
        }
    }

    @FunctionalInterface
    private interface CornerConsumer {
        void accept(
                double x,
                double y,
                double z
        );
    }
}
