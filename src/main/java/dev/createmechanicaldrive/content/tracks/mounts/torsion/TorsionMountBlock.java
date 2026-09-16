package dev.createmechanicaldrive.content.tracks.mounts.torsion;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.equipment.wrench.WrenchItem;
import com.simibubi.create.foundation.block.IBE;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.createmechanicaldrive.content.tracks.wheels.support.SupportWheelItem;
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
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

public class TorsionMountBlock extends Block
        implements IBE<TorsionMountBlockEntity>, IWrenchable {
    public static final DirectionProperty HORIZONTAL_FACING =
            BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty REVERSED =
            BooleanProperty.create("reversed");

    private static final VoxelShape BASE_SHAPE_UP =
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 15.0D);
    private static final Map<Direction, VoxelShape> BASE_SHAPES =
            createBaseShapes(BASE_SHAPE_UP);
    private static final Map<Direction, VoxelShape> REVERSED_BASE_SHAPES =
            createBaseShapes(mirrorAcrossBlockCenter(BASE_SHAPE_UP));

    public TorsionMountBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(HORIZONTAL_FACING, Direction.NORTH)
                .setValue(REVERSED, false));
    }

    public boolean supportsAttachment(ItemStack stack) {
        return TorsionMountAttachments.supports(stack);
    }

    public boolean supportsSupportWheel(ItemStack stack) {
        return TorsionMountAttachments.supportsSupportWheel(stack);
    }

    public boolean allowsSupportWheel() {
        return true;
    }

    public double armLength() {
        return TorsionMountBlockEntity.ARM_LENGTH;
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        super.createBlockStateDefinition(builder);
        builder.add(HORIZONTAL_FACING, REVERSED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction output = context.getClickedFace();
        if (output.getAxis().isVertical()) {
            return null;
        }

        BlockState state = defaultBlockState()
                .setValue(HORIZONTAL_FACING, output);
        return state.canSurvive(context.getLevel(), context.getClickedPos())
                ? state
                : null;
    }

    @Override
    public boolean canSurvive(
            BlockState state,
            LevelReader level,
            BlockPos pos
    ) {
        Direction output = state.getValue(HORIZONTAL_FACING);
        BlockPos supportPos = pos.relative(output.getOpposite());
        return level.getBlockState(supportPos).isFaceSturdy(
                level,
                supportPos,
                output
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
        // Let Create's wrench reach IWrenchable even while a wheel occupies
        // the attachment slot. Otherwise the generic occupied-slot guard
        // consumes the click before WrenchItem can call onWrenched.
        if (held.getItem() instanceof WrenchItem) {
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
        if (hit.getDirection() != state.getValue(HORIZONTAL_FACING)) {
            return super.useItemOn(held, state, level, pos, player, hand, hit);
        }

        TorsionMountBlockEntity mount = getMount(level, pos);
        if (SupportWheelItem.isSupportWheel(held)
                && supportsSupportWheel(held)) {
            if (mount == null || !mount.getSupportWheel().isEmpty()) {
                return level.isClientSide
                        ? ItemInteractionResult.SUCCESS
                        : ItemInteractionResult.CONSUME;
            }
            if (!mount.canInstallSupportWheel()) {
                if (!level.isClientSide) {
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.translatable(
                                    "item.mechanical_drive.support_wheel.cannot_install"
                            ),
                            true
                    );
                }
                return level.isClientSide
                        ? ItemInteractionResult.SUCCESS
                        : ItemInteractionResult.CONSUME;
            }
            if (level.isClientSide) {
                return ItemInteractionResult.SUCCESS;
            }
            return swapSupportWheel(level, pos, player, held)
                    ? ItemInteractionResult.CONSUME
                    : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!held.isEmpty()
                && mount != null
                && !mount.getAttachment().isEmpty()) {
            return level.isClientSide
                    ? ItemInteractionResult.SUCCESS
                    : ItemInteractionResult.CONSUME;
        }

        if (!supportsAttachment(held)) {
            return super.useItemOn(held, state, level, pos, player, hand, hit);
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
        if (!player.getMainHandItem().isEmpty()
                || hit.getDirection() != state.getValue(HORIZONTAL_FACING)) {
            return super.useWithoutItem(state, level, pos, player, hit);
        }

        TorsionMountBlockEntity mount = getMount(level, pos);
        if (mount != null
                && mount.mechanicalDrive$getTrackSprocket() != null) {
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

        if (mount == null || mount.getAttachment().isEmpty()) {
            if (mount != null && !mount.getSupportWheel().isEmpty()) {
                if (level.isClientSide) {
                    return InteractionResult.SUCCESS;
                }
                return swapSupportWheel(
                        level,
                        pos,
                        player,
                        ItemStack.EMPTY
                ) ? InteractionResult.CONSUME : InteractionResult.PASS;
            }
            return super.useWithoutItem(state, level, pos, player, hit);
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (!mount.getSupportWheel().isEmpty()) {
            return swapSupportWheel(
                    level,
                    pos,
                    player,
                    ItemStack.EMPTY
            ) ? InteractionResult.CONSUME : InteractionResult.PASS;
        }

        return swapAttachment(level, pos, player, ItemStack.EMPTY)
                ? InteractionResult.CONSUME
                : InteractionResult.PASS;
    }

    @Override
    public InteractionResult onWrenched(
            BlockState state,
            UseOnContext context
    ) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        level.setBlock(
                pos,
                state.cycle(REVERSED),
                Block.UPDATE_ALL
        );
        IWrenchable.playRotateSound(level, pos);
        return InteractionResult.SUCCESS;
    }

    private static boolean swapAttachment(
            Level level,
            BlockPos pos,
            Player player,
            ItemStack held
    ) {
        TorsionMountBlockEntity mount = getMount(level, pos);
        if (mount == null
                || held.isEmpty()
                && mount.mechanicalDrive$getTrackSprocket() != null
                || (!held.isEmpty()
                && (!mount.isAttachmentSupported(held)
                || !mount.getAttachment().isEmpty()))) {
            return false;
        }

        ItemStack previous = mount.getAttachment().copy();
        ItemStack replacement = held.isEmpty()
                ? ItemStack.EMPTY
                : held.copyWithCount(1);
        mount.getInventory().setStackInSlot(
                TorsionMountBlockEntity.ATTACHMENT_SLOT,
                replacement
        );

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

    private static boolean swapSupportWheel(
            Level level,
            BlockPos pos,
            Player player,
            ItemStack held
    ) {
        TorsionMountBlockEntity mount = getMount(level, pos);
        if (mount == null
                || held.isEmpty()
                && mount.mechanicalDrive$getTrackSprocket() != null
                || !held.isEmpty()
                && (!mount.isSupportWheelSupported(held)
                || !mount.getSupportWheel().isEmpty()
                || !mount.canInstallSupportWheel())) {
            return false;
        }

        ItemStack previous = mount.getSupportWheel().copy();
        ItemStack replacement = held.isEmpty()
                ? ItemStack.EMPTY
                : held.copyWithCount(1);
        mount.getInventory().setStackInSlot(
                TorsionMountBlockEntity.SUPPORT_WHEEL_SLOT,
                replacement
        );

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
    private static TorsionMountBlockEntity getMount(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof TorsionMountBlockEntity mount
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
            TorsionMountBlockEntity mount = getMount(level, pos);
            if (mount != null) {
                dev.createmechanicaldrive.content.tracks.chain.TrackAssemblyManager
                        .disassembleForWheel(mount);
            }
            if (mount != null && !mount.getAttachment().isEmpty()) {
                if (!level.isClientSide) {
                    ItemEntity drop = new ItemEntity(
                            level,
                            pos.getX() + 0.5D,
                            pos.getY() + 0.5D,
                            pos.getZ() + 0.5D,
                            mount.getAttachment().copy()
                    );
                    drop.setDefaultPickUpDelay();
                    level.addFreshEntity(drop);
                }
                mount.getInventory().setWithoutNotification(ItemStack.EMPTY);
            }
            if (mount != null && !mount.getSupportWheel().isEmpty()) {
                if (!level.isClientSide) {
                    ItemEntity drop = new ItemEntity(
                            level,
                            pos.getX() + 0.5D,
                            pos.getY() + 0.5D,
                            pos.getZ() + 0.5D,
                            mount.getSupportWheel().copy()
                    );
                    drop.setDefaultPickUpDelay();
                    level.addFreshEntity(drop);
                }
                mount.getInventory().setWithoutNotification(
                        TorsionMountBlockEntity.SUPPORT_WHEEL_SLOT,
                        ItemStack.EMPTY
                );
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
        super.neighborChanged(state, level, pos, block, neighborPos, movedByPiston);
        Direction support = state.getValue(HORIZONTAL_FACING).getOpposite();
        if (!level.isClientSide
                && neighborPos.equals(pos.relative(support))
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
        return baseShape(state);
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return baseShape(state);
    }

    @Override
    public Class<TorsionMountBlockEntity> getBlockEntityClass() {
        return TorsionMountBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends TorsionMountBlockEntity>
    getBlockEntityType() {
        return CreateMechanicalDrive.TORSION_MOUNT_BLOCK_ENTITY.get();
    }

    private static VoxelShape baseShape(BlockState state) {
        Map<Direction, VoxelShape> shapes = state.getValue(REVERSED)
                ? REVERSED_BASE_SHAPES
                : BASE_SHAPES;
        return shapes.get(state.getValue(HORIZONTAL_FACING));
    }

    private static Map<Direction, VoxelShape> createBaseShapes(
            VoxelShape source
    ) {
        Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            shapes.put(direction, rotateFromUp(source, direction));
        }
        return shapes;
    }

    private static VoxelShape mirrorAcrossBlockCenter(VoxelShape shape) {
        VoxelShape result = Shapes.empty();
        for (var box : shape.toAabbs()) {
            result = Shapes.or(result, Block.box(
                    16.0D - box.maxX * 16.0D,
                    box.minY * 16.0D,
                    box.minZ * 16.0D,
                    16.0D - box.minX * 16.0D,
                    box.maxY * 16.0D,
                    box.maxZ * 16.0D
            ));
        }
        return result;
    }

    private static VoxelShape rotateFromUp(
            VoxelShape shape,
            Direction facing
    ) {
        VoxelShape result = Shapes.empty();
        for (var box : shape.toAabbs()) {
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
                default -> throw new IllegalArgumentException(
                        "Torsion mount cannot face " + facing
                );
            };
            result = Shapes.or(result, transformed);
        }
        return result;
    }
}
