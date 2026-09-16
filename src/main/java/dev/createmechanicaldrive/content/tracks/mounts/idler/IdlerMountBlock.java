package dev.createmechanicaldrive.content.tracks.mounts.idler;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
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
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.Containers;

import java.util.EnumMap;
import java.util.Map;

public class IdlerMountBlock extends Block
        implements IBE<IdlerMountBlockEntity>, IWrenchable {
    public static final DirectionProperty HORIZONTAL_FACING =
            BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape MOVING_AXLE_SHAPE_UP = Shapes.or(
            Block.box(5.0D, 1.0D, 4.0D, 11.0D, 2.0D, 12.0D),
            Block.box(4.0D, 1.0D, 5.0D, 12.0D, 2.0D, 11.0D),
            Block.box(5.0D, 0.975D, 5.0D, 11.0D, 3.0D, 11.0D)
    );
    private static final VoxelShape STATIC_MOUNT_SHAPE_UP = Shapes.or(
            Block.box(15.0D, 0.0D, 5.0D, 16.0D, 1.0D, 11.0D),
            Block.box(1.0D, 0.0D, 4.0D, 15.0D, 1.0D, 12.0D),
            Block.box(0.0D, 0.0D, 5.0D, 1.0D, 1.0D, 11.0D)
    );

    private static final Map<Direction, VoxelShape> STATIC_MOUNT_SHAPES =
            createRotatedMountShapes();

    public IdlerMountBlock(Properties properties) {
        super(properties);
        registerDefaultState(
                defaultBlockState().setValue(
                        HORIZONTAL_FACING,
                        Direction.NORTH
                )
        );
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        super.createBlockStateDefinition(builder);
        builder.add(HORIZONTAL_FACING);
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

        IdlerMountBlockEntity mount = getMount(level, pos);
        if (!held.isEmpty()
                && mount != null
                && !mount.getAttachment().isEmpty()) {
            return level.isClientSide
                    ? ItemInteractionResult.SUCCESS
                    : ItemInteractionResult.CONSUME;
        }

        if (!IdlerMountAttachmentShapes.supports(held)) {
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

        IdlerMountBlockEntity mount = getMount(level, pos);
        if (mount == null || mount.getAttachment().isEmpty()) {
            return super.useWithoutItem(state, level, pos, player, hit);
        }

        if (mount.mechanicalDrive$getTrackSprocket() != null) {
            if (!level.isClientSide) {
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable(
                                "item.mechanical_drive.track_link.remove_track_first"
                        ),
                        true
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
        IdlerMountBlockEntity mount = getMount(level, pos);
        if (mount == null
                || held.isEmpty()
                && mount.mechanicalDrive$getTrackSprocket() != null
                || (!held.isEmpty()
                && (!IdlerMountAttachmentShapes.supports(held)
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
    private static IdlerMountBlockEntity getMount(
            Level level,
            BlockPos pos
    ) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof IdlerMountBlockEntity mount
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
            IdlerMountBlockEntity mount = getMount(level, pos);
            if (mount != null) {
                dev.createmechanicaldrive.content.tracks.chain.TrackAssemblyManager
                        .disassembleForWheel(mount);
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
        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (blockEntity instanceof IdlerMountBlockEntity mount) {
            return mount.getCachedOutlineShape();
        }

        return STATIC_MOUNT_SHAPES.get(
                state.getValue(HORIZONTAL_FACING)
        );
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (blockEntity instanceof IdlerMountBlockEntity mount) {
            return mount.getCachedCollisionShape();
        }

        return STATIC_MOUNT_SHAPES.get(
                state.getValue(HORIZONTAL_FACING)
        );
    }

    static VoxelShape createCombinedShape(
            Direction outputDirection,
            ItemStack attachment,
            double axleOffset,
            boolean collision
    ) {
        CollisionContext context = CollisionContext.empty();

        VoxelShape attachmentShape = collision
                ? IdlerMountAttachmentShapes.getCollisionShape(
                attachment,
                outputDirection,
                context,
                axleOffset
        )
                : IdlerMountAttachmentShapes.getOutlineShape(
                attachment,
                outputDirection,
                context,
                axleOffset
        );

        return Shapes.or(
                STATIC_MOUNT_SHAPES.get(outputDirection),
                IdlerMountAttachmentShapes.moveLaterally(
                        IdlerMountAttachmentShapes.rotateFromUp(
                                MOVING_AXLE_SHAPE_UP,
                                outputDirection
                        ),
                        outputDirection,
                        axleOffset
                ),
                attachmentShape
        );
    }

    @Override
    public Class<IdlerMountBlockEntity> getBlockEntityClass() {
        return IdlerMountBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends IdlerMountBlockEntity>
    getBlockEntityType() {
        return CreateMechanicalDrive.IDLER_MOUNT_BLOCK_ENTITY.get();
    }

    private static Map<Direction, VoxelShape> createRotatedMountShapes() {
        Map<Direction, VoxelShape> shapes =
                new EnumMap<>(Direction.class);

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            shapes.put(
                    direction,
                    IdlerMountAttachmentShapes.rotateFromUp(
                            STATIC_MOUNT_SHAPE_UP,
                            direction
                    )
            );
        }

        return shapes;
    }
}
