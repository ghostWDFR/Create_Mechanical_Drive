package dev.createmechanicaldrive.content.rotary_limiter;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.AbstractEncasedShaftBlock;
import com.simibubi.create.foundation.block.IBE;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
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

public class RotaryLimiterBlock
        extends AbstractEncasedShaftBlock
        implements IBE<RotaryLimiterBlockEntity>, IWrenchable {

    public static final DirectionProperty FACING =
            BlockStateProperties.FACING;

    public RotaryLimiterBlock(Properties properties) {
        super(properties);

        registerDefaultState(
                defaultBlockState()
                        .setValue(AXIS, Direction.Axis.X)
                        .setValue(FACING, Direction.EAST)
        );
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(
            BlockPlaceContext context
    ) {
        Player player =
                context.getPlayer();

        boolean crouching =
                player != null
                        && player.isCrouching();

        Direction lookDirection =
                context.getNearestLookingDirection();

        Direction facing =
                crouching
                        ? lookDirection.getOpposite()
                        : lookDirection;

        return defaultBlockState()
                .setValue(FACING, facing)
                .setValue(AXIS, facing.getAxis());
    }

    @Override
    public boolean hasShaftTowards(
            LevelReader level,
            BlockPos pos,
            BlockState state,
            Direction direction
    ) {
        return direction.getAxis()
                == state.getValue(AXIS);
    }

    @Override
    public Direction.Axis getRotationAxis(
            BlockState state
    ) {
        return state.getValue(AXIS);
    }

    public static Direction getOutputDirection(
            BlockState state
    ) {
        return state.getValue(FACING);
    }

    public static Direction getInputDirection(
            BlockState state
    ) {
        return state.getValue(FACING)
                .getOpposite();
    }

    @Override
    public List<ItemStack> getDrops(
            BlockState state,
            LootParams.Builder params
    ) {
        return List.of(
                new ItemStack(
                        CreateMechanicalDrive
                                .ROTARY_LIMITER_ITEM
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
                        .ROTARY_LIMITER_ITEM
                        .get()
        );
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        super.createBlockStateDefinition(builder);

        builder.add(FACING);
    }

    @Override
    public Class<RotaryLimiterBlockEntity>
    getBlockEntityClass() {
        return RotaryLimiterBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends RotaryLimiterBlockEntity>
    getBlockEntityType() {
        return CreateMechanicalDrive
                .ROTARY_LIMITER_BLOCK_ENTITY
                .get();
    }
}