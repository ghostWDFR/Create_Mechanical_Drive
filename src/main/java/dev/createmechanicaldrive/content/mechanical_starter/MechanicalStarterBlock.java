package dev.createmechanicaldrive.content.mechanical_starter;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;

public class MechanicalStarterBlock
        extends DirectionalKineticBlock
        implements IBE<MechanicalStarterBlockEntity>, IWrenchable {

    public static final float ROTATION_SPEED =
            32.0F;

    public static final float STRESS_CAPACITY =
            512.0F / ROTATION_SPEED;

    public static final int IMPULSE_TICKS =
            4;

    public MechanicalStarterBlock(
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
                        context.getNearestLookingDirection()
                                .getOpposite()
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
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit
    ) {
        if (player.isSpectator()) {
            return InteractionResult.PASS;
        }

        Direction buttonSide =
                state.getValue(FACING);

        if (hit.getDirection() != buttonSide) {
            return InteractionResult.PASS;
        }

        withBlockEntityDo(
                level,
                pos,
                starter -> starter.activate(
                        player.isShiftKeyDown()
                )
        );

        if (!level.isClientSide) {
            level.playSound(
                    null,
                    pos,
                    SoundEvents.STONE_BUTTON_CLICK_ON,
                    SoundSource.BLOCKS,
                    0.35F,
                    0.75F
            );
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        if (stack.isEmpty()) {
            return ItemInteractionResult
                    .PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (AllItems.WRENCH.isIn(stack)) {
            return ItemInteractionResult
                    .PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        return ItemInteractionResult
                .SKIP_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected boolean areStatesKineticallyEquivalent(
            BlockState oldState,
            BlockState newState
    ) {
        return oldState.getBlock() == newState.getBlock()
                && oldState.getValue(FACING)
                == newState.getValue(FACING);
    }

    @Override
    public List<ItemStack> getDrops(
            BlockState state,
            LootParams.Builder params
    ) {
        return List.of(
                new ItemStack(
                        CreateMechanicalDrive
                                .MECHANICAL_STARTER_ITEM
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
                        .MECHANICAL_STARTER_ITEM
                        .get()
        );
    }

    @Override
    public Class<MechanicalStarterBlockEntity>
    getBlockEntityClass() {
        return MechanicalStarterBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends MechanicalStarterBlockEntity>
    getBlockEntityType() {
        return CreateMechanicalDrive
                .MECHANICAL_STARTER_BLOCK_ENTITY
                .get();
    }
}
