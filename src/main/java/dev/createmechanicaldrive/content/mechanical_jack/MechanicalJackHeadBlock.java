package dev.createmechanicaldrive.content.mechanical_jack;

import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.ryanhcode.sable.api.block.BlockSubLevelAssemblyListener;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.Explosion;

import java.util.function.BiConsumer;

public class MechanicalJackHeadBlock extends Block implements BlockSubLevelAssemblyListener {

    public static final DirectionProperty MOUNT =
            DirectionProperty.create(
                    "mount"
            );

    private static final VoxelShape SHAPE_UP =
            Block.box(
                    0.0D,
                    14.0D,
                    0.0D,
                    16.0D,
                    16.0D,
                    16.0D
            );

    private static final VoxelShape SHAPE_DOWN =
            Block.box(
                    0.0D,
                    0.0D,
                    0.0D,
                    16.0D,
                    2.0D,
                    16.0D
            );

    private static final VoxelShape SHAPE_NORTH =
            Block.box(
                    0.0D,
                    0.0D,
                    0.0D,
                    16.0D,
                    16.0D,
                    2.0D
            );

    private static final VoxelShape SHAPE_SOUTH =
            Block.box(
                    0.0D,
                    0.0D,
                    14.0D,
                    16.0D,
                    16.0D,
                    16.0D
            );

    private static final VoxelShape SHAPE_EAST =
            Block.box(
                    14.0D,
                    0.0D,
                    0.0D,
                    16.0D,
                    16.0D,
                    16.0D
            );

    private static final VoxelShape SHAPE_WEST =
            Block.box(
                    0.0D,
                    0.0D,
                    0.0D,
                    2.0D,
                    16.0D,
                    16.0D
            );

    public MechanicalJackHeadBlock(
            Properties properties
    ) {
        super(properties);

        registerDefaultState(
                defaultBlockState()
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
    protected RenderShape getRenderShape(
            BlockState state
    ) {
        return RenderShape.INVISIBLE;
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
            case UP -> SHAPE_UP;
        };
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return getShapeForMount(
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
        return getShapeForMount(
                state
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
        return new ItemStack(
                CreateMechanicalDrive
                        .MECHANICAL_JACK_ITEM
                        .get()
        );
    }

    @Override
    public void beforeMove(
            ServerLevel originLevel,
            ServerLevel resultingLevel,
            BlockState state,
            BlockPos oldPos,
            BlockPos newPos
    ) {
        MechanicalJackPhysics.beforeHeadMoved(
                originLevel,
                oldPos
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
        MechanicalJackPhysics.afterHeadMoved(
                originLevel,
                resultingLevel,
                oldPos,
                newPos
        );
    }
    @Override
    protected void onExplosionHit(
            BlockState state,
            Level level,
            BlockPos pos,
            Explosion explosion,
            BiConsumer<ItemStack, BlockPos> dropConsumer
    ) {
        if (!level.isClientSide
                && level instanceof ServerLevel serverLevel) {
            MechanicalJackPhysics.removeFromHead(
                    serverLevel,
                    pos
            );

            return;
        }

        super.onExplosionHit(
                state,
                level,
                pos,
                explosion,
                dropConsumer
        );
    }

    @Override
    protected void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean movedByPiston
    ) {
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
                && level instanceof ServerLevel serverLevel) {

            MechanicalJackPhysics.removeFromHead(
                    serverLevel,
                    pos,
                    !player.isCreative()
            );
        }

        return super.playerWillDestroy(
                level,
                pos,
                state,
                player
        );
    }
}