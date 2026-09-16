package dev.createmechanicaldrive.content.dog_clutch;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.foundation.block.IBE;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.HitResult;

import javax.annotation.Nullable;
import java.util.List;

public class DogClutchBlock
        extends KineticBlock
        implements IBE<DogClutchBlockEntity>, IWrenchable {

    public static final DirectionProperty FACING =
            BlockStateProperties.FACING;

    public DogClutchBlock(Properties properties) {
        super(properties);

        registerDefaultState(
                defaultBlockState()
                        .setValue(
                                FACING,
                                Direction.UP
                        )
        );
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(
            BlockPlaceContext context
    ) {
        Direction facing =
                context.getNearestLookingDirection();

        BlockPos pos =
                context.getClickedPos();

        LevelReader level =
                context.getLevel();

        Direction connectedDirection =
                findConnectableShaftDirection(
                        level,
                        pos
                );

        if (connectedDirection != null) {
            facing =
                    connectedDirection;
        }

        return defaultBlockState()
                .setValue(
                        FACING,
                        facing
                );
    }

    @Nullable
    private Direction findConnectableShaftDirection(
            LevelReader level,
            BlockPos pos
    ) {
        for (
                Direction direction
                : Direction.values()
        ) {
            BlockPos neighborPos =
                    pos.relative(direction);

            BlockState neighborState =
                    level.getBlockState(
                            neighborPos
                    );

            if (!(
                    neighborState.getBlock()
                            instanceof IRotate rotate
            )) {
                continue;
            }

            if (!rotate.hasShaftTowards(
                    level,
                    neighborPos,
                    neighborState,
                    direction.getOpposite()
            )) {
                continue;
            }

            return direction;
        }

        return null;
    }

    @Override
    public boolean hasShaftTowards(
            LevelReader level,
            BlockPos pos,
            BlockState state,
            Direction direction
    ) {
        return direction.getAxis()
                == state.getValue(FACING)
                .getAxis();
    }

    @Override
    public Direction.Axis getRotationAxis(
            BlockState state
    ) {
        return state.getValue(FACING)
                .getAxis();
    }

    @Override
    public void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean isMoving
    ) {
        super.onPlace(
                state,
                level,
                pos,
                oldState,
                isMoving
        );

        if (!level.isClientSide) {
            level.scheduleTick(
                    pos,
                    this,
                    1
            );
        }
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

        level.scheduleTick(
                pos,
                this,
                1
        );
    }

    @Override
    protected void tick(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            RandomSource random
    ) {
        updateRedstoneState(
                state,
                level,
                pos
        );
    }

    private void updateRedstoneState(
            BlockState state,
            ServerLevel level,
            BlockPos pos
    ) {
        boolean powered =
                level.hasNeighborSignal(pos);

        boolean engaged =
                state.is(
                        CreateMechanicalDrive
                                .DOG_CLUTCH_ENGAGED
                                .get()
                );

        boolean shouldBeEngaged =
                !powered;

        if (shouldBeEngaged == engaged) {
            return;
        }

        Direction facing =
                state.getValue(FACING);

        BlockState newState =
                (
                        shouldBeEngaged
                                ? CreateMechanicalDrive
                                .DOG_CLUTCH_ENGAGED
                                .get()
                                : CreateMechanicalDrive
                                .DOG_CLUTCH
                                .get()
                )
                        .defaultBlockState()
                        .setValue(
                                FACING,
                                facing
                        );

        KineticBlockEntity
                .switchToBlockState(
                        level,
                        pos,
                        newState
                );

        if (
                level.getBlockEntity(pos)
                        instanceof DogClutchBlockEntity blockEntity
        ) {
            blockEntity.triggerGearAnimation(
                    shouldBeEngaged
            );
        }
    }

    @Override
    public void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean isMoving
    ) {
        if (
                newState.is(
                        CreateMechanicalDrive
                                .DOG_CLUTCH
                                .get()
                )
                        || newState.is(
                        CreateMechanicalDrive
                                .DOG_CLUTCH_ENGAGED
                                .get()
                )
        ) {
            return;
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
    public List<ItemStack> getDrops(
            BlockState state,
            LootParams.Builder params
    ) {
        return List.of(
                new ItemStack(
                        CreateMechanicalDrive
                                .DOG_CLUTCH_ITEM
                                .get()
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
        return new ItemStack(
                CreateMechanicalDrive
                        .DOG_CLUTCH_ITEM
                        .get()
        );
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<
                    Block,
                    BlockState
                    > builder
    ) {
        super.createBlockStateDefinition(
                builder
        );

        builder.add(FACING);
    }

    @Override
    public Class<DogClutchBlockEntity>
    getBlockEntityClass() {
        return DogClutchBlockEntity.class;
    }

    @Override
    public BlockEntityType<
            ? extends DogClutchBlockEntity
            > getBlockEntityType() {
        return CreateMechanicalDrive
                .DOG_CLUTCH_BLOCK_ENTITY
                .get();
    }
}
