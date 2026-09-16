package dev.createmechanicaldrive.content.shaft_distributor;

import dev.createmechanicaldrive.CreateMechanicalDrive;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.HitResult;

import java.util.List;

public class FourWayShaftDistributorBlock
        extends ShaftDistributorBlock {

    public FourWayShaftDistributorBlock(
            Properties properties
    ) {
        super(properties);
    }

    public static boolean isShaftDirection(
            BlockState state,
            Direction direction
    ) {
        return direction.getAxis()
                == state.getValue(FACING).getAxis()
                || direction.getAxis() == getOutputAxis(state);
    }

    public static float getSpeedModifier(
            BlockState state,
            Direction source,
            Direction target
    ) {
        if (!isShaftDirection(state, source)
                || !isShaftDirection(state, target)) {
            return 0.0F;
        }

        return getSideFactor(state, target)
                / getSideFactor(state, source);
    }

    private static float getSideFactor(
            BlockState state,
            Direction side
    ) {
        Direction throughSide = getOutputDirection(state);

        if (side.getAxis() == throughSide.getAxis()) {
            return 1.0F;
        }

        Direction primaryShortSide = state.getValue(FACING);
        float primaryFactor =
                ShaftDistributorBlock.getCrossAxisSpeedModifier(
                        state,
                        throughSide,
                        primaryShortSide
                );

        return side == primaryShortSide
                ? primaryFactor
                : -primaryFactor;
    }

    @Override
    public boolean hasShaftTowards(
            LevelReader level,
            BlockPos pos,
            BlockState state,
            Direction direction
    ) {
        return isShaftDirection(state, direction);
    }

    @Override
    public List<ItemStack> getDrops(
            BlockState state,
            LootParams.Builder params
    ) {
        return List.of(
                new ItemStack(
                        CreateMechanicalDrive
                                .FOUR_WAY_SHAFT_DISTRIBUTOR_ITEM
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
                        .FOUR_WAY_SHAFT_DISTRIBUTOR_ITEM
                        .get()
        );
    }

    @Override
    public BlockEntityType<? extends ShaftDistributorBlockEntity>
    getBlockEntityType() {
        return CreateMechanicalDrive
                .FOUR_WAY_SHAFT_DISTRIBUTOR_BLOCK_ENTITY
                .get();
    }
}