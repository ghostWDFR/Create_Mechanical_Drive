package dev.createmechanicaldrive.content.shaft_distributor;

import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class ShaftDistributorBlockEntity
        extends SplitShaftBlockEntity {

    public ShaftDistributorBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state
    ) {
        super(type, pos, state);
    }

    @Override
    public float getRotationSpeedModifier(
            Direction direction
    ) {
        BlockState state = getBlockState();

        if (!isShaftDirection(state, direction)) {
            return 0.0F;
        }

        if (!hasSource()) {
            return 1.0F;
        }

        Direction sourceFacing = getSourceFacing();

        if (!isShaftDirection(state, sourceFacing)) {
            return 1.0F;
        }

        if (state.getBlock() instanceof FourWayShaftDistributorBlock) {
            return FourWayShaftDistributorBlock.getSpeedModifier(
                    state,
                    sourceFacing,
                    direction
            );
        }

        if (sourceFacing.getAxis() == direction.getAxis()) {
            return 1.0F;
        }

        return ShaftDistributorBlock.getCrossAxisSpeedModifier(
                state,
                sourceFacing,
                direction
        );
    }

    public float getVisualSpeedForSide(
            Direction side
    ) {
        BlockState state = getBlockState();

        if (!isShaftDirection(state, side)) {
            return 0.0F;
        }

        if (!hasSource()) {
            return getSpeed();
        }

        return getSpeed() * getRotationSpeedModifier(side);
    }

    private static boolean isShaftDirection(
            BlockState state,
            Direction direction
    ) {
        if (state.getBlock() instanceof FourWayShaftDistributorBlock) {
            return FourWayShaftDistributorBlock.isShaftDirection(
                    state,
                    direction
            );
        }

        return ShaftDistributorBlock.isShaftDirection(state, direction);
    }

    @Override
    protected boolean isNoisy() {
        return false;
    }
}