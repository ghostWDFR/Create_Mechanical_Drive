package dev.createmechanicaldrive.content.tracks.mounts.sprocket;

import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.createmechanicaldrive.content.tracks.chain.TrackAssemblyManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import java.util.EnumMap;
import java.util.Map;

public class SprocketMountBlock extends HorizontalKineticBlock
        implements IBE<SprocketMountBlockEntity> {
    private static final VoxelShape SHAPE_UP = Shapes.or(
            Block.box(3.0D, 0.0D, 1.0D, 13.0D, 1.0D, 15.0D),
            Block.box(2.0D, 0.0D, 2.0D, 14.0D, 1.0D, 14.0D),
            Block.box(4.0D, 1.0D, 2.0D, 12.0D, 3.0D, 12.0D)
    );

    private static final Map<Direction, VoxelShape> ROTATED_SHAPES =
            createRotatedShapes();

    public SprocketMountBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction outputDirection = context.getClickedFace();
        if (outputDirection.getAxis().isVertical()) {
            return null;
        }

        BlockState placementState = defaultBlockState().setValue(
                HORIZONTAL_FACING,
                outputDirection
        );
        return placementState.canSurvive(
                context.getLevel(),
                context.getClickedPos()
        ) ? placementState : null;
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(HORIZONTAL_FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(
            LevelReader level,
            BlockPos pos,
            BlockState state,
            Direction direction
    ) {
        return direction == state
                .getValue(HORIZONTAL_FACING)
                .getOpposite();
    }

    @Override
    public boolean canSurvive(
            BlockState state,
            LevelReader level,
            BlockPos pos
    ) {
        Direction outputDirection = state.getValue(HORIZONTAL_FACING);
        BlockPos supportPos = pos.relative(outputDirection.getOpposite());
        return level.getBlockState(supportPos).isFaceSturdy(
                level,
                supportPos,
                outputDirection
        );
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack held,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        if (hit.getDirection() != state.getValue(HORIZONTAL_FACING)) {
            return super.useItemOn(
                    held,
                    state,
                    level,
                    pos,
                    player,
                    hand,
                    hit
            );
        }

        SprocketMountBlockEntity mount = getMount(level, pos);
        if (!held.isEmpty()
                && mount != null
                && !mount.getAttachment().isEmpty()) {
            return level.isClientSide
                    ? ItemInteractionResult.SUCCESS
                    : ItemInteractionResult.CONSUME;
        }

        if (!SprocketMountAttachmentShapes.supports(held)) {
            return super.useItemOn(
                    held,
                    state,
                    level,
                    pos,
                    player,
                    hand,
                    hit
            );
        }

        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }

        return swapAttachment(level, pos, player, held)
                ? ItemInteractionResult.CONSUME
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit
    ) {
        if (hit.getDirection() != state.getValue(HORIZONTAL_FACING)) {
            return super.useWithoutItem(state, level, pos, player, hit);
        }

        SprocketMountBlockEntity mount = getMount(level, pos);
        if (mount == null || mount.getAttachment().isEmpty()) {
            return super.useWithoutItem(state, level, pos, player, hit);
        }

        if (mount.getTrackAssembly() != null) {
            if (!level.isClientSide) {
                TrackAssemblyManager.disassemble(mount, true);
                level.playSound(
                        null,
                        pos,
                        SoundEvents.CHAIN_BREAK,
                        SoundSource.BLOCKS,
                        0.75F,
                        1.0F
                );
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        return swapAttachment(level, pos, player, ItemStack.EMPTY)
                ? InteractionResult.CONSUME
                : InteractionResult.PASS;
    }

    private static boolean swapAttachment(
            Level level,
            BlockPos pos,
            Player player,
            ItemStack held
    ) {
        SprocketMountBlockEntity mount = getMount(level, pos);
        if (mount == null
                || held.isEmpty() && mount.getTrackAssembly() != null
                || (!held.isEmpty()
                && (!SprocketMountAttachmentShapes.supports(held)
                || !mount.getAttachment().isEmpty()))) {
            return false;
        }

        ItemStack previous = mount.getAttachment().copy();
        ItemStack replacement = held.isEmpty()
                ? ItemStack.EMPTY
                : held.copyWithCount(1);
        mount.getInventory().setStackInSlot(0, replacement);

        if (!held.isEmpty() && !player.hasInfiniteMaterials()) {
            held.shrink(1);
        }
        if (!previous.isEmpty()) {
            player.getInventory().placeItemBackInInventory(previous);
        }

        level.playSound(
                null,
                pos,
                replacement.isEmpty()
                        ? SoundEvents.ITEM_PICKUP
                        : SoundEvents.ITEM_FRAME_ADD_ITEM,
                SoundSource.PLAYERS,
                0.75F,
                0.8F + level.random.nextFloat() * 0.4F
        );
        return true;
    }

    @Nullable
    private static SprocketMountBlockEntity getMount(
            Level level,
            BlockPos pos
    ) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof SprocketMountBlockEntity mount
                ? mount
                : null;
    }

    @Override
    public void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean moving
    ) {
        if (!state.is(newState.getBlock())) {
            SprocketMountBlockEntity mount = getMount(level, pos);
            if (mount != null) {
                dev.createmechanicaldrive.content.tracks.chain.TrackAssemblyManager
                        .disassemble(mount, true);
            }
            if (mount != null && !mount.getAttachment().isEmpty()) {
                if (!level.isClientSide) {
                    Direction output =
                            state.getValue(HORIZONTAL_FACING);

                    BlockPos dropPos =
                            pos.relative(output);

                    Containers.dropItemStack(
                            level,
                            dropPos.getX() + 0.5D,
                            dropPos.getY() + 0.5D,
                            dropPos.getZ() + 0.5D,
                            mount.getAttachment().copy()
                    );
                }
                mount.getInventory().setWithoutNotification(ItemStack.EMPTY);
            }
        }

        super.onRemove(state, level, pos, newState, moving);
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

        if (!level.isClientSide
                && neighborPos.equals(pos.relative(
                state.getValue(HORIZONTAL_FACING).getOpposite()
        ))
                && !state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        Direction outputDirection = state.getValue(HORIZONTAL_FACING);
        return Shapes.or(
                rotateShape(outputDirection),
                SprocketMountAttachmentShapes.getOutlineShape(
                        getAttachment(level, pos),
                        outputDirection,
                        context
                )
        );
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        Direction outputDirection = state.getValue(HORIZONTAL_FACING);
        return Shapes.or(
                rotateShape(outputDirection),
                SprocketMountAttachmentShapes.getCollisionShape(
                        getAttachment(level, pos),
                        outputDirection,
                        context
                )
        );
    }

    @Override
    public Class<SprocketMountBlockEntity> getBlockEntityClass() {
        return SprocketMountBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SprocketMountBlockEntity>
    getBlockEntityType() {
        return CreateMechanicalDrive.SPROCKET_MOUNT_BLOCK_ENTITY.get();
    }

    private static Map<Direction, VoxelShape> createRotatedShapes() {
        Map<Direction, VoxelShape> shapes =
                new EnumMap<>(Direction.class);

        shapes.put(
                Direction.NORTH,
                rotateShapeUncached(Direction.NORTH)
        );
        shapes.put(
                Direction.SOUTH,
                rotateShapeUncached(Direction.SOUTH)
        );
        shapes.put(
                Direction.WEST,
                rotateShapeUncached(Direction.WEST)
        );
        shapes.put(
                Direction.EAST,
                rotateShapeUncached(Direction.EAST)
        );

        return shapes;
    }

    private static VoxelShape rotateShape(Direction facing) {
        return ROTATED_SHAPES.get(facing);
    }

    private static VoxelShape rotateShapeUncached(Direction facing) {
        VoxelShape result = Shapes.empty();

        for (var box : SHAPE_UP.toAabbs()) {
            double minX = box.minX * 16.0D;
            double minY = box.minY * 16.0D;
            double minZ = box.minZ * 16.0D;
            double maxX = box.maxX * 16.0D;
            double maxY = box.maxY * 16.0D;
            double maxZ = box.maxZ * 16.0D;

            VoxelShape transformed = switch (facing) {
                case NORTH -> Block.box(
                        minX, minZ, 16.0D - maxY,
                        maxX, maxZ, 16.0D - minY
                );
                case SOUTH -> Block.box(
                        minX, 16.0D - maxZ, minY,
                        maxX, 16.0D - minZ, maxY
                );
                case WEST -> Block.box(
                        16.0D - maxY, minZ, minX,
                        16.0D - minY, maxZ, maxX
                );
                case EAST -> Block.box(
                        minY, minZ, 16.0D - maxX,
                        maxY, maxZ, 16.0D - minX
                );
                default -> throw new IllegalStateException(
                        "Sprocket mount cannot face " + facing
                );
            };

            result = Shapes.or(result, transformed);
        }

        return result;
    }

    private static ItemStack getAttachment(
            BlockGetter level,
            BlockPos pos
    ) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof SprocketMountBlockEntity mount
                ? mount.getAttachment()
                : ItemStack.EMPTY;
    }
}
