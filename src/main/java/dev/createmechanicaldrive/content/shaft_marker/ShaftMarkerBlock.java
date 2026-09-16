package dev.createmechanicaldrive.content.shaft_marker;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

public class ShaftMarkerBlock extends DirectionalKineticBlock
        implements IBE<ShaftMarkerBlockEntity>, IWrenchable {
    public static final EnumProperty<DyeColor> COLOR =
            EnumProperty.create("color", DyeColor.class);

    private static final VoxelShape UP_SHAPE =
            Block.box(4.0D, 0.0D, 4.0D, 12.0D, 10.0D, 12.0D);
    private static final VoxelShape DOWN_SHAPE =
            Block.box(4.0D, 6.0D, 4.0D, 12.0D, 16.0D, 12.0D);
    private static final VoxelShape NORTH_SHAPE =
            Block.box(4.0D, 4.0D, 6.0D, 12.0D, 12.0D, 16.0D);
    private static final VoxelShape SOUTH_SHAPE =
            Block.box(4.0D, 4.0D, 0.0D, 12.0D, 12.0D, 10.0D);
    private static final VoxelShape WEST_SHAPE =
            Block.box(6.0D, 4.0D, 4.0D, 16.0D, 12.0D, 12.0D);
    private static final VoxelShape EAST_SHAPE =
            Block.box(0.0D, 4.0D, 4.0D, 10.0D, 12.0D, 12.0D);

    public ShaftMarkerBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.UP)
                .setValue(COLOR, DyeColor.WHITE));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(
                FACING,
                context.getClickedFace()
        );
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(
            LevelReader level,
            BlockPos pos,
            BlockState state,
            Direction direction
    ) {
        return direction == state.getValue(FACING).getOpposite();
    }

    @Override
    public boolean canSurvive(
            BlockState state,
            LevelReader level,
            BlockPos pos
    ) {
        Direction supportDirection = state.getValue(FACING).getOpposite();
        BlockPos supportPos = pos.relative(supportDirection);
        return !level.getBlockState(supportPos).isAir();
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

        BlockPos supportPos = pos.relative(
                state.getValue(FACING).getOpposite()
        );
        if (!neighborPos.equals(supportPos)
                || state.canSurvive(level, pos)) {
            return;
        }

        Block.dropResources(state, level, pos);
        level.destroyBlock(pos, false);
    }

    @Override
    protected boolean areStatesKineticallyEquivalent(
            BlockState oldState,
            BlockState newState
    ) {
        return oldState.getBlock() == newState.getBlock()
                && oldState.getValue(FACING) == newState.getValue(FACING);
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
        if (!(stack.getItem() instanceof DyeItem dyeItem)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        DyeColor color = dyeItem.getDyeColor();
        if (state.getValue(COLOR) == color) {
            return ItemInteractionResult.SUCCESS;
        }

        if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(COLOR, color), 3);
            level.playSound(
                    null,
                    pos,
                    SoundEvents.DYE_USE,
                    SoundSource.BLOCKS,
                    1.0F,
                    1.0F
            );
            if (!player.isCreative()) {
                stack.shrink(1);
            }
        }
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    public VoxelShape getShape(
            BlockState state,
            net.minecraft.world.level.BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return switch (state.getValue(FACING)) {
            case UP -> UP_SHAPE;
            case DOWN -> DOWN_SHAPE;
            case NORTH -> NORTH_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            case EAST -> EAST_SHAPE;
        };
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public List<ItemStack> getDrops(
            BlockState state,
            LootParams.Builder params
    ) {
        return List.of(createStack(state.getValue(COLOR)));
    }

    @Override
    public ItemStack getCloneItemStack(
            BlockState state,
            HitResult target,
            LevelReader level,
            BlockPos pos,
            Player player
    ) {
        return createStack(state.getValue(COLOR));
    }

    private static ItemStack createStack(DyeColor color) {
        return ShaftMarkerItem.colored(
                new ItemStack(CreateMechanicalDrive.SHAFT_MARKER_ITEM.get()),
                color
        );
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        super.createBlockStateDefinition(builder);
        builder.add(COLOR);
    }

    @Override
    public Class<ShaftMarkerBlockEntity> getBlockEntityClass() {
        return ShaftMarkerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ShaftMarkerBlockEntity>
    getBlockEntityType() {
        return CreateMechanicalDrive.SHAFT_MARKER_BLOCK_ENTITY.get();
    }
}
