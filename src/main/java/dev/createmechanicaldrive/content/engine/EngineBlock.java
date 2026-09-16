package dev.createmechanicaldrive.content.engine;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.foundation.block.IBE;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.List;

public class EngineBlock
        extends HorizontalKineticBlock
        implements IBE<EngineBlockEntity>, IWrenchable {

    public static final DirectionProperty FACING =
            BlockStateProperties.HORIZONTAL_FACING;

    public static final BooleanProperty LIT =
            BlockStateProperties.LIT;

    public EngineBlock(Properties properties) {
        super(properties);

        registerDefaultState(
                defaultBlockState()
                        .setValue(FACING, Direction.NORTH)
                        .setValue(LIT, false)
        );
    }

    @Override
    public BlockState getStateForPlacement(
            BlockPlaceContext context
    ) {
        Level level =
                context.getLevel();

        BlockPos pos =
                context.getClickedPos();

        Direction horizontal =
                context.getHorizontalDirection();

        Direction preferredFacing =
                horizontal.getAxis() == Direction.Axis.X
                        ? horizontal
                        : horizontal.getOpposite();

        Direction clickedFace =
                context.getClickedFace();

        Direction[] directions;

        if (clickedFace.getAxis().isHorizontal()) {
            directions =
                    new Direction[] {
                            clickedFace,
                            clickedFace.getOpposite(),
                            clickedFace.getClockWise(),
                            clickedFace.getCounterClockWise()
                    };
        } else {
            directions =
                    new Direction[] {
                            horizontal.getOpposite(),
                            horizontal,
                            horizontal.getClockWise(),
                            horizontal.getCounterClockWise()
                    };
        }

        boolean connectedToEngine =
                false;

        for (Direction direction : directions) {
            BlockPos neighbourPos =
                    pos.relative(
                            direction
                    );

            BlockState neighbourState =
                    level.getBlockState(
                            neighbourPos
                    );

            if (!(neighbourState.getBlock()
                    instanceof EngineBlock neighbourEngine)) {
                continue;
            }

            Direction neighbourFacing =
                    neighbourState.getValue(
                            FACING
                    );

            BlockState candidateState =
                    defaultBlockState()
                            .setValue(
                                    FACING,
                                    neighbourFacing
                            )
                            .setValue(
                                    LIT,
                                    false
                            );

            if (!neighbourEngine.hasShaftTowards(
                    level,
                    neighbourPos,
                    neighbourState,
                    direction.getOpposite()
            )) {
                continue;
            }

            if (!hasShaftTowards(
                    level,
                    pos,
                    candidateState,
                    direction
            )) {
                continue;
            }

            preferredFacing =
                    neighbourFacing;

            connectedToEngine =
                    true;

            break;
        }

        if (!connectedToEngine) {
            for (Direction direction : directions) {
                BlockPos neighbourPos =
                        pos.relative(
                                direction
                        );

                BlockState neighbourState =
                        level.getBlockState(
                                neighbourPos
                        );

                if (!(neighbourState.getBlock()
                        instanceof IRotate rotate)) {
                    continue;
                }

                if (!rotate.hasShaftTowards(
                        level,
                        neighbourPos,
                        neighbourState,
                        direction.getOpposite()
                )) {
                    continue;
                }

                preferredFacing =
                        direction.getAxis() == Direction.Axis.X
                                ? direction
                                : direction.getOpposite();

                break;
            }
        }

        return defaultBlockState()
                .setValue(
                        FACING,
                        preferredFacing
                )
                .setValue(
                        LIT,
                        false
                );
    }

    @Override
    public void setPlacedBy(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable LivingEntity placer,
            ItemStack stack
    ) {
        super.setPlacedBy(
                level,
                pos,
                state,
                placer,
                stack
        );

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (!(level.getBlockEntity(pos)
                instanceof EngineBlockEntity engine)) {
            return;
        }

        List<EngineBlockEntity> group =
                engine.getEngineGroup();

        if (group.size() <= 3) {
            engine.synchronizeWithEngineGroupAfterPlacement();
            return;
        }

        boolean existingGroupRunning =
                false;

        for (EngineBlockEntity groupEngine : group) {
            if (groupEngine == engine) {
                continue;
            }

            if (groupEngine.isRunning()) {
                existingGroupRunning =
                        true;

                break;
            }
        }

        if (!existingGroupRunning) {
            return;
        }

        Block.popResource(
                serverLevel,
                pos,
                new ItemStack(
                        CreateMechanicalDrive.ENGINE.get()
                )
        );

        serverLevel.destroyBlock(
                pos,
                false
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
        if (stack.is(Items.LAVA_BUCKET)) {
            if (!(level.getBlockEntity(pos)
                    instanceof EngineBlockEntity engine)) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }

            FluidStack lava =
                    new FluidStack(
                            Fluids.LAVA,
                            1000
                    );

            int accepted =
                    engine.getLavaFuelHandler()
                            .fill(
                                    lava,
                                    IFluidHandler.FluidAction.SIMULATE
                            );

            if (accepted < 1000) {
                return ItemInteractionResult.SUCCESS;
            }

            if (!level.isClientSide) {
                engine.getLavaFuelHandler()
                        .fill(
                                lava,
                                IFluidHandler.FluidAction.EXECUTE
                        );

                level.playSound(
                        null,
                        pos,
                        SoundEvents.BUCKET_EMPTY_LAVA,
                        SoundSource.BLOCKS,
                        1.0F,
                        1.0F
                );

                if (!player.isCreative()) {
                    player.setItemInHand(
                            hand,
                            new ItemStack(
                                    Items.BUCKET
                            )
                    );
                }
            }

            return ItemInteractionResult.SUCCESS;
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public InteractionResult onSneakWrenched(
            BlockState state,
            UseOnContext context
    ) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
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

        if (player != null
                && !player.isCreative()) {
            player.getInventory()
                    .placeItemBackInInventory(
                            new ItemStack(
                                    CreateMechanicalDrive.ENGINE.get()
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
    public boolean hasShaftTowards(
            LevelReader level,
            BlockPos pos,
            BlockState state,
            Direction direction
    ) {
        return direction.getAxis()
                == state.getValue(FACING).getAxis();
    }

    @Override
    public Direction.Axis getRotationAxis(
            BlockState state
    ) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        super.createBlockStateDefinition(builder);
        builder.add(LIT);
    }

    @Override
    public List<ItemStack> getDrops(
            BlockState state,
            LootParams.Builder params
    ) {
        return List.of(
                new ItemStack(
                        CreateMechanicalDrive.ENGINE.get()
                )
        );
    }

    @Override
    public Class<EngineBlockEntity> getBlockEntityClass() {
        return EngineBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends EngineBlockEntity>
    getBlockEntityType() {
        return CreateMechanicalDrive.ENGINE_BLOCK_ENTITY.get();
    }
}
