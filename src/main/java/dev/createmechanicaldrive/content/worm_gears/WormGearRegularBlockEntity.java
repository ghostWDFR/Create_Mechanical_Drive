package dev.createmechanicaldrive.content.worm_gears;

import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class WormGearRegularBlockEntity
        extends KineticBlockEntity {

    private static final float WORM_RATIO = 1.0F / 4.0F;

    public WormGearRegularBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state
    ) {
        super(
                type,
                pos,
                state
        );
    }

    public static boolean isValidWormCogConnection(
            BlockState wormState,
            BlockState cogState,
            BlockPos diff
    ) {
        if (!ICogWheel.isSmallCog(cogState)) {
            return false;
        }

        if (diff.distManhattan(BlockPos.ZERO) != 1) {
            return false;
        }

        if (!(wormState.getBlock() instanceof IRotate worm)) {
            return false;
        }

        if (!(cogState.getBlock() instanceof IRotate cog)) {
            return false;
        }

        Direction.Axis wormAxis =
                worm.getRotationAxis(wormState);

        Direction.Axis cogAxis =
                cog.getRotationAxis(cogState);

        Direction connectionDirection =
                Direction.getNearest(
                        diff.getX(),
                        diff.getY(),
                        diff.getZ()
                );

        Direction.Axis connectionAxis =
                connectionDirection.getAxis();

        if (wormAxis == cogAxis) {
            return false;
        }

        if (connectionAxis == wormAxis) {
            return false;
        }

        return connectionAxis != cogAxis;
    }

    public static int getDirectionSign(
            Direction.Axis wormAxis,
            Direction.Axis cogAxis,
            Direction connectionDirection
    ) {
        Direction.Axis connectionAxis =
                connectionDirection.getAxis();

        int permutationSign;

        if (
                wormAxis == Direction.Axis.X
                        && cogAxis == Direction.Axis.Y
                        && connectionAxis == Direction.Axis.Z
        ) {
            permutationSign = 1;
        } else if (
                wormAxis == Direction.Axis.Y
                        && cogAxis == Direction.Axis.Z
                        && connectionAxis == Direction.Axis.X
        ) {
            permutationSign = 1;
        } else if (
                wormAxis == Direction.Axis.Z
                        && cogAxis == Direction.Axis.X
                        && connectionAxis == Direction.Axis.Y
        ) {
            permutationSign = 1;
        } else {
            permutationSign = -1;
        }

        int sideSign =
                connectionDirection.getAxisDirection()
                        == Direction.AxisDirection.POSITIVE
                        ? 1
                        : -1;

        return -permutationSign * sideSign;
    }

    public float getOutputSpeedFor(
            KineticBlockEntity target
    ) {
        BlockState wormState =
                getBlockState();

        BlockState cogState =
                target.getBlockState();

        BlockPos diff =
                target.getBlockPos()
                        .subtract(getBlockPos());

        if (!isValidWormCogConnection(
                wormState,
                cogState,
                diff
        )) {
            return 0.0F;
        }

        if (!(wormState.getBlock() instanceof IRotate worm)) {
            return 0.0F;
        }

        if (!(cogState.getBlock() instanceof IRotate cog)) {
            return 0.0F;
        }

        Direction.Axis wormAxis =
                worm.getRotationAxis(wormState);

        Direction.Axis cogAxis =
                cog.getRotationAxis(cogState);

        Direction connectionDirection =
                Direction.getNearest(
                        diff.getX(),
                        diff.getY(),
                        diff.getZ()
                );

        int directionSign =
                getDirectionSign(
                        wormAxis,
                        cogAxis,
                        connectionDirection
                );

        return getSpeed()
                * -directionSign
                * WORM_RATIO;
    }
}