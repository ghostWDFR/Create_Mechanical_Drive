package dev.createmechanicaldrive.content.rigid_link;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import dev.createmechanicaldrive.CreateMechanicalDrive;
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
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

public class RigidLinkJointBlock extends DirectionalBlock
        implements IBE<RigidLinkJointBlockEntity>, IWrenchable,
        BlockSubLevelAssemblyListener {
    public static final MapCodec<RigidLinkJointBlock> CODEC =
            simpleCodec(RigidLinkJointBlock::new);
    private static final VoxelShape SHAPE_UP = Shapes.or(
            Block.box(4, 0, 4, 12, 2, 12),
            Block.box(6, 2, 6, 10, 5, 10),
            Block.box(7, 5, 7, 9, 8, 9)
    );

    public RigidLinkJointBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.UP));
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState placementState = defaultBlockState()
                .setValue(FACING, context.getClickedFace());
        return placementState.canSurvive(context.getLevel(), context.getClickedPos())
                ? placementState
                : null;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level,
                                   BlockPos pos, CollisionContext context) {
        return rotateShape(state.getValue(FACING));
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction support = state.getValue(FACING).getOpposite();
        BlockState supportState = level.getBlockState(pos.relative(support));
        return !supportState.isAir()
                && !(supportState.getBlock() instanceof RigidLinkJointBlock);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos,
                                   Block block, BlockPos neighbourPos,
                                   boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, neighbourPos, movedByPiston);
        if (!level.isClientSide
                && neighbourPos.equals(pos.relative(state.getValue(FACING).getOpposite()))
                && !state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    protected void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean isMoving
    ) {
        if (!state.is(
                newState.getBlock()
        )) {
            BlockEntity blockEntity =
                    level.getBlockEntity(
                            pos
                    );

            if (blockEntity
                    instanceof RigidLinkJointBlockEntity joint) {

                if (!level.isClientSide) {
                    joint.destroyAllLinks(
                            true
                    );
                } else {
                    joint.destroyAllLinks(
                            false
                    );
                }
            }
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
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (blockEntity instanceof RigidLinkJointBlockEntity joint
                && joint.hasLinks()) {
            if (!(level instanceof ServerLevel serverLevel)) {
                return InteractionResult.SUCCESS;
            }

            Player player = context.getPlayer();
            BlockEvent.BreakEvent event =
                    new BlockEvent.BreakEvent(serverLevel, pos, state, player);
            NeoForge.EVENT_BUS.post(event);
            if (event.isCanceled()) {
                return InteractionResult.SUCCESS;
            }

            RigidLinkJointBlockEntity.Connection connection =
                    joint.getLinks().getFirst();
            joint.destroyConnection(connection, false);

            if (player != null && !player.isCreative()) {
                player.getInventory().placeItemBackInInventory(new ItemStack(
                        connection.linkType()
                                == RigidLinkJointBlockEntity.LinkType.LIMITED
                                ? CreateMechanicalDrive.RIGID_LINK_LIMITED_ITEM.get()
                                : CreateMechanicalDrive.RIGID_LINK_ITEM.get()
                ));
            }

            IWrenchable.playRemoveSound(level, pos);
            return InteractionResult.SUCCESS;
        }

        return IWrenchable.super.onSneakWrenched(state, context);
    }

    @Override
    public void afterMove(ServerLevel originLevel, ServerLevel resultingLevel,
                          BlockState newState, BlockPos oldPos, BlockPos newPos) {
        if (resultingLevel.getBlockEntity(newPos)
                instanceof RigidLinkJointBlockEntity movedJoint) {
            movedJoint.afterAssemblyMove(originLevel, resultingLevel, oldPos);
        }
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target,
                                       LevelReader level, BlockPos pos, Player player) {
        return new ItemStack(CreateMechanicalDrive.RIGID_LINK_JOINT_ITEM.get());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    @Override
    public Class<RigidLinkJointBlockEntity> getBlockEntityClass() {
        return RigidLinkJointBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends RigidLinkJointBlockEntity> getBlockEntityType() {
        return CreateMechanicalDrive.RIGID_LINK_JOINT_BLOCK_ENTITY.get();
    }

    private static VoxelShape rotateShape(Direction facing) {
        VoxelShape result = Shapes.empty();
        for (var box : SHAPE_UP.toAabbs()) {
            double minX = box.minX;
            double minY = box.minY;
            double minZ = box.minZ;
            double maxX = box.maxX;
            double maxY = box.maxY;
            double maxZ = box.maxZ;
            VoxelShape transformed = switch (facing) {
                case DOWN -> Block.box(16 - maxX * 16, 16 - maxY * 16, minZ * 16,
                        16 - minX * 16, 16 - minY * 16, maxZ * 16);
                case NORTH -> Block.box(minX * 16, minZ * 16, 16 - maxY * 16,
                        maxX * 16, maxZ * 16, 16 - minY * 16);
                case SOUTH -> Block.box(minX * 16, 16 - maxZ * 16, minY * 16,
                        maxX * 16, 16 - minZ * 16, maxY * 16);
                case WEST -> Block.box(16 - maxY * 16, minZ * 16, minX * 16,
                        16 - minY * 16, maxZ * 16, maxX * 16);
                case EAST -> Block.box(minY * 16, minZ * 16, 16 - maxX * 16,
                        maxY * 16, maxZ * 16, 16 - minX * 16);
                case UP -> Block.box(minX * 16, minY * 16, minZ * 16,
                        maxX * 16, maxY * 16, maxZ * 16);
            };
            result = Shapes.or(result, transformed);
        }
        return result;
    }
}
