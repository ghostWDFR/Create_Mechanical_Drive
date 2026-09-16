package dev.createmechanicaldrive.content.cardan_shaft;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.createmechanicaldrive.content.mechanical_jack.MechanicalJackPhysics;
import dev.ryanhcode.sable.api.block.BlockSubLevelAssemblyListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.List;

public class CardanJointBlock
        extends DirectionalKineticBlock
        implements IBE<CardanJointBlockEntity>, IWrenchable,
        BlockSubLevelAssemblyListener {
    private static final VoxelShape SHAPE_UP =
            makeShape(
                    4.0D,
                    0.0D,
                    4.0D,
                    12.0D,
                    7.0D,
                    12.0D,
                    5.5D,
                    0.0D,
                    5.5D,
                    10.5D,
                    11.0D,
                    10.5D
            );

    private static final VoxelShape SHAPE_DOWN =
            makeShape(
                    4.0D,
                    9.0D,
                    4.0D,
                    12.0D,
                    16.0D,
                    12.0D,
                    5.5D,
                    5.0D,
                    5.5D,
                    10.5D,
                    16.0D,
                    10.5D
            );

    private static final VoxelShape SHAPE_NORTH =
            makeShape(
                    4.0D,
                    4.0D,
                    9.0D,
                    12.0D,
                    12.0D,
                    16.0D,
                    5.5D,
                    5.5D,
                    5.0D,
                    10.5D,
                    10.5D,
                    16.0D
            );

    private static final VoxelShape SHAPE_SOUTH =
            makeShape(
                    4.0D,
                    4.0D,
                    0.0D,
                    12.0D,
                    12.0D,
                    7.0D,
                    5.5D,
                    5.5D,
                    0.0D,
                    10.5D,
                    10.5D,
                    11.0D
            );

    private static final VoxelShape SHAPE_WEST =
            makeShape(
                    9.0D,
                    4.0D,
                    4.0D,
                    16.0D,
                    12.0D,
                    12.0D,
                    5.0D,
                    5.5D,
                    5.5D,
                    16.0D,
                    10.5D,
                    10.5D
            );

    private static final VoxelShape SHAPE_EAST =
            makeShape(
                    0.0D,
                    4.0D,
                    4.0D,
                    7.0D,
                    12.0D,
                    12.0D,
                    0.0D,
                    5.5D,
                    5.5D,
                    11.0D,
                    10.5D,
                    10.5D
            );

    public CardanJointBlock(
            Properties properties
    ) {
        super(properties);

        registerDefaultState(
                defaultBlockState()
                        .setValue(
                                FACING,
                                Direction.UP
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
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return getShapeForFacing(
                state.getValue(FACING)
        );
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
        return direction == state.getValue(FACING)
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

        level.destroyBlock(
                pos,
                false
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
                instanceof CardanJointBlockEntity joint) {
            joint.destroyLink(
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
                instanceof CardanJointBlockEntity joint) {
            boolean collectShaft =
                    joint.hasLink()
                            && !joint.isPending();

            joint.destroyLink(
                    false
            );

            if (collectShaft
                    && player != null
                    && !player.isCreative()) {
                player.getInventory()
                        .placeItemBackInInventory(
                                new ItemStack(
                                        CreateMechanicalDrive
                                                .CARDAN_SHAFT_ITEM
                                                .get()
                                )
                        );
            }
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
    public void afterMove(
            ServerLevel originLevel,
            ServerLevel resultingLevel,
            BlockState newState,
            BlockPos oldPos,
            BlockPos newPos
    ) {
        if (resultingLevel.getBlockEntity(newPos)
                instanceof CardanJointBlockEntity movedJoint) {
            movedJoint.afterAssemblyMove(
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
        return List.of();
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
                        .CARDAN_SHAFT_ITEM
                        .get()
        );
    }

    @Override
    protected RenderShape getRenderShape(
            BlockState state
    ) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        super.createBlockStateDefinition(
                builder
        );
    }


    private static VoxelShape makeShape(
            double baseMinX,
            double baseMinY,
            double baseMinZ,
            double baseMaxX,
            double baseMaxY,
            double baseMaxZ,
            double shaftMinX,
            double shaftMinY,
            double shaftMinZ,
            double shaftMaxX,
            double shaftMaxY,
            double shaftMaxZ
    ) {
        return Shapes.or(
                Block.box(
                        baseMinX,
                        baseMinY,
                        baseMinZ,
                        baseMaxX,
                        baseMaxY,
                        baseMaxZ
                ),
                Block.box(
                        shaftMinX,
                        shaftMinY,
                        shaftMinZ,
                        shaftMaxX,
                        shaftMaxY,
                        shaftMaxZ
                )
        );
    }

    private static VoxelShape getShapeForFacing(
            Direction facing
    ) {
        return switch (facing) {
            case DOWN -> SHAPE_DOWN;
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case WEST -> SHAPE_WEST;
            case EAST -> SHAPE_EAST;
            case UP -> SHAPE_UP;
        };
    }

    @Override
    public Class<CardanJointBlockEntity> getBlockEntityClass() {
        return CardanJointBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CardanJointBlockEntity> getBlockEntityType() {
        return CreateMechanicalDrive
                .CARDAN_JOINT_BLOCK_ENTITY
                .get();
    }
}
