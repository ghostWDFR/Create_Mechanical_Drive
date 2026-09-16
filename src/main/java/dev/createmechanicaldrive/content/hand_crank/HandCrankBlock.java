package dev.createmechanicaldrive.content.hand_crank;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.AllBlocks;
import net.minecraft.world.item.context.BlockPlaceContext;
import com.simibubi.create.infrastructure.config.AllConfigs;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

public class HandCrankBlock
        extends RotatedPillarKineticBlock
        implements IBE<HandCrankBlockEntity>, IWrenchable {

    public static final int ROTATION_SPEED =
            32;

    public static final float STRESS_CAPACITY =
            16.0F;

    private static final VoxelShape SHAPE_X =
            Block.box(
                    0.0D,
                    5.0D,
                    5.0D,
                    16.0D,
                    11.0D,
                    11.0D
            );

    private static final VoxelShape SHAPE_Y =
            Block.box(
                    5.0D,
                    0.0D,
                    5.0D,
                    11.0D,
                    16.0D,
                    11.0D
            );

    private static final VoxelShape SHAPE_Z =
            Block.box(
                    5.0D,
                    5.0D,
                    0.0D,
                    11.0D,
                    11.0D,
                    16.0D
            );

    public HandCrankBlock(
            Properties properties
    ) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return switch (
                state.getValue(
                        AXIS
                )
                ) {
            case X -> SHAPE_X;
            case Y -> SHAPE_Y;
            case Z -> SHAPE_Z;
        };
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
        return state.getValue(
                AXIS
        );
    }

    @Override
    public boolean hasShaftTowards(
            LevelReader level,
            BlockPos pos,
            BlockState state,
            Direction direction
    ) {
        return direction.getAxis()
                == state.getValue(
                AXIS
        );
    }

    @Override
    public boolean canBeReplaced(
            BlockState state,
            BlockPlaceContext context
    ) {
        return false;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        return turn(
                level,
                pos,
                player,
                ItemStack.EMPTY
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
        if (stack.isEmpty()) {
            return ItemInteractionResult
                    .PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (AllItems.WRENCH.isIn(
                stack
        )) {
            return ItemInteractionResult
                    .PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        return ItemInteractionResult
                .SKIP_DEFAULT_BLOCK_INTERACTION;
    }

    private boolean hasValidConnections(
            Level level,
            BlockPos pos,
            BlockState state
    ) {
        if (!(level.getBlockEntity(pos)
                instanceof HandCrankBlockEntity crank)) {
            return false;
        }

        Direction.Axis axis =
                state.getValue(
                        AXIS
                );

        Direction positive =
                Direction.get(
                        Direction.AxisDirection.POSITIVE,
                        axis
                );

        Direction negative =
                positive.getOpposite();

        BlockPos positivePos =
                pos.relative(
                        positive
                );

        BlockPos negativePos =
                pos.relative(
                        negative
                );

        if (isCreateHandCrank(
                level,
                positivePos
        ) || isCreateHandCrank(
                level,
                negativePos
        )) {
            return false;
        }

        return hasKineticConnection(
                level,
                crank,
                positivePos
        ) && hasKineticConnection(
                level,
                crank,
                negativePos
        );
    }

    private boolean isCreateHandCrank(
            Level level,
            BlockPos pos
    ) {
        return level.getBlockState(
                pos
        ).is(
                AllBlocks.HAND_CRANK.get()
        );
    }

    private boolean hasKineticConnection(
            Level level,
            HandCrankBlockEntity crank,
            BlockPos neighbourPos
    ) {
        if (!(level.getBlockEntity(neighbourPos)
                instanceof KineticBlockEntity neighbour)) {
            return false;
        }

        return RotationPropagator.isConnected(
                crank,
                neighbour
        ) || RotationPropagator.isConnected(
                neighbour,
                crank
        );
    }

    private InteractionResult turn(
            Level level,
            BlockPos pos,
            Player player,
            ItemStack heldStack
    ) {
        if (player.isSpectator()) {
            return InteractionResult.PASS;
        }

        BlockState state =
                level.getBlockState(
                        pos
                );

        if (!hasValidConnections(
                level,
                pos,
                state
        )) {
            if (!level.isClientSide) {
                level.destroyBlock(
                        pos,
                        true
                );
            }

            return InteractionResult.FAIL;
        }

        withBlockEntityDo(
                level,
                pos,
                blockEntity ->
                        blockEntity.turn(
                                player.isShiftKeyDown()
                        )
        );

        if (!AllItems.EXTENDO_GRIP.isIn(
                heldStack
        )) {
            player.causeFoodExhaustion(
                    ROTATION_SPEED
                            * AllConfigs
                            .server()
                            .kinetics
                            .crankHungerMultiplier
                            .getF()
                            * 2.0F
            );
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public List<ItemStack> getDrops(
            BlockState state,
            LootParams.Builder params
    ) {
        return List.of(
                new ItemStack(
                        CreateMechanicalDrive
                                .HAND_CRANK_ITEM
                                .get()
                )
        );
    }

    @Override
    protected RenderShape getRenderShape(
            BlockState state
    ) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public Class<HandCrankBlockEntity>
    getBlockEntityClass() {
        return HandCrankBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends HandCrankBlockEntity>
    getBlockEntityType() {
        return CreateMechanicalDrive
                .HAND_CRANK_BLOCK_ENTITY
                .get();
    }
}